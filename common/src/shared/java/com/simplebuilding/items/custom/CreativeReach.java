package com.simplebuilding.items.custom;

import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;

/**
 * Unbegrenzte Reichweite der Kreativ-Werkzeuge (Queue Nachtrag 29): Kreativ-Baustab in der Haupthand, oder
 * ein Baustab mit einer Kreativ-Blaupause in der Nebenhand. Trifft die Vanilla-Reichweite nichts, schickt
 * Vanilla nur ein "Benutzen" in die Luft; der Stab wirft dann selbst einen Strahl bis {@link #FAR_REACH}
 * (Client fuer die Vorschau, Server fuer den Bau, je mit der eigenen Blickrichtung des Spielers).
 *
 * <p>1024 Bloecke liegen hinter jeder Sichtweite (hoechstens 32 Chunks = 512 Bloecke); dahinter ist die
 * Welt nicht geladen, und ein Strahl dorthin traefe ohnehin nur Luft. Deshalb ist die Grenze praktisch
 * unbegrenzt. Kein Attribut: Vanilla deckelt {@code block_interaction_range} bei 64.
 */
public final class CreativeReach {

    /** Weiteste Zielentfernung (Bloecke, ab dem Auge). */
    public static final double FAR_REACH = 1024.0;

    private CreativeReach() {
    }

    /** Ob der Spieler gerade mit unbegrenzter Reichweite baut. */
    public static boolean active(Player player) {
        ItemStack main = player.getMainHandItem();
        if (!(main.getItem() instanceof BuildingWandItem wand)) {
            return false;
        }
        return wand.isCreative() || BlueprintItem.isCreative(player.getOffhandItem());
    }

    /** Der Block im Blick bis {@link #FAR_REACH}, oder {@code null}, wenn der Strahl nichts trifft. */
    public static BlockHitResult farHit(Player player) {
        HitResult hit = player.pick(FAR_REACH, 1.0F, false);
        return hit instanceof BlockHitResult block && hit.getType() == HitResult.Type.BLOCK ? block : null;
    }
}
