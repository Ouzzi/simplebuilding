package com.simplebuilding.mixin;

import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import com.simplebuilding.util.BundleUtil; // Import!
import com.simplebuilding.util.TrimEffectUtil;
import net.minecraft.block.BlockState;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.entity.player.PlayerInventory;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.tag.ItemTags;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.ModifyVariable;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfo;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PlayerEntity.class)
public abstract class PlayerEntityMixin {

    @Shadow public abstract PlayerInventory getInventory();

    @Inject(method = "getBlockBreakingSpeed", at = @At("RETURN"), cancellable = true)
    private void modifyMiningSpeedForStripMiner(BlockState state, CallbackInfoReturnable<Float> cir) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        ItemStack stack = player.getMainHandStack();

        if (!stack.isIn(ItemTags.PICKAXES)) {
            return;
        }

        if (!stack.getItem().isCorrectForDrops(stack, state)) {
            return;
        }

        var registry = player.getEntityWorld().getRegistryManager();
        var enchantLookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var stripMinerKey = enchantLookup.getOptional(ModEnchantments.STRIP_MINER);

        if (stripMinerKey.isEmpty()) return;

        int level = EnchantmentHelper.getLevel(stripMinerKey.get(), stack);

        if (level > 0) {
            float originalSpeed = cir.getReturnValue();
            float divisor = 1.0f;


            switch (level) {
                case 1 -> divisor = 2.0f;
                case 2 -> divisor = 3.0f;
                case 3 -> divisor = 4.0f;
            }

            cir.setReturnValue(originalSpeed / divisor);
        }
    }

    // --- WAYFINDER (Hunger beim Sprinten) ---
    @Inject(method = "addExhaustion", at = @At("HEAD"), cancellable = true)
    private void simplebuilding$modifyExhaustion(float exhaustion, CallbackInfo ci) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.isSprinting()) {
            int wayfinderCount = TrimEffectUtil.getTrimCount(player, "wayfinder");
            if (wayfinderCount > 0) {
                // Reduziere Erschöpfung um 3% pro Teil
                float reduction = 1.0f - (wayfinderCount * 0.03f);
                // Da wir addExhaustion nicht direkt ändern können ohne Accessor/Shadow,
                // rufen wir hier super auf (schwierig in Mixin) oder wir brechen ab und rufen mit neuem Wert auf?
                // Besser: @ModifyVariable
            }
        }
    }

    @ModifyVariable(method = "addExhaustion", at = @At("HEAD"), argsOnly = true)
    private float simplebuilding$reduceExhaustion(float exhaustion) {
        PlayerEntity player = (PlayerEntity) (Object) this;
        if (player.isSprinting()) {
            int count = TrimEffectUtil.getTrimCount(player, "wayfinder");
            if (count > 0) {
                float reduction = (count * 0.03f); // 3% pro Teil
                float newExhaustion = exhaustion * (1.0f - reduction);

                // Debug Log (nur wenn Erschöpfung > 0, um Spam zu minimieren)
                if (exhaustion > 0) {
                     Simplebuilding.LOGGER.info("Trim Bonus Active: Wayfinder! Exhaustion reduced by {}% ({} -> {})", (int)(reduction * 100), exhaustion, newExhaustion);
                }

                return newExhaustion;
            }
        }
        return exhaustion;
    }
}