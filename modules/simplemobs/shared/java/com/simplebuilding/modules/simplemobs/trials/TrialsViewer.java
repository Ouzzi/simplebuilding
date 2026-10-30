package com.simplebuilding.modules.simplemobs.trials;

import com.simplebuilding.modules.simplemobs.EndNetherMobs;
import com.simplebuilding.modules.simplemobs.MobsConfig;
import com.simplebuilding.modules.simplemobs.WitherMobs;
import com.simplebuilding.modules.simplemobs.WitherWings;
import com.simplelib.api.RecipeViewer;
import com.simplelib.compat.jei.InWorldUpgrade;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * The recipe viewer side of the trial chain (owner: JEI pages are a duty): the soul brewing recipes (Evil Potion I-V; Void Potion I-V while the
 * Void chain is on), and the info pages for the flow of the Evil Omen and the Void chain, converting a vault, and the Weathered Trials.
 * The vault conversion itself is a {@code simplelib:in_world_upgrades} entry and shows in SimpleLib's In-World Upgrades page.
 */
public final class TrialsViewer {
    public static final String OWNER = "simplemobs";
    public static final String KEY = "jei.simplemobs.trials.";

    private TrialsViewer() {}

    public static List<InWorldUpgrade> special() {
        List<InWorldUpgrade> out = new ArrayList<>();
        for (int stage = 1; stage <= Trials.STAGES; stage++) {
            out.add(brew(TrialKind.EVIL, stage, WitherMobs.HEART));
            if (Trials.voidOn()) out.add(brew(TrialKind.VOID, stage, WitherWings.ENDER_HEART));
        }
        return out;
    }

    private static InWorldUpgrade brew(TrialKind kind, int stage, Item heart) {
        Item potion = (kind == TrialKind.VOID ? Trials.VOID_POTIONS : Trials.EVIL_POTIONS)[stage - 1];
        return new InWorldUpgrade(OWNER + "/soul_brewing/" + kind.id() + "_" + stage,
                List.of(InWorldUpgrade.Input.of(Items.OMINOUS_BOTTLE, 1), InWorldUpgrade.Input.of(heart, 1), InWorldUpgrade.Input.of(EndNetherMobs.POWDER, 1)),
                List.of(Items.BREWING_STAND), new ItemStack(potion),
                List.of(Component.translatable(KEY + "brew_note", stage), Component.translatable(KEY + "brew_fuel")));
    }

    public static List<RecipeViewer.Info> infoPages() {
        List<RecipeViewer.Info> out = new ArrayList<>();
        out.add(page("weathered", new ItemStack(Trials.WEATHERED_TRIAL_SPAWNER_ITEM), new ItemStack(Trials.WEATHERED_VAULT_ITEM), new ItemStack(Items.TRIAL_KEY), new ItemStack(Items.OMINOUS_TRIAL_KEY)));
        out.add(page("evil_flow", new ItemStack(Trials.EVIL_POTIONS[0]), new ItemStack(Trials.EVIL_KEYS[0]), new ItemStack(Trials.EVIL_VAULT_ITEM), new ItemStack(WitherMobs.HEART)));
        out.add(page("void_flow", new ItemStack(Trials.VOID_POTIONS[0]), new ItemStack(Trials.VOID_KEYS[0]), new ItemStack(Trials.VOID_VAULT_ITEM), new ItemStack(WitherWings.ENDER_HEART)));
        out.add(page("convert", new ItemStack(Trials.WEATHERED_VAULT_ITEM), new ItemStack(Trials.EVIL_VAULT_ITEM), new ItemStack(Trials.VOID_VAULT_ITEM),
                new ItemStack(Items.DIAMOND_BLOCK), new ItemStack(Items.NETHERITE_BLOCK)));
        out.addAll(lootPages());
        return out;
    }

    // --- vault loot with weights (TR31-R): read from assets/simplemobs/loot_info/trials.json, written by the structure generator from the same rows as the loot tables
    private static volatile JsonObject lootInfo;

    private static JsonObject lootInfo() {
        if (lootInfo == null) {
            try (var in = TrialsViewer.class.getResourceAsStream("/assets/simplemobs/loot_info/trials.json")) {
                lootInfo = in == null ? new JsonObject() : JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
            } catch (Exception e) {
                lootInfo = new JsonObject();
            }
        }
        return lootInfo;
    }

    /** One info page per vault table (Weathered, ominous, Evil I-V, Void I-V while the chain is on): the rolls and every entry with its weight and share. */
    public static List<RecipeViewer.Info> lootPages() {
        List<RecipeViewer.Info> out = new ArrayList<>();
        JsonObject info = lootInfo();
        for (String name : info.keySet()) {
            boolean isVoid = name.startsWith("void_");
            if (isVoid && !Trials.voidOn()) continue;
            if (name.startsWith("evil_") && !Trials.evilOn()) continue;
            JsonObject table = info.getAsJsonObject(name);
            JsonArray rows = table.getAsJsonArray("rows");
            int total = 0;
            for (JsonElement r : rows) total += r.getAsJsonObject().get("weight").getAsInt();
            List<Component> lines = new ArrayList<>();
            lines.add(Component.translatable(KEY + "loot.head", table.get("rolls").getAsInt()));
            for (JsonElement r : rows) {
                JsonObject row = r.getAsJsonObject();
                lines.add(Component.translatable(KEY + "loot.row", new ItemStack(TrialsExtras.item(row.get("item").getAsString())).getHoverName(), count(row),
                        String.format(Locale.ROOT, "%.0f %%", 100.0 * row.get("weight").getAsInt() / total), row.get("weight").getAsInt()));
            }
            lines.add(Component.translatable(KEY + "loot.tail" + (MobsConfig.trialLootNoRepeat && !name.startsWith("weathered") ? ".distinct" : "")));
            ItemStack vault = new ItemStack(name.startsWith("evil") ? Trials.EVIL_VAULT_ITEM : isVoid ? Trials.VOID_VAULT_ITEM : Trials.WEATHERED_VAULT_ITEM);
            ItemStack key = name.startsWith("evil") ? new ItemStack(Trials.EVIL_KEYS[stageOf(name) - 1]) : isVoid ? new ItemStack(Trials.VOID_KEYS[stageOf(name) - 1])
                    : new ItemStack(name.equals("weathered_ominous") ? Items.OMINOUS_TRIAL_KEY : Items.TRIAL_KEY);
            out.add(new RecipeViewer.Info(List.of(vault, key), lines));
        }
        return out;
    }

    private static int stageOf(String table) {
        return Integer.parseInt(table.substring(table.lastIndexOf('_') + 1));
    }

    private static String count(JsonObject row) {
        if (row.get("book").getAsBoolean()) return "";
        int lo = row.get("min").getAsInt(), hi = row.get("max").getAsInt();
        return lo == hi ? "x" + lo : "x" + lo + "-" + hi;
    }

    private static RecipeViewer.Info page(String name, ItemStack... items) {
        List<Component> lines = new ArrayList<>();
        for (int i = 1; i <= 4; i++) lines.add(Component.translatable(KEY + name + "." + i));
        return new RecipeViewer.Info(List.of(items), lines);
    }
}
