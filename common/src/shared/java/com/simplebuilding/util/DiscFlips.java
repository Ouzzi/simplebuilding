package com.simplebuilding.util;

import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.items.custom.SledgehammerItem;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.gameevent.GameEvent;

/**
 * B-Seiten der Schallplatten (Besitzer 2026-10-03, Easter Egg): eine abgelegte Platte der Mod (Schleichen + Rechtsklick,
 * sie steht im Tag {@code simplebuilding:placeable_small}, {@link PlacedSmallParts}) wird mit einem Rechtsklick des
 * Vorschlaghammers zu ihrer anderen Seite - A-Seite zur B-Seite und zurueck, endlos. Liegen mehrere Platten, trifft
 * es die zuletzt gelegte. Jeder Wechsel kostet den Hammer einen Punkt Haltbarkeit (wie eine Umformung,
 * {@link SledgehammerItem#RESHAPE_DAMAGE}) und braucht dieselben Rechte wie jede Umwandlung
 * ({@link TransformTargets#mayTransform}). Der Handhinweis fragt {@link #canFlip}, die Aktion fragt dasselbe.
 *
 * <p>Vanilla-Platten haben keine B-Seite: dafuer braeuchte es eigene Fassungen fremder Stuecke.
 */
public final class DiscFlips {
    private DiscFlips() {
    }

    /** Index der zuletzt gelegten wendbaren Platte im Haeufchen, oder -1. */
    public static int targetIndex(List<ItemStack> parts) {
        for (int i = parts.size() - 1; i >= 0; i--) {
            if (MusicDiscs.otherSide(parts.get(i).getItem()) != null) {
                return i;
            }
        }
        return -1;
    }

    /** Ob ein Rechtsklick mit {@code tool} auf das Haeufchen bei {@code pos} eine Platte wendet. */
    public static boolean canFlip(Level level, BlockPos pos, ItemStack tool) {
        if (!(tool.getItem() instanceof SledgehammerItem) || !(level.getBlockState(pos).getBlock() instanceof PlacedSmallPartsBlock)
                || !(level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity pile)) {
            return false;
        }
        return targetIndex(pile.parts()) >= 0;
    }

    /**
     * Wendet die Platte (Server; der Client sagt nur voraus): ersetzt sie an Ort und Stelle durch ihre andere Seite
     * (Name und andere Komponenten bleiben), ein Punkt Haltbarkeit, Klang und Noten. False, wenn nichts passt oder
     * der Spieler hier nichts aendern darf.
     */
    public static boolean flip(Level level, BlockPos pos, Player player, ItemStack tool, InteractionHand hand) {
        if (!canFlip(level, pos, tool) || !TransformTargets.mayTransform(level, player, pos, Direction.UP, tool)) {
            return false;
        }
        if (level.isClientSide()) {
            return true;
        }
        PlacedSmallPartsBlockEntity pile = (PlacedSmallPartsBlockEntity) level.getBlockEntity(pos);
        List<ItemStack> parts = new ArrayList<>(pile.parts());
        int index = targetIndex(parts);
        Item other = MusicDiscs.otherSide(parts.get(index).getItem());
        parts.set(index, parts.get(index).transmuteCopy(other, 1));
        pile.setParts(parts);
        tool.hurtAndBreak(SledgehammerItem.RESHAPE_DAMAGE, player, hand);
        level.playSound(null, pos, SoundEvents.ANVIL_LAND, SoundSource.BLOCKS, 0.3F, 1.8F);
        level.playSound(null, pos, SoundEvents.NOTE_BLOCK_CHIME.value(), SoundSource.BLOCKS, 0.6F, 1.2F);
        if (level instanceof ServerLevel server) {
            for (int i = 0; i < 4; i++) {
                // Vanillas Noten-Partikel: die Geschwindigkeit x waehlt die Farbe (0..1).
                server.sendParticles(ParticleTypes.NOTE, pos.getX() + 0.3 + i * 0.15, pos.getY() + 0.4, pos.getZ() + 0.5,
                        0, i / 4.0, 0.0, 0.0, 1.0);
            }
        }
        level.gameEvent(GameEvent.BLOCK_CHANGE, pos, GameEvent.Context.of(player, level.getBlockState(pos)));
        return true;
    }
}
