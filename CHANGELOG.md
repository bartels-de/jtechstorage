# Changelog

All notable changes to JTechStorage. The format follows [Keep a Changelog](https://keepachangelog.com/en/1.1.0/).
All versions so far are pre-releases (0.x) for Minecraft 26.3 / Fabric.

## [0.29.4]

### Changed
- **Terminal grid fields are now real inventory slots.** Taking and storing items uses the vanilla code, so everything
  works exactly as in a chest: clicks, Shift-click, number keys, the swap-offhand key, Q, dragging to spread items,
  double-click and creative middle-click. Each field holds up to one stack; the number still shows the total in the
  network.
- Replaces the hand-made click handling of 0.29.1–0.29.3.

## [0.29.3]

### Changed
- Terminal clicks now match vanilla chests even more closely:
  - **Double-click** an item fills the stack on your cursor, first from the network and then from your inventory.
    Before, the second click put the stack straight back.
  - **Double-click** an item in your inventory also tops up from the network.
  - **Shift + double-click** an item moves all of that kind into your inventory, until it is full.
  - **Right-click** with an item on a different item swaps them, like left-click.

## [0.29.2]

### Fixed
- Terminal: while the mouse is over the item grid, items keep their places when amounts change. Before, sorting by amount
  could move a different item under the cursor while you were taking items out (e.g. with Shift held). The list sorts
  again as soon as the mouse leaves the grid; items used up completely leave a gap until then.

## [0.29.1]

### Changed
- **The terminal handles items like a chest:**
  - Left-click with an item on a different item stores yours and picks that one up.
  - Number keys 1–9 and the swap-offhand key over an item put a stack into that slot; what was there is stored.
  - Number keys over an empty field store that hotbar slot.
  - **Q** drops one item, **Ctrl + Q** a stack.
  - Middle click in creative picks up a full stack.
- The search field stays active when the terminal opens; click into the grid to use the keys.

## [0.29.0]

### Added
- **New interface themes:** *Frost glass* and *Crystal glass* (slightly see-through, glowing accent border) and *Modern light*
  and *Modern dark* (flat, rounded pixel corners, soft shadow). The classic light and dark themes stay.
- **Effects**, each one switchable: slide-in, hover glow, border shimmer, sheen, background particles and count animation in
  the terminal. Presets *off*, *subtle* (default) and *rich*.
- New accent color **teal**.
- Tooltips in the mod's screens match the theme.

### Changed
- **Redesigned settings screen** with theme tiles, color swatches, effect switches and a live preview.
- Terminal: search field with magnifier, themed scrollbar, buttons and category tabs in the new themes.
- On light themes, amounts in slots get a dark outline so they stay readable.

## [0.28.0]

### Added
- **Amethyst Chests** in three tiers whose slots hold several stacks:
  - **Amethyst Chest** – 54 slots, 2 stacks each (chest, amethyst shards, copper).
  - **Crystal Chest** – 81 slots, 4 stacks each (Amethyst Chest, amethyst blocks, iron).
  - **Resonance Chest** – 108 slots, 8 stacks each (Crystal Chest, gold, diamonds).
- They link to the storage network like any chest. The terminal, the Amethyst Sprite, hoppers and Crystal Pushers use the
  bigger stacks.
- Right-click a placed chest with a better one to upgrade it in place; contents and name stay, the old chest comes back.
- Handbook page and guide section "Amethyst Chests".

### Changed
- The Amethyst Sprite's tidying and the Farm Monitor's "full" check respect containers with bigger stacks.

## [0.27.3]

### Changed
- The **Geode Keeper** never disappears on his own any more (declining his offer still sends him away).
- The Geode Keeper can be **killed** (40 health, no drops).
- The Geode Keeper can be moved in a **boat** or **minecart**; players and mobs still cannot shove him around.

## [0.27.2]

### Added
- Recipe: a **block of quartz** can be turned back into **4 nether quartz** in the crafting grid.

### Changed
- The **Chunk Loader** is crafted with a **block of diamond** instead of an Amethyst Core.

## [0.27.1]

### Added
- **Chunk Loader:** keeps its own chunk loaded at all times, also while you are offline. Runs on amethyst shards (1 per
  hour, amethyst block = 4), can be fed by hoppers, switches off without fuel and messages its owner. Each player may have
  25 active chunk loaders by default. Its window shows status, remaining time and the chunk borders on request.
- Config section `farm`: `chunkLoadersPerPlayer`, `chunkLoaderMinutesPerShard`.

## [0.27.0]

### Added
- **Farm tools** for your Minecraft farms (iron, gold, mob, crop farms …):
  - **Amethyst Hopper:** works like a hopper (down or sideways, 5 slots, redstone lock, comparator) but moves **8 items
    every 4 ticks** – 16 times as fast – and sucks in items in a 3 × 3 area above it.
  - **Crystal Pusher:** pushes items from the container behind it into the container in front of it, in all six
    directions (upwards too). Placed facing where you look, stopped by redstone, with a 9-slot filter (empty = all).
  - **Farm Monitor:** placed against a farm's output chest it counts what arrives – items per hour, a chart of the last
    hour, the most common items – and warns its owner when nothing arrives for 5 / 10 / 30 minutes or the chest is full
    (screen turns red, chat message). A **safe zone** (8–32 blocks) keeps **companions from attacking the farm's mobs**,
    even with special attacks. Linked with a Storage Key, the terminal lists all farms under **Statistics → Farms**.
- New guide page *Farm tools* and a handbook page.

## [0.26.4]

### Added
- **Crow Double** (Night Raven, passive): a hit that would kill you makes you burst into crows instead. A dark violet,
  translucent shadow decoy with your own skin stays for 6 seconds and draws all monsters within 16 blocks, then dissolves
  into smoke and crows. You reappear 5–8 blocks away with 4 hearts, invisible and with Resistance II for 3 seconds.
  Once every 10 minutes (6 minutes at level 100), while the raven is within 32 blocks; it does not help against the void
  or `/kill`. The menu and the HUD (✧) show when it is ready.

## [0.26.3]

### Added
- **Night Raven passive abilities** – always active, stronger with every level:
  - **Crow Scatter:** when hit, it sometimes bursts into a swarm of crows, takes no damage and reassembles a few blocks
    away (10 % at level 1 up to 35 % at level 100, at most every 3 s).
  - **Scout Crows:** every 30 s (down to 20 s) three small shadow crows circle you, make monsters within 12–22 blocks
    glow for 6 s and caw a warning when a creeper is behind you.
  - **Guardian Crow:** a crow throws itself in front of arrows and other projectiles aimed at you (15 % up to 35 %, at
    most every 2 s, while the raven is within 24 blocks).

## [0.26.2]

### Changed
- The **Night Raven** is a much stronger fighter: 70 % of the mantis' damage (was 50 %), quicker strikes (every 1.1 s
  instead of 1.6 s), more health (18 / 32 / 50 instead of 16 / 24 / 36) and light armor (0 / 2 / 4).

