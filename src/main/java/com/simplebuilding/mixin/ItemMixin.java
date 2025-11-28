package com.simplebuilding.mixin;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.enchantment.Enchantment;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.*;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.registry.RegistryWrapper;
import net.minecraft.registry.entry.RegistryEntry;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.world.World;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.Random;

//bundle als Baumeister-Werkzeug not working


@Mixin(Item.class)
public abstract class ItemMixin {

    @Inject(method = "useOnBlock", at = @At("HEAD"), cancellable = true)
    public void useBundleAsBuildingTool(ItemUsageContext context, CallbackInfoReturnable<ActionResult> cir) {
        ItemStack stack = context.getStack();

        // 1. Nur für Bundles
        if (!(stack.getItem() instanceof BundleItem)) {
            return;
        }

        PlayerEntity player = context.getPlayer();
        World world = context.getWorld();

        // --- DEBUG START ---
        if (!world.isClient()) {
            System.out.println("--- BUNDLE INTERACTION START ---");
        }
        // --- DEBUG END ---

        // 2. Enchantment Check
        boolean isMasterBuilder = false;
        boolean isColorPalette = false;

        // BYPASS: Im Creative Mode immer aktiv (zum Testen der Platzierung)
        if (player.getAbilities().creativeMode) {
            if (!world.isClient()) System.out.println("DEBUG: Creative Mode -> Force Active");
            isMasterBuilder = true;
        } else {
            // Normaler Check
            RegistryWrapper.WrapperLookup registryManager = world.getRegistryManager();
            if (ModEnchantments.MASTER_BUILDER != null) {
                var masterEntry = getEnchantment(registryManager, ModEnchantments.MASTER_BUILDER);
                if (masterEntry != null) {
                    isMasterBuilder = EnchantmentHelper.getLevel(masterEntry, stack) > 0;
                    if (!world.isClient()) System.out.println("DEBUG: Enchantment Level: " + EnchantmentHelper.getLevel(masterEntry, stack));
                } else {
                    if (!world.isClient()) System.out.println("DEBUG: Enchantment Registry Entry is NULL (JSON Missing?)");
                }

                // Color Palette Check
                var colorEntry = getEnchantment(registryManager, ModEnchantments.COLOR_PALETTE);
                if (colorEntry != null) {
                    isColorPalette = EnchantmentHelper.getLevel(colorEntry, stack) > 0;
                }
            }
        }

        if (isMasterBuilder) {
            // 3. Inhalt prüfen
            BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
            if (contents == null || contents.isEmpty()) {
                if (!world.isClient()) System.out.println("DEBUG: Bundle Empty -> Return");
                return;
            }

            // 4. Auswahl
            ItemStack selectedStack = null;
            int selectedIndex = -1;

            List<ItemStack> items = new ArrayList<>();
            for (ItemStack s : contents.iterate()) {
                items.add(s);
            }

            if (items.isEmpty()) return;

            if (isColorPalette) {
                selectedIndex = new Random().nextInt(items.size());
                selectedStack = items.get(selectedIndex);
            } else {
                selectedIndex = 0; // Oberstes Item
                selectedStack = items.get(0);
            }

            if (!world.isClient()) System.out.println("DEBUG: Selected Item: " + selectedStack.getItem().getName().getString());

            // 5. Platzieren
            if (selectedStack.getItem() instanceof BlockItem blockItem) {
                ItemStack placementStack = selectedStack.copy();

                // HitResult bauen
                BlockHitResult hitResult = new BlockHitResult(
                        context.getHitPos(),
                        context.getSide(),
                        context.getBlockPos(),
                        false
                );

                ItemUsageContext newContext = new ItemUsageContext(world, player, context.getHand(), placementStack, hitResult);

                ActionResult result = blockItem.useOnBlock(newContext);

                if (!world.isClient()) System.out.println("DEBUG: Place Result: " + result);

                if (result.isAccepted()) {
                    if (!world.isClient()) {
                        // Inventar Update
                        List<ItemStack> newItems = new ArrayList<>(items);
                        ItemStack toReduce = newItems.get(selectedIndex);

                        ItemStack updatedStack = toReduce.copy();
                        if (updatedStack.getCount() > 1) {
                            updatedStack.decrement(1);
                            newItems.set(selectedIndex, updatedStack);
                        } else {
                            newItems.remove(selectedIndex);
                        }

                        BundleContentsComponent newContents = new BundleContentsComponent(newItems);
                        stack.set(DataComponentTypes.BUNDLE_CONTENTS, newContents);

                        world.playSound(null, context.getBlockPos(), SoundEvents.ITEM_BUNDLE_REMOVE_ONE, SoundCategory.PLAYERS, 0.8f, 0.8f + world.random.nextFloat() * 0.4f);
                    }
                    // ERFOLG: Wir sagen Minecraft "Erledigt" -> Bundle kippt NICHT aus
                    cir.setReturnValue(ActionResult.SUCCESS);
                } else {
                    // FEHLSCHLAG (z.B. Block blockiert): Wir sagen "Fehlgeschlagen" -> Bundle kippt NICHT aus
                    cir.setReturnValue(ActionResult.FAIL);
                }
            } else {
                // Item ist kein Block: Wir sagen "Fehlgeschlagen" -> Bundle kippt NICHT aus
                if (!world.isClient()) System.out.println("DEBUG: Item is not a BlockItem");
                cir.setReturnValue(ActionResult.FAIL);
            }
        } else {
            if (!world.isClient()) System.out.println("DEBUG: Not Master Builder -> Allow Vanilla");
        }
    }

    private RegistryEntry<Enchantment> getEnchantment(RegistryWrapper.WrapperLookup registry, net.minecraft.registry.RegistryKey<Enchantment> key) {
        try {
            Optional<RegistryEntry.Reference<Enchantment>> optional = registry.getOrThrow(RegistryKeys.ENCHANTMENT).getOptional(key);
            return optional.orElse(null);
        } catch (Exception e) {
            // Das hier passiert, wenn die RegistryKeys noch nicht registriert sind oder DataGen fehlt
            System.err.println("SimpleBuilding Debug: Enchantment lookup failed for " + key.getValue());
            return null;
        }
    }
}