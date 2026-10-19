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
  that flies players and cargo along a route of up to four named stops.
- ⚔️ **Bosses and End content.** The Amethyst Robot, the Crystal Lair in the outer End with the Great Amethyst Spider,
  and Amethyst Obsidian gear with a set bonus.
- 🦊 **Crystal enchantments.** Five new enchantments that only the Amethyst Fox can dig up.
- 📚 **A library of books.** Handbook, recipe book, bestiary with 3D models, item lexicon, crystal chronicle and
  Chel's joke book with 75 collectible jokes.
- 🎨 **Readable, themeable UI.** Light and dark themes, accent colors and adjustable terminal size, in English and German.
- 🔊 **Own sounds and a server config.** Synthesized crystal sounds for every creature, and a commented config file for
  ranges, bosses and helper speeds.

## 📖 Guide

The full guide lives in [`docs/guide`](docs/guide/README.md): every block, item, helper, companion and boss, all
recipes with pictures, every key and command and every config option – one page per topic.

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

On first join you get the **Storage Handbook** (basics and controls). Every other topic has its own book, see
[Books](#books). A short hologram tour by Jbartels also explains
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
- **Statistics:** the chart button right of the hotbar shows the fill level, a 14-day history, categories, the most
  common items and today's biggest gains and losses.
- **Category tabs:** the tabs on the left of the terminal filter the list by the 15 sorting categories. Click the active
  tab again (or right-click) to show everything.
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

All helpers are persistent and share the same controls:

| Action | Effect |
|---|---|
| Click with a bound Storage Key | link to your storage |
| Right-click | main action (status, menu or mount) |
| Sneak + right-click twice | pack up into an item (name, color and contents are kept) |
| Inventory key while riding | chest (Storage Griffin) or menu (Amethyst Phoenix) |
| Dye / hat / shears | recolor / put on a hat / reset |

Hover over any mod item and hold **Shift** to see its controls. Every helper recipe needs an **Amethyst Core**, which
drops from the Amethyst Robot.

| Helper | What it does |
|---|---|
| **Amethyst Sprite** | Gives every chest a fixed category (15 categories) and moves stacks there. Picks up loose items around the terminal, tidies chests from the inside and empties placed backpacks. |
| **Watch Moth** | Night guard. Makes monsters within 24 blocks glow, rings an alarm and reports targets to Peter. |
| **Storage Griffin** | Flying mount with a 27-slot chest (inventory key while riding). It unloads into the network when it lands near the terminal. |
| **Spark Beetle** | Charges the terminal with resonance. Feed it redstone or glowstone for a boost. |
| **Chel** | Chaotic auto-crafter. Keeps up to 8 items in stock using crafting recipes, and tells a joke for a diamond. |
| **Peter Chel Parker** | Chel fed a nether star. Base protector who fights every monster within 16 blocks. |
| **Cheldrogan** | Peter fed a Dragon Heart. A rideable, hovering crystal serpent dragon (see below). |
| **Ashley** | A grubby ash creature. Peter heals by kicking her, which she does not appreciate. |
| **Phoenix** | Mail carrier that lives in a Phoenix Nest (see below). |
| **Amethyst Phoenix** | Large travel and cargo phoenix (see below). |
| **Amethyst Fox** | Digs up books with crystal enchantments (see below). |
| **Crystal Wolf** | A tamed wolf fed an Amethyst Core. Guards the base, herds animals, brings monster loot into storage; stronger in a pack. |
| **Moon Wolf** | The alpha. Appears at full moon, won over with an Amethyst Bone; leads the pack, scents structures, howls in the storm. |

Helpers can be dyed and wear hats (pumpkin, flowers, cake, candle and more; extendable via the
`jtechstorage:helper_hats` item tag). At night they rest unless the terminal has resonance.
Helpers sleep in a **Helper Bed** (wolves in a **Wolf Basket**) within 16 blocks of the terminal. Each one reserves its own bed; if none is free, it lies down on the ground. Each helper has its own sleep pose; Peter and the Amethyst Phoenix only doze and Cheldrogan coils up at night (visual only) and stay
on duty.

### Cheldrogan

Craft a **Dragon Heart** (dragon's breath, Crystal Obsidian, amethyst blocks, a nether star and an Amethyst Obsidian
Upgrade) and right-click Peter Chel Parker with it. He rises as **Cheldrogan**, a hovering crystal serpent dragon: a
long, wingless body of amethyst scales with golden belly plates and crystal spines, a glowing mane, branching crystal
antlers, long whiskers, a crystal flame at the tail tip and a resonance pearl floating in front of him. He always hovers
and slithers through the air; at night he coils up on the ground.

- **Riding:** right-click to mount. Press jump to take off, then he flies where you look and jump rises.
  - **Left click** = **tail sweep**: he spins once around himself and his tail hurls everything nearby away.
  - **R** = **crystal breath**: a beam that follows your gaze. Where it hits, resonance crystals grow from the ground and
    encase monsters for a few seconds (the crystals vanish on their own and drop nothing).
  - **V** = **crystal shard rain**: he rears up and amethyst shards rain down on the spot you aim at.
  - Breath and shard rain have short cooldowns (configurable); both keys can be changed in the controls.
- **Guarding:** without a rider he guards the base like Peter: shard rain on groups or distant monsters, tail sweep up
  close, crystal breath at range and against targets he cannot reach. He heals at Ashley. If you dismount far away from
  his base, that spot becomes his new base.
- At 0 health the Dragon Heart burns out and he turns back into Peter.

### Phoenix mail

Place a **Phoenix Nest** and link it to your storage. It becomes your mail address. Put a **Phoenix** nearby and link
it, and it moves into the nest. In the nest you choose a destination, put up to nine items in the parcel slots and send.
The phoenix takes the items from your storage and they arrive in the recipient's storage, or in the nest inbox if storage
is full. Unloaded destinations simply wait, so nothing is lost. Daily standing orders and an accept list
(everyone / team / closed) are supported.

### Amethyst Phoenix

Right-click a phoenix with an **Amethyst Obsidian Upgrade** to evolve it into a large, majestic Amethyst Phoenix.

- Build up to four **Phoenix Stops** and right-click each to give it a name and a rest time (30 seconds, 5 minutes or
  one night).
- Click the stops with a **Flight Plan** in the order you want. Right-click in the air to reorder or remove stops, then
  right-click the phoenix with the plan.
- It visits the stops in order, rests as long as each stop says and starts over. It keeps chunks along its path loaded.
- **Delivery orders:** put chests or barrels right next to a stop and choose *Unload*, *Load* or *Both* in the stop's
  window. Up to 9 filters (with an amount per visit) decide which items move. *Both* unloads everything that isn't in
  the filters and loads the filter items, so one stop can deliver and pick up at the same time.
- **Two seats.** The player in front can press jump to depart early.
- **Up to four chests** (27 slots each). More chests and more cargo make it slower.
- Sit mode stops all movement. The inventory key while riding opens its menu; packing it up keeps chests, cargo and route.

### Resonance Storm

Sometimes the resonance rings false. A **Resonance Storm** gathers over a Storage Terminal. It starts on its own when
you stand in the chunk of **your own terminal**, have **at least one helper** and have mined **more than 500 amethyst**
(blocks, buds and clusters, counted by the vanilla statistics) since your last storm – at most once per in-game day
(all configurable). Game masters can also use `/jtechstorage storm start`.

- the network is disturbed while it rages: half range, helpers at half speed, resonance drains fast
- three waves of **Crystal Shardlings** march on the terminal; the last wave brings the **Storm Herald**
- defeat the herald to end the storm. Peter Chel Parker, Cheldrogan and the Watch Moths fight alongside you

The **Signal Lantern** lights up and emits a redstone signal while an Amethyst Phoenix rests at a stop within 8 blocks.

### Companions

Deep inside amethyst geodes there sometimes lives a **Geode Keeper**, an old hermit with crystals on his shoulders and
a geode staff. Every geode has exactly one chance to hold one (a soft hum tells you he is near). Right-click him and he
offers you one of three companions, chosen at random. Accept or decline by clicking in chat; if you decline, he
disappears and you can try your luck in another geode.

The keeper is also a **trader** and works like a villager (shift + right-click, or a plain right-click once you have a
companion): amethyst shards for Resonance Treats, experience bottles, Amethyst Cores, Wireless Cores, budding amethyst,
a totem and more; he also buys calcite, smooth basalt, glow berries and echo shards. Every trade gives him experience –
he climbs five levels like a villager and unlocks two new random offers per level. His stock refills every 10 minutes.
Once you accept a companion from him or trade with him he settles in his geode for good; keepers now also appear for
players who already have a companion. You can only have one companion. All companions fight at your
side: the mantis at full strength, the raven with 50 % and the shroom with 25 % of its damage.

| Companion | Role |
| --- | --- |
| **Shadow Mantis** | The strongest fighter: attacks what you attack or what attacks you, and monsters near you. Stage 3 hits everything around the target. |
| **Amethyst Shroom** | Heals you with glowing spores – automatically or on key press (**H** heals, **Shift + H** switches mode), with a cooldown of 30 / 20 / 15 s. Stage 2 also heals your pets and companions, stage 3 heals all players nearby, cures poison and saves you at low health. |
| **Night Raven** | Collects items around you into its bag (9 / 18 / 27 slots) and flies them to your Storage Terminal (link it with a bound Storage Key). |

- **Follows you everywhere**, also through portals and after you respawn. Right-click or the **G** key toggles
  **follow / stay**. `/jtechstorage companion call` brings it to you.
- **Companion menu** (**K** key or shift + right-click; the raven opens its bag on shift + right-click): level, health
  and stats with a live portrait, follow / stay, call, the shroom's heal mode, the raven's bag and the **stance** –
  *aggressive* (attacks everything hostile near you), *defensive* (only defends you and itself) or *passive*.
- **Talent tree:** one talent point every 5 levels (up to 20). Each species has three branches with four talents –
  mantis *Blade / Armor / Hunt*, shroom *Healing / Protection / Poison*, raven *Bag / Combat / Flight*. Deeper talents
  need the one above and sometimes stage 2 or 3; resetting costs an Amethyst Core.
- **HUD** in the top left: name, level, health, experience, follow / stay, stance, treat time and the shroom's cooldown,
  the raven's bag or the mantis' flight stamina while riding. It can be switched off in the menu.
- **Riding at stage 3:** the mantis carries you on the ground and flies briefly while you hold Space (about 4 seconds,
  then it glides down and recharges); the grown raven carries you through the air for as long as you like.
- **Grows with you:** half of every bit of experience you collect goes to your companion. It levels like a player and
  reaches **stage 2 at level 30** and **stage 3 at level 100**, each with a new look and stronger abilities.
- It never dies. When it takes too much damage it is exhausted for a short while and recovers.
- **Resonance Treat** (honey bottle + 2 amethyst shards → 4): right-click your companion for 5 minutes of double
  experience (stacks up to 15 minutes), a minute of speed, 2 hearts and an end to its exhaustion.
- `/jtechstorage companion release confirm` releases it for good. Game masters can spawn a keeper with
  `/jtechstorage companion keeper` and give XP with `/jtechstorage companion xp <amount>` for testing.

### Bosses and the End

- **Amethyst Robot:** summoned with a **Resonance Igniter**. A huge mech with crystal fist, laser cannon, stomp and dash
  attacks, and an overload phase below 50 %. It always drops iron upgrades and has a 30 % chance to drop an Amethyst Core
  or Wireless Core. Without a target it powers down into standby.
- **Crystal Lair:** a floating arena in the outer End. Find it with the **Lair Compass**. Your first visit awakens the
  **Great Amethyst Spider** (600 HP) with poison bites, leaps, crystal webs, spike lines, a resonance shriek and
  spiderlings. Summon it again at the altar with a **Crystal Cocoon**. Each kill drops exactly **one Amethyst Obsidian
  Upgrade**. Without a target it lurks low with its eyes half closed.

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

The mod has its own creative tab and advancement tab. With JEI, items without a recipe (boss loot, evolutions) show an
info page that explains where they come from.

### Books

| Book | Recipe (shapeless) | Contents |
|---|---|---|
| **Storage Handbook** | book + amethyst shard | Basics, terminal, upgrades, teams, phoenix, backpacks, controls |
| **Recipe Book** | + crafting table | Every recipe, sorted by chapter |
| **Bestiary** | + feather | All helpers, evolutions and bosses with a 3D model and health |
| **Item Lexicon** | + item frame | Every block, tool, material and piece of gear explained |
| **Crystal Chronicle** | + ender eye | Legend, bosses, Amethyst Obsidian, crystal enchantments, evolutions, roadmap |
| **Chel's Joke Book** | + cookie | Collects every joke Chel and Peter tell you (75 in total) |

Every book has a clickable table of contents, and ☰ at the top left jumps back to it. Chel gives you your first joke
book with his first joke, and collecting all 75 jokes unlocks an advancement.

### Configuration

On first start the mod creates `config/jtechstorage.json` (JSON with comments). It controls:

- **Network:** range and capacity for every upgrade tier, wireless range factor, resonance bonuses.
- **Bosses:** health, armor and a damage multiplier for the Amethyst Robot and the Great Amethyst Spider, plus the
  robot's core drop chance.
- **Helpers:** the Amethyst Fox's dig interval and feeding limit, Chel's crafting speed, the Amethyst Sprite's pause
  and radii, watch and guard radii, and phoenix speeds.
- **Companions:** levels for stage 2 and 3, the share of collected XP that goes to the companion, the chance for a
  Geode Keeper per geode and the search radius.

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
