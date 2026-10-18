package com.simplebuilding.util;

import com.simplebuilding.items.custom.OctantItem;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Predicate;
import net.minecraft.core.BlockPos;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.AABB;
import org.jetbrains.annotations.Nullable;

/**
 * The octant selection the sledgehammer breaks instead of its area (owner 2026-09-28). On 26.3 SimpleBuilding hands it to
 * Simple Hammers through SimpleLib {@code HammerActions.registerArea} ({@code LibServices.init}); the 26.2 hammer asks
 * {@code SledgehammerUtils#octantSelection}, which keeps its own copy of this rule.
 */
public final class OctantHammerArea {
    /** Groesste Oktant-Auswahl, die der Hammer bricht: so viele Stellen in der Box ... */
    public static final int OCTANT_MAX_VOLUME = 4096;
    /** ... und keine Kante laenger als so viele Bloecke. */
    public static final int OCTANT_MAX_EDGE = 32;

    private OctantHammerArea() {
    }

    /**
     * Die Oktant-Auswahl, die der Hammer bricht (Besitzer 2026-09-28): ein Oktant mit beiden Ecken in
     * der Nebenhand, der angeschlagene Block liegt in seiner Figur, und die Auswahl ist nicht groesser
     * als {@value #OCTANT_MAX_VOLUME} Stellen bei hoechstens {@value #OCTANT_MAX_EDGE} Bloecken
     * Kantenlaenge. Liefert die Stellen der Figur (der Ursprung zuerst) oder {@code null}.
     */
    public static @Nullable List<BlockPos> selection(Player player, BlockPos origin) {
        ItemStack octant = player.getOffhandItem();
        if (!(octant.getItem() instanceof OctantItem)) {
            return null;
        }
        AABB bounds = OctantShape.bounds(OctantShape.data(octant));
        if (bounds == null) {
            return null;
        }
        int sx = (int) Math.round(bounds.getXsize());
        int sy = (int) Math.round(bounds.getYsize());
        int sz = (int) Math.round(bounds.getZsize());
        if (sx > OCTANT_MAX_EDGE || sy > OCTANT_MAX_EDGE || sz > OCTANT_MAX_EDGE
                || (long) sx * sy * sz > OCTANT_MAX_VOLUME) {
            return null;
        }
        Predicate<BlockPos> inShape = OctantShape.of(octant);
        if (inShape == null || !inShape.test(origin)) {
            return null;
        }
        List<BlockPos> positions = new ArrayList<>();
        positions.add(origin.immutable());
        int minX = (int) Math.round(bounds.minX);
        int minY = (int) Math.round(bounds.minY);
        int minZ = (int) Math.round(bounds.minZ);
        for (int x = minX; x < minX + sx; x++) {
            for (int y = minY; y < minY + sy; y++) {
                for (int z = minZ; z < minZ + sz; z++) {
                    BlockPos pos = new BlockPos(x, y, z);
                    if (!pos.equals(origin) && inShape.test(pos)) {
                        positions.add(pos);
                    }
                }
            }
        }
        return positions;
    }

}
