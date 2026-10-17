<p align="center">
  <img src="src/main/resources/assets/jtechstorage/icon.png" width="128" alt="JTechStorage icon">
</p>

<h1 align="center">JTechStorage</h1>

<p align="center">
  A vanilla-friendly storage network for <b>Minecraft 26.3 (Fabric)</b>, built from deepslate, amethyst and your own chests.
</p>

<p align="center">
  <img alt="Minecraft 26.3" src="https://img.shields.io/badge/Minecraft-26.3-62B47A">
  <img alt="Fabric" src="https://img.shields.io/badge/Loader-Fabric-DBD0B4">
  <img alt="Java 25" src="https://img.shields.io/badge/Java-25-E76F00">
  <img alt="License MIT" src="https://img.shields.io/badge/License-MIT-blue">
</p>

---

JTechStorage links your existing chests, barrels and shulker boxes into one searchable network, with no new storage blocks
and no power required. Around that core it adds small amethyst helpers that sort, guard, craft and carry for you, team and
trade terminals, phoenix mail, two bosses and an End update with gear above netherite.

**Highlights**

- 📦 **One terminal for all your chests.** Exact counts, search, sorting and filters. A crafting terminal refills its grid
  from the network, and a wireless terminal opens it from anywhere.
- 🧚 **Amethyst helpers.** A sprite that sorts chests by category, a watch moth, a griffin mount, a chaotic auto-crafter
  and more.
- 👥 **Teams and trade.** Roles from guest to owner, team colors, event channels and a small shop that sells straight
  from storage.
- 🔥 **Phoenix mail and travel.** Send parcels to a friend's storage. Evolve the phoenix into a rideable amethyst phoenix
  that shuttles players and cargo between two stops.
- ⚔️ **Bosses and End content.** The Amethyst Robot, the Crystal Lair in the outer End with the Great Amethyst Spider,
  and Amethyst Obsidian gear with a set bonus.
- 🦊 **Crystal enchantments.** Five new enchantments that only the Amethyst Fox can dig up.
- 🎨 **Readable, themeable UI.** Light and dark themes, accent colors and adjustable terminal size, in English and German.
- 🔊 **Own sounds and a server config.** Synthesized crystal sounds for every creature, and a commented config file for
  ranges, bosses and helper speeds.

## Installation

