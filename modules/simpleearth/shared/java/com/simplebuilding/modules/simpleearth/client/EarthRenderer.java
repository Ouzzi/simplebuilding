package com.simplebuilding.modules.simpleearth.client;

import com.mojang.blaze3d.platform.NativeImage;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexConsumer;
import com.simplebuilding.modules.simpleearth.SimpleEarth;
import com.simplebuilding.modules.simpleearth.config.EarthClientConfig;
import com.simplebuilding.modules.simpleearth.terrain.TileMath;
import com.simplebuilding.modules.simpleearth.view.EarthView;
import com.simplebuilding.modules.simpleearth.view.Meshes;
import com.simplebuilding.modules.simpleearth.view.Patch;
import com.simplebuilding.modules.simpleearth.view.Quat;
import com.simplebuilding.modules.simpleearth.view.Scene;
import com.simplebuilding.modules.simpleearth.view.V3;
import com.simplebuilding.modules.simpleearth.view.ViewParams;
import java.util.ArrayList;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.SubmitNodeCollector;
import net.minecraft.client.renderer.rendertype.RenderTypes;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.world.phys.Vec3;

/**
 * Draws the orbit view (concept §5, §6.1) into the level: a black sky with stars, the flat sun, the cube moon (lit by the sun,
 * phase = Vanilla's moon phase, E10), the earth of the chosen version with its terrain faces, sun lighting and a stepped
 * atmosphere rim, extra lines and other players in orbit. Everything is scaled down into the camera's near range (the view is
 * scale-free, so it looks the same) and fades in over {@code fadeBand} blocks above the render switch. Below the switch nothing
 * is drawn - the world there is Vanilla.
 */
public final class EarthRenderer {
    private static final int LIGHT = 0xF000F0;
    private static final Identifier WHITE = SimpleEarth.id("earth_white"), MOON = SimpleEarth.id("earth_moon");
    private static boolean texturesReady;
    private static long lastNanos;
    private static Scene ghost, last;
    private static double ghostAlpha;
    private static float[] stars;

    /**
     * WF25-3: every earth point is pushed this many blocks (up to 32, less with a small render distance) further from the camera along its line of sight, so the picture
     * from the eye stays exactly the same but nothing of the earth is ever nearer than this - in third person (F5 looking
     * down at yourself) the earth used to slide between camera and player once its scaled surface came closer than the
     * camera distance (with 20+ chunks render distance just below y 2778).
     */
    static double push = 32;
    /** Nearest earth distance of the last frame (client test). */
    public static volatile double lastNearest = Double.NaN;
    private static double nearest;

    private EarthRenderer() {}

    /** Scaled into the near range, then pushed {@link #push} blocks back along the line of sight (same direction, same picture). */
    static V3 place(V3 rel, double scale) {
        V3 v = rel.mul(scale);
        double len = v.length();
        if (len < 1e-9) return new V3(0, -push, 0);
        if (len + push < nearest) nearest = len + push;
        return v.mul((push + len) / len);
    }

