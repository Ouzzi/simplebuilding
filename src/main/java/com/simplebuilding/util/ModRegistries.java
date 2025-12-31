package com.simplebuilding.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.networking.DoubleJumpPayload;
import com.simplebuilding.recipe.ReinforcedBundleRecipe;
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
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.*;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import net.minecraft.util.collection.DefaultedList;

import java.util.Optional;

public class ModRegistries {

    // --- Serializer Definition ---
    public static final RecipeSerializer<ReinforcedBundleRecipe> REINFORCED_BUNDLE_SERIALIZER = new RecipeSerializer<ReinforcedBundleRecipe>() {

        // CODEC: Liest/Schreibt das JSON
        private static final MapCodec<ReinforcedBundleRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(ShapedRecipe::getCategory),
                RawShapedRecipe.CODEC.forGetter(recipe -> {
                    // PROBLEM FIX: Wir können nicht einfach das 'raw' Feld nehmen, da es evtl. kein Data hat.
                    // Aber: Wenn wir über DataGen kommen, hat das RawShapedRecipe, das wir im Provider erstellt haben,
                    // tatsächlich Data (weil wir es mit create(Map, List) erstellt haben)!
                    // Wir müssen also sicherstellen, dass wir DASSELBE Objekt zurückgeben, oder eines mit Data.

                    // Da wir in ReinforcedBundleRecipe kein Getter für 'raw' haben (es ist protected/package-private in ShapedRecipe),
                    // müssen wir einen Trick anwenden oder Reflection nutzen.
                    // ODER: Wir nutzen die Tatsache, dass wir DataGen sind.

                    // Da ShapedRecipe kein "getRaw()" hat, müssen wir es in unserer Klasse exposen.
                    return ((ReinforcedBundleRecipe)recipe).getRaw();
                }),
                ItemStack.CODEC.fieldOf("result").forGetter(ReinforcedBundleRecipe::getResultStack)
        ).apply(instance, ReinforcedBundleRecipe::new));

        // PACKET CODEC: Für Netzwerk-Sync
        public static final PacketCodec<RegistryByteBuf, ReinforcedBundleRecipe> PACKET_CODEC = PacketCodec.ofStatic(
                (buf, recipe) -> {
                    buf.writeString(recipe.getGroup());
                    buf.writeEnumConstant(recipe.getCategory());
                    RawShapedRecipe.PACKET_CODEC.encode(buf, recipe.getRaw());
                    ItemStack.PACKET_CODEC.encode(buf, recipe.getResultStack());
                },
                buf -> new ReinforcedBundleRecipe(
                        buf.readString(),
                        buf.readEnumConstant(CraftingRecipeCategory.class),
                        RawShapedRecipe.PACKET_CODEC.decode(buf),
                        ItemStack.PACKET_CODEC.decode(buf)
                )
        );

        @Override
        public MapCodec<ReinforcedBundleRecipe> codec() {
            return CODEC;
        }

        @Override
        public PacketCodec<RegistryByteBuf, ReinforcedBundleRecipe> packetCodec() {
            return PACKET_CODEC;
        }
    };

    public static void registerModStuffs() {
        registerEvents();
        registerNetworking();
        Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(Simplebuilding.MOD_ID, "reinforced_bundle"), REINFORCED_BUNDLE_SERIALIZER);
    }

    private static void registerNetworking() {
        // Payload registrieren
        PayloadTypeRegistry.playC2S().register(DoubleJumpPayload.ID, DoubleJumpPayload.CODEC);

        // Receiver registrieren
        // TODO: Configuration check (disable double jump if not enabled)
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
                            bootStack.damage(1, player, EquipmentSlot.FEET);
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