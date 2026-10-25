# TNTFreeze — experimental Paper plugin source

Commands (OP): `/tntfreeze add`, `/tntfreeze remove`, `/tntfreeze list`.

This is NOT true lazy chunk loading. It attempts to hold TNT's location, velocity and fuse in marked chunks and restore it on unmark. It does not prevent server entity ticking or guarantee that TNT won't detonate during the initial tick. Test on a backup world.

## Compile without installing Java locally
1. Create a GitHub repository and upload the CONTENTS of this folder (including `.github/workflows/build.yml`).
2. Open **Actions → Build TNTFreeze → Run workflow**.
3. After a successful run, download the **TNTFreeze-jar** artifact, unzip it and place the `.jar` in your Paper server `plugins/` folder.

If the workflow fails, inspect the Gradle error. The dependency is set to `paper-api:26.3-R0.1-SNAPSHOT`; availability has not been verified offline. The GitHub action needs network access to download dependencies.

## Local build
Install JDK 25 and Gradle 9.2.1, then run `gradle build`. The JAR is produced under `build/libs/`.

## Known limitations
- A Bukkit scheduler runs after entity ticks; TNT may tick or explode before freeze logic executes.
- Marked chunks are not force-loaded; TNT may unload.
- TNT motion restoration may not perfectly match vanilla behavior.
- This plugin has not been compiled or runtime-tested against Paper 26.3 build 214.
