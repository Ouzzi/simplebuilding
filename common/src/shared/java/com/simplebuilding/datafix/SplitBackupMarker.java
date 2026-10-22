package com.simplebuilding.datafix;

import com.simplebuilding.version.McVersion;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Instant;
import net.minecraft.server.MinecraftServer;
import net.minecraft.world.level.storage.LevelResource;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Split stage 9 (PLAN-MODULAUFTEILUNG §3.4, docs/ai/PLAN-B-S9B-2026-10-22.md): SimpleBuilding 2.0 moved many ids into
 * its sub-mods; old worlds migrate on load (SimpleLib id moves). The first server start of a world with this version
 * writes {@value #FILE} into the world folder - once, never again - so players and server admins can see that the
 * split version opened the world and whether it existed before. An existing world (its overworld has played at least
 * one tick) also gets a warning in the log that recommends a backup; a new world only an info line.
 *
 * <p>Nothing is copied: a copy of {@code level.dat} and {@code playerdata} alone would leave the chunks - most of a
 * world - unsaved and look safer than it is. The changelog asks for a backup of the whole world folder.
 * Only on 26.3 ({@link McVersion#SPLIT_BACKUP_MARKER}); 26.2 keeps the single jar.
 */
public final class SplitBackupMarker {
    /** The marker in the world folder. */
    public static final String FILE = "simplebuilding-split-backup.marker";
    /** The release that split SimpleBuilding into sub-mods. */
    public static final String SPLIT_VERSION = "2.0.0";
    private static final Logger LOGGER = LoggerFactory.getLogger("simplebuilding");

    private SplitBackupMarker() {
    }

    /** Server started (all loaders): the marker for this server's world. */
    public static void onServerStarted(MinecraftServer server) {
        if (!McVersion.SPLIT_BACKUP_MARKER) {
            return;
        }
        write(server.getWorldPath(LevelResource.ROOT), server.overworld().getGameTime() > 0);
    }

    /**
     * Writes the marker into {@code worldDir} unless it is there already; true when it was written now. Public for the
     * game test, which runs it against temporary folders.
     */
    public static boolean write(Path worldDir, boolean existingWorld) {
        if (!McVersion.SPLIT_BACKUP_MARKER) {
            return false;
        }
        Path marker = worldDir.resolve(FILE);
        if (Files.exists(marker)) {
            return false;
        }
        try {
            Files.createDirectories(worldDir);
            Files.writeString(marker, "# SimpleBuilding " + SPLIT_VERSION + " split its content into sub-mods; old ids migrate on load.\n"
                    + "# Written once on the first start of this world with that version. A backup of the world folder from\n"
                    + "# before the update is the only way back.\n"
                    + "split=" + SPLIT_VERSION + "\n"
                    + "firstStart=" + Instant.now() + "\n"
                    + "world=" + (existingWorld ? "existing" : "new") + "\n", StandardCharsets.UTF_8);
        } catch (IOException e) {
            LOGGER.warn("Could not write the split backup marker {}", marker, e);
            return false;
        }
        if (existingWorld) {
            LOGGER.warn("SimpleBuilding {}: first start of this world since the split into sub-mods. Old ids migrate "
                    + "automatically; keep a backup of the world from before this update. Marker: {}", SPLIT_VERSION, marker);
        } else {
            LOGGER.info("SimpleBuilding {}: new world, split backup marker written ({})", SPLIT_VERSION, marker);
        }
        return true;
    }
}
