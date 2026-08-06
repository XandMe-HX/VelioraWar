package id.veliora.war.queue;

import id.veliora.war.match.MatchMode;
import id.veliora.war.match.MatchSize;
import id.veliora.war.match.MatchTeam;

import java.util.UUID;

public record QueueEntry(UUID playerId, MatchMode mode, MatchSize size, MatchTeam team, long joinedAt) {
    public QueueEntry(UUID playerId, MatchMode mode, MatchSize size, MatchTeam team) {
        this(playerId, mode, size, team, System.currentTimeMillis());
    }
}
