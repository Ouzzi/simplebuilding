package com.simplebuilding.modules.simpletweaks;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.network.chat.Component;
import java.util.function.Consumer;
/** Inert legacy artifact. Custom data is retained by Vanilla; never grants land or authority. */
public final class LegacyDeed {
    public static void register() {
        var id = Identifier.fromNamespaceAndPath("simpletweaks", "claim_deed");
        Registry.register(BuiltInRegistries.ITEM, id, new Item(new Item.Properties().stacksTo(16).setId(ResourceKey.create(Registries.ITEM, id))) {
            @Override public void appendHoverText(ItemStack stack, TooltipContext context, TooltipDisplay display, Consumer<Component> out, TooltipFlag flag) {
                out.accept(Component.translatable("tooltip.simpletweaks.claim_deed.inactive"));
                out.accept(Component.translatable("tooltip.simpletweaks.claim_deed.data"));
            }
        });
    }
    private LegacyDeed() {}
}
