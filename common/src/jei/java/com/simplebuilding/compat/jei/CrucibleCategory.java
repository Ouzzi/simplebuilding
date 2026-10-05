package com.simplebuilding.compat.jei;

import com.simplebuilding.compat.CrucibleRecipeCatalog.Entry;
import com.simplebuilding.crucible.CrucibleCompat;
import java.util.List;
import java.util.Locale;
import mezz.jei.api.gui.builder.IRecipeLayoutBuilder;
import mezz.jei.api.gui.drawable.IDrawable;
import mezz.jei.api.gui.widgets.IRecipeExtrasBuilder;
import mezz.jei.api.helpers.IGuiHelper;
import mezz.jei.api.recipe.IFocusGroup;
import mezz.jei.api.recipe.category.IRecipeCategory;
import mezz.jei.api.recipe.types.IRecipeType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.Identifier;

/** Widget-only category, like MobDropCategory; loaded exclusively by JEI. */
final class CrucibleCategory implements IRecipeCategory<Entry> {
    static final IRecipeType<Entry> TYPE = IRecipeType.create("simplebuilding", "crucible", Entry.class);
    private final IDrawable icon;

    CrucibleCategory(IGuiHelper gui) {
        icon = gui.createDrawableItemLike(BuiltInRegistries.ITEM.getValue(Identifier.parse("simplelib:iron_crucible")));
    }

    @Override public IRecipeType<Entry> getRecipeType() { return TYPE; }
    @Override public Component getTitle() { return Component.translatable("jei.simplebuilding.category.crucible"); }
    @Override public int getWidth() { return 176; }
    @Override public int getHeight() { return 76; }
    @Override public IDrawable getIcon() { return icon; }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, Entry entry, IFocusGroup focuses) {
        builder.addInputSlot(1, 5).setStandardSlotBackground().addItemStacks(List.of(entry.input()));
        builder.addOutputSlot(61, 5).setOutputSlotBackground().addItemStacks(List.of(entry.result()));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, Entry entry, IFocusGroup focuses) {
        builder.addRecipeArrowWidget().setPosition(27, 5);
        int color = switch (entry.heat()) {
            case 1 -> 0xFF996600;
            case 2 -> 0xFFCC4400;
            default -> 0xFF007F88;
        };
        builder.addText(Component.translatable("jade.simplebuilding.crucible.heat",
                Component.translatable("crucible.simplebuilding.heat." + entry.heat())), 176, 10).setPosition(0, 30).setColor(color);
        String seconds = String.format(Locale.ROOT, "%.1f", CrucibleCompat.cookingTicks(entry.baseTicks(), entry.heat()) / 20.0);
        builder.addText(Component.translatable("jei.simplebuilding.crucible.time", seconds), 176, 20)
                .setPosition(0, 42).setColor(0xFF404040);
        if (entry.warming()) builder.addText(Component.translatable("jei.simplebuilding.crucible.warming"), 176, 10)
                .setPosition(0, 65).setColor(0xFF404040);
    }

    @Override
    public Identifier getIdentifier(Entry entry) {
        Identifier input = BuiltInRegistries.ITEM.getKey(entry.input().getItem());
        return Identifier.fromNamespaceAndPath("simplebuilding", "crucible/" + input.getNamespace() + "/" + input.getPath() + "/" + entry.heat());
    }
}
