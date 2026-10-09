package com.simplebuilding.clientgametest;

import java.util.Set;
import java.util.TreeSet;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.network.chat.Component;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;

/**
 * Owner N21/N28: the Enderite bucket holds two buckets. The half items draw the half textures, the full items the
 * full ones, and the tooltip names the fill (1/2, 2/2). Shows empty, half and full buckets in the inventory and puts
 * the hotbar back empty. Only on lines with the crucible buckets (26.3).
 */
public final class EnderiteBucketClientTest {
    private EnderiteBucketClientTest() {}

    /** Hotbar slot, item id and the sprite its icon must draw. */
    private static final String[][] SHOWCASE = {
            {"hotbar.0", "simplebuilding:enderite_bucket", "simplebuilding:item/enderite_bucket"},
            {"hotbar.1", "simplebuilding:enderite_water_bucket", "simplebuilding:item/enderite_water_bucket_half"},
            {"hotbar.2", "simplebuilding:enderite_water_bucket_full", "simplebuilding:item/enderite_water_bucket"},
            {"hotbar.3", "simplebuilding:enderite_lava_bucket", "simplebuilding:item/enderite_lava_bucket_half"},
            {"hotbar.4", "simplebuilding:enderite_lava_bucket_full", "simplebuilding:item/enderite_lava_bucket"},
            {"hotbar.5", "simplebuilding:enderite_soul_lava_bucket", "simplebuilding:item/enderite_soul_lava_bucket_half"},
            {"hotbar.6", "simplebuilding:enderite_soul_lava_bucket_full", "simplebuilding:item/enderite_soul_lava_bucket"},
    };

    public static void inWorld(Script script) {
        if (com.simplebuilding.fluid.ModFluids.FULL_ENDERITE_WATER_BUCKET == null) return;
        TestScene.build(script, "minecraft:stone", "creative");
        for (String[] piece : SHOWCASE) {
            script.command("item replace entity @a " + piece[0] + " with " + piece[1]);
        }
        script.awaitPackets();
        script.act("half and full Enderite buckets draw their own textures and name their fill", client -> {
            StringBuilder problems = new StringBuilder();
            for (int slot = 0; slot < SHOWCASE.length; slot++) {
                ItemStack stack = client.player.getInventory().getItem(slot);
                ItemStackRenderState state = new ItemStackRenderState();
                client.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, client.level, client.player, 0);
                Set<String> sprites = new TreeSet<>();
                for (int i = 0; i < 16; i++) {
                    sprites.add(String.valueOf(state.pickParticleMaterial(RandomSource.create()).sprite().contents().name()));
                }
                if (!sprites.contains(SHOWCASE[slot][2])) {
                    problems.append(SHOWCASE[slot][1]).append(" draws ").append(sprites).append("; ");
                }
            }
            requireLine(client, problems, com.simplebuilding.fluid.ModFluids.ENDERITE_WATER_BUCKET, "Holds 1/2 buckets");
            requireLine(client, problems, com.simplebuilding.fluid.ModFluids.FULL_ENDERITE_LAVA_BUCKET, "Holds 2/2 buckets");
            if (problems.length() > 0) throw new AssertionError("Enderite buckets: " + problems);
        });
        // Survival for the shot: in creative, InventoryScreen hands over to the creative inventory.
        script.command("gamemode survival @a");
        script.act("open the inventory", client -> client.gui.setScreen(new InventoryScreen(client.player)));
        script.await("wait for the inventory", 100,
                client -> client.gui.screen() instanceof InventoryScreen,
                client -> "the inventory never opened; the current screen is " + client.gui.screen());
        script.idle("let the item icons settle", 10);
        script.shot("enderite-buckets-inventory");
        script.act("close the inventory", client -> client.gui.setScreen(null));
        script.await("wait until the inventory is closed", 100,
                client -> client.gui.screen() == null,
                client -> "a screen is still open: " + client.gui.screen());
        script.command("gamemode creative @a");
        for (String[] piece : SHOWCASE) {
            script.command("item replace entity @a " + piece[0] + " with minecraft:air");
        }
    }

    private static void requireLine(net.minecraft.client.Minecraft client, StringBuilder problems, Item item, String text) {
        ItemStack stack = new ItemStack(item);
        for (Component line : stack.getTooltipLines(Item.TooltipContext.of(client.level), client.player, TooltipFlag.NORMAL)) {
            if (line.getString().equals(text)) return;
        }
        problems.append(item).append(" has no tooltip line '").append(text).append("'; ");
    }
}