    public static void render(SubmitNodeCollector collector, PoseStack poseStack, Vec3 camera) {
        Minecraft mc = Minecraft.getInstance();
        if (mc.player == null || mc.level == null) return;
        long now = System.nanoTime();
        double dt = lastNanos == 0 ? 0 : Math.min(0.25, (now - lastNanos) / 1e9);
        lastNanos = now;
        double fade = EarthClient.fade(camera.y);
        if (fade <= 0) {
            ghost = last = null;
            return;
        }
        ensureTextures();
        FaceTextures.newFrame();
        lastNearest = nearest;
        nearest = Double.MAX_VALUE;
        float partial = mc.getDeltaTracker().getGameTimeDeltaPartialTick(false);
        EarthView view = EarthClient.view();
        double edge = EarthClient.edge();
        double h = EarthClient.eyeHeight(camera.y);
        ViewParams params = EarthClientConfig.params(edge);
        params.set("eyeHeight", h).set("heightFraction", EarthClient.heightFraction(camera.y));
        Scene scene = view.step(EarthClient.relX(partial), EarthClient.relZ(partial), dt, params);
        V3 eye = scene.contact.add(new V3(0, Math.max(0.02, h - scene.eyeDrop), 0));
        int renderBlocks = mc.options.getEffectiveRenderDistance() * 16;
        double maxDist = Math.max(24, Math.min(160, renderBlocks * 0.5));
        // everything stays inside the sky box (corners at 0.95 maxDist, i.e. before the Vanilla fog that would swallow it):
        // the earth reaches at most push + (h + 3) * scale = 0.97 of the box's face distance (WG26-2)
        double skyR = maxDist * 0.95 / Math.sqrt(3);
        push = Math.min(32, skyR * 0.3);
        double scale = (0.97 * skyR - push) / (h + 3);
        long clock = mc.level.getOverworldClockTime();
        double dayAngle = ((clock % 24000L) / 24000.0) * Math.PI * 2;
        V3 sun = new V3(Math.cos(dayAngle), Math.sin(dayAngle), 0.25).normalize();
        FaceTextures.Context[] ctx = contexts(edge, h, EarthClientConfig.get("relief"), EarthClientConfig.get("foldRadius"),
                EarthClient.ax() + EarthClient.relX(partial), EarthClient.az() + EarthClient.relZ(partial));

        sky(collector, poseStack, maxDist, sun, fade, clock);
        int reach = (int) EarthClientConfig.render("renderDistance");
        List<Patch> drawn = new ArrayList<>();
        for (Patch p : scene.patches) if (ring(p) <= reach) drawn.add(p);
        // the face under the player first: its tiles are asked for first and its image gets the paint budget first
        List<Patch> byRing = new ArrayList<>(drawn);
        byRing.sort(java.util.Comparator.comparingInt(EarthRenderer::ring));
        for (Patch p : byRing) request(scene, p, ctx[ring(p)]);
        Map<Patch, Identifier> textures = new IdentityHashMap<>();
        for (Patch p : byRing) textures.put(p, texture(scene, p, ctx[ring(p)], params));
        for (Patch p : drawn) patch(collector, poseStack, scene, p, eye, scale, sun, fade, textures.get(p));
        if (scene.closedBody && EarthClientConfig.get("atmosphere") > 0.01) atmosphere(collector, poseStack, scene, eye, scale, sun, fade);
        lines(collector, poseStack, scene, eye, scale, fade);
        others(collector, poseStack, view, scene, eye, scale, fade);
        // cross-fade (the previews' "Bild-Überblendung"): the frame before the change fades out over 0.35 s
        if (ghost != null) {
            for (Patch p : ghost.patches) {
                if (ring(p) <= reach) patch(collector, poseStack, ghost, p, eye, scale, sun, fade * ghostAlpha, texture(ghost, p, ctx[ring(p)], params));
            }
            ghostAlpha -= dt / 0.35;
            if (ghostAlpha <= 0.01) ghost = null;
        }
        if (scene.fadeStart && last != null) {
            ghost = last;
            ghostAlpha = 1;
        }
        last = scene;
    }

    /** Tile ring of a piece: 0 = the tile under the player, 1 = its neighbours, 2 = the far ring and the cube bottom. */
    static int ring(Patch p) {
        return Math.min(2, Math.max(Math.abs(p.tx), Math.abs(p.tz)));
    }

    private static FaceTextures.Context[] contexts(double edge, double h, double relief, double foldRadius, double px, double pz) {
        int[] res = TileMath.ringResolutions(edge, h, (int) EarthClientConfig.get("faceResolution"), (int) EarthClientConfig.render("detail"),
                (int) EarthClientConfig.render("detailRange"), EarthClient.minGrid());
        FaceTextures.Context[] out = new FaceTextures.Context[3];
        for (int r = 0; r < 3; r++) {
            int e = Math.max(EarthClient.minGrid(), TileMath.exponentFor(edge, res[r]));
            out[r] = new FaceTextures.Context(EarthClient.ax(), EarthClient.az(), edge, Math.min(TileMath.MAX_E, e), EarthClient.seaLevel(),
                    relief, foldRadius, res[r], px, pz);
        }
        return out;
    }

    private static void request(Scene scene, Patch p, FaceTextures.Context ctx) {
        if (p.kind != Patch.TERRAIN) return;
        FaceTextures.request(scene.curTx + p.tx, scene.curTz + p.tz, ctx);
        if (p.mix > 0) FaceTextures.request(scene.curTx + p.mixTx, scene.curTz + p.mixTz, ctx);
    }

