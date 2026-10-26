package com.simplebuilding.modules.simpleearth;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.simplebuilding.modules.simpleearth.config.EarthSettings;
import com.simplebuilding.modules.simpleearth.view.EarthView;
import com.simplebuilding.modules.simpleearth.view.Patch;
import com.simplebuilding.modules.simpleearth.view.Quat;
import com.simplebuilding.modules.simpleearth.view.Scene;
import com.simplebuilding.modules.simpleearth.view.V3;
import com.simplebuilding.modules.simpleearth.view.ViewParams;
import org.junit.jupiter.api.Test;

/** The fold/turn maths of V1-V12 without the game: finite, point under the player on top, no jumps where none are announced. */
class EarthViewTest {
    static final double L = 65536, H = L / 2, DT = 1 / 60.0;

    static ViewParams params() {
        ViewParams p = new ViewParams();
        p.edge = L;
        return p;
    }

    /** Lets the slerped versions settle on a spot. */
    static Scene settle(EarthView view, double x, double z) {
        Scene s = null;
        for (int i = 0; i < 600; i++) s = view.step(x, z, DT, params());
        return s;
    }

    @Test
    void quaternionBasics() {
        Quat q = Quat.fromUnitVectors(new V3(1, 1, 0).normalize(), V3.UP);
        V3 r = q.rotate(new V3(1, 1, 0).normalize());
        assertTrue(r.distance(V3.UP) < 1e-9, r.toString());
        Quat a = Quat.axisAngle(V3.UP, 1.0), b = Quat.axisAngle(V3.UP, 2.0);
        assertEquals(57.2958, a.angleTo(b), 1e-3);
        assertTrue(a.slerp(b, 0.5).angleTo(Quat.axisAngle(V3.UP, 1.5)) < 1e-6);
    }

    @Test
    void everyVersionStaysFiniteOnLongFlights() {
        double[][] dirs = {{0, -1}, {1, 0}, {0.7071, -0.7071}, {-0.6, 0.8}, {0.31, 0.95}};
        for (int ver = 1; ver <= EarthSettings.VERSIONS; ver++) {
            for (double[] d : dirs) {
                EarthView view = EarthView.create(ver);
                double x = 0, z = 0, step = L * 0.004;
                for (int i = 0; i < 1500; i++) {
                    x += d[0] * step;
                    z += d[1] * step;
                    Scene s = view.step(x, z, DT, params());
                    assertTrue(s.finite(), "V" + ver + " frame " + i);
                    assertFalse(s.patches.isEmpty(), "V" + ver);
                }
            }
        }
    }

    @Test
    void pointUnderThePlayerIsTheContactPointOnTop() {
        double[][] spots = {{0, 0}, {0.3, -0.2}, {-0.7, 0.55}, {0.92, 0.1}, {0.85, -0.8}};
        for (int ver = 1; ver <= EarthSettings.VERSIONS; ver++) {
            for (double[] sp : spots) {
                EarthView view = EarthView.create(ver);
                Scene s = settle(view, sp[0] * H, sp[1] * H);
                V3 at = s.locate(0, 0, (sp[0] + 1) / 2, (sp[1] + 1) / 2);
                assertNotNull(at, "V" + ver + " " + sp[0] + "/" + sp[1]);
                assertTrue(at.distance(s.contact) < 0.03, "V" + ver + " at " + at + " contact " + s.contact);
                // the contact point lies straight below the eye (eye = contact + up * h)
                V3 dir = s.contact.sub(s.bodyOffset).normalize();
                // V5: the ground normal under the player points up, the body is moved so the contact sits at (0, 1, 0)
                if (ver >= 5) assertTrue(Math.hypot(s.contact.x(), s.contact.z()) < 1e-6, "V" + ver + " contact straight below the eye");
                else assertTrue(Math.hypot(dir.x(), dir.z()) < 0.08, "V" + ver + " tilt " + dir);
            }
        }
    }

