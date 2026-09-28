package com.simplebuilding.compat.rei;

import com.simplebuilding.compat.InWorldRecipeCatalog;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import me.shedaniel.rei.api.common.category.CategoryIdentifier;
import me.shedaniel.rei.api.common.display.Display;
import me.shedaniel.rei.api.common.display.DisplaySerializer;
import me.shedaniel.rei.api.common.display.basic.BasicDisplay;
import me.shedaniel.rei.api.common.entry.EntryIngredient;
import me.shedaniel.rei.api.common.util.EntryIngredients;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;

/**
 * One in-world transformation from {@link InWorldRecipeCatalog} as an REI display. Inputs are the
 * catalog's input stacks followed by the tool slot (so "uses" of a tool find it), the output is the
 * result. Client-side only: registered by {@link SimplebuildingReiClientPlugin}, never sent over
 * the network, hence no serializer.
 */
final class InWorldDisplay extends BasicDisplay {

    private static final Map<InWorldRecipeCatalog.Kind, CategoryIdentifier<InWorldDisplay>> CATEGORIES =
            new EnumMap<>(InWorldRecipeCatalog.Kind.class);

    private final InWorldRecipeCatalog.Entry entry;

    InWorldDisplay(InWorldRecipeCatalog.Entry entry) {
        super(inputs(entry), List.of(ingredient(entry.output())),
                Optional.of(Identifier.fromNamespaceAndPath("simplebuilding", "in_world/" + entry.id().replace(':', '/'))));
        this.entry = entry;
    }

    static synchronized CategoryIdentifier<InWorldDisplay> category(InWorldRecipeCatalog.Kind kind) {
        return CATEGORIES.computeIfAbsent(kind, k -> CategoryIdentifier.of("simplebuilding", "in_world_" + k.id()));
    }

    InWorldRecipeCatalog.Entry entry() {
        return entry;
    }

    /** Number of input slots before the tool slot. */
    int stackInputs() {
        return entry.inputs().size();
    }

    @Override
    public CategoryIdentifier<?> getCategoryIdentifier() {
        return category(entry.kind());
    }

    @Override
    public DisplaySerializer<? extends Display> getSerializer() {
        return null;
    }

    private static List<EntryIngredient> inputs(InWorldRecipeCatalog.Entry entry) {
        List<EntryIngredient> out = new ArrayList<>();
        for (InWorldRecipeCatalog.Stack stack : entry.inputs()) {
            out.add(ingredient(stack));
        }
        out.add(ingredient(new InWorldRecipeCatalog.Stack(entry.tools(), 1)));
        return out;
    }

    static EntryIngredient ingredient(InWorldRecipeCatalog.Stack stack) {
        List<ItemStack> stacks = new ArrayList<>(stack.items().size());
        for (Item item : stack.items()) {
            stacks.add(new ItemStack(item, stack.count()));
        }
        return EntryIngredients.ofItemStacks(stacks);
    }
}
