package com.simplemobheads.compat;

import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.data.HeadDef;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.IModPlugin;
import mezz.jei.api.JeiPlugin;
import mezz.jei.api.registration.IRecipeRegistration;
import mezz.jei.api.registration.ISubtypeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/** Loaded only by JEI: every variant as its own entry and an info page per head naming its mob and the charged creeper. */
@JeiPlugin
public final class MobHeadsJeiPlugin implements IModPlugin {
    @Override
    public Identifier getPluginUid() {
        return SimpleMobHeads.id("jei");
    }

    @Override
    public void registerItemSubtypes(ISubtypeRegistration registration) {
        for (Item item : MobHeads.items()) {
            registration.registerFromDataComponentTypes(item, MobHeads.VARIANT);
        }
    }

    @Override
    public void registerRecipes(IRecipeRegistration registration) {
        for (HeadDef head : MobHeads.heads()) {
            List<ItemStack> stacks = new ArrayList<>();
            stacks.add(new ItemStack(MobHeads.item(head.id())));
            for (String variant : head.variantIds()) {
                if (!variant.equals(head.firstVariant())) {
                    stacks.add(MobHeads.stack(head, variant));
                }
            }
            Component mob = BuiltInRegistries.ENTITY_TYPE.getOptional(Identifier.parse(head.mob()))
                    .<Component>map(type -> type.getDescription()).orElse(Component.translatable("block.simplemobheads." + head.item()));
            registration.addItemStackInfo(stacks, Component.translatable(
                    head.drop().equals("ritual") ? "jei.simplemobheads.ritual_head_source"
                    : head.drop().equals("wither") ? "jei.simplemobheads.boss_head_source" : "jei.simplemobheads.head_source", mob));
        }
    }
}
