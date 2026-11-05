# Minecraft compatibility groups

Build and distribute one runtime JAR per row. Install only the JAR matching the Minecraft version, together with the matching Fabric API.

| Local branch | Supported Minecraft versions | Compile target |
| --- | --- | --- |
| `1.21.1` | 1.21.1 | 1.21.1 |
| `1.21.2-1.21.4` | 1.21.2, 1.21.3, 1.21.4 | 1.21.3 |
| `1.21.5` | 1.21.5 | 1.21.5 |
| `1.21.6-1.21.8` | 1.21.6, 1.21.7, 1.21.8 | 1.21.8 |
| `1.21.9-1.21.11` | 1.21.9, 1.21.10, 1.21.11 | 1.21.11 |
| `26.1-26.1.2` | 26.1, 26.1.1, 26.1.2 | 26.1.2 |
| `26.2` | 26.2 | 26.2 |
| `master` | 26.3 | 26.3 |

The shared branches declare exact supported versions in `fabric.mod.json`; `game_versions` in `gradle.properties` lists the same versions for publication. Their archive names include the compatibility group. Mod versions are unchanged.

## Validation

The 2026-11-05 consolidation checked the shared production bytecode against Minecraft and Fabric API dependencies on all 12 grouped targets. Production-client smoke tests open the settings and variant screens, generate every default mob/non-mob icon and available no-world enum variant icon, then shut down cleanly without creating a world. Rebuilt shared JAR entries are compared with the cross-version-tested candidates.

All 12 shared-group targets and the separate 1.21.1, 1.21.5 and 26.2 clients passed these smoke checks. The separate 26.3 client built and passed packaging checks, but its runtime launch failed with native exit `0xC0000005` during resource loading; it remains unverified at runtime and is not merged into another group. No cause is assigned from that exit code alone.

This is not a claim of full gameplay, resource-pack visual parity, datapack-registry variant or third-party mod compatibility. Separate groups retain materially different Minecraft integration APIs; no single 1.21.1-1.21.11 JAR is claimed.

Collected runtime artifacts and detailed evidence are under `build/ports`. Source JARs and Gradle intermediates are development artifacts, not additional downloads to install. Re-run compatibility checks before adding another Minecraft version to a group.

Branch refs, uncommitted source and prior artifacts were backed up under `.git/cleanup-backups/consolidation-20261105-120457` before cleanup. The bundle was restored into an isolated bare repository and the source archive was hash-checked. No commits, remote branch changes or releases are part of this local consolidation.
