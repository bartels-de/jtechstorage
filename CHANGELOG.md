# Changelog

All notable changes to JTechStorage. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
All versions so far are pre-releases (0.x) for Minecraft 26.3 / Fabric.

## [0.22.0]

### Added
- **Custom sounds** for all helpers and both bosses (40 sound events, 93 synthesized variants, subtitles in English
  and German):
  - helpers have their own ambient sounds (quiet, every ~15 s, silent while resting) and work sounds, such as the
    sprite sorting, Chel hammering and giggling, Ashley sneezing, the fox digging and finding a book, and phoenix
    take-offs
  - the Amethyst Robot and the Great Amethyst Spider got new ambient, hurt, death, step and attack sounds (laser
    charge and shot, fist slam, resonance shriek, crystal web, awakening)
- **Server config** `config/jtechstorage.json` with comments: network ranges and capacities, boss health, armor,
  damage multiplier and core chance, and helper speeds and radii. Clients receive the server's values for upgrade
  tooltips and the handbook.

### Changed
- Upgrade tooltips and the handbook's upgrade page show the configured values.

## [0.21.1]

### Changed
- **Hologram tour** updated to the current mod: 20 steps (previously 15). New steps for the Crafting, Team and Trade
  Terminals, phoenix mail, the Amethyst Phoenix, the Crystal Lair with the Great Amethyst Spider, the Amethyst Fox and
  the UI settings. The phoenix, Amethyst Phoenix, spider and fox appear as holograms.
- **Storage Handbook** updated:
  - new pages: contents, terminal controls and search, interface settings
  - pages ordered by chapter (basics, teams & trade, bosses & End, helpers, phoenix, backpacks, recipes)
  - welcome, Amethyst Sprite and backpack pages describe current features
- English README for GitHub, this changelog, GitHub Actions build, `.gitignore` and `.gitattributes`.

## [0.21.0]

### Added
- **Amethyst Fox**: a crystal kitsune helper with three glowing tails and orbiting shards. About every three in-game
  days it digs up an enchanted book with a crystal enchantment and stores it in the network. Feeding glow berries or
  sweet berries speeds it up.
- Five **crystal enchantments**, only obtainable from the Amethyst Fox:
  - Double Jump (boots, I–II)
  - Sprint (leggings, I–III)
  - Recall (Storage Key: return to the terminal once per day)
  - Careful Touch (pickaxe: mines monster spawners with their mob, and budding amethyst)
  - Life Spark (sword/axe, I–III: melee hits heal you)
- Advancements "Treasure Sniffer" and "Forgotten Knowledge".
- Handbook pages for the fox and the enchantments.

## [0.20.1]

### Changed
- All mod screens reworked for readability:
  - full-size text with sufficient contrast in light and dark themes
  - button labels pick black or white automatically
  - long texts are shortened with a tooltip
  - wider Team, Trade and Amethyst Phoenix screens
  - long handbook pages are split automatically
- Amethyst Phoenix redesign: S-shaped neck, crystal crown and halo, layered wings with flight feathers, five tail feathers.

### Fixed
- Right-clicking the Amethyst Phoenix with a chest or the Route Feather mounted the player.

## [0.20.0]

### Added
- **Amethyst Phoenix**: an evolution of the phoenix (right-click with an Amethyst Obsidian Upgrade).
  - shuttles between two stops set with the new **Route Feather** and rests one night at each stop
  - two seats and up to four chests; more cargo makes it slower
  - sit mode stops all movement

### Changed
- The phoenix flies calmer and no longer jitters around its nest.

### Removed
- Crystal Paladin. Existing paladins turn back into Peter Chel Parker and drop their dragon egg; old items convert
  automatically.

## [0.19.1]

### Added
- Full-set bonus for Amethyst Obsidian armor: +2 hearts, Fire Resistance, Haste I and Resonance Thorns.

