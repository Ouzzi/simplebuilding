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

    public static void registerTab() {
        TAB = Registry.register(BuiltInRegistries.CREATIVE_MODE_TAB, SimpleStorage.id("storage"), CreativeModeTab.builder(CreativeModeTab.Row.TOP, 0)
                .title(Component.translatable("itemgroup.simplestorage.storage"))
                .icon(() -> new ItemStack(StorageItems.CRATES.get(com.simplestorage.crate.CrateWood.OAK)))
                .displayItems((parameters, output) -> tabStacks().forEach(output::accept))
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
