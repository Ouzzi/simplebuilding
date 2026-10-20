package com.simplestorage.test;

import com.simplelib.api.IdMoves;
import com.simplestorage.SimpleStorage;
import com.simplestorage.bundle.BundleEnchantments;
import com.simplestorage.bundle.PlacedBundleBlockEntity;
import com.simplestorage.registry.StorageBlockEntities;
import com.simplestorage.registry.StorageBlocks;
import com.simplestorage.registry.StorageItems;
import com.simplestorage.registry.StorageRegistry;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Split stage 8c-1 (docs/ai/PLAN-B-S8C1-STORAGE-2026-10-20.md): the bundles, quivers, placed bundle and storage
 * enchantments are registered, old {@code simplebuilding:} worlds migrate (a filled, enchanted bundle stack and a placed
 * bundle), the recipes work on their own (Leather without SimpleBuilding, its Leather Sheet with it), and the dyeing is
 * Simple Storage's own. Every test runs standalone and with SimpleBuilding.
 */
final class BundleStorageTests {
    private static final BlockPos SPOT = new BlockPos(2, 2, 2);

    private BundleStorageTests() {
    }

    static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String path : SimpleStorage.movedBundles()) {
            if (!BuiltInRegistries.ITEM.containsKey(SimpleStorage.id(path))) problems.add("item " + path);
        }
        if (!BuiltInRegistries.BLOCK.containsKey(SimpleStorage.id("placed_bundle"))) problems.add("block placed_bundle");
        if (BuiltInRegistries.ITEM.containsKey(SimpleStorage.id("placed_bundle"))) problems.add("placed_bundle has an item");
        if (StorageBlockEntities.PLACED_BUNDLE == null || !StorageBlockEntities.PLACED_BUNDLE.isValid(StorageBlocks.PLACED_BUNDLE.defaultBlockState())) {
            problems.add("block entity placed_bundle");
        }
        if (!BuiltInRegistries.RECIPE_SERIALIZER.containsKey(SimpleStorage.id("reinforced_bundle"))) problems.add("recipe serializer reinforced_bundle");
        var enchantments = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (ResourceKey<Enchantment> key : List.of(BundleEnchantments.DRAWER, BundleEnchantments.DEEP_POCKETS, BundleEnchantments.FUNNEL)) {
            var holder = enchantments.get(key);
            if (holder.isEmpty()) {
                problems.add("enchantment " + key.identifier());
                continue;
            }
            // Every tier of both containers takes all three (an upgrade never takes an enchantment away).
            for (Item item : StorageItems.bundles()) {
                if (!holder.get().value().canEnchant(new ItemStack(item))) problems.add(key.identifier().getPath() + " refuses " + item);
            }
            if (holder.get().value().canEnchant(new ItemStack(Items.BUNDLE))) problems.add(key.identifier().getPath() + " takes Vanilla's bundle");
        }
        // Fire-proof and rarities as in SimpleBuilding: Netherite uncommon, Enderite epic, both fire-proof.
        for (Item item : List.of(StorageItems.NETHERITE_BUNDLE, StorageItems.ENDERITE_BUNDLE, StorageItems.NETHERITE_QUIVER, StorageItems.ENDERITE_QUIVER)) {
            if (!new ItemStack(item).has(DataComponents.DAMAGE_RESISTANT)) problems.add(item + " not fire-proof");
        }
        for (Item item : List.of(StorageItems.QUIVER, StorageItems.REINFORCED_QUIVER, StorageItems.NETHERITE_QUIVER, StorageItems.ENDERITE_QUIVER)) {
            var equippable = new ItemStack(item).get(DataComponents.EQUIPPABLE);
            if (equippable == null || equippable.slot() != net.minecraft.world.entity.EquipmentSlot.CHEST) problems.add(item + " not worn in the chest slot");
        }
        if (StorageRegistry.bundleStacks().size() != 7) problems.add("tab bundles " + StorageRegistry.bundleStacks().size());
        h.assertTrue(problems.isEmpty(), "bundles registered: " + problems);
        h.succeed();
    }

    static void migrationFromSimplebuilding(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String path : SimpleStorage.movedBundles()) {
            if (BuiltInRegistries.ITEM.getValue(SimpleStorage.old(path)) != BuiltInRegistries.ITEM.getValue(SimpleStorage.id(path))) problems.add("item " + path);
        }
        if (BuiltInRegistries.BLOCK.getValue(SimpleStorage.old("placed_bundle")) != StorageBlocks.PLACED_BUNDLE) problems.add("block placed_bundle");
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleStorage.old("placed_bundle")) != StorageBlockEntities.PLACED_BUNDLE) problems.add("block entity placed_bundle");
        for (String path : SimpleStorage.MOVED_BUNDLE_RECIPES) {
            if (!SimpleStorage.id(path).equals(IdMoves.target(Registries.RECIPE, SimpleStorage.old(path)))) problems.add("recipe " + path);
            Identifier advancement = SimpleStorage.id("recipes/tools/" + path);
            if (!advancement.equals(IdMoves.advancement(SimpleStorage.old("recipes/tools/" + path)))) problems.add("advancement " + path);
        }
        var enchantments = h.getLevel().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
        for (String path : SimpleStorage.MOVED_ENCHANTMENTS) {
            var old = enchantments.get(ResourceKey.create(Registries.ENCHANTMENT, SimpleStorage.old(path)));
            if (old.isEmpty() || !old.get().is(ResourceKey.create(Registries.ENCHANTMENT, SimpleStorage.id(path)))) problems.add("enchantment " + path + " -> " + old);
        }
        // A filled, enchanted Netherite bundle of an old world (item, enchantment and contents under the old ids).
        ItemStack bundle = ItemStack.CODEC.parse(h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), oldBundle()).result().orElse(ItemStack.EMPTY);
        if (!bundle.is(StorageItems.NETHERITE_BUNDLE)) problems.add("old bundle loads as " + bundle);
        if (BundleEnchantments.level(bundle, h.getLevel(), BundleEnchantments.DRAWER) != 2) problems.add("old bundle lost Drawer II: " + bundle.getEnchantments());
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.size() != 1 || !contents.items().getFirst().create().is(Items.COBBLESTONE)) problems.add("old bundle contents " + contents);
        // A placed bundle of an old world: block entity simplebuilding:placed_bundle with the bundle under "Bundle".
        CompoundTag placed = new CompoundTag();
        placed.putString("id", "simplebuilding:placed_bundle");
        placed.put("Bundle", oldBundle());
        BlockEntity be = BlockEntity.loadStatic(h.absolutePos(SPOT), StorageBlocks.PLACED_BUNDLE.defaultBlockState(), placed, h.getLevel().registryAccess());
        if (!(be instanceof PlacedBundleBlockEntity loaded) || !loaded.getBundle().is(StorageItems.NETHERITE_BUNDLE) || loaded.contents().size() != 1) {
            problems.add("old placed bundle loads as " + be);
        }
        h.assertTrue(problems.isEmpty(), "bundle migration: " + problems);
        h.succeed();
    }

    private static CompoundTag oldBundle() {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", "simplebuilding:netherite_bundle");
        stack.putInt("count", 1);
        CompoundTag components = new CompoundTag();
        CompoundTag enchantments = new CompoundTag();
        enchantments.putInt("simplebuilding:drawer", 2);
        components.put("minecraft:enchantments", enchantments);
        ListTag inside = new ListTag();
        CompoundTag cobble = new CompoundTag();
        cobble.putString("id", "minecraft:cobblestone");
        cobble.putInt("count", 40);
        inside.add(cobble);
        components.put("minecraft:bundle_contents", inside);
        stack.put("components", components);
        return stack;
    }

    /**
     * The upgrade path through the grid and the smithing table: quiver from Vanilla items, Reinforced from the plain one
     * (with SimpleBuilding's Leather Sheet when it is loaded, else Leather; the result keeps everything of the container),
     * Netherite (Vanilla template) and Enderite (SimpleLib's template and ingot). No recipe stays under simplebuilding.
     */
    static void recipesWorkOnTheirOwn(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        var level = h.getLevel();
        var manager = level.getServer().getRecipeManager();
        boolean sb = BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("simplebuilding", "leather_sheet"));
        for (String path : SimpleStorage.MOVED_BUNDLE_RECIPES) {
            if (manager.byKey(ResourceKey.create(Registries.RECIPE, SimpleStorage.id(path))).isEmpty() && (sb || !path.startsWith("reinforced_"))) problems.add("missing " + path);
            if (manager.byKey(ResourceKey.create(Registries.RECIPE, SimpleStorage.old(path))).isPresent()) problems.add("still under simplebuilding: " + path);
        }
        for (String path : List.of("reinforced_bundle_without_simplebuilding", "reinforced_quiver_without_simplebuilding")) {
            if (manager.byKey(ResourceKey.create(Registries.RECIPE, SimpleStorage.id(path))).isPresent() == sb) problems.add((sb ? "active with SimpleBuilding: " : "missing alone: ") + path);
        }
        ItemStack string = new ItemStack(Items.STRING);
        ItemStack pebble = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplelib", "diamond_pebble")));
        ItemStack sheet = sb ? new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplebuilding", "leather_sheet"))) : new ItemStack(Items.LEATHER);
        // Quiver: " SL" / "SLN" / "B  ".
        ItemStack quiver = craft(h, List.of(ItemStack.EMPTY, string, new ItemStack(Items.LEATHER), string, new ItemStack(Items.LEATHER),
                new ItemStack(Items.COPPER_NUGGET), new ItemStack(Items.BUNDLE), ItemStack.EMPTY, ItemStack.EMPTY));
        if (!quiver.is(StorageItems.QUIVER)) problems.add("quiver recipe gives " + quiver);
        // Reinforced bundle: "DSD" / "DBD" / "DXD"; the contents of the Vanilla bundle stay.
        ItemStack vanilla = new ItemStack(Items.BUNDLE);
        vanilla.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.of(net.minecraft.world.item.ItemStackTemplate.fromNonEmptyStack(new ItemStack(Items.FEATHER, 3)))));
        ItemStack reinforced = craft(h, List.of(pebble, string, pebble, pebble, vanilla, pebble, pebble, sheet, pebble));
        if (!reinforced.is(StorageItems.REINFORCED_BUNDLE)) problems.add("reinforced bundle recipe gives " + reinforced);
        else if (reinforced.get(DataComponents.BUNDLE_CONTENTS) == null || reinforced.get(DataComponents.BUNDLE_CONTENTS).size() != 1) problems.add("reinforced bundle lost the contents");
        ItemStack reinforcedQuiver = craft(h, List.of(pebble, string, pebble, pebble, new ItemStack(StorageItems.QUIVER), pebble, pebble, sheet, pebble));
        if (!reinforcedQuiver.is(StorageItems.REINFORCED_QUIVER)) problems.add("reinforced quiver recipe gives " + reinforcedQuiver);
        smith(h, problems, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), StorageItems.REINFORCED_BUNDLE, new ItemStack(Items.NETHERITE_INGOT), StorageItems.NETHERITE_BUNDLE);
        smith(h, problems, new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE), StorageItems.REINFORCED_QUIVER, new ItemStack(Items.NETHERITE_INGOT), StorageItems.NETHERITE_QUIVER);
        ItemStack template = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplelib", "enderite_upgrade_template")));
        ItemStack ingot = new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplelib", "enderite_ingot")));
        smith(h, problems, template, StorageItems.NETHERITE_BUNDLE, ingot, StorageItems.ENDERITE_BUNDLE);
        smith(h, problems, template, StorageItems.NETHERITE_QUIVER, ingot, StorageItems.ENDERITE_QUIVER);
        // Dyeing (26.3, like Vanilla's bundles): container + dye = one fixed colour, every component kept.
        ItemStack enchanted = new ItemStack(StorageItems.REINFORCED_QUIVER);
        enchanted.set(DataComponents.CUSTOM_NAME, net.minecraft.network.chat.Component.literal("Arrows"));
        ItemStack dyed = craft(h, List.of(enchanted, new ItemStack(Items.RED_DYE)));
        if (!dyed.is(StorageItems.REINFORCED_QUIVER) || !dyed.has(DataComponents.DYED_COLOR) || !dyed.has(DataComponents.CUSTOM_NAME)) problems.add("dyed quiver " + dyed);
        TagKey<Item> dyeable = TagKey.create(Registries.ITEM, SimpleStorage.id("dyeable_storage"));
        for (Item item : StorageItems.bundles()) if (!new ItemStack(item).is(dyeable)) problems.add(item + " not dyeable");
        h.assertTrue(problems.isEmpty(), "bundle recipes: " + problems);
        h.succeed();
    }

    private static ItemStack craft(GameTestHelper h, List<ItemStack> grid) {
        int size = grid.size() == 9 ? 3 : 2;
        List<ItemStack> cells = new ArrayList<>(grid);
        while (cells.size() < size * size) cells.add(ItemStack.EMPTY);
        CraftingInput input = CraftingInput.of(size, size, cells);
        var level = h.getLevel();
        return level.getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, level)
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
    }

    private static void smith(GameTestHelper h, List<String> problems, ItemStack template, Item base, ItemStack addition, Item result) {
        SmithingRecipeInput input = new SmithingRecipeInput(template, new ItemStack(base), addition);
        var level = h.getLevel();
        ItemStack out = level.getServer().getRecipeManager().getRecipeFor(RecipeType.SMITHING, input, level)
                .map(holder -> holder.value().assemble(input)).orElse(ItemStack.EMPTY);
        if (!out.is(result)) problems.add(base + " smiths into " + out + ", not " + result);
    }
}
