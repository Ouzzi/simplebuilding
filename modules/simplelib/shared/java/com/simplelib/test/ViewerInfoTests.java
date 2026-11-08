package com.simplelib.test;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplelib.api.RecipeViewer;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;

/**
 * JEI24 (a-fix09b): a module's announced info pages ({@link RecipeViewer#info}) are there, each shows items, and every
 * translatable line - also the arguments - has its text in the module's en_us and de_de. Called by the modules' catalogues.
 */
public final class ViewerInfoTests {
    private ViewerInfoTests() {
    }

    public static void infoPages(GameTestHelper h, String owner, int minPages) {
        List<RecipeViewer.Info> pages = RecipeViewer.info(owner);
        h.assertTrue(pages.size() >= minPages, owner + ": " + pages.size() + " info pages, expected at least " + minPages);
        List<String> problems = new ArrayList<>();
        for (String language : List.of("en_us", "de_de")) {
            JsonObject lang = lang(owner, language);
            if (lang == null) {
                problems.add("no " + language + " of " + owner);
                continue;
            }
            for (RecipeViewer.Info page : pages) {
                if (page.items().isEmpty()) problems.add("page without items: " + page.lines());
                for (Component line : page.lines()) keys(line, lang, language, problems);
            }
        }
        h.assertTrue(problems.isEmpty(), owner + " info page problems: " + problems);
        h.succeed();
    }

    private static void keys(Component line, JsonObject lang, String language, List<String> problems) {
        if (line.getContents() instanceof TranslatableContents t) {
            String key = t.getKey();
            boolean own = key.startsWith("jei.") || key.startsWith("jade.") || key.startsWith("tooltip.");
            if (own && (!lang.has(key) || lang.get(key).getAsString().isBlank())) problems.add(key + " missing in " + language);
            for (Object arg : t.getArgs()) {
                if (arg instanceof Component c) keys(c, lang, language, problems);
            }
        }
        for (Component sibling : line.getSiblings()) keys(sibling, lang, language, problems);
    }

    private static JsonObject lang(String owner, String language) {
        String path = "assets/" + owner + "/lang/" + language + ".json";
        try (InputStream in = ViewerInfoTests.class.getClassLoader().getResourceAsStream(path)) {
            if (in == null) return null;
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (Exception e) {
            return null;
        }
    }
}
