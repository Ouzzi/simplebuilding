package com.simplebuilding.blocks.custom;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.particles.DustParticleOptions;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.TorchBlock;
import net.minecraft.world.level.block.WallTorchBlock;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Sulfur torch, sulfur lantern and sulfur campfire (queue N58, follow-up of the sulfur fire): light 12 like the
 * sulfur fire, yellow-green dust instead of flame particles; standing in the lit campfire gives the same nausea as
 * sulfur fire. Registered from {@link ThemedFireBlock#register()}; the campfire uses Vanilla's campfire block entity
 * (accepted by {@code SkullBlockEntityTypeMixin}).
 */
public final class SulfurLights {
    public static final int LIGHT = ThemedFireBlock.LIGHT;
    private static final DustParticleOptions DUST = new DustParticleOptions(0xC4DC28, 0.6F);

    public static Block TORCH;
    public static Block WALL_TORCH;
    public static Block LANTERN;
    public static Block CAMPFIRE;

    private SulfurLights() {
    }

    public static void register() {
        if (!McVersion.FIRES || TORCH != null) return;
        TORCH = register("sulfur_torch", Blocks.SOUL_TORCH, p -> new Torch(p.lightLevel(s -> LIGHT)));
        WALL_TORCH = register("sulfur_wall_torch", Blocks.SOUL_WALL_TORCH, p -> new WallTorch(p.lightLevel(s -> LIGHT)));
        LANTERN = register("sulfur_lantern", Blocks.SOUL_LANTERN, p -> new LanternBlock(p.lightLevel(s -> LIGHT)));
        CAMPFIRE = register("sulfur_campfire", Blocks.SOUL_CAMPFIRE,
                p -> new Campfire(p.lightLevel(s -> s.getValue(CampfireBlock.LIT) ? LIGHT : 0)));
    }

    private static Block register(String name, Block copy, java.util.function.Function<BlockBehaviour.Properties, Block> factory) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name);
        BlockBehaviour.Properties props = BlockBehaviour.Properties.ofFullCopy(copy).setId(ResourceKey.create(Registries.BLOCK, id));
        return Registry.register(BuiltInRegistries.BLOCK, id, factory.apply(props));
    }

    private static void sparkle(Level level, double x, double y, double z, RandomSource random) {
        level.addParticle(DUST, x, y, z, 0.0, 0.0, 0.0);
        if (random.nextInt(4) == 0) level.addParticle(ParticleTypes.SMOKE, x, y, z, 0.0, 0.0, 0.0);
    }

    private static final class Torch extends TorchBlock {
        Torch(BlockBehaviour.Properties props) {
            super(ParticleTypes.FLAME, props);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            sparkle(level, pos.getX() + 0.5, pos.getY() + 0.7, pos.getZ() + 0.5, random);
        }
    }

    private static final class WallTorch extends WallTorchBlock {
        WallTorch(BlockBehaviour.Properties props) {
            super(ParticleTypes.FLAME, props);
        }

        @Override
        public void animateTick(BlockState state, Level level, BlockPos pos, RandomSource random) {
            Direction away = state.getValue(WallTorchBlock.FACING).getOpposite();
            sparkle(level, pos.getX() + 0.5 + 0.27 * away.getStepX(), pos.getY() + 0.92, pos.getZ() + 0.5 + 0.27 * away.getStepZ(), random);
        }
    }

    private static final class Campfire extends CampfireBlock {
        Campfire(BlockBehaviour.Properties props) {
            super(true, 1, props);
        }

        @Override
        protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
            super.entityInside(state, level, pos, entity, applier, intersects);
            if (state.getValue(LIT) && !level.isClientSide() && entity instanceof LivingEntity living) {
                MobEffectInstance current = living.getEffect(MobEffects.NAUSEA);
                if (current == null || current.getDuration() < ThemedFireBlock.NAUSEA_TICKS / 2) {
                    living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, ThemedFireBlock.NAUSEA_TICKS, 0));
                }
            }
        }
    }
}
