# Combined verification, stage 5

Fixed a regression: reloadAll called MatchManager.shutdown, cancelling the queue
timer permanently. Reload now restarts exactly one queue timer after configuration
and arenas load. Existing queues remain cancelled on reload as before.

Remaining integration finding (not fixed by this lifecycle patch):
Suite RaceBenefits directly changes health (Vampire combat) and reapplies potion
effects/passive attributes. War's CUSTOM regain filter and one-time attribute
snapshot do not provide complete isolation. Do not claim pure vanilla combat yet.

Server acceptance checklist:
- Non-OP Java and Bedrock: join queue, cancel, wait 60 seconds; repeat after reload.
- Two players: countdown departure, crystal/anchor self/opponent damage, refill
  repeated and armor visible; use Vampire/Orc alongside Human for isolation checks.
- AFK: TAB visible; ride/teleport/return; Essentials remains state owner.
- Kits: Premium centered; permission/price enforced; /ce menu is text only.
- OreMask: PacketEvents active, correct world, enclosed ores hidden, exposed ores
  revealed; /voremask skipped/errors counters and real server timing checked.

No live server/client session was available to execute this checklist.
