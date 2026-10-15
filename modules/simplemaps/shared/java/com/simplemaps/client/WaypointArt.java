package com.simplemaps.client;

import com.simplemaps.CubeNet;
import com.simplemaps.Waypoint;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.client.resources.model.geometry.BakedQuad;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import org.jspecify.annotations.Nullable;

/** Draws a waypoint's icon (owner N65): the 3D block model, one side of the block as a flat picture, or the item. */
public final class WaypointArt {
    private WaypointArt() {}

    public static Item item(Identifier id) {
        return BuiltInRegistries.ITEM.getOptional(id).orElse(Items.AIR);
    }

    /** Whether the item has a block model that can be shown in 3D or side by side (owner N65: otherwise no side choice). */
    public static boolean hasBlockModel(Item item) {
        return item instanceof BlockItem;
    }

    public static Direction direction(int face) {
        return switch (face) {
            case CubeNet.EAST -> Direction.EAST;
            case CubeNet.SOUTH -> Direction.SOUTH;
            case CubeNet.WEST -> Direction.WEST;
            case CubeNet.UP -> Direction.UP;
            case CubeNet.DOWN -> Direction.DOWN;
            default -> Direction.NORTH;
        };
    }

    /** The texture of one side of the block (untinted); null when the item has no block model. */
    public static @Nullable TextureAtlasSprite faceSprite(Item item, int face) {
        if (!(item instanceof BlockItem blockItem) || face < CubeNet.NORTH) return null;
        BlockStateModel model = Minecraft.getInstance().getModelManager().getBlockStateModelSet().get(blockItem.getBlock().defaultBlockState());
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);
        Direction dir = direction(face);
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(dir)) return quad.materialInfo().sprite();
        }
        for (BlockStateModelPart part : parts) {
            for (BakedQuad quad : part.getQuads(null)) if (quad.direction() == dir) return quad.materialInfo().sprite();
        }
        return model.particleMaterial().sprite();
    }

    public static void draw(GuiGraphicsExtractor g, Waypoint w, int x, int y, int size) {
        draw(g, w.icon(), w.face(), x, y, size);
    }

    /** {@code size} px square at {@code x, y}. */
    public static void draw(GuiGraphicsExtractor g, Identifier icon, int face, int x, int y, int size) {
        Item item = item(icon);
        if (item == Items.AIR) return;
        TextureAtlasSprite sprite = face == CubeNet.MODEL ? null : faceSprite(item, face);
        if (sprite != null) {
            g.blitSprite(RenderPipelines.GUI_TEXTURED, sprite, x, y, size, size);
            return;
        }
        g.pose().pushMatrix();
        g.pose().translate(x, y);
        g.pose().scale(size / 16.0F, size / 16.0F);
        g.item(new ItemStack(item), 0, 0);
        g.pose().popMatrix();
    }
}