    /** Distance of a fixed world point (tile + uv) from the contact point, frame by frame, across an edge. */
    static double maxJump(int ver, double v0, double du) {
        EarthView view = EarthView.create(ver);
        double x = 0.9 * H, z = v0 * H;
        settle(view, x, z);
        // anchors: 0.3 faces east of the start on the start tile, and a point on the east neighbour
        double[][] anchors = {{1.2 * H, z + 0.1 * H}, {1.5 * H, z - 0.2 * H}, {0.6 * H, z}};
        V3[] last = new V3[anchors.length];
        double worst = 0;
        for (int i = 0; i < 60; i++) {
            x += du * H;
            Scene s = view.step(x, z, DT, params());
            for (int k = 0; k < anchors.length; k++) {
                V3 rel = relative(view, s, anchors[k][0], anchors[k][1]);
                if (rel != null && last[k] != null && !s.fadeStart) worst = Math.max(worst, rel.distance(last[k]) - Math.abs(du) * 1.05);
                last[k] = rel;
            }
        }
        return worst;
    }

    static V3 relative(EarthView view, Scene s, double wx, double wz) {
        int tx = view.tileOf(wx) - s.curTx, tz = view.tileOf(wz) - s.curTz;
        double tu = (wx - (view.tileOf(wx) * L - H)) / L, tv = (wz - (view.tileOf(wz) * L - H)) / L;
        V3 at = s.locate(tx, tz, tu, tv);
        return at == null ? null : at.sub(s.contact);
    }

    @Test
    void noJumpAcrossAnEdgeForTheSmoothVersions() {
        for (int ver : new int[]{4, 5, 6, 7, 9}) {
            for (double v0 : new double[]{0.0, 0.4, -0.25}) {
                double jump = maxJump(ver, v0, 0.004);
                assertTrue(jump < 0.02, "V" + ver + " v=" + v0 + " jump " + jump);
            }
        }
        // the metric does see the owner's finding (WA24-6): the shortest-rotation cube of V1 jerks at a skewed edge
        assertTrue(maxJump(1, 0.4, 0.004) > 0.04, "V1 skewed edge jump");
    }

    @Test
    void v9AndV6StaySmoothOverTheCornerOfTheWorld() {
        for (int ver : new int[]{6, 9}) {
            EarthView view = EarthView.create(ver);
            double x = 0.9 * H, z = -0.9 * H, step = 0.004 * H;
            settle(view, x, z);
            double[][] anchors = {{1.3 * H, -1.2 * H}, {0.7 * H, -0.8 * H}, {1.2 * H, -0.7 * H}};
            V3[] last = new V3[anchors.length];
            double worst = 0;
            for (int i = 0; i < 50; i++) {
                x += step;
                z -= step;
                Scene s = view.step(x, z, DT, params());
                for (int k = 0; k < anchors.length; k++) {
                    V3 rel = relative(view, s, anchors[k][0], anchors[k][1]);
                    if (rel != null && last[k] != null && !s.fadeStart) worst = Math.max(worst, rel.distance(last[k]) - 0.004 * 1.5);
                    last[k] = rel;
                }
            }
            assertTrue(worst < 0.02, "V" + ver + " corner jump " + worst);
        }
    }

    @Test
    void newTerrainBehindEveryEdgeLikeATesseract() {
        for (int ver = 1; ver <= EarthSettings.VERSIONS; ver++) {
            EarthView view = EarthView.create(ver);
            Scene s = null;
            for (double z = 0; z >= -4.2 * L; z -= L * 0.01) s = view.step(0, z, DT, params());
            assertEquals(-4, s.curTz, "V" + ver + ": four edges north = four new tiles, never back to the start");
            assertEquals(0, s.curTx);
        }
    }