    private static Identifier texture(Scene scene, Patch p, FaceTextures.Context ctx, ViewParams params) {
        if (p.kind != Patch.TERRAIN || p.alpha <= 0.01) return WHITE;
        // blend weight in 1/8 steps (stored as 1/16): a gliding V4/V5/V10-V12 side face reuses a few cached images instead of
        // painting a new one for every 1/16 of change (WD25-22)
        int mixQ = 2 * (int) Math.round(Meshes.clamp(p.mix, 0, 1) * 8);
        int foldQ = p.folded ? (int) Math.round(params.get("foldRadius", 0.6) * 100) : 0;
        return FaceTextures.get(new FaceTextures.Key(scene.curTx + p.tx, scene.curTz + p.tz, p.mirror, scene.curTx + p.mixTx,
                scene.curTz + p.mixTz, p.mixMirror, mixQ, ctx.res(), foldQ > 0 ? scene.curTx : 0, foldQ > 0 ? scene.curTz : 0, foldQ,
                (int) Math.round(ctx.relief() * 4), scene.equiangular), ctx);
    }

    private static void patch(SubmitNodeCollector collector, PoseStack poseStack, Scene scene, Patch p, V3 eye, double scale, V3 sun,
                              double fade, Identifier texture) {
        double alpha = p.alpha * fade;
        if (alpha <= 0.01) return;
        int base = p.kind == Patch.TERRAIN ? 0xFFFFFFFF : p.color;
        V3 center = scene.bodyOffset;
        int cols = p.cols, rows = p.rows;
        float[] xyz = new float[cols * rows * 3];
        int[] argb = new int[cols * rows];
        for (int j = 0; j < rows; j++) {
            for (int i = 0; i < cols; i++) {
                V3 at = p.at(i, j);
                V3 n = normal(p, i, j);
                if (scene.closedBody && n.dot(at.sub(center)) < 0) n = n.mul(-1);
                double light = 0.38 + 0.8 * Math.max(0, n.dot(sun));
                int k = j * cols + i;
                V3 r = place(at.sub(eye), scale);
                xyz[3 * k] = (float) r.x();
                xyz[3 * k + 1] = (float) r.y();
                xyz[3 * k + 2] = (float) r.z();
                argb[k] = shade(base, Math.min(1.15, light), alpha);
            }
        }
        collector.submitCustomGeometry(poseStack, RenderTypes.text(texture), (pose, buffer) -> {
            for (int j = 0; j < rows - 1; j++) {
                for (int i = 0; i < cols - 1; i++) {
                    int a = j * cols + i, b = a + 1, c = a + cols + 1, d = a + cols;
                    quad(pose, buffer, xyz, argb, p, a, b, c, d);
                    quad(pose, buffer, xyz, argb, p, a, d, c, b);
                }
            }
        });
    }

    private static void quad(PoseStack.Pose pose, VertexConsumer buffer, float[] xyz, int[] argb, Patch p, int... idx) {
        for (int k : idx) {
            buffer.addVertex(pose, xyz[3 * k], xyz[3 * k + 1], xyz[3 * k + 2]).setColor(argb[k]).setUv((float) p.u[k], (float) p.v[k]).setLight(LIGHT);
        }
    }

    /** Grid normal at a vertex (textured side: dv x du), from the neighbours. */
    static V3 normal(Patch p, int i, int j) {
        int i0 = Math.max(0, i - 1), i1 = Math.min(p.cols - 1, i + 1), j0 = Math.max(0, j - 1), j1 = Math.min(p.rows - 1, j + 1);
        V3 du = p.at(i1, j).sub(p.at(i0, j)), dv = p.at(i, j1).sub(p.at(i, j0));
        V3 n = dv.cross(du);
        if (n.length() < 1e-9) {
            // degenerate (triangle windows): use the whole patch
            n = p.at(0, p.rows - 1).sub(p.at(0, 0)).cross(p.at(p.cols - 1, 0).sub(p.at(0, 0)));
        }
        return n.normalize();
    }

    static int shade(int argb, double light, double alpha) {
        int r = (int) Math.min(255, ((argb >> 16) & 0xFF) * light), g = (int) Math.min(255, ((argb >> 8) & 0xFF) * light),
                b = (int) Math.min(255, (argb & 0xFF) * light);
        int a = (int) Math.round(((argb >>> 24) & 0xFF) * Meshes.clamp(alpha, 0, 1));
        return a << 24 | r << 16 | g << 8 | b;
    }

