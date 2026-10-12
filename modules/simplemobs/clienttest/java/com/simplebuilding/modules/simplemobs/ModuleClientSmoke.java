package com.simplebuilding.modules.simplemobs;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.server.level.ServerLevel;

/** Boots the module, then shows the Deceiver in idle and in every action pose (screenshots). */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplemobs")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplemobs-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var p = server.getPlayerList().getPlayers().getFirst();
                var level = (ServerLevel) p.level();
                var at = p.blockPosition().offset(0, 0, 3);
                level.setBlockAndUpdate(at, net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                var d = MobsRegistry.DECEIVER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (d == null) throw new AssertionError("Deceiver did not spawn");
                d.snapTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 180f, 0f);
                d.setYBodyRot(180f);
                d.setYHeadRot(180f);
                d.setNoAi(true);
                level.addFreshEntity(d);
            });
            context.waitTicks(30);
            context.takeScreenshot("simplemobs-deceiver-idle");
            for (var pose : new Object[][] {{"simplemobs-deceiver-summon", DeceiverEntity.ACT_SUMMON, 22}, {"simplemobs-deceiver-teleport", DeceiverEntity.ACT_TELEPORT, 8},
                    {"simplemobs-deceiver-drink", DeceiverEntity.ACT_DRINK, 16}, {"simplemobs-deceiver-unmask", DeceiverEntity.ACT_UNMASK, 4}}) {
                world.getServer().runOnServer(server -> {
                    var d = server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getEntities(MobsRegistry.DECEIVER, e -> true).getFirst();
                    d.playAction((Integer) pose[1], 200);
                });
                context.waitTicks((Integer) pose[2]);
                context.takeScreenshot((String) pose[0]);
            }
            world.getServer().runOnServer(server -> {
                var d = server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getEntities(MobsRegistry.DECEIVER, e -> true).getFirst();
                d.playAction(DeceiverEntity.ACT_NONE, 0);
                d.setArmored(true);
            });
            context.waitTicks(20);
            context.takeScreenshot("simplemobs-deceiver-armored");
            // Hollow Howler: idle, jaws open, shriek, emerging
            world.getServer().runOnServer(server -> {
                var level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                level.getEntities(MobsRegistry.DECEIVER, e -> true).forEach(e -> e.discard());
                var p = server.getPlayerList().getPlayers().getFirst();
                var at = p.blockPosition().offset(0, 0, 5);
                for (int dx = -1; dx <= 1; dx++) for (int dz = -1; dz <= 1; dz++) level.setBlockAndUpdate(at.offset(dx, 0, dz), net.minecraft.world.level.block.Blocks.SCULK.defaultBlockState());
                var h = MobsRegistry.HOLLOW_HOWLER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (h == null) throw new AssertionError("Hollow Howler did not spawn");
                h.snapTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 180f, 0f);
                h.setYBodyRot(180f);
                h.setYHeadRot(180f);
                h.setNoAi(true);
                level.addFreshEntity(h);
            });
            context.waitTicks(30);
            context.takeScreenshot("simplemobs-howler-idle");
            world.getServer().runOnServer(server -> {
                var h = server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getEntities(MobsRegistry.HOLLOW_HOWLER, e -> true).getFirst();
                h.onHeard(h.blockPosition(), null, null);
            });
            context.waitTicks(3);
            context.takeScreenshot("simplemobs-howler-listen");
            world.getServer().runOnServer(server -> server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getEntities(MobsRegistry.HOLLOW_HOWLER, e -> true).getFirst().holdJawsOpen());
            context.waitTicks(10);
            context.takeScreenshot("simplemobs-howler-bite");
            world.getServer().runOnServer(server -> {
                var level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                var h = level.getEntities(MobsRegistry.HOLLOW_HOWLER, e -> true).getFirst();
                h.startBite();
                h.shriek(level, HowlerLogic.Phase.ONE);
            });
            context.waitTicks(3);
            context.takeScreenshot("simplemobs-howler-shriek");
            world.getServer().runOnServer(server -> server.getLevel(net.minecraft.world.level.Level.OVERWORLD).getEntities(MobsRegistry.HOLLOW_HOWLER, e -> true).getFirst().startEmerging());
            context.waitTicks(16);
            context.takeScreenshot("simplemobs-howler-emerge");
            // Shellker closed (bedrock disguise) next to an open one with its slit and eye, a real bedrock block for comparison
            world.getServer().runOnServer(server -> {
                var level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                level.getEntities(MobsRegistry.HOLLOW_HOWLER, e -> true).forEach(e -> e.discard());
                var p = server.getPlayerList().getPlayers().getFirst();
                var at = p.blockPosition().offset(0, 0, 3);
                for (int dx = -2; dx <= 2; dx++) level.setBlockAndUpdate(at.offset(dx, 0, 0), net.minecraft.world.level.block.Blocks.STONE.defaultBlockState());
                level.setBlockAndUpdate(at.offset(-2, 1, 0), net.minecraft.world.level.block.Blocks.BEDROCK.defaultBlockState());
                for (int i = 0; i < 2; i++) {
                    var s = EndNetherMobs.SHELLKER.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    if (s == null) throw new AssertionError("Shellker did not spawn");
                    s.snapTo(at.getX() + 0.5 + (i == 0 ? -1 : 1), at.getY() + 1, at.getZ() + 0.5, 180f, 0f);
                    s.setNoAi(true);
                    s.setYHeadRot(180f);
                    level.addFreshEntity(s);
                    if (i == 1) s.setOpen(true);
                }
            });
            context.waitTicks(30);
            context.takeScreenshot("simplemobs-shellker");
            // Soulfire Blaze charging, and the Shellker helmet on armor stands (plain and gold-trimmed)
            world.getServer().runOnServer(server -> {
                var level = server.getLevel(net.minecraft.world.level.Level.OVERWORLD);
                level.getEntities(EndNetherMobs.SHELLKER, e -> true).forEach(e -> e.discard());
                var p = server.getPlayerList().getPlayers().getFirst();
                var at = p.blockPosition().offset(0, 0, 4);
                var b = EndNetherMobs.SOULFIRE_BLAZE.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                if (b == null) throw new AssertionError("Soulfire Blaze did not spawn");
                b.snapTo(at.getX() + 0.5, at.getY() + 1, at.getZ() + 0.5, 180f, 0f);
                b.setYBodyRot(180f);
                b.setYHeadRot(180f);
                b.setNoAi(true);
                b.setSoulCharged(true);
                level.addFreshEntity(b);
                var trims = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TRIM_MATERIAL);
                var patterns = level.registryAccess().lookupOrThrow(net.minecraft.core.registries.Registries.TRIM_PATTERN);
                for (int i = 0; i < 2; i++) {
                    var stand = net.minecraft.world.entity.EntityTypes.ARMOR_STAND.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
                    stand.snapTo(at.getX() + 0.5 + (i == 0 ? -2 : 2), at.getY() + 1, at.getZ() + 0.5, 180f, 0f);
                    var helmet = new net.minecraft.world.item.ItemStack(EndNetherMobs.HELMET);
                    if (i == 1) helmet.set(net.minecraft.core.component.DataComponents.TRIM, new net.minecraft.world.item.equipment.trim.ArmorTrim(
                            trims.getOrThrow(net.minecraft.world.item.equipment.trim.TrimMaterials.GOLD), patterns.getOrThrow(net.minecraft.world.item.equipment.trim.TrimPatterns.SENTRY)));
                    stand.setItemSlot(net.minecraft.world.entity.EquipmentSlot.HEAD, helmet);
                    level.addFreshEntity(stand);
                }
            });
            context.waitTicks(30);
            context.takeScreenshot("simplemobs-soulfire-blaze");
            if (net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplebuilding")) testCentre(context, world);
        }
    }

    /**
     * Queue N43, with SimpleBuilding loaded: its test centre's mob station (one cell per mob of every mod). Builds the
     * section, then per cell stands at its viewpoint and presses buttons by powering them like a player would (a redstone
     * block in the button's place): still spawn, the Deceiver armored, the Shellker's four gateway guards.
     */
    private static void testCentre(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world) {
        java.util.function.Consumer<String> run = command -> world.getServer().runOnServer(server -> {
            server.getCommands().performPrefixedCommand(server.createCommandSourceStack().withSuppressedOutput(), command);
        });
        run.accept("kill @e[type=!minecraft:player]");
        run.accept("gamemode creative @a");
        run.accept("time set noon");
        run.accept("weather clear");
        run.accept("sbtestcentre section mobs");
        // Cell layout (SimpleBuilding MobStations): viewpoint 4 in front of the buttons; still at x-3, remove x+1, extra x+3.
        String[][] cells = {{"simplemobs", "deceiver"}, {"simplemobs", "hollow_howler"}, {"simplemobs", "shellker"},
                {"simplemobs", "soulfire_blaze"}, {"simpleenddragonfight", "dragonmite"}};
        for (String[] cell : cells) {
            if (!net.minecraft.core.registries.BuiltInRegistries.ENTITY_TYPE.containsKey(net.minecraft.resources.Identifier.fromNamespaceAndPath(cell[0], cell[1]))) continue;
            String name = cell[1];
            run.accept("execute as @p at @s run sbtestcentre tp mob/" + cell[0] + "/" + name);
            context.waitTicks(10);
            press(context, run, -3);
            if (name.equals("shellker")) press(context, run, 3);
            context.waitTicks(10);
            world.getConnection().waitForChunksRender();
            context.waitTicks(30);
            context.takeScreenshot("testcenter-mobs-" + name);
            if (name.equals("deceiver")) {
                press(context, run, 1);
                context.waitTicks(5);
                press(context, run, 3);
                context.waitTicks(30);
                context.takeScreenshot("testcenter-mobs-deceiver-armored");
            }
            press(context, run, 1);
            context.waitTicks(5);
        }
    }

    /** Powers the button {@code dx} to the side of the viewpoint (one up, four ahead) for a moment, then puts it back. */
    private static void press(ClientGameTestContext context, java.util.function.Consumer<String> run, int dx) {
        run.accept("execute as @p at @s run setblock ~" + dx + " ~1 ~4 minecraft:redstone_block");
        context.waitTicks(4);
        run.accept("execute as @p at @s run setblock ~" + dx + " ~1 ~4 minecraft:stone_button[face=wall,facing=north]");
        context.waitTicks(2);
    }
}
