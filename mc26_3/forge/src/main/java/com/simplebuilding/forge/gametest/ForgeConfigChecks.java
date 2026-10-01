package com.simplebuilding.forge.gametest;

import java.nio.file.*;
import com.simplebuilding.config.SimplebuildingConfig;
import me.shedaniel.autoconfig.AutoConfig;
import net.minecraft.gametest.framework.GameTestHelper;

/** Additional Forge persistence assertions before the canonical option/default test. */
public final class ForgeConfigChecks {
    public static void verify(GameTestHelper helper) {
        try {
            Path path = Files.createTempDirectory("simplebuilding-forge-config-").resolve("settings.json");
            var config = AutoConfig.read(path, SimplebuildingConfig.class);
            config.airJumpCooldownTicks = 777; config.showModHud = false;
            AutoConfig.write(path, config);
            var reloaded = AutoConfig.read(path, SimplebuildingConfig.class);
            helper.assertTrue(reloaded.airJumpCooldownTicks == 777 && !reloaded.showModHud, "Forge JSON survives a new holder/read");
            Files.writeString(path, "{\"airJumpCooldownTicks\":2147483647,\"tools\":null,\"tweaks\":null,\"server\":null}");
            reloaded = AutoConfig.read(path, SimplebuildingConfig.class);
            helper.assertTrue(reloaded.airJumpCooldownTicks == 6000 && reloaded.tools != null && reloaded.tweaks != null && reloaded.server != null, "Unsafe values clamp and missing groups recover");
            Files.writeString(path, "{broken"); byte[] broken = Files.readAllBytes(path);
            helper.assertTrue(AutoConfig.read(path, SimplebuildingConfig.class).airJumpCooldownTicks == 400 && java.util.Arrays.equals(broken, Files.readAllBytes(path)), "Malformed original retained, validated defaults");
            Path directoryTarget = path.resolveSibling("directory"); Files.createDirectory(directoryTarget);
            try { AutoConfig.write(directoryTarget, config); throw new AssertionError("Failed save reported success"); }
            catch (IllegalStateException expected) { helper.assertTrue(Files.isDirectory(directoryTarget), "Failed atomic save preserves destination"); }
        } catch (java.io.IOException ex) { throw new AssertionError(ex); }
    }
}
