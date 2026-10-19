# Companions

A companion is your personal partner: it follows you everywhere, fights at your side, grows with your experience and
can be ridden at its final stage. You can have **one** companion at a time.

![Shadow Mantis and Night Raven](../images/companions.png)

## The Geode Keeper

Deep inside **amethyst geodes** there sometimes lives a **Geode Keeper** – an old hermit with crystals on his shoulders
and a staff topped with a cracked geode.

- Every geode has **exactly one chance** to hold a keeper (60 % by default). A soft hum and a message tell you when he
  is near. Geodes are noticed in the overworld below y = 80.
- **Right-click** him: he offers you **one of three companions**, chosen at random. Accept or decline by clicking in chat.
- **Decline** and he disappears – look for another geode and hope for a different offer.
- **Accept** and your companion appears next to him. The keeper then **settles** in his geode for good and stays your
  trading partner.
- An unsettled keeper leaves after a while if nobody is around.

### Trading with the Geode Keeper

The keeper trades **like a villager**, using the normal trading screen.

- **Shift + right-click** opens the trades. Once you have a companion, a plain right-click is enough.
- Every trade gives him experience. He climbs **five levels** (Novice → Master, with the same XP thresholds as
  villagers) and unlocks **two new random offers per level** (three at level 1).
- His stock **refills every 10 minutes**. Trades give you experience orbs.
- Trading with him also makes him settle in his geode.
- Keepers appear for **every** player – also for those who already have a companion.

| Level | Possible offers (a random selection appears) |
|---|---|
| 1 | 6 amethyst shards → 2 Resonance Treats · 4 shards → 1 amethyst cluster · 16 calcite → 3 shards · 16 smooth basalt → 3 shards · 1 emerald → 6 shards |
| 2 | 12 shards → 3 bottles o' enchanting · 8 shards → 4 tinted glass · 10 shards → 2 large amethyst buds · 16 shards → name tag · 12 glow berries → 4 shards |
| 3 | 24 shards → Resonance Igniter · 20 shards + 2 emeralds → spyglass · 16 shards → 8 Resonance Treats · 1 echo shard → 16 shards |
| 4 | 48 shards + 1 diamond → **Amethyst Core** · 40 shards + 1 diamond → **Wireless Core** · 32 shards + 8 emeralds → 16 bottles o' enchanting |
| 5 | 64 shards + 4 diamonds → **budding amethyst** · 64 shards + 2 emerald blocks → Totem of Undying · 64 shards + Amethyst Core → **Ender Upgrade** |

## The three companions

| | Shadow Mantis | Amethyst Shroom | Night Raven |
|---|---|---|---|
| Role | fighter | healer | carrier |
| Damage (of the mantis) | 100 % | 25 % | 50 % |
| Attacks every | 1.3 s | 2.0 s | 1.6 s |
| Health (stage 1 / 2 / 3) | 20 / 40 / 70 | 20 / 30 / 45 | 16 / 24 / 36 |
| Damage (stage 1 / 2 / 3) | 4 / 7 / 11 | 1 / 1.75 / 2.75 | 2 / 3.5 / 5.5 |
| Rideable at stage 3 | ground + short flights | – | permanent flight |

### Shadow Mantis

A pitch-black praying mantis with glowing slit eyes, serrated claws and swept-back horns. It attacks whatever you attack
or whatever attacks you, and monsters near you (it leaves creepers alone).

- **Stage 1 – Youngling.**
- **Stage 2:** dorsal spikes, a cobra hood and leather wings; armor 4, more damage and knockback.
- **Stage 3 – Blade Queen:** spiked pauldrons, a crown, amethyst scythes and crystal wings; armor 8. Its **scythe strike**
  also hits monsters around the target.
- **Riding (stage 3):** right-click to mount. Hold **Space** to fly for about 4 seconds; then it glides down and recharges
  on the ground.

### Amethyst Shroom

A mushroom on two legs with glowing spots on its cap. It heals you when you are hurt.

- **H** heals right away, **Shift + H** switches between *automatic* and *on key*. After healing it needs a break:
  **30 / 20 / 15 seconds** at stage 1 / 2 / 3.
- **Stage 1:** Regeneration once you have lost 2 hearts.
- **Stage 2:** 2 hearts instantly + Regeneration, also for your pets and other companions nearby.
- **Stage 3:** 3 hearts + Regeneration II, removes poison and wither, heals all players nearby. **Emergency heal:**
  below 30 % health it saves you with 4 hearts (every 60 s, automatic mode only – unless it has the *Lifesaver* talent).
