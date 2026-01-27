package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.damage.DamageSource;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import net.minecraft.storage.ReadView;
import net.minecraft.storage.WriteView;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameEntityMixin {

    @Shadow public abstract ItemStack getHeldItemStack();
    // Keine Shadow Methoden für setInvisible/isInvisible nötig, wir nutzen Casts

    @Unique
    private boolean simplebuilding$locked = false;

    // --- DATEN ---
    @Inject(method = "writeCustomData", at = @At("TAIL"))
    private void simplebuilding$writeCustomData(WriteView view, CallbackInfo ci) {
        view.putBoolean("SimpleBuildingLocked", this.simplebuilding$locked);
    }

    @Inject(method = "readCustomData", at = @At("TAIL"))
    private void simplebuilding$readCustomData(ReadView view, CallbackInfo ci) {
        this.simplebuilding$locked = view.getBoolean("SimpleBuildingLocked", false);
    }

    // --- SCHUTZ ---
    @Inject(method = "damage", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$preventDamageIfLocked(ServerWorld world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.simplebuilding$locked && source.getSource() instanceof PlayerEntity player && !player.isCreative()) {
            cir.setReturnValue(false);
        }
    }

    // --- INTERAKTIONEN ---
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$onInteract(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack handStack = player.getStackInHand(hand);
        boolean isClient = player.getEntityWorld().isClient();
        ItemFrameEntity itemFrame = (ItemFrameEntity) (Object) this;

        // SNEAK INTERAKTIONEN
        if (player.isSneaking()) {

            // 1. LOCKEN (Glas) - Höchste Priorität
            if (handStack.isOf(Items.GLASS_PANE) && !this.getHeldItemStack().isEmpty() && !this.simplebuilding$locked) {
                if (!isClient) {
                    this.simplebuilding$locked = true;
                    if (!player.isCreative()) handStack.decrement(1);
                    player.playSound(SoundEvents.BLOCK_GLASS_PLACE, 1.0f, 1.0f);
                    player.sendMessage(Text.literal("Item Frame gesperrt (Locked).").formatted(Formatting.AQUA), true);
                }
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            // 2. ENTSPERREN (Wenn gelockt)
            if (this.simplebuilding$locked) {
                if (!isClient) {
                    this.simplebuilding$locked = false;
                    player.dropItem(new ItemStack(Items.GLASS_PANE), false);
                    player.playSound(SoundEvents.BLOCK_GLASS_BREAK, 1.0f, 1.0f);
                    player.sendMessage(Text.literal("Item Frame entsperrt.").formatted(Formatting.GREEN), true);
                }
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            // 3. UNSICHTBAR MACHEN (Schere - nur wenn noch sichtbar)
            // Hinweis: Wir nutzen !isInvisible() hier.
            if (handStack.isOf(Items.SHEARS) && !this.getHeldItemStack().isEmpty() && !itemFrame.isInvisible()) {
                if (!isClient) {
                    itemFrame.setInvisible(true);
                    player.playSound(SoundEvents.ENTITY_SHEEP_SHEAR, 1.0f, 1.2f);
                    player.sendMessage(Text.literal("Item Frame unsichtbar gemacht.").formatted(Formatting.GRAY), true);
                    if (!player.isCreative()) {
                        EquipmentSlot slot = (hand == Hand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                        handStack.damage(1, player, slot);
                    }
                }
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }

            // 4. SICHTBAR MACHEN (Fallback, wenn unsichtbar)
            if (itemFrame.isInvisible()) {
                if (!isClient) {
                    itemFrame.setInvisible(false);
                    player.playSound(SoundEvents.ITEM_BRUSH_BRUSHING_GENERIC, 1.0f, 1.0f);
                    player.sendMessage(Text.literal("Item Frame sichtbar gemacht.").formatted(Formatting.YELLOW), true);
                }
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }
        }

        // NORMALE INTERAKTIONEN
        if (handStack.isOf(ModItems.MAGNET)) {
            var registry = player.getEntityWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
            var enchantmentEntry = registry.getOptional(ModEnchantments.CONSTRUCTORS_TOUCH);
            boolean hasEnchantment = false;
            if (enchantmentEntry.isPresent()) {
                hasEnchantment = EnchantmentHelper.getLevel(enchantmentEntry.get(), handStack) > 0;
            }
            if (hasEnchantment && !this.getHeldItemStack().isEmpty()) {
                if (!isClient) {
                    String itemId = net.minecraft.registry.Registries.ITEM.getId(this.getHeldItemStack().getItem()).toString();
                    NbtComponent nbtComponent = handStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                    NbtCompound nbt = nbtComponent.copyNbt();
                    nbt.putString("MagnetFilter", itemId);
                    handStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
                    player.sendMessage(Text.literal("Magnet Filter set to: " + itemId).formatted(Formatting.GREEN), true);
                    player.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.5f, 1.5f);
                }
                cir.setReturnValue(ActionResult.SUCCESS);
                return;
            }
        }

        if (this.simplebuilding$locked) {
            cir.setReturnValue(ActionResult.FAIL);
        }
    }
}