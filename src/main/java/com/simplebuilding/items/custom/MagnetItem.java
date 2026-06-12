package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.entity.LivingEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.network.packet.s2c.play.EntityVelocityUpdateS2CPacket;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.util.math.Box;
import net.minecraft.util.math.Vec3d;
import net.minecraft.world.World;
import org.jspecify.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;

import static com.simplebuilding.util.EnchantmentHelper.*;

public class MagnetItem extends Item {

    private static final String FILTER_KEY = "MagnetFilter";
    private static final double BASE_RANGE = 4.0;
    private static final double BOOSTED_RANGE = 8.0;
    private static final double RANGE_ENCHANTMENT_BOOST = 2.0;
    private static final double SYNC_DISTANCE_SQ = 64 * 64;

    public MagnetItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof PlayerEntity player)) return;

        if (!isHeldInHand(player, stack, slot)) return;

        // Shift deaktiviert den Magneten
        if (player.isSneaking()) return;

        double currentRange = getCurrentRange(stack, world);

        String filterId = getFilterId(stack);

        Box box = player.getBoundingBox().expand(currentRange);
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box, itemEntity -> true);
        Vec3d targetPos = player.getEyePos().subtract(0, 0.5, 0);

        for (ItemEntity itemEntity : items) {
            if (itemEntity.isRemoved() || itemEntity.getStack().isEmpty()) continue;

            if (!passesFilter(itemEntity, filterId)) continue;

            applyMagnetForce(itemEntity, targetPos);

            // Pickup Delay resetten
            itemEntity.setPickupDelay(0);

            syncVelocityToNearbyPlayers(world, itemEntity);
        }
    }

    private static boolean isHeldInHand(PlayerEntity player, ItemStack stack, @Nullable EquipmentSlot slot) {
        if (slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND) {
            return true;
        }

        return player.getMainHandStack() == stack || player.getOffHandStack() == stack;
    }

    private static double getCurrentRange(ItemStack stack, ServerWorld world) {
        double range = hasConstructorsTouch(stack, world) ? BOOSTED_RANGE : BASE_RANGE;
        int rangeLevel = getEnchantmentLevel(stack, world, ModEnchantments.RANGE);
        if (rangeLevel > 0) {
            range += (rangeLevel * RANGE_ENCHANTMENT_BOOST);
        }
        return range;
    }

    private static boolean passesFilter(ItemEntity itemEntity, @Nullable String filterId) {
        if (filterId == null || filterId.isEmpty()) {
            return true;
        }

        String itemId = net.minecraft.util.registry.Registries.ITEM.getId(itemEntity.getStack().getItem()).toString();
        return filterId.equals(itemId);
    }

    private static void applyMagnetForce(ItemEntity itemEntity, Vec3d targetPos) {
        Vec3d itemPos = new Vec3d(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
        Vec3d vec = targetPos.subtract(itemPos);
        double distanceSq = vec.lengthSquared();

        if (distanceSq > 1.0) {
            Vec3d pull = vec.normalize().multiply(0.10);
            Vec3d newVel = itemEntity.getVelocity().multiply(0.80).add(pull);

            if (itemEntity.isOnGround()) {
                newVel = newVel.add(0, 0.15, 0);
            }

            itemEntity.setVelocity(newVel);
            return;
        }

        itemEntity.setVelocity(itemEntity.getVelocity().multiply(0.2));
    }

    private static void syncVelocityToNearbyPlayers(ServerWorld world, ItemEntity itemEntity) {
        EntityVelocityUpdateS2CPacket packet = new EntityVelocityUpdateS2CPacket(itemEntity);
        for (ServerPlayerEntity serverPlayer : world.getPlayers()) {
            if (serverPlayer.squaredDistanceTo(itemEntity) < SYNC_DISTANCE_SQ) {
                serverPlayer.networkHandler.sendPacket(packet);
            }
        }
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, LivingEntity entity, Hand hand) {
        return ActionResult.PASS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);
        if (player.isSneaking() && getFilterId(stack) != null) {
            if (!world.isClient()) {
                setFilterId(stack, null);
                player.sendMessage(Text.literal("Magnet Filter cleared.").formatted(Formatting.YELLOW), true);
                world.playSound(null, player.getBlockPos(), SoundEvents.UI_BUTTON_CLICK.value(), SoundCategory.PLAYERS, 0.5f, 1.0f);
            }
            return ActionResult.SUCCESS;
        }
        return ActionResult.PASS;
    }

    @Override
    @SuppressWarnings("deprecation")
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent component, Consumer<Text> tooltip, TooltipType type) {
        String filter = getFilterId(stack);
        if (filter != null && !filter.isEmpty()) {
            tooltip.accept(Text.literal("Filtering: " + filter).formatted(Formatting.GOLD));
        } else {
            tooltip.accept(Text.literal("No Filter active").formatted(Formatting.GRAY));
        }
        tooltip.accept(Text.literal("Sneak + Right Click to clear").formatted(Formatting.DARK_GRAY));
    }

    private void setFilterId(ItemStack stack, String id) {
        NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtComponent.copyNbt();
        if (id == null) nbt.remove(FILTER_KEY);
        else nbt.putString(FILTER_KEY, id);
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private String getFilterId(ItemStack stack) {
        NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtComponent.copyNbt();
        // Hier den leeren String als Default, falls getString einen braucht
        if (nbt.contains(FILTER_KEY)) {
            return nbt.getString(FILTER_KEY, "");
        }
        return null;
    }
}
