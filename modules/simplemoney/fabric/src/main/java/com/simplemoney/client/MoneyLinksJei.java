package com.simplemoney.client;

import com.simplemoney.MoneyLinks;
import mezz.jei.api.*;
import mezz.jei.api.registration.IRecipeRegistration;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** Optional JEI entrypoint; absent items are skipped through public registries. */
@JeiPlugin
public final class MoneyLinksJei implements IModPlugin {
    public Identifier getPluginUid() { return Identifier.parse("simplemoney:links"); }
    public void registerRecipes(IRecipeRegistration r) {
        var explanation = Component.translatable("jei.simplemoney.links");
        for (var row : MoneyLinks.PRICES.values()) {
            var id = Identifier.parse(row.item());
            if (BuiltInRegistries.ITEM.containsKey(id))
                r.addItemStackInfo(java.util.List.of(new ItemStack(BuiltInRegistries.ITEM.getValue(id))), explanation);
        }
        r.addItemStackInfo(java.util.List.of(new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.parse("simplemoney:money_bill")))), explanation);
    }
}
