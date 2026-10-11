# Dimensional Towers (NeoForge 1.21.1)
Themed towers: Overworld (deepslate tiles/tuff/copper), Nether (nether bricks), End (end stone bricks/purpur).
4 floors with stairs + 2 double chests each (random vanilla loot tables), boss arena on the roof:
200 HP Wither Skeleton that throws fireballs, drops an Ominous Trial Key -> opens the Vault (ominous trial loot),
plus 4 End City chests (roof only).

## Build the jar (no installs)
1. Make a free GitHub repo, upload everything in this folder (keep the .github folder).
2. Actions tab -> "Build mod" -> run. Download artifact `dimtowers-jar` -> unzip -> drop the jar in /mods.

## Tweaking
Spawn rarity: src/main/resources/data/dimtowers/worldgen/placed_feature/*.json ("chance" = 1 in N chunks).
