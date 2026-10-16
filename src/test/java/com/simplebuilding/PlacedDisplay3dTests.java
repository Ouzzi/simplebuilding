package com.simplebuilding;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.gametest.ModelFaces;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Item display 3D (owner 2026-10-15: "kein Z-Fighting, keine koplanaren Flaechen"): in every shipped parent model of the
 * placed templates, sherds and discs (SimpleBuilding and Simple Trims) no two faces share a plane over an area, and no
 * element pokes into another one. Piles and stacks are checked in game ({@code PlacedDisplay3dTests} game tests).
 */
class PlacedDisplay3dTests {
    private static final Path SB = Path.of("mc26_3", "overlay", "resources", "assets", "simplebuilding", "models", "block");
    private static final Path ST = Path.of("modules", "simpletrims", "shared", "resources", "assets", "simpletrims", "models", "block");

    private static List<Path> parents() throws IOException {
        List<Path> out = new ArrayList<>();
        for (Path dir : List.of(SB, ST)) {
            try (Stream<Path> s = Files.list(dir)) {
                s.filter(p -> p.getFileName().toString().matches("placed_(slab_.*|disc_round)\\.json")).sorted().forEach(out::add);
            }
        }
        return out;
    }

    private static JsonArray elements(Path p) throws IOException {
        JsonObject json = JsonParser.parseString(Files.readString(p, StandardCharsets.UTF_8)).getAsJsonObject();
        return json.getAsJsonArray("elements");
    }

    @Test
    void parentModelsExistForBothMods() throws IOException {
        List<Path> parents = parents();
        assertTrue(parents.stream().anyMatch(p -> p.startsWith(SB) && p.getFileName().toString().equals("placed_disc_round.json")));
        assertTrue(parents.stream().filter(p -> p.startsWith(SB)).count() >= 5, "SimpleBuilding silhouettes: " + parents);
        assertTrue(parents.stream().filter(p -> p.startsWith(ST)).count() >= 3, "Simple Trims silhouettes: " + parents);
    }

    @Test
    void noTwoFacesShareAPlane() throws IOException {
        List<String> all = new ArrayList<>();
        for (Path p : parents()) {
            all.addAll(ModelFaces.conflicts(ModelFaces.faces(elements(p), p.getFileName().toString()),
                    ModelFaces.faces(elements(p), p.getFileName().toString()), true));
        }
        assertEquals(List.of(), all);
    }

    @Test
    void noElementPokesIntoAnother() throws IOException {
        for (Path p : parents()) {
            JsonArray e = elements(p);
            for (int i = 0; i < e.size(); i++) {
                for (int j = i + 1; j < e.size(); j++) {
                    double[][] a = box(e.get(i).getAsJsonObject());
                    double[][] b = box(e.get(j).getAsJsonObject());
                    boolean overlap = true;
                    for (int axis = 0; axis < 3; axis++) {
                        overlap &= Math.min(a[1][axis], b[1][axis]) - Math.max(a[0][axis], b[0][axis]) > 1.0E-6;
                    }
                    assertTrue(!overlap, p.getFileName() + ": elements " + i + " and " + j + " overlap");
                }
            }
        }
    }

    @Test
    void thicknessAndDiscHeight() throws IOException {
        for (Path p : parents()) {
            JsonArray e = elements(p);
            double lo = Double.MAX_VALUE;
            double hi = -Double.MAX_VALUE;
            boolean disc = p.getFileName().toString().contains("disc");
            for (int i = 0; i < e.size(); i++) {
                double[][] b = box(e.get(i).getAsJsonObject());
                int axis = disc ? 1 : 2;
                lo = Math.min(lo, b[0][axis]);
                hi = Math.max(hi, b[1][axis]);
            }
            assertEquals(disc ? 1.0 : 16.0 / 7.0, hi - lo, 1.0E-3, p.getFileName().toString());
        }
    }

    private static double[][] box(JsonObject e) {
        JsonArray f = e.getAsJsonArray("from");
        JsonArray t = e.getAsJsonArray("to");
        return new double[][]{{f.get(0).getAsDouble(), f.get(1).getAsDouble(), f.get(2).getAsDouble()},
                {t.get(0).getAsDouble(), t.get(1).getAsDouble(), t.get(2).getAsDouble()}};
    }
}
