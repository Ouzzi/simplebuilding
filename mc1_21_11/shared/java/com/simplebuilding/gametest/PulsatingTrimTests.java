package com.simplebuilding.gametest;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.GlowingTrimUtils;
import com.simplebuilding.util.SledgehammerCrafting;
import com.simplebuilding.util.TrimUpgrades;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import net.minecraft.core.NonNullList;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.Container;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.inventory.SmithingMenu;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.CraftingRecipe;
import net.minecraft.world.item.crafting.RecipeHolder;
import net.minecraft.world.item.crafting.RecipeType;
import net.minecraft.world.level.GameType;

/**
 * Pulsating Armor Trim und die Vorlagen-Namen (Besitzer 2026-09-28): die Vorlage entsteht an der
 * Werkbank aus Echoscherbe + Vorschlaghammer, der Hammer bleibt beschaedigt im Raster; am Schmiedetisch
 * macht sie mit einer Echoscherbe den Besatz pulsierend (einmalig, neben Glowing und Emitting); die
 * Vorlagen heissen wie Vanillas "Netherite Upgrade" / "Host Armor Trim" ohne "Smithing".
 */
public final class PulsatingTrimTests {

    private PulsatingTrimTests() {
    }

    /**
     * Echoscherbe + Vorschlaghammer (jede Stufe, beide Reihenfolgen) ergibt die Vorlage. Der Hammer
     * bleibt als Rest im Raster und verliert {@link SledgehammerCrafting#CRAFT_DAMAGE} Haltbarkeit; ein
     * Hammer kurz vor dem Bruch bricht und hinterlaesst nichts. Die Echoscherbe wird verbraucht, und
     * andere formlose Rezepte behalten ihre Vanilla-Reste (Eimer beim Kuchen).
     */
    public static void thePulsatingTemplateIsCraftedFromAnEchoShardAndAnySledgehammerThatStays(GameTestHelper helper) {
        List<String> problems = new ArrayList<>();
        for (Item hammerItem : List.of(ModItems.STONE_SLEDGEHAMMER, ModItems.IRON_SLEDGEHAMMER, ModItems.ENDERITE_SLEDGEHAMMER)) {
            for (boolean hammerFirst : new boolean[]{true, false}) {
                ItemStack hammer = new ItemStack(hammerItem);
                hammer.setDamageValue(3);
                List<ItemStack> grid = hammerFirst ? List.of(hammer, new ItemStack(Items.ECHO_SHARD))
                        : List.of(new ItemStack(Items.ECHO_SHARD), hammer);
                CraftingInput input = CraftingInput.of(2, 1, grid);
                Optional<RecipeHolder<CraftingRecipe>> match = find(helper, input);
                if (match.isEmpty()) {
                    problems.add(grid + " crafts nothing");
                    continue;
                }
                ItemStack out = assemble(helper, match.get().value(), input);
                if (!out.is(ModItems.PULSATING_TRIM_TEMPLATE) || out.getCount() != 1) {
                    problems.add(grid + " crafts " + out + " instead of one pulsating trim template");
                }
                NonNullList<ItemStack> rest = match.get().value().getRemainingItems(input);
                int hammerSlot = hammerFirst ? 0 : 1;
                ItemStack left = rest.get(hammerSlot);
                if (!left.is(hammerItem) || left.getDamageValue() != 3 + SledgehammerCrafting.CRAFT_DAMAGE) {
                    problems.add(grid + ": the hammer came back as " + left + " (damage " + left.getDamageValue()
                            + ") instead of the same hammer with damage " + (3 + SledgehammerCrafting.CRAFT_DAMAGE));
                }
                if (!rest.get(1 - hammerSlot).isEmpty()) {
                    problems.add(grid + ": the echo shard left " + rest.get(1 - hammerSlot));
                }
            }
        }
        // Ein Hammer kurz vor dem Bruch bricht beim Herstellen.
        ItemStack worn = new ItemStack(ModItems.IRON_SLEDGEHAMMER);
        worn.setDamageValue(worn.getMaxDamage() - SledgehammerCrafting.CRAFT_DAMAGE);
        CraftingInput wornInput = CraftingInput.of(2, 1, List.of(worn, new ItemStack(Items.ECHO_SHARD)));
        Optional<RecipeHolder<CraftingRecipe>> wornMatch = find(helper, wornInput);
        if (wornMatch.isEmpty()) {
            problems.add("a worn hammer + echo shard crafts nothing");
        } else if (!wornMatch.get().value().getRemainingItems(wornInput).get(0).isEmpty()) {
            problems.add("a hammer on its last point of durability survived crafting: "
                    + wornMatch.get().value().getRemainingItems(wornInput).get(0));
        }
        // Echoscherbe allein ist kein Rezept.
        if (find(helper, CraftingInput.of(1, 1, List.of(new ItemStack(Items.ECHO_SHARD)))).isPresent()) {
            problems.add("an echo shard alone crafts something");
        }
        // Vanilla-Reste anderer formloser Rezepte bleiben (Kuchen: drei Eimer zurueck).
        CraftingInput cake = CraftingInput.of(3, 3, List.of(
                new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.MILK_BUCKET), new ItemStack(Items.MILK_BUCKET),
                new ItemStack(Items.SUGAR), new ItemStack(Items.EGG), new ItemStack(Items.SUGAR),
                new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT), new ItemStack(Items.WHEAT)));
        Optional<RecipeHolder<CraftingRecipe>> cakeMatch = find(helper, cake);
        if (cakeMatch.isPresent()) {
            long buckets = cakeMatch.get().value().getRemainingItems(cake).stream().filter(s -> s.is(Items.BUCKET)).count();
            if (buckets != 3) {
                problems.add("the cake recipe left " + buckets + " buckets instead of 3");
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " crafting problems: " + problems);
        TestCleanup.succeed(helper);
    }

    /**
     * Am echten Schmiedetisch: Pulsating-Vorlage + Ruestung + Echoscherbe setzt die Komponente
     * {@code simplebuilding:pulsating} auf einer Kopie (Basis unveraendert, Glowing/Emitting bleiben),
     * ein zweites Mal gibt es kein Ergebnis. Falsches Material, gekreuzte Vorlagen und Nicht-Ruestung
     * tun nichts. Mit Glowing kombiniert pulsiert der Besatz leuchtend - beide Komponenten liegen dann
     * auf demselben Teil. Die Helligkeit laeuft zwischen voll und schwarz.
     */
    public static void thePulsatingUpgradeMakesTheTrimPulseOnceAndCombinesWithGlowing(GameTestHelper helper) {
        SmithingMenu table = new SmithingMenu(1, menuPlayer(helper).getInventory());
        ItemStack chest = new ItemStack(ModItems.ENDERITE_CHESTPLATE);
        GlowingTrimUtils.setGlowLevel(chest, 1);
        ItemStack pulsing = smith(table, ModItems.PULSATING_TRIM_TEMPLATE, chest, Items.ECHO_SHARD);
        helper.assertTrue(pulsing.is(ModItems.ENDERITE_CHESTPLATE) && pulsing.getCount() == 1,
                "the pulsating upgrade produced " + pulsing + " instead of the chestplate");
        helper.assertTrue(GlowingTrimUtils.isPulsating(pulsing) && Boolean.TRUE.equals(pulsing.get(ModDataComponentTypes.PULSATING)),
                "the smithed chestplate does not pulse: " + pulsing.getComponents());
        helper.assertTrue(GlowingTrimUtils.getGlowLevel(pulsing) == 1,
                "the pulsating upgrade changed the glow level to " + GlowingTrimUtils.getGlowLevel(pulsing));
        helper.assertTrue(!GlowingTrimUtils.isPulsating(chest), "the chestplate in the base slot was changed in place");

        ItemStack again = smith(table, ModItems.PULSATING_TRIM_TEMPLATE, pulsing.copy(), Items.ECHO_SHARD);
        helper.assertTrue(again.isEmpty(), "a pulsating chestplate was upgraded a second time: " + again);

        // Glowing auf ein pulsierendes Teil geht weiter (die Wirkungen sind unabhaengig).
        ItemStack plain = smith(table, ModItems.PULSATING_TRIM_TEMPLATE, new ItemStack(Items.DIAMOND_HELMET), Items.ECHO_SHARD);
        ItemStack glowing = smith(table, ModItems.GLOWING_TRIM_TEMPLATE, plain.copy(), Items.GLOW_INK_SAC);
        helper.assertTrue(GlowingTrimUtils.isPulsating(glowing) && GlowingTrimUtils.getGlowLevel(glowing) == 1,
                "glowing on a pulsating helmet lost one of the two: " + glowing.getComponents());

        // Nicht unsere Kombination: kein Ergebnis bzw. keine Wirkung.
        ItemStack wrongMaterial = smith(table, ModItems.PULSATING_TRIM_TEMPLATE, new ItemStack(Items.DIAMOND_BOOTS), Items.GLOW_INK_SAC);
        helper.assertTrue(!GlowingTrimUtils.isPulsating(wrongMaterial), "the pulsating template worked with a glow ink sac");
        ItemStack crossed = smith(table, ModItems.GLOWING_TRIM_TEMPLATE, new ItemStack(Items.DIAMOND_BOOTS), Items.ECHO_SHARD);
        helper.assertTrue(!GlowingTrimUtils.isPulsating(crossed) && !crossed.is(Items.DIAMOND_BOOTS),
                "the glowing template + echo shard produced " + crossed);
        ItemStack stick = smith(table, ModItems.PULSATING_TRIM_TEMPLATE, new ItemStack(Items.STICK), Items.ECHO_SHARD);
        helper.assertTrue(stick.isEmpty(), "a stick was made pulsating: " + stick);
        // Die Kreuzung, die das alte Platzhalter-Rezept mit dem ganzen Vorlagen-Tag zuliess: Glowing-Vorlage
        // + Glowstone ergab eine Emitting-Vorlage und kostete die Ruestung.
        ItemStack oldCross = smith(table, ModItems.GLOWING_TRIM_TEMPLATE, new ItemStack(Items.DIAMOND_BOOTS), Items.GLOWSTONE_DUST);
        helper.assertTrue(oldCross.isEmpty(), "glowing template + boots + glowstone still produces " + oldCross);

        // Die Slots nehmen Vorlage und Echoscherbe an (Platzhalter-Rezept).
        helper.assertTrue(table.getSlot(SmithingMenu.TEMPLATE_SLOT).mayPlace(new ItemStack(ModItems.PULSATING_TRIM_TEMPLATE)),
                "the template slot refuses the pulsating template");
        helper.assertTrue(table.getSlot(SmithingMenu.ADDITIONAL_SLOT).mayPlace(new ItemStack(Items.ECHO_SHARD)),
                "the addition slot refuses an echo shard");

        // TrimUpgrades kennt die drei Aufwertungen in Anzeige-Reihenfolge (JEI).
        helper.assertTrue(TrimUpgrades.ALL.size() == 3 && TrimUpgrades.ALL.get(2).template() == ModItems.PULSATING_TRIM_TEMPLATE
                        && TrimUpgrades.ALL.get(2).material() == Items.ECHO_SHARD,
                "TrimUpgrades.ALL is " + TrimUpgrades.ALL);

        // Der Puls: voll -> schwarz -> voll innerhalb einer Periode.
        long period = GlowingTrimUtils.PULSE_PERIOD_MS;
        helper.assertTrue(Math.abs(GlowingTrimUtils.pulseBrightness(0) - 1.0F) < 1.0E-4
                        && GlowingTrimUtils.pulseBrightness(period / 2) < 1.0E-4
                        && Math.abs(GlowingTrimUtils.pulseBrightness(period) - 1.0F) < 1.0E-4,
                "the pulse does not run full -> black -> full");
        helper.assertTrue(GlowingTrimUtils.pulseTint(1.0F) == 0xFFFFFFFF && GlowingTrimUtils.pulseTint(0.0F) == 0xFF000000,
                "the pulse tint is not white at full and black at zero");
        TestCleanup.succeed(helper);
    }

    /**
     * Die Vorlagen heissen wie Vanillas Gegenstuecke ohne "Smithing" (en_us und de_de), der Magnet heisst
     * Attractor/Attraktor (Id bleibt {@code magnet}), und kein Sprachtext nennt mehr die alten Namen.
     */
    public static void templatesAndTheAttractorCarryTheirNewNames(GameTestHelper helper) {
        JsonObject en = lang(helper, "en_us");
        JsonObject de = lang(helper, "de_de");
        Map<Item, String[]> names = Map.of(
                ModItems.GLOWING_TRIM_TEMPLATE, new String[]{"Glowing Armor Trim", "Leuchtender Rüstungsbesatz"},
                ModItems.EMITTING_TRIM_TEMPLATE, new String[]{"Emitting Armor Trim", "Strahlender Rüstungsbesatz"},
                ModItems.PULSATING_TRIM_TEMPLATE, new String[]{"Pulsating Armor Trim", "Pulsierender Rüstungsbesatz"},
                ModItems.BASIC_UPGRADE_TEMPLATE, new String[]{"Basic Upgrade", "Basis-Aufwertung"},
                ModItems.ENDERITE_UPGRADE_TEMPLATE, new String[]{"Enderite Upgrade", "Enderit-Aufwertung"},
                ModItems.MAGNET, new String[]{"Attractor", "Attraktor"});
        List<String> problems = new ArrayList<>();
        names.forEach((item, pair) -> {
            String key = item.getDescriptionId();
            String english = en.has(key) ? en.get(key).getAsString() : null;
            String german = de.has(key) ? de.get(key).getAsString() : null;
            if (!pair[0].equals(english)) {
                problems.add(key + " is '" + english + "' in en_us instead of '" + pair[0] + "'");
            }
            if (!pair[1].equals(german)) {
                problems.add(key + " is '" + german + "' in de_de instead of '" + pair[1] + "'");
            }
        });
        helper.assertTrue(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(ModItems.MAGNET).getPath().equals("magnet"),
                "the attractor's id changed; it must stay simplebuilding:magnet");
        for (String key : en.keySet()) {
            String text = en.get(key).getAsString();
            for (String old : List.of("Trim Smithing Template", "Upgrade Smithing Template", "Magnet ", "Magnet:", "a Magnet")) {
                if (text.contains(old)) {
                    problems.add("en_us " + key + " still says '" + old.trim() + "': " + text);
                }
            }
        }
        for (String key : de.keySet()) {
            String text = de.get(key).getAsString();
            for (String old : List.of("Leuchtende Schmiedevorlage", "Strahlende Schmiedevorlage", "Basis-Schmiedevorlage",
                    "Enderit-Schmiedevorlage", "Magnet ", "Magneten")) {
                if (text.contains(old)) {
                    problems.add("de_de " + key + " still says '" + old.trim() + "': " + text);
                }
            }
        }
        helper.assertTrue(problems.isEmpty(), problems.size() + " name problems: " + problems);
        TestCleanup.succeed(helper);
    }

    // =====================================================================================

    private static Optional<RecipeHolder<CraftingRecipe>> find(GameTestHelper helper, CraftingInput input) {
        return helper.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, helper.getLevel());
    }

    private static ItemStack assemble(GameTestHelper helper, CraftingRecipe recipe, CraftingInput input) {
        return recipe.assemble(input, helper.getLevel().registryAccess());
    }

    /**
     * Ein Spieler fuer das Menue, nicht in der Welt. MC 1.21.11 hat kein
     * {@code makeMockServerPlayer(GameType)}; gebaut wie {@code DynamicLightTests#detachedPlayer}.
     */
    private static Player menuPlayer(GameTestHelper helper) {
        ServerPlayer player = new ServerPlayer(helper.getLevel().getServer(), helper.getLevel(),
                new com.mojang.authlib.GameProfile(java.util.UUID.randomUUID(), "test-mock-player"),
                net.minecraft.server.level.ClientInformation.createDefault()) {
            @Override
            public GameType gameMode() {
                return GameType.SURVIVAL;
            }
        };
        GameType.SURVIVAL.updatePlayerAbilities(player.getAbilities());
        return player;
    }

    private static ItemStack smith(SmithingMenu table, Item template, ItemStack base, Item material) {
        Container inputs = table.getSlot(SmithingMenu.TEMPLATE_SLOT).container;
        table.getSlot(SmithingMenu.TEMPLATE_SLOT).set(new ItemStack(template));
        table.getSlot(SmithingMenu.BASE_SLOT).set(base);
        table.getSlot(SmithingMenu.ADDITIONAL_SLOT).set(new ItemStack(material));
        table.slotsChanged(inputs);
        return table.getSlot(table.getResultSlot()).getItem().copy();
    }

    private static JsonObject lang(GameTestHelper helper, String locale) {
        String path = "assets/simplebuilding/lang/" + locale + ".json";
        try (InputStream in = PulsatingTrimTests.class.getClassLoader().getResourceAsStream(path)) {
            helper.assertTrue(in != null, path + " is not on the classpath");
            return JsonParser.parseReader(new InputStreamReader(in, StandardCharsets.UTF_8)).getAsJsonObject();
        } catch (java.io.IOException e) {
            throw new IllegalStateException("cannot read " + path, e);
        }
    }
}
