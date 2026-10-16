package com.simplebuilding.gametest;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.function.UnaryOperator;

/**
 * Faces of block-model JSON (cuboid elements) and the check that no two of them fight about a plane (Item display 3D, owner
 * 2026-10-15: "kein Z-Fighting, keine koplanaren Flaechen"). Two faces conflict when they lie in one plane (within
 * {@link #PLANE_TOLERANCE} px) and share an area of more than {@link #MIN_AREA} px squared, no matter which way they look.
 * Plain Java (JSON in, numbers out), used by the game tests (piles, stacks) and a JUnit test (the shipped models).
 */
public final class ModelFaces {
    /** Planes closer than this (px) count as one. */
    public static final double PLANE_TOLERANCE = 0.01;
    /** Overlap smaller than this (px squared) is an edge touch, not a shared area. */
    public static final double MIN_AREA = 1.0E-4;

    private ModelFaces() {
    }

    /** One rectangular face: four corners (px), a label for messages. */
    public record Face(String label, double[][] corners) {
    }

    /** Reads {@code /assets/<ns>/<path>} from the class path, or null. */
    public static JsonObject read(String namespace, String path) {
        try (InputStream in = ModelFaces.class.getResourceAsStream("/assets/" + namespace + "/" + path)) {
            return in == null ? null : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (IOException e) {
            throw new IllegalStateException(path, e);
        }
    }

    /** The cuboid elements of {@code model} ({@code ns:block/name}), found along the parent chain; null when there is no such model. */
    public static JsonArray elements(String model) {
        String current = model;
        for (int depth = 0; depth < 6 && current != null && !current.startsWith("minecraft:block/block"); depth++) {
            int colon = current.indexOf(':');
            JsonObject json = read(colon < 0 ? "minecraft" : current.substring(0, colon), "models/" + current.substring(colon + 1) + ".json");
            if (json == null) {
                return null;
            }
            if (json.has("elements")) {
                return json.getAsJsonArray("elements");
            }
            current = json.has("parent") ? json.get("parent").getAsString() : null;
        }
        return null;
    }

    /** All faces of the elements, in model px. Rotated elements are not supported (they throw, the shipped models have none). */
    public static List<Face> faces(JsonArray elements, String label) {
        List<Face> out = new ArrayList<>();
        int index = 0;
        for (JsonElement e : elements) {
            JsonObject element = e.getAsJsonObject();
            if (element.has("rotation")) {
                throw new IllegalStateException(label + " element " + index + " is rotated");
            }
            double[] a = array(element.getAsJsonArray("from"));
            double[] b = array(element.getAsJsonArray("to"));
            for (String side : element.getAsJsonObject("faces").keySet()) {
                double[][] c = switch (side) {
                    case "up" -> new double[][]{{a[0], b[1], a[2]}, {b[0], b[1], a[2]}, {b[0], b[1], b[2]}, {a[0], b[1], b[2]}};
                    case "down" -> new double[][]{{a[0], a[1], a[2]}, {b[0], a[1], a[2]}, {b[0], a[1], b[2]}, {a[0], a[1], b[2]}};
                    case "north" -> new double[][]{{a[0], a[1], a[2]}, {b[0], a[1], a[2]}, {b[0], b[1], a[2]}, {a[0], b[1], a[2]}};
                    case "south" -> new double[][]{{a[0], a[1], b[2]}, {b[0], a[1], b[2]}, {b[0], b[1], b[2]}, {a[0], b[1], b[2]}};
                    case "west" -> new double[][]{{a[0], a[1], a[2]}, {a[0], b[1], a[2]}, {a[0], b[1], b[2]}, {a[0], a[1], b[2]}};
                    case "east" -> new double[][]{{b[0], a[1], a[2]}, {b[0], b[1], a[2]}, {b[0], b[1], b[2]}, {b[0], a[1], b[2]}};
                    default -> throw new IllegalStateException(label + ": unknown face " + side);
                };
                out.add(new Face(label + "#" + index + "." + side, c));
            }
            index++;
        }
        return out;
    }

    private static double[] array(JsonArray a) {
        return new double[]{a.get(0).getAsDouble(), a.get(1).getAsDouble(), a.get(2).getAsDouble()};
    }

    /** The faces with every corner mapped through {@code f}. */
    public static List<Face> map(List<Face> faces, UnaryOperator<double[]> f) {
        List<Face> out = new ArrayList<>();
        for (Face face : faces) {
            double[][] c = new double[4][];
            for (int i = 0; i < 4; i++) {
                c[i] = f.apply(face.corners()[i]);
            }
            out.add(new Face(face.label(), c));
        }
        return out;
    }

    /** Messages for every pair (a from {@code a}, b from {@code b}; with {@code same} each pair once) that fights about a plane. */
    public static List<String> conflicts(List<Face> a, List<Face> b, boolean same) {
        List<String> out = new ArrayList<>();
        for (int i = 0; i < a.size(); i++) {
            Face fa = a.get(i);
            double[] n = normal(fa);
            for (int j = same ? i + 1 : 0; j < b.size(); j++) {
                Face fb = b.get(j);
                double[] m = normal(fb);
                if (Math.abs(dot(n, m)) < 1.0 - 1.0E-6 || Math.abs(dot(n, sub(fb.corners()[0], fa.corners()[0]))) > PLANE_TOLERANCE) {
                    continue;
                }
                double area = overlap(fa, fb, n);
                if (area > MIN_AREA) {
                    out.add(fa.label() + " and " + fb.label() + " share a plane over " + area + " px2");
                }
            }
        }
        return out;
    }

    private static double[] normal(Face f) {
        double[] u = sub(f.corners()[1], f.corners()[0]);
        double[] v = sub(f.corners()[3], f.corners()[0]);
        double[] n = {u[1] * v[2] - u[2] * v[1], u[2] * v[0] - u[0] * v[2], u[0] * v[1] - u[1] * v[0]};
        double len = Math.sqrt(dot(n, n));
        return len == 0.0 ? n : new double[]{n[0] / len, n[1] / len, n[2] / len};
    }

    private static double[] sub(double[] a, double[] b) {
        return new double[]{a[0] - b[0], a[1] - b[1], a[2] - b[2]};
    }

    private static double dot(double[] a, double[] b) {
        return a[0] * b[0] + a[1] * b[1] + a[2] * b[2];
    }

    /** Area of the overlap of two coplanar rectangles, in the plane basis of {@code fa}. */
    private static double overlap(Face fa, Face fb, double[] n) {
        double[] u = sub(fa.corners()[1], fa.corners()[0]);
        double ul = Math.sqrt(dot(u, u));
        u = new double[]{u[0] / ul, u[1] / ul, u[2] / ul};
        double[] v = {n[1] * u[2] - n[2] * u[1], n[2] * u[0] - n[0] * u[2], n[0] * u[1] - n[1] * u[0]};
        List<double[]> subject = project(fb, u, v, fa.corners()[0]);
        List<double[]> clip = project(fa, u, v, fa.corners()[0]);
        if (area(clip) < 0.0) {
            java.util.Collections.reverse(clip);
        }
        if (area(subject) < 0.0) {
            java.util.Collections.reverse(subject);
        }
        for (int i = 0; i < clip.size() && !subject.isEmpty(); i++) {
            double[] p = clip.get(i);
            double[] q = clip.get((i + 1) % clip.size());
            List<double[]> input = subject;
            subject = new ArrayList<>();
            for (int k = 0; k < input.size(); k++) {
                double[] s = input.get(k);
                double[] e = input.get((k + 1) % input.size());
                boolean sIn = side(p, q, s) >= 0.0;
                boolean eIn = side(p, q, e) >= 0.0;
                if (sIn != eIn) {
                    double ds = side(p, q, s);
                    double de = side(p, q, e);
                    double t = ds / (ds - de);
                    subject.add(new double[]{s[0] + (e[0] - s[0]) * t, s[1] + (e[1] - s[1]) * t});
                }
                if (eIn) {
                    subject.add(e);
                }
            }
        }
        return subject.size() < 3 ? 0.0 : Math.abs(area(subject));
    }

    private static List<double[]> project(Face f, double[] u, double[] v, double[] origin) {
        List<double[]> out = new ArrayList<>();
        for (double[] c : f.corners()) {
            double[] d = sub(c, origin);
            out.add(new double[]{dot(d, u), dot(d, v)});
        }
        return out;
    }

    private static double side(double[] p, double[] q, double[] s) {
        return (q[0] - p[0]) * (s[1] - p[1]) - (q[1] - p[1]) * (s[0] - p[0]);
    }

    private static double area(List<double[]> poly) {
        double sum = 0.0;
        for (int i = 0; i < poly.size(); i++) {
            double[] a = poly.get(i);
            double[] b = poly.get((i + 1) % poly.size());
            sum += a[0] * b[1] - b[0] * a[1];
        }
        return sum / 2.0;
    }
}
