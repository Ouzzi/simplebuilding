package com.simplestorage.test;

import com.simplelib.api.IdMoves;
import com.simplestorage.SimpleStorage;
import com.simplestorage.backpack.BackpackBlockEntity;
import com.simplestorage.backpack.BackpackContents;
import com.simplestorage.backpack.BackpackItem;
import com.simplestorage.backpack.BackpackTier;
import com.simplestorage.bundle.BundleEnchantments;
import com.simplestorage.registry.StorageBlockEntities;
import com.simplestorage.registry.StorageBlocks;
import com.simplestorage.registry.StorageComponents;
import com.simplestorage.registry.StorageItems;
import com.simplestorage.registry.StorageMenus;
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
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.entity.BlockEntity;

/**
 * Split stage 8c-2 (docs/ai/PLAN-B-S8C2-STORAGE-2026-10-21.md): the backpacks are registered, old {@code simplebuilding:}
 * worlds migrate (a filled, dyed, enchanted backpack stack and a placed backpack), the not-allowed tag and an unreadable
 * entry behave as before, and an accessory backpack counts as worn (SimpleLib AccessorySlots). Every test runs standalone
 * and with SimpleBuilding.
 */
final class BackpackStorageTests {
    private static final BlockPos SPOT = new BlockPos(2, 2, 2);

    private BackpackStorageTests() {
    }