## [0.26.1]

### Changed
- The **Amethyst Shroom** now fights only from a distance: it keeps away from monsters and hurls spore bolts (up to 10
  blocks, 12 at stage 3) instead of headbutting them.

## [0.26.0]

Everything since 0.25.0 is about companions: a home base, special attacks, customization with visible equipment, and a
bond that grows with every meal, pat and game of fetch.

### Added
- **Your base:** the 3 × 3 chunks around a Storage Terminal you placed count as your base for companions.
  - **Shadow Mantis:** fights 50 % harder in your base. Tell it to stay while it is in your base and it **stands guard**
    instead of sitting down: it patrols around the terminal and attacks monsters within 24 blocks, even while you are
    away.
  - **Amethyst Shroom:** from stage 2, crops, stems, berry bushes, cocoa, nether wart, saplings, sugar cane, cactus and
    bamboo in your base grow about 3× as fast (stage 3: about 4–5×). At stage 3 you can **fly like in creative mode** in
    your base while your shroom is with you; leaving the base ends the flight with Slow Falling.
- **Night Raven exploration trips:** *Explore* in the companion menu sends the raven off for 4 minutes; it brings back
  3 / 4 / 5 finds by stage – copper and iron, then gold, emeralds, lapis and redstone, then diamonds, ender pearls and
  rarities – and rests for 5 minutes afterwards.
