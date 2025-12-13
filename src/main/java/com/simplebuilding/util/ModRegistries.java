package com.simplebuilding.util;

import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.networking.DoubleJumpPayload;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.fabricmc.fabric.api.networking.v1.PayloadTypeRegistry;
import net.fabricmc.fabric.api.networking.v1.ServerPlayNetworking;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot; // Wichtig für den Zugriff auf Schuhe
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;

public class ModRegistries {

    public static void registerModStuffs() {
        registerEvents();
        registerNetworking();
    }

    private static void registerNetworking() {
        // Payload registrieren
        PayloadTypeRegistry.playC2S().register(DoubleJumpPayload.ID, DoubleJumpPayload.CODEC);

        // Receiver registrieren
        ServerPlayNetworking.registerGlobalReceiver(DoubleJumpPayload.ID, (payload, context) -> {
            context.server().execute(() -> {
                ServerPlayerEntity player = context.player();
                var registry = player.getEntityWorld().getRegistryManager();
                var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
                var doubleJump = enchantments.getOptional(ModEnchantments.DOUBLE_JUMP);

                if (doubleJump.isPresent()) {
                    // Wir holen direkt das Item aus dem Schuh-Slot
                    ItemStack bootStack = player.getEquippedStack(EquipmentSlot.FEET);

                    // Prüfen, ob die Schuhe die Verzauberung haben
                    if (EnchantmentHelper.getLevel(doubleJump.get(), bootStack) > 0) {

                        // 1. Fallschaden zurücksetzen
                        player.fallDistance = 0;

                        // 2. Haltbarkeit abziehen (2 Punkte), wenn nicht Creative
                        if (!player.isCreative()) {
                            // damage(amount, entity, breakCallback)
                            bootStack.damage(4, player, EquipmentSlot.FEET);
                        }
                    }
                }
            });
        });

        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof SledgehammerItem) {
                BlockState state = world.getBlockState(hitResult.getBlockPos());
                if (state.isOf(Blocks.DIAMOND_BLOCK)) {
                    if (!world.isClient()) {
                        // Break block naturally? No, we want specific drops.
                        world.breakBlock(hitResult.getBlockPos(), false); // false = no standard drops

                        // Drop 81 Pebbles (9 Diamonds * 9 Pebbles/Diamond)
                        // This maintains value conservation (1 Block = 9 Diamonds = 81 Pebbles)
                        int totalPebbles = 81;
                        while (totalPebbles > 0) {
                            int batch = Math.min(totalPebbles, 64);
                            ItemEntity itemEntity = new ItemEntity(world,
                                    hitResult.getPos().x, hitResult.getPos().y, hitResult.getPos().z,
                                    new ItemStack(ModItems.DIAMOND_PEBBLE, batch));
                            world.spawnEntity(itemEntity);
                            totalPebbles -= batch;
                        }

                        world.playSound(null, hitResult.getBlockPos(), SoundEvents.BLOCK_METAL_BREAK, SoundCategory.BLOCKS, 1f, 1f);

                        if (!player.isCreative()) {
                            stack.damage(1, player, EquipmentSlot.MAINHAND);
                        }
                    }
                    return ActionResult.SUCCESS;
                }
            }
            return ActionResult.PASS;
        });
    }

    private static void registerEvents() {
        // Constructor's Touch (Debug Stick Logik auf normalem Stick)
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            var registry = world.getRegistryManager();
            var enchantments = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
            var constructorsTouch = enchantments.getOptional(ModEnchantments.CONSTRUCTORS_TOUCH);

            if (constructorsTouch.isPresent() &&
                    EnchantmentHelper.getLevel(constructorsTouch.get(), stack) > 0 &&
                    stack.isOf(Items.STICK)) {

                if (!world.isClient()) {
                    BlockState state = world.getBlockState(hitResult.getBlockPos());
                    var properties = state.getProperties();
                    if (!properties.isEmpty()) {
                        Property<?> property = properties.iterator().next();
                        BlockState newState = cycleState(state, property, player.isSneaking());
                        world.setBlockState(hitResult.getBlockPos(), newState, 18);
                        player.sendMessage(Text.of("§7" + property.getName() + ": §f" + newState.get(property).toString()), true);
                    }
                }
                return ActionResult.SUCCESS;
            }
            return ActionResult.PASS;
        });
    }

    private static <T extends Comparable<T>> BlockState cycleState(BlockState state, Property<T> property, boolean inverse) {
        return state.with(property, cycle(property.getValues(), state.get(property), inverse));
    }

    private static <T> T cycle(Iterable<T> elements, T current, boolean inverse) {
        if (inverse) return com.google.common.collect.Iterables.getLast(elements);
        java.util.Iterator<T> it = elements.iterator();
        while (it.hasNext()) {
            if (it.next().equals(current)) {
                if (it.hasNext()) return it.next();
            }
        }
        return elements.iterator().next();
    }
}