package com.simplelib.compat.jei;

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
import net.minecraft.network.chat.FormattedText;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The shared JEI tab of the Simple mods for upgrades done in the world (principle "common things live in SimpleLib"):
 * inputs, the tool slot, an arrow and the result, the notes underneath. Only widgets and slots, no own drawing.
 * Registered by {@link SimpleLibJeiPlugin}; JEI hides it while no module added an entry.
 */
public final class InWorldUpgradeCategory implements IRecipeCategory<InWorldUpgrade> {
    public static final IRecipeType<InWorldUpgrade> TYPE = IRecipeType.create("simplelib", "in_world_upgrade", InWorldUpgrade.class);
    private static final int WIDTH = 176;
    private static final int SLOT_Y = 5;
    private static final int TEXT_TOP = 30;
    private static final int LINE_HEIGHT = 10;
    /** Room for the longest note block (a module's notes are 2-3 lines). */
    private static final int HEIGHT = TEXT_TOP + 3 * LINE_HEIGHT;

    private final IDrawable icon;

    public InWorldUpgradeCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(Items.ANVIL);
    }

    @Override
    public IRecipeType<InWorldUpgrade> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.simplelib.category.in_world_upgrade");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    private static int toolX(InWorldUpgrade entry) {
        return entry.inputs().size() * 20 + 4;
    }

    private static int arrowX(InWorldUpgrade entry) {
        return toolX(entry) + 18 + 6;
    }

    private static int outputX(InWorldUpgrade entry) {
        return arrowX(entry) + 24 + 10;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, InWorldUpgrade entry, IFocusGroup focuses) {
        int x = 1;
        for (InWorldUpgrade.Input input : entry.inputs()) {
            builder.addInputSlot(x, SLOT_Y).setStandardSlotBackground().addItemStacks(stacks(input.items(), input.count()));
            x += 20;
        }
        builder.addInputSlot(toolX(entry) + 1, SLOT_Y).setStandardSlotBackground().addItemStacks(stacks(entry.tools(), 1));
        builder.addOutputSlot(outputX(entry), SLOT_Y).setOutputSlotBackground().addItemStack(entry.output());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, InWorldUpgrade entry, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(arrowX(entry), SLOT_Y);
        if (!entry.notes().isEmpty()) {
            List<FormattedText> lines = new ArrayList<>(entry.notes());
            builder.addText(lines, WIDTH, lines.size() * LINE_HEIGHT).setPosition(0, TEXT_TOP).setColor(0xFF404040);
        }
    }

    @Override
    public Identifier getIdentifier(InWorldUpgrade entry) {
        return Identifier.fromNamespaceAndPath("simplelib", "in_world_upgrade/" + entry.id().replace(':', '/'));
    }

    private static List<ItemStack> stacks(List<Item> items, int count) {
        List<ItemStack> out = new ArrayList<>(items.size());
        for (Item item : items) out.add(new ItemStack(item, count));
        return out;
    }
}
