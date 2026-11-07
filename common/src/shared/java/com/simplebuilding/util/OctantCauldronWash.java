package com.simplebuilding.util;

import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.cauldron.CauldronInteraction;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.stats.Stats;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.DyeColor;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.LayeredCauldronBlock;

/**
 * Einen gefaerbten Oktanten im Wasserkessel waschen: er wird zum einfachen Oktanten, behaelt seine
 * gespeicherten Daten, und der Kessel verliert eine Stufe Wasser. Die Interaktion und die Liste der
 * waschbaren Oktanten stehen hier einmal; jeder Loader traegt {@link #INTERACTION} fuer
 * {@link #washableOctants()} in den Wasserkessel ein, das Wiki und der JEI-Katalog lesen dieselbe
 * Liste ueber {@link InWorldTransformations}.
 */
public final class OctantCauldronWash {
    /** Wasserstufen, die ein Waschgang kostet: {@code LayeredCauldronBlock.lowerFillLevel} senkt um eine. */
    public static final int WATER_LEVELS = 1;

    public static final CauldronInteraction INTERACTION = (state, world, pos, player, hand, stack) -> {
        if (!isWashable(stack)) {
            return InteractionResult.PASS;
        }
        if (!world.isClientSide()) {
            // transmuteCopy uebernimmt alle geaenderten Komponenten (Ecken, Verzauberungen, Haltbarkeit, Name) -
            // ein neuer Oktant mit nur CUSTOM_DATA verlor beim Waschen Verzauberungen und Schaden.
            ItemStack newStack = stack.transmuteCopy(BuildingToolRefs.octant(), stack.getCount());
            player.setItemInHand(hand, newStack);
            player.awardStat(Stats.CLEAN_ARMOR);
            LayeredCauldronBlock.lowerFillLevel(state, world, pos);
            // Vanilla waescht Farbe lautlos; hier plaetschert es und das Wasser spritzt (keine Texte).
            world.playSound(null, pos, SoundEvents.GENERIC_SPLASH, SoundSource.BLOCKS, 0.5f, 1.3f);
            if (world instanceof ServerLevel server) {
                server.sendParticles(ParticleTypes.SPLASH, pos.getX() + 0.5, pos.getY() + 0.95, pos.getZ() + 0.5, 12, 0.25, 0.0, 0.25, 0.1);
                server.sendParticles(ParticleTypes.BUBBLE_POP, pos.getX() + 0.5, pos.getY() + 0.9, pos.getZ() + 0.5, 6, 0.2, 0.0, 0.2, 0.0);
            }
        }
        return InteractionResult.SUCCESS;
    };

    private OctantCauldronWash() {
    }

    /** A dyed octant (the plain one has nothing to wash off); the wash and the hand hint ask this. */
    public static boolean isWashable(ItemStack stack) {
        // b-btools07: by id, so the catalogue and the hand hint also know Simple Building Tools' octants on 26.3 (it washes them itself).
        Item item = stack.getItem();
        return BuildingToolRefs.isOctant(item) && item != BuildingToolRefs.octant();
    }

    /** Die gefaerbten Oktanten in {@link DyeColor}-Reihenfolge - genau die, die gewaschen werden. */
    public static List<Item> washableOctants() {
        List<Item> out = new ArrayList<>();
        for (DyeColor color : DyeColor.values()) {
            Item coloredItem = BuildingToolRefs.octant(color);
            if (coloredItem != null && coloredItem != net.minecraft.world.item.Items.AIR) {
                out.add(coloredItem);
            }
        }
        return out;
    }
}
