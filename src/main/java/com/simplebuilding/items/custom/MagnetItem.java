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
import net.minecraft.registry.RegistryKeys;
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
    // Basis-Reichweite etwas erhöht
    private static final double BASE_RANGE = 5.0;
    private static final double BOOSTED_RANGE = 8.0;

    public MagnetItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        if (!(entity instanceof PlayerEntity player)) return;

        // Prüfen, ob das Item aktiv gehalten wird (Main oder Offhand)
        boolean isHeld = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        if (!isHeld) return;

        // Shift deaktiviert den Magneten
        if (player.isSneaking()) return;

        boolean hasEnchantment = hasConstructorsTouch(stack, world);
        double range = hasEnchantment ? BOOSTED_RANGE : BASE_RANGE;
        String filterId = getFilterId(stack);

        // Box um den Spieler
        double x = player.getX();
        double y = player.getY();
        double z = player.getZ();
        Box box = new Box(x - range, y - range, z - range, x + range, y + range, z + range);

        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box, itemEntity -> true);

        for (ItemEntity itemEntity : items) {
            if (itemEntity.cannotPickup() || itemEntity.getOwner() != null) continue;

            if (filterId != null && !filterId.isEmpty()) {
                String itemId = net.minecraft.registry.Registries.ITEM.getId(itemEntity.getStack().getItem()).toString();
                if (!filterId.equals(itemId)) {
                    continue;
                }
            }

            // --- PHYSIK LOGIK ---

            // Zielposition: Spieler-Mitte (leicht erhöht, damit Items nicht in den Füßen stecken)
            Vec3d targetPos = new Vec3d(x, y + 0.5, z);
            Vec3d itemPos = new Vec3d(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());
            Vec3d vecToPlayer = targetPos.subtract(itemPos);
            double distanceSq = vecToPlayer.lengthSquared();

            // Wenn weiter weg als 1 Block: Anziehen
            if (distanceSq > 1.0) {
                // Geschwindigkeit der Anziehung (0.45 ist relativ stark)
                Vec3d pullForce = vecToPlayer.normalize().multiply(0.45);

                // Aktuelle Geschwindigkeit holen
                Vec3d currentVel = itemEntity.getVelocity();

                // Neue Geschwindigkeit berechnen:
                // Wir dämpfen die alte Geschwindigkeit (0.85), damit sie nicht durch die Gegend fliegen,
                // und addieren die Zugkraft.
                Vec3d newVel = currentVel.multiply(0.85).add(pullForce);

                // Wenn das Item am Boden liegt, geben wir einen "Hopser" nach oben,
                // um die Bodenreibung zu brechen.
                if (itemEntity.isOnGround()) {
                    newVel = newVel.add(0, 0.15, 0);
                }

                // SetVelocity markiert das Entity automatisch als "updated"
                itemEntity.setVelocity(newVel);
            }
            // Wenn sehr nah (< 1 Block): Stark bremsen ("verweilen")
            else {
                itemEntity.setVelocity(itemEntity.getVelocity().multiply(0.5));
            }

            // Pickup Delay auf 0 setzen, damit man es sofort einsammeln kann
            itemEntity.setPickupDelay(0);
        }
    }

    @Override
    public ActionResult useOnEntity(ItemStack stack, PlayerEntity player, LivingEntity entity, Hand hand) {
        return ActionResult.PASS;
    }

    @Override
    public ActionResult use(World world, PlayerEntity player, Hand hand) {
        ItemStack stack = player.getStackInHand(hand);

        // Shift-Rechtsklick -> Filter löschen
        if (player.isSneaking()) {
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

        tooltip.accept(Text.literal("Sneak + Right Click to clear filter").formatted(Formatting.DARK_GRAY));
        tooltip.accept(Text.literal("Works in Mainhand or Offhand").formatted(Formatting.BLUE));
    }

    private void setFilterId(ItemStack stack, String id) {
        NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtComponent.copyNbt();

        if (id == null) {
            nbt.remove(FILTER_KEY);
        } else {
            nbt.putString(FILTER_KEY, id);
        }

        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

    private String getFilterId(ItemStack stack) {
        NbtComponent nbtComponent = stack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = nbtComponent.copyNbt();
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