package com.simpleriding.mixin.client;

import com.simpleriding.RidingBookModels;
import net.minecraft.client.renderer.item.ItemModelResolver;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.resources.Identifier;
import net.minecraft.world.entity.ItemOwner;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.ModifyVariable;

/** Avoid replacing minecraft:enchanted_book, which SimpleBuilding also customizes. */
@Mixin(ItemModelResolver.class)
public abstract class RidingBookModelMixin {
    @ModifyVariable(method = "appendItemLayers", at = @At("STORE"), ordinal = 0)
    private Identifier simpleriding$bookModel(Identifier original, ItemStackRenderState state,
            ItemStack stack, ItemDisplayContext context, Level level, ItemOwner owner, int seed) {
        return RidingBookModels.select(stack, original);
    }
}
