package com.simplebuilding.tweaks.easter;

import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * Der "Funny Stick", Ende der Easter-Kette ({@link EasterEggs}): ein Stock, sonst nichts. In der Hand
 * (Haupt- oder Nebenhand) steigen alle paar Ticks ein, zwei Funken auf - Endstab-Glitzern, dazu ab und
 * zu eine Note oder ein gluecklicher Dorfbewohner-Funke. Serverseitig gesendet, damit auch andere
 * Spieler sehen, womit sich jemand vier Netherit-Stufen Arbeit verdient hat.
 */
public class FunnyStickItem extends Item {
    /** Alle so viele Ticks ein Funkenstoss. */
    public static final int PARTICLE_INTERVAL = 4;

    public FunnyStickItem(Item.Properties properties) {
        super(properties);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        if (isHeld(slot) && level.getGameTime() % PARTICLE_INTERVAL == 0) {
            sparkle(level, owner, slot == EquipmentSlot.OFFHAND);
        }
    }

    public static boolean isHeld(@Nullable EquipmentSlot slot) {
        return slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
    }

    /** Ein Funkenstoss an der Hand des Traegers; liefert die Zahl der gesendeten Partikel (Tests). */
    public static int sparkle(ServerLevel level, Entity owner, boolean offhand) {
        // Ungefaehr dort, wo die Hand ist: seitlich vor dem Koerper auf Brusthoehe.
        double yaw = Math.toRadians(owner.getYRot() + (offhand ? -40 : 40));
        double x = owner.getX() - Math.sin(yaw) * 0.45;
        double z = owner.getZ() + Math.cos(yaw) * 0.45;
        double y = owner.getY() + owner.getBbHeight() * 0.55;
        long phase = level.getGameTime() / PARTICLE_INTERVAL;
        ParticleOptions accent = phase % 8 == 0 ? ParticleTypes.NOTE : phase % 5 == 0 ? ParticleTypes.HAPPY_VILLAGER : null;
        int sent = level.sendParticles(ParticleTypes.END_ROD, x, y, z, 1, 0.08, 0.1, 0.08, 0.01);
        if (accent != null) {
            sent += level.sendParticles(accent, x, y + 0.25, z, 1, 0.05, 0.05, 0.05, 0.0);
        }
        return sent;
    }
}
