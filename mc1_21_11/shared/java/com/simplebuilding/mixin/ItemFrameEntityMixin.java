package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.damagesource.DamageSource;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.decoration.ItemFrame;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.Vec3;
import net.minecraft.world.level.storage.ValueInput;
import net.minecraft.world.level.storage.ValueOutput;
import com.simplebuilding.enchantment.ModEnchantments;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrame.class)
public abstract class ItemFrameEntityMixin {

    @Shadow public abstract ItemStack getItem();
    // Keine Shadow Methoden für setInvisible/isInvisible nötig, wir nutzen Casts

    @Unique
    private boolean simplebuilding$locked = false;

    // --- DATEN ---
    @Inject(method = "addAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$writeCustomData(ValueOutput view, CallbackInfo ci) {
        view.putBoolean("SimpleBuildingLocked", this.simplebuilding$locked);
    }

    @Inject(method = "readAdditionalSaveData", at = @At("TAIL"))
    private void simplebuilding$readCustomData(ValueInput view, CallbackInfo ci) {
        this.simplebuilding$locked = view.getBooleanOr("SimpleBuildingLocked", false);
    }

    // --- SCHUTZ ---
    @Inject(method = "hurtServer", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$preventDamageIfLocked(ServerLevel world, DamageSource source, float amount, CallbackInfoReturnable<Boolean> cir) {
        if (this.simplebuilding$locked && source.getDirectEntity() instanceof Player player && !player.isCreative()) {
            cir.setReturnValue(false);
        }
    }

    // --- INTERAKTIONEN ---
    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$onInteract(Player player, InteractionHand hand, Vec3 hitPos, CallbackInfoReturnable<InteractionResult> cir) {
        ItemStack handStack = player.getItemInHand(hand);
        boolean isClient = player.level().isClientSide();
        ItemFrame itemFrame = (ItemFrame) (Object) this;

        // SNEAK INTERAKTIONEN
        if (player.isShiftKeyDown()) {

            // 1. LOCKEN (Glas) - Höchste Priorität
            if (handStack.is(Items.GLASS_PANE) && !this.getItem().isEmpty() && !this.simplebuilding$locked) {
                if (!isClient) {
                    this.simplebuilding$locked = true;
                    if (!player.isCreative()) handStack.shrink(1);
                    player.playSound(SoundEvents.GLASS_PLACE, 1.0f, 1.0f);
                    player.displayClientMessage(Component.literal("Item Frame gesperrt (Locked).").withStyle(ChatFormatting.AQUA), true);
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            // 2. ENTSPERREN (Wenn gelockt)
            if (this.simplebuilding$locked) {
                if (!isClient) {
                    this.simplebuilding$locked = false;
                    player.drop(new ItemStack(Items.GLASS_PANE), false);
                    player.playSound(SoundEvents.GLASS_BREAK, 1.0f, 1.0f);
                    player.displayClientMessage(Component.literal("Item Frame entsperrt.").withStyle(ChatFormatting.GREEN), true);
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            // 3. UNSICHTBAR MACHEN (Schere - nur wenn noch sichtbar)
            // Hinweis: Wir nutzen !isInvisible() hier.
            if (handStack.is(Items.SHEARS) && !this.getItem().isEmpty() && !itemFrame.isInvisible()) {
                if (!isClient) {
                    itemFrame.setInvisible(true);
                    player.playSound(SoundEvents.SHEEP_SHEAR, 1.0f, 1.2f);
                    player.displayClientMessage(Component.literal("Item Frame unsichtbar gemacht.").withStyle(ChatFormatting.GRAY), true);
                    if (!player.isCreative()) {
                        EquipmentSlot slot = (hand == InteractionHand.MAIN_HAND) ? EquipmentSlot.MAINHAND : EquipmentSlot.OFFHAND;
                        handStack.hurtAndBreak(1, player, slot);
                    }
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }

            // 4. SICHTBAR MACHEN (Fallback, wenn unsichtbar)
            if (itemFrame.isInvisible()) {
                if (!isClient) {
                    itemFrame.setInvisible(false);
                    player.playSound(SoundEvents.BRUSH_GENERIC, 1.0f, 1.0f);
                    player.displayClientMessage(Component.literal("Item Frame sichtbar gemacht.").withStyle(ChatFormatting.YELLOW), true);
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
        }

        // NORMALE INTERAKTIONEN
        if (handStack.is(ModItems.MAGNET)) {
            var registry = player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
            var enchantmentEntry = registry.get(ModEnchantments.CONSTRUCTORS_TOUCH);
            boolean hasEnchantment = false;
            if (enchantmentEntry.isPresent()) {
                hasEnchantment = EnchantmentHelper.getItemEnchantmentLevel(enchantmentEntry.get(), handStack) > 0;
            }
            if (hasEnchantment && !this.getItem().isEmpty()) {
                if (!isClient) {
                    String itemId = net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(this.getItem().getItem()).toString();
                    CustomData nbtComponent = handStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
                    CompoundTag nbt = nbtComponent.copyTag();
                    nbt.putString("MagnetFilter", itemId);
                    handStack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
                    player.displayClientMessage(Component.literal("Magnet Filter set to: " + itemId).withStyle(ChatFormatting.GREEN), true);
                    player.playSound(SoundEvents.RESPAWN_ANCHOR_SET_SPAWN, 0.5f, 1.5f);
                }
                cir.setReturnValue(InteractionResult.SUCCESS);
                return;
            }
        }

        if (this.simplebuilding$locked) {
            cir.setReturnValue(InteractionResult.FAIL);
        }
    }
}