- **Special attacks:** a new *Special* branch in every talent tree with three special attacks, of which you can learn
  **only one** (from stage 2). Trigger it in combat with the **B** key (changeable) or the *Special* button in the menu;
  it targets the monster you look at. The HUD shows its cooldown as a small bar under the companion icon.
  - Shadow Mantis: **Paralyzing Sting** (leap, 150 % damage, 5 s stun, 30 s poison), **Blade Storm** (three spinning
    slashes against everything within 4 blocks), **Execution** (leap, 250 % damage, doubled below 35 % health).
  - Amethyst Shroom: **Spore Veil** (you and the shroom invisible for 60 s, monsters lose track of you, its cooldowns
    halved and running twice as fast), **Life Bloom** (10 s healing circle), **Paralysis Cloud** (damage, Slowness,
    Weakness and Poison around the target).
  - Night Raven: **Raven Swarm** (five shadow ravens circle above the target and dive at it, each hitting for 150 %),
    **Shadow Dive** (300 % damage, Blindness, knockback), **Terror Cry** (monsters within 10 blocks back off, lose their
    target and are slowed, weakened and glowing).
  - **Mastery** (+25 % power and −20 % cooldown per rank) and **Instinct** (uses the special attack on its own when you
    drop below 30 % health or 3 monsters surround you).
- **A master talent at the end of every branch** (stage 3): mantis *Bloodlust*, *Crystal Skin*, *Predator*; shroom
  *Second Life* (saves you from death once every 10 minutes), *Mushroom Skin*, *Spore Field*; raven *Treasure Hunter*,
  *Raven Call*, *Night Wings*.
- **Companion customization:** a new **Look** tab in the companion menu with a large live portrait. Everything is
  unlocked by your companion's progress:
  - **Coloring:** Amethyst, Frost (level 20), Ember (defeat 200 monsters), Jade (level 50), Gold (stage 3 and level 100),
    Soulbond (bond *Soulmate*).
  - **Swappable body parts:** mantis horns (swept / crown / sickles), raven tail (fan / long / forked), shroom cap
    (tiered / witch hat / flat) – second form from level 30, third from level 60. At stage 3 also the mantis wings
    (crystal / leather / none), the raven crest (crystal / feathers / none) and the shroom's back (backpack / cape /
    nothing).
  - **Glow color:** any color from a color bar, plus default and white (from level 10) and pink (bond *Soulmate*).
- **Companion equipment** with three visible slots – **collar**, **amulet** and **armor** – in iron (level 10), gold
  (level 25), diamond (level 45, stage 2), netherite (level 70) and amethyst obsidian (level 100, stage 3). The bonus
  always comes from the best unlocked material, the look is free to choose (or hidden):
  - collar +10 / 20 / 30 / 40 / 50 % health,
  - armor +2 / 4 / 6 / 8 / 10 armor (plus toughness from netherite),
  - amulet: mantis +5–25 % damage, shroom +10–50 % healing, raven +1–5 blocks collection radius.
- **Bond:** your companion grows closer to you – the bond never decreases. Five stages: Stranger, Familiar (100),
  Friend (300), Partner (600) and Soulmate (1000).
  - **Feed** it the food of its kind: mantis raw meat and fish, shroom berries, mushrooms and bone meal, raven seeds and
    gold nuggets. Its **favorite food of the day** (shown in the menu) gives +25 once a day, other food +3 up to five
    times a day.
  - **Pet** it with the *Pet* button in the menu: +5, once per minute, ten times a day.
  - **Play fetch:** throw a stick or an amethyst shard (Q) – it runs or flies after it and brings it back: +10, ten
    times a day.
  - **Gestures:** *Happy Hop* (Familiar), *Nuzzle* (Friend) and a gesture of its own (Partner) – mantis *Blade Salute*,
    shroom *Shroom Dance*, raven *Bow*. It shows them when it is happy; the *Look* tab plays them on click.
  - **Soulmate** unlocks the rosé **Soulbond** coloring, the pink glow color and a soft pink **aura** (can be switched
    off in the *Look* tab).
