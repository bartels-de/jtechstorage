# Farm tools

Four blocks for your Minecraft farms – iron farms, gold farms, mob farms, crop farms. They don't build farms for you
and don't make them produce more: they move items faster, show what a farm produces and keep your companion from
attacking the mobs your farm needs.

## Amethyst Hopper

A hopper made of deepslate, gold and amethyst. It works exactly like a normal hopper – outputs down or sideways, has
five slots, is locked by a redstone signal and works with comparators – but it is **16 times as fast**: it moves **8 items
every 4 ticks**. Without a container above it, it also **sucks in items lying in a 3 × 3 area above it**.

## Crystal Pusher

Pushes items from the container **behind** it into the container **in front** of it – in **all six directions**, upwards
too. When you place it, the output faces the direction you are looking (like a dispenser); the violet line on its sides
shows the direction of travel. Same speed as the Amethyst Hopper; a redstone signal stops it.

Right-click opens its **filter** with 9 slots: click with an item to set a slot, without an item to clear it, or
Shift-click an item in your inventory. With an empty filter it pushes everything. It respects the sides of furnaces,
brewing stands and the like, just like a hopper.

## Farm Monitor

Place it with its **back against your farm's output chest** (if there is no container behind it, it watches the one
below, above or to the side). It counts every item that newly arrives in that container.

- The **screen** on the front shows green bars while the farm runs and red bars when it has stopped, the container is
  full or there is no container.
- **Right-click** opens its window:
  - a **name** for the farm (an anvil-renamed monitor keeps its name),
  - **items per hour** (from the last hour, extrapolated while it measures for less than an hour) and a **chart** of the
    last hour in 5-minute steps,
  - the **5 most common items** since the last reset,
  - **Warn after** 5, 10 or 30 minutes without new items – the monitor turns red and its owner gets a chat message (also
    when the container is full or missing),
  - **Safe zone** of 8, 16, 24 or 32 blocks around the monitor: **companions don't attack monsters there**, not even with
    special attacks or area damage – the zombie in your iron farm stays in peace,
  - **Reset** clears the statistics.
- **Click it with a bound Storage Key** to link it to that terminal (click again to unlink). The terminal then lists it
  under **Statistics → ⚙ Farms** with name, position, items per hour and status – also while the farm is not loaded (with
  the last known values).

## Chunk Loader

Keeps **its own chunk** loaded at all times – like `/forceload`, also while you are offline – so your farm keeps running.

- **Fuel:** amethyst shards, **1 shard per hour** of real time (an amethyst block counts as 4 shards). Put them into its
  slot or feed it with a hopper. Without fuel it switches off (the crystal goes dark) and its owner gets a chat message.
- **Limit:** each player may have **25 active chunk loaders** by default (`farm.chunkLoadersPerPlayer` in the
  [configuration](15-configuration.md)); one more stays off. Several loaders in the same chunk don't get in each other's way.
- **Right-click** shows the status ("Active – chunk 12, −7 stays loaded"), the remaining time, how many you have, and a
  button that shows the chunk borders with particles for 10 seconds.

| Block | Recipe |
|---|---|
| Amethyst Hopper | hopper, gold ingot, 2 amethyst shards |
| Crystal Pusher | dropper, gold ingot, 2 amethyst shards |
| Farm Monitor | observer, glass pane, 2 gold ingots, amethyst shard |
| Chunk Loader | block of diamond, glass, 2 gold ingots, 5 polished deepslate |

See [Recipes](13-recipes.md#farm-tools) for the crafting grids.

---

[← Configuration](15-configuration.md) · [Guide overview](README.md)
