package com.simplebuilding.modules.simplemodels.client;

import com.mojang.blaze3d.vertex.VertexConsumer;
import com.mojang.renderpearl.api.pipeline.RenderPipeline;
import com.simplebuilding.modules.simplemodels.editor.EditableModel;
import com.simplebuilding.modules.simplemodels.editor.ModelEditorLayout.Rect;
import com.simplebuilding.modules.simplemodels.editor.ModelGeometry;
import com.simplebuilding.modules.simplemodels.mixin.ModelGuiStateAccessor;
import com.simplelib.api.client.ui.UiOrbitCamera;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Set;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphicsExtractor;
import net.minecraft.client.gui.navigation.ScreenRectangle;
import net.minecraft.client.gui.render.TextureSetup;
import net.minecraft.client.renderer.RenderPipelines;
import net.minecraft.client.renderer.state.gui.GuiElementRenderState;
import net.minecraft.client.renderer.texture.AbstractTexture;
import net.minecraft.client.renderer.texture.TextureAtlas;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.Identifier;
import net.minecraft.util.ARGB;
import org.joml.Matrix3x2f;
import org.joml.Vector2f;
import org.joml.Vector3f;

/**
 * Editor viewport (b-models13 stage 2): draws an {@link EditableModel} into the GUI the way {@code BlueprintView} draws a
 * blueprint - faces built on the CPU from {@link ModelGeometry} (vanilla corner order, UVs, rotation), projected
 * orthographically by SimpleLib's {@link UiOrbitCamera}, sorted back to front (painter's algorithm) and handed in as one
 * textured GUI element on the block atlas. Lines (floor grid, block outline, selection, gizmo) are thin quads in a
 * second, untextured element. No picture-in-picture renderer, so nothing differs per loader.
 */
public final class ModelViewport {
    public static final int BACKGROUND = 0xFF1E1F23;
    public static final int GRID = 0x30FFFFFF, BLOCK_OUTLINE = 0x80F2EEE8, SELECTION = 0xFFFFD84A;
    /** Axis colours (concept §3): muted red / green / blue. */
    public static final int[] AXIS = {0xFFD04848, 0xFF5DAA3C, 0xFF4A78D0};

    /** A line segment in GUI pixels. */
    public record Line(float x0, float y0, float x1, float y1, float width, int color) {}

    private ModelViewport() {}

    /** Model point (block pixels) to screen, through the camera, centred on the block. */
    public static Vector2f project(UiOrbitCamera camera, Rect r, Vector3f p) {
        return camera.project(new Vector3f(p).sub(ModelGeometry.CENTRE), r.x(), r.y(), r.w(), r.h(), ModelGeometry.VIEW_RADIUS);
    }

    /** The texture id a face draws: {@code #var} references followed (max. 8 steps), else null. */
    static Identifier resolve(EditableModel model, String texture) {
        String value = texture;
        for (int i = 0; i < 8 && value != null && value.startsWith("#"); i++) value = model.textures().get(value.substring(1));
        if (value == null || value.startsWith("#")) return null;
        return Identifier.tryParse(value);
    }

    /** The block-atlas sprite a face texture resolves to (missing sprite for unknown ids), or null without an atlas. */
    public static TextureAtlasSprite sprite(EditableModel model, String texture) {
        AbstractTexture t = Minecraft.getInstance().getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        if (!(t instanceof TextureAtlas atlas)) return null;
        Identifier id = resolve(model, texture);
        return id == null ? atlas.missingSprite() : atlas.getSprite(id);
    }

    /** Draws background, grid, model, block outline and the extra lines (selection, gizmo) into {@code r}. */
    public static void render(GuiGraphicsExtractor g, Rect r, UiOrbitCamera camera, EditableModel model, Set<Integer> hidden, List<Line> overlay) {
        g.fill(r.x(), r.y(), r.right(), r.bottom(), BACKGROUND);
        Matrix3x2f pose = new Matrix3x2f(g.pose());
        ScreenRectangle area = new ScreenRectangle(r.x(), r.y(), r.w(), r.h()).transformMaxBounds(pose);
        var state = ((ModelGuiStateAccessor) g).simplemodels$guiRenderState();
        // Floor grid 16 x 16 at y = 0 under the model.
        List<Line> grid = new ArrayList<>();
        for (int i = 0; i <= 16; i++) {
            grid.add(line(camera, r, new Vector3f(i, 0, 0), new Vector3f(i, 0, 16), 1f, GRID));
            grid.add(line(camera, r, new Vector3f(0, 0, i), new Vector3f(16, 0, i), 1f, GRID));
        }
        state.addGuiElement(new LinesState(grid, pose, area));
        state.addGuiElement(mesh(camera, r, model, hidden, pose, area));
        List<Line> lines = new ArrayList<>(box(camera, r, new Vector3f[]{
                v(0, 0, 0), v(0, 0, 16), v(0, 16, 0), v(0, 16, 16), v(16, 0, 0), v(16, 0, 16), v(16, 16, 0), v(16, 16, 16)}, 1f, BLOCK_OUTLINE));
        lines.addAll(overlay);
        state.addGuiElement(new LinesState(lines, pose, area));
    }

    private static Vector3f v(float x, float y, float z) {
        return new Vector3f(x, y, z);
    }

    public static Line line(UiOrbitCamera camera, Rect r, Vector3f a, Vector3f b, float width, int color) {
        Vector2f p = project(camera, r, a), q = project(camera, r, b);
        return new Line(p.x, p.y, q.x, q.y, width, color);
    }

