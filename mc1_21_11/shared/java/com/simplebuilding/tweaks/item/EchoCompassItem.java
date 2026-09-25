package com.simplebuilding.tweaks.item;

import java.util.Optional;
import java.util.Set;
import net.minecraft.ChatFormatting;
import net.minecraft.core.BlockPos;
import net.minecraft.core.GlobalPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.particles.ParticleTypes;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.effect.MobEffectInstance;
import net.minecraft.world.effect.MobEffects;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.LodestoneTracker;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Blocks;
import org.jetbrains.annotations.Nullable;

/**
 * Echo-Kompass (ersetzt das Datenpaket "Echo Compass", das Simple Tweaks per libs/ einband; neu
 * geschrieben, kein Code daraus). Rechtsklick auf einen Leitstein verknuepft, Benutzen teleportiert
 * auf den Block darueber und verbraucht eine Enderperle; der Kompass bleibt und nimmt 1 Haltbarkeit
 * ueber {@link ItemStack#hurtAndBreak} - damit wirken Unbreaking und Mending (Simple-Tweaks-Bug:
 * das Datenpaket zog die Haltbarkeit direkt ab).
 */
public class EchoCompassItem extends Item {
    public static final int DURABILITY = 64;
    public static final int COOLDOWN_TICKS = 120;

    public EchoCompassItem(Item.Properties properties) {
        super(properties);
    }

    public static @Nullable GlobalPos target(ItemStack stack) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        return tracker == null ? null : tracker.target().orElse(null);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel level, Entity owner, @Nullable EquipmentSlot slot) {
        LodestoneTracker tracker = stack.get(DataComponents.LODESTONE_TRACKER);
        if (tracker != null) {
            LodestoneTracker updated = tracker.tick(level);
            if (updated != tracker) {
                stack.set(DataComponents.LODESTONE_TRACKER, updated);
            }
        }
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        Level level = context.getLevel();
        BlockPos pos = context.getClickedPos();
        if (!level.getBlockState(pos).is(Blocks.LODESTONE)) {
            return super.useOn(context);
        }
        if (!level.isClientSide()) {
            context.getItemInHand().set(DataComponents.LODESTONE_TRACKER,
                    new LodestoneTracker(Optional.of(GlobalPos.of(level.dimension(), pos)), true));
            level.playSound(null, pos, SoundEvents.LODESTONE_COMPASS_LOCK, SoundSource.PLAYERS, 1.0f, 1.0f);
            level.playSound(null, pos, SoundEvents.ENCHANTMENT_TABLE_USE, SoundSource.PLAYERS, 1.0f, 1.5f);
            level.playSound(null, pos, SoundEvents.WARDEN_ATTACK_IMPACT, SoundSource.PLAYERS, 1.0f, 0.0f);
            if (level instanceof ServerLevel serverLevel) {
                spawnEffectParticles(serverLevel, pos.getX() + 0.5, pos.getY() + 1.0, pos.getZ() + 0.5);
            }
            if (context.getPlayer() != null) {
                context.getPlayer().addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, true, false, true));
            }
        }
        return InteractionResult.SUCCESS;
    }

    @Override
    public InteractionResult use(Level level, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (level.isClientSide() || !(player instanceof ServerPlayer serverPlayer)) {
            return InteractionResult.SUCCESS;
        }
        return teleport(serverPlayer, hand, stack) ? InteractionResult.SUCCESS : InteractionResult.FAIL;
    }

    /** Der eigentliche Sprung; true, wenn teleportiert wurde. */
    public static boolean teleport(ServerPlayer player, InteractionHand hand, ItemStack stack) {
        GlobalPos target = target(stack);
        if (target == null) {
            player.displayClientMessage(Component.translatable("message.simplebuilding.echo_compass.unlinked").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (player.getCooldowns().isOnCooldown(stack)) {
            return false;
        }
        ServerLevel targetLevel = player.level().getServer().getLevel(target.dimension());
        if (targetLevel == null || !targetLevel.getBlockState(target.pos()).is(Blocks.LODESTONE)) {
            player.displayClientMessage(Component.translatable("message.simplebuilding.echo_compass.lodestone_missing").withStyle(ChatFormatting.RED), true);
            return false;
        }
        if (!player.getAbilities().instabuild && !consumeEnderPearl(player.getInventory())) {
            player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 40, 0, true, false, true));
            player.level().playSound(null, player.blockPosition(), SoundEvents.FIRE_EXTINGUISH, SoundSource.PLAYERS, 0.25f, 2.0f);
            player.displayClientMessage(Component.translatable("message.simplebuilding.echo_compass.no_pearl").withStyle(ChatFormatting.RED), true);
            return false;
        }

        double x = target.pos().getX() + 0.5;
        double y = target.pos().getY() + 1.0;
        double z = target.pos().getZ() + 0.5;
        player.teleportTo(targetLevel, x, y, z, Set.of(), player.getYRot(), player.getXRot(), true);
        targetLevel.playSound(null, x, y, z, SoundEvents.PLAYER_TELEPORT, SoundSource.PLAYERS, 1.0f, 0.0f);
        targetLevel.playSound(null, x, y, z, SoundEvents.WARDEN_SONIC_BOOM, SoundSource.PLAYERS, 1.0f, 1.0f);
        spawnEffectParticles(targetLevel, x, y, z);
        player.addEffect(new MobEffectInstance(MobEffects.BLINDNESS, 20, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.GLOWING, 60, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOW_FALLING, 20, 0, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.SLOWNESS, 20, 9, true, false, true));
        player.addEffect(new MobEffectInstance(MobEffects.NAUSEA, 120, 0, true, false, true));

        player.getCooldowns().addCooldown(stack, COOLDOWN_TICKS);
        stack.hurtAndBreak(1, player, hand.asEquipmentSlot());
        return true;
    }

    /** Eine Enderperle ohne eigene Daten verbrauchen (wie das Datenpaket: {@code ender_pearl[!custom_data]}). */
    public static boolean consumeEnderPearl(Inventory inventory) {
        for (int i = 0; i < inventory.getContainerSize(); i++) {
            ItemStack stack = inventory.getItem(i);
            if (stack.is(Items.ENDER_PEARL) && !stack.has(DataComponents.CUSTOM_DATA)) {
                stack.shrink(1);
                inventory.setChanged();
                return true;
            }
        }
        return false;
    }

    private static void spawnEffectParticles(ServerLevel level, double x, double y, double z) {
        level.sendParticles(ParticleTypes.PORTAL, x, y, z, 100, 0.5, 1, 0.5, 0);
        level.sendParticles(ParticleTypes.END_ROD, x, y, z, 50, 0.5, 1, 0.5, 0.1);
        level.sendParticles(ParticleTypes.SCULK_SOUL, x, y, z, 50, 0.5, 1, 0.5, 0.1);
    }

    @Override
    public boolean isFoil(ItemStack stack) {
        return true;
    }
}
