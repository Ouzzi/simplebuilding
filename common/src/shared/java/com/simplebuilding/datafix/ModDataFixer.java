package com.simplebuilding.datafix;

import com.mojang.datafixers.DSL;
import com.mojang.datafixers.DataFixer;
import com.mojang.serialization.Dynamic;
import java.util.Map;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.ListTag;
import net.minecraft.nbt.NbtOps;
import net.minecraft.nbt.Tag;
import net.minecraft.util.datafix.fixes.References;

/**
 * Carries the mod's own saved data through Minecraft's data fixers when a world from an older
 * Minecraft version is loaded (26.2 -> 26.3 above all).
 *
 * <p><b>Why this exists.</b> Vanilla's DataFixerUpper only knows vanilla types. A chunk that holds a
 * {@code simplebuilding:mod_hopper} gets its vanilla parts upgraded, but the hopper's
 * {@code Items} list passes through untouched, because the fixer has no schema for that block
 * entity id. The same holds for mod entities, and for anything inside the mod's own item component
 * {@code simplebuilding:backpack_contents}. On 26.3 that is not academic: 26.3 renamed the explorer
 * maps into items of their own (26.2: {@code minecraft:filled_map} with an explorer decoration;
 * 26.3: {@code minecraft:ocean_monument_map} ...), turned
 * {@code minecraft:pot_decorations} from a list into a map, dropped {@code minecraft:map_color} and
 * renamed the block-state fields {@code Name}/{@code Properties} to {@code id}/{@code properties}.
 * Without this pass an old explorer map in a mod hopper or a backpack does not decode on 26.3 and
 * is lost; a backpack's whole contents list failed with it; a rising block turned into sand.
 *
 * <p><b>How.</b> {@code DataFixTypesMixin} calls {@link #afterVanilla} right after vanilla fixed a
 * chunk, an entity chunk, a player, a structure or a saved-data file - with the very same version
 * range vanilla used, so nothing is guessed. This pass then walks the fixed tree and runs the
 * vanilla fixer once more over each piece of mod data, dressed up as the vanilla thing it is
 * shaped like:
 * <ul>
 *   <li>mod hoppers / furnaces / blast furnaces / smokers as {@code minecraft:hopper} /
 *       {@code furnace} / {@code blast_furnace} / {@code smoker} (their classes extend or mirror
 *       those, so every vanilla fix to them applies 1:1); the hopper's extra
 *       {@code GhostItems.Items} as item stacks;</li>
 *   <li>the backpack block entity's {@code Contents} and every
 *       {@code simplebuilding:backpack_contents} component entry as item stacks
 *       ({@code slot} rides along in the remainder);</li>
 *   <li>the rising block ({@code simplebuilding:levitating_block}) as {@code minecraft:falling_block}
 *       - its {@code BlockState} and {@code TileEntityData}.</li>
 * </ul>
 * Everything else the mod stores (octant corners, ore detector target, blueprint code, survival
 * counters, owned lights, sledgehammer progress, blueprint jobs) is either plain numbers/strings in
 * a mod-own format or already read tolerantly (ore detector), see docs/UPGRADE-26.2-26.3.md.
 *
 * <p>Nothing here runs for data that is already current ({@code from >= to}), so a world that was
 * never upgraded costs nothing. After the vanilla fix the walk recurses into the fixed pieces
 * as well, so a backpack inside a mod hopper inside a structure file is reached too.
 */
public final class ModDataFixer {

    private static final String NS = "simplebuilding:";

    /** Mod block entity id -> the vanilla block entity id it is shaped like. */
    private static final Map<String, String> BLOCK_ENTITY_ALIASES = Map.of(
            NS + "mod_hopper", "minecraft:hopper",
            NS + "mod_furnace", "minecraft:furnace",
            NS + "mod_blast_furnace", "minecraft:blast_furnace",
            NS + "mod_smoker", "minecraft:smoker");

    static final String BACKPACK_BLOCK_ENTITY = NS + "backpack";
    static final String BACKPACK_CONTENTS_COMPONENT = NS + "backpack_contents";
    static final String LEVITATING_BLOCK = NS + "levitating_block";

    private ModDataFixer() {
    }

