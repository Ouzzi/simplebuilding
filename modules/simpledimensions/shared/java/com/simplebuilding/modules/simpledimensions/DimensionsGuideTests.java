package com.simplebuilding.modules.simpledimensions;

import com.simplebuilding.modules.simpledimensions.guide.DimensionsGuide;
import com.simplebuilding.modules.simpledimensions.guide.DimensionsGuideAccess;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/** W5 / IG31-64 (docs/ai/PLAN-A-DIMSX11-2026-11-09.md): default dimensions, the easy-to-get guide and its content. */
public final class DimensionsGuideTests {
    private DimensionsGuideTests() {
    }

    /** Skyblock, Mining and Travel exist from the first start: definitions loaded, switches on by default, definition files written. */
    public static void defaultDimensions(GameTestHelper h) {
        var runtime = DimensionRuntime.get(h.getLevel().getServer());
        for (String id : List.of("skyblock", "mining", "travel")) {
            var config = runtime.config(id);
            h.assertTrue(config != null, "default dimension missing: " + id);
            h.assertTrue(config.targetDimensionId.equals("simpledimensions:" + id), "wrong target for " + id + ": " + config.targetDimensionId);
            h.assertTrue(Files.isRegularFile(DimensionRuntime.CONFIG_ROOT.resolve("dimensions").resolve(id + ".json")), id + ".json not written on first start");
            h.assertTrue(runtime.settings.allowsAccess(config) == runtime.settings.accessEnabled, id + " not switched on by default");
        }
        var fresh = new DimensionSettings();
        h.assertTrue(fresh.accessEnabled && fresh.skyblockEnabled && fresh.miningEnabled && fresh.travelEnabled && fresh.giveGuideOnFirstTravel,
                "a fresh settings object must have every default dimension and the guide gift on");
        h.succeed();
    }

    /** The guide is cheap: book + crafting table gives the Simple Modpack guide (SimpleLib's recipe). */
    public static void guideRecipe(GameTestHelper h) {
        var guide = BuiltInRegistries.ITEM.getOptional(DimensionsGuide.MODPACK_GUIDE).orElse(null);
        h.assertTrue(guide != null, "the Simple Modpack guide is missing - SimpleLib not bundled?");
        CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.BOOK), new ItemStack(Items.CRAFTING_TABLE)));
        ItemStack crafted = h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
        h.assertTrue(crafted.is(guide), "book + crafting table does not craft the guide");
        h.succeed();
    }

    /** The chapter covers the default dimensions, getting the guide, inventory/rights and the commands, in both languages. */
    public static void guideContent(GameTestHelper h) {
        for (String locale : List.of("en_us", "de_de")) {
            String lang = read("/assets/simpledimensions/lang/" + locale + ".json");
            h.assertTrue(lang != null, locale + " lang missing");
            var data = com.google.gson.JsonParser.parseString(lang).getAsJsonObject();
            StringBuilder all = new StringBuilder();
            for (int i = 1; i <= DimensionsGuide.PAGES; i++) {
                h.assertTrue(data.has("simpledimensions.guide.page." + i), locale + " page " + i + " missing");
                all.append(data.get("simpledimensions.guide.page." + i).getAsString()).append(' ');
            }
            String text = all.toString();
            for (String word : List.of("Skyblock", "Mining", "Travel", "/simpledimensions guide", "/simpledimensions config")) {
                h.assertTrue(text.contains(word), locale + " guide lacks: " + word);
            }
            h.assertTrue(data.has("text.autoconfig.simpledimensions.option.giveGuideOnFirstTravel") && data.has("text.autoconfig.simpledimensions.option.giveGuideOnFirstTravel.@Tooltip"),
                    locale + " lacks the first-trip option text");
            h.assertTrue(data.has("simpledimensions.guide.command.given") && data.has("simpledimensions.guide.command.missing"), locale + " lacks the command texts");
        }
        h.assertTrue(DimensionsGuide.PAGES >= 30, "the handbook has not grown: " + DimensionsGuide.PAGES);
        h.succeed();
    }

    /** /simpledimensions guide and the first journey: the guide is handed over (chapter open), the first journey only once. */
    public static void guideWelcome(GameTestHelper h) {
        var guide = BuiltInRegistries.ITEM.getOptional(DimensionsGuide.MODPACK_GUIDE).orElse(null);
        h.assertTrue(guide != null, "the Simple Modpack guide is missing");
        var player = h.makeMockServerPlayerInLevel();
        h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
        var server = h.getLevel().getServer();
        var chapter = server.getAdvancements().get(DimensionsGuide.CHAPTER);
        var welcome = server.getAdvancements().get(DimensionsGuideAccess.WELCOME);
        h.assertTrue(chapter != null && welcome != null, "chapter or welcome advancement missing");
        for (var holder : List.of(chapter, welcome)) for (String c : holder.value().criteria().keySet()) player.getAdvancements().revoke(holder, c);
        player.getInventory().clearContent();
        h.assertTrue(DimensionsGuideAccess.give(player), "the command could not hand out the guide");
        h.assertTrue(has(player, guide), "no guide after the command");
        h.assertTrue(player.getAdvancements().getOrStartProgress(chapter).isDone(), "the command did not open the chapter");
        player.getInventory().clearContent();
        h.assertTrue(DimensionsGuideAccess.welcome(player), "the first journey gave no guide");
        h.assertTrue(has(player, guide), "no guide after the first journey");
        player.getInventory().clearContent();
        h.assertTrue(!DimensionsGuideAccess.welcome(player), "the second journey gave the guide again");
        h.assertTrue(!has(player, guide), "a guide appeared on the second journey");
        h.succeed();
    }

    private static boolean has(net.minecraft.server.level.ServerPlayer player, net.minecraft.world.item.Item item) {
        for (int i = 0; i < player.getInventory().getContainerSize(); i++) if (player.getInventory().getItem(i).is(item)) return true;
        return false;
    }

    private static String read(String path) {
        try (var in = DimensionsGuideTests.class.getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            return null;
        }
    }
}
