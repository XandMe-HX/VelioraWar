package id.veliora.war.queue;

import id.veliora.war.match.*;
import org.junit.jupiter.api.Test;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.*;

class QueueManagerTest {
    @Test void separatesModesAndSizesAndKeepsArrivalOrder() {
        QueueManager queue = new QueueManager();
        UUID first = UUID.randomUUID(), second = UUID.randomUUID();
        queue.enqueue(first, MatchMode.SWORD_DUEL, MatchSize.ONE_VS_ONE, MatchTeam.RED, 100);
        queue.enqueue(second, MatchMode.SWORD_DUEL, MatchSize.ONE_VS_ONE, MatchTeam.RED, 100);
        assertEquals(2, queue.count(MatchMode.SWORD_DUEL, MatchSize.ONE_VS_ONE));
        assertEquals(0, queue.count(MatchMode.MACE_PVP, MatchSize.ONE_VS_ONE));
        assertEquals(0, queue.count(MatchMode.SWORD_DUEL, MatchSize.TWO_VS_TWO));
        assertEquals(first, queue.entries().get(0).playerId());
        assertEquals(second, queue.entries().get(1).playerId());
    }

    @Test void snapshotAllowsRemovingExpiredOrDisconnectedEntries() {
        QueueManager queue = new QueueManager();
        UUID player = UUID.randomUUID();
        queue.enqueue(player, MatchMode.SWORD_DUEL, MatchSize.ONE_VS_ONE, MatchTeam.RED, 100);
        for (QueueEntry entry : queue.entries()) queue.remove(entry.playerId());
        assertFalse(queue.contains(player));
        assertTrue(queue.entries().isEmpty());
    }

    @Test void enforcesCapacityWithoutAddingExtraPlayer() {
        QueueManager queue = new QueueManager();
        queue.enqueue(UUID.randomUUID(), MatchMode.SWORD_DUEL, MatchSize.ONE_VS_ONE, MatchTeam.RED, 1);
        assertEquals(-1, queue.enqueue(UUID.randomUUID(), MatchMode.SWORD_DUEL,
                MatchSize.ONE_VS_ONE, MatchTeam.RED, 1));
        assertEquals(1, queue.count(MatchMode.SWORD_DUEL, MatchSize.ONE_VS_ONE));
    }
}
