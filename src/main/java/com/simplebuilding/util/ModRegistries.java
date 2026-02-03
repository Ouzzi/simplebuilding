package com.simplebuilding.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.recipe.ReinforcedBundleRecipe;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.ItemEntity;
import net.minecraft.item.ItemStack;
import net.minecraft.item.Items;
import net.minecraft.network.RegistryByteBuf;
import net.minecraft.network.codec.PacketCodec;
import net.minecraft.recipe.RawShapedRecipe;
import net.minecraft.recipe.RecipeSerializer;
import net.minecraft.recipe.ShapedRecipe;
import net.minecraft.recipe.book.CraftingRecipeCategory;
import net.minecraft.registry.Registries;
import net.minecraft.registry.Registry;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvents;
import net.minecraft.state.property.Property;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Identifier;
import com.simplebuilding.enchantment.ModEnchantments; // Importe behalten für Events
import net.minecraft.enchantment.EnchantmentHelper; // Importe behalten für Events

public class ModRegistries {

    // --- Serializer Definition ---
    public static final RecipeSerializer<ReinforcedBundleRecipe> REINFORCED_BUNDLE_SERIALIZER = new RecipeSerializer<ReinforcedBundleRecipe>() {
        private static final MapCodec<ReinforcedBundleRecipe> CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
                Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::getGroup),
                CraftingRecipeCategory.CODEC.fieldOf("category").orElse(CraftingRecipeCategory.MISC).forGetter(ShapedRecipe::getCategory),
                RawShapedRecipe.CODEC.forGetter(recipe -> ((ReinforcedBundleRecipe)recipe).getRaw()),
                ItemStack.CODEC.fieldOf("result").forGetter(ReinforcedBundleRecipe::getResultStack)
        ).apply(instance, ReinforcedBundleRecipe::new));

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
        // registerNetworking(); <--- ENTFERNT! Das macht jetzt ModMessages.
        Registry.register(Registries.RECIPE_SERIALIZER, Identifier.of(Simplebuilding.MOD_ID, "reinforced_bundle"), REINFORCED_BUNDLE_SERIALIZER);
    }

    private static void registerEvents() {
        // Constructor's Touch
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

        // Sledgehammer Diamond Block
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getStackInHand(hand);
            if (stack.getItem() instanceof SledgehammerItem) {
                BlockState state = world.getBlockState(hitResult.getBlockPos());
                if (state.isOf(Blocks.DIAMOND_BLOCK)) {
                    if (!world.isClient()) {
                        world.breakBlock(hitResult.getBlockPos(), false);
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