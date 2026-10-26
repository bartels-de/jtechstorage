# Helpers

Helpers are small amethyst creatures that work for your network. Every helper recipe needs an **Amethyst Core**
(from the [Amethyst Robot](09-bosses-and-end.md#amethyst-robot) or the [Geode Keeper](07-companions.md#trading-with-the-geode-keeper)).
Recipes: [Recipes → Helpers](13-recipes.md#helpers-and-their-homes).

## Controls for all helpers

| Action | Effect |
|---|---|
| Use the helper item on the ground | place the helper |
| Click with a bound Storage Key | link it to your network |
| Right-click | main action (status, menu or mount) |
| Sneak + right-click **twice** | pack it up into an item – name, color and contents are kept |
| Inventory key while riding | opens the chest (Storage Griffin) or the menu (Amethyst Phoenix) |
| Right-click with a dye | recolor its crystals |
| Right-click with a hat item | put on a hat |
| Right-click with shears | take the hat off and restore the original color |

Hats: pumpkin, flowers, cake, mushrooms, amethyst cluster, candle, cookie and more (modpacks can add items through the
`jtechstorage:helper_hats` item tag).

## Overview

| | Helper | Job |
|---|---|---|
| <img src="../images/items/amethyst_sprite.png" width="32"> | [Amethyst Sprite](#amethyst-sprite) | sorts chests by category, collects items |
| <img src="../images/items/collector_moth.png" width="32"> | [Watch Moth](#watch-moth) | night guard, stops monster spawns |
| <img src="../images/items/storage_griffin.png" width="32"> | [Storage Griffin](#storage-griffin) | flying mount with a chest |
| <img src="../images/items/spark_beetle.png" width="32"> | [Spark Beetle](#spark-beetle) | charges resonance |
| <img src="../images/items/chel.png" width="32"> | [Chel](#chel) | chaotic auto-crafter |
| <img src="../images/items/peter_chel_parker.png" width="32"> | [Peter Chel Parker](#peter-chel-parker) | base protector (evolved Chel) |
| <img src="../images/items/cheldrogan.png" width="32"> | [Cheldrogan](05-cheldrogan.md) | rideable crystal serpent dragon (evolved Peter) |
| <img src="../images/items/ashley.png" width="32"> | [Ashley](#ashley) | Peter's (unwilling) healer |
| <img src="../images/items/phoenix.png" width="32"> | [Phoenix](06-phoenix.md#phoenix-mail) | mail carrier |
| <img src="../images/items/amethyst_phoenix.png" width="32"> | [Amethyst Phoenix](06-phoenix.md#amethyst-phoenix) | travel and cargo flights |
| <img src="../images/items/amethyst_fox.png" width="32"> | [Amethyst Fox](10-crystal-enchantments.md) | digs up crystal enchantment books |
| <img src="../images/items/crystal_wolf.png" width="32"> | [Crystal Wolf](#crystal-wolf) | guard, herder and loot collector |
| <img src="../images/items/moon_wolf.png" width="32"> | [Moon Wolf](#moon-wolf) | leader of the pack |

## Amethyst Sprite

Link it with a bound key and it starts tidying up:

- gives every linked chest a fixed **category** (15 categories, see [Storage network](02-storage-network.md#category-tabs))
  and moves stacks to the right chest,
- picks up **dropped items** within 12 blocks of the terminal,
- tidies chests from the inside (merges stacks),
- empties **placed backpacks** nearby into the network.

Hold the key and look at a chest to see its category.

## Watch Moth

A nocturnal guard. At night it circles above its home and

- **stops natural monster spawns** within **24 blocks** of its home while it is awake,
- makes monsters within that range glow – only those it can actually see,
- rings a bell to warn you and posts to the *guard* channel,
- shows [Peter Chel Parker](#peter-chel-parker) his targets.

It sleeps during the day; then the spawn protection pauses too.

## Storage Griffin

A flying mount with a **27-slot chest**. Link it with the key.

- Right-click: mount. While riding, the inventory key opens its chest.
- When it lands within terminal range, it **unloads everything into the network**.
- Packing it up keeps the chest contents.

## Spark Beetle

Charges your terminal with **resonance** when it is linked and within 8 blocks of the terminal (×2 in rain, ×5 in
thunder). Feed it redstone, glowstone or a redstone block for a boost. It always sleeps at night. What resonance does:
[Storage network → Resonance](02-storage-network.md#resonance-optional).

## Chel

A chaotic little auto-crafter.

- Link him with your key, then **right-click with an empty hand** to open his order menu.
- Drop an item on a slot and choose an amount: Chel keeps that many **in stock** (up to 8 orders) and crafts the missing
  ones from your network with normal crafting recipes.
- Give him a **diamond** and he tells you a joke (they are collected in [Chel's Joke Book](12-books-and-interface.md#books)).
- Give him a **nether star** and he evolves into [Peter Chel Parker](#peter-chel-parker).

## Peter Chel Parker

Chel after a nether star: an amethyst knight who protects your base.

- Guards **16 blocks** around his home and fights every monster: heavy hits with a shockwave and thrown crystals
  against flying enemies.
- He can be hurt. To heal he kicks [Ashley](#ashley).
- At 0 health he turns back into Chel.
- He never takes a break at night (he only dozes).
- Give him a **Dragon Heart** to evolve him into [Cheldrogan](05-cheldrogan.md).

## Ashley

A rather filthy ash creature: she leaves a trail of ash, sneezes dust clouds and has never washed. Peter Chel Parker
and Cheldrogan heal by kicking her – healing embers fly out of her fur.

## Crystal Wolf

Feed a **tamed wolf** an **Amethyst Core** and it becomes a Crystal Wolf. Link it with your key.

- Guards the base (20 blocks around the terminal) and attacks monsters.
- Herds stray farm animals back.
- Brings monster loot (bones, string, gunpowder …) into the network.
- Fights harder in a pack.
- **Bone:** switch between following you and guarding the base. **Meat:** heal it.
- It cannot die: with too little health it retreats exhausted.
- At night it sleeps in a [Wolf Basket](#beds-and-rest) and wakes up at once when monsters come close or a storm rages.

## Moon Wolf

Once you own a pack of at least **5 Crystal Wolves**, a Moon Wolf sometimes appears on a hill near your base on
**full-moon nights**. At dawn it disappears again.

- Win it over with an **Amethyst Bone**.
- It leads the pack: Crystal Wolves near it bite harder.
- Right-click it with a **compass**: it scents the nearest structure (village, mineshaft, ruined portal, buried treasure,
  trial chambers, ancient city).
- During a [Resonance Storm](08-resonance-storm.md) it howls at the moon: storm creatures freeze for a moment and the
  pack becomes stronger and faster.

## Beds and rest

- At night your helpers rest (the Watch Moth during the day). With **resonance** in the terminal they keep working.
- Place **Helper Beds** (small helpers) and **Wolf Baskets** (wolves) within **16 blocks** of the terminal. Every helper
  reserves its own bed; if none is free, it lies down on the ground.
- The Storage Griffin, the phoenixes and Cheldrogan need no bed. Peter and the Amethyst Phoenix only doze, Cheldrogan
  coils up – they all stay on duty.

## Evolutions

| From | Item | To |
|---|---|---|
| Chel | nether star | Peter Chel Parker |
| Peter Chel Parker | Dragon Heart | Cheldrogan |
| Phoenix | Amethyst Obsidian Upgrade | Amethyst Phoenix |
| tamed wolf | Amethyst Core | Crystal Wolf |

Owner and network link are kept with every evolution. Cheldrogan falls back to Peter at 0 health, Peter to Chel.

---

[← Teams and trade](03-teams-and-trade.md) · [Guide overview](README.md) · Next: [Cheldrogan →](05-cheldrogan.md)
