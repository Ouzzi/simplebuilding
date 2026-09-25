package com.simplebuilding.blueprint;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.custom.BlueprintItem;
import com.simplebuilding.items.custom.OctantItem;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.AABB;

/**
 * Scannt die Auswahl eines Oktanten in eine Blaupause - am Kartentisch: Oktant links oben,
 * leere oder unsignierte Blaupause links unten, rechts kommt die gefuellte Blaupause heraus.
 *
 * <p>Regeln: die Auswahl (Bounding Box von Pos1/Pos2) ist hoechstens 128 Bloecke je Kante gross,
 * geladen, und der Kartentisch steht hoechstens {@value #MAX_TABLE_DISTANCE} Bloecke von ihr
 * entfernt. Luft wird uebersprungen, von Block-Entities wird nur der Zustand uebernommen (kein
 * Truheninhalt). Die Koordinaten beginnen bei der kleinsten belegten Stelle.
 */
public final class BlueprintScanner {
    public static final int MAX_TABLE_DISTANCE = 32;

    private BlueprintScanner() {
    }

    /** Ergebnis des Scans: entweder eine gefuellte Blaupause oder ein Grund fuer die Ablehnung. */
    public record Outcome(ItemStack result, Component error) {
        static Outcome fail(String key, Object... args) {
            return new Outcome(ItemStack.EMPTY, Component.translatable("simplebuilding.blueprint.scan." + key, args));
        }
    }

    public static boolean isOctant(ItemStack stack) {
        return stack.getItem() instanceof OctantItem;
    }

    /** Leere oder unsignierte Blaupause: die kann der Kartentisch (neu) beschreiben. */
    public static boolean isWritableBlueprint(ItemStack stack) {
        return stack.getItem() instanceof BlueprintItem
                && !stack.getOrDefault(ModDataComponentTypes.BLUEPRINT, BlueprintContent.EMPTY).signed();
    }

    public static Optional<BlockPos> corner(ItemStack octant, String key) {
        CompoundTag nbt = octant.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        return nbt.getIntArray(key).filter(a -> a.length == 3).map(a -> new BlockPos(a[0], a[1], a[2]));
    }

    public static Outcome scanAtTable(Level level, BlockPos table, ItemStack octant, ItemStack blueprint) {
        if (!isOctant(octant) || !isWritableBlueprint(blueprint)) {
            return Outcome.fail("wrong_items");
        }
        Optional<BlockPos> p1 = corner(octant, "Pos1");
        Optional<BlockPos> p2 = corner(octant, "Pos2");
        if (p1.isEmpty() || p2.isEmpty()) {
            return Outcome.fail("no_selection");
        }
        BlockPos min = BlockPos.min(p1.get(), p2.get());
        BlockPos max = BlockPos.max(p1.get(), p2.get());
        int sx = max.getX() - min.getX() + 1, sy = max.getY() - min.getY() + 1, sz = max.getZ() - min.getZ() + 1;
        if (sx > BlueprintCode.GRID || sy > BlueprintCode.GRID || sz > BlueprintCode.GRID) {
            return Outcome.fail("too_large", sx, sy, sz, BlueprintCode.GRID);
        }
        AABB reach = new AABB(min.getX(), min.getY(), min.getZ(), max.getX() + 1, max.getY() + 1, max.getZ() + 1)
                .inflate(MAX_TABLE_DISTANCE);
        if (!reach.contains(table.getX() + 0.5, table.getY() + 0.5, table.getZ() + 0.5)) {
            return Outcome.fail("too_far", MAX_TABLE_DISTANCE);
        }
        if (!level.hasChunksAt(min, max)) {
            return Outcome.fail("not_loaded");
        }
        BlueprintModel model = scan(level, min, max);
        if (model.isEmpty()) {
            return Outcome.fail("nothing");
        }
        String code = BlueprintCode.serialize(model);
        if (code.length() > BlueprintCode.MAX_CODE_LENGTH) {
            return Outcome.fail("too_complex", code.length(), BlueprintCode.MAX_CODE_LENGTH);
        }
        ItemStack result = blueprint.copyWithCount(1);
        BlueprintContent old = result.getOrDefault(ModDataComponentTypes.BLUEPRINT, BlueprintContent.EMPTY);
        result.set(ModDataComponentTypes.BLUEPRINT, new BlueprintContent(code, old.title(), "", false));
        result.remove(ModDataComponentTypes.BLUEPRINT_ROTATION);
        return new Outcome(result, null);
    }

    /** Alle Nicht-Luft-Bloecke der Box, auf die kleinste belegte Stelle verschoben. */
    public static BlueprintModel scan(Level level, BlockPos min, BlockPos max) {
        BlueprintModel model = new BlueprintModel();
        BlockPos.MutableBlockPos pos = new BlockPos.MutableBlockPos();
        for (int y = min.getY(); y <= max.getY(); y++) {
            for (int z = min.getZ(); z <= max.getZ(); z++) {
                for (int x = min.getX(); x <= max.getX(); x++) {
                    pos.set(x, y, z);
                    BlockState state = level.getBlockState(pos);
                    if (state.isAir() || state.is(Blocks.MOVING_PISTON)) {
                        continue;
                    }
                    model.set(x - min.getX(), y - min.getY(), z - min.getZ(), state);
                }
            }
        }
        return model.normalized();
    }
}
