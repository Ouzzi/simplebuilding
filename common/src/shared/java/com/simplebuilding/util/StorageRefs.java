package com.simplebuilding.util;

import com.simplebuilding.version.McVersion;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.level.block.Block;

/**
 * The storage blocks that left SimpleBuilding, by id (split stage 8a, docs/ai/PLAN-B-S8-STORAGE-2026-10-19.md): the
 * crates of every wood and the Storage Crafting Table belong to the required, bundled sub-mod Simple Storage
 * ({@code simplestorage:*}) and SimpleBuilding imports none of its classes (principle 3): tabs, the search tab, the test
 * centre and the tests find them in the registry. Both exist on 26.3 only. Call after registration (registry lookups);
 * an unregistered id gives air.
 *
 * <p>Split stage 8b (docs/ai/PLAN-B-S8B-STORAGE-2026-10-19.md): the tiered and trapped chests, the trapped copper chests
 * and the tier shulker boxes followed. They exist on both lines: on 26.2 SimpleBuilding still registers them
 * ({@code McVersion.STORAGE_IN_SB}, {@code simplebuilding:*}), so these lookups work everywhere.
 *
 * <p>Split stage 8b-2 (docs/ai/PLAN-B-S8C-STORAGE-2026-10-20.md): the chest carts and chest boats (26.3 only,
 * {@code McVersion.TIERED_VEHICLES}) and their wood component {@code boat_wood}.
 *
 * <p>Split stage 8c-1 (docs/ai/PLAN-B-S8C1-STORAGE-2026-10-20.md): the bundles, the quivers and the placed bundle (both lines;
 * 26.2 keeps them in SimpleBuilding). SimpleBuilding's own features on them (Master Builder in the wand and the pick key,
 * the back layer) recognise them by id and work through Vanilla's {@code bundle_contents}.
 */
public final class StorageRefs {
    public static final String STORAGE_CRAFTING_TABLE = "storage_crafting_table";

    private StorageRefs() {
    }

    public static Identifier id(String path) {
        return Identifier.fromNamespaceAndPath(McVersion.STORAGE_NAMESPACE, path);
    }

    public static Block block(String path) {
        return BuiltInRegistries.BLOCK.getValue(id(path));
    }

    public static Item item(String path) {
        return BuiltInRegistries.ITEM.getValue(id(path));
    }

    /** Tier names of the chests and shulker boxes, in tier order. */
    public static final java.util.List<String> TIERS = java.util.List.of("reinforced", "netherite", "enderite");

    /** The tiered chest of {@code tier} ({@code "reinforced"}, {@code "netherite"}, {@code "enderite"}). */
    public static Block chestBlock(String tier) {
        return block(tier + "_chest");
    }

    public static Item chest(String tier) {
        return item(tier + "_chest");
    }

    /** The trapped twin of a tiered chest; air where the line has none ({@code McVersion.TRAPPED_TIERED_CHESTS}). */
    public static Block trappedChestBlock(String tier) {
        return block(tier + "_trapped_chest");
    }

    public static Item trappedChest(String tier) {
        return item(tier + "_trapped_chest");
    }

    public static Block shulkerBoxBlock(String tier) {
        return block(tier + "_shulker_box");
    }

    public static Item shulkerBox(String tier) {
        return item(tier + "_shulker_box");
    }

    /** The eight trapped copper chests (oxidation order, then waxed); empty where the line has none. */
    public static java.util.List<Item> trappedCopperChests() {
        if (!McVersion.TRAPPED_TIERED_CHESTS) return java.util.List.of();
        java.util.List<Item> out = new java.util.ArrayList<>();
        for (String waxed : java.util.List.of("", "waxed_")) {
            for (String stage : java.util.List.of("", "exposed_", "weathered_", "oxidized_")) out.add(item(waxed + stage + "trapped_copper_chest"));
        }
        return out;
    }

    /** The crate of a wood ({@code "oak"}, {@code "ashwood"}); air when it is not registered. */
    public static Item crate(String wood) {
        return item(wood + "_crate");
    }

    /** The chest cart of a tier ({@code "reinforced"}...); air where the line has no tiered vehicles. */
    public static Item chestMinecart(String tier) {
        return item(tier + "_chest_minecart");
    }

    /** The chest boat of a tier (its wood is the component {@link #boatWood()}); air where the line has none. */
    public static Item chestBoat(String tier) {
        return item(tier + "_chest_boat");
    }

    /** Vanilla's boat woods of 26.3 in tab order, then Simple End's (only while it is loaded): the woods a chest boat takes. */
    public static java.util.List<String> boatWoods() {
        java.util.List<String> out = new java.util.ArrayList<>(java.util.List.of("oak", "spruce", "birch", "jungle", "acacia",
                "cherry", "dark_oak", "pale_oak", "mangrove", "poplar", "bamboo"));
        if (com.simplebuilding.platform.ModEnvironment.isModLoaded("simpleend")) out.addAll(java.util.List.of("ashwood", "duskwood"));
        return out;
    }

    /** The wood component of the chest boats ({@code <storage>:boat_wood}); null where it is not registered. */
    @SuppressWarnings("unchecked")
    public static net.minecraft.core.component.DataComponentType<String> boatWood() {
        return (net.minecraft.core.component.DataComponentType<String>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(id("boat_wood"));
    }

    /** A chest boat stack of {@code wood} (without the component where it is missing). */
    public static net.minecraft.world.item.ItemStack chestBoatStack(Item boat, String wood) {
        net.minecraft.world.item.ItemStack stack = new net.minecraft.world.item.ItemStack(boat);
        var component = boatWood();
        if (component != null) stack.set(component, wood);
        return stack;
    }

    /** Split stage 8c-1: the bundles, then the quivers, in tier order (item paths). */
    public static final java.util.List<String> BUNDLES = java.util.List.of("reinforced_bundle", "netherite_bundle", "enderite_bundle",
            "quiver", "reinforced_quiver", "netherite_quiver", "enderite_quiver");

    /** The item key of {@code path} in the storage namespace (datagen tags name the moved items by id). */
    public static net.minecraft.resources.ResourceKey<Item> itemKey(String path) {
        return net.minecraft.resources.ResourceKey.create(net.minecraft.core.registries.Registries.ITEM, id(path));
    }

    /** The bundles and quivers in {@link #BUNDLES} order (air where one is not registered). */
    public static java.util.List<Item> bundles() {
        return BUNDLES.stream().map(StorageRefs::item).toList();
    }

    /** A mod bundle or quiver (a {@code BundleItem} of the storage namespace); never Vanilla's bundles. */
    public static boolean isStorageBundle(net.minecraft.world.item.ItemStack stack) {
        return !stack.isEmpty() && stack.getItem() instanceof net.minecraft.world.item.BundleItem
                && McVersion.STORAGE_NAMESPACE.equals(BuiltInRegistries.ITEM.getKey(stack.getItem()).getNamespace());
    }

    /** One of the four quivers. */
    public static boolean isQuiver(net.minecraft.world.item.ItemStack stack) {
        return isStorageBundle(stack) && BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath().endsWith("quiver");
    }

    /** The placed bundle block (air where it is not registered). */
    public static Block placedBundle() {
        return block("placed_bundle");
    }
}
