package com.simplebuilding.clientgametest;

import com.simplebuilding.util.BuildingToolRefs;
import com.simplebuilding.util.ChiselRefs;
import com.simplebuilding.version.McVersion;
import java.util.Map;
import net.minecraft.client.KeyMapping;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;

/**
 * The building tools' and chisels' classes the client tests look into. On 26.3 they belong to Simple Building Tools
 * ({@code com.simplebuildingtools.*}) and Simple Chisels; on 26.2 they are SimpleBuilding's own ({@code com.simplebuilding.*},
 * {@link McVersion#BTOOLS_IN_SB}). This tree is also compiled where the sub-mods are not on the classpath (26.2, 26.3
 * NeoForge), so it names their classes by string only - like {@link BuildingToolRefs} names their items by id. Both
 * packages share the relative class names, so one path resolves on either line.
 */
final class MovedTools {
    private MovedTools() {
    }

    /** The class at {@code relative} (e.g. {@code "client.gui.OctantScreen"}) in the building tools' package of this line. */
    static Class<?> btools(String relative) {
        String name = (McVersion.BTOOLS_IN_SB ? "com.simplebuilding." : "com.simplebuildingtools.") + relative;
        try {
            return Class.forName(name);
        } catch (ClassNotFoundException e) {
            throw new AssertionError("The building tools class " + name + " is not on this client's classpath", e);
        }
    }

    @SuppressWarnings("unchecked")
    static Class<? extends Screen> octantScreen() {
        return (Class<? extends Screen>) btools("client.gui.OctantScreen");
    }

    @SuppressWarnings("unchecked")
    static Class<? extends Screen> buildingWandScreen() {
        return (Class<? extends Screen>) btools("client.gui.BuildingWandScreen");
    }

    /** A new screen of {@code screenClass} for {@code stack}, through its public {@code (ItemStack)} constructor. */
    static Screen newScreen(Class<? extends Screen> screenClass, ItemStack stack) {
        try {
            return screenClass.getConstructor(ItemStack.class).newInstance(stack);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not build " + screenClass.getName() + " for " + stack, e);
        }
    }

    /** The building tools' octant figure toggle key ({@code ClientState.octantFigureToggleKey} of this line), null when unregistered. */
    static KeyMapping octantFigureToggleKey() {
        return toolsKey("octantFigureToggleKey");
    }

    /** The building tools' settings key (octant manager, wand settings; {@code ClientState.settingsKey} of this line), null when unregistered. */
    static KeyMapping settingsKey() {
        return toolsKey("settingsKey");
    }

    private static KeyMapping toolsKey(String field) {
        try {
            return (KeyMapping) btools("client.ClientState").getField(field).get(null);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("ClientState." + field + " is gone", e);
        }
    }

    /** {@code RangefinderHudOverlay.render} of this line. */
    static void renderRangefinder(GuiGraphicsExtractor graphics) {
        try {
            btools("client.gui.RangefinderHudOverlay").getMethod("render", GuiGraphicsExtractor.class).invoke(null, graphics);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("RangefinderHudOverlay.render could not be called", e);
        }
    }

    /**
     * {@code BuildingWandItem.getPreviewStates} for the wand {@code stack} (its own square diameter), asked through the
     * held item's class.
     */
    @SuppressWarnings("unchecked")
    static Map<BlockPos, BlockState> previewStates(Level level, Player player, ItemStack stack, BlockPos origin, Direction face) {
        Item wand = stack.getItem();
        try {
            int diameter = (int) wand.getClass().getMethod("getWandSquareDiameter").invoke(wand);
            return (Map<BlockPos, BlockState>) wand.getClass().getMethod("getPreviewStates", Level.class, Player.class,
                    ItemStack.class, BlockPos.class, Direction.class, int.class).invoke(null, level, player, stack, origin, face, diameter);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("BuildingWandItem.getPreviewStates could not be called on " + stack, e);
        }
    }

    /** A blueprint with this content ({@code BlueprintContent} and the blueprint component of this line). */
    @SuppressWarnings("unchecked")
    static ItemStack blueprint(String code, String title, String author, boolean signed) {
        ItemStack stack = new ItemStack(BuildingToolRefs.blueprint());
        DataComponentType<Object> type = (DataComponentType<Object>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(BuildingToolRefs.id("blueprint"));
        try {
            stack.set(type, btools("blueprint.BlueprintContent").getConstructor(String.class, String.class, String.class, boolean.class)
                    .newInstance(code, title, author, signed));
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("Could not build a blueprint for '" + code + "'", e);
        }
        return stack;
    }

    /** Whether {@code item} is the chisel of {@code tier} on this line. */
    static boolean isChisel(Item item, String tier) {
        return BuiltInRegistries.ITEM.getKey(item).equals(ChiselRefs.id(tier + "_chisel"));
    }

    /** One of the chisel's tables ({@code "getForwardMap"}, ...), read from the registered chisel of {@code tier}. */
    @SuppressWarnings("unchecked")
    static Map<Block, Block> chiselTable(String tier, String getter) {
        Item chisel = ChiselRefs.item(tier);
        try {
            return (Map<Block, Block>) chisel.getClass().getMethod(getter).invoke(chisel);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("ChiselItem." + getter + " could not be read from " + chisel, e);
        }
    }
}