    @Test
    void cornerOfTheWorldTurnsAndV3ShowsTheTesseract() {
        for (int ver : new int[]{1, 3}) {
            EarthView view = EarthView.create(ver);
            settle(view, 0.99 * H, -0.99 * H);
            Scene s = view.step(1.01 * H, -1.01 * H, DT, params());
            assertEquals("corner", s.event, "V" + ver);
            Scene next = view.step(1.011 * H, -1.011 * H, 0.3, params());
            if (ver == 3) assertFalse(next.lines.isEmpty(), "V3 inversion lines");
            assertEquals(1, next.curTx);
            assertEquals(-1, next.curTz);
        }
    }

    @Test
    void v3AndV8OpenACornerWindow() {
        for (int ver : new int[]{3, 8}) {
            // V8: the window fills the gap while both sides are still folded and fades as the real corner piece comes in
            double at = ver == 3 ? 0.9 : 0.75;
            Scene s = settle(EarthView.create(ver), at * H, -at * H);
            assertTrue(s.patches.stream().anyMatch(p -> p.overlay && p.tx == 1 && p.tz == -1), "V" + ver + " window shows the diagonal tile");
            Scene mid = settle(EarthView.create(ver), 0.1 * H, 0.1 * H);
            assertFalse(mid.patches.stream().anyMatch(p -> p.overlay && p.kind == Patch.TERRAIN), "V" + ver + " no window in the middle");
        }
    }

    @Test
    void v9ShellHasNoGaps() {
        Scene s = settle(EarthView.create(9), 0.63 * H, -0.41 * H);
        assertEquals(25, s.patches.size());
        for (Patch p : s.patches) {
            for (Patch q : s.patches) {
                if (q.tx == p.tx + 1 && q.tz == p.tz) {
                    assertTrue(p.at(1, 0).distance(q.at(0, 0)) < 1e-9 && p.at(1, 1).distance(q.at(0, 1)) < 1e-9);
                }
                if (q.tz == p.tz + 1 && q.tx == p.tx) {
                    assertTrue(p.at(0, 1).distance(q.at(0, 0)) < 1e-9 && p.at(1, 1).distance(q.at(1, 0)) < 1e-9);
                }
            }
        }
    }

    @Test
    void v7CellsHaveWallsAndEdges() {
        Scene s = settle(EarthView.create(7), 0.2 * H, 0.2 * H);
        assertTrue(s.patches.stream().anyMatch(p -> p.kind == Patch.COLOR));
        assertTrue(s.lines.size() >= 12);
    }

    @Test
    void v10TipsOverTheEdgeWithoutAJumpAndEndsLevel() {
        for (int ver = 10; ver <= 12; ver++) {
            EarthView view = EarthView.create(ver);
            settle(view, 0.995 * H, 0.2 * H);
            double[][] anchors = {{1.3 * H, 0.3 * H}, {0.6 * H, 0.1 * H}};
            Scene before = view.step(0.999 * H, 0.2 * H, 1e-4, params());
            Scene at = view.step(1.001 * H, 0.2 * H, 1e-4, params());
            assertEquals("edge", at.event, "V" + ver);
            for (double[] a : anchors) {
                V3 p0 = relative(view, before, a[0], a[1]), p1 = relative(view, at, a[0], a[1]);
                if (p0 != null && p1 != null) assertTrue(p0.distance(p1) < 0.02, "V" + ver + " renamed picture jumps " + p0.distance(p1));
            }
            Scene end = null;
            for (int i = 0; i < 90; i++) end = view.step(1.001 * H, 0.2 * H, DT, params());
            assertTrue(end.orientation.angleTo(Quat.IDENTITY) < 1e-3, "V" + ver + " level again after the tip");
            assertTrue(end.contact.distance(V3.UP) < 1e-9, "V" + ver);
            // during the tip the cube turns about the point under the player
            Scene mid = view.step(1.001 * H, 0.2 * H, 1e-4, params());
            assertTrue(mid.finite());
        }
    }

