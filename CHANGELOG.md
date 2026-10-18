# Changelog

All notable changes to JTechStorage. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
All versions so far are pre-releases (0.x) for Minecraft 26.3 / Fabric.

## [0.24.0]

### Added
- **Resonance Storm:** crystal creatures attack a Storage Terminal and disturb the network.
  - started with `/jtechstorage storm start` (game masters) at the nearest Storage Terminal within 64 blocks;
    `/jtechstorage storm stop` ends it. A proper trigger will follow later
  - thunderstorm, purple-red sparks and a boss bar with the wave progress for players within 96 blocks
  - while it rages the network is disturbed: half terminal range, helpers work at half speed and resonance drains
    five times faster; opening the terminal shows a warning
  - three waves of Crystal Shardlings spawn 24–40 blocks around the terminal and march on it; the last wave brings the
    Storm Herald. Defeating the herald ends the storm and the network is back to normal at once
  - Peter Chel Parker, Cheldrogan and the Watch Moths fight the storm creatures on their own
- **Crystal Shardling:** small, fast crawler of corrupted amethyst with glowing cracks, back shards, four glowing eyes
  and a jaw full of splinter teeth; pounces on players, Peter and iron golems and shatters into shards when it dies.
- **Storm Herald:** a hovering mini-boss in a tattered storm robe with a cracked crystal crown, a storm core and a
  tuning-fork staff; keeps its distance, hurls resonance bolts and summons new shardlings. Drops amethyst, amethyst
  blocks and sometimes an Amethyst Core.
- **Signal Lantern:** lights up and emits a redstone signal (strength 15) while an Amethyst Phoenix rests at a stop
  within 8 blocks. Recipe: gold nugget, polished deepslate, amethyst shard and redstone.
- Sounds for the storm (gathering, waves, end), the shardling and the herald; guide book pages for the storm, the
  creatures and the lantern.
- **Crystal Wolf:** feed your tamed wolf an Amethyst Core and it becomes a Crystal Wolf, a helper that lives in your base.
  - dark violet fur with glowing amethyst war paint, a thick ruff, crystal shoulder plates, a crest of five crystals,
    crystal claws and a tail with a crystal cluster
  - once linked it guards 20 blocks around the terminal: attacks monsters, herds stray farm animals back and brings
    monster loot (bones, string, gunpowder, pearls …) into storage
  - fights harder in a pack (+1 damage per other Crystal Wolf nearby, up to +3) and with a Moon Wolf nearby (×1.5)
  - sleeps in a Wolf Basket at night but wakes up when monsters come close or a Resonance Storm rages
  - bone: follow you or guard the current spot; meat: heal. It never dies – at low health it retreats exhausted and
    recovers
- **Moon Wolf:** the alpha. On full-moon nights it sometimes appears wild on a hill near a base and disappears at dawn –
  but only once you have at least 5 Crystal Wolves of your own around your base.
  - win it over with the new **Amethyst Bone** (bone, amethyst shards, glowstone dust)
  - silver-white with a mane of moonlight strands, a floating crescent crown, icy glowing runes and three orbiting moon
    shards
  - leads the pack, scents the nearest structure with a compass (village, mineshaft, ruined portal, treasure, trial
    chambers, ancient city) and howls in a Resonance Storm: storm creatures nearby freeze, the pack gets strength and
    speed
- **Helper Home:** the **Helper Bed** (small helpers) and the **Wolf Basket** (wolves). At night every helper picks its own
  free sleeping spot of its kind within 16 blocks of the terminal, reserves it so nobody else takes it, walks there and
  sleeps in it (it hops in if the way is blocked). If there is no free bed left, it simply lies down on the ground.
  The griffin, the phoenixes and Cheldrogan keep their own places.
- Sounds for both wolves (huffing, growling, howls, yelps, footsteps, sleep), guide book pages, advancements
  "Pack Leader", "Howling at the Moon" and "Good Night".

### Changed
- **Cheldrogan redone as a crystal serpent dragon.** He is now a long, wingless dragon that always hovers and slithers
  through the air: nine body segments with amethyst scales, golden belly plates and crystal spines, a glowing mane,
  branching crystal antlers, whiskers and beard, a crystal flame at the tail tip and a floating resonance pearl. At night
  he coils up on the ground. New model, texture, item icon and sounds; no fall damage and no footsteps.
- New attacks (ridden and when guarding):
  - **Tail sweep** (left click): he spins once around himself and hurls everything within 5.5 blocks away.
  - **Crystal breath** (R): now a beam that follows the rider's gaze. Where it hits, **Resonance Crystals** grow from the
    ground and encase monsters for about three seconds. The crystals vanish on their own and drop nothing.
  - **Crystal shard rain** (V, new key): he rears up and amethyst shards rain down on the aimed spot.
