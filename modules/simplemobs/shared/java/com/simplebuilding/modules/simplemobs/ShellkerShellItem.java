package com.simplebuilding.modules.simplemobs;

import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.ConversionParams;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.Shulker;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Shellker Shell: right-click a Shulker to dress it in the shell - it becomes a Shellker (the only way to farm them). */
public class ShellkerShellItem extends Item {
    public ShellkerShellItem(Properties properties) {
        super(properties);
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity target, InteractionHand hand) {
        if (!(target instanceof Shulker shulker) || target instanceof ShellkerEntity || !target.isAlive()) return InteractionResult.PASS;
        if (!(player.level() instanceof ServerLevel level)) return InteractionResult.SUCCESS;
        return dress(level, shulker, stack, player) ? InteractionResult.SUCCESS_SERVER : InteractionResult.PASS;
    }

    /** Converts the Shulker into a Shellker on the same face and uses up one shell. */
    static boolean dress(ServerLevel level, Shulker shulker, ItemStack stack, Player player) {
        var face = shulker.getAttachFace();
        ShellkerEntity shellker = shulker.convertTo(EndNetherMobs.SHELLKER, ConversionParams.single(shulker, false, false), s -> s.attachTo(face));
        if (shellker == null) return false;
        stack.consume(1, player);
        level.playSound(null, shellker.blockPosition(), SoundEvents.ARMOR_EQUIP_TURTLE.value(), shellker.getSoundSource(), 1.0F, 0.7F);
        return true;
    }
}
