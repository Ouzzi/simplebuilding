package com.simplebuilding.mixin.client;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.networking.MasterBuilderPickPayload;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.client.Minecraft;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.enchantment.EnchantmentHelper;
import net.minecraft.world.phys.HitResult;
import org.jetbrains.annotations.Nullable;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;

@Mixin(Minecraft.class)
public abstract class MinecraftClientMixin {

    @Shadow @Nullable public LocalPlayer player;
    @Shadow @Nullable public HitResult hitResult;

    // MC 1.21.11: Die Methode heisst hier noch pickBlock() (ab 26.2 pickBlockOrEntity()).
    @Inject(method = "pickBlock", at = @At("HEAD"), cancellable = true)
    private void onDoItemPick(CallbackInfo ci) {
        if (this.player == null || this.hitResult == null) return;
        if (this.player.isCreative()) return;

        ItemStack targetStack = null;
        if (this.hitResult.getType() == HitResult.Type.BLOCK) {
            var blockHit = (net.minecraft.world.phys.BlockHitResult) this.hitResult;
            var state = this.player.level().getBlockState(blockHit.getBlockPos());

            // FIX: 'getPickStack' ist protected in AbstractBlock.
            // Wir nutzen new ItemStack, da wir später nur den Item-Typ vergleichen (ItemStack.areItemsEqual).
            targetStack = new ItemStack(state.getBlock());

        } else if (this.hitResult.getType() == HitResult.Type.ENTITY) {
            var entityHit = (net.minecraft.world.phys.EntityHitResult) this.hitResult;
            targetStack = entityHit.getEntity().getPickResult();
        }

        if (targetStack == null || targetStack.isEmpty()) return;

        int slot = this.player.getInventory().findSlotMatchingItem(targetStack);
        if (slot != -1) {
            return;
        }

        Inventory inv = this.player.getInventory();
        for (int i = 0; i < inv.getContainerSize(); i++) {
            ItemStack stack = inv.getItem(i);

            if (stack.getItem() instanceof ReinforcedBundleItem) {
                // FIX: get -> getOrThrow
                var registry = this.player.level().registryAccess().lookupOrThrow(Registries.ENCHANTMENT);
                // FIX: getEntry -> getOptional (für RegistryKey)
                var masterBuilderEntry = registry.get(ModEnchantments.MASTER_BUILDER);

                if (masterBuilderEntry.isPresent() && EnchantmentHelper.getItemEnchantmentLevel(masterBuilderEntry.get(), stack) > 0) {

                    BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
                    if (contents != null) {
                        boolean foundInBundle = false;
                        // MC 1.21.11: BundleContents#items() liefert direkt ItemStacks;
                        // die Template-Indirektion (ItemStackTemplate#create) gibt es erst ab 26.2.
                        for (ItemStack contentStack : contents.items()) {
                            if (ItemStack.isSameItem(contentStack, targetStack)) {
                                foundInBundle = true;
                                break;
                            }
                        }

                        if (foundInBundle) {
                            ClientNetworking.send(new MasterBuilderPickPayload(targetStack));
                            ci.cancel(); 
                            return;
                        }
                    }
                }
            }
        }
    }
}