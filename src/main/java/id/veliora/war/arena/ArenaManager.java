package id.veliora.war.arena;

import id.veliora.war.match.MatchMode;
import id.veliora.war.match.MatchSize;
import id.veliora.war.storage.ArenaStorage;
import org.bukkit.Location;
import org.bukkit.entity.Player;
import java.util.*;

public final class ArenaManager {
    private final ArenaStorage storage;
    private final Map<String, Arena> arenas = new LinkedHashMap<>();
    private final Map<UUID, Location> firstPositions = new LinkedHashMap<>();
    private final Map<UUID, Location> secondPositions = new LinkedHashMap<>();
    public ArenaManager(ArenaStorage storage) { this.storage=storage; reload(); }

    public void reload() {
        Map<String,Arena> loaded=storage.loadAll(); arenas.clear();
        if(!loaded.isEmpty()) migrateToGlobalLand(loaded);
    }
    private void migrateToGlobalLand(Map<String,Arena> loaded) {
        ArenaRegion global=unionRegion(loaded.values());
        Arena flagSource=loaded.values().iterator().next();
        for(MatchMode mode:MatchMode.playable()) {
            Arena source=loaded.get(mode.id());
            if(source==null) source=loaded.values().stream().filter(a->a.mode()==mode).findFirst().orElse(null);
            Arena profile=new Arena(mode.id(),global); profile.mode(mode); profile.size(MatchSize.FOUR_VS_FOUR);
            copyFlags(flagSource,profile); if(source!=null) copyModeSettings(source,profile);
            arenas.put(profile.id(),profile);
        }
        save();
    }
    private ArenaRegion unionRegion(Collection<Arena> source) {
        Arena first=source.iterator().next(); String world=first.region().world();
        int minX=first.region().minX(),minY=first.region().minY(),minZ=first.region().minZ();
        int maxX=first.region().maxX(),maxY=first.region().maxY(),maxZ=first.region().maxZ();
        for(Arena a:source) {
            if(!world.equals(a.region().world())) continue;
            minX=Math.min(minX,a.region().minX()); minY=Math.min(minY,a.region().minY()); minZ=Math.min(minZ,a.region().minZ());
            maxX=Math.max(maxX,a.region().maxX()); maxY=Math.max(maxY,a.region().maxY()); maxZ=Math.max(maxZ,a.region().maxZ());
        }
        return new ArenaRegion(world,minX,minY,minZ,maxX,maxY,maxZ);
    }
    private void copyFlags(Arena source,Arena target){for(ArenaFlag f:ArenaFlag.values())target.flag(f,source.flag(f));}
    private void copyModeSettings(Arena source,Arena target){
        target.redSpawn(source.redSpawn()); target.greenSpawn(source.greenSpawn());
        target.refillNpcLocation(source.refillNpcLocation()); target.enabled(source.enabled()&&target.isComplete());
    }
    public void save(){storage.saveAll(arenas.values());}
    public void setPosition(Player p,int n){if(n==1)firstPositions.put(p.getUniqueId(),p.getLocation());else secondPositions.put(p.getUniqueId(),p.getLocation());}
    public Location position(Player p,int n){Location l=n==1?firstPositions.get(p.getUniqueId()):secondPositions.get(p.getUniqueId());return l==null?null:l.clone();}

    public void claimGlobal(Player p){
        if(hasLand())throw new IllegalStateException("Land global sudah ada. Gunakan /vgwar redefine confirm untuk mengganti batas");
        createGlobalProfiles(p,new EnumMap<>(MatchMode.class),null);
    }
    public void redefineGlobal(Player p){
        if(!hasLand())throw new IllegalStateException("Land belum ada. Gunakan /vgwar claim");
        Map<MatchMode,Arena> old=new EnumMap<>(MatchMode.class);
        for(Arena a:arenas.values())if(a.mode()!=null)old.put(a.mode(),a);
        createGlobalProfiles(p,old,globalArena().orElse(null));
    }
    private void createGlobalProfiles(Player p,Map<MatchMode,Arena> old,Arena flagSource){
        Location first=position(p,1),second=position(p,2);
        if(first==null||second==null)throw new IllegalStateException("Tentukan pos1 dan pos2 terlebih dahulu");
        if(first.getWorld()==null||second.getWorld()==null||!first.getWorld().equals(second.getWorld()))
            throw new IllegalStateException("pos1 dan pos2 harus berada di world yang sama");
        ArenaRegion region=new ArenaRegion(first,second); arenas.clear();
        for(MatchMode mode:MatchMode.playable()){
            Arena profile=new Arena(mode.id(),region);profile.mode(mode);profile.size(MatchSize.FOUR_VS_FOUR);
            if(flagSource!=null)copyFlags(flagSource,profile);
            Arena previous=old.get(mode);if(previous!=null)copyModeSettings(previous,profile);
            profile.enabled(false);arenas.put(profile.id(),profile);
        }
        firstPositions.remove(p.getUniqueId());secondPositions.remove(p.getUniqueId());save();
    }
    public Optional<Arena> globalArena(){return MatchMode.playable().stream().map(m->arenas.get(m.id())).filter(Objects::nonNull).findFirst();}
    public Optional<ArenaRegion> globalRegion(){return globalArena().map(Arena::region);}
    public boolean globalFlag(ArenaFlag f){return globalArena().map(a->a.flag(f)).orElse(f.defaultValue());}
    public void setGlobalFlag(ArenaFlag f,boolean value){arenas.values().forEach(a->a.flag(f,value));save();}
    public Optional<Arena> forMode(MatchMode mode){if(mode==null||!mode.isPlayable())return Optional.empty();return Optional.ofNullable(arenas.get(mode.id()));}
    public Optional<Arena> get(String id){MatchMode m=MatchMode.from(id).orElse(null);if(m!=null)return forMode(m);return Optional.ofNullable(arenas.get(normalize(id)));}
    public Optional<Arena> at(Location l){return globalArena().filter(a->a.region().contains(l));}
    public Optional<Arena> available(MatchMode m,MatchSize s){return forMode(m).filter(Arena::enabled).filter(a->a.state()==ArenaState.WAITING);}
    /**
     * All Mode uses the same global land claim as the other profiles, but it
     * still needs its own spawn and enabled state. Returning an empty optional
     * here made a configured All Mode impossible to enter or refill.
     */
    public Optional<Arena> allMode(){return forMode(MatchMode.ALL_MODE);}
    public boolean hasLand(){return globalArena().isPresent();}
    public boolean maintenance(){return arenas.values().stream().noneMatch(Arena::enabled);}
    public boolean hasActiveMatch(){return arenas.values().stream().anyMatch(a->a.state()!=ArenaState.WAITING&&a.state()!=ArenaState.DISABLED);}
    public int enableCompleteProfiles(){int n=0;for(Arena a:arenas.values()){boolean c=a.isComplete();a.enabled(c);if(c)n++;}save();return n;}
    public void disableAll(){arenas.values().forEach(a->{a.enabled(false);a.state(ArenaState.DISABLED);});save();}
    public void deleteLand(){if(hasActiveMatch())throw new IllegalStateException("Masih ada pertandingan aktif. Land tidak boleh dihapus");arenas.clear();save();}
    public Collection<Arena> all(){return List.copyOf(arenas.values());}
    public List<String> ids(){return new ArrayList<>(arenas.keySet());}
    private String normalize(String id){return id.toLowerCase().replace(' ','_').replace('-','_');}
}
