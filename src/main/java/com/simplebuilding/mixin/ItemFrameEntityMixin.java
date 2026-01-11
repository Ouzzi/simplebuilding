package com.simplebuilding.mixin;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Formatting;
import net.minecraft.util.Hand;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(ItemFrameEntity.class)
public abstract class ItemFrameEntityMixin {

    @Shadow public abstract ItemStack getHeldItemStack();

    @Inject(method = "interact", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$onInteractWithMagnet(PlayerEntity player, Hand hand, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack handStack = player.getStackInHand(hand);

        // Prüfen ob Spieler den Magneten hält
        if (handStack.isOf(ModItems.MAGNET)) {

            // FIX: .getOrThrow(...) verwenden statt .get(...)
            var registry = player.getEntityWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);

            // FIX: .getOptional(...) verwenden, um das Enchantment zu finden
            var enchantmentEntry = registry.getOptional(ModEnchantments.CONSTRUCTORS_TOUCH);

            boolean hasEnchantment = false;
            if (enchantmentEntry.isPresent()) {
                hasEnchantment = EnchantmentHelper.getLevel(enchantmentEntry.get(), handStack) > 0;
            }

            // Nur interagieren, wenn Enchantment da ist und ItemFrame nicht leer ist
            if (hasEnchantment && !this.getHeldItemStack().isEmpty()) {
                if (!player.getEntityWorld().isClient()) {
                    // Item ID aus dem Frame holen
                    String itemId = net.minecraft.registry.Registries.ITEM.getId(this.getHeldItemStack().getItem()).toString();

                    // Filter auf Magnet speichern (NBT Component Update)
                    NbtComponent nbtComponent = handStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
                    NbtCompound nbt = nbtComponent.copyNbt();
                    nbt.putString("MagnetFilter", itemId);
                    handStack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));

                    player.sendMessage(Text.literal("Magnet Filter set to: " + itemId).formatted(Formatting.GREEN), true);
                    player.playSound(SoundEvents.BLOCK_RESPAWN_ANCHOR_SET_SPAWN, 0.5f, 1.5f);
                }

                // Erfolg zurückgeben und Vanilla-Interaktion (Drehen des Items) verhindern
                cir.setReturnValue(ActionResult.SUCCESS);
            }
        }
    }
}