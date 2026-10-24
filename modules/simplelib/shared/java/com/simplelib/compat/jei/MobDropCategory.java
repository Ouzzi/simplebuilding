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
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The shared JEI tab "Mob Drops" of the Simple mods (JEI24): victim (spawn egg), killer (if one is needed), arrow, up to
 * five result slots (more results cycle in the last one), the notes underneath. Like {@link InWorldUpgradeCategory} only
 * widgets and slots. Registered by {@link SimpleLibJeiPlugin}; JEI hides it while no module added a drop.
 */
public final class MobDropCategory implements IRecipeCategory<MobDrop> {
    public static final IRecipeType<MobDrop> TYPE = IRecipeType.create("simplelib", "mob_drop", MobDrop.class);
    private static final int WIDTH = 176;
    private static final int SLOT_Y = 5;
    private static final int TEXT_TOP = 30;
    private static final int LINE_HEIGHT = 10;
    private static final int HEIGHT = TEXT_TOP + 3 * LINE_HEIGHT;
    private static final int RESULT_X = 83;
    private static final int RESULT_SLOTS = 5;

    private final IDrawable icon;

    public MobDropCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(Items.ZOMBIE_HEAD);
    }

    @Override
    public IRecipeType<MobDrop> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.simplelib.category.mob_drop");
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

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MobDrop drop, IFocusGroup focuses) {
        if (!drop.victimIcon().isEmpty()) builder.addInputSlot(1, SLOT_Y).setStandardSlotBackground().addItemStack(drop.victimIcon());
        if (!drop.killerIcon().isEmpty()) builder.addInputSlot(25, SLOT_Y).setStandardSlotBackground().addItemStack(drop.killerIcon());
        List<ItemStack> results = drop.results();
        int own = results.size() <= RESULT_SLOTS ? results.size() : RESULT_SLOTS - 1;
        for (int i = 0; i < own; i++) {
            builder.addOutputSlot(RESULT_X + i * 18, SLOT_Y).setOutputSlotBackground().addItemStack(results.get(i));
        }
        if (own < results.size()) {
            builder.addOutputSlot(RESULT_X + own * 18, SLOT_Y).setOutputSlotBackground()
                    .addItemStacks(new ArrayList<>(results.subList(own, results.size())));
        }
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MobDrop drop, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(49, SLOT_Y);
        if (!drop.notes().isEmpty()) {
            List<FormattedText> lines = new ArrayList<>(drop.notes());
            builder.addText(lines, WIDTH, 3 * LINE_HEIGHT).setPosition(0, TEXT_TOP).setColor(0xFF404040);
        }
    }

    @Override
    public Identifier getIdentifier(MobDrop drop) {
        return Identifier.fromNamespaceAndPath("simplelib", "mob_drop/" + drop.id().replace(':', '/'));
    }
}
