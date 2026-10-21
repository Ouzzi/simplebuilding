package com.simplestorage.registry;

import com.simplestorage.SimpleStorage;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Loader-neutral registration order: blocks, items, block entities (per loader), menus, creative tab. */
public final class StorageRegistry {
    public static CreativeModeTab TAB;
    /** Split stage 8c-1: shaped upgrade that keeps every component of the bundle or quiver in the grid. */
    public static final net.minecraft.world.item.crafting.RecipeSerializer<com.simplestorage.bundle.ReinforcedBundleRecipe> REINFORCED_BUNDLE_SERIALIZER =
            new net.minecraft.world.item.crafting.RecipeSerializer<>(com.simplestorage.bundle.ReinforcedBundleRecipe.MAP_CODEC, com.simplestorage.bundle.ReinforcedBundleRecipe.STREAM_CODEC);

    /** In the RECIPE_SERIALIZER phase (Fabric: init); 8c-2: the backpack upgrade that keeps the backpack's components. */
    public static void registerSerializers() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SimpleStorage.id("reinforced_bundle"), REINFORCED_BUNDLE_SERIALIZER);
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, SimpleStorage.id("backpack_upgrade"), com.simplestorage.backpack.BackpackUpgradeRecipe.SERIALIZER);
    }

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleStorage.id("storage"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simplestorage.storage"))
                .icon(() -> new ItemStack(StorageItems.CRATES.get(com.simplestorage.crate.CrateWood.OAK)))
                .displayItems((parameters, output) -> {
                    tabStacks().forEach(output::accept);
                    // Split stage 8c-1: the storage enchantments as books at their highest level.
                    if (SimpleStorage.on()) parameters.holders().lookup(net.minecraft.core.registries.Registries.ENCHANTMENT).ifPresent(enchantments -> {
                        for (var key : List.of(com.simplestorage.bundle.BundleEnchantments.DEEP_POCKETS, com.simplestorage.bundle.BundleEnchantments.FUNNEL,
                                com.simplestorage.bundle.BundleEnchantments.DRAWER)) {
                            enchantments.get(key).ifPresent(holder -> {
                                ItemStack book = new ItemStack(net.minecraft.world.item.Items.ENCHANTED_BOOK);
                                var stored = new net.minecraft.world.item.enchantment.ItemEnchantments.Mutable(net.minecraft.world.item.enchantment.ItemEnchantments.EMPTY);
                                stored.upgrade(holder, holder.value().getMaxLevel());
                                book.set(net.minecraft.core.component.DataComponents.STORED_ENCHANTMENTS, stored.toImmutable());
                                output.accept(book);
                            });
                        }
                    });
                })
                .build());
    }

    /** The table, the crates, the chests, the shulker boxes, the chest carts and chest boats (oak), the guide (empty while the module is switched off: Vanilla hides an empty tab). */
    public static List<ItemStack> tabStacks() {
        List<ItemStack> out = new ArrayList<>();
        if (!SimpleStorage.on()) return out;
        out.add(new ItemStack(StorageItems.STORAGE_CRAFTING_TABLE));
        out.addAll(crateStacks());
        out.addAll(chestStacks());
        out.addAll(shulkerBoxStacks());
        out.addAll(chestMinecartStacks());
        out.addAll(chestBoatStacks());
        out.addAll(bundleStacks());
        out.addAll(backpackStacks());
        out.add(com.simplestorage.guide.StorageGuide.stack());
        return out;
    }

    public static List<ItemStack> crateStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Item crate : StorageItems.CRATES.values()) out.add(new ItemStack(crate));
        return out;
    }

    /** Tiered chests, their trapped twins, then the trapped copper chests (oxidation order, then waxed). */
    public static List<ItemStack> chestStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Item item : List.of(StorageItems.REINFORCED_CHEST, StorageItems.NETHERITE_CHEST, StorageItems.ENDERITE_CHEST,
                StorageItems.REINFORCED_TRAPPED_CHEST, StorageItems.NETHERITE_TRAPPED_CHEST, StorageItems.ENDERITE_TRAPPED_CHEST)) {
            out.add(new ItemStack(item));
        }
        for (Item item : StorageItems.TRAPPED_COPPER_CHESTS) out.add(new ItemStack(item));
        return out;
    }

    public static List<ItemStack> shulkerBoxStacks() {
        return List.of(new ItemStack(StorageItems.REINFORCED_SHULKER_BOX), new ItemStack(StorageItems.NETHERITE_SHULKER_BOX),
                new ItemStack(StorageItems.ENDERITE_SHULKER_BOX));
    }

    /** Split stage 8b-2: the chest carts in tier order. */
    public static List<ItemStack> chestMinecartStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Item item : StorageItems.CHEST_MINECARTS.values()) out.add(new ItemStack(item));
        return out;
    }

    /** The chest boats in tier order (oak; the wood is a component the recipes set). */
    public static List<ItemStack> chestBoatStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Item item : StorageItems.CHEST_BOATS.values()) out.add(new ItemStack(item));
        return out;
    }

    /** Split stage 8c-1: the bundles, then the quivers. */
    public static List<ItemStack> bundleStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Item item : StorageItems.bundles()) out.add(new ItemStack(item));
        return out;
    }

    /** Split stage 8c-2: the backpacks in tier order. */
    public static List<ItemStack> backpackStacks() {
        List<ItemStack> out = new ArrayList<>();
        for (Item item : StorageItems.backpacks()) out.add(new ItemStack(item));
        return out;
    }

    /**
     * Whether the module fills Vanilla's tabs itself: only on its own (SimpleBuilding's search-tab chains place the
     * crates behind each wood and the table behind the crafting table by id), only while switched on and allowed.
     */
    public static boolean fillsVanillaTabs(boolean simpleBuildingLoaded, boolean vanillaTabsAllowed) {
        return !simpleBuildingLoaded && vanillaTabsAllowed && SimpleStorage.on();
    }

    /** After registration (Fabric init, NeoForge/Forge common setup): dyed tier boxes wash in a water cauldron, dispensers place them. */
    public static void registerBehaviours() {
        for (Item box : com.simplestorage.chest.TieredShulkerBoxes.items()) {
            ((com.simplestorage.mixin.CauldronDispatcherAccessor) (Object) net.minecraft.core.cauldron.CauldronInteractions.WATER)
                    .simplestorage$put(box, com.simplestorage.chest.TieredShulkerBoxes.WASH);
        }
        com.simplestorage.chest.TieredShulkerBoxes.registerDispenserBehavior();
    }

    private StorageRegistry() {}
}
