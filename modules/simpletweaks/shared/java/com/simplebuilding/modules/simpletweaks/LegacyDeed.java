package com.simplebuilding.modules.simpletweaks;
import com.simplebuilding.modules.simpletweaks.deed.ClaimDeedItem;
import com.simplebuilding.modules.simpletweaks.deed.DeedCloningRecipe;
import com.simplebuilding.modules.simpletweaks.deed.DeedData;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
/**
 * Registration entry of the claim deed (the name is historical: registry id {@code simpletweaks:claim_deed} and old
 * stacks stay loadable; legacy custom data is retained, only the server ledger grants authority).
 * The item itself is {@link ClaimDeedItem}; loaders call the three steps from the matching registry events
 * (components, items, recipe serializers) or one after the other (Fabric).
 */
public final class LegacyDeed {
    /** The {@code simpletweaks:claim_table} block (before the items). */
    public static void registerBlocks() { com.simplebuilding.modules.simpletweaks.table.ClaimTableBlock.registerBlock(); }
    public static void register() {
        com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.register();
        ClaimDeedItem.register();
        com.simplebuilding.modules.simpletweaks.table.ClaimTableBlock.registerItem();
    }
    /** The {@code simpletweaks:deed} data component (binding of a deed to its owner; display only). */
    public static void registerComponents() { DeedData.register(); }
    /** The {@code simpletweaks:claim_deed_copy} recipe serializer. */
    public static void registerRecipes() {
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath("simpletweaks", "claim_deed_copy"), DeedCloningRecipe.SERIALIZER);
    }
    private LegacyDeed() {}

    /** The module's own creative tab (items are also sorted into the Vanilla tabs, see the loader hooks). */
    public static void registerTab() {
        net.minecraft.core.Registry.register(net.minecraft.core.registries.BuiltInRegistries.CREATIVE_MODE_TAB,
                net.minecraft.resources.Identifier.fromNamespaceAndPath("simpletweaks", "claims"),
                net.minecraft.world.item.CreativeModeTab.builder(net.minecraft.world.item.CreativeModeTab.Row.TOP, 0)
                        .title(net.minecraft.network.chat.Component.translatable("itemgroup.simpletweaks.claims"))
                        .icon(() -> new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simpletweaks.deed.ClaimDeedItem.item()))
                        .displayItems((params, out) -> {
                            if (!com.simplebuilding.modules.simpletweaks.claims.Claims.on()) return;
                            out.accept(new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simpletweaks.guide.TweaksGuide.book()));
                            out.accept(new net.minecraft.world.item.ItemStack(com.simplebuilding.modules.simpletweaks.deed.ClaimDeedItem.item()));
                        }).build());
    }
}