### Changed
- New high-detail textures for all Amethyst Obsidian items and the armor model.

## [0.19.0]

### Added
- **End update**: Crystal Lair structure in the outer End, Lair Compass, Crystal Altar and Crystal Cocoon.
- **Great Amethyst Spider** boss (600 HP, six attacks, enrage phase). Drops exactly one Amethyst Obsidian Upgrade per kill.
- **Amethyst Obsidian** armor and tools (smithing upgrade from netherite, better than netherite) and Crystal Obsidian.

## [0.18.0]

### Added
- **Amethyst Core**: Amethyst Robot loot, required for every helper recipe.

### Changed
- Placed backpacks are now real block models (no longer visible through walls).
- New Trade Terminal interface.
- Amethyst Robot redesign.
- Missing translations for the Team and Trade terminals added.

## [0.17.0]

### Added
- UI settings: light/dark theme, accent color, terminal height, item count size.
- Backpacks can be placed as blocks, and the Amethyst Sprite empties them into the network.

### Changed
- The Amethyst Robot replaces the dinosaur boss; the Amethyst Draught became the Resonance Igniter.

## [0.16.1]

### Fixed
- Compiler warnings cleaned up.

## [0.16.0]

### Added
- **Phoenix mail**: Phoenix Nest (mail address, parcels from storage, standing orders, inbox) and the Phoenix courier.
- "Mail" team channel.

## [0.15.0]

### Added
- **Team Terminal** with roles (Guest, Member, Builder, Manager, Owner), team color and event channels.
- **Trade Terminal**: six item-for-item offers sold directly from storage.
- Crystal Paladin (removed again in 0.20.0).

### Changed
- The friends panel in the Storage Terminal is replaced by teams. Former friends become Builders.

## [0.14.1]

### Changed
- Backpack model and icons redesigned.

## [0.14.0]

### Added
- First boss, a giganotosaurus summoned with the Amethyst Draught, dropping the Wireless Core and iron upgrades.
- The Wireless Terminal now requires a Wireless Core.
- Hologram tour by Jbartels on first join.

## [0.13.0]

### Added
- **Backpacks** in five tiers (9–54 slots): wearable, dyeable, upgradeable with contents kept, auto-pickup.

## [0.12.0]

### Added
- Ashley.
- Crystal throw and shockwave for Peter Chel Parker.

### Changed
- The Collector Moth became the **Watch Moth** (night guard). The Amethyst Sprite now collects items.
- Helpers rest at night unless the terminal has resonance.
- Peter can now be hurt and heals by kicking Ashley.
- All recipes are noticeably more expensive, in line with vanilla.

## [0.11.0]

### Added
- Order menu for Chel and diamond jokes.
- Helper cosmetics (dyes, hats).
- **Peter Chel Parker** (Chel + nether star).

## [0.10.1]

### Changed
- Amethyst Sprite and moth redesigned in the shared amethyst style.

## [0.10.0]

### Added
- **Chel**, the chaotic auto-crafter.
- Network name shown on all helpers.

### Changed
- Griffin and beetle redesigned.
- The sprite also tidies chests from the inside; 15 sorting categories.
- All helpers move slower.

## [0.9.0]

### Added
- **Storage Griffin** (mount with chest).
- **Spark Beetle** and optional **resonance** bonus.

## [0.8.0]

### Added
- Fixed sorting categories per chest.
- Access sync for the terminal.

### Removed
- Storage turtle.

## [0.7.0]

### Changed
- Project renamed from "Storage Net" to **JTechStorage** (mod id `jtechstorage`).

### Added
- Collector Moth, helper base class, storage turtle.

## [0.2.0] – [0.6.0] (Storage Net)

### Added
- Storage Terminal, network of linked containers, range and capacity upgrades.
- Storage Key, Wireless Terminal, friends and access modes.
- Crafting Terminal, JEI integration, Storage Handbook and advancements.
- Amethyst Sprite (category sorting).

