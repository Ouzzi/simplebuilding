package com.simplefun.client;
import mezz.jei.api.*;import mezz.jei.api.registration.IRecipeRegistration;import com.simplefun.heads.*;import net.minecraft.resources.Identifier;import net.minecraft.world.item.ItemStack;import net.minecraft.network.chat.Component;
/** Loaded only by JEI, never by the module bootstrap. */
@JeiPlugin public final class FunJeiPlugin implements IModPlugin {
 public Identifier getPluginUid(){return AnimalHeads.id("jei");}
 public void registerRecipes(IRecipeRegistration r){for(var t:AnimalHead.values())r.addItemStackInfo(java.util.List.of(new ItemStack(AnimalHeads.ITEMS.get(t))),Component.translatable("jei.simplefun.head_source"));r.addItemStackInfo(java.util.List.of(new ItemStack(com.simplefun.registry.ModItems.BRICK_SNOWBALL)),Component.translatable("jei.simplefun.brick_snowball"));}
}
