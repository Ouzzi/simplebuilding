package com.simplebuilding.compat.jei;

import com.simplebuilding.compat.InWorldRecipeCatalog;
import com.simplebuilding.items.ModItems;
import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;
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
import net.minecraft.world.level.ItemLike;

/**
 * One in-world transformation kind as a JEI category. Layout, left to right: the inputs (block,
 * nugget), the tool slot (every tool that can do it, weakest first, cycling), an arrow (animated
 * over the real duration where there is one), the result; the notes from the catalog underneath.
 *
 * <p>Only widgets and slots, no own drawing code: {@code draw} takes {@code GuiGraphics} on
 * 1.21.11 and {@code GuiGraphicsExtractor} on 26.2, so avoiding it keeps this file identical on
 * both Minecraft lines.
 */
final class InWorldCategory implements IRecipeCategory<InWorldRecipeCatalog.Entry> {

    private static final int WIDTH = 176;
    private static final int SLOT_Y = 5;
    private static final int TEXT_TOP = 30;
    private static final int LINE_HEIGHT = 10;

    private static final Map<InWorldRecipeCatalog.Kind, IRecipeType<InWorldRecipeCatalog.Entry>> TYPES =
            new EnumMap<>(InWorldRecipeCatalog.Kind.class);

    private final InWorldRecipeCatalog.Kind kind;
    private final IDrawable icon;
    private final int height;

    InWorldCategory(InWorldRecipeCatalog.Kind kind, IGuiHelper gui, List<InWorldRecipeCatalog.Entry> entries) {
        this.kind = kind;
        this.icon = gui.createDrawableItemLike(iconOf(kind));
        int lines = 0;
        for (InWorldRecipeCatalog.Entry entry : entries) {
            lines = Math.max(lines, entry.notes().size());
        }
        this.height = TEXT_TOP + lines * LINE_HEIGHT;
    }

    static synchronized IRecipeType<InWorldRecipeCatalog.Entry> recipeType(InWorldRecipeCatalog.Kind kind) {
        return TYPES.computeIfAbsent(kind,
                k -> IRecipeType.create("simplebuilding", "in_world_" + k.id(), InWorldRecipeCatalog.Entry.class));
    }

    /** Exhaustive on purpose: a new kind does not compile until it has an icon here. */
    private static ItemLike iconOf(InWorldRecipeCatalog.Kind kind) {
        return switch (kind) {
            case MACHINE_UPGRADE -> ModItems.NETHERITE_SLEDGEHAMMER;
            case RESHAPE -> Items.STONE_STAIRS;
            case DIAMOND_CRUSH -> ModItems.DIAMOND_PEBBLE;
            case CHISEL -> ModItems.IRON_CHISEL;
            case SHEAR_WOOL -> Items.SHEARS;
            case TRIM_TEMPLATE -> ModItems.GLOWING_TRIM_TEMPLATE;
            case CAULDRON_WASH -> Items.CAULDRON;
        };
    }

    @Override
    public IRecipeType<InWorldRecipeCatalog.Entry> getRecipeType() {
        return recipeType(kind);
    }

    @Override
    public Component getTitle() {
        return Component.translatable(kind.titleKey());
    }

    @Override
    public int getWidth() {
        return WIDTH;
    }

    @Override
    public int getHeight() {
        return height;
    }

    @Override
    public IDrawable getIcon() {
        return icon;
    }

    private static int toolX(InWorldRecipeCatalog.Entry entry) {
        return entry.inputs().size() * 20 + 4;
    }

    private static int arrowX(InWorldRecipeCatalog.Entry entry) {
        return toolX(entry) + 18 + 6;
    }

    private static int outputX(InWorldRecipeCatalog.Entry entry) {
        return arrowX(entry) + 24 + 10;
    }

    @Override
    public void setRecipe(IRecipeLayoutBuilder builder, InWorldRecipeCatalog.Entry entry, IFocusGroup focuses) {
        int x = 1;
        for (InWorldRecipeCatalog.Stack stack : entry.inputs()) {
            builder.addInputSlot(x, SLOT_Y).setStandardSlotBackground().addItemStacks(stacks(stack));
            x += 20;
        }
        builder.addInputSlot(toolX(entry) + 1, SLOT_Y).setStandardSlotBackground().addItemStacks(stacks(entry.tools(), 1));
        builder.addOutputSlot(outputX(entry), SLOT_Y).setOutputSlotBackground().addItemStacks(stacks(entry.output()));
    }

    @Override
    public void createRecipeExtras(IRecipeExtrasBuilder builder, InWorldRecipeCatalog.Entry entry, IFocusGroup focuses) {
        if (entry.durationTicks() > 0) {
            builder.addAnimatedRecipeArrowWidget(entry.durationTicks()).setPosition(arrowX(entry), SLOT_Y);
        } else {
            builder.addRecipeArrowWidget().setPosition(arrowX(entry), SLOT_Y);
        }
        if (!entry.notes().isEmpty()) {
            List<FormattedText> lines = new ArrayList<>(entry.notes());
            builder.addText(lines, WIDTH, lines.size() * LINE_HEIGHT)
                    .setPosition(0, TEXT_TOP)
                    .setColor(0xFF404040);
        }
    }

    @Override
    public Identifier getIdentifier(InWorldRecipeCatalog.Entry entry) {
        return Identifier.fromNamespaceAndPath("simplebuilding", "in_world/" + entry.id().replace(':', '/'));
    }

    private static List<ItemStack> stacks(InWorldRecipeCatalog.Stack stack) {
        return stacks(stack.items(), stack.count());
    }

    private static List<ItemStack> stacks(List<Item> items, int count) {
        List<ItemStack> out = new ArrayList<>(items.size());
        for (Item item : items) {
            out.add(new ItemStack(item, count));
        }
        return out;
    }
}