    /** The 12 edges of a box given by its 8 corners (index bits x 4, y 2, z 1, see {@link ModelGeometry#corners}). */
    public static List<Line> box(UiOrbitCamera camera, Rect r, Vector3f[] corners, float width, int color) {
        List<Line> out = new ArrayList<>(12);
        for (int i = 0; i < 8; i++) for (int bit = 1; bit <= 4; bit <<= 1) {
            if ((i & bit) == 0) out.add(line(camera, r, corners[i], corners[i | bit], width, color));
        }
        return out;
    }

    private static MeshState mesh(UiOrbitCamera camera, Rect r, EditableModel model, Set<Integer> hidden, Matrix3x2f pose, ScreenRectangle area) {
        Minecraft mc = Minecraft.getInstance();
        AbstractTexture texture = mc.getTextureManager().getTexture(TextureAtlas.LOCATION_BLOCKS);
        TextureAtlas atlas = texture instanceof TextureAtlas a ? a : null;
        List<float[]> screens = new ArrayList<>();
        List<float[]> uvs = new ArrayList<>();
        List<Integer> colours = new ArrayList<>();
        List<Float> depths = new ArrayList<>();
        for (int i = 0; i < model.elements().size(); i++) {
            if (hidden.contains(i)) continue;
            EditableModel.Element e = model.elements().get(i);
            for (String dir : e.faces().keySet()) {
                ModelGeometry.Quad quad = ModelGeometry.quad(i, e, dir);
                Vector3f n = camera.rotation().transform(new Vector3f(quad.normal()));
                if (n.z < -1.0e-4f) continue; // back face
                Identifier id = resolve(model, quad.face().texture());
                TextureAtlasSprite sprite = atlas == null ? null : id == null ? atlas.missingSprite() : atlas.getSprite(id);
                if (sprite == null) continue;
                float[] screen = new float[8], uv = new float[8];
                float depth = 0;
                for (int k = 0; k < 4; k++) {
                    Vector3f p = new Vector3f(quad.pos()[k * 3], quad.pos()[k * 3 + 1], quad.pos()[k * 3 + 2]);
                    Vector2f s = project(camera, r, p);
                    screen[k * 2] = s.x;
                    screen[k * 2 + 1] = s.y;
                    depth += camera.depth(p.sub(ModelGeometry.CENTRE));
                    uv[k * 2] = sprite.getU(quad.uv()[k * 2]);
                    uv[k * 2 + 1] = sprite.getV(quad.uv()[k * 2 + 1]);
                }
                float shade = shade(quad.normal());
                screens.add(screen);
                uvs.add(uv);
                colours.add(ARGB.color(255, (int) (255 * shade), (int) (255 * shade), (int) (255 * shade)));
                depths.add(depth);
            }
        }
        Integer[] order = new Integer[screens.size()];
        for (int i = 0; i < order.length; i++) order[i] = i;
        Arrays.sort(order, (a, b) -> Float.compare(depths.get(a), depths.get(b)));
        TextureSetup setup = texture == null ? TextureSetup.noTexture() : TextureSetup.singleTexture(texture.getTextureView(), texture.getSampler());
        return new MeshState(screens, uvs, colours, order, pose, setup, area);
    }

    /** Vanilla's directional shading (top bright, bottom dark, north/south brighter than east/west). */
    static float shade(Vector3f n) {
        float up = n.y > 0 ? 1.0f : 0.5f;
        return Math.min(1f, n.x * n.x * 0.6f + n.z * n.z * 0.8f + n.y * n.y * up);
    }

    private record MeshState(List<float[]> screen, List<float[]> uv, List<Integer> colour, Integer[] order, Matrix3x2f pose,
                             TextureSetup textureSetup, ScreenRectangle area) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (int q : order) {
                float[] s = screen.get(q), t = uv.get(q);
                int c = colour.get(q);
                for (int i = 0; i < 4; i++) consumer.addVertexWith2DPose(pose, s[i * 2], s[i * 2 + 1]).setUv(t[i * 2], t[i * 2 + 1]).setColor(c);
            }
        }
        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI_TEXTURED; }
        @Override public ScreenRectangle scissorArea() { return area; }
        @Override public ScreenRectangle bounds() { return area; }
    }

    /** Line segments as thin quads (width in GUI pixels), untextured. */
    private record LinesState(List<Line> lines, Matrix3x2f pose, ScreenRectangle area) implements GuiElementRenderState {
        @Override
        public void buildVertices(VertexConsumer consumer) {
            for (Line l : lines) {
                float dx = l.x1() - l.x0(), dy = l.y1() - l.y0();
                float len = (float) Math.sqrt(dx * dx + dy * dy);
                if (len < 1.0e-3f) continue;
                float nx = -dy / len * l.width() / 2, ny = dx / len * l.width() / 2;
                // Counter-clockwise on screen like vanilla's fill (the GUI pipelines cull back faces).
                consumer.addVertexWith2DPose(pose, l.x0() - nx, l.y0() - ny).setColor(l.color());
                consumer.addVertexWith2DPose(pose, l.x0() + nx, l.y0() + ny).setColor(l.color());
                consumer.addVertexWith2DPose(pose, l.x1() + nx, l.y1() + ny).setColor(l.color());
                consumer.addVertexWith2DPose(pose, l.x1() - nx, l.y1() - ny).setColor(l.color());
            }
        }
        @Override public RenderPipeline pipeline() { return RenderPipelines.GUI; }
        @Override public TextureSetup textureSetup() { return TextureSetup.noTexture(); }
        @Override public ScreenRectangle scissorArea() { return area; }
        @Override public ScreenRectangle bounds() { return area; }
    }
}