- When guarding he picks the attack himself: shard rain on groups or distant monsters, tail sweep up close, crystal
  breath at range or against targets he cannot reach. The claw strike is gone.
- Smaller hitbox (2.2 × 2.6 blocks) matching the new shape; rider sits on his back behind the neck.
- New config option `cheldroganRainCooldownSeconds` (default 10) for the ridden shard rain.
- **Advancements reorganized as a guide:** storage → helpers → storm, phoenix, team, bosses and the End, with
  descriptions that explain the next step. New: "Weathered the Storm", "Eye of the Storm" and "Beacon Fire".
- **Mob rework, wave 1:** the Amethyst Sprite, Watch Moth, Spark Beetle and Ashley have new models and textures at
  double detail density, new animations and a sleep pose.
  - Amethyst Sprite: deepslate head with large glowing eyes, gold circlet and crystal crown, embroidered robe with core
    crystal and satchel, crystal hands, two pairs of glass shard wings, crystal pendulum tail; sits down to sleep
  - Watch Moth: furry collar, compound eyes, feathered antennae, crystal lantern tail, eye-spot forewings with fringes,
    six legs; lands and folds its wings to sleep
  - Spark Beetle: copper rhinoceros beetle with redstone rivets, amethyst-inlaid elytra that open while charging to
    reveal glowing hind wings, lightning-rod horn, tripod walking gait; tucks its legs in to sleep
  - Ashley: fluffy soot ball with glowing embers, big eyes, an amethyst hair clip (dyeable) and rising smoke puffs;
    waddles, sneezes and slumps down to sleep
  - work, alarm and sneeze animations play when the action happens
- **Mob rework, wave 2:** the Storage Griffin, Chel, Amethyst Fox and Phoenix got the same treatment.
  - Storage Griffin: eagle front with scaled amethyst plumage, feather collar, hooked beak and crystal crest, lion
    hindquarters with glowing veins and a crystal tail tuft, wings that lie flat on the ground and spread wide in
    flight, leather saddle with two saddlebags; lies down with its head tucked in to sleep
  - Chel: huge head with a crooked look behind a gold monocle, gap-toothed grin, crystal hair and a mini crafting table
    hat, leather bib apron, tool belt, gloves, buckled boots and an amethyst hammer he really swings; dozes off sitting
  - Amethyst Fox: crystal kitsune with collar ruff, back crystals, gold headband, crystal-tipped ears, three bushy
    three-part tails and orbiting crystal shards; digs with its front paws, curls up under its tails to sleep
  - Phoenix: flame-gradient plumage with a glowing chest, flame crest with amethyst tips, long tail feathers with eye
    spots and two flame streamers; sleeps with its head in its feathers when sitting at night (visual only)
- **New and reworked sounds** for these four helpers: layered crystal chimes with chorus and reverb, new sleep sounds
  (breathing, snoring, humming), beetle and Ashley footsteps.
- **Wave 2 sounds:** reworked calls for the griffin, Chel, fox and phoenix, plus wing beats for the griffin and
  phoenix, griffin footsteps and sleep sounds for all four.
- **Mob rework, wave 3:** Peter Chel Parker, the Amethyst Phoenix and the Jbartels hologram got new models and
  textures at double detail density.
  - Peter Chel Parker: deepslate plate armour with gold trim and rivets, visored helmet with a T-slit (glowing eyes
    behind it), gold circlet, crystal crest and helmet fins, breastplate with a glowing core crystal, cape with the
    hammer crest, crystal war hammer with gold bands and a new round shield; at night, when idle, he dozes leaning on
    his hammer with the visor closed (visual only, he stays on guard)
  - Amethyst Phoenix: glowing crystal breast, feather collar, gold hooked beak, diadem with jewel, five crystal crown
    feathers and a spinning halo, wings with flame tips that fold along the body, five long tail feathers with eye
    spots, two leather saddles with a girth strap and up to four saddle chests; sits with its legs tucked in and sleeps
    at night on the ground with its head in its back feathers (passenger seats move down while it sits or sleeps)
  - Jbartels hologram: amethyst hoodie with hood, drawstrings, zip and crystal emblem, coat tail, holo glasses, headset
    with microphone, jeans and sneakers, holo tablet and a spinning projection ring; explains with gestures, types on
    the tablet, waves, and goes into standby when nobody is near
- **Wave 3 sounds:** Peter hums through his helmet, armoured footsteps and a dozing snore; new calls, wing beats and a
  sleep sound for the Amethyst Phoenix; the Jbartels hologram has its own appear, talk and vanish sounds.
