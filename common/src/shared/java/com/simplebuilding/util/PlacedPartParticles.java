package com.simplebuilding.util;

import com.simplebuilding.blocks.custom.PlacedSmallPartsBlock;
import com.simplebuilding.blocks.entity.custom.PlacedSmallPartsBlockEntity;
import com.simplebuilding.items.ModItems;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.particles.ParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.state.BlockState;
import org.joml.Vector3f;

/**
 * Partikel eines Kleinteil-Haeufchens (2026-10-03), clientseitig aus {@code PlacedSmallPartsBlock#animateTick}:
 *
 * <ul>
 *   <li>Brennende Kerzen: an jedem Docht die Vanilla-Flamme ({@code small_flame}), ab und zu Rauch und das
 *       Kerzen-Knistern - genau wie {@code AbstractCandleBlock#animateTick}; immer an, wie bei Vanilla.</li>
 *   <li>Leuchtende Teile ({@link #GLOW}): selten ein dezenter Vanilla-Partikel ueber dem Teil. Abschaltbar mit der
 *       Client-Option {@code tools.placedPartParticles}; die Vanilla-Einstellung "Partikel" wirkt zusaetzlich.</li>
 * </ul>
 */
public final class PlacedPartParticles {
    /** Je Aufruf von {@code animateTick} zeigt ein leuchtendes Teil mit dieser Wahrscheinlichkeit (1 zu n) einen Partikel. */
    public static final int GLOW_CHANCE = 3;

    private static Map<Item, ParticleOptions> glow;

    private PlacedPartParticles() {
    }

    /** Welche Teile glimmen und womit (Vanilla-Partikel); Mod-Teile nur, wenn registriert. */
    public static Map<Item, ParticleOptions> glow() {
        Map<Item, ParticleOptions> map = glow;
        if (map == null) {
            map = new HashMap<>();
            map.put(Items.GLOWSTONE_DUST, ParticleTypes.WAX_ON);
            map.put(Items.GLOW_INK_SAC, ParticleTypes.GLOW);
            map.put(Items.PRISMARINE_CRYSTALS, ParticleTypes.GLOW);
            map.put(Items.NETHER_STAR, ParticleTypes.END_ROD);
            map.put(Items.BLAZE_ROD, ParticleTypes.SMALL_FLAME);
            map.put(Items.ECHO_SHARD, ParticleTypes.SCULK_CHARGE_POP);
            if (ModItems.ASTRALIT_DUST != null) {
                map.put(ModItems.ASTRALIT_DUST, ParticleTypes.END_ROD);
            }
            if (ModItems.SAGE_ORB != null) {
                map.put(ModItems.SAGE_ORB, ParticleTypes.ENCHANT);
            }
            glow = map;
        }
        return map;
    }

    /** Der Glanz-Partikel dieses Teils oder null. */
    public static ParticleOptions glowOf(ItemStack part) {
        return part.isEmpty() ? null : glow().get(part.getItem());
    }

    public static void animate(BlockState state, Level level, BlockPos pos, RandomSource random) {
        if (!(state.getBlock() instanceof PlacedSmallPartsBlock) || !(level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity pile)) {
            return;
        }
        List<ItemStack> parts = pile.parts();
        int count = Math.min(parts.size(), PlacedSmallParts.MAX_PARTS);
        Direction facing = state.getValue(PlacedSmallPartsBlock.FACING);
        boolean lit = state.getValue(PlacedSmallPartsBlock.LIT);
        boolean cosmetic = cosmeticOn();
        for (int i = 0; i < count; i++) {
            ItemStack part = parts.get(i);
            PlacedSmallParts.Kind kind = PlacedSmallParts.kind(part);
            if (kind == PlacedSmallParts.Kind.CANDLE) {
                if (lit) {
                    Vector3f wick = PlacedSmallParts.center(facing, count, i, true);
                    double x = pos.getX() + wick.x, y = pos.getY() + wick.y, z = pos.getZ() + wick.z;
                    float f = random.nextFloat();
                    if (f < 0.3F) {
                        level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
                        if (f < 0.17F) {
                            level.playLocalSound(x, y, z, SoundEvents.CANDLE_AMBIENT, SoundSource.BLOCKS,
                                    1.0F + random.nextFloat(), random.nextFloat() * 0.7F + 0.3F, false);
                        }
                    }
                    level.addParticle(ParticleTypes.SMALL_FLAME, x, y, z, 0.0, 0.0, 0.0);
                }
                continue;
            }
            ParticleOptions particle = cosmetic ? glowOf(part) : null;
            if (particle == null || random.nextInt(GLOW_CHANCE) != 0) {
                continue;
            }
            Vector3f c = PlacedSmallParts.center(facing, count, i, kind.standing());
            double x = pos.getX() + c.x + (random.nextDouble() - 0.5) * 0.2;
            double y = pos.getY() + c.y + 0.05 + random.nextDouble() * 0.1;
            double z = pos.getZ() + c.z + (random.nextDouble() - 0.5) * 0.2;
            double rise = particle == ParticleTypes.END_ROD || particle == ParticleTypes.ENCHANT ? 0.01 : 0.0;
            level.addParticle(particle, x, y, z, 0.0, rise, 0.0);
        }
    }

    /** Rauch an jedem Docht, wenn die Kerzen ausgehen (wie {@code AbstractCandleBlock#extinguish}). */
    public static void wickSmoke(Level level, BlockPos pos, BlockState state) {
        if (!(level.getBlockEntity(pos) instanceof PlacedSmallPartsBlockEntity pile)) {
            return;
        }
        List<ItemStack> parts = pile.parts();
        int count = Math.min(parts.size(), PlacedSmallParts.MAX_PARTS);
        for (int i = 0; i < count; i++) {
            if (PlacedSmallParts.isCandle(parts.get(i))) {
                Vector3f wick = PlacedSmallParts.center(state.getValue(PlacedSmallPartsBlock.FACING), count, i, true);
                level.addParticle(ParticleTypes.SMOKE, pos.getX() + wick.x, pos.getY() + wick.y, pos.getZ() + wick.z, 0.0, 0.1, 0.0);
            }
        }
    }

    /** Client-Option {@code tools.placedPartParticles} (Standard an); ohne lesbare Config an. */
    private static boolean cosmeticOn() {
        try {
            return com.simplebuilding.Simplebuilding.getConfig().tools.placedPartParticles;
        } catch (RuntimeException | LinkageError noConfig) {
            return true;
        }
    }
}
