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
 * Launchpad (Simple Tweaks): mit Windkugeln laden (Rechtsklick), 3 s stehen, Start. Die
 * Enderit-Stufe fasst 32 statt 16 Ladungen und schuetzt bis zur naechsten Landung vor Fallschaden.
 */
public class LaunchpadBlock extends WaterloggedPadBlock {
    public static final MapCodec<LaunchpadBlock> CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            propertiesCodec(),
            Codec.BOOL.fieldOf("enderite").forGetter(LaunchpadBlock::isEnderite)
    ).apply(i, LaunchpadBlock::new));

    private final boolean enderite;

    public LaunchpadBlock(BlockBehaviour.Properties properties, boolean enderite) {
        super(properties, Block.box(1, 0, 1, 15, 1, 15), PadOwnership.OWNER_PAD, PadOwnership.STRANGER_PAD);
        this.enderite = enderite;
    }

    public boolean isEnderite() {
        return enderite;
    }

    public int maxCharges() {
        return enderite ? 32 : 16;
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
        if (!level.isClientSide() && level.getBlockEntity(pos) instanceof LaunchpadBlockEntity launchpad) {
            int current = launchpad.getCharges();
            if (current >= maxCharges()) {
                player.displayClientMessage(Component.translatable("message.simplebuilding.launchpad.full", maxCharges()).withStyle(ChatFormatting.RED), true);
                return InteractionResult.FAIL;
            }
            launchpad.addCharge(maxCharges());
            if (!player.getAbilities().instabuild) {
                stack.shrink(1);
            }
            level.playSound(null, pos, SoundEvents.BUNDLE_INSERT, SoundSource.BLOCKS, 1.0f, 1.5f);
            player.displayClientMessage(Component.translatable("message.simplebuilding.launchpad.charges", current + 1, maxCharges()).withStyle(ChatFormatting.GREEN), true);
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
