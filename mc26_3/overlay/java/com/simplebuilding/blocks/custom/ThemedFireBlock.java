package com.simplebuilding.blocks.custom;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.version.McVersion;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.tags.TagKey;
import net.minecraft.util.RandomSource;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.InsideBlockEffectApplier;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.block.BaseFireBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.material.MapColor;

/**
 * Sulfur fire and copper fire (queue N33, 26.3): a fire between normal fire and soul fire, like soul fire it
 * burns only on its own base blocks (tags) and never spreads. Damage 1.5 (fire 1, soul fire 2), light 12 (fire 15,
 * soul fire 10). Sulfur fire also gives nausea to everything standing in it; copper fire has no effect.
 * Placed by flint and steel / fire charges / lava ignition through {@code BaseFireBlock#getState}
 * ({@code BaseFireGetStateMixin}). No item and no loot, like all fires.
 */
public class ThemedFireBlock extends BaseFireBlock {
    public static final float DAMAGE = 1.5F;
    public static final int LIGHT = 12;
    /** Nausea of sulfur fire: refreshed to this length while standing in it (ticks). */
    public static final int NAUSEA_TICKS = 200;
    public static final TagKey<Block> SULFUR_BASE = base("sulfur_fire_base_blocks");
    public static final TagKey<Block> COPPER_BASE = base("copper_fire_base_blocks");

    public static Block SULFUR_FIRE;
    public static Block COPPER_FIRE;

    private final TagKey<Block> baseBlocks;
    private final boolean nausea;

    private ThemedFireBlock(BlockBehaviour.Properties properties, TagKey<Block> baseBlocks, boolean nausea) {
        super(properties, DAMAGE);
        this.baseBlocks = baseBlocks;
        this.nausea = nausea;
    }

    private static TagKey<Block> base(String path) {
        return TagKey.create(Registries.BLOCK, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, path));
    }

    public static void register() {
        if (!McVersion.FIRES || SULFUR_FIRE != null) return;
        SULFUR_FIRE = register("sulfur_fire", MapColor.COLOR_YELLOW, SULFUR_BASE, true);
        COPPER_FIRE = register("copper_fire", MapColor.WARPED_NYLIUM, COPPER_BASE, false);
    }

    private static Block register(String name, MapColor color, TagKey<Block> base, boolean nausea) {
        Identifier id = Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, name);
        BlockBehaviour.Properties properties = BlockBehaviour.Properties.ofFullCopy(Blocks.SOUL_FIRE)
                .setId(ResourceKey.create(Registries.BLOCK, id)).mapColor(color).lightLevel(state -> LIGHT);
        return Registry.register(BuiltInRegistries.BLOCK, id, new ThemedFireBlock(properties, base, nausea));
    }

    /** The themed fire that ignites on {@code below}, or null (used by the BaseFireBlock#getState mixin). */
    public static BlockState fireFor(BlockGetter level, BlockPos pos) {
        if (SULFUR_FIRE == null) return null;
        BlockState below = level.getBlockState(pos.below());
        if (below.is(SULFUR_BASE)) return SULFUR_FIRE.defaultBlockState();
        if (below.is(COPPER_BASE)) return COPPER_FIRE.defaultBlockState();
        return null;
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos, Direction direction,
            BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        return canSurvive(state, level, pos) ? state : Blocks.AIR.defaultBlockState();
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return level.getBlockState(pos.below()).is(baseBlocks);
    }

    @Override
    protected boolean canBurn(BlockState state) {
        return false;
    }

    @Override
    protected void entityInside(BlockState state, Level level, BlockPos pos, Entity entity, InsideBlockEffectApplier applier, boolean intersects) {
        super.entityInside(state, level, pos, entity, applier, intersects);
        if (nausea && !level.isClientSide() && entity instanceof LivingEntity living) {
            MobEffectInstance current = living.getEffect(MobEffects.NAUSEA);
            if (current == null || current.getDuration() < NAUSEA_TICKS / 2) {
                living.addEffect(new MobEffectInstance(MobEffects.NAUSEA, NAUSEA_TICKS, 0));
            }
        }
    }
}
