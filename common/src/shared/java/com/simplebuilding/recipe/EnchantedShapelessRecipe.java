package com.simplebuilding.recipe;

import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponentPatch;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.resources.ResourceKey;
import net.minecraft.util.Util;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapelessRecipe;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.ShapelessCraftingRecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.enchantment.Enchantment;
import net.minecraft.world.item.enchantment.ItemEnchantments;
import net.minecraft.world.level.Level;

/**
 * Formloses Werkbank-Rezept, bei dem eine Zutat eine bestimmte Verzauberung tragen muss
 * ({@code simplebuilding:enchanted_shapeless}). Benutzt vom Flypad I (Besitzer 2026-09-29): Enderit-Kern +
 * Enderit-Druckplatte + Shulkerkopf + eine Elytra <b>mit Reparatur</b> - eine Elytra ohne Reparatur passt nicht.
 * {@code Ingredient} vergleicht keine Komponenten, und die Komponenten-Zutaten der Loader (Fabric
 * {@code fabric:components}, NeoForge {@code neoforge:components}) schreiben verschiedenes JSON; dieses Rezept
 * ist auf allen Loadern dasselbe.
 *
 * <p>JSON wie {@code crafting_shapeless}, dazu {@code enchanted_item} (das Item, das verzaubert sein muss) und
 * {@code enchantment}. Gezaehlt werden aufgebrachte und gespeicherte Verzauberungen ({@code enchantments},
 * {@code stored_enchantments}). Rezeptbuch, Handbuch und JEI zeigen die Zutat mit Verzauberungsglanz und dem
 * Namen der Verzauberung als Zeile ({@link #requiredDisplay}).
 */
public class EnchantedShapelessRecipe extends ShapelessRecipe {

    public static final MapCodec<EnchantedShapelessRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Recipe.CommonInfo.MAP_CODEC.forGetter(r -> r.commonInfo),
            CraftingRecipe.CraftingBookInfo.MAP_CODEC.forGetter(r -> r.bookInfo),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(r -> r.result),
            Ingredient.CODEC.listOf(1, 9).fieldOf("ingredients").forGetter(r -> r.ingredients),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("enchanted_item").forGetter(r -> r.enchantedItem),
            ResourceKey.codec(Registries.ENCHANTMENT).fieldOf("enchantment").forGetter(r -> r.enchantment)
    ).apply(i, EnchantedShapelessRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EnchantedShapelessRecipe> STREAM_CODEC = StreamCodec.composite(
            Recipe.CommonInfo.STREAM_CODEC, r -> r.commonInfo,
            CraftingRecipe.CraftingBookInfo.STREAM_CODEC, r -> r.bookInfo,
            ItemStackTemplate.STREAM_CODEC, r -> r.result,
            Ingredient.CONTENTS_STREAM_CODEC.apply(ByteBufCodecs.list()), r -> r.ingredients,
            ByteBufCodecs.registry(Registries.ITEM), r -> r.enchantedItem,
            ResourceKey.streamCodec(Registries.ENCHANTMENT), r -> r.enchantment,
            EnchantedShapelessRecipe::new);

    /** Registriert von jedem Loader unter {@code simplebuilding:enchanted_shapeless} ({@code TweaksContent}). */
    public static final RecipeSerializer<EnchantedShapelessRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final ItemStackTemplate result;
    private final List<Ingredient> ingredients;
    private final Item enchantedItem;
    private final ResourceKey<Enchantment> enchantment;

    public EnchantedShapelessRecipe(Recipe.CommonInfo commonInfo, CraftingRecipe.CraftingBookInfo bookInfo, ItemStackTemplate result,
                                    List<Ingredient> ingredients, Item enchantedItem, ResourceKey<Enchantment> enchantment) {
        super(commonInfo, bookInfo, result, ingredients);
        this.result = result;
        this.ingredients = ingredients;
        this.enchantedItem = enchantedItem;
        this.enchantment = enchantment;
    }

    public List<Ingredient> ingredients() {
        return ingredients;
    }

    public Item enchantedItem() {
        return enchantedItem;
    }

    public ResourceKey<Enchantment> enchantment() {
        return enchantment;
    }

    @Override
    public boolean matches(CraftingInput input, Level level) {
        if (!super.matches(input, level)) {
            return false;
        }
        for (int slot = 0; slot < input.size(); slot++) {
            ItemStack stack = input.getItem(slot);
            if (stack.is(enchantedItem) && !hasEnchantment(stack, enchantment)) {
                return false;
            }
        }
        return true;
    }

    /** Ob der Stapel die Verzauberung aufgebracht oder (Buch) gespeichert traegt. */
    public static boolean hasEnchantment(ItemStack stack, ResourceKey<Enchantment> enchantment) {
        return contains(stack.get(DataComponents.ENCHANTMENTS), enchantment)
                || contains(stack.get(DataComponents.STORED_ENCHANTMENTS), enchantment);
    }

    private static boolean contains(ItemEnchantments enchantments, ResourceKey<Enchantment> enchantment) {
        if (enchantments == null) {
            return false;
        }
        for (Holder<Enchantment> holder : enchantments.keySet()) {
            if (holder.is(enchantment)) {
                return true;
            }
        }
        return false;
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapelessRecipe> getSerializer() {
        return (RecipeSerializer<ShapelessRecipe>) (RecipeSerializer<?>) SERIALIZER;
    }

    /**
     * Die verlangte Zutat fuer Rezeptbuch, Handbuch und JEI: das Item mit Verzauberungsglanz und dem Namen der
     * Verzauberung als Zeile. Die echte Verzauberung braucht die Registry, die {@code display()} nicht hat.
     */
    public SlotDisplay requiredDisplay() {
        DataComponentPatch patch = DataComponentPatch.builder()
                .set(DataComponents.ENCHANTMENT_GLINT_OVERRIDE, true)
                .set(DataComponents.LORE, new ItemLore(List.of(
                        Component.translatable(Util.makeDescriptionId("enchantment", enchantment.identifier())))))
                .build();
        return new SlotDisplay.ItemStackSlotDisplay(new ItemStackTemplate(enchantedItem, patch));
    }

    /** Die Zutaten als Anzeige, die verzauberte Zutat durch {@link #requiredDisplay} ersetzt. */
    public List<SlotDisplay> ingredientDisplays() {
        List<SlotDisplay> slots = new ArrayList<>();
        for (Ingredient ingredient : ingredients) {
            slots.add(ingredient.test(new ItemStack(enchantedItem)) ? requiredDisplay() : ingredient.display());
        }
        return slots;
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of(new ShapelessCraftingRecipeDisplay(ingredientDisplays(),
                new SlotDisplay.ItemStackSlotDisplay(this.result), new SlotDisplay.ItemSlotDisplay(Items.CRAFTING_TABLE)));
    }
}
