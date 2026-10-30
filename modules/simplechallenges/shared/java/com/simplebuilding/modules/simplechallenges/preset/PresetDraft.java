package com.simplebuilding.modules.simplechallenges.preset;

import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import java.util.ArrayList;
import java.util.List;

/**
 * A preset while the player edits it on the selection screen: plain fields, turned into the preset file text with {@link #toJson}.
 * Only user presets are edited; the file the player saves is read again by the same {@link PresetParser} as every other preset.
 */
public final class PresetDraft {
    public String name = "My challenge";
    public String icon = "minecraft:grass_block";
    public String family = "unlock";
    /** Unlock mode (null = the preset has none). */
    public Integer cell;
    public List<int[]> start = new ArrayList<>();
    public Currency currency = Currency.ITEMS;
    public Difficulty difficulty = Difficulty.MEDIUM;
    public List<String> dimensions = new ArrayList<>(List.of("minecraft:overworld"));
    public int pityMinutes = 30;
    public int creditsTask, creditsGoal, creditsChallenge;
    /** World mode (null = none). */
    public WorldMode.Kind worldKind;
    public String style = "classic";
    public int spacing = 4;
    /** Lucky / Random Drops mode (null = none) and Skyline mode (null = none): kept as they are, the editor has no controls for them yet. */
    public LuckyMode lucky;
    public SkylineMode skyline;

    public static PresetDraft from(Preset p) {
        PresetDraft d = new PresetDraft();
        d.name = p.name();
        d.icon = p.icon();
        d.family = p.family();
        UnlockMode u = p.unlock();
        if (u != null) {
            d.cell = u.cell();
            d.start = new ArrayList<>();
            for (int[] r : u.start()) d.start.add(r.clone());
            d.currency = u.currency();
            d.difficulty = u.difficulty();
            d.dimensions = new ArrayList<>(u.dimensions());
            d.pityMinutes = u.pityMinutes();
            d.creditsTask = u.creditsTask();
            d.creditsGoal = u.creditsGoal();
            d.creditsChallenge = u.creditsChallenge();
        }
        WorldMode w = p.world();
        if (w != null) {
            d.worldKind = w.kind();
            d.style = w.style().isEmpty() ? "classic" : w.style();
            d.spacing = w.spacing();
        }
        d.lucky = p.lucky();
        d.skyline = p.skyline();
        return d;
    }

    /** A paid-with-credits preset needs credits from advancements: switching to credits fills the usual 1 / 2 / 5. */
    public void setCurrency(Currency c) {
        currency = c;
        if (c == Currency.CREDITS && creditsTask + creditsGoal + creditsChallenge == 0) {
            creditsTask = 1;
            creditsGoal = 2;
            creditsChallenge = 5;
        }
    }

    public String toJson() {
        JsonObject root = new JsonObject();
        root.addProperty("schemaVersion", PresetParser.SCHEMA);
        root.addProperty("name", name);
        root.addProperty("icon", icon);
        root.addProperty("family", family);
        JsonArray modes = new JsonArray();
        if (worldKind != null) {
            JsonObject w = new JsonObject();
            w.addProperty("type", worldKind.type);
            if (worldKind == WorldMode.Kind.ISLAND) w.addProperty("style", style);
            if (worldKind == WorldMode.Kind.SKYGRID) w.addProperty("spacing", spacing);
            modes.add(w);
        }
        if (lucky != null) {
            JsonObject l = new JsonObject();
            l.addProperty("type", lucky.kind().type);
            l.addProperty("alpha", lucky.alpha());
            l.addProperty("pity", lucky.pity());
            modes.add(l);
        }
        if (skyline != null) {
            JsonObject k = new JsonObject();
            k.addProperty("type", "skyline");
            k.addProperty("layout", skyline.layout().id);
            k.addProperty("easterEggs", skyline.easterEggs());
            modes.add(k);
        }
        if (cell != null) {
            JsonObject u = new JsonObject();
            u.addProperty("type", "unlock");
            u.addProperty("cell", cell);
            JsonArray rects = new JsonArray();
            for (int[] r : start) {
                JsonArray a = new JsonArray();
                for (int v : r) a.add(v);
                rects.add(a);
            }
            u.add("start", rects);
            u.addProperty("currency", currency.id());
            u.addProperty("difficulty", difficulty.id());
            JsonArray dims = new JsonArray();
            for (String dim : dimensions) dims.add(dim);
            u.add("dimensions", dims);
            u.addProperty("pityMinutes", pityMinutes);
            if (creditsTask + creditsGoal + creditsChallenge > 0) {
                JsonObject c = new JsonObject();
                c.addProperty("task", creditsTask);
                c.addProperty("goal", creditsGoal);
                c.addProperty("challenge", creditsChallenge);
                u.add("credits", c);
            }
            modes.add(u);
        }
        root.add("modes", modes);
        return new GsonBuilder().setPrettyPrinting().create().toJson(root);
    }
}
