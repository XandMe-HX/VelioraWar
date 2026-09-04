# Progress 1: combat/refill

- Explosion player damage uses one handler and respects the activity arena's
  PVP/explosion flags, countdown, elimination and spawn protection. Region
  cancellation is overridden only for an eligible participant inside that arena.
  Crystal self-damage is no longer mistaken for friendly fire.
- Removed unreliable anchor attribution based on any click within 12 blocks in
  the preceding 2.5 seconds. Unattributed anchor explosions use vanilla damage,
  including self/team damage, rather than guessing ownership.
- CPVP/All Mode default armor has Protection IV only. A once-only backed-up
  migration removes Blast Protection where Protection is also present.
- Refill is deferred until after the inventory click, revalidates participation,
  closes the menu and resends inventory after applying. No health refill is added.
- Starting saturation is 5 instead of 20. During combat, natural food healing is
  capped at 1 HP per 4 seconds; CUSTOM regain events are blocked by default.
  Potion and golden-apple regain reasons are unchanged. Both settings live under
  combat in config.yml. Other plugins directly calling setHealth are not covered.

Verification: automated queue and armor migration tests plus Maven build.
Live Java/Bedrock testing is still required: crystal/anchor opponent and self
damage, protected spectators/countdown, explosion flag off, golden apples and
potions, repeated refill and visible equipment. Check external region flags if
another plugin prevents the explosion itself from occurring.

Install by replacing JAR and restarting. Arena definitions are not reset.
Inventory event reference:
https://jd.papermc.io/paper/1.21.8/org/bukkit/event/inventory/InventoryClickEvent.html
