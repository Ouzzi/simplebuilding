package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.PotionPadBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.chat.Component;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.entity.projectile.Projectile;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.alchemy.PotionContents;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.BaseEntityBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.entity.BlockEntityTicker;
import net.minecraft.world.level.block.entity.BlockEntityType;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import org.jetbrains.annotations.Nullable;

/**
 * Trank-Pad I-III (Besitzer 2026-09-28, docs/SIMPLETWEAKS-UEBERNAHME.md Abschnitt 2.4): ein
 * Wurftrank (Splash oder Verweil), der auf dem Pad zerschellt, wird gespeichert und ersetzt den
 * vorigen; ein Wasser-Wurftrank wischt das Pad leer. Jeder Spieler, der das Pad betritt, bekommt die
 * gespeicherten Wirkungen mit der Verstaerkung des Tranks fuer 30/60/120 s (Stufe I/II/III), solange
 * er darauf steht immer wieder aufgefrischt, nie laenger. Sofortwirkungen (Heilung, Schaden) wirken
 * einmal je Betreten, hoechstens alle {@link PotionPadBlockEntity#INSTANT_COOLDOWN_TICKS} Ticks je
 * Spieler. Unbegrenzt haltbar.
 */
public class PotionPadBlock extends PadBlock {
    public static final MapCodec<PotionPadBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            Codec.INT.fieldOf("tier").forGetter(PotionPadBlock::getTier)
    ).apply(i, PotionPadBlock::new));

    /** Hoechste Stufe. */
    public static final int MAX_TIER = 3;
    /** Wirkdauer je Stufe in Ticks: 30 s, 60 s, 120 s. */
    private static final int[] DURATION_TICKS = {30 * 20, 60 * 20, 120 * 20};

    private final int tier;

    public PotionPadBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties, Block.box(1, 0, 1, 15, 1, 15), PadOwnership.OWNER_PAD, PadOwnership.STRANGER_PAD);
        this.tier = Math.max(1, Math.min(MAX_TIER, tier));
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

    @Override
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

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof PotionPadBlockEntity pad) {
            PotionContents stored = pad.getStored();
            Component message = stored == null
                    ? Component.translatable("message.simplebuilding.potion_pad.empty").withStyle(ChatFormatting.GRAY)
                    : Component.translatable("message.simplebuilding.potion_pad.stored",
                            stored.getName("item.minecraft.splash_potion.effect."), effectDuration() / 20).withStyle(ChatFormatting.LIGHT_PURPLE);
            player.displayClientMessage(message, true);
        }
        return InteractionResult.SUCCESS;
    }
}
