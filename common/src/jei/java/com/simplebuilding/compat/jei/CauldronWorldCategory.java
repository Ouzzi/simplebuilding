package com.simplebuilding.compat.jei;

import com.simplebuilding.compat.CauldronWorldCatalog.Entry;
import com.simplebuilding.compat.InWorldRecipeCatalog.Stack;
import java.util.ArrayList;
import java.util.List;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * "Cauldron and crucible": the in-world ways around cauldrons and crucibles (build, attach a barrel,
 * reinforce, butter, cheese). Layout like {@code InWorldCategory}: the inputs, the tool slot (only when the entry
 * has tools), an arrow, the result; one text widget per note underneath. Widgets only, no drawing code.
 */
final class CauldronWorldCategory implements IRecipeCategory<Entry> {
    static final IRecipeType<Entry> TYPE = IRecipeType.create("simplebuilding", "cauldron_world", Entry.class);

    private static final int WIDTH = 176;
    private static final int SLOT_Y = 5;
    private static final int TEXT_TOP = 30;
    private static final int NOTE_HEIGHT = 20;

    private final IDrawable icon;
    private final int height;

    CauldronWorldCategory(IGuiHelper gui, List<Entry> entries) {
        icon = gui.createDrawableItemLike(Items.CAULDRON);
        int notes = 0;
        for (Entry entry : entries) {
            notes = Math.max(notes, entry.notes().size());
        }
        height = TEXT_TOP + notes * NOTE_HEIGHT;
    }

    @Override public IRecipeType<Entry> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.simplebuilding.category.cauldron_world"); }
    @Override public int getWidth() { return WIDTH; }
    @Override public int getHeight() { return height; }
    @Override public IDrawable getIcon() { return icon; }

    private static int toolX(Entry entry) {
        return entry.inputs().size() * 20 + 4;
    }

    private static int arrowX(Entry entry) {
        return entry.tools().isEmpty() ? toolX(entry) : toolX(entry) + 18 + 6;
    }

    private static int outputX(Entry entry) {
        return arrowX(entry) + 24 + 10;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Entry entry, IFocusGroup focuses) {
        int x = 1;
        for (Stack stack : entry.inputs()) {
            builder.addInputSlot(x, SLOT_Y).setStandardSlotBackground().addItemStacks(stacks(stack.items(), stack.count()));
            x += 20;
        }
        if (!entry.tools().isEmpty()) {
            builder.addInputSlot(toolX(entry) + 1, SLOT_Y).setStandardSlotBackground().addItemStacks(stacks(entry.tools(), 1));
        }
        builder.addOutputSlot(outputX(entry), SLOT_Y).setOutputSlotBackground()
                .addItemStacks(stacks(entry.output().items(), entry.output().count()));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Entry entry, IFocusGroup focuses) {
        if (entry.durationTicks() > 0) {
            builder.addAnimatedRecipeArrowWidget(entry.durationTicks()).setPosition(arrowX(entry), SLOT_Y);
        } else {
            builder.addRecipeArrowWidget().setPosition(arrowX(entry), SLOT_Y);
        }
        int y = TEXT_TOP;
        for (Component note : entry.notes()) {
            builder.addText(note, WIDTH, NOTE_HEIGHT).setPosition(0, y).setColor(0xFF404040);
            y += NOTE_HEIGHT;
        }
    }

    @Override
    public Identifier getIdentifier(Entry entry) {
        return Identifier.fromNamespaceAndPath("simplebuilding", "cauldron_world/" + entry.id());
    }

    private static List<ItemStack> stacks(List<Item> items, int count) {
        List<ItemStack> out = new ArrayList<>(items.size());
        for (Item item : items) {
            out.add(new ItemStack(item, count));
        }
        return out;
    }
}
