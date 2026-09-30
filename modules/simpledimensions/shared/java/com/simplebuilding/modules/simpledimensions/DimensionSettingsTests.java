package com.simplebuilding.modules.simpledimensions;

import dev.simpledimension.common.portal.DimensionPortalConfig;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import java.nio.file.Files;
import java.util.List;

/** Exercises Vanilla useItemOn and the registered loader ticks, without direct travel/tick helpers. */
public final class DimensionSettingsTests {
    public static void journey(GameTestHelper h, String id, int startTick) {
        // Existing tests share one server. Begin after their mutations and serialize these journeys.
        h.runAfterDelay(startTick, () -> new Journey(h, id).run());
    }

    private static final class Journey {
        final GameTestHelper h;
        final String id;
        final byte[] original;
        final byte[] definition;
        final ServerPlayer player;
        final BlockPos cell;
        BlockPos exactReturn;
        BlockPos arrival;

        Journey(GameTestHelper h, String id) {
            this.h = h;
            this.id = id;
            try {
                original = Files.readAllBytes(DimensionRuntime.CONFIG_ROOT.resolve("server.json"));
                definition = Files.readAllBytes(DimensionRuntime.CONFIG_ROOT.resolve("dimensions/" + id + ".json"));
            } catch (Exception e) { throw new AssertionError(e); }
            var config = runtime().config(id);
            cell = DimensionTests.build(h, config, id.equals("skyblock") ? 1 : 0, Direction.Axis.X);
            if (config.requireSeparateLight) h.getLevel().setBlock(cell.offset(0,0,2), Blocks.SEA_LANTERN.defaultBlockState(), 3);
            player = h.makeMockServerPlayerInLevel();
            player.setGameMode(net.minecraft.world.level.GameType.SURVIVAL);
            player.setNoGravity(true);
            player.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.FLINT_AND_STEEL));
            h.runBeforeTestEnd(() -> {
                h.getLevel().getServer().getPlayerList().remove(player);
                try { Files.write(DimensionRuntime.CONFIG_ROOT.resolve("server.json"), original); }
                catch (Exception e) { throw new AssertionError(e); }
                DimensionRuntime.stop(h.getLevel().getServer());
            });
            h.assertTrue(h.getLevel().getServer().getPlayerList().getPlayers().contains(player), "Player receives actual server ticks");
            position(cell.south());
        }

        DimensionRuntime runtime() { return DimensionRuntime.get(h.getLevel().getServer()); }
        void position(BlockPos pos) { player.setPos(pos.getX()+.5, pos.getY(), pos.getZ()+.5); }
        void toggle(boolean enabled) {
            var settings = runtime().settings;
            settings.accessEnabled = true;
            settings.portalDelayTicks = 0;
            settings.teleportCooldownTicks = 20;
            set(settings, id, enabled);
            settings.save(DimensionRuntime.CONFIG_ROOT);
            DimensionRuntime.stop(h.getLevel().getServer());
            // A stale per-definition flag must not override the dedicated server setting.
            runtime().config(id).enabled = false;
            h.assertTrue(runtime().settings.allowsAccess(runtime().config(id)) == enabled, id + " persisted access");
        }
        void ignite() {
            var hit = new BlockHitResult(Vec3.atCenterOf(cell.below()), Direction.UP, cell.below(), false);
            player.gameMode.useItemOn(player, player.level(), player.getMainHandItem(), InteractionHand.MAIN_HAND, hit);
        }
        void atSource(String message) { h.assertTrue(player.level() == h.getLevel(), id + ": " + message); }
        void atTarget() {
            h.assertTrue(player.level().dimension().identifier().toString().equals("simpledimension:" + id), id + " actual outbound tick hook");
            h.assertTrue(DimensionRuntime.safe(player.level(), player.blockPosition()), id + " safe arrival");
        }
        void run() {
            toggle(false);
            ignite();
            h.assertTrue(!DimensionRegistry.portal(h.getLevel().getBlockState(cell)), id + " disabled ignition via server hook");
            // Vanilla may light ordinary fire after the module declines the interaction.
            if (h.getLevel().getBlockState(cell).is(Blocks.FIRE)) h.getLevel().setBlock(cell, Blocks.AIR.defaultBlockState(), 3);
            toggle(true);
            ignite();
            h.assertTrue(DimensionRegistry.portal(h.getLevel().getBlockState(cell)), id + " reenabled ignition via server hook");
            toggle(false);
            position(cell);
            h.startSequence().thenIdle(23).thenExecute(() -> {
                atSource("unlinked outbound refused while off");
                h.assertTrue(!((SkyPortalBlockEntity) h.getLevel().getBlockEntity(cell)).linked, id + " no destination built while off");
                toggle(true);
            }).thenIdle(3).thenExecute(() -> {
                atTarget();
                arrival = player.blockPosition();
                var exit = (SkyPortalBlockEntity) player.level().getBlockEntity(arrival);
                h.assertTrue(exit.generated && exit.linked, id + " generated return");
                exactReturn = exit.link;
                toggle(false);
            }).thenIdle(3).thenExecute(() -> {
                atSource("return tick hook works with persisted switch off");
                h.assertTrue(player.blockPosition().equals(exactReturn), id + " exact return while off");
                position(cell.south());
            }).thenIdle(2).thenExecute(() -> position(cell)).thenIdle(23).thenExecute(() -> {
                atSource("linked outbound refused while off");
                h.assertTrue(((SkyPortalBlockEntity) h.getLevel().getBlockEntity(cell)).linked, id + " link retained while off");
                toggle(true);
            }).thenIdle(3).thenExecute(() -> {
                atTarget();
                h.assertTrue(player.blockPosition().equals(arrival), id + " existing connection reopened");
                toggle(false);
            }).thenIdle(3).thenExecute(() -> {
                atSource("second return remains open");
                try {
                    h.assertTrue(java.util.Arrays.equals(definition, Files.readAllBytes(DimensionRuntime.CONFIG_ROOT.resolve("dimensions/"+id+".json"))), id + " definition untouched");
                } catch (Exception e) { throw new AssertionError(e); }
            }).thenSucceed();
        }
    }

    private static void set(DimensionSettings settings, String id, boolean enabled) {
        switch (id) {
            case "skyblock" -> settings.skyblockEnabled = enabled;
            case "mining" -> settings.miningEnabled = enabled;
            case "travel" -> settings.travelEnabled = enabled;
            default -> throw new AssertionError(id);
        }
    }

    public static void persistence(GameTestHelper h) {
        try {
            var root = Files.createTempDirectory("dimension-settings-test");
            var definitions = dev.simpledimension.common.config.DimensionConfigStore.loadAndGenerate(root, 100);
            for (var definition : definitions) {
                var json = com.google.gson.JsonParser.parseString(Files.readString(root.resolve("dimensions/"+definition.id+".json"))).getAsJsonObject();
                h.assertTrue(!json.has("enabled"), "Built-in examples do not offer a second access switch");
            }
            // Older server files do not contain the three new settings.
            Files.writeString(root.resolve("server.json"), "{\"accessEnabled\":true}");
            var settings = DimensionSettings.load(root);
            var presets = List.of(DimensionPortalConfig.defaultSkyblock(), DimensionPortalConfig.miningDimensionPreset(), DimensionPortalConfig.travelDimensionPreset());
            for (var config : presets) h.assertTrue(settings.allowsAccess(config), config.id + " default on in old settings");
            for (var config : presets) for (boolean enabled : new boolean[]{false,true}) {
                set(settings, config.id, enabled);
                settings.save(root);
                settings = DimensionSettings.load(root);
                for (var other : presets) h.assertTrue(settings.allowsAccess(other) == (!other.id.equals(config.id) || enabled), "Independent persisted switches: " + config.id + "/" + other.id);
            }
            var custom = new DimensionPortalConfig(); custom.id = "custom"; custom.enabled = false;
            h.assertTrue(!settings.allowsAccess(custom), "Custom definition retains its legacy switch");
            settings.accessEnabled = false;
            for (var config : presets) h.assertTrue(!settings.allowsAccess(config), "Global access still gates " + config.id);
        } catch (Exception e) { throw new AssertionError(e); }
        h.succeed();
    }
}
