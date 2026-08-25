package com.simplebuilding.util;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.Simplebuilding;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.SledgehammerItem;
import com.simplebuilding.recipe.ReinforcedBundleRecipe;
import net.fabricmc.fabric.api.event.player.UseBlockCallback;
import net.minecraft.ChatFormatting;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.Property;
import com.simplebuilding.enchantment.ModEnchantments; // Importe behalten für Events

import java.util.ArrayList;
import java.util.List;

import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

public class ModRegistries {

    // --- Serializer Definition ---
    public static final RecipeSerializer<ReinforcedBundleRecipe> REINFORCED_BUNDLE_SERIALIZER = new RecipeSerializer<>(
            RecordCodecBuilder.mapCodec(instance -> instance.group(
                    Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::group),
                    CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
                    ShapedRecipePattern.MAP_CODEC.forGetter(recipe -> ((ReinforcedBundleRecipe) recipe).getRaw()),
                    ItemStack.CODEC.fieldOf("result").forGetter(ReinforcedBundleRecipe::getResultStack)
            ).apply(instance, ReinforcedBundleRecipe::new)),
            StreamCodec.of(
                    (buf, recipe) -> {
                        buf.writeUtf(recipe.group());
                        buf.writeEnum(recipe.category());
                        ShapedRecipePattern.STREAM_CODEC.encode(buf, recipe.getRaw());
                        ItemStack.STREAM_CODEC.encode(buf, recipe.getResultStack());
                    },
                    buf -> new ReinforcedBundleRecipe(
                            buf.readUtf(),
                            buf.readEnum(CraftingBookCategory.class),
                            ShapedRecipePattern.STREAM_CODEC.decode(buf),
                            ItemStack.STREAM_CODEC.decode(buf)
                    )
            )
    );

    public static void registerModStuffs() {
        registerEvents();
        // registerNetworking(); <--- ENTFERNT! Das macht jetzt ModMessages.
        Registry.register(BuiltInRegistries.RECIPE_SERIALIZER, Identifier.fromNamespaceAndPath(Simplebuilding.MOD_ID, "reinforced_bundle"), REINFORCED_BUNDLE_SERIALIZER);
    }

    private static void registerEvents() {
        // Constructor's Touch
        UseBlockCallback.EVENT.register((player, world, hand, hitResult) -> {
            ItemStack stack = player.getItemInHand(hand);
            if (hasEnchantment(stack, world, ModEnchantments.CONSTRUCTORS_TOUCH) && stack.is(Items.STICK)) {

                if (!world.isClientSide()) {
                    BlockState state = world.getBlockState(hitResult.getBlockPos());
                    var properties = state.getProperties();
                    if (!properties.isEmpty()) {
                        Property<?> property = properties.iterator().next();
                        BlockState newState = cycleState(state, property, player.isShiftKeyDown());
                        world.setBlock(hitResult.getBlockPos(), newState, 18);
                        Component message = Component.literal(property.getName() + ": ").withStyle(ChatFormatting.GRAY)
                                .append(Component.literal(String.valueOf(newState.getValue(property))).withStyle(ChatFormatting.WHITE));
                        if (player instanceof ServerPlayer serverPlayer) {
                            serverPlayer.sendOverlayMessage(message);
                        }
                    }
                }
                return InteractionResult.SUCCESS;
            }
            return InteractionResult.PASS;
        });
    }

    private static <T extends Comparable<T>> BlockState cycleState(BlockState state, Property<T> property, boolean inverse) {
        return state.setValue(property, cycle(property.getPossibleValues(), state.getValue(property), inverse));
    }

    private static <T> T cycle(Iterable<T> elements, T current, boolean inverse) {
        List<T> values = new ArrayList<>();
        for (T value : elements) {
            values.add(value);
        }

        if (values.isEmpty()) {
            return current;
        }

        int index = values.indexOf(current);
        if (index < 0) {
            return inverse ? values.get(values.size() - 1) : values.get(0);
        }

        int nextIndex = inverse
                ? (index - 1 + values.size()) % values.size()
                : (index + 1) % values.size();
        return values.get(nextIndex);
    }
}