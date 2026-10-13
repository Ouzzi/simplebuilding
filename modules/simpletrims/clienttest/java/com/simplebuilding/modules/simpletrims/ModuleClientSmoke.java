package com.simplebuilding.modules.simpletrims;

import com.simpletrims.TrimGlow;
import java.util.List;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;

/**
 * Client smoke: the client mixins load (trim glow/pulse renderer, tooltip lines), an armor stand shows a Glowing +
 * Pulsating trim at night, and placed trim templates lie as plates on the floor (block entity renderer).
 */
public final class ModuleClientSmoke implements FabricClientGameTest {
    @Override
    public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simpletrims")) throw new AssertionError("Module did not boot");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            world.getConnection().waitForChunksRender();
            var server = world.getServer();
            server.runCommand("gamemode creative @a");
            server.runCommand("time set midnight");
            server.runCommand("gamerule advance_time false");
            server.runCommand("execute as @a at @s run tp @s ~ ~ ~ 0 25");
            server.runCommand("execute at @a run fill ~-3 ~-1 ~1 ~3 ~-1 ~6 minecraft:stone");
            server.runCommand("execute at @a run summon minecraft:armor_stand ~ ~ ~4 {Rotation:[180f,0f],equipment:{chest:{id:\"minecraft:iron_chestplate\",components:{"
                    + "\"minecraft:trim\":{material:\"minecraft:gold\",pattern:\"minecraft:sentry\"},\"simpletrims:glow_level\":1,\"simpletrims:pulsating\":true}},"
                    + "head:{id:\"minecraft:iron_helmet\",components:{\"minecraft:trim\":{material:\"minecraft:emerald\",pattern:\"minecraft:wild\"},\"simpletrims:pulsating\":true}}}}");
            String[] templates = {"simpletrims:glowing_trim_template", "simpletrims:emitting_trim_template", "simpletrims:pulsating_trim_template",
                    "minecraft:coast_armor_trim_smithing_template"};
            for (int i = 0; i < templates.length; i++) {
                server.runCommand("execute at @a run setblock ~" + (i - 2) + " ~ ~2 simpletrims:placed_template[face=floor,facing=north]{Template:{id:\""
                        + templates[i] + "\",count:1}}");
            }
            context.waitTicks(40);
            context.takeScreenshot("simpletrims-night");
            context.runOnClient(c -> {
                ItemStack chest = new ItemStack(Items.IRON_CHESTPLATE);
                for (int i = 0; i < 3; i++) TrimGlow.incrementEmissionLevel(chest);
                TrimGlow.setGlowLevel(chest, 1);
                TrimGlow.setPulsating(chest, true);
                List<Component> lines = chest.getTooltipLines(Item.TooltipContext.of(c.level), c.player, TooltipFlag.NORMAL);
                for (String key : List.of("tooltip.simpletrims.radiance_level", "tooltip.simpletrims.glow_level", "tooltip.simpletrims.pulsating",
                        "tooltip.simpletrims.pulsating.glow")) {
                    boolean found = lines.stream().anyMatch(line -> line.getContents() instanceof TranslatableContents t && t.getKey().equals(key));
                    if (!found) throw new AssertionError("tooltip line " + key + " missing: " + lines);
                }
            });
            context.waitTicks(25);
            context.takeScreenshot("simpletrims-night-pulse");
        }
    }
}
