package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.EnchantmentHelper;
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
import net.minecraft.registry.RegistryKeys;
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

public class MagnetItem extends Item {

    private static final String FILTER_KEY = "MagnetFilter";
    private static final double BASE_RANGE = 4.0;
    private static final double BOOSTED_RANGE = 8.0;

    public MagnetItem(Settings settings) {
        super(settings);
    }

    // WICHTIG: Dies ist die korrekte Signatur für deine Version (laut deiner Item.java)
    // ServerWorld statt World, EquipmentSlot statt int slot.
    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        // Da der Parameter schon ServerWorld ist, brauchen wir kein isClient Check mehr.

        if (!(entity instanceof PlayerEntity player)) return;

        // Prüfen ob Item in Main- oder Offhand ist.
        // slot kann null sein, daher Vorsicht.
        boolean isHeldMain = slot == EquipmentSlot.MAINHAND;
        boolean isHeldOff = slot == EquipmentSlot.OFFHAND;

        // Sicherheitshalber prüfen wir auch das Inventar, falls slot null ist
        if (!isHeldMain && !isHeldOff) {
            if (player.getMainHandStack() == stack) isHeldMain = true;
            else if (player.getOffHandStack() == stack) isHeldOff = true;
            else return; // Nicht in der Hand
        }

        // Shift deaktiviert
        if (player.isSneaking()) return;

        boolean hasEnchantment = hasConstructorsTouch(stack, world);
        double range = hasEnchantment ? BOOSTED_RANGE : BASE_RANGE;
        String filterId = getFilterId(stack);

        // Box um den Spieler
        Box box = player.getBoundingBox().expand(range);
        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box, itemEntity -> true);

        for (ItemEntity itemEntity : items) {
            if (itemEntity.isRemoved() || itemEntity.getStack().isEmpty()) continue;

            // Filter Check
            if (filterId != null && !filterId.isEmpty()) {
                String itemId = net.minecraft.registry.Registries.ITEM.getId(itemEntity.getStack().getItem()).toString();
                if (!filterId.equals(itemId)) continue;
            }

            // --- PHYSIK ---
            // Wir nutzen getEyePos() wie von dir gewünscht
            Vec3d targetPos = player.getEyePos().subtract(0, 0.5, 0);

            // Item Position (ItemEntity hat kein getEyePos, wir nehmen getX/Y/Z)
            Vec3d itemPos = new Vec3d(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());

            Vec3d vec = targetPos.subtract(itemPos);
            double distanceSq = vec.lengthSquared();

            if (distanceSq > 1.0) {
                // Anziehen
                Vec3d pull = vec.normalize().multiply(0.10);
                Vec3d currentVel = itemEntity.getVelocity();

                // Neue Velocity berechnen
                Vec3d newVel = currentVel.multiply(0.80).add(pull);

                if (itemEntity.isOnGround()) {
                    newVel = newVel.add(0, 0.15, 0);
                }

                itemEntity.setVelocity(newVel);
            } else {
                // Bremsen
                itemEntity.setVelocity(itemEntity.getVelocity().multiply(0.2));
            }

            // Pickup Delay resetten
            itemEntity.setPickupDelay(0);

            // Update an Clients senden
            world.getPlayers().stream()
                    .filter(p -> p.squaredDistanceTo(itemEntity) < 64 * 64)
                    .forEach(p -> ((ServerPlayerEntity)p).networkHandler.sendPacket(new EntityVelocityUpdateS2CPacket(itemEntity)));
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

    private boolean hasConstructorsTouch(ItemStack stack, World world) {
        var registry = world.getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
        var enchantmentEntry = registry.getOptional(ModEnchantments.CONSTRUCTORS_TOUCH);
        if (enchantmentEntry.isPresent()) {
            return EnchantmentHelper.getLevel(enchantmentEntry.get(), stack) > 0;
        }
        return false;
    }
}