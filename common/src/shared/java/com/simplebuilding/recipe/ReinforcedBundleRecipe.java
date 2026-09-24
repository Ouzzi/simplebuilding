package com.simplebuilding.recipe;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import com.simplebuilding.util.ModRegistries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.BundleItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.crafting.CraftingBookCategory;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.ShapedRecipe;
import net.minecraft.world.item.crafting.ShapedRecipePattern;
import net.minecraft.world.item.crafting.TransmuteRecipe;

/**
 * Ein geformtes Rezept, das einen Behaelter aufwertet und dabei alles mitnimmt, was an ihm haengt.
 *
 * <p>Benutzt von {@code recipe/reinforced_bundle.json} (Vanilla-Buendel -> verstaerktes Buendel)
 * und {@code recipe/reinforced_quiver.json} (Koecher -> verstaerkter Koecher). Ein gewoehnliches
 * {@code crafting_shaped} baut sein Ergebnis mit {@code result.create()} neu und wirft damit Inhalt,
 * Verzauberungen und Namen des eingelegten Behaelters weg - und das Rezeptbuch legt ohne Rueckfrage
 * auch einen gefuellten oder verzauberten Koecher ein, weil {@code Ingredient.of} keine Komponenten
 * vergleicht. Hier uebernimmt das Ergebnis deshalb den kompletten Komponenten-Patch des Behaelters,
 * genau wie der Schmiedetisch bei der Netherit-Aufwertung
 * ({@link TransmuteRecipe#createWithOriginalComponents}).
 *
 * <p>Der Behaelter ist die erste Zutat im Raster, die ein {@link BundleItem} ist - der
 * {@code QuiverItem} ist ueber {@code ReinforcedBundleItem} auch eines; Faden, Lederplatte,
 * Diamantkiesel und Kupfer-Nugget sind es nicht.
 */
public class ReinforcedBundleRecipe extends ShapedRecipe {

    // MC 26.2: Das Ergebnis ist ein ItemStackTemplate wie bei Vanillas ShapedRecipe. Ein ItemStack
    // liesse sich im Datagen nicht bauen (der ItemStack-Konstruktor liest dort noch ungebundene
    // Komponenten), und das Datagen muss dieses Rezept schreiben koennen.
    public static final MapCodec<ReinforcedBundleRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(instance -> instance.group(
            Codec.STRING.optionalFieldOf("group", "").forGetter(ShapedRecipe::group),
            CraftingBookCategory.CODEC.fieldOf("category").orElse(CraftingBookCategory.MISC).forGetter(ShapedRecipe::category),
            ShapedRecipePattern.MAP_CODEC.forGetter(ReinforcedBundleRecipe::getRaw),
            ItemStackTemplate.CODEC.fieldOf("result").forGetter(ReinforcedBundleRecipe::resultTemplate)
    ).apply(instance, ReinforcedBundleRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, ReinforcedBundleRecipe> STREAM_CODEC = StreamCodec.of(
            (buf, recipe) -> {
                buf.writeUtf(recipe.group());
                buf.writeEnum(recipe.category());
                ShapedRecipePattern.STREAM_CODEC.encode(buf, recipe.getRaw());
                ItemStackTemplate.STREAM_CODEC.encode(buf, recipe.resultTemplate());
            },
            buf -> new ReinforcedBundleRecipe(
                    buf.readUtf(),
                    buf.readEnum(CraftingBookCategory.class),
                    ShapedRecipePattern.STREAM_CODEC.decode(buf),
                    ItemStackTemplate.STREAM_CODEC.decode(buf)
            )
    );

    private final ItemStackTemplate result;
    private final ShapedRecipePattern rawPattern;

    public ReinforcedBundleRecipe(String group, CraftingBookCategory category, ShapedRecipePattern raw, ItemStackTemplate result) {
        super(
                new net.minecraft.world.item.crafting.Recipe.CommonInfo(true),
                new CraftingRecipe.CraftingBookInfo(category, group),
                raw,
                result
        );
        this.result = result;
        this.rawPattern = raw;
    }

    public ItemStackTemplate resultTemplate() {
        return this.result;
    }

    public ShapedRecipePattern getRaw() {
        return this.rawPattern;
    }

    @Override
    public ItemStack assemble(CraftingInput input) {
        for (int i = 0; i < input.size(); i++) {
            ItemStack stack = input.getItem(i);

            if (stack.getItem() instanceof BundleItem) {
                return TransmuteRecipe.createWithOriginalComponents(this.result, stack);
            }
        }
        // Ohne Behaelter im Raster kann das Muster nicht gepasst haben; nur zur Sicherheit.
        return super.assemble(input);
    }

    @Override
    @SuppressWarnings("unchecked")
    public RecipeSerializer<ShapedRecipe> getSerializer() {
        return (RecipeSerializer<ShapedRecipe>) (RecipeSerializer<?>) ModRegistries.REINFORCED_BUNDLE_SERIALIZER;
    }
}
