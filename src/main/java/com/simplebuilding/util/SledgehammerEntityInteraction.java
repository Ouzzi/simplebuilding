package com.simplebuilding.util;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import net.fabricmc.fabric.api.event.player.AttackEntityCallback;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.decoration.ItemFrameEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;

public class SledgehammerEntityInteraction {
    public static void register() {
        AttackEntityCallback.EVENT.register((player, world, hand, entity, hitResult) -> {
            if (world.isClient()) return ActionResult.PASS;
            if (hand != Hand.MAIN_HAND) return ActionResult.PASS;

            ItemStack mainStack = player.getMainHandStack();
            ItemStack offStack = player.getOffHandStack();

            // Check: Sledgehammer in Main, Glow Ink in Off
            if (mainStack.getItem() instanceof SledgehammerItem && offStack.isOf(Items.GLOW_INK_SAC)) {
                if (entity instanceof ItemFrameEntity itemFrame) {
                    ItemStack frameStack = itemFrame.getHeldItemStack();

                    // Check: Ist im Frame ein Item mit Armor Trim? (Templates sind Items, wir prüfen hier Templates)
                    // Da Armor Trims technisch Templates sind, prüfen wir auf das Item Tag oder Klasse.
                    // Vereinfachung: Wir prüfen, ob es ein Smithing Template ist.
                    if (isTrimTemplate(frameStack)) {
                        
                        // Transformation!
                        itemFrame.setHeldItemStack(new ItemStack(ModItems.GLOWING_TRIM_TEMPLATE), true);
                        
                        // Kosten abziehen
                        if (!player.isCreative()) {
                            offStack.decrement(1);
                            mainStack.damage(1, player, EquipmentSlot.MAINHAND);
                        }

                        // Effekte
                        world.playSound(null, itemFrame.getBlockPos(), SoundEvents.BLOCK_AMETHYST_BLOCK_HIT, SoundCategory.BLOCKS, 1.0f, 1.5f);
                        world.playSound(null, itemFrame.getBlockPos(), SoundEvents.ITEM_GLOW_INK_SAC_USE, SoundCategory.BLOCKS, 1.0f, 1.0f);
                        
                        return ActionResult.SUCCESS;
                    }
                }
            }
            return ActionResult.PASS;
        });
    }

    private static boolean isTrimTemplate(ItemStack stack) {
        // Prüft auf Vanilla Trim Templates oder deine Mod Templates
        String path = stack.getItem().toString();
        return path.contains("trim_smithing_template");
    }
}