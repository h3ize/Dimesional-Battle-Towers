# Dimensional Battle Towers (NeoForge 1.21.1)

Dimensional Battle Towers adds 19 large, fully themed boss towers that generate across the Overworld, Nether, and End. Each tower is built from biome-matched blocks, filled with trial-spawner floors of rising difficulty, and crowned by a unique boss fight with signature attacks, enrage phases, and theme-aware minion waves.

Climb five floors, clear the champion hall, then face the boss for a massive vault of netherite gear, named weapons, trim templates, and high XP.

## Towers (19)

| Tower              | Where                        | Boss pool                                   |
| ------------------ | ---------------------------- | ------------------------------------------- |
| Overworld          | any overworld biome          | Rotting Colossus, Bone Archon, Spider Queen |
| Nether             | nether                       | Ashen Tyrant, Blaze King                    |
| End                | end islands                  | Void Stalker, Ashen Tyrant                  |
| Piglin             | nether                       | Gilded Warlord, Blaze King                  |
| Crimson            | crimson forest               | Gilded Warlord, Blaze King                  |
| Warped             | warped forest                | Void Stalker, Ashen Tyrant                  |
| Frozen             | snowy biomes                 | Bone Archon, Axe Warlord                    |
| Desert             | desert                       | Rotting Colossus, Spider Queen              |
| Oak                | forest, flower forest        | Axe Warlord, Rotting Colossus               |
| Dark Forest        | dark forest                  | Axe Warlord, Spider Queen                   |
| Jungle             | jungle biomes                | Spider Queen, Slime King                    |
| Swamp              | swamp, mangrove swamp        | Slime King, Tidal Tyrant                    |
| Taiga              | taiga biomes                 | Rotting Colossus, Bone Archon               |
| Badlands           | badlands biomes              | Rotting Colossus, Ashen Tyrant              |
| Pillager           | pillager outpost biomes      | Axe Warlord                                 |
| Ocean (prismarine) | beaches, stony shores        | Tidal Tyrant                                |
| Deep Dark (sculk)  | windswept hills, stony peaks | Void Stalker, Ashen Tyrant                  |
| Cherry             | cherry grove, meadow         | Axe Warlord, Slime King                     |
| Savanna (acacia)   | savanna biomes               | Rotting Colossus, Bone Archon               |

Each theme uses its own wall/floor/pillar blocks, glass color, light source, armor trim pattern, pottery sherd, and floor-mob pool.

## Bosses (10)

| Boss                 | Base mob        | Signature move                                                                 |
| -------------------- | --------------- | ------------------------------------------------------------------------------ |
| Ashen Tyrant         | Wither Skeleton | Wither-skull fan (centre skull is the blue charged one)                        |
| Rotting Colossus     | Zombie          | Telegraphed ground-slam shockwave                                              |
| Bone Archon          | Skeleton        | Arrow fan + raining arrows from the sky when enraged                           |
| Gilded Warlord       | Piglin Brute    | Calls in hoglins                                                               |
| Axe Warlord          | Vindicator      | Telegraphed leap with landing shockwave                                        |
| Blaze King           | Blaze           | Large-fireball barrage + ring of small fireballs                               |
| Spider Queen         | Spider          | Temporary cobwebs on the target + cave-spider brood                            |
| Void Stalker         | Enderman        | Blinks next to you + dragon’s-breath clouds (no vanilla arrow immunity / block stealing) |
| Slime King           | Slime           | Ground slam, sheds small slimes when hit, splits on death                      |
| Tidal Tyrant         | Drowned         | Thrown-trident fan                                                             |

### Shared boss behaviour
- Purple notched boss bar
- Enrages at 50% HP → permanent Speed II + Strength II, lightning strikes on the target
- Minion waves at 75% / 40% / 15% HP (2 → 3 → 4 minions from that tower’s own mob list)
- 3000 XP on kill

## Floors (5)

| Floor | Identity | Spawner |
| ----- | -------- | ------- |
| 1 | Armory — armour stands with mixed gear (iron at best, rare diamond tools), candle tables, supply barrels | Trial Spawner, 6 mobs, one type |
| 2 | Library — lecterns, desks, bookshelf stacks, chiseled bookshelves | Trial Spawner, 8 mobs, one type |
| 3 | Trophy hall — mob heads on pedestals between candlesticks, infested bricks | Trial Spawner, 10 mobs, one type |
| 4 | Treasury — decorated pots, theme-loot chests | Trial Spawner, 12 mobs, one type |
| 5 | Champion hall — armour stands (iron at best), lantern braziers | Trial Spawner, 6 champions (80 HP, Strength II) |

Floor mobs scale with the floor: lower floors use the weaker half of the theme’s list, higher floors use the stronger half.

## Loot

**Boss vault** (opened with an Ominous Trial Key)
- 8 rolls from a rich table (netherite, enchanted gear, elytra, totems, enchanted golden apples, diamond/netherite blocks)
- 2–3 guaranteed trim templates
- Guaranteed named weapon and named chestplate for that theme
- 40% chance of an extra enchanted netherite piece

**Boss drops**
- 6–9 rolls with larger stacks
- Two enchanted books
- 1–2 gear rolls
- Guaranteed enchanted golden apple + netherite ingot
- 3000 XP

Named items carry the gold lore line: “Taken from a Dimensional Battle Tower.”

**Floor vaults** — smaller per-theme table with a chance at the theme’s trim template.

**Top-floor / treasury chests** — end city treasure + theme trim template + named gear (no maps).

**Tower Maps** — sold by Cartographer villagers (Journeyman trade: 20 emeralds + compass). Points to the nearest tower via the `dimtowers:towers` structure tag.

## Carry On

If Carry On is installed, tower bosses are added to its entity blacklist (`carryon:entity_blacklist`) so they cannot be picked up. Carry On removes a carried mob from the world, and a Trial Spawner treats a missing mob as dead and ejects its key.

Floor trial spawners refill killed mobs almost instantly (2 ticks between spawns), so two or more missing mobs come back together.

## Advancements

| Advancement            | How to unlock                                      |
| ---------------------- | -------------------------------------------------- |
| Towering Ambition      | Visit any Dimensional Battle Tower                 |
| Tower Slayer           | Kill any tower boss                                |
| Master of the Towers   | Kill all 10 boss types                             |
| Tower Cartographer     | Visit all 19 tower types                           |
| Keyring                | Open a vault with both a Trial Key and an Ominous Trial Key |