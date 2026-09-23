package com.simplebuilding;

import com.google.gson.Gson;
import com.google.gson.JsonElement;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.StringReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LanguageFilesTest {

    private static final Path LANG_DIR = Path.of("src", "main", "resources", "assets", "simplebuilding", "lang");
    private static final Pattern KEY_PATTERN = Pattern.compile("^\\s*\\\"([^\\\"]+)\\\"\\s*:\\s*");

    @Test
    void enUsJsonIsStrictlyValid() throws IOException {
        assertStrictJsonObject(read("en_us.json"), "en_us.json");
    }

    @Test
    void deDeJsonIsStrictlyValid() throws IOException {
        assertStrictJsonObject(read("de_de.json"), "de_de.json");
    }

    @Test
    void enUsHasNoDuplicateKeys() throws IOException {
        assertNoDuplicateTopLevelKeys(read("en_us.json"), "en_us.json");
    }

    @Test
    void deDeHasNoDuplicateKeys() throws IOException {
        assertNoDuplicateTopLevelKeys(read("de_de.json"), "de_de.json");
    }

    @Test
    void requiredUiKeysExistInBothLangFiles() throws IOException {
        List<String> requiredKeys = List.of(
                "simplebuilding.gui.lock_tooltip",
                "key.simplebuilding.toggle_highlight",
                "key.simplebuilding.simple_settings"
        );

        Map<String, String> en = parseFlatStringMap(read("en_us.json"), "en_us.json");
        Map<String, String> de = parseFlatStringMap(read("de_de.json"), "de_de.json");

        for (String key : requiredKeys) {
            assertTrue(en.containsKey(key), "Missing key in en_us.json: " + key);
            assertTrue(de.containsKey(key), "Missing key in de_de.json: " + key);
            assertNotNull(en.get(key), "Null value in en_us.json for key: " + key);
            assertNotNull(de.get(key), "Null value in de_de.json for key: " + key);
        }
    }

    /**
     * Every key has its translation in both languages, on both Minecraft lines. The client tests run
     * in en_us only, so a key that is missing from de_de - as the three block item names once were
     * from both files - would show its bare key to a German player without any test noticing.
     */
    @Test
    void enUsAndDeDeCarryTheSameKeysOnBothLines() throws IOException {
        for (Path dir : List.of(LANG_DIR, Path.of("mc1_21_11", "fabric", "src", "main", "resources", "assets",
                "simplebuilding", "lang"))) {
            Map<String, String> en = parseFlatStringMap(Files.readString(dir.resolve("en_us.json"),
                    StandardCharsets.UTF_8), dir + "/en_us.json");
            Map<String, String> de = parseFlatStringMap(Files.readString(dir.resolve("de_de.json"),
                    StandardCharsets.UTF_8), dir + "/de_de.json");

            List<String> onlyEnglish = new ArrayList<>(en.keySet());
            onlyEnglish.removeAll(de.keySet());
            List<String> onlyGerman = new ArrayList<>(de.keySet());
            onlyGerman.removeAll(en.keySet());

            assertTrue(onlyEnglish.isEmpty(), dir + ": keys without a German entry: " + onlyEnglish);
            assertTrue(onlyGerman.isEmpty(), dir + ": keys without an English entry: " + onlyGerman);
        }
    }

    private static String read(String fileName) throws IOException {
        return Files.readString(LANG_DIR.resolve(fileName), StandardCharsets.UTF_8);
    }

    private static void assertStrictJsonObject(String json, String fileName) {
        JsonReader reader = new JsonReader(new StringReader(json));
        JsonElement element = JsonParser.parseReader(reader);
        assertTrue(element.isJsonObject(), fileName + " must contain a top-level JSON object.");
    }

    private static void assertNoDuplicateTopLevelKeys(String content, String fileName) {
        String[] lines = content.split("\\R");
        Map<String, Integer> counts = new HashMap<>();

        for (String line : lines) {
            Matcher matcher = KEY_PATTERN.matcher(line);
            if (!matcher.find()) {
                continue;
            }
            String key = matcher.group(1);
            counts.put(key, counts.getOrDefault(key, 0) + 1);
        }

        List<String> duplicates = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : counts.entrySet()) {
            if (entry.getValue() > 1) {
                duplicates.add(entry.getKey());
            }
        }

        assertFalse(!duplicates.isEmpty(), fileName + " has duplicate keys: " + duplicates);
    }

    private static Map<String, String> parseFlatStringMap(String json, String fileName) {
        JsonElement element = new Gson().fromJson(json, JsonElement.class);
        assertTrue(element.isJsonObject(), fileName + " must contain a top-level JSON object.");

        Map<String, String> values = new HashMap<>();
        element.getAsJsonObject().entrySet().forEach(entry -> {
            JsonElement value = entry.getValue();
            values.put(entry.getKey(), value.isJsonPrimitive() ? value.getAsString() : null);
        });
        return values;
    }
}