- Fights with headbutts.

### Night Raven

A jet-black raven with glowing eyes, a spiky ruff and a courier bag. At stage 3 (**Raven Lord**) it wears a bone mask,
horns, a crystal crest and a saddle.

- **Collects items** around you into its bag: **9 / 18 / 27 slots** and a radius of **3 / 5 / 8 blocks** at stage 1 / 2 / 3.
  Items you just dropped yourself are left alone for 30 seconds.
- **Right-click it with a bound Storage Key** to link it to your terminal. When the bag is two-thirds full it flies the
  items there (invisible while it is away) and comes back. Clicking with the key again sends it right away.
- **Shift + right-click** opens its bag.
- **Riding (stage 3):** right-click to mount. It flies where you look for as long as you like; Space rises, Sneak
  dismounts.

## Growing up

- **Half of all experience** you collect goes to your companion (if it follows you and is within 32 blocks).
- It levels like a player and reaches **stage 2 at level 30** and **stage 3 at level 100** – each with a new look, more
  health and stronger abilities. Both levels are [configurable](15-configuration.md).
- **Resonance Treat** (honey bottle + 2 amethyst shards → 4): right-click your companion for **5 minutes of double
  experience** (feed more to stack up to 15 minutes), a minute of speed, 2 hearts and an end to its exhaustion.
- It **never dies**. If it takes too much damage it is **exhausted** for 45 seconds, stops fighting and recovers. If it
  falls into the void it is brought back to you.

## Following, staying and calling

- It follows you **everywhere** – through portals, into other dimensions and after you respawn.
- **Right-click** or the **G** key (up to 48 blocks away) toggles **follow / stay**. At stage 3, right-click on the mantis
  or raven mounts it instead, so use G.
- `/jtechstorage companion call` – or *Call* in the menu – brings it to you, even from another dimension.
- `/jtechstorage companion release confirm` releases it for good.

## Companion menu

Press **K** (or Shift + right-click the mantis or shroom) to open the menu. It works from any distance.

**Overview tab**

- a live portrait of your companion (when it is nearby),
- level, experience and health bars, damage, armor, attack speed, mode and stance,
- buttons: **Follow/Stay**, **Stance**, **Call**, **HUD on/off**; the shroom adds *Heal now* and its heal mode, the raven
  opens its *Bag*.

**Stances**

| Stance | Behaviour |
|---|---|
| Aggressive | attacks what you attack, what attacks you, and every monster near you (default) |
| Defensive | only defends you and itself |
| Passive | does not fight at all |

## Talent tree

- You get **1 talent point every 5 levels** – 20 points at level 100.
- Every species has **three branches with four talents**. A talent needs at least one point in the talent above it, and
  the lower talents need **stage 2 or 3**.
- Talents you can learn right now glow green in the *Talents* tab; hover for the exact effect, click to learn.
- **Reset** gives all points back and costs **1 Amethyst Core** (free in creative mode).

<!-- talents:start -->
### Shadow Mantis

**Blade**

| Talent | Ranks | From stage | Effect |
|---|---:|---:|---|
| ⚔ Sharp Blades | 3 | 1 | +8% damage per rank. |
| » Quick Strike | 2 | 1 | Attacks 8% faster per rank. |
| ✦ Precision | 2 | 2 | 10% chance per rank to deal double damage. |
| ☼ Whirling Scythe | 1 | 3 | The scythe strike hits monsters around the target with full instead of half damage and in a wider circle. |

**Armor**

| Talent | Ranks | From stage | Effect |
|---|---:|---:|---|
| ◆ Chitin Shell | 3 | 1 | +2 armor per rank. |
| ▣ Crystal Plates | 2 | 1 | +10% health per rank. |
| ✶ Thorns | 2 | 2 | Reflects 15% of melee damage per rank back at the attacker. |
| ♥ Unbreakable | 1 | 3 | Once every 5 minutes she stays on her feet with half health instead of being exhausted and gets Resistance II for a moment. |

**Hunt**

