package com.simplemobheads;

import com.simplelib.api.IdMoves;
import com.simplemobheads.data.HeadDef;
import com.simplemobheads.data.HeadTable;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;

/**
 * Old ids of the heads that moved in (concept §4): Simple Fun's four animal heads ({@code simplefun:pig_head}, ...) and
 * SimpleBuilding's twelve ({@code simplebuilding:blaze_head}, {@code simplebuilding:stray_skull}, ...) - items, standing
 * and wall blocks (chunk palettes keep placed heads, their block entity is Vanilla's {@code minecraft:skull}) and Simple
 * Fun's collection advancements. SimpleLib's registry mixins answer a missed lookup of the old id with the new object; the
 * next save writes the new id. Registered in the mod constructor / initializer, before any world loads.
 */
public final class HeadMoves {
    private static boolean done;

    private HeadMoves() {
    }

    public static synchronized void register() {
        if (done) {
            return;
        }
        done = true;
        for (HeadDef head : HeadTable.HEADS) {
            if (head.from().isEmpty()) {
                continue;
            }
            Identifier item = Identifier.fromNamespaceAndPath(head.from(), head.item());
            IdMoves.move(Registries.ITEM, item, SimpleMobHeads.id(head.item()));
            IdMoves.move(Registries.BLOCK, item, SimpleMobHeads.id(head.item()));
            IdMoves.move(Registries.BLOCK, Identifier.fromNamespaceAndPath(head.from(), head.wall()), SimpleMobHeads.id(head.wall()));
            if (head.from().equals("simplefun")) {
                IdMoves.move(Registries.ADVANCEMENT, Identifier.fromNamespaceAndPath("simplefun", "content/" + head.item()),
                        SimpleMobHeads.id("heads/" + head.id()));
            }
        }
    }
}
