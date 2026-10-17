package com.simplebuilding.modules.simpletweaks.admin;

import net.minecraft.core.HolderLookup;
import net.minecraft.nbt.*;
import net.minecraft.world.item.ItemStack;

/** Item stacks to and from NBT/SNBT with the server's registries (components included). */
public final class AdminCodecs {
    public static Tag save(HolderLookup.Provider registries, ItemStack stack) {
        return ItemStack.OPTIONAL_CODEC.encodeStart(registries.createSerializationContext(NbtOps.INSTANCE), stack).getOrThrow();
    }
    public static ItemStack load(HolderLookup.Provider registries, Tag tag) {
        if (tag == null) return ItemStack.EMPTY;
        return ItemStack.OPTIONAL_CODEC.parse(registries.createSerializationContext(NbtOps.INSTANCE), tag).result().orElse(ItemStack.EMPTY);
    }
    public static String snbt(HolderLookup.Provider registries, ItemStack stack) { return save(registries, stack).toString(); }
    public static ItemStack fromSnbt(HolderLookup.Provider registries, String text) {
        try { return load(registries, TagParser.parseCompoundFully(text)); } catch (Exception e) { return ItemStack.EMPTY; }
    }
    private AdminCodecs() {}
}
