package com.simplequalityoflife.mixin;

import com.simplequalityoflife.Simplequalityoflife;
import com.simplequalityoflife.config.SimplequalityoflifeConfig;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.monster.piglin.PiglinAi;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.trim.ArmorTrim;
import net.minecraft.world.item.equipment.trim.TrimMaterials;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin(PiglinAi.class)
public class PiglinGoldMixin {

    @Inject(method = "isWearingSafeArmor", at = @At("HEAD"), cancellable = true)
    private static void onIsWearingPiglinSafeArmor(LivingEntity entity, CallbackInfoReturnable<Boolean> cir) {
        if (!(entity instanceof Player player)) return;

        SimplequalityoflifeConfig.QOL config = Simplequalityoflife.getConfig().qOL;

        if (config.piglinsIgnoreGoldTrims) {
            for (EquipmentSlot slot : EquipmentSlot.values()) {
                if (slot.getType() != EquipmentSlot.Type.HUMANOID_ARMOR) continue;

                ItemStack stack = player.getItemBySlot(slot);
                if (stack.isEmpty()) continue;

                ArmorTrim trim = stack.get(DataComponents.TRIM);
                if (trim != null && trim.material().is(TrimMaterials.GOLD)) {
                    cir.setReturnValue(true);
                    return;
                }
            }
        }
        if (config.piglinsIgnoreGoldTools) {
            if (isGoldTool(player.getMainHandItem()) || isGoldTool(player.getOffhandItem())) {
                cir.setReturnValue(true);
            }
        }
    }

    @Unique
    private static boolean isGoldTool(ItemStack stack) {
        if (stack.isEmpty()) return false;
        Item item = stack.getItem();
        return item == Items.GOLDEN_SWORD ||
                item == Items.GOLDEN_SPEAR ||
                item == Items.GOLDEN_PICKAXE ||
                item == Items.GOLDEN_AXE ||
                item == Items.GOLDEN_SHOVEL ||
                item == Items.GOLDEN_HOE;
    }
}
