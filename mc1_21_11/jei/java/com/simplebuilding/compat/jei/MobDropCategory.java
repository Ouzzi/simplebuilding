package com.simplebuilding.compat.jei;

import com.simplebuilding.compat.MobDropCatalog;
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
 * "Mob drops": items that only come from a mob dying in one particular way - a head from a charged
 * creeper's explosion (vanilla heads and the mod's blaze head), a music disc from a creeper a skeleton
 * killed. Layout: victim (spawn egg) + killer (spawn egg), arrow, the result(s) cycling; the way it
 * has to die underneath. Data from {@link MobDropCatalog}.
 *
 * <p>Like {@link InWorldCategory} only widgets and slots, no drawing code, so this file is the same
 * on every Minecraft line.
 */
final class MobDropCategory implements IRecipeCategory<MobDropCatalog.Drop> {

    static final IRecipeType<MobDropCatalog.Drop> TYPE =
            IRecipeType.create("simplebuilding", "mob_drop", MobDropCatalog.Drop.class);

    private static final int WIDTH = 176;
    private static final int SLOT_Y = 5;
    private static final int TEXT_TOP = 30;
    private static final int LINE_HEIGHT = 10;

    private final IDrawable icon;

    MobDropCategory(IGuiHelper gui) {
        this.icon = gui.createDrawableItemLike(Items.CREEPER_HEAD);
    }

    @Override
    public IRecipeType<MobDropCatalog.Drop> getRecipeType() {
        return TYPE;
    }

    @Override
    public Component getTitle() {
        return Component.translatable("jei.simplebuilding.category.mob_drop");
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return TEXT_TOP + 3 * LINE_HEIGHT;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, MobDropCatalog.Drop drop, IFocusGroup focuses) {
        builder.addInputSlot(1, SLOT_Y).setStandardSlotBackground().addItemStacks(List.of(new ItemStack(drop.victimIcon())));
        builder.addInputSlot(25, SLOT_Y).setStandardSlotBackground().addItemStacks(List.of(new ItemStack(drop.killerIcon())));
        List<ItemStack> results = new ArrayList<>();
        for (Item item : drop.results()) {
            results.add(new ItemStack(item));
        }
        builder.addOutputSlot(83, SLOT_Y).setOutputSlotBackground().addItemStacks(results);
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, MobDropCatalog.Drop drop, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(49, SLOT_Y);
        List<FormattedText> lines = new ArrayList<>();
        lines.add(Component.translatable(drop.cause().noteKey(), drop.victim().getDescription(), drop.results().size()));
        builder.addText(lines, WIDTH, 3 * LINE_HEIGHT).setPosition(0, TEXT_TOP).setColor(0xFF404040);
    }

    @Override
    public Identifier getIdentifier(MobDropCatalog.Drop drop) {
        return Identifier.fromNamespaceAndPath("simplebuilding", "mob_drop/" + drop.id().replace(':', '/'));
    }
}