1. Install [Fabric Loader](https://fabricmc.net/use/) **0.19.5+** for Minecraft **26.3**.
2. Put [Fabric API](https://modrinth.com/mod/fabric-api) and `jtechstorage-<version>.jar` into your `mods` folder.
3. Optional: [JEI](https://modrinth.com/mod/jei) for recipe lookup and one-click crafting transfer.

Java **25** is required. The mod has to be installed on both the server and the client.

## Getting started

1. Place a **Storage Terminal**. It belongs to you.
2. **Sneak + right-click** the terminal with a **Storage Key** to bind the key.
3. **Sneak + right-click** a chest, barrel or shulker box with the key to add it to the network (again to remove it).
   Double chests count as one. While you hold the key, linked containers sparkle.
4. Open the terminal. You'll see every item in every linked container.

On first join you get the **Storage Handbook** (all recipes and guides). A short hologram tour by Jbartels also explains
the mod step by step. Sneak + right-click with the handbook restarts the tour.

## Features

### Storage network

| Upgrade tier | Range (blocks) | Capacity (containers) |
|---|---:|---:|
| none | 20 | 8 |
| Iron\* | 32 | 16 |
| Gold | 48 | 32 |
| Diamond | 64 | 64 |
| Netherite | 96 | 128 |
| Ender | 128 | – |

\* Iron upgrades are boss loot only. Netherite upgrades are made at the smithing table. The **Ender Upgrade** makes the
wireless terminal work without a range limit and across dimensions.

- **Terminal controls:** left click takes a stack, right click takes half, shift-click moves it to your inventory, and
  clicking with an item stores it. Search supports names and enchantments (`sharpness v`), `$enchantment`, `@mod` and
  `#tag`.
- **Sorting rules:** an item frame with an item on a linked chest makes that chest accept only that item, and the chest
  is preferred for it.
- **Crafting Terminal:** a 3×3 grid that refills from the network after every craft. Shift-click crafts as many as
  possible. With JEI, the `+` button transfers recipes from network and inventory.
- **Wireless Terminal:** needs a **Wireless Core** (boss loot). Bind it to a terminal, then right-click in the air.
  Range is 4 × terminal range.
- **Resonance (optional):** a linked Spark Beetle charges the terminal for bonus range, wireless range and helper speed.
  The network always works without it.

### Teams and trade

- **Team Terminal:** team name and color (shown on all helpers), access mode (private, team, public), member roles and
  event channels (team, trade, guard, storage, mail).

  | Role | Permissions |
  |---|---|
  | Guest | view contents |
  | Member | + take and store items, craft, wireless |
  | Builder | + link chests and helpers, use helpers |
  | Manager | + upgrades, team settings, trade terminal, manage members up to Builder |
  | Owner | everything, appoints managers, sets access |

- **Trade Terminal:** six item-for-item offers. Goods come straight from storage, and payment is stored there, even while
  you're offline (the base must be loaded).

### Helpers

All helpers are invulnerable and persistent. Link them with a bound Storage Key. Sneak + right-click with an empty hand
packs them back into an item. Every helper recipe needs an **Amethyst Core**, which drops from the Amethyst Robot.

| Helper | What it does |
|---|---|
| **Amethyst Sprite** | Gives every chest a fixed category (15 categories) and moves stacks there. Picks up loose items around the terminal, tidies chests from the inside and empties placed backpacks. |
| **Watch Moth** | Night guard. Makes monsters within 24 blocks glow, rings an alarm and reports targets to Peter. |
| **Storage Griffin** | Flying mount with a 27-slot chest. It unloads into the network when it lands near the terminal. |
| **Spark Beetle** | Charges the terminal with resonance. Feed it redstone or glowstone for a boost. |
| **Chel** | Chaotic auto-crafter. Keeps up to 8 items in stock using crafting recipes, and tells a joke for a diamond. |
| **Peter Chel Parker** | Chel fed a nether star. Base protector who fights every monster within 16 blocks. |
| **Ashley** | A grubby ash creature. Peter heals by kicking her, which she does not appreciate. |
| **Phoenix** | Mail carrier that lives in a Phoenix Nest (see below). |
| **Amethyst Phoenix** | Large travel and cargo phoenix (see below). |
| **Amethyst Fox** | Digs up books with crystal enchantments (see below). |

Helpers can be dyed and wear hats (pumpkin, flowers, cake, candle and more; extendable via the
`jtechstorage:helper_hats` item tag). At night they rest unless the terminal has resonance.

### Phoenix mail

Place a **Phoenix Nest** and link it to your storage. It becomes your mail address. Put a **Phoenix** nearby and link
it, and it moves into the nest. In the nest you choose a destination, put up to nine items in the parcel slots and send.
The phoenix takes the items from your storage and they arrive in the recipient's storage, or in the nest inbox if storage
is full. Unloaded destinations simply wait, so nothing is lost. Daily standing orders and an accept list
(everyone / team / closed) are supported.

### Amethyst Phoenix

Right-click a phoenix with an **Amethyst Obsidian Upgrade** to evolve it into a large, majestic Amethyst Phoenix.

- Use a **Route Feather** to set stop A and stop B, then right-click the phoenix with it.
- It shuttles between the stops, rests one whole night at each and departs at sunrise. It keeps chunks along its path
  loaded.
- **Two seats.** The player in front can press jump to depart early.
- **Up to four chests** (27 slots each). More chests and more cargo make it slower.
- Sit mode stops all movement. Sneak + right-click opens its menu.

### Bosses and the End

- **Amethyst Robot:** summoned with a **Resonance Igniter**. A huge mech with crystal fist, laser cannon, stomp and dash
  attacks, and an overload phase below 50 %. It always drops iron upgrades and has a 30 % chance to drop an Amethyst Core
  or Wireless Core.
- **Crystal Lair:** a floating arena in the outer End. Find it with the **Lair Compass**. Your first visit awakens the
  **Great Amethyst Spider** (600 HP) with poison bites, leaps, crystal webs, spike lines, a resonance shriek and
  spiderlings. Summon it again at the altar with a **Crystal Cocoon**. Each kill drops exactly **one Amethyst Obsidian
  Upgrade**.

### Amethyst Obsidian gear

Smithing table: Amethyst Obsidian Upgrade + netherite piece + **Crystal Obsidian**. One upgrade per piece; enchantments
are kept.

| | Netherite | Amethyst Obsidian |
|---|---:|---:|
| Armor (helmet / chest / legs / boots) | 3 / 8 / 6 / 3 | 4 / 9 / 7 / 4 |
| Toughness / knockback resistance | 3 / 0.1 | 4 / 0.15 |
| Tool durability / speed | 2031 / 9 | 2700 / 11 |
| Sword damage | 8 | 9 |
| Enchantability | 15 | 18 |

**Full set bonus:** +2 hearts, Fire Resistance and Haste I. Melee attackers take 3 + 25 % of the damage back and are
slowed (**Resonance Thorns**).

### Amethyst Fox and crystal enchantments

The **Amethyst Fox** is a crystal kitsune with three glowing tails. Link it to your storage. About every three in-game
days it digs in dirt, sand or gravel during the day and finds an **enchanted book** with a crystal enchantment, which goes
straight into your storage. Glow berries (−6 h) and sweet berries (−3 h) speed it up, up to four times per day.

These enchantments are not available from the enchanting table, villagers or loot:

| Enchantment | Applies to | Levels | Effect |
|---|---|---|---|
| Double Jump | boots | I–II | One extra mid-air jump per level |
| Sprint | leggings | I–III | +12 % speed per level while sprinting |
| Recall | Storage Key | I | Right-click in the air to return to your terminal once per day |
| Careful Touch | pickaxe | I | Mines monster spawners (the mob is kept) and budding amethyst |
| Life Spark | sword / axe | I–III | Melee hits heal 10 % of the damage per level (max. 2 hearts) |

### Backpacks

| Tier | Slots | Recipe |
|---|---:|---|
| Leather | 9 | leather, string, chest |
| Iron | 18 | leather backpack + iron block |
| Gold | 27 | iron backpack + gold block |
| Diamond | 36 | gold backpack + diamond block |
| Netherite | 54 | smithing table, fireproof |

Right-click to open, wear it in the chest slot, dye it, and upgrade it without losing the contents. Sneak + right-click on
the ground to place it as a block (hopper-compatible). Picked-up items go into backpacks that already contain them.

### Interface

The gear button in the terminal opens UI settings, which apply to all screens of the mod and are saved in
`config/jtechstorage-client.json`:

- light or dark theme
- accent color
- terminal height (4–10 rows or auto)
- item count size

The mod also adds its own advancement tab and JEI support.

### Configuration

On first start the mod creates `config/jtechstorage.json` (JSON with comments). It controls:

- **Network:** range and capacity for every upgrade tier, wireless range factor, resonance bonuses.
- **Bosses:** health, armor and a damage multiplier for the Amethyst Robot and the Great Amethyst Spider, plus the
  robot's core drop chance.
- **Helpers:** the Amethyst Fox's dig interval and feeding limit, Chel's crafting speed, the Amethyst Sprite's pause
  and radii, watch and guard radii, and phoenix speeds.

Changes apply after a restart. Delete a line (or the whole file) to restore its default. On a server, the server's
file counts; clients receive its values for tooltips and the handbook.

### Sounds

All helpers and both bosses have their own synthesized sounds in the mod's crystal style: chimes, hums, chirps and
mechanical noises, with subtitles in English and German.

## Languages

English and German.

## Building from source

Requirements: **JDK 25**.

```bash
./gradlew build        # jar in build/libs/
./gradlew runClient    # test client (JEI included)
```

In IntelliJ IDEA, import the folder as a Gradle project and use the `runClient` or `build` tasks.

The project uses Fabric Loom with Mojang mappings and split client/common source sets:

```
src/main/      common code, data (recipes, advancements, enchantments, worldgen)
src/client/    rendering, models, screens, JEI plugin
```

## Changelog

See [CHANGELOG.md](CHANGELOG.md).

## License

[MIT](LICENSE) © 2026 Jannik Bartels
