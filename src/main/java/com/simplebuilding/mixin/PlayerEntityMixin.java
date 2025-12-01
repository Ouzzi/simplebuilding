package com.simplebuilding.mixin;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.util.BundleUtil; // Import!
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Shadow public abstract PlayerInventory getInventory();

    @Inject(method = "getProjectileType", at = @At("HEAD"), cancellable = true)
    private void getProjectileTypeFromBundle(ItemStack stack, CallbackInfoReturnable<ItemStack> cir) {
        if (!(stack.getItem() instanceof net.minecraft.item.BowItem)) return;

        PlayerEntity player = (PlayerEntity) (Object) this;

        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack slotStack = player.getInventory().getStack(i);

            if (slotStack.getItem() instanceof ReinforcedBundleItem) {
                if (hasQuiverEnchantment(slotStack, player)) {
                    // Nutzung der Utility Klasse
                    ItemStack arrowStack = BundleUtil.findArrow(slotStack);
                    if (!arrowStack.isEmpty()) {
                        cir.setReturnValue(arrowStack.copy());
                        return;
                    }
                }
            }
        }
    }
    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void modifyMiningSpeedForStripMiner(BlockState state, CallbackInfoReturnable<Float> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        ItemStack stack = player.getMainHandStack();

        // 1. Performance-Check: Ist es überhaupt eine Spitzhacke?
        if (!stack.isIn(ItemTags.PICKAXES)) {
            return;
        }

        // 2. Logik-Check: Ist das Werkzeug für den Block geeignet?
        // Wenn ich mit Strip-Miner-Picke auf Erde schlage, soll es nicht langsamer sein,
        // da Strip Miner dort eh nicht auslöst.
        if (!stack.getItem().isCorrectForDrops(stack, state)) {
            return;
        }

        // 3. Enchantment prüfen
        var registry = player.getEntityWorld().getRegistryManager();
        var enchantLookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var stripMinerKey = enchantLookup.getOptional(ModEnchantments.STRIP_MINER);

        if (stripMinerKey.isEmpty()) return;

        int level = EnchantmentHelper.getLevel(stripMinerKey.get(), stack);

        if (level > 0) {
            float originalSpeed = cir.getReturnValue();
            float divisor = 1.0f;

            // Berechnung basierend auf der Anzahl der abgebauten Blöcke:
            // Level 1: +1 Block  (Total 2) -> Speed / 2
            // Level 2: +2 Blöcke (Total 3) -> Speed / 3
            // Level 3: +4 Blöcke (Total 5) -> Speed / 5

            switch (level) {
                case 1 -> divisor = 2.0f;
                case 2 -> divisor = 3.0f;
                case 3 -> divisor = 4.0f;
                default -> divisor = 1.0f; // Fallback
            }

            // Geschwindigkeit anpassen
            cir.setReturnValue(originalSpeed / divisor);
        }
    }

    private boolean hasQuiverEnchantment(ItemStack stack, PlayerEntity player) {
        var registry = player.getEntityWorld().getRegistryManager();
        var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var quiver = enchantments.getOptional(ModEnchantments.QUIVER);
        return quiver.isPresent() && EnchantmentHelper.getLevel(quiver.get(), stack) > 0;
    }
}