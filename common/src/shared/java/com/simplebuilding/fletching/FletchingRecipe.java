package com.simplebuilding.fletching;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.mojang.serialization.codecs.RecordCodecBuilder;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.ChatFormatting;
import net.minecraft.core.component.DataComponents;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.chat.Component;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.util.StringRepresentable;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.ItemLore;
import net.minecraft.world.item.crafting.Ingredient;
import net.minecraft.world.item.crafting.PlacementInfo;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.RecipeBookCategory;
import net.minecraft.world.item.crafting.RecipeSerializer;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.item.crafting.display.RecipeDisplay;
import net.minecraft.world.item.crafting.display.SlotDisplay;
import net.minecraft.world.item.crafting.display.SmithingRecipeDisplay;
import net.minecraft.world.level.Level;

/**
 * Ein Eintrag im Rezeptbuch des Befiederungstischs ({@code simplebuilding:fletching}): genau ein Teil (N16, Besitzer:
 * drei Kategorien Spitze, Stab, Feder; je Kategorie nur die Wahlmoeglichkeiten). Ein Klick legt das Teil aus dem eigenen
 * Inventar in seinen Slot, die anderen beiden Slots bleiben; das Ergebnis rechnet das {@link FletchingMenu} weiterhin
 * selbst aus den Slots aus. Die Anzeige nutzt Vanillas {@link SmithingRecipeDisplay}: das Teil an seiner Stelle (Spitze,
 * Schaft, Befiederung), die anderen leer; als Ergebnis zeigt das Buch das Teil mit seiner Wirkung als Kurz-Tooltip.
 *
 * <p>Eingabe ist {@link SmithingRecipeInput} in der Reihenfolge Spitze, Schaft, Befiederung.
 */
public record FletchingRecipe(Kind kind, String part) implements Recipe<SmithingRecipeInput> {

    /** Die drei Rezeptbuch-Kategorien in Slot-Reihenfolge. */
    public enum Kind implements StringRepresentable {
        TIP("tip", FletchingMenu.TIP_SLOT),
        SHAFT("shaft", FletchingMenu.SHAFT_SLOT),
        FLETCHING("fletching", FletchingMenu.FLETCHING_SLOT);

        private final String name;
        private final int slot;

        Kind(String name, int slot) {
            this.name = name;
            this.slot = slot;
        }

        @Override
        public String getSerializedName() {
            return name;
        }

        /** Menue-Slot, in den Teile dieser Kategorie gehoeren. */
        public int slot() {
            return slot;
        }

        /** Alle Teile der Kategorie in Enum-Reihenfolge (Namen wie in {@link ArrowParts}). */
        public List<String> parts() {
            StringRepresentable[] values = switch (this) {
                case TIP -> ArrowParts.Tip.values();
                case SHAFT -> ArrowParts.Shaft.values();
                case FLETCHING -> ArrowParts.Fletching.values();
            };
            List<String> out = new ArrayList<>();
            for (StringRepresentable value : values) {
                out.add(value.getSerializedName());
            }
            return out;
        }

        /** Das Item eines Teils, {@link Items#AIR} wenn es den Namen nicht gibt. */
        public Item input(String part) {
            switch (this) {
                case TIP -> {
                    for (ArrowParts.Tip tip : ArrowParts.Tip.values()) if (tip.getSerializedName().equals(part)) return tip.input();
                }
                case SHAFT -> {
                    for (ArrowParts.Shaft shaft : ArrowParts.Shaft.values()) if (shaft.getSerializedName().equals(part)) return shaft.input();
                }
                case FLETCHING -> {
                    for (ArrowParts.Fletching fletching : ArrowParts.Fletching.values()) if (fletching.getSerializedName().equals(part)) return fletching.input();
                }
            }
            return Items.AIR;
        }
    }