    /** Stepped fresnel rim (4-6 steps, pixel look): day blue, night indigo, dusk orange at the light border. */
    private static void atmosphere(SubmitNodeCollector collector, PoseStack poseStack, Scene scene, V3 eye, double scale, V3 sun, double fade) {
        double strength = EarthClientConfig.get("atmosphere");
        Quat q = scene.orientation;
        V3 center = scene.bodyOffset;
        collector.submitCustomGeometry(poseStack, RenderTypes.text(WHITE), (pose, buffer) -> {
            for (int f = 0; f < 6; f++) {
                Patch shell = Meshes.cubeFace(f, 0.12, 6).transform(q, V3.ZERO).scale(1.075 * scene.bodyScale);
                int cols = shell.cols;
                float[] xyz = new float[shell.pos.length * 3];
                int[] argb = new int[shell.pos.length];
                for (int k = 0; k < shell.pos.length; k++) {
                    V3 at = shell.pos[k].add(center);
                    V3 n = shell.pos[k].normalize();
                    V3 toEye = eye.sub(at).normalize();
                    double rim = Math.pow(1 - Math.abs(n.dot(toEye)), 2.6);
                    rim = Math.floor(rim * 5) / 5;
                    double d = n.dot(sun);
                    double day = Meshes.smooth(-0.25, 0.35, d), dusk = (1 - Meshes.smooth(0, 0.22, Math.abs(d))) * 0.7;
                    double cr = 0.16 + (0.42 - 0.16) * day, cg = 0.12 + (0.68 - 0.12) * day, cb = 0.42 + (1.0 - 0.42) * day;
                    cr += (1.0 - cr) * dusk;
                    cg += (0.55 - cg) * dusk;
                    cb += (0.25 - cb) * dusk;
                    int a = (int) (255 * Meshes.clamp(rim * 1.3 * strength * fade, 0, 1));
                    argb[k] = a << 24 | (int) (cr * 255) << 16 | (int) (cg * 255) << 8 | (int) (cb * 255);
                    V3 r = place(at.sub(eye), scale);
                    xyz[3 * k] = (float) r.x();
                    xyz[3 * k + 1] = (float) r.y();
                    xyz[3 * k + 2] = (float) r.z();
                }
                for (int j = 0; j < shell.rows - 1; j++) {
                    for (int i = 0; i < cols - 1; i++) {
                        int a = j * cols + i, b = a + 1, c = a + cols + 1, dd = a + cols;
                        quad(pose, buffer, xyz, argb, shell, a, b, c, dd);
                        quad(pose, buffer, xyz, argb, shell, a, dd, c, b);
                    }
                }
            }
        });
    }

    /** Lines as thin quads facing the eye (the lines render type ignores the earth's scale-down nicely only at 1 px). */
    private static void lines(SubmitNodeCollector collector, PoseStack poseStack, Scene scene, V3 eye, double scale, double fade) {
        if (scene.lines.isEmpty()) return;
        collector.submitCustomGeometry(poseStack, RenderTypes.text(WHITE), (pose, buffer) -> {
            for (int k = 0; k < scene.lines.size(); k++) {
                double[] l = scene.lines.get(k);
                double alpha = l[6] * fade;
                if (alpha <= 0.01) continue;
                V3 a = place(new V3(l[0], l[1], l[2]).sub(eye), scale), b = place(new V3(l[3], l[4], l[5]).sub(eye), scale);
                V3 mid = V3.lerp(a, b, 0.5), dir = b.sub(a), side = dir.cross(mid).normalize().mul(Math.max(0.004, mid.length() * 0.0025));
                if (!side.finite() || side.length() < 1e-9) continue;
                int c = shade(scene.lineColors.get(k), 1, alpha);
                V3[] q = {a.sub(side), b.sub(side), b.add(side), a.add(side)};
                for (int pass = 0; pass < 2; pass++) {
                    for (int m = 0; m < 4; m++) {
                        V3 v = q[pass == 0 ? m : 3 - m];
                        buffer.addVertex(pose, (float) v.x(), (float) v.y(), (float) v.z()).setColor(c).setUv(0.5f, 0.5f).setLight(LIGHT);
                    }
                }
            }
        });
    }

