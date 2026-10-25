# Farm tools

Five blocks for your Minecraft farms – iron farms, gold farms, mob farms, crop farms. They don't build farms for you
and don't make them produce more: they move items faster, replant for you, show what a farm produces and keep your companion from
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

## Block Placer

Works like a **dispenser** – 9 slots, faces all six directions, fires on a redstone pulse, can be filled by hoppers –
but instead of shooting items out it **places the next block** from its inventory right in front of it.

- It places any block, and plants too: saplings, seeds, mushrooms, sugar cane, bamboo and so on – as long as they can
  stand there (saplings on dirt, seeds on farmland …).
- If the spot is taken or nothing fits, it just clicks.
- Great for tree farms: put saplings in it and pulse it after the tree is cut.

**Recipe:** cobbled deepslate all around, a piston in the middle and redstone at the bottom centre – like a dispenser
with a piston instead of a bow.

## Crystal Conveyor

A flat belt that faces the direction you look in when you place it. Everything on it moves in the arrow direction:
**items, animals, monsters and players**.

- At the end of the belt, items go straight into a **chest, barrel, hopper** or any other container in front of it.
- **Sneak** to stand still on a belt. A **redstone signal** stops that belt piece.
- **Fast Crystal Conveyor:** twice as fast and glows softly.

**Recipes:** 3 amethyst shards over iron – redstone – iron gives **8 belts**. **8 belts around a glow crystal** give 8
fast belts.

## Crystal Sorter

Takes items from the **back** (a hopper above or behind it, or a conveyor running into it) and sends them out on three
sides: **left**, **straight on** (the front) and **right**.

- **Right-click** opens the filter window with three rows of 9 slots: *To the left*, *Straight on* and *To the right*.
  Click a slot with an item to set a filter (the item stays with you), click with an empty hand to clear it.
  Shift-click from your inventory fills the next free slot.
- Items that match **no filter** go straight on – or stay inside if you switch the button to **Rest: hold**.
- Outputs go into a container on that side, or are dropped onto a conveyor or into free space.
- It moves up to 8 items every 4 ticks. A **redstone signal** stops it, a **comparator** reads 15 while an item waits.

**Recipe:** an amethyst shard on top, a conveyor – hopper – conveyor in the middle row and a comparator below.

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

[← Configuration](15-configuration.md) · [Guide overview](README.md) · Next: [The Crystal Depths →](17-crystal-depths.md)
