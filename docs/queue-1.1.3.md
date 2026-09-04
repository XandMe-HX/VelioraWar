# Waiting queue 1.1.3

Regular team-mode joins wait outside the arena until the selected match size has
all participants. Teams are assigned automatically. Each queue entry expires
after 60 seconds (queue.wait-timeout-seconds); /vgwar leave cancels immediately.
No inventory backup or teleport occurs while merely queued.

The expiry scan runs once per second, not every tick. Disconnect, shutdown and
maintenance remove queued entries. Accepted direct duels retain their existing
acceptance flow; All Mode remains a separate free-for-all path.

Manual server checks:
- One non-OP member joins 1vs1: stays at original location, retains inventory,
  may move/use ordinary commands; times out after 60 seconds.
- Second member joins same mode/size before timeout: automatic opposing teams,
  teleport and countdown. 2vs2 requires four members.
- Different modes/sizes do not match; full arena does not extend timeout.
- Leave/disconnect/maintenance cancels queue. Countdown departure returns remaining
  players instead of leaving them trapped waiting alone.
- Replacing the JAR requires restart. No arena setup reset is needed.
