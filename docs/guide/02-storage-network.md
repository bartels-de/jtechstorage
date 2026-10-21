# Storage network

JTechStorage turns the chests you already have into one network. There are no new storage blocks, no cables and no power:
a **Storage Terminal** shows and manages the contents of every container you link to it.

## Your first network

| Step | Action |
|---|---|
| 1 | Place a **Storage Terminal**. The player who places it is its owner. |
| 2 | **Sneak + right-click** the terminal with a **Storage Key** to bind the key. |
| 3 | **Sneak + right-click** chests, barrels or shulker boxes with the bound key to link them. Again to unlink. |
| 4 | Open the terminal with a right-click. |

- A double chest counts as **one** container.
- Linked containers **sparkle** while you hold the key.
- The same bound key also links **helpers**, a **Team Terminal**, a **Trade Terminal**, a **Phoenix Nest** and the
  **Night Raven** companion to this network.

## The terminal

Items are taken and stored just like in a chest. Number keys and Q work once you have clicked into the grid (the search
field is active when the terminal opens). While the mouse is over the grid, items stay in place even if their amounts
change; the list sorts again when the mouse leaves the grid.

| Action | Effect |
|---|---|
| Left click on an item | take a stack |
| Right click on an item | take half a stack |
| Shift + click on an item | move it straight into your inventory |
| Left click with an item on the cursor | store it in the network (on a different item: swap) |
| Right click with an item on the cursor | store one |
| Number keys 1–9 over an item | put a stack into that hotbar slot |
| Q / Ctrl + Q over an item | drop one / a stack |
| Right-click the search field | clear the search |

### Search

| Input | Finds |
|---|---|
| `diamond` | item names containing the text |
| `sharpness v` | items with that enchantment and level |
| `$mending` | items with an enchantment |
| `@minecraft` | items from a mod |
| `#logs` | items in a tag |

### Category tabs

The tabs on the left filter the list by the **15 categories**: Food, Tools & Weapons, Armor, Redstone, Ores & Raw
Materials, Ingots & Gems, Plants, Farming, Wood, Stone, Dirt & Sand, Mob Drops, Decor & Colors, Magic & Potions and
Miscellaneous. Click the active tab again (or right-click a tab) to show everything.

### Statistics

The chart button right of the hotbar opens the statistics: fill level, a 14-day history, the share of each category,
the most common items, and today's biggest gains and losses.

## Amethyst Chests

Bigger chests whose slots hold **several stacks**. Link them with the key like any other chest; the terminal, the
Amethyst Sprite, hoppers and Crystal Pushers all use the bigger stacks.

| Chest | Slots | Per slot | Recipe |
|---|---|---|---|
| Amethyst Chest | 54 | 2 stacks (128) | chest, 4 amethyst shards, 4 copper ingots |
| Crystal Chest | 81 | 4 stacks (256) | Amethyst Chest, 4 amethyst blocks, 4 iron ingots |
| Resonance Chest | 108 | 8 stacks (512) | Crystal Chest, 4 gold ingots, 4 diamonds |

- They open like a barrel; the front with the gold lock faces you when you place one.
- **Upgrade in place:** right-click a placed chest with a better one. The contents and the name stay, and you get
  the old chest back.
- Taking items out gives you at most one normal stack at a time. Unstackable items (tools, potions …) stay single.
- Break a chest and its contents drop as normal stacks. Like shulker boxes, they don't accept shulker boxes.
- Comparators read them like a chest.

## Upgrades

Upgrades go into the slots of the terminal. **Range** decides how far away containers may be, **capacity** how many
containers you can link.

| Tier | Range (blocks) | Capacity (containers) | How to get it |
|---|---:|---:|---|
| none | 20 | 8 | – |
| Iron | 32 | 16 | **Amethyst Robot** loot only |
| Gold | 48 | 32 | crafted from the iron upgrade |
| Diamond | 64 | 64 | crafted from the gold upgrade |
| Netherite | 96 | 128 | smithing table |
| Ender (range only) | 128 | – | crafted; also makes the Wireless Terminal unlimited and cross-dimensional |

All numbers can be changed in the [configuration](15-configuration.md). Recipes: [Recipes → Storage network](13-recipes.md#storage-network).

## Sorting rules

Hang an **item frame with an item** on a linked chest: that chest now accepts **only** this item, and items of this kind
go there first. Several frames on one chest allow several items.

For fully automatic sorting by category, use the [Amethyst Sprite](04-helpers.md#amethyst-sprite).

## Crafting Terminal

A Storage Terminal with a 3×3 crafting grid (Storage Terminal + Crafter).

- After every craft the grid **refills itself** from the network.
- **Shift-click** the result to craft as many as possible.
- **×** puts everything from the grid back into the network.
- With **JEI**, the `+` button transfers a whole recipe from network and inventory into the grid.

## Wireless Terminal

Opens your storage from a distance.

1. Craft the Wireless Terminal and combine it with a **Wireless Core** in the crafting grid. Without the core it does
   nothing. The core drops from the [Amethyst Robot](09-bosses-and-end.md#amethyst-robot) and can be bought from the
   [Geode Keeper](07-companions.md#trading-with-the-geode-keeper).
2. **Sneak + right-click** your Storage Terminal with it to bind it.
3. **Right-click in the air** to open your storage.

Its range is **4 × the terminal range**. With an **Ender Upgrade** in the terminal it works everywhere, even in other
dimensions.

## Resonance (optional)

Resonance is a bonus – the network never needs it. A linked **Spark Beetle** within 8 blocks charges the terminal
(×2 in rain, ×5 during thunder). Feed the beetle redstone, glowstone or a redstone block for a boost.

While the terminal is charged:

- **+16 blocks** range,
- **double** wireless range,
- helpers work **80 % faster**, and they keep working at night (the Spark Beetle itself always sleeps at night, so charge
  up during the day).

A [Resonance Storm](08-resonance-storm.md) drains resonance quickly and halves the range while it rages.

---

[← Getting started](01-getting-started.md) · [Guide overview](README.md) · Next: [Teams and trade →](03-teams-and-trade.md)
