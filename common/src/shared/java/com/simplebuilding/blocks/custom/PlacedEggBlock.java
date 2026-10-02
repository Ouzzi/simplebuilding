package com.simplebuilding.blocks.custom;

import com.mojang.serialization.MapCodec;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.util.RandomSource;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.chicken.Chicken;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.item.enchantment.Enchantments;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.LevelReader;
import net.minecraft.world.level.ScheduledTickAccess;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.EnumProperty;
import net.minecraft.world.phys.shapes.CollisionContext;
import net.minecraft.world.phys.shapes.VoxelShape;
import org.jetbrains.annotations.Nullable;

/**
 * Gelegtes Ei (Besitzer 2026-10-02): Schleichen + Rechtsklick mit einem Ei stellt es aufrecht auf einen Block (Vanilla-,
 * blaues oder braunes Ei). Mit Behutsamkeit abgebaut faellt das Ei heraus; sonst zerbricht es und schluepft genau wie
 * ein geworfenes Ei: 1/8 Kueken, davon 1/32 vier, in der Variante des Eis ({@code minecraft:chicken/variant}).
 * Kein Item und keine Loot-Tabelle; das Ei steckt in der Variante des Blocks.
 */
public class PlacedEggBlock extends Block {
    public static final EnumProperty<Egg> EGG = EnumProperty.create("egg", Egg.class);
    public static final MapCodec<PlacedEggBlock> CODEC = com.simplebuilding.version.BlockCodecs.simple(PlacedEggBlock::new);
    private static final VoxelShape SHAPE = Block.box(5.5, 0.0, 5.5, 10.5, 6.5, 10.5);

    public enum Egg implements StringRepresentable {
        WHITE("white", Items.EGG), BLUE("blue", Items.BLUE_EGG), BROWN("brown", Items.BROWN_EGG);

        private final String name;
        private final Item item;

        Egg(String name, Item item) {
            this.name = name;
            this.item = item;
        }

        public Item item() {
            return item;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        public static @Nullable Egg of(ItemStack stack) {
            for (Egg egg : values()) {
                if (stack.is(egg.item)) return egg;
            }
            return null;
        }
    }

    public PlacedEggBlock(BlockBehaviour.Properties properties) {
        super(properties);
        this.registerDefaultState(this.stateDefinition.any().setValue(EGG, Egg.WHITE));
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends Block> codec() {
        return CODEC;
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(EGG);
    }

    @Override
    public @Nullable BlockState getStateForPlacement(BlockPlaceContext context) {
        Egg egg = Egg.of(context.getItemInHand());
        if (egg == null) return null;
        BlockState state = this.defaultBlockState().setValue(EGG, egg);
        return state.canSurvive(context.getLevel(), context.getClickedPos()) ? state : null;
    }

    @Override
    protected VoxelShape getShape(BlockState state, BlockGetter level, BlockPos pos, CollisionContext context) {
        return SHAPE;
    }

    @Override
    protected boolean canSurvive(BlockState state, LevelReader level, BlockPos pos) {
        return Block.canSupportCenter(level, pos.below(), Direction.UP);
    }

    @Override
    protected BlockState updateShape(BlockState state, LevelReader level, ScheduledTickAccess ticks, BlockPos pos,
                                     Direction direction, BlockPos neighborPos, BlockState neighborState, RandomSource random) {
        if (direction == Direction.DOWN && !state.canSurvive(level, pos)) {
            return Blocks.AIR.defaultBlockState();
        }
        return super.updateShape(state, level, ticks, pos, direction, neighborPos, neighborState, random);
    }

    /** Behutsamkeit: das Ei; sonst zerbricht es und schluepft vielleicht (wie {@code ThrownEgg#onHit}). */
    @Override
    protected void spawnAfterBreak(BlockState state, ServerLevel level, BlockPos pos, ItemStack tool, boolean dropExperience) {
        super.spawnAfterBreak(state, level, pos, tool, dropExperience);
        ItemStack egg = new ItemStack(state.getValue(EGG).item());
        boolean silk = !tool.isEmpty() && EnchantmentHelper.getItemEnchantmentLevel(
                level.registryAccess().lookupOrThrow(Registries.ENCHANTMENT).getOrThrow(Enchantments.SILK_TOUCH), tool) > 0;
        if (silk) {
            popResource(level, pos, egg);
            return;
        }
        level.playSound(null, pos, SoundEvents.TURTLE_EGG_BREAK, SoundSource.BLOCKS, 0.7F, 1.4F);
        hatch(level, pos, egg, level.getRandom());
    }

    /** Die Schluepf-Regel des geworfenen Eis: 1/8 ein Kueken, davon 1/32 vier. Liefert die Zahl der Kueken. */
    public static int hatch(ServerLevel level, BlockPos pos, ItemStack egg, RandomSource random) {
        if (random.nextInt(8) != 0) {
            return 0;
        }
        int count = random.nextInt(32) == 0 ? 4 : 1;
        int spawned = 0;
        for (int i = 0; i < count; i++) {
            Chicken chicken = EntityTypes.CHICKEN.create(level, EntitySpawnReason.TRIGGERED);
            if (chicken == null) continue;
            chicken.setAge(-24000);
            chicken.snapTo(pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, random.nextFloat() * 360.0F, 0.0F);
            Optional.ofNullable(egg.get(DataComponents.CHICKEN_VARIANT)).ifPresent(chicken::setVariant);
            level.addFreshEntity(chicken);
            spawned++;
        }
        return spawned;
    }
}