    @Test
    void v10HasEdgeLinesV11CellsV12TurnsItsCornerUp() {
        Scene v10 = settle(EarthView.create(10), 0.2 * H, 0.1 * H);
        assertEquals(12, v10.lines.size(), "twelve cube edges");
        Scene v11 = settle(EarthView.create(11), 0.2 * H, 0.1 * H);
        assertEquals(6 + 6 * 9, v11.patches.size(), "6 cores + 3 x 3 pieces per face");
        assertEquals(6, v11.patches.stream().filter(p -> p.kind == Patch.COLOR).count());
        EarthView v12 = EarthView.create(12);
        ViewParams high = params().set("heightFraction", 1);
        Scene s = null;
        for (int i = 0; i < 600; i++) s = v12.step(0.4 * H, -0.3 * H, DT, high);
        V3 corner = s.orientation.rotate(new V3(1, 1, -1)).normalize();
        assertTrue(corner.distance(V3.UP) < 1e-3, "the corner of the player's quarter points up: " + corner);
        assertTrue(s.bodyOffset.add(s.orientation.rotate(new V3(1, 1, -1))).distance(V3.UP) < 1e-3, "and sits under the eye");
        Scene low = settle(EarthView.create(12), 0.4 * H, -0.3 * H);
        assertTrue(low.orientation.angleTo(Quat.IDENTITY) < 1e-6, "near the switch height the top stays level");
    }

    @Test
    void v3CornerWindowLiesOnTheRoundedBody() {
        // WE25-2: the window used to be a flat triangle across the sharp corner, standing off the body like a loose plate
        Scene s = settle(EarthView.create(3), 0.95 * H, -0.95 * H);
        Patch w = s.patches.stream().filter(p -> p.overlay && p.kind == Patch.TERRAIN).findFirst().orElseThrow();
        for (V3 p : w.pos) {
            V3 b = s.orientation.conjugate().rotate(p).mul(1 / 1.004);
            double r = 0.07, lim = 1 - r;
            V3 inner = new V3(Math.max(-lim, Math.min(lim, b.x())), Math.max(-lim, Math.min(lim, b.y())), Math.max(-lim, Math.min(lim, b.z())));
            double off = b.distance(inner), max = Math.max(Math.abs(b.x()), Math.max(Math.abs(b.y()), Math.abs(b.z())));
            // on the rounded cube: r away from the inner box, or on a flat face (one coordinate 1)
            assertTrue(Math.abs(off - r) < 1e-6 || Math.abs(max - 1) < 1e-6, "window vertex on the body: " + b + " off " + off);
        }
    }

    @Test
    void orbitVersionsKeepTheDistanceAndShowTheGroundBelow() {
        // E29-2: flying turns the earth; the eye (contact + (h - eyeDrop)) stays 1 + h from the centre, the ground under the
        // player straight below, also near edges and corners
        for (int ver : new int[]{13, 14, 15, 16, 17, 18, 19, 20}) {
            EarthView view = EarthView.create(ver);
            double x = 0, z = 0;
            for (int i = 0; i < 900; i++) {
                x += L * 0.004;
                z -= L * 0.0025;
                Scene s = view.step(x, z, DT, params());
                assertTrue(s.finite() && s.equiangular, "V" + ver);
                assertEquals(1.0, s.contact.y() - s.eyeDrop, 1e-9, "V" + ver + " eye distance from the centre");
                assertTrue(Math.hypot(s.contact.x(), s.contact.z()) < 1e-9);
                if (i % 50 == 0 && !s.fadeStart) {
                    double tu = (x - (view.tileOf(x) * L - H)) / L, tv = (z - (view.tileOf(z) * L - H)) / L;
                    V3 at = s.locate(0, 0, tu, tv);
                    if (at != null) assertTrue(Math.hypot(at.x(), at.z()) < 0.06, "V" + ver + " ground below the eye, frame " + i + ": " + at);
                }
            }
        }
    }

    @Test
    void equiangularFacesReachTheEdgeAtHalfAnEdge() {
        assertEquals(1.0, com.simplebuilding.modules.simpleearth.view.OrbitMath.angular(1.0), 1e-12);
        assertEquals(Math.tan(Math.PI / 8), com.simplebuilding.modules.simpleearth.view.OrbitMath.angular(0.5), 1e-12);
    }
}
