# Resonance Storm

> *Not every resonance is a song. Sometimes it rings false – then the amethyst splinters, and from the splinters crawl
> creatures that want only one thing: to silence the heart of a storage.*

A **Resonance Storm** is an attack on your base. Crystal creatures march on your Storage Terminal in three waves, and
the last wave brings their leader, the **Storm Herald**.

## When does it start?

Mining amethyst builds up resonance. When it has built up enough, it discharges over your terminal. A storm starts on
its own when **all** of these are true:

| Condition | Default |
|---|---|
| You stand in the **chunk of your own Storage Terminal** (you placed it) | – |
| At least **one of your helpers** is near that terminal (within 96 blocks) | – |
| You mined **more than 500 amethyst** since your last storm | 500 |
| Your last storm was **at least one in-game day** ago | 1 day |
| You are in survival or adventure mode and the difficulty is not *Peaceful* | – |

- Amethyst means amethyst blocks, buds and clusters, counted by the normal Minecraft statistics (so amethyst you mined
  before installing this version counts too).
- After a storm the count starts again: the next storm comes after another 500 amethyst.
- You get a chat message when your mining triggers a storm.
- Everything can be changed or switched off in the [configuration](15-configuration.md) (`storm` section).
- Operators can start and stop storms by hand: `/jtechstorage storm start` (at the nearest terminal within 64 blocks)
  and `/jtechstorage storm stop`.

## What happens

1. **It gathers.** Thunder rolls in, purple-red sparks fly and a boss bar appears for everyone within 96 blocks.
2. **Three waves** of **Crystal Shardlings** appear 24–40 blocks around the terminal and march on it. Each wave has more
   shardlings (and more with more players nearby). When a wave is beaten – or after 2½ minutes – the next one follows a few
   seconds later.
3. In the **third wave** the **Storm Herald** joins them.
4. **Defeat the herald** and the storm ends at once: the sky clears and all remaining shardlings crumble.

### While the storm rages

Your network within 96 blocks of the storm is **disturbed**:

- half range,
- helpers work at half speed,
- resonance charge drains fast.

## The enemies

| Creature | Description |
|---|---|
| **Crystal Shardling** | A small, fast crawler of corrupted amethyst. Comes in swarms, pounces on its targets and marches on the terminal when it has none. Shatters into shards when it dies. |
| **Storm Herald** | Leader of the storm. Hovers just above the ground, keeps its distance and hurls resonance bolts. When shardlings run low it summons new ones. Loot: amethyst, amethyst blocks and, with some luck, an **Amethyst Core**. |

## Defending your base

- [Peter Chel Parker](04-helpers.md#peter-chel-parker) and [Cheldrogan](05-cheldrogan.md) fight every storm creature in
  their guard radius.
- [Watch Moths](04-helpers.md#watch-moth) make the creatures they can see glow.
- [Crystal Wolves](04-helpers.md#crystal-wolf) wake up and defend the base; the [Moon Wolf](04-helpers.md#moon-wolf)
  howls and freezes the storm creatures for a moment while the pack gets stronger.
- Your [companion](07-companions.md) fights at your side.

---

[← Companions](07-companions.md) · [Guide overview](README.md) · Next: [Bosses and the End →](09-bosses-and-end.md)
