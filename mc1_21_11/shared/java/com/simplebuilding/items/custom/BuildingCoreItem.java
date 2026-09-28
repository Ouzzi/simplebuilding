package com.simplebuilding.items.custom;

import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;

/**
 * Baukern (Kupfer bis Enderit): nicht stapelbar; ein Rechtsklick spielt eine kurze Animation ohne
 * Spielwirkung (Besitzer 2026-09-28). Welche, entscheidet ein Wurf mit den Gewichten 70/20/10:
 * <ul>
 *   <li>{@link Animation#GLOW} (70 %): sanftes Leuchten - ein Puls aus Glimmer und Staub in der Farbe
 *       des Kerns um die Hand, leises Amethyst-Klingen.</li>
 *   <li>{@link Animation#ORBIT} (20 %): Funken kreisen in zwei Ringen um den Spieler und steigen auf,
 *       Amethyst-Resonanz.</li>
 *   <li>{@link Animation#BURST} (10 %): ein Stern wie der Netherstern - vier lange Strahlen aus
 *       Endstab-Funken und ein Feuerwerksfunkenregen, dazu der Klang eines erwachenden Leuchtfeuers.</li>
 * </ul>
 * Partikel und Klang schickt der Server an alle Spieler in der Naehe ({@code sendParticles},
 * {@code playSound(null, ...)}); der Client schwingt die Hand. Danach eine kurze Abklingzeit, damit
 * Dauerklicken keinen Partikelsturm macht.
 */
public class BuildingCoreItem extends Item {
    /** Abklingzeit nach einer Animation, in Ticks (1,5 s). */
    public static final int COOLDOWN_TICKS = 30;

    /** Die drei Animationen, in Wurf-Reihenfolge; die Gewichte summieren sich zu 100. */
    public enum Animation {
        GLOW(70), ORBIT(20), BURST(10);

        public final int weight;

        Animation(int weight) {
            this.weight = weight;
        }
    }

    /** Farbe des Kerns fuer den Staub (RGB). */
    private final int color;

    public BuildingCoreItem(Properties properties, int color) {
        super(properties);
        this.color = color;
    }

    public int color() {
        return color;
    }

    /** Die Animation zu einem Wurf {@code roll} aus 0..99: 0-69 Leuchten, 70-89 Kreisen, 90-99 Stern. */
    public static Animation fromRoll(int roll) {
        int bound = 0;
        for (Animation animation : Animation.values()) {
            bound += animation.weight;
            if (roll < bound) {
                return animation;
            }
        }
        return Animation.BURST;
    }

    /** Wuerfelt eine Animation mit den Gewichten 70/20/10. */
    public static Animation roll(RandomSource random) {
        return fromRoll(random.nextInt(100));
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level instanceof ServerLevel server) {
            play(server, player, roll(player.getRandom()), color);
            player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        }
        return InteractionResult.SUCCESS;
    }

    /** Spielt {@code animation} am Spieler: Partikel und Klang fuer alle in der Naehe. */
    public static void play(ServerLevel level, Player player, Animation animation, int color) {
        Vec3 look = player.getLookAngle();
        Vec3 hand = player.getEyePosition().add(look.x * 0.7, look.y * 0.7 - 0.35, look.z * 0.7);
        DustParticleOptions dust = new DustParticleOptions(color, 1.0F);
        RandomSource random = player.getRandom();
        switch (animation) {
            case GLOW -> {
                level.sendParticles(dust, hand.x, hand.y, hand.z, 14, 0.25, 0.25, 0.25, 0.0);
                level.sendParticles(ParticleTypes.GLOW, hand.x, hand.y, hand.z, 6, 0.2, 0.2, 0.2, 0.02);
                level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS,
                        0.8F, 1.1F + random.nextFloat() * 0.3F);
            }
            case ORBIT -> {
                double cy = player.getY() + player.getBbHeight() * 0.55;
                int points = 20;
                for (int i = 0; i < points; i++) {
                    double angle = 2 * Math.PI * i / points;
                    double x = player.getX() + Math.cos(angle) * 1.1;
                    double z = player.getZ() + Math.sin(angle) * 1.1;
                    // Tangential und leicht aufwaerts: die Funken laufen ein Stueck im Kreis weiter.
                    burstOne(level, ParticleTypes.ELECTRIC_SPARK, x, cy + (i % 2) * 0.35, z,
                            -Math.sin(angle), 0.35, Math.cos(angle), 0.12);
                    if (i % 4 == 0) {
                        burstOne(level, dust, x, cy, z, 0, 0, 0, 0);
                    }
                }
                level.sendParticles(ParticleTypes.GLOW, hand.x, hand.y, hand.z, 4, 0.15, 0.15, 0.15, 0.01);
                level.playSound(null, player.getX(), cy, player.getZ(), SoundEvents.AMETHYST_BLOCK_RESONATE, SoundSource.PLAYERS,
                        1.0F, 1.4F);
            }
            case BURST -> {
                // Vier Strahlen wie der Netherstern (waagerecht quer zum Blick und senkrecht), dann Funkenregen.
                Vec3 side = new Vec3(-look.z, 0, look.x).normalize();
                Vec3 up = new Vec3(0, 1, 0);
                for (Vec3 ray : new Vec3[]{side, side.reverse(), up, up.reverse()}) {
                    for (int step = 1; step <= 6; step++) {
                        double d = step * 0.18;
                        burstOne(level, ParticleTypes.END_ROD, hand.x + ray.x * d, hand.y + ray.y * d, hand.z + ray.z * d,
                                ray.x, ray.y, ray.z, 0.04 * step);
                    }
                }
                level.sendParticles(ParticleTypes.FIREWORK, hand.x, hand.y, hand.z, 24, 0.1, 0.1, 0.1, 0.18);
                level.sendParticles(dust, hand.x, hand.y, hand.z, 20, 0.45, 0.45, 0.45, 0.0);
                level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.BEACON_ACTIVATE, SoundSource.PLAYERS, 0.7F, 1.5F);
                level.playSound(null, hand.x, hand.y, hand.z, SoundEvents.AMETHYST_BLOCK_CHIME, SoundSource.PLAYERS, 1.0F, 0.7F);
            }
        }
    }

    /** Ein Partikel mit fester Bewegung: {@code count = 0} macht aus dem Versatz die Richtung. */
    private static void burstOne(ServerLevel level, ParticleOptions particle, double x, double y, double z,
                                 double dx, double dy, double dz, double speed) {
        level.sendParticles(particle, x, y, z, 0, dx, dy, dz, speed);
    }
}
