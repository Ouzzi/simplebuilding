package com.simplebuilding.mixin.client;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.networking.MasterBuilderPickPayload;
import net.fabricmc.fabric.api.client.networking.v1.ClientPlayNetworking;
import net.minecraft.client.MinecraftClient;
import net.minecraft.client.network.ClientPlayerEntity;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.hit.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(MinecraftClient.class)
public abstract class MinecraftClientMixin {

    @Shadow @Nullable public ClientPlayerEntity player;
    @Shadow @Nullable public HitResult crosshairTarget;

    @Inject(method = "doItemPick", at = @At("HEAD"), cancellable = true)
    private void onDoItemPick(CallbackInfo ci) {
        if (this.player == null || this.crosshairTarget == null) return;
        if (this.player.isCreative()) return;

        ItemStack targetStack = null;
        if (this.crosshairTarget.getType() == HitResult.Type.BLOCK) {
            var blockHit = (net.minecraft.util.hit.BlockHitResult) this.crosshairTarget;
            var state = this.player.getEntityWorld().getBlockState(blockHit.getBlockPos());

            // FIX: 'getPickStack' ist protected in AbstractBlock.
            // Wir nutzen new ItemStack, da wir später nur den Item-Typ vergleichen (ItemStack.areItemsEqual).
            targetStack = new ItemStack(state.getBlock());

        } else if (this.crosshairTarget.getType() == HitResult.Type.ENTITY) {
            var entityHit = (net.minecraft.util.hit.EntityHitResult) this.crosshairTarget;
            targetStack = entityHit.getEntity().getPickBlockStack();
        }

        if (targetStack == null || targetStack.isEmpty()) return;

        int slot = this.player.getInventory().getSlotWithStack(targetStack);
        if (slot != -1) {
            return;
        }

        PlayerInventory inv = this.player.getInventory();
        for (int i = 0; i < inv.size(); i++) {
            ItemStack stack = inv.getStack(i);

            if (stack.getItem() instanceof ReinforcedBundleItem) {
                // FIX: get -> getOrThrow
                var registry = this.player.getEntityWorld().getRegistryManager().getOrThrow(RegistryKeys.ENCHANTMENT);
                // FIX: getEntry -> getOptional (für RegistryKey)
                var masterBuilderEntry = registry.getOptional(ModEnchantments.MASTER_BUILDER);

                if (masterBuilderEntry.isPresent() && EnchantmentHelper.getLevel(masterBuilderEntry.get(), stack) > 0) {

                    BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
                    if (contents != null) {
                        boolean foundInBundle = false;
                        for (ItemStack contentStack : contents.iterate()) {
                            if (ItemStack.areItemsEqual(contentStack, targetStack)) {
                                foundInBundle = true;
                                break;
                            }
                        }

                        if (foundInBundle) {
                            ClientPlayNetworking.send(new MasterBuilderPickPayload(targetStack));
                            ci.cancel(); 
                            return;
                        }
                    }
                }
            }
        }
    }
}