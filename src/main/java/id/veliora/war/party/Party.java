package id.veliora.war.party;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.UUID;

public final class Party {
    private final UUID leader;
    private final LinkedHashSet<UUID> members = new LinkedHashSet<>();

    public Party(UUID leader) {
        this.leader = leader;
        this.members.add(leader);
    }

    public UUID leader() { return leader; }
    public Set<UUID> members() { return Set.copyOf(members); }
    public boolean add(UUID player, int maximum) { return members.size() < maximum && members.add(player); }
    public boolean remove(UUID player) { return !leader.equals(player) && members.remove(player); }
    public boolean contains(UUID player) { return members.contains(player); }
}