    public static final MapCodec<FletchingRecipe> MAP_CODEC = RecordCodecBuilder.mapCodec(i -> i.group(
            StringRepresentable.fromEnum(Kind::values).fieldOf("kind").forGetter(FletchingRecipe::kind),
            Codec.STRING.fieldOf("part").forGetter(FletchingRecipe::part)
    ).apply(i, FletchingRecipe::new));

    public static final StreamCodec<RegistryFriendlyByteBuf, FletchingRecipe> STREAM_CODEC = StreamCodec.composite(
            ByteBufCodecs.idMapper(i -> Kind.values()[i], Kind::ordinal), FletchingRecipe::kind,
            ByteBufCodecs.STRING_UTF8, FletchingRecipe::part,
            FletchingRecipe::new);

    /** Registriert von jedem Loader unter {@code simplebuilding:fletching}. */
    public static final RecipeSerializer<FletchingRecipe> SERIALIZER = new RecipeSerializer<>(MAP_CODEC, STREAM_CODEC);

    /** Alle Teile aller Kategorien: ein Rezept je Teil. */
    public static List<FletchingRecipe> all() {
        List<FletchingRecipe> out = new ArrayList<>();
        for (Kind kind : Kind.values()) {
            for (String part : kind.parts()) {
                out.add(new FletchingRecipe(kind, part));
            }
        }
        return out;
    }

    public Item input() {
        return kind.input(part);
    }

    /** Rezept-ID-Pfad, z. B. {@code fletching/tip/iron}. */
    public String idPath() {
        return "fletching/" + kind.getSerializedName() + "/" + part;
    }

    /** Sehr kurzer Tooltip mit der Wirkung des Teils (dieselben Texte wie am Pfeil). */
    public Component effect() {
        return Component.translatable("container.simplebuilding.fletching.effect." + kind.getSerializedName() + "." + part)
                .withStyle(style -> style.withColor(ChatFormatting.BLUE).withItalic(false));
    }

    @Override
    public boolean matches(SmithingRecipeInput input, Level level) {
        ItemStack stack = switch (kind) {
            case TIP -> input.template();
            case SHAFT -> input.base();
            case FLETCHING -> input.addition();
        };
        return stack.is(input());
    }

    @Override
    public ItemStack assemble(SmithingRecipeInput input) {
        return result();
    }

    /** Was das Buch zeigt: das Teil selbst mit seiner Wirkung als Lore. */
    public ItemStack result() {
        ItemStack stack = new ItemStack(input());
        stack.set(DataComponents.LORE, new ItemLore(List.of(effect())));
        return stack;
    }

    /** Keine Toasts: alle Befiederungsrezepte werden beim Oeffnen des Tisches auf einmal freigeschaltet. */
    @Override
    public boolean showNotification() {
        return false;
    }

    @Override
    public String group() {
        return "";
    }

    @Override
    public RecipeSerializer<FletchingRecipe> getSerializer() {
        return SERIALIZER;
    }

    @Override
    public RecipeType<FletchingRecipe> getType() {
        return FletchingRecipes.TYPE;
    }

    @Override
    public PlacementInfo placementInfo() {
        return PlacementInfo.create(List.of(Ingredient.of(input())));
    }

    @Override
    public List<RecipeDisplay> display() {
        SlotDisplay item = new SlotDisplay.ItemSlotDisplay(input());
        SlotDisplay empty = SlotDisplay.Empty.INSTANCE;
        return List.of(new SmithingRecipeDisplay(
                kind == Kind.TIP ? item : empty,
                kind == Kind.SHAFT ? item : empty,
                kind == Kind.FLETCHING ? item : empty,
                new SlotDisplay.ItemStackSlotDisplay(ItemStackTemplate.fromNonEmptyStack(result())),
                new SlotDisplay.ItemSlotDisplay(Items.FLETCHING_TABLE)));
    }

    @Override
    public RecipeBookCategory recipeBookCategory() {
        return FletchingRecipes.category(kind);
    }
}