- Menu and HUD show whether the companion is in your base, on your shoulder, guarding or exploring, plus its bond stage
  and today's favorite food.
- Companions count the monsters they defeat. New config option `companions.equipment` (equipment gives stats).
- **Call** also works when the companion is in an unloaded area: it is fetched from its last known position. The same
  happens after you respawn far away or teleport.
- In-game handbook: new pages *Look and equipment* and *Bond*.

### Changed
- **Stage 1 shroom and raven sit on your shoulder** (shroom right, raven left). The shroom no longer fights at stage 1 and
  only starts with stage 2; the raven flies off to fight and returns to your shoulder afterwards.
- The stage 3 **Amethyst Shroom** carries a **leather backpack** instead of the moss cape: flap, side pocket, bedroll and
  straps with golden buckles, and a glowing healing potion sticking out at the top.
- Talent points: **one every 4 levels, up to 30** (was one every 5 levels, up to 20). Learned talents are kept. The
  talent tab shows four branches with five talents each.
- *Lucky Find* and *Swift Delivery* also improve the raven's exploration trips.
- Companions only join you on solid ground: while you fly over the void or high in the air they wait until you have
  landed. They take no fall damage and land on safe ground when rescued from the void.
- The companion menu has three tabs (Overview, Talents, Look) and is a little taller. Companion models and textures were
  rebuilt to hold all variants.

### Fixed
- Monsters kept attacking an exhausted companion, and every hit restarted its exhaustion. Exhausted companions are now
  ignored by monsters and take no further damage until they recover.
- After dying and respawning far away, the companion stayed behind.
- *Call* did not work when the companion was out of render distance or in an unloaded chunk.
- While you flew, the companion was teleported into the air next to you and fell into the void (End) or took fall damage.
- The Night Raven's wings now spread properly in flight and flap around the body.
- A raven that was away while the world was saved stayed invisible after loading.

## [0.25.0]

