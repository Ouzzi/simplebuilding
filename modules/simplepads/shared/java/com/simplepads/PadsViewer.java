package com.simplepads;

import com.simplelib.api.RecipeViewer;
import com.simplepads.block.PadsBlocks;
import com.simplepads.item.PadsItems;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.ItemLike;

/**
 * Simple Pads in the recipe viewer (JEI24, a-fix09b): one info page per pad family and plate, as SimpleBuilding showed them
 * before the split ({@code jei.simplebuilding.info.*}, now {@code jei.simplepads.info.*}), plus the line with what this
 * server set (stranger break time, chunk loader owner rule) from the pads' own tuning. Free of JEI classes, shown by
 * SimpleLib's JEI plugin, so also without SimpleBuilding. Registered from {@link SimplePads#init}.
 */
public final class PadsViewer {
    public static final String INFO = "jei.simplepads.info.";
    public static final String SERVER = "jei.simplepads.server.";

    private PadsViewer() {
    }

    public static void register() {
        RecipeViewer.info(SimplePads.MOD_ID, PadsViewer::pages);
    }

    public static List<RecipeViewer.Info> pages() {
        List<RecipeViewer.Info> out = new ArrayList<>();
        PadsConfig.Tuning tuning = SimplePads.config().tuning;
        String pads = seconds(tuning.strangerBreakProgress(false));
        Component padLine = Component.translatable(SERVER + "pads", pads);
        page(out, "elytra_pad", padLine, PadsBlocks.ELYTRA_PAD, PadsBlocks.NETHERITE_ELYTRA_PAD, PadsBlocks.ENDERITE_ELYTRA_PAD);
        page(out, "flypad", padLine, PadsBlocks.FLYPAD, PadsBlocks.REINFORCED_FLYPAD, PadsBlocks.STELLAR_FLYPAD);
        page(out, "spawn_teleporter", padLine, PadsBlocks.SPAWN_TELEPORTER, PadsBlocks.SPAWN_TELEPORTER_TIER_2, PadsBlocks.ENDERITE_SPAWN_TELEPORTER);
        page(out, "chunk_loader", Component.translatable(SERVER + "chunk_loader",
                        Component.translatable(SERVER + (tuning.chunkLoaderRequireOwnerOnline ? "chunk_loader.owner_online" : "chunk_loader.always")),
                        seconds(tuning.strangerBreakProgress(true))),
                PadsBlocks.CHUNK_LOADER, PadsBlocks.NETHERITE_CHUNK_LOADER, PadsBlocks.ENDERITE_CHUNK_LOADER);
        page(out, "launchpad", padLine, PadsBlocks.LAUNCHPAD, PadsBlocks.NETHERITE_LAUNCHPAD, PadsBlocks.ENDERITE_LAUNCHPAD);
        page(out, "potion_pad", padLine, PadsBlocks.POTION_PAD, PadsBlocks.REINFORCED_POTION_PAD, PadsBlocks.INFUSED_POTION_PAD);
        page(out, "diamond_pressure_plate", null, PadsBlocks.DIAMOND_PRESSURE_PLATE);
        page(out, "netherite_pressure_plate", null, PadsBlocks.NETHERITE_PRESSURE_PLATE);
        page(out, "enderite_pressure_plate", null, PadsBlocks.ENDERITE_PRESSURE_PLATE);
        page(out, "copper_pressure_plate", null, PadsBlocks.COPPER_PRESSURE_PLATE, PadsBlocks.EXPOSED_COPPER_PRESSURE_PLATE,
                PadsBlocks.WEATHERED_COPPER_PRESSURE_PLATE, PadsBlocks.OXIDIZED_COPPER_PRESSURE_PLATE, PadsBlocks.WAXED_COPPER_PRESSURE_PLATE,
                PadsBlocks.WAXED_EXPOSED_COPPER_PRESSURE_PLATE, PadsBlocks.WAXED_WEATHERED_COPPER_PRESSURE_PLATE,
                PadsBlocks.WAXED_OXIDIZED_COPPER_PRESSURE_PLATE);
        page(out, "spawn_elytra", null, PadsItems.SPAWN_ELYTRA);
        return out;
    }

    private static void page(List<RecipeViewer.Info> out, String key, Component serverLine, ItemLike... items) {
        List<ItemStack> stacks = new ArrayList<>();
        for (ItemLike item : items) {
            if (item != null && !new ItemStack(item).isEmpty()) stacks.add(new ItemStack(item));
        }
        if (stacks.isEmpty()) return;
        List<Component> lines = new ArrayList<>(List.of(Component.translatable(INFO + key)));
        if (serverLine != null) lines.add(serverLine);
        out.add(new RecipeViewer.Info(stacks, lines));
    }

    /** Progress per tick as whole seconds until broken (as SimpleBuilding's ServerTuningInfo). */
    private static String seconds(float progressPerTick) {
        return progressPerTick <= 0 ? "-" : String.valueOf(Math.round(1.0f / (progressPerTick * 20.0f)));
    }
}
