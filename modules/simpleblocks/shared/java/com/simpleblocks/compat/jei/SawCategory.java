package com.simpleblocks.compat.jei;

import com.simpleblocks.saw.Sawing;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;
import net.minecraft.world.item.ItemStack;

/** JEI category of the cuts only the Shapecutter has ({@code simpleblocks:sawing}); the stonecutter's cuts stay in JEI's own category. */
public final class SawCategory implements IRecipeCategory<Sawing.Entry> {
    public static final IRecipeType<Sawing.Entry> TYPE = IRecipeType.create("simpleblocks", "sawing", Sawing.Entry.class);
    private final IDrawable icon;

    public SawCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(Sawing.SAW_ITEM);
    }

    @Override public IRecipeType<Sawing.Entry> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.simpleblocks.category.sawing"); }
    @Override public int getWidth() { return 82; }
    @Override public int getHeight() { return 26; }
    @Override public IDrawable getIcon() { return this.icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Sawing.Entry entry, IFocusGroup focuses) {
        builder.addInputSlot(1, 5).setStandardSlotBackground()
                .addItemStacks(entry.input().items().map(h -> new ItemStack(h.value())).toList());
        builder.addOutputSlot(61, 5).setOutputSlotBackground().addItemStack(entry.resultStack());
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Sawing.Entry entry, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(27, 5);
    }

    @Override
    public Identifier getIdentifier(Sawing.Entry entry) {
        return entry.id();
    }
}