    /** E9: other players in orbit as small light dots on the shown tiles. */
    private static void others(SubmitNodeCollector collector, PoseStack poseStack, EarthView view, Scene scene, V3 eye, double scale, double fade) {
        List<float[]> list = EarthClient.others();
        if (list.isEmpty()) return;
        double edge = EarthClient.edge(), half = edge / 2;
        List<V3> dots = new ArrayList<>();
        for (float[] o : list) {
            double rx = o[0] - EarthClient.ax(), rz = o[1] - EarthClient.az();
            int tx = view.tileOf(rx), tz = view.tileOf(rz);
            double tu = (rx - (tx * edge - half)) / edge, tv = (rz - (tz * edge - half)) / edge;
            V3 at = scene.locate(tx - scene.curTx, tz - scene.curTz, tu, tv);
            if (at != null) dots.add(at);
        }
        if (dots.isEmpty()) return;
        collector.submitCustomGeometry(poseStack, RenderTypes.text(WHITE), (pose, buffer) -> {
            for (V3 d : dots) billboard(pose, buffer, place(d.sub(eye), scale), Math.max(0.05, place(d.sub(eye), scale).length() * 0.012), shade(0xFFFFF2A0, 1, fade));
        });
    }

    /** Black sky (a cube inside the view range that hides the Vanilla sky), stars, the flat sun and the cube moon. */
    private static void sky(SubmitNodeCollector collector, PoseStack poseStack, double maxDist, V3 sun, double fade, long clock) {
        // the sky box: corners before the fog, the earth inside it (see render)
        double r = maxDist * 0.95 / Math.sqrt(3);
        double starLevel = EarthClientConfig.get("stars");
        int black = shade(0xFF020308, 1, fade);
        collector.submitCustomGeometry(poseStack, RenderTypes.text(WHITE), (pose, buffer) -> {
            for (int f = 0; f < 6; f++) {
                V3[] c = new V3[4];
                double[][] ab = {{-1, -1}, {1, -1}, {1, 1}, {-1, 1}};
                for (int k = 0; k < 4; k++) c[k] = Meshes.facePoint(f, ab[k][0], ab[k][1]).mul(r);
                for (int pass = 0; pass < 2; pass++) {
                    for (int m = 0; m < 4; m++) {
                        V3 v = c[pass == 0 ? m : 3 - m];
                        buffer.addVertex(pose, (float) v.x(), (float) v.y(), (float) v.z()).setColor(black).setUv(0.5f, 0.5f).setLight(LIGHT);
                    }
                }
            }
            if (starLevel > 0.01) {
                float[] s = stars();
                double dist = r * 0.97;
                for (int i = 0; i < s.length; i += 4) {
                    V3 d = new V3(s[i], s[i + 1], s[i + 2]);
                    int a = (int) (255 * Meshes.clamp(s[i + 3] * starLevel * fade, 0, 1));
                    billboard(pose, buffer, d.mul(dist), dist * 0.0028, a << 24 | 0xDFE6FF);
                }
            }
            // the sun: a flat pixel square far away (no parallax), with a soft square halo
            billboard(pose, buffer, sun.mul(r * 0.9), r * 0.05, shade(0xFFFFF1A8, 1, fade));
            billboard(pose, buffer, sun.mul(r * 0.899), r * 0.032, shade(0xFFFFFBE6, 1, fade));
            for (int k = 1; k <= 4; k++) billboard(pose, buffer, sun.mul(r * 0.901), r * (0.05 + 0.03 * k), shade(0x14FFDC78, 1, fade));
        });
        moon(collector, poseStack, r, sun, fade, clock);
    }

    /** E10: the moon as a rounded cube lit by the sun; its place relative to the sun follows Vanilla's moon phase. */
    private static void moon(SubmitNodeCollector collector, PoseStack poseStack, double r, V3 sun, double fade, long clock) {
        double size = EarthClientConfig.get("moonSize");
        if (size <= 0.01) return;
        // E10: the phase follows Vanilla's moon (client option moonPhases); off = always full
        int phase = EarthClientConfig.renderOn("moonPhases") ? (int) ((clock / 24000L % 8L + 8L) % 8L) : 0;
        double angle = Math.PI - phase * Math.PI / 4;
        V3 dir = Quat.axisAngle(new V3(0, 0, 1), angle).rotate(sun).normalize();
        V3 centre = dir.mul(r * 0.96);
        double half = r * 0.035 * size;
        Quat spin = Quat.fromUnitVectors(new V3(0, 0, 1), dir.mul(-1));
        collector.submitCustomGeometry(poseStack, RenderTypes.text(MOON), (pose, buffer) -> {
            for (int f = 0; f < 6; f++) {
                Patch face = Meshes.cubeFace(f, 0.2, 2).transform(spin, V3.ZERO);
                int cols = face.cols;
                float[] xyz = new float[face.pos.length * 3];
                int[] argb = new int[face.pos.length];
                for (int k = 0; k < face.pos.length; k++) {
                    V3 n = face.pos[k].normalize();
                    double light = 0.08 + 1.0 * Math.max(0, n.dot(sun));
                    argb[k] = shade(0xFFFFFFFF, light, fade);
                    V3 v = centre.add(face.pos[k].mul(half));
                    xyz[3 * k] = (float) v.x();
                    xyz[3 * k + 1] = (float) v.y();
                    xyz[3 * k + 2] = (float) v.z();
                }
                for (int j = 0; j < face.rows - 1; j++) {
                    for (int i = 0; i < cols - 1; i++) {
                        int a = j * cols + i, b = a + 1, c = a + cols + 1, d = a + cols;
                        quad(pose, buffer, xyz, argb, face, a, b, c, d);
                        quad(pose, buffer, xyz, argb, face, a, d, c, b);
                    }
                }
            }
        });
    }

