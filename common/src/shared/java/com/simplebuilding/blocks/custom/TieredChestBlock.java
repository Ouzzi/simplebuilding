package com.simplebuilding.blocks.custom;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.blocks.entity.ModBlockEntities;
import com.simplebuilding.blocks.entity.custom.TieredChestBlockEntity;
import com.simplebuilding.platform.TieredChestMenus;
import com.simplebuilding.util.SledgehammerUpgrades;
import com.simplebuilding.util.TieredChests;
import java.util.Optional;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.MenuProvider;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.ChestBlock;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Verstaerkte, Netherit- und Enderittruhe. Eine Vanilla-Truhe in allem, was die Welt sieht
 * (Form, Doppeltruhen, Wasser, Katzen und Bloecke auf dem Deckel, Piglins, Statistik, Deckel,
 * Klang), mit eigener Block-Entity ({@link TieredChestBlockEntity}) und eigenem Menue.
 *
 * <p><b>Doppeltruhen</b> bilden sich nur aus zwei Truhen derselben Stufe: Vanillas
 * {@code chestCanConnectTo} fragt {@code state.is(this)}, und jede Stufe ist ein eigener Block.
 * Aufgewertet werden beide Haelften zugleich ({@link TieredChests#upgradeInPlace}).
 */
public class TieredChestBlock extends ChestBlock {
    private static final Codec<ChestTier> TIER_CODEC = Codec.INT.xmap(ChestTier::byId, ChestTier::ordinal);
    public static final MapCodec<TieredChestBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            TIER_CODEC.fieldOf("tier").forGetter(TieredChestBlock::tier),
            com.simplebuilding.version.BlockCodecs.propertiesField()
    ).apply(i, TieredChestBlock::new));

    private final ChestTier tier;

    public TieredChestBlock(ChestTier tier, BlockBehaviour.Properties properties) {
        this(tier, net.minecraft.sounds.SoundEvents.CHEST_OPEN, net.minecraft.sounds.SoundEvents.CHEST_CLOSE, properties);
    }

    public TieredChestBlock(ChestTier tier, SoundEvent open, SoundEvent close, BlockBehaviour.Properties properties) {
        super(() -> ModBlockEntities.TIERED_CHEST_BE, open, close, properties);
        this.tier = tier;
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    public MapCodec<? extends ChestBlock> codec() {
        return CODEC;
    }

    public ChestTier tier() {
        return this.tier;
    }

    @Override
    public BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new TieredChestBlockEntity(pos, state);
    }

    /**
     * Wie Vanilla, nur mit dem eigenen Menue samt Oeffnungsdaten. Im Schmiedestand (Hammer plus
     * Aufwertungs-Material) geht der Klick an den Hammer weiter, wenn die Aufwertung beginnen kann.
     */
    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hit) {
        if (SledgehammerUpgrades.shouldSkipBlockUse(state, level, pos, player, InteractionHand.MAIN_HAND)) {
            return InteractionResult.PASS;
        }
        if (level instanceof ServerLevel serverLevel && player instanceof ServerPlayer serverPlayer) {
            TieredChests.Opening opening = TieredChests.opening(this, state, level, pos);
            if (opening != null) {
                TieredChestMenus.open(serverPlayer, opening.provider(), opening.data());
                player.awardStat(this.getOpenChestStat());
                PiglinAi.angerNearbyPiglins(serverLevel, player, true);
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected @Nullable MenuProvider getMenuProvider(BlockState state, Level level, BlockPos pos) {
        TieredChests.Opening opening = TieredChests.opening(this, state, level, pos);
        return opening == null ? null : opening.provider();
    }

    /** Wie Vanilla, aber gegen die Stapelgrenze dieser Stufe gerechnet (256 Steine sind voll, nicht 64). */
    @Override
    protected int getAnalogOutputSignal(BlockState state, Level level, BlockPos pos, Direction direction) {
        return TieredChests.analogSignal(getContainer(this, state, level, pos, false), this.tier);
    }

    /** Fuer Tests und Nachbarn: die Haelfte, mit der diese Truhe verbunden ist, sonst leer. */
    public static Optional<BlockPos> partner(BlockState state, BlockPos pos) {
        return state.getValue(TYPE) == net.minecraft.world.level.block.state.properties.ChestType.SINGLE
                ? Optional.empty() : Optional.of(getConnectedBlockPos(pos, state));
    }
}
