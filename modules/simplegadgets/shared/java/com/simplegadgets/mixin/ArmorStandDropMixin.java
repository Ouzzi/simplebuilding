package com.simplegadgets.mixin;

import com.simplegadgets.rotator.ArmorPoses;
import net.minecraft.world.entity.decoration.ArmorStand;
import net.minecraft.world.item.ItemStack;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyArg;

/** BA03-5: the item an armor stand drops when a player breaks it keeps the stand's pose (see {@link ArmorPoses#keepPose}). */
@Mixin(ArmorStand.class)
public abstract class ArmorStandDropMixin {
    @ModifyArg(method = "brokenByPlayer", index = 2,
            at = @At(value = "INVOKE", target = "Lnet/minecraft/world/level/block/Block;popResource(Lnet/minecraft/world/level/Level;Lnet/minecraft/core/BlockPos;Lnet/minecraft/world/item/ItemStack;)V"))
    private ItemStack simplegadgets$keepPose(ItemStack drop) {
        return ArmorPoses.keepPose((ArmorStand) (Object) this, drop);
    }
}
