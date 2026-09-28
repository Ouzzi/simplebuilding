package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import com.simplebuilding.tweaks.component.TweaksComponents;
import com.simplebuilding.tweaks.easter.EasterEggs;
import java.util.List;
import com.simplebuilding.version.BlockCodecs;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.BlockPlaceContext;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.StateDefinition;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Trank-Pad I-III (Besitzer 2026-09-28, docs/SIMPLETWEAKS-UEBERNAHME.md Abschnitt 2.4): ein
 * Wurftrank (Splash oder Verweil), der auf dem Pad zerschellt, wird gespeichert und ersetzt den
 * vorigen; ein Wasser-Wurftrank wischt das Pad leer. Wer auf dem Pad steht, bekommt die gespeicherten
 * Wirkungen mit der Verstaerkung des Tranks in drei Schritten (nach 1/2/3 s: 25/50/100 % von 30/60/120 s
 * fuer Stufe I/II/III, die letzte Easter-Stufe 240 s); Sofortwirkungen einmal beim 3-s-Schritt. Der
 * 100-%-Schritt setzt das Pad fuer die doppelte Wirkdauer in die Abklingzeit ({@link #cooldownAt},
 * Blockzustand {@link #COOLING} mit animierter Textur), die nur gesetzt weiterlaeuft; abgebaut traegt das
 * Item die Restzeit ({@link TweaksComponents#POTION_PAD_COOLDOWN}). Unbegrenzt haltbar.
 * Ablauf im Detail: {@link PotionPadBlockEntity}.
 */
public class PotionPadBlock extends PadBlock {
    public static final MapCodec<PotionPadBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            BlockCodecs.propertiesField(),
            Codec.INT.fieldOf("tier").forGetter(PotionPadBlock::getTier)
    ).apply(i, PotionPadBlock::new));

    /** Abklingzeit laeuft: das Pad gibt nichts, die Textur ist animiert. */
    public static final BooleanProperty COOLING = BooleanProperty.create("cooling");

    /** Hoechste Stufe. */
    public static final int MAX_TIER = 3;
    /** Wirkdauer je Stufe in Ticks: 30 s, 60 s, 120 s. */
    private static final int[] DURATION_TICKS = {30 * 20, 60 * 20, 120 * 20};

    private final int tier;

    public PotionPadBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties, Block.box(1, 0, 1, 15, 1, 15), PadOwnership.OWNER_PAD, PadOwnership.STRANGER_PAD);
        this.tier = Math.max(1, Math.min(MAX_TIER, tier));
        registerDefaultState(stateDefinition.any().setValue(COOLING, false));
    }

    @Override
    protected void createBlockStateDefinition(StateDefinition.Builder<Block, BlockState> builder) {
        builder.add(COOLING);
    }

    /** Ein abgebautes Pad in der Abklingzeit wird wieder als abklingendes Pad gesetzt. */
    @Override
    public BlockState getStateForPlacement(BlockPlaceContext context) {
        Integer rest = context.getItemInHand().get(TweaksComponents.POTION_PAD_COOLDOWN);
        return defaultBlockState().setValue(COOLING, rest != null && rest > 0);
    }

    public int getTier() {
        return tier;
    }

    /** Wirkdauer der gespeicherten Effekte beim Betreten, in Ticks. */
    public int effectDuration() {
        return effectDuration(tier);
    }

    public static int effectDuration(int tier) {
        return DURATION_TICKS[Math.max(1, Math.min(MAX_TIER, tier)) - 1];
    }

    /**
     * Wirkdauer des gesetzten Pads: die Stufendauer, bei der letzten Easter-Stufe ({@link EasterEggs})
     * doppelt so lang (Stufe III: 240 s statt 120 s).
     */
    public int effectDurationAt(BlockGetter level, BlockPos pos) {
        return EasterEggs.isBoosted(level, pos) ? 2 * effectDuration() : effectDuration();
    }

    /** Abklingzeit nach dem vollen Schritt: doppelte Wirkdauer des gesetzten Pads (60/120/240 s, Easter-Endstufe 480 s). */
    public int cooldownAt(BlockGetter level, BlockPos pos) {
        return 2 * effectDurationAt(level, pos);
    }

    /**
     * Beim Abbau behaelt das Item den gespeicherten Trank und eine laufende Abklingzeit (Restticks als
     * {@link TweaksComponents#POTION_PAD_COOLDOWN}); die Easter-Stufe schreibt {@link PadBlock#getDrops}.
     */
    @Override
    protected List<ItemStack> getDrops(BlockState state, LootParams.Builder params) {
        List<ItemStack> drops = super.getDrops(state, params);
        if (params.getOptionalParameter(LootContextParams.BLOCK_ENTITY) instanceof PotionPadBlockEntity pad) {
            for (ItemStack stack : drops) {
                if (stack.is(this.asItem())) {
                    if (pad.getStored() != null) {
                        stack.set(DataComponents.POTION_CONTENTS, pad.getStored());
                    }
                    if (pad.isCoolingDown()) {
                        stack.set(TweaksComponents.POTION_PAD_COOLDOWN, pad.getCooldown());
                    }
                }
            }
        }
        return drops;
    }

    // No @Override: MC 26.3 removed block codecs; this only overrides on 26.2.
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new PotionPadBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return level.isClientSide() ? null : createTickerHelper(type, TweaksBlockEntities.POTION_PAD, PotionPadBlockEntity::serverTick);
    }

    /** Ein Wurftrank zerschellt auf dem Pad: seine Wirkungen werden gespeichert (siehe {@link #absorb}). */
    @Override
    protected void onProjectileHit(Level level, BlockState state, BlockHitResult hit, Projectile projectile) {
        super.onProjectileHit(level, state, hit, projectile);
        if (!level.isClientSide() && PotionPadBlockEntity.isThrownPotion(projectile)
                && level.getBlockEntity(hit.getBlockPos()) instanceof PotionPadBlockEntity pad
                && projectile instanceof net.minecraft.world.entity.projectile.throwableitemprojectile.ThrowableItemProjectile thrown) {
            absorb(pad, thrown.getItem());
        }
    }

    /**
     * Speichert die Wirkungen des Tranks auf dem Pad (ersetzt die vorigen). Ein Trank ohne Wirkungen
     * (Wasser, seltsamer Trank) wischt das Pad leer. Liefert, ob danach Wirkungen gespeichert sind.
     */
    public static boolean absorb(PotionPadBlockEntity pad, ItemStack potion) {
        PotionContents contents = potion.getOrDefault(DataComponents.POTION_CONTENTS, PotionContents.EMPTY);
        pad.setStored(contents.hasEffects() ? contents : null);
        return pad.getStored() != null;
    }
}
