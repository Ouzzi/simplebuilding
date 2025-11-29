package com.simplebuilding.event;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.util.ModTags;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.ContainerComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class MasterBuilderHandler {

    public static void register() {
        // ÄNDERUNG: UseBlockCallback statt UseItemCallback
        // Parameter: (player, world, hand, hitResult) -> Wir bekommen das HitResult geschenkt!
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {

            // Wir prüfen nur die Main Hand, um doppelte Ausführung zu vermeiden (optional, aber empfohlen)
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

            ItemStack stack = player.getStackInHand(hand);

            // 1. Prüfen: Ist es ein Item, das Master Builder haben kann?
            if (!stack.isIn(ModTags.Items.EXTRA_INVENTORY_ITEMS_ENCHANTABLE)) {
                return ActionResult.PASS; // Hier jetzt einfaches ActionResult
            }

            // 2. Prüfen: Hat es das Enchantment?
            var registryManager = world.getRegistryManager();
            var masterBuilderKey = ModEnchantments.MASTER_BUILDER;
            var enchantmentEntry = registryManager.getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(masterBuilderKey);

            if (enchantmentEntry.isEmpty() || EnchantmentHelper.getLevel(enchantmentEntry.get(), stack) <= 0) {
                return ActionResult.PASS;
            }

            // 3. Raycast ENTFERNT!
            // Wir nutzen einfach das 'hitResult' aus den Lambda-Parametern.
            // Das Event feuert nur, wenn wir einen Block treffen, also ist hitResult immer gültig.

            // 4. Versuchen, einen Block aus dem Inventar zu holen
            ItemStack foundStack = ItemStack.EMPTY;

            // Fall A: Bundle
            if (stack.getItem() == Items.BUNDLE) {
                BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
                if (contents != null && !contents.isEmpty()) {
                    foundStack = contents.get(0);
                    // TODO: Color Palette Logik
                }
            }
            // Fall B: Shulker Box
            else {
                ContainerComponent contents = stack.get(DataComponentTypes.CONTAINER);
                if (contents != null) {
                    for (ItemStack s : contents.iterateNonEmpty()) {
                        foundStack = s;
                        break;
                    }
                }
            }

            // 5. Block platzieren
            if (!foundStack.isEmpty() && foundStack.getItem() instanceof BlockItem blockItem) {

                final ItemStack finalStackToPlace = foundStack;

                // SCHRITT 1: Den "gefakten" UsageContext erstellen
                // Hier überschreiben wir getStack(), damit Minecraft denkt, wir halten den Block
                ItemUsageContext fakeContext = new ItemUsageContext(world, player, hand, stack, hitResult) {
                    @Override
                    public ItemStack getStack() {
                        return finalStackToPlace;
                    }
                };

                // SCHRITT 2: In einen ItemPlacementContext umwandeln
                // BlockItem.place braucht zwingend diesen Typen!
                ItemPlacementContext placementContext = new ItemPlacementContext(fakeContext);

                // SCHRITT 3: Platzieren
                ActionResult result = blockItem.place(placementContext);

                if (result.isAccepted()) {
                    if (!player.getAbilities().creativeMode) {
                        removeFromContainer(stack, finalStackToPlace);
                    }
                    player.swingHand(hand);
                    return ActionResult.SUCCESS;
                }
            }

            return ActionResult.PASS;
        });
    }

    private static void removeFromContainer(ItemStack container, ItemStack itemToRemove) {
        // TODO: Implementation für Bundle/Shulker update
    }
}