    /**
     * Post-pass after vanilla's {@code DataFixTypes#update}. Returns the (possibly replaced) value;
     * anything that is not NBT (options, advancements JSON, ...) is returned unchanged - the mod
     * stores no items there.
     */
    @SuppressWarnings("unchecked")
    public static <T> Dynamic<T> afterVanilla(DataFixer fixer, Dynamic<T> fixed, int from, int to) {
        if (from >= to || !(fixed.getValue() instanceof Tag root)) {
            return fixed;
        }
        Tag result = walk(fixer, root, from, to);
        return result == root ? fixed : new Dynamic<>(fixed.getOps(), (T) result);
    }

    /** Walks a tree, fixing mod data in place where possible; returns the replacement for {@code tag}. */
    static Tag walk(DataFixer fixer, Tag tag, int from, int to) {
        if (tag instanceof ListTag list) {
            for (int i = 0; i < list.size(); i++) {
                Tag child = list.get(i);
                Tag replaced = walk(fixer, child, from, to);
                if (replaced != child) {
                    list.set(i, replaced);
                }
            }
            return list;
        }
        if (!(tag instanceof CompoundTag compound)) {
            return tag;
        }
        CompoundTag current = fixOwnShape(fixer, compound, from, to);
        for (String key : current.keySet().toArray(new String[0])) {
            Tag child = current.get(key);
            if (key.equals(BACKPACK_CONTENTS_COMPONENT) && child instanceof ListTag entries) {
                fixItemStacks(fixer, entries, from, to);
            }
            Tag replaced = walk(fixer, child, from, to);
            if (replaced != child) {
                current.put(key, replaced);
            }
        }
        return current;
    }

    /** Fixes the compound itself if it is a mod block entity or mod entity. */
    private static CompoundTag fixOwnShape(DataFixer fixer, CompoundTag compound, int from, int to) {
        String id = compound.getStringOr("id", "");
        if (!id.startsWith(NS)) {
            return compound;
        }
        String alias = BLOCK_ENTITY_ALIASES.get(id);
        // An item stack of the same block never carries these keys; its id is the block's item id anyway.
        if (alias != null && !compound.contains("count")) {
            CompoundTag fixed = fixAs(fixer, References.BLOCK_ENTITY, compound, id, alias, from, to);
            fixed.getCompound("GhostItems").flatMap(ghost -> ghost.getList("Items"))
                    .ifPresent(ghostItems -> fixItemStacks(fixer, ghostItems, from, to));
            return fixed;
        }
        if (id.equals(BACKPACK_BLOCK_ENTITY) && !compound.contains("count")) {
            compound.getList("Contents").ifPresent(entries -> fixItemStacks(fixer, entries, from, to));
            return compound;
        }
        if (id.equals(LEVITATING_BLOCK) && compound.contains("BlockState")) {
            return fixAs(fixer, References.ENTITY, compound, id, "minecraft:falling_block", from, to);
        }
        return compound;
    }

    /** Runs the vanilla fix over {@code compound} as if its id were {@code vanillaId}, then puts the mod id back. */
    private static CompoundTag fixAs(DataFixer fixer, DSL.TypeReference type, CompoundTag compound,
                                     String modId, String vanillaId, int from, int to) {
        CompoundTag disguised = compound.copy();
        disguised.putString("id", vanillaId);
        Tag fixed = fixer.update(type, new Dynamic<>(NbtOps.INSTANCE, disguised), from, to).getValue();
        if (!(fixed instanceof CompoundTag result)) {
            return compound;
        }
        result.putString("id", modId);
        return result;
    }

    /** Fixes every compound in {@code entries} as a vanilla item stack, in place. */
    private static void fixItemStacks(DataFixer fixer, ListTag entries, int from, int to) {
        for (int i = 0; i < entries.size(); i++) {
            if (entries.get(i) instanceof CompoundTag entry) {
                Tag fixed = fixer.update(References.ITEM_STACK, new Dynamic<>(NbtOps.INSTANCE, entry), from, to).getValue();
                if (fixed instanceof CompoundTag && fixed != entry) {
                    entries.set(i, fixed);
                }
            }
        }
    }
}
