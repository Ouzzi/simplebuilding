package com.simplebuilding.woodwork;

import java.util.Locale;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jspecify.annotations.Nullable;

/**
 * The motifs of carved wood: one per Vanilla pottery sherd ({@code minecraft:<name>_pottery_sherd}). The picture is
 * the sherd's decorated-pot pattern, recoloured at resource load (assets/minecraft/atlases/blocks.json,
 * {@code paletted_permutations}).
 */
public enum SherdMotif implements StringRepresentable {
    ANGLER, ARCHER, ARMS_UP, BLADE, BREWER, BURN, DANGER, EXPLORER, FLOW, FRIEND, GUSTER, HEART, HEARTBREAK, HOWL,
    MINER, MOURNER, PLENTY, PRIZE, SCRAPE, SHEAF, SHELTER, SKULL, SNORT;

    private final String name = name().toLowerCase(Locale.ROOT);

    @Override
    public String getSerializedName() {
        return this.name;
    }

    /** The sherd that carves this motif. */
    public Item sherd() {
        return BuiltInRegistries.ITEM.getValue(Identifier.withDefaultNamespace(this.name + "_pottery_sherd"));
    }

    /** The motif of a sherd stack, or null for anything else. */
    public static @Nullable SherdMotif of(ItemStack stack) {
        if (stack.isEmpty()) {
            return null;
        }
        Identifier id = BuiltInRegistries.ITEM.getKey(stack.getItem());
        if (!"minecraft".equals(id.getNamespace()) || !id.getPath().endsWith("_pottery_sherd")) {
            return null;
        }
        String motif = id.getPath().substring(0, id.getPath().length() - "_pottery_sherd".length());
        for (SherdMotif value : values()) {
            if (value.name.equals(motif)) {
                return value;
            }
        }
        return null;
    }
}