    static void registered(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String path : SimpleStorage.movedBackpacks()) {
            if (!BuiltInRegistries.ITEM.containsKey(SimpleStorage.id(path))) problems.add("item " + path);
            if (!BuiltInRegistries.BLOCK.containsKey(SimpleStorage.id(path))) problems.add("block " + path);
        }
        for (BackpackTier tier : BackpackTier.values()) {
            Item item = StorageItems.BACKPACKS.get(tier);
            if (!(item instanceof BackpackItem backpack) || backpack.getTier() != tier) problems.add("item of " + tier + " is " + item);
            if (!StorageBlockEntities.BACKPACK.isValid(StorageBlocks.BACKPACKS.get(tier).defaultBlockState())) problems.add("block entity on " + tier);
            var equippable = new ItemStack(item).get(DataComponents.EQUIPPABLE);
            if (equippable == null || equippable.slot() != EquipmentSlot.CHEST || !equippable.swappable()) problems.add(item + " not worn in the chest slot");
            boolean metal = tier.ordinal() >= BackpackTier.NETHERITE.ordinal();
            if (new ItemStack(item).has(DataComponents.DAMAGE_RESISTANT) != metal) problems.add(item + " fire-proof " + !metal);
        }
        if (StorageComponents.BACKPACK_CONTENTS == null) problems.add("component backpack_contents");
        if (StorageMenus.BACKPACK == null) problems.add("menu backpack");
        if (!BuiltInRegistries.RECIPE_SERIALIZER.containsKey(SimpleStorage.id("backpack_upgrade"))) problems.add("recipe serializer backpack_upgrade");
        if (StorageRegistry.backpackStacks().size() != 4) problems.add("tab backpacks " + StorageRegistry.backpackStacks().size());
        h.assertTrue(problems.isEmpty(), "backpacks registered: " + problems);
        h.succeed();
    }

    static void migrationFromSimplebuilding(GameTestHelper h) {
        List<String> problems = new ArrayList<>();
        for (String path : SimpleStorage.movedBackpacks()) {
            if (BuiltInRegistries.ITEM.getValue(SimpleStorage.old(path)) != BuiltInRegistries.ITEM.getValue(SimpleStorage.id(path))) problems.add("item " + path);
            if (BuiltInRegistries.BLOCK.getValue(SimpleStorage.old(path)) != BuiltInRegistries.BLOCK.getValue(SimpleStorage.id(path))) problems.add("block " + path);
        }
        if (BuiltInRegistries.BLOCK_ENTITY_TYPE.getValue(SimpleStorage.old("backpack")) != StorageBlockEntities.BACKPACK) problems.add("block entity backpack");
        if (BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(SimpleStorage.old("backpack_contents")) != StorageComponents.BACKPACK_CONTENTS) {
            problems.add("component backpack_contents");
        }
        for (String path : SimpleStorage.MOVED_BACKPACK_RECIPES) {
            if (!SimpleStorage.id(path).equals(IdMoves.target(Registries.RECIPE, SimpleStorage.old(path)))) problems.add("recipe " + path);
            Identifier advancement = SimpleStorage.id("recipes/tools/" + path);
            if (!advancement.equals(IdMoves.advancement(SimpleStorage.old("recipes/tools/" + path)))) problems.add("advancement " + path);
        }
        // A filled, dyed Enderite backpack with Deep Pockets II of an old world (item, component, enchantment under the old ids).
        var ops = h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
        ItemStack backpack = ItemStack.CODEC.parse(ops, oldBackpack()).result().orElse(ItemStack.EMPTY);
        if (!backpack.is(StorageItems.BACKPACKS.get(BackpackTier.ENDERITE))) problems.add("old backpack loads as " + backpack);
        if (BundleEnchantments.level(backpack, h.getLevel(), BundleEnchantments.DEEP_POCKETS) != 2) problems.add("old backpack lost Deep Pockets II: " + backpack.getEnchantments());
        var colour = backpack.get(DataComponents.DYED_COLOR);
        if (colour == null || colour.rgb() != 0x3355AA) problems.add("old backpack colour " + colour);
        BackpackContents contents = BackpackItem.contents(backpack);
        if (contents.size() != 2 || !contents.stackAt(0).is(Items.COBBLESTONE) || contents.stackAt(0).getCount() != 200
                || contents.entries().get(1).slot() != 36 || !contents.stackAt(1).is(Items.DIAMOND)) {
            problems.add("old backpack contents " + contents);
        }
        // A placed backpack of an old world: block entity simplebuilding:backpack with "Contents" and "Color".
        CompoundTag placed = new CompoundTag();
        placed.putString("id", "simplebuilding:backpack");
        placed.put("Contents", oldEntries());
        placed.putInt("Color", 0x3355AA);
        BlockEntity be = BlockEntity.loadStatic(h.absolutePos(SPOT), StorageBlocks.BACKPACKS.get(BackpackTier.ENDERITE).defaultBlockState(), placed,
                h.getLevel().registryAccess());
        if (!(be instanceof BackpackBlockEntity loaded) || loaded.dyeColor() != 0x3355AA || loaded.container().toContents().size() != 2) {
            problems.add("old placed backpack loads as " + be);
        }
        h.assertTrue(problems.isEmpty(), "backpack migration: " + problems);
        h.succeed();
    }

    private static ListTag oldEntries() {
        ListTag entries = new ListTag();
        CompoundTag cobble = new CompoundTag();
        cobble.putInt("slot", 0);
        cobble.putString("id", "minecraft:cobblestone");
        cobble.putInt("count", 200);
        entries.add(cobble);
        CompoundTag diamond = new CompoundTag();
        diamond.putInt("slot", 36);
        diamond.putString("id", "minecraft:diamond");
        diamond.putInt("count", 3);
        entries.add(diamond);
        return entries;
    }

    private static CompoundTag oldBackpack() {
        CompoundTag stack = new CompoundTag();
        stack.putString("id", "simplebuilding:enderite_backpack");
        stack.putInt("count", 1);
        CompoundTag components = new CompoundTag();
        CompoundTag enchantments = new CompoundTag();
        enchantments.putInt("simplebuilding:deep_pockets", 2);
        components.put("minecraft:enchantments", enchantments);
        components.putInt("minecraft:dyed_color", 0x3355AA);
        components.put("simplebuilding:backpack_contents", oldEntries());
        stack.put("components", components);
        return stack;
    }

    /** Nothing from {@code simplestorage:not_allowed_in_backpack} goes in (SimpleBuilding's {@code ModpackTests} before 8c-2). */
    static void refusesTheNotAllowedTag(GameTestHelper h) {
        h.assertTrue(new ItemStack(Items.STRUCTURE_VOID).is(BackpackItem.NOT_ALLOWED), "structure void is missing from the not-allowed tag");
        h.assertTrue(!BackpackItem.mayStore(new ItemStack(Items.STRUCTURE_VOID)), "a backpack takes an item of the not-allowed tag");
        h.assertTrue(!BackpackItem.mayStore(new ItemStack(StorageItems.BACKPACKS.get(BackpackTier.BASIC))), "a backpack takes a backpack");
        h.assertTrue(!BackpackItem.mayStore(new ItemStack(Items.SHULKER_BOX)), "a backpack takes a shulker box");
        h.assertTrue(BackpackItem.mayStore(new ItemStack(Items.COBBLESTONE)), "a backpack refuses cobblestone");
        h.succeed();
    }

    /** One entry that no longer decodes (an unknown item) costs that entry, not the whole backpack. */
    static void unreadableEntryKeepsTheRest(GameTestHelper h) {
        ListTag list = oldEntries();
        CompoundTag bad = new CompoundTag();
        bad.putInt("slot", 5);
        bad.putString("id", "simplestorage:item_that_never_existed");
        bad.putInt("count", 1);
        list.add(0, bad);
        BackpackContents contents = BackpackContents.CODEC.parse(h.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE), list)
                .result().orElse(null);
        h.assertTrue(contents != null && contents.size() == 2, "expected the two readable entries, got " + contents);
        h.assertValueEqual(contents.stackAt(0).getCount(), 200, "count of the first readable entry");
        h.succeed();
    }

    /**
     * A backpack in an accessory slot counts as worn (SimpleLib AccessorySlots; Curios/Trinkets hooks are the module's since
     * 8c-2): after the chest slot, before the inventory - for the key, the funnel and SimpleLib CarriedStacks.
     */
    static void accessoryBackpackCountsAsWorn(GameTestHelper h) {
        var player = h.makeMockPlayer(GameType.SURVIVAL);
        ItemStack accessory = new ItemStack(StorageItems.BACKPACKS.get(BackpackTier.REINFORCED));
        accessory.set(StorageComponents.BACKPACK_CONTENTS, new BackpackContents(List.of(BackpackContents.Entry.of(0, new ItemStack(Items.ARROW, 5)))));
        com.simplelib.api.AccessorySlots.Hook hook = entity -> entity == player ? List.of(accessory) : List.of();
        com.simplelib.api.AccessorySlots.register(hook);
        h.runBeforeTestEnd(() -> com.simplelib.api.AccessorySlots.unregister(hook));
        try {
            player.getInventory().setItem(3, new ItemStack(StorageItems.BACKPACKS.get(BackpackTier.BASIC)));
            h.assertTrue(BackpackItem.wornBackpack(player) == accessory, "the accessory backpack is not the worn one");
            h.assertValueEqual(BackpackItem.carriedBackpackSlot(player), BackpackItem.ACCESSORY_SLOT, "slot the backpack key opens");
            var entries = BackpackItem.carriedEntries(player);
            h.assertTrue(entries.size() == 1 && entries.getFirst().container() == accessory && entries.getFirst().stack().is(Items.ARROW),
                    "CarriedStacks entries of the accessory backpack: " + entries);
            ItemStack left = entries.getFirst().stack().copy();
            left.shrink(2);
            entries.getFirst().write(left);
            h.assertValueEqual(BackpackItem.contents(accessory).stackAt(0).getCount(), 3, "arrows left after a CarriedStacks write-back");
            ItemStack chest = new ItemStack(StorageItems.BACKPACKS.get(BackpackTier.NETHERITE));
            player.setItemSlot(EquipmentSlot.CHEST, chest);
            h.assertTrue(BackpackItem.wornBackpack(player) == chest, "the chest slot does not come before the accessory slot");
        } finally {
            com.simplelib.api.AccessorySlots.unregister(hook);
        }
        h.succeed();
    }
}
