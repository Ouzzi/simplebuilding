package com.simplebuilding.tweaks.easter;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.List;
import java.util.Optional;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.SmithingRecipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.level.Level;

/**
 * Ein Schritt der Easter-Kette ({@link EasterEggs}), Serializer {@code simplebuilding:easter_smithing}.
 * Wie ein Schmiede-Umwandlungsrezept, aber die Basis muss genau die Easter-Stufe {@code base_stage}
 * tragen (0 = ein normales Pad ohne Komponente); das Ergebnis bekommt {@code result_stage} samt Namen,
 * oder ist, bei {@code result_stage} 0, ein frisches Item (der Funny Stick, ohne die Komponenten des Pads).
 *
 * <p>Versteckt: kein Rezeptbuch ({@link #isSpecial()}), keine Anzeige fuer JEI/REI/EMI
 * ({@link #display()} leer), keine Freischalt-Meldung. Damit die normalen Aufwertungen ein Easter-Pad
 * nicht als gewoehnliche Basis nehmen (dieselben Zutaten!), lehnt
 * {@code SmithingTransformEasterGuardMixin} jede Basis mit Easter-Stufe ab.
 */
public class EasterSmithingRecipe implements SmithingRecipe {
    public static final MapCodec<EasterSmithingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            Ingredient.CODEC.optionalFieldOf("template").forGetter(r -> r.template),
            Ingredient.CODEC.fieldOf("base").forGetter(r -> r.base),
            Codec.intRange(0, EasterEggs.MAX_STAGES).optionalFieldOf("base_stage", 0).forGetter(r -> r.baseStage),
            Ingredient.CODEC.optionalFieldOf("addition").forGetter(r -> r.addition),
            BuiltInRegistries.ITEM.byNameCodec().fieldOf("result").forGetter(r -> r.result),
            Codec.intRange(0, EasterEggs.MAX_STAGES).optionalFieldOf("result_stage", 0).forGetter(r -> r.resultStage)
    ).apply(i, EasterSmithingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, EasterSmithingRecipe> STREAM_CODEC = StreamCodec.composite(
            Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, r -> r.template,
            Ingredient.CONTENTS_STREAM_CODEC, r -> r.base,
            ByteBufCodecs.VAR_INT, r -> r.baseStage,
            Ingredient.OPTIONAL_CONTENTS_STREAM_CODEC, r -> r.addition,
            ByteBufCodecs.registry(Registries.ITEM), r -> r.result,
            ByteBufCodecs.VAR_INT, r -> r.resultStage,
            EasterSmithingRecipe::new);

    /** Registriert von jedem Loader unter {@code simplebuilding:easter_smithing}. */
    public static final RecipeSerializer<EasterSmithingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    private final Optional<Ingredient> template;
    private final Ingredient base;
    private final int baseStage;
    private final Optional<Ingredient> addition;
    private final Item result;
    private final int resultStage;

    public EasterSmithingRecipe(Optional<Ingredient> template, Ingredient base, int baseStage, Optional<Ingredient> addition,
                                Item result, int resultStage) {
        this.template = template;
        this.base = base;
        this.baseStage = baseStage;
        this.addition = addition;
        this.result = result;
        this.resultStage = resultStage;
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        return Ingredient.testOptionalIngredient(this.template, input.template())
                && this.base.test(input.base())
                && EasterEggs.stageOf(input.base()) == this.baseStage
                && Ingredient.testOptionalIngredient(this.addition, input.addition());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        return assembleFrom(input.base());
    }

    /** Das Ergebnis zu einer Basis; auch fuer die Spieltests. */
    public ItemStack assembleFrom(ItemStack base) {
        if (this.resultStage == 0) {
            return new ItemStack(this.result);
        }
        // Wie Vanilla-Umwandlungen: alles von der Basis mitnehmen (Besitzername, eigener Name) ...
        ItemStack out = base.transmuteCopy(this.result, 1);
        // ... und die Easter-Stufe samt Namen neu setzen.
        return EasterEggs.mark(out, this.resultStage);
    }

    public int baseStage() {
        return baseStage;
    }

    public int resultStage() {
        return resultStage;
    }

    public Item result() {
        return result;
    }

    @Override
    public boolean isSpecial() {
        return true;
    }

    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public List<RecipeDisplay> display() {
        return List.of();
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.NOT_PLACEABLE;
    }

    @Override
    public RecipeSerializer<EasterSmithingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public Optional<Ingredient> templateIngredient() {
        return this.template;
    }

    @Override
    public Ingredient baseIngredient() {
        return this.base;
    }

    @Override
    public Optional<Ingredient> additionIngredient() {
        return this.addition;
    }
}
