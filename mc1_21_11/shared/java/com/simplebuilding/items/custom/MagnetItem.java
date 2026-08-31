package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.network.protocol.game.ClientboundSetEntityMotionPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import static com.simplebuilding.util.EnchantmentHelper.*;

public class MagnetItem extends Item {

    private static final String FILTER_KEY = "MagnetFilter";
    private static final double BASE_RANGE = 4.0;
    private static final double BOOSTED_RANGE = 8.0;
    private static final double RANGE_ENCHANTMENT_BOOST = 2.0;
    private static final double SYNC_DISTANCE_SQ = 64 * 64;

    public MagnetItem(Properties settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof Player player)) return;

        if (!isHeldInHand(player, stack, slot)) return;

        // Shift deaktiviert den Magneten
        if (player.isShiftKeyDown()) return;

        double currentRange = getCurrentRange(stack, world);

        String filterId = getFilterId(stack);

        AABB box = player.getBoundingBox().inflate(currentRange);
        List<ItemEntity> items = world.getEntitiesOfClass(ItemEntity.class, box, itemEntity -> true);
        Vec3 targetPos = player.getEyePosition().subtract(0, 0.5, 0);

        for (ItemEntity itemEntity : items) {
            if (itemEntity.isRemoved() || itemEntity.getItem().isEmpty()) continue;

            if (!passesFilter(itemEntity, filterId)) continue;

            applyMagnetForce(itemEntity, targetPos);

            // Pickup Delay resetten
            itemEntity.setPickUpDelay(0);

            syncVelocityToNearbyPlayers(world, itemEntity);
        }
    }

    private static boolean isHeldInHand(Player player, ItemStack stack, @Nullable EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
            return true;
        }

        return player.getMainHandItem() == stack || player.getOffhandItem() == stack;
    }

    private static double getCurrentRange(ItemStack stack, ServerLevel world) {
        double range = hasConstructorsTouch(stack, world) ? BOOSTED_RANGE : BASE_RANGE;
        int rangeLevel = getMagnetRangeLevel(stack, world);
        if (rangeLevel > 0) {
            range += (rangeLevel * RANGE_ENCHANTMENT_BOOST);
        }
        return range;
    }

    private static boolean passesFilter(ItemEntity itemEntity, @Nullable String filterId) {
        if (filterId == null || filterId.isEmpty()) {
            return true;
        }

        String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(itemEntity.getItem().getItem()).toString();
        return filterId.equals(itemId);
    }

    private static void applyMagnetForce(ItemEntity itemEntity, Vec3 targetPos) {
        Vec3 itemPos = new Vec3(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
        Vec3 vec = targetPos.subtract(itemPos);
        double distanceSq = vec.lengthSqr();

        if (distanceSq > 1.0) {
            Vec3 pull = vec.normalize().scale(0.10);
            Vec3 newVel = itemEntity.getDeltaMovement().scale(0.80).add(pull);

            if (itemEntity.onGround()) {
                newVel = newVel.add(0, 0.15, 0);
            }

            itemEntity.setDeltaMovement(newVel);
            return;
        }

        itemEntity.setDeltaMovement(itemEntity.getDeltaMovement().scale(0.2));
    }

    private static void syncVelocityToNearbyPlayers(ServerLevel world, ItemEntity itemEntity) {
        ClientboundSetEntityMotionPacket packet = new ClientboundSetEntityMotionPacket(itemEntity);
        for (ServerPlayer serverPlayer : world.players()) {
            if (serverPlayer.distanceToSqr(itemEntity) < SYNC_DISTANCE_SQ) {
                serverPlayer.connection.send(packet);
            }
        }
    }

    @Override
    public InteractionResult interactLivingEntity(ItemStack stack, Player player, LivingEntity entity, InteractionHand hand) {
        return InteractionResult.PASS;
    }

    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (player.isShiftKeyDown() && getFilterId(stack) != null) {
            if (!world.isClientSide()) {
                setFilterId(stack, null);
                player.displayClientMessage(Component.literal("Magnet Filter cleared.").withStyle(ChatFormatting.YELLOW), true);
                world.playSound(null, player.blockPosition(), SoundEvents.UI_BUTTON_CLICK.value(), SoundSource.PLAYERS, 0.5f, 1.0f);
            }
            return InteractionResult.SUCCESS;
        }
        return InteractionResult.PASS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay component, Consumer<Component> tooltip, TooltipFlag type) {
        String filter = getFilterId(stack);
        if (filter != null && !filter.isEmpty()) {
            tooltip.accept(Component.literal("Filtering: " + filter).withStyle(ChatFormatting.GOLD));
        } else {
            tooltip.accept(Component.literal("No Filter active").withStyle(ChatFormatting.GRAY));
        }
        tooltip.accept(Component.literal("Sneak + Right Click to clear").withStyle(ChatFormatting.DARK_GRAY));
    }

    private void setFilterId(ItemStack stack, String id) {
        CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtComponent.copyTag();
        if (id == null) nbt.remove(FILTER_KEY);
        else nbt.putString(FILTER_KEY, id);
        stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
    }

    private String getFilterId(ItemStack stack) {
        CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtComponent.copyTag();
        // Hier den leeren String als Default, falls getString einen braucht
        if (nbt.contains(FILTER_KEY)) {
            return nbt.getStringOr(FILTER_KEY, "");
        }
        return null;
    }
}