package com.simplebuilding.gametest;

import com.simplebuilding.component.BackpackContents;
import com.simplebuilding.util.StorageRefs;
import net.minecraft.core.component.DataComponentGetter;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.resources.RegistryOps;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;

/**
 * The backpacks for SimpleBuilding's tests on both lines (split stage 8c-2, docs/ai/PLAN-B-S8C2-STORAGE-2026-10-21.md): on
 * 26.3 they are Simple Storage's ({@code simplestorage:*}), on 26.2 still SimpleBuilding's. Items and blocks by id
 * ({@link StorageRefs}); the contents component by id as well, read and written through its codec. SimpleBuilding's own
 * {@link BackpackContents} only serves as the reader of that saved form - both mods save the same entries
 * ({@code slot}, {@code id}, {@code count}, {@code components}), the module's class is never imported.
 */
final class StorageBackpacks {
    private StorageBackpacks() {
    }

    /** {@code ""}, {@code "reinforced"}, {@code "netherite"}, {@code "enderite"} -> the backpack item. */
    static Item item(String tier) {
        return StorageRefs.item(path(tier));
    }

    static Block block(String tier) {
        return StorageRefs.block(path(tier));
    }

    private static String path(String tier) {
        return tier.isEmpty() ? "backpack" : tier + "_backpack";
    }

    @SuppressWarnings("unchecked")
    static DataComponentType<Object> contentsType() {
        return (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(StorageRefs.id("backpack_contents"));
    }

    private static RegistryOps<Tag> ops(GameTestHelper helper) {
        return helper.getLevel().registryAccess().createSerializationContext(NbtOps.INSTANCE);
    }

    /** The contents of {@code holder} (an item stack or a placed backpack's components) in the saved form, or null. */
    static BackpackContents read(GameTestHelper helper, DataComponentGetter holder) {
        DataComponentType<Object> type = contentsType();
        Object value = type == null ? null : holder.get(type);
        if (value == null) return null;
        Tag saved = type.codecOrThrow().encodeStart(ops(helper), value).getOrThrow();
        return BackpackContents.CODEC.parse(ops(helper), saved).getOrThrow();
    }

    /** Sets {@code contents} on the backpack {@code stack} (empty contents: no component, like the mod). */
    static ItemStack write(GameTestHelper helper, ItemStack stack, BackpackContents contents) {
        DataComponentType<Object> type = contentsType();
        if (contents.isEmpty()) {
            stack.remove(type);
            return stack;
        }
        Tag saved = BackpackContents.CODEC.encodeStart(ops(helper), contents).getOrThrow();
        stack.set(type, type.codecOrThrow().parse(ops(helper), saved).getOrThrow());
        return stack;
    }

    /** A backpack of {@code tier} holding {@code stacks} in component slots 0, 1, 2, ... */
    static ItemStack with(GameTestHelper helper, String tier, ItemStack... stacks) {
        java.util.List<BackpackContents.Entry> entries = new java.util.ArrayList<>();
        for (int i = 0; i < stacks.length; i++) entries.add(BackpackContents.Entry.of(i, stacks[i]));
        return write(helper, new ItemStack(item(tier)), new BackpackContents(entries));
    }

    /** Everything of {@code item} in the backpack. */
    static int count(GameTestHelper helper, ItemStack backpack, Item item) {
        BackpackContents contents = read(helper, backpack);
        int total = 0;
        if (contents != null) for (ItemStack stack : contents.stacks()) if (stack.is(item)) total += stack.getCount();
        return total;
    }
}