| Talent | Ranks | From stage | Effect |
|---|---:|---:|---|
| » Swift Legs | 2 | 1 | +7% speed per rank. |
| ◎ Hunting Instinct | 2 | 1 | Hunts monsters 4 blocks further around you per rank (normally 16). |
| ↑ Leap Attack | 1 | 2 | Leaps at targets 4–10 blocks away (every 4 s). |
| ✧ Crystal Flight | 2 | 3 | +50% flight stamina per rank while you ride her. |

### Amethyst Shroom

**Healing**

| Talent | Ranks | From stage | Effect |
|---|---:|---:|---|
| ♥ Strong Spores | 3 | 1 | Heals 15% more per rank. |
| » Fast Bloom | 2 | 1 | Heal cooldown 12% shorter per rank. |
| ◎ Wide Spores | 2 | 2 | Heals 3 blocks further per rank. |
| ✚ Lifesaver | 1 | 3 | The emergency heal comes twice as often (every 30 s) and also works in key mode. |

**Protection**

| Talent | Ranks | From stage | Effect |
|---|---:|---:|---|
| ▮ Tough Stalk | 3 | 1 | +10% health per rank. |
| ◆ Spore Shield | 2 | 1 | Healed allies get Absorption for 10 s (strength = rank). |
| ☁ Calm Aura | 2 | 2 | Healed allies get 5 s of Resistance per rank. |
| ✧ Purify | 1 | 3 | Healing removes all harmful effects. |

**Poison**

| Talent | Ranks | From stage | Effect |
|---|---:|---:|---|
| ☠ Toxic Spores | 3 | 1 | Hits poison for 2 s per rank. |
| ✶ Spore Burst | 2 | 1 | +15% damage per rank. |
| ❄ Numbing Spores | 2 | 2 | Hits slow the target for 2 s per rank. |
| ☣ Toxic Cloud | 1 | 3 | Every heal gives monsters within 5 blocks Poison II. |

### Night Raven

**Bag**

| Talent | Ranks | From stage | Effect |
|---|---:|---:|---|
| ◎ Keen Eye | 3 | 1 | Collects items 1.5 blocks further per rank. |
| ✉ Eager Courier | 2 | 1 | Flies to the terminal when the bag is half (rank 2: a third) full instead of two thirds. |
| » Swift Delivery | 2 | 2 | Returns 25% faster per rank. |
| ✦ Lucky Find | 1 | 3 | Brings back a few amethyst shards from 30% of deliveries. |

**Combat**

| Talent | Ranks | From stage | Effect |
|---|---:|---:|---|
| ⚔ Sharp Beak | 3 | 1 | +10% damage per rank. |
| ↓ Dive | 2 | 1 | Attacks 10% faster per rank. |
| ◉ Eye Peck | 2 | 2 | 10% chance per rank to blind and weaken the target for 3 s. |
| ☾ Shadow Swarm | 1 | 3 | 20% chance per hit: shadow crows deal 3 damage to monsters around the target. |

**Flight**

| Talent | Ranks | From stage | Effect |
|---|---:|---:|---|
| ≈ Tailwind | 3 | 1 | +7% speed per rank (also while riding). |
| ❦ Feather Fall | 2 | 1 | When you fall more than 6 (rank 2: 3) blocks, you get Slow Falling. |
| ◐ Night Eye | 1 | 2 | Makes monsters within 16 blocks of you glow briefly every 10 s. |
| ♛ Sky Lord | 2 | 3 | Flies 20% faster per rank while you ride it. |
<!-- talents:end -->

## HUD

A small display in the top left corner shows your companion at a glance:

- species, stage, name and level,
- health and experience bars,
- follow / stay / away / exhausted, the stance and the remaining treat time,
- the shroom's heal cooldown, the raven's bag fill, or the mantis' flight stamina while you ride it,
- the distance (or "other dimension") when it is far away.

Switch it off with the *HUD* button in the companion menu (saved per player).

## Commands

| Command | Who | Effect |
|---|---|---|
| `/jtechstorage companion call` | everyone | bring your companion to you |
| `/jtechstorage companion release confirm` | everyone | release your companion for good |
| `/jtechstorage companion accept` / `decline` | everyone | answer a keeper's offer (the chat buttons do this) |
| `/jtechstorage companion keeper` | operators | spawn a Geode Keeper in front of you |
| `/jtechstorage companion xp <amount>` | operators | give your companion experience |

---

[← Phoenix mail and travel](06-phoenix.md) · [Guide overview](README.md) · Next: [Resonance Storm →](08-resonance-storm.md)
