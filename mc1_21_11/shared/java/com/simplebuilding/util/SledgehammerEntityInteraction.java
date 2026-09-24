package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import java.util.LinkedHashMap;
import java.util.Map;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;

/**
 * Ein Vorschlaghammer-Schlag (Linksklick) auf einen Rahmen mit einer Ruestungsbesatz-Vorlage wertet
 * sie auf: Leuchttintenbeutel in der Nebenhand ergibt die leuchtende, Glowstonestaub die strahlende
 * Besatzvorlage. Die Tabelle ({@link #trimUpgrades()}), die Vorlagen-Regel ({@link #isTrimTemplate})
 * und die Kosten stehen hier einmal; das Wiki und der JEI-Katalog lesen sie ueber
 * {@link InWorldTransformations}.
 */
public final class SledgehammerEntityInteraction {
    /** Haltbarkeit, die ein Schlag den Hammer kostet (ausserhalb des Kreativmodus). */
    public static final int HAMMER_DAMAGE = 1;
    /** Wie viele Stueck des Nebenhand-Materials ein Schlag verbraucht (ausserhalb des Kreativmodus). */
    public static final int CATALYST_COST = 1;

    private SledgehammerEntityInteraction() {
    }

    /** Nebenhand-Material -> Ergebnis, in fester Reihenfolge. */
    public static Map<Item, Item> trimUpgrades() {
        Map<Item, Item> upgrades = new LinkedHashMap<>();
        upgrades.put(Items.GLOW_INK_SAC, ModItems.GLOWING_TRIM_TEMPLATE);
        upgrades.put(Items.GLOWSTONE_DUST, ModItems.EMITTING_TRIM_TEMPLATE);
        return upgrades;
    }

    public static InteractionResult handleAttackEntity(Player player, Level world, InteractionHand hand, Entity entity) {
        if (world.isClientSide()) return InteractionResult.PASS;
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;

        ItemStack mainStack = player.getMainHandItem();
        ItemStack offStack = player.getOffhandItem();
        Item result = trimUpgrades().get(offStack.getItem());

        if (result == null || !(mainStack.getItem() instanceof SledgehammerItem)
                || !(entity instanceof ItemFrame itemFrame) || !isTrimTemplate(itemFrame.getItem().getItem())) {
            return InteractionResult.PASS;
        }

        itemFrame.setItem(new ItemStack(result), true);

        if (!player.isCreative()) {
            offStack.shrink(CATALYST_COST);
            mainStack.hurtAndBreak(HAMMER_DAMAGE, player, EquipmentSlot.MAINHAND);
        }

        boolean glowing = result == ModItems.GLOWING_TRIM_TEMPLATE;
        world.playSound(null, itemFrame.blockPosition(), SoundEvents.AMETHYST_BLOCK_HIT, SoundSource.BLOCKS, 1.0f, 1.5f);
        world.playSound(null, itemFrame.blockPosition(), glowing ? SoundEvents.GLOW_INK_SAC_USE : SoundEvents.BLAZE_SHOOT,
                SoundSource.BLOCKS, 1.0f, 1.0f);

        world.addParticle(ParticleTypes.GLOW, itemFrame.getX(), itemFrame.getY(), itemFrame.getZ(), 0.0, 0.1, 0.0);
        world.addParticle(glowing ? ParticleTypes.GLOW_SQUID_INK : ParticleTypes.LARGE_SMOKE,
                itemFrame.getX(), itemFrame.getY(), itemFrame.getZ(), 0.0, 0.1, 0.0);

        return InteractionResult.SUCCESS;
    }

    /**
     * Namensregel: jedes Item, dessen registrierter Name {@code trim_smithing_template} enthaelt -
     * alle Vanilla-Besatzvorlagen. Die mod-eigenen Ergebnisse heissen {@code *_trim_template} und
     * fallen nicht darunter, ein umgewandelter Rahmen laesst sich also nicht noch einmal umwandeln.
     */
    public static boolean isTrimTemplate(Item item) {
        return BuiltInRegistries.ITEM.getKey(item).getPath().contains("trim_smithing_template");
    }
}
