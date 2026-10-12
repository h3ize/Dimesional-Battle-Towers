# Dimensional Towers (NeoForge 1.21.1)

Large themed boss towerseach

## Towers (19)
| Tower | Where | Boss pool |
|---|---|---|
| Overworld | any overworld biome | Rotting Colossus, Bone Archon, Spider Queen |
| Nether | nether | Ashen Tyrant, Blaze King |
| End | end islands | Void Stalker, Ashen Tyrant |
| Piglin | nether | Gilded Warlord, Blaze King |
| Crimson | crimson forest | Gilded Warlord, Blaze King |
| Warped | warped forest | Void Stalker, Ashen Tyrant |
| Frozen | snowy biomes | Bone Archon, Axe Warlord |
| Desert | desert | Rotting Colossus, Spider Queen |
| Oak | forest, flower forest | Axe Warlord, Rotting Colossus |
| Dark Forest | dark forest | Axe Warlord, Spider Queen |
| Jungle | jungle biomes | Spider Queen, Slime King |
| Swamp | swamp, mangrove swamp | Slime King, Tidal Tyrant |
| Taiga | taiga biomes | Rotting Colossus, Bone Archon |
| Badlands | badlands biomes | Rotting Colossus, Ashen Tyrant |
| Pillager | pillager outpost biomes | Axe Warlord |
| Ocean (prismarine) | beaches, stony shores | Tidal Tyrant |
| Deep Dark (sculk) | windswept hills, stony peaks | Void Stalker, Ashen Tyrant |
| Cherry | cherry grove, meadow | Axe Warlord, Slime King |
| Savanna (acacia) | savanna biomes | Rotting Colossus, Bone Archon |

## Bosses
| Boss | Base mob | Move |
|---|---|---|
| Ashen Tyrant | wither skeleton | wither-skull fan (centre skull is the blue one) |
| Rotting Colossus | zombie | telegraphed ground-slam shockwave |
| Bone Archon | skeleton | arrow fan, plus arrows raining from the sky when enraged |
| Gilded Warlord | piglin brute | calls in hoglins |
| Axe Warlord | vindicator | telegraphed leap with a landing shockwave |
| Blaze King | blaze | large-fireball barrage and a ring of small fireballs |
| Spider Queen | spider | temporary cobwebs on the target, cave-spider brood |
| Void Stalker | enderman | blinks next to you, dragon's-breath clouds (vanilla arrow immunity and block stealing removed) |
| Slime King | slime | ground slam, sheds small slimes when hit, splits on death |
| Tidal Tyrant | drowned | thrown-trident fan |

## Floors
| Floor | Identity | Spawner |
|---|---|---|
| 1 | Armory: armour stands with random mixed gear (iron at best, diamond tools are rare), candle tables, supply barrels | Trial Spawner, 6 mobs, one mob type |
| 2 | Library: lecterns, desks, bookshelf stacks, chiseled shelves | Trial Spawner, 8 mobs, one mob type |
| 3 | Trophy hall: mob heads on pedestals between candlesticks, infested bricks | Trial Spawner, 10 mobs, one mob type |
| 4 | Treasury: decorated pots, theme-loot chests | Trial Spawner, 12 mobs, one mob type |
| 5 | Champion hall: armour stands (iron at best), lantern braziers | Trial Spawner, 6 champions (80 HP, strength II) |

## Loot
- Boss vault (ominous key): 8 rolls of a much richer table (netherite, enchanted gear, elytra, totems, enchanted golden apples, diamond/netherite blocks),
  2-3 guaranteed trim templates, a guaranteed named weapon AND a guaranteed named chestplate per theme, and a 40% chance of an extra enchanted netherite piece.
- Boss drops: 6-9 rolls with larger stacks, two enchanted books, 1-2 gear rolls, a guaranteed enchanted golden apple and netherite ingot, and 3000 XP.
- Named items carry the lore line "Taken from a Boss Tower." in gold.
- Floor vaults: smaller per-theme table with a chance of the theme's trim template.
- Top-floor chests and the treasury: `end_city_treasure` plus the theme's trim template and named gear. No maps in chests.
- Tower Maps are sold by Cartographer villagers (journeyman trade: 20 emeralds, with a compass) and point at the nearest tower via
  the `dimtowers:towers` structure tag.
- Bosses drop heads (where vanilla has one) and a music disc.

## Advancements
Towering Ambition, Tower Slayer, Master of the Towers (all ten boss types), Tower Cartographer (visit all 19 tower types),
Keyring (open a vault with a trial key and an ominous trial key).


## Build
```
gradle wrapper
./gradlew build
./gradlew runClient
```
Requires JDK 21. This update was written without a Minecraft toolchain, so it has not been compiled yet.