    /** A square facing the camera (at the origin), both windings. */
    private static void billboard(PoseStack.Pose pose, VertexConsumer buffer, V3 at, double half, int argb) {
        V3 d = at.normalize();
        V3 t1 = d.cross(Math.abs(d.y()) > 0.9 ? new V3(1, 0, 0) : V3.UP).normalize().mul(half), t2 = d.cross(t1).normalize().mul(half);
        V3[] q = {at.sub(t1).sub(t2), at.add(t1).sub(t2), at.add(t1).add(t2), at.sub(t1).add(t2)};
        for (int pass = 0; pass < 2; pass++) {
            for (int m = 0; m < 4; m++) {
                V3 v = q[pass == 0 ? m : 3 - m];
                buffer.addVertex(pose, (float) v.x(), (float) v.y(), (float) v.z()).setColor(argb).setUv(0.5f, 0.5f).setLight(LIGHT);
            }
        }
    }

    /** 1 200 stars, fixed pseudo-random directions and brightness (the previews' hash). */
    private static float[] stars() {
        if (stars != null) return stars;
        float[] s = new float[1200 * 4];
        for (int i = 0; i < 1200; i++) {
            double u = hash(i, 1, 3) * 2 - 1, a = hash(i, 2, 3) * Math.PI * 2, q = Math.sqrt(1 - u * u);
            s[4 * i] = (float) (q * Math.cos(a));
            s[4 * i + 1] = (float) u;
            s[4 * i + 2] = (float) (q * Math.sin(a));
            s[4 * i + 3] = (float) (0.35 + 0.65 * hash(i, 4, 3));
        }
        return stars = s;
    }

    static double hash(int ix, int iz, int seed) {
        int h = ix * 374761393 ^ iz * 668265263 ^ seed * 362437;
        h = (h ^ (h >>> 13)) * 1274126177;
        return ((h ^ (h >>> 16)) & 0x7FFFFFFF) / (double) 0x7FFFFFFF;
    }

    /** A white texel for plain colours and a 16 x 16 pixel moon (own pixels, A8: no Vanilla copy). */
    private static void ensureTextures() {
        if (texturesReady) return;
        texturesReady = true;
        DynamicTexture white = new DynamicTexture(() -> "simpleearth white", 1, 1, true);
        white.getPixels().setPixel(0, 0, 0xFFFFFFFF);
        white.upload();
        Minecraft.getInstance().getTextureManager().register(WHITE, white);
        DynamicTexture moon = new DynamicTexture(() -> "simpleearth moon", 16, 16, true);
        NativeImage img = moon.getPixels();
        for (int y = 0; y < 16; y++) {
            for (int x = 0; x < 16; x++) {
                int v = 170 + (int) (hash(x, y, 99) * 40);
                img.setPixel(x, y, 0xFF000000 | v << 16 | v << 8 | Math.min(255, v + 8));
            }
        }
        int[][] craters = {{3, 4, 3}, {10, 9, 4}, {11, 3, 2}, {5, 11, 2}};
        for (int[] c : craters) {
            for (int y = 0; y < c[2]; y++) for (int x = 0; x < c[2]; x++) img.setPixel(c[0] + x, c[1] + y, y == 0 ? 0xFFC9C9D4 : 0xFF8E8E9A);
        }
        moon.upload();
        Minecraft.getInstance().getTextureManager().register(MOON, moon);
    }

    /** After a resource reload the registered textures are gone. */
    public static void reload() {
        texturesReady = false;
        FaceTextures.clear();
    }
}
