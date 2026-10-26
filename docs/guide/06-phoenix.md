# Phoenix mail and travel

## Phoenix mail

<img src="../images/items/phoenix.png" width="64" align="right">

1. Place a **Phoenix Nest** and link it to your network with a bound Storage Key. The nest is now your **mail address**.
2. Place a **Phoenix** next to it and link it too. It moves into the nest.
3. Open the nest (or right-click the phoenix), choose a **destination**, put up to **nine items** on the parcel fields and
   press *Send*.

- The phoenix takes the items **from your storage** and flies off.
- Parcels arrive in the **recipient's storage**, or in the nest inbox if that storage is full.
- If the destination is not loaded, the parcel simply waits – nothing is lost.
- **Standing orders:** send the same parcel automatically every day.
- **Accept list:** receive parcels from everyone, only your team, or nobody.
- The flight time depends on the distance (`phoenixMailBlocksPerSecond` in the [configuration](15-configuration.md)).
- **Between dimensions:** nests in the Overworld and the Crystal Depths (or any other dimension) can send to each other.
  Each side needs its own nest linked to its own terminal; such a flight always takes **60 seconds**.

## Amethyst Phoenix

<img src="../images/items/amethyst_phoenix.png" width="64" align="right">

Right-click a Phoenix with an **Amethyst Obsidian Upgrade** and it evolves into a large, majestic Amethyst Phoenix that
carries players and cargo along a route.

### Building a route

1. Build up to **six Phoenix Stops**. Right-click each one to give it a **name** and a **rest time**: 30 seconds,
   5 minutes or one night.
2. Click the stops with a **Flight Plan** in the order you want.
3. Right-click the Flight Plan in the air to reorder or remove stops.
4. Right-click the Amethyst Phoenix with the Flight Plan.

The phoenix lands on top of each stop, rests as long as the stop says and then flies on. After the last stop it starts
over. It keeps the chunks along its path loaded.

**Routes across dimensions:** the stops may be in different dimensions – for example two in the Overworld and two in the
Crystal Depths. When the next stop is in the other dimension, the phoenix climbs, vanishes in a **flame jump** and appears
right above that stop, riders and cargo included. No portal is needed. The Flight Plan window shows the dimension under
each stop's coordinates.

In the caves of the Crystal Depths it flies below the ceiling. If it gets stuck on the way (a wall, a narrow passage), it
jumps straight above the stop after a few seconds.

### Riding and cargo

| Action | Effect |
|---|---|
| Right-click | mount – there are **two seats** |
| Jump (front seat) | depart early |
| Right-click with a chest | add a chest (up to **four**, 27 slots each) |
| Inventory key while riding | menu: chests and sit mode |
| Sit mode | stops all movement |
| Sneak + right-click twice | pack it up – chests, cargo and route come along |

More chests and more cargo make it slower.

### Delivery orders

Put chests or barrels **right next to a stop** and choose *Unload*, *Load* or *Both* in the stop's window.

- Up to **9 filters** decide which items move. With the scroll wheel you set an amount per visit.
- *Unload* empties the phoenix's chests into the containers, *Load* fills them from the containers.
- *Both* unloads everything that is **not** in the filters and loads the filter items – one stop can deliver and pick up
  at the same time.

## Signal Lantern

The **Signal Lantern** lights up and emits a **redstone signal of strength 15** while an Amethyst Phoenix rests at a stop
within 8 blocks. Use it to open gates when the phoenix lands, ring a bell or start a hopper line.

---

[← Cheldrogan](05-cheldrogan.md) · [Guide overview](README.md) · Next: [Companions →](07-companions.md)
