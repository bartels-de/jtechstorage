# Configuration

On its first start the mod writes **`config/jtechstorage.json`** – a JSON file with comments in English and German. Changes apply after a **restart**. Delete a line (or the whole file) to get the default value back. On a server the server's file counts; clients receive its values for tooltips and the handbook.

The interface settings (theme, accent color, effects, terminal height, count size, companion HUD) are per player and live in **`config/jtechstorage-client.json`** – change them with the gear button in the terminal (and the HUD button in the companion menu).

## Storage network (`network`)

| Key | Default | Description |
|---|---:|---|
| `baseRange` | 20 | Range without upgrade (blocks) |
| `ironRange` | 32 | Range: iron upgrade |
| `goldRange` | 48 | Range: gold upgrade |
| `diamondRange` | 64 | Range: diamond upgrade |
| `netheriteRange` | 96 | Range: netherite upgrade |
| `enderRange` | 128 | Range: ender upgrade (wireless terminal is unlimited anyway) |
| `crystalRange` | 192 | Range: crystal upgrade (pure amethyst, keeps the ender abilities) |
| `baseCapacity` | 8 | Linked containers without upgrade |
| `ironCapacity` | 16 | Capacity: iron upgrade |
| `goldCapacity` | 32 | Capacity: gold upgrade |
| `diamondCapacity` | 64 | Capacity: diamond upgrade |
| `netheriteCapacity` | 128 | Capacity: netherite upgrade |
| `crystalCapacity` | 256 | Capacity: crystal upgrade (pure amethyst) |
| `wirelessRangeFactor` | 4 | Wireless terminal range = terminal range x this |
| `resonanceRangeBonus` | 16 | Extra range while charged with resonance |
| `resonanceHelperSpeed` | 1.8 | Helper speed multiplier while charged |
| `beetleChargePerSecond` | 2 | Resonance per second from a Spark Beetle (rain x2, thunder x5) |

## Bosses (`bosses`)

| Key | Default | Description |
|---|---:|---|
| `robot.health` / `robot.armor` | 400 / 10 | Amethyst Robot |
| `robot.damageMultiplier` | 1.0 | Scales all robot attacks |
| `robotCoreChance` | 0.3 | Chance for an Amethyst Core or Wireless Core per kill |
| `spider.health` / `spider.armor` | 600 / 14 | Great Amethyst Spider |
| `spider.damageMultiplier` | 1.0 | Scales all spider attacks |

## Helpers (`helpers`)

| Key | Default | Description |
|---|---:|---|
| `foxDigIntervalDays` | 3.0 | Amethyst Fox: in-game days between finds |
| `foxMaxFeedingsPerDay` | 4 | Amethyst Fox: berries per day |
| `chelCraftIntervalSeconds` | 5.0 | Chel: seconds between crafting rounds |
| `chelMaxCraftsPerRound` | 4 | Chel: crafts per round |
| `spritePauseSeconds` | 2.5 | Amethyst Sprite: pause between jobs |
| `spriteCollectRadius` | 12 | Amethyst Sprite: item pickup radius around the terminal |
| `spriteBackpackRadius` | 10 | Amethyst Sprite: radius for emptying placed backpacks |
| `mothWatchRadius` | 24 | Watch Moth: radius for spawn protection and glowing monsters |
| `peterGuardRadius` | 16 | Peter Chel Parker (and Cheldrogan): guard radius |
| `cheldroganHealth` | 250.0 | Cheldrogan: health (0 = turns back into Peter) |
| `cheldroganDamage` | 24.0 | Cheldrogan: tail sweep damage |
| `cheldroganBreathCooldownSeconds` | 4.0 | Cheldrogan: crystal breath cooldown when ridden |
| `cheldroganRainCooldownSeconds` | 10.0 | Cheldrogan: shard rain cooldown when ridden |
| `phoenixMailBlocksPerSecond` | 32.0 | Phoenix mail: flight speed for parcel delivery time |
| `amethystPhoenixBlocksPerSecond` | 12.0 | Amethyst Phoenix: travel speed without cargo |
| `milestoneHologram` | true | Jbartels' hologram appears briefly at big milestones |

## Companions (`companions`)

| Key | Default | Description |
|---|---:|---|
| `stage2Level` | 30 | Companion level for stage 2 |
| `stage3Level` | 100 | Companion level for stage 3 |
| `xpShare` | 0.5 | Share of collected XP that goes to the companion (0.0-1.0) |
| `keeperChance` | 0.6 | Chance that a Geode Keeper lives in a newly found geode (0.0-1.0) |
| `keeperSearchRadius` | 12 | Radius around players in which geodes are noticed |
| `equipment` | true | Companion equipment (collar, amulet, armor) gives stats |

## Resonance Storm (`storm`)

| Key | Default | Description |
|---|---:|---|
| `naturalStorms` | true | Storms start on their own at your terminal (needs a helper) |
| `amethystPerStorm` | 500 | Amethyst mined before the first storm and between storms |
| `cooldownDays` | 1.0 | Minimum in-game days between two storms of a player |

## Farm tools (`farm`)

| Key | Default | Description |
|---|---:|---|
| `chunkLoadersPerPlayer` | 25 | Active chunk loaders per player (0 = none allowed) |
| `chunkLoaderMinutesPerShard` | 60 | Minutes one amethyst shard keeps a chunk loaded |

---

[← Controls and commands](14-controls-and-commands.md) · [Guide overview](README.md) · Next: [Farm tools →](16-farm-tools.md)