- **Mob rework, wave 4 (bosses):** the Amethyst Robot and the Great Amethyst Spider have new models and textures at
  double detail density. This completes the mob rework.
  - Amethyst Robot: steel armour with rivets, gold edges and cooling fins, chest reactor behind a grille, glowing eye
    slit, jaw grille and cheek vents, crystal horns, laser cannon with cooling rings and a glowing muzzle, crystal fist
    with knuckle crystals, energy cell, pylons, glowing thrusters, energy ring, cape and new knee joints; without a
    target it powers down (head and arms sink, pylons fold in, an eye shutter closes)
  - Great Amethyst Spider: obsidian armour with glowing amethyst veins, segmented abdomen with an hourglass mark,
    crystal clusters and glowing spinnerets, eight glowing eyes, chelicerae with crystal fangs, spiny legs with crystal
    knees and claws; without a target it lurks low with its legs drawn in and its eyes half closed
  - the standby poses are visual only; fights work as before
- **Wave 4 sounds:** all boss sounds reworked (servo motors, hydraulics, beeps, layered shrieks, crystal debris); new
  standby hum for the robot and lurking breath for the spider.

## [0.23.0]

### Added
- **Cheldrogan**, Peter Chel Parker's dragon form: a four-block amethyst dragon on two legs.
  - evolve Peter with the new **Dragon Heart** (dragon's breath, Crystal Obsidian, amethyst blocks, nether star,
    Amethyst Obsidian Upgrade)
  - rideable: walks on the ground, flies after taking off; left click = claw strike, R = crystal breath (rebindable)
  - without a rider he guards the base, breathes crystal fire at out-of-reach monsters and heals at Ashley
  - turns back into Peter at 0 health
  - detailed model: horned head with crystal crown, S-shaped neck, gold chest plate with glowing core, bat wings with
    glowing veins (folded on the ground, spread in flight) and a tail with a crystal morning star
  - own sounds (growl, roar, claws, crystal breath, wing beats, transformation), advancement "Dragon Rider"
  - config: `cheldroganHealth`, `cheldroganDamage`, `cheldroganBreathCooldownSeconds`
- **Phoenix Stops and the Flight Plan** for the Amethyst Phoenix:
  - up to four named stops, each with its own rest time (30 seconds, 5 minutes or one night)
  - the Flight Plan collects stops by clicking them; right-click in the air opens a window to reorder or remove them
  - the phoenix visits the stops in order and starts over
- **Delivery orders** for Phoenix Stops: chests or barrels right next to a stop are filled or emptied when the Amethyst
  Phoenix lands.
  - modes: Off, Unload, Load, Both (unload everything not in the filters, then load the filter items)
  - up to 9 filters (by item), each with an amount per visit (scroll wheel; Shift ×16, Ctrl ×64; right-click = all)
  - the stop window shows the last visit with the amounts moved; passengers get a delivery message
- **Storage statistics** in the Storage Terminal (button right of the hotbar):
  - fill level, linked chests, item kinds and total items
  - history of the last 14 in-game days (one snapshot per day) plus the current value
  - item count per category, the most common items, and the biggest gains and losses since the start of the day
- **Category tabs** in the Storage Terminal: 15 tabs (plus "all") filter the list by sorting category and show how many
  items each category holds.
- **One book per topic:** Recipe Book, Bestiary (3D models and health of every creature), Item Lexicon, Crystal
  Chronicle (legend, bosses, enchantments, evolutions, roadmap) and Chel's Joke Book. All books have clickable contents
  and chapter pages.
- **Chel's Joke Book:** 55 new jokes (75 in total). Every joke you hear is recorded, Chel prefers jokes you don't know
  yet, and your first joke book is a gift from Chel. Advancement "Laughing Stock" for all 75.
- **Tooltips** for helpers, terminals and tools: a short description, plus the controls while holding Shift.
- **Own creative tab** "JTechStorage" with every mod item (armor and tools also stay in the Combat tab).
- **JEI info pages** for items without a recipe: Amethyst Core, Wireless Core, iron upgrades, Amethyst Obsidian Upgrade,
  Peter Chel Parker, Cheldrogan, Amethyst Phoenix and the Storage Handbook.

### Changed
- **Unified helper controls:** sneak + right-click with an empty hand now packs up every helper, and it has to be done
  twice within 3 seconds (protects against accidental packing).
  - Storage Griffin: the chest opens with the inventory key while riding (no longer sneak + right-click).
  - Amethyst Phoenix: the menu opens with the inventory key while riding; packing it up keeps chests, cargo and route
    instead of requiring empty chests.
- The Storage Handbook now covers basics and controls (new page "Helper Controls"); recipes moved to the Recipe Book.
- The search field of the Storage Terminal is focused right away, so you can start typing immediately (Esc closes the
  terminal).
- Right-clicking a Phoenix with an empty hand opens the mail menu of its nest.

### Removed
- Route Feather. Old feathers turn into an empty Flight Plan, and existing routes are kept as stops "A" and "B".

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

