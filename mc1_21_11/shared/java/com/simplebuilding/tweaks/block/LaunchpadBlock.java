package com.simplebuilding.tweaks.block;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.tweaks.block.entity.LaunchpadBlockEntity;
import com.simplebuilding.tweaks.block.entity.TweaksBlockEntities;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.network.chat.Component;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.context.UseOnContext;
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
 * Launchpad (Simple Tweaks): mit Windkugeln laden (Rechtsklick eine, schleichend alle aus der Hand),
 * 3 s stehen, Start. Stufen I-III (Diamant/Netherit/Enderit) fassen 4/8/16 Ladungen; jede Ladung
 * zaehlt doppelt so viel wie frueher, 16 Ladungen starten also so hoch wie die alten 32. Die
 * Enderit-Stufe schuetzt zusaetzlich bis zur naechsten Landung vor Fallschaden.
 */
public class LaunchpadBlock extends WaterloggedPadBlock {
    public static final MapCodec<LaunchpadBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            Codec.INT.fieldOf("tier").forGetter(LaunchpadBlock::getTier)
    ).apply(i, LaunchpadBlock::new));

    /** Hoechste Stufe (Enderit): Fallschutz nach dem Start. */
    public static final int ENDERITE_TIER = 3;

    private final int tier;

    public LaunchpadBlock(BlockBehaviour.Properties properties, int tier) {
        super(properties, Block.box(1, 0, 1, 15, 1, 15), PadOwnership.OWNER_PAD, PadOwnership.STRANGER_PAD);
        this.tier = Math.max(1, Math.min(ENDERITE_TIER, tier));
    }

    public int getTier() {
        return tier;
    }

    public boolean isEnderite() {
        return tier >= ENDERITE_TIER;
    }

    public int maxCharges() {
        return maxCharges(tier);
    }

    /** Fassungsvermoegen je Stufe: I = 4, II = 8, III = 16. */
    public static int maxCharges(int tier) {
        return 4 << (Math.max(1, Math.min(ENDERITE_TIER, tier)) - 1);
    }

    @Override
    protected MapCodec<? extends BaseEntityBlock> codec() {
        return CODEC;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new LaunchpadBlockEntity(pos, state);
    }

    @Override
    public <T extends BlockEntity> @Nullable BlockEntityTicker<T> getTicker(Level level, BlockState state, BlockEntityType<T> type) {
        return createTickerHelper(type, TweaksBlockEntities.LAUNCHPAD, LaunchpadBlockEntity::tick);
    }

    @Override
    protected InteractionResult useItemOn(ItemStack stack, BlockState state, Level level, BlockPos pos, Player player,
                                          InteractionHand hand, BlockHitResult hitResult) {
        if (!stack.is(Items.WIND_CHARGE) || hand != InteractionHand.MAIN_HAND) {
            return super.useItemOn(stack, state, level, pos, player, hand, hitResult);
        }
        return deposit(stack, level, pos, player, false);
    }

    /**
     * Schleichen + Rechtsklick: Vanilla fragt den Block dann gar nicht (ServerPlayerGameMode#useItemOn
     * unterdrueckt {@code useItemOn} bei gehaltener Umschalttaste), sondern das Item -
     * {@code LaunchpadWindChargeMixin} leitet {@code WindChargeItem#useOn} hierher. Liefert null, wenn
     * es kein schleichender Klick mit der Haupthand auf ein Launchpad ist.
     */
    public static @Nullable InteractionResult bulkDeposit(UseOnContext context) {
        Player player = context.getPlayer();
        if (player == null || !player.isSecondaryUseActive() || context.getHand() != InteractionHand.MAIN_HAND
                || !context.getItemInHand().is(Items.WIND_CHARGE)) {
            return null;
        }
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!(level.getBlockState(pos).getBlock() instanceof LaunchpadBlock pad)) {
            return null;
        }
        return pad.deposit(context.getItemInHand(), level, pos, player, true);
    }

    /** Laedt eine Windkugel (all = false) oder alle aus dem Stapel, hoechstens bis zum Fassungsvermoegen. */
    public InteractionResult deposit(ItemStack stack, Level level, BlockPos pos, Player player, boolean all) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LaunchpadBlockEntity launchpad) {
            int max = maxCharges();
            int current = launchpad.getCharges();
            if (current >= max) {
                player.displayClientMessage(Component.translatable("message.simplebuilding.launchpad.full", max).withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            int added = launchpad.addCharges(all ? stack.getCount() : 1, max);
            if (!player.getAbilities().instabuild) {
                stack.shrink(added);
            }
            level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 1.0f, 1.5f);
            player.displayClientMessage(Component.translatable("message.simplebuilding.launchpad.charges", current + added, max).withStyle(ChatFormatting.GREEN), true);
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    protected InteractionResult useWithoutItem(BlockState state, Level level, BlockPos pos, Player player, BlockHitResult hitResult) {
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LaunchpadBlockEntity launchpad) {
            player.displayClientMessage(Component.translatable("message.simplebuilding.launchpad.charges", launchpad.getCharges(), maxCharges()).withStyle(ChatFormatting.AQUA), true);
        }
        return InteractionResult.SUCCESS;
    }
}