### Added
- **Companions:** a loyal friend from the geode who goes everywhere with you and grows with you. One companion per
  player.
  - **Geode Keeper:** an old hermit who sometimes lives inside amethyst geodes (every geode has one chance, a soft hum
    tells you he is near). He offers you one random companion; accept or decline by clicking in chat. Decline and he
    vanishes – look for another geode. Accept and he settles in his geode for good.
  - **Shadow Mantis** (attack): a pitch-black mantis with glowing slit eyes, swept-back horns, twitching mandibles and
    serrated claws. Fights what you attack, what attacks you and monsters near you (never creepers). Stage 2 adds dorsal
    spikes, a cobra hood and leather wings; stage 3 (Blade Queen) gets spiked pauldrons, a crown, amethyst scythes and
    faceted crystal wings, and its scythe strike also hits monsters around the target.
  - **Amethyst Shroom** (healing): a mushroom on two legs that heals you with glowing spores – automatically or on key
    press (**H** heals, **Shift + H** switches mode), with a cooldown of 30 / 20 / 15 s per stage. Stage 2 also heals your
    pets and companions, stage 3 heals all players nearby, cures poison and wither and saves you at low health.
  - **Night Raven** (carrying): a jet-black raven with glowing eyes and a spiky ruff. Collects items around you into its
    bag (9 / 18 / 27 slots) and flies them to your Storage Terminal once linked with a bound Storage Key. Stage 3
    (Raven Lord) wears a bone mask, horns, a crystal crest and a saddle with saddlebags.
  - **All companions fight:** the mantis at full strength (4 / 7 / 11 damage per stage, an attack every 1.3 s), the raven
    with 50 % (every 1.6 s) and the shroom with 25 % (every 2 s).
  - **Grows with you:** half of the experience you collect goes to your companion. Stage 2 at companion level 30, stage 3
    at level 100 (vanilla level curve), each with its own model.
  - **Follows you everywhere**, also through portals, into other dimensions and after respawning. Right-click or the
    **G** key (up to 48 blocks away) toggles follow / stay. It never dies – it gets exhausted and recovers, and it is
    rescued from the void.
  - **Riding at stage 3:** the mantis walks and flies briefly while you hold Space (about 4 s, then it glides down and
    recharges); the raven flies for as long as you like.
  - **Companion menu** (**K** key or shift + right-click; the raven opens its bag on shift + right-click): live portrait,
    level, XP, health, damage, armor, attack speed, mode and stance, with buttons for follow / stay, stance, call, heal
    now and heal mode (shroom), the bag (raven) and the HUD. Works from afar and across dimensions.
  - **Stances:** aggressive (default), defensive (only defends you and itself) and passive (doesn't fight).
  - **Talent tree:** one point every 5 levels (up to 20), three branches with four talents per species; deeper talents
    need the one above and partly stage 2 or 3. Resetting costs an Amethyst Core.
    - Mantis – *Blade:* Sharp Blades, Quick Strike, Precision, Whirling Scythe · *Armor:* Chitin Shell, Crystal Plates,
      Thorns, Unbreakable · *Hunt:* Swift Legs, Hunting Instinct, Leap Attack, Crystal Flight.
    - Shroom – *Healing:* Strong Spores, Fast Bloom, Wide Spores, Lifesaver · *Protection:* Tough Stalk, Spore Shield,
      Calm Aura, Purify · *Poison:* Toxic Spores, Spore Burst, Numbing Spores, Toxic Cloud.
    - Raven – *Bag:* Keen Eye, Eager Courier, Swift Delivery, Lucky Find · *Combat:* Sharp Beak, Dive, Eye Peck,
      Shadow Swarm · *Flight:* Tailwind, Feather Fall, Night Eye, Sky Lord.
  - **Companion HUD** in the top left: name, stage, level, health and XP bars, follow / stay, stance, treat time and,
    depending on the species, heal cooldown, bag fill or flight stamina while riding; shows the distance when the
    companion is far away or in another dimension. Can be switched off in the menu.
  - **Resonance Treat** (honey bottle + 2 amethyst shards → 4): double experience for 5 minutes (stacks up to 15),
    a minute of speed, 2 hearts and an end to exhaustion. The bonus experience is not taken from you.
  - Commands: `/jtechstorage companion accept | decline | call | release confirm`, for game masters `keeper` and
    `xp <amount>`.
  - Config section `companions` (stage levels, XP share, keeper chance, search radius), sounds for all companions and
    the keeper, guide book and bestiary pages, advancements "A Loyal Friend", "Growing Together" and "Kindred Spirits".
- **The Geode Keeper trades** like a villager (vanilla trading screen; shift + right-click, or a plain right-click once
  you have a companion): amethyst shards for Resonance Treats, experience bottles, tinted glass, amethyst buds, name tags,
  Resonance Igniters, a spyglass, Amethyst Cores, Wireless Cores, budding amethyst, a Totem of Undying and an Ender Range
  Upgrade; he buys calcite, smooth basalt, glow berries, echo shards and emeralds for shards. Five levels with villager
  XP thresholds, two new random offers per level, stock refills every 10 minutes, trades give experience orbs. Keepers
  also appear for players who already have a companion, and trading with one makes him stay for good.
- **Natural Resonance Storms:** a storm now starts on its own when you stand in the chunk of your own Storage Terminal,
  have at least one helper and have mined more than 500 amethyst since your last storm (at most once per in-game day).
  New config section `storm` (`naturalStorms`, `amethystPerStorm`, `cooldownDays`).
- **Guide for GitHub** in [`docs/guide`](docs/guide/README.md): one page per topic, plus a recipe page with item pictures
  and a configuration page, both generated from the mod's own files.

### Changed
- `/jtechstorage storm` still requires game master rights; the new `/jtechstorage companion` commands are open to all
  players.

### Fixed
- **Cheldrogan's tail** no longer sways all the time: the serpent body only slithers while he moves; at rest he stays
  still apart from a slight breath.
- In-game handbook: the Resonance Storm page no longer says it can only be started by command.

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

