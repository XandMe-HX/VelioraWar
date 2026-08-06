package id.veliora.war.party;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

public final class PartyManager {
    private final Map<UUID, Party> byMember = new HashMap<>();

    public Party create(UUID leader) {
        Party party = new Party(leader);
        byMember.put(leader, party);
        return party;
    }

    public Optional<Party> get(UUID player) { return Optional.ofNullable(byMember.get(player)); }

    public boolean add(Party party, UUID player, int maximum) {
        if (byMember.containsKey(player) || !party.add(player, maximum)) return false;
        byMember.put(player, party);
        return true;
    }

    public void leave(UUID player) {
        Party party = byMember.remove(player);
        if (party == null) return;
        if (party.leader().equals(player)) party.members().forEach(byMember::remove);
        else party.remove(player);
    }
}
