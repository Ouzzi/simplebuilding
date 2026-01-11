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
    private static final double BASE_RANGE = 3.5;
    private static final double BOOSTED_RANGE = 6.0;

    public MagnetItem(Settings settings) {
        super(settings);
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, @Nullable EquipmentSlot slot) {
        // Läuft nur auf dem Server (durch ServerWorld garantiert)

        if (!(entity instanceof PlayerEntity player)) return;

        // Prüfen, ob das Item gehalten wird (Mainhand oder Offhand)
        // 'slot' ist null, wenn es im Inventar liegt aber nicht ausgerüstet ist.
        boolean isHeld = slot == EquipmentSlot.MAINHAND || slot == EquipmentSlot.OFFHAND;
        if (!isHeld) return;

        boolean hasEnchantment = hasConstructorsTouch(stack, world);
        double range = hasEnchantment ? BOOSTED_RANGE : BASE_RANGE;

        String filterId = getFilterId(stack);

        Box box = new Box(
                player.getX() - range, player.getY() - range, player.getZ() - range,
                player.getX() + range, player.getY() + range, player.getZ() + range
        );

        List<ItemEntity> items = world.getEntitiesByClass(ItemEntity.class, box, itemEntity -> true);

        for (ItemEntity itemEntity : items) {
            if (itemEntity.cannotPickup() || itemEntity.getOwner() != null) continue;

            if (filterId != null && !filterId.isEmpty()) {
                String itemId = net.minecraft.registry.Registries.ITEM.getId(itemEntity.getStack().getItem()).toString();
                if (!filterId.equals(itemId)) {
                    continue;
                }
            }

            // Anzieh-Physik
            Vec3d targetPos = new Vec3d(player.getX(), player.getY() + 0.5, player.getZ());
            Vec3d itemPos = new Vec3d(itemEntity.getX(), itemEntity.getY(), itemEntity.getZ());

            Vec3d direction = targetPos.subtract(itemPos).normalize().multiply(0.15);
            itemEntity.setVelocity(itemEntity.getVelocity().add(direction));
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
        if (nbtComponent.copyNbt().contains(FILTER_KEY)) {
            return nbtComponent.copyNbt().getString(FILTER_KEY, "");
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