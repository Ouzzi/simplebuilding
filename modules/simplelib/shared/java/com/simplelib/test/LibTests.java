package com.simplelib.test;

import com.google.gson.JsonObject;
import com.simplelib.config.LibConfig;
import com.simplelib.registry.LibComponents;
import com.simplelib.warm.Warm;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.LiquidBlock;
import net.minecraft.world.level.block.state.BlockState;

/**
 * GameTests of SimpleLib (warm food, config, UI, hopper limits; the id moves, in-world upgrades and materials have their own
 * classes). One body per id; the Fabric adapter ({@code ModuleGameTest}) and the NeoForge/Forge registrations use the same
 * names ({@code module_game_test_<name>}). The crucible tests moved to Simple Crucibles (split stage 5).
 */
public final class LibTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    static {
        ALL.put("config_bounds", LibTests::configBounds);
        ALL.put("broken_config_is_quarantined", LibTests::brokenConfigIsQuarantined);
        ALL.put("warm_stacks_by_mean", LibTests::warmStacksByMean);
        ALL.put("warm_bundle_insulates", LibTests::warmBundleInsulates);
        ALL.put("hoppers_fill_raised_slots", LibTests::hoppersFillRaisedSlots);
        ALL.put("inventory_brightness_tones_palettes", LibTests::inventoryBrightness);
        ALL.put("ui_text_contrast", LibTests::uiTextContrast);
        ALL.put("shift_tooltip_collapse", LibTests::shiftTooltipCollapse);
        ALL.put("id_moves_registries", IdMovesTests::registries);
        ALL.put("id_moves_recipe_book_and_advancements", IdMovesTests::recipeBookAndAdvancements);
        ALL.put("id_moves_refuse_loops", IdMovesTests::refuseLoops);
        ALL.put("id_moves_adopt_config", IdMovesTests::adoptConfig);
        ALL.put("in_world_upgrades_data_type", InWorldUpgradeTests::dataType);
        ALL.put("in_world_upgrade_handlers", InWorldUpgradeTests::handlers);
        ALL.put("octet_cells", InWorldUpgradeTests::octetCells);
        ALL.put("material_requests_registered", MaterialRequestTests::registered);
        ALL.put("material_requests_recipes", MaterialRequestTests::recipesFollowRequests);
        ALL.put("material_requests_anvil_crushing", MaterialRequestTests::anvilCrushingFollowsRequest);
        ALL.put("pixel_palette_lab_nearest", PixelArtTests::labNearest);
        ALL.put("pixel_dither_deterministic", PixelArtTests::ditherDeterministic);
        ALL.put("pixel_rle_round_trip", PixelArtTests::rleRoundTrip);
        ALL.put("pixel_size_limit", PixelArtTests::sizeLimit);
        ALL.put("simple_rarity_names", LibTests::simpleRarityNames);
        ALL.put("legendary_advancements", LegendaryTests::advancements);
        ALL.put("legendary_enchantments", LegendaryTests::enchantments);
    }

    // ------------------------------------------------------------ helpers

    private static void check(GameTestHelper h, boolean ok, String message) {
        h.assertTrue(ok, message);
    }

    // ------------------------------------------------------------ tests


    /**
     * N67 readability rule: text needs 4.5:1 to its background. Every standard palette (and the derived ones of the mod
     * screens) keeps that for its label in all three inventory brightnesses and for the bookmark numbers; the shared
     * inks read on the inventory grey and on paper; the fixer leaves good colours alone and nudges bad ones.
     */
    private static void uiTextContrast(GameTestHelper h) {
        var brightness = com.simplelib.api.client.ui.UiBrightness.current();
        try {
            java.util.Map<String, com.simplelib.api.client.ui.UiPalette> palettes = new LinkedHashMap<>();
            palettes.put("INVENTORY", com.simplelib.api.client.ui.UiPalette.INVENTORY);
            palettes.put("BARREL", com.simplelib.api.client.ui.UiPalette.BARREL);
            palettes.put("IRON", com.simplelib.api.client.ui.UiPalette.IRON);
            palettes.put("REINFORCED", com.simplelib.api.client.ui.UiPalette.REINFORCED);
            palettes.put("NETHERITE", com.simplelib.api.client.ui.UiPalette.NETHERITE);
            palettes.put("ENDERITE", com.simplelib.api.client.ui.UiPalette.ENDERITE);
            // derived palettes of the mod screens: leather backpack, auto smither, crafter, fletching, astral table, crafting table,
            // the paper of deed / table, the Wayfinder's brown, the sales-block copper
            int[] fills = {0xFF8E6440, 0xFF4F5560, 0xFF5E5651, 0xFFC5B485, 0xFF4B3866, 0xFFA27A4A, 0xFFE2D3AA, 0xFFB08A57, 0xFFB9774F};
            for (int fill : fills) {
                palettes.put("derived " + Integer.toHexString(fill), com.simplelib.api.client.ui.UiPalette.derived(fill));
            }
            for (var brightnessMode : com.simplelib.api.client.ui.UiBrightness.values()) {
                com.simplelib.api.client.ui.UiBrightness.set(brightnessMode);
                for (var entry : palettes.entrySet()) {
                    var palette = entry.getValue();
                    double label = com.simplelib.api.client.ui.UiContrast.ratio(palette.label(), palette.fill());
                    check(h, label >= com.simplelib.api.client.ui.UiContrast.MIN,
                            entry.getKey() + " label in " + brightnessMode + " has contrast " + label);
                    for (boolean filled : new boolean[] {true, false}) {
                        for (boolean hovered : new boolean[] {true, false}) {
                            double ribbon = com.simplelib.api.client.ui.UiContrast.ratio(palette.ribbonLabel(filled, hovered), palette.ribbonFill(filled, hovered));
                            check(h, ribbon >= com.simplelib.api.client.ui.UiContrast.MIN,
                                    entry.getKey() + " ribbon number (" + filled + "," + hovered + ") in " + brightnessMode + " has contrast " + ribbon);
                        }
                    }
                }
            }
            com.simplelib.api.client.ui.UiBrightness.set(com.simplelib.api.client.ui.UiBrightness.LIGHT);
        } finally {
            com.simplelib.api.client.ui.UiBrightness.set(brightness);
        }
        int grey = com.simplelib.api.client.ui.UiPalette.INVENTORY.fill(), paper = 0xFFE2D3AA;
        for (int ink : new int[] {com.simplelib.api.client.ui.UiContrast.INK_OK, com.simplelib.api.client.ui.UiContrast.INK_ERROR,
                com.simplelib.api.client.ui.UiContrast.INK_SOFT, 0xFF404040 /* Vanilla label / dialog text */,
                com.simplelib.api.client.ui.UiPalette.DARK_LABEL}) {
            check(h, com.simplelib.api.client.ui.UiContrast.readable(ink, grey) && com.simplelib.api.client.ui.UiContrast.readable(ink, paper),
                    "ink " + Integer.toHexString(ink) + " does not read on grey and paper");
        }
        // the dark light-brightness text colours are unchanged where they were already fine
        check(h, com.simplelib.api.client.ui.UiContrast.ensure(0xFF404040, grey) == 0xFF404040, "a readable colour was changed");
        // the former white own-entry text of the land table was 1.7:1 on the grey: the fixer must turn it dark
        int fixed = com.simplelib.api.client.ui.UiContrast.ensure(0xFFFFFFFF, grey);
        check(h, com.simplelib.api.client.ui.UiContrast.readable(fixed, grey), "white on grey not fixed: " + Integer.toHexString(fixed));
        check(h, com.simplelib.api.client.ui.UiContrast.ratio(0xFF000000, 0xFFFFFFFF) > 20.9
                && com.simplelib.api.client.ui.UiContrast.ratio(0xFF808080, 0xFF808080) == 1.0, "ratio maths");
        h.succeed();
    }

    /**
     * "Hold Shift for more" (owner 2026-10-21, {@code UiShiftTooltip}): without Shift only the first lines and the hint, with
     * Shift every line; a tooltip with nothing to hide gets no hint; the hint names its key in both languages and reads on
     * the tooltip background (4.5:1).
     */
    private static void shiftTooltipCollapse(GameTestHelper h) {
        java.util.List<net.minecraft.network.chat.Component> full = java.util.List.of(net.minecraft.network.chat.Component.literal("Resonance"),
                net.minecraft.network.chat.Component.literal("(E + S + C) / 3"), net.minecraft.network.chat.Component.empty(),
                net.minecraft.network.chat.Component.literal("Experience"));
        var brief = com.simplelib.api.client.ui.UiShiftTooltip.collapse(full, 1, false);
        check(h, brief.size() == 2 && brief.get(0) == full.get(0), "without Shift: first line plus hint, got " + brief.size() + " lines");
        check(h, brief.get(1).getContents() instanceof net.minecraft.network.chat.contents.TranslatableContents t
                && t.getKey().equals(com.simplelib.api.client.ui.UiShiftTooltip.KEY), "second line is not the hold-Shift hint: " + brief.get(1));
        var gray = net.minecraft.network.chat.TextColor.fromLegacyFormat(net.minecraft.ChatFormatting.GRAY);
        check(h, gray != null && gray.equals(brief.get(1).getStyle().getColor()), "hint is not the shared grey");
        check(h, com.simplelib.api.client.ui.UiShiftTooltip.collapse(full, 1, true).equals(full), "with Shift not every line");
        var two = com.simplelib.api.client.ui.UiShiftTooltip.collapse(full, 2, false);
        check(h, two.size() == 3 && two.get(1) == full.get(1), "brief 2 keeps two lines");
        check(h, com.simplelib.api.client.ui.UiShiftTooltip.collapse(full.subList(0, 1), 1, false).size() == 1, "nothing hidden, still a hint");
        int grey = 0xFF000000 | gray.getValue();
        double ratio = com.simplelib.api.client.ui.UiContrast.ratio(grey, com.simplelib.api.client.ui.UiShiftTooltip.TOOLTIP_BACKGROUND);
        check(h, ratio >= com.simplelib.api.client.ui.UiContrast.MIN, "hint contrast " + ratio);
        for (String lang : new String[] {"en_us", "de_de"}) {
            try (var in = LibTests.class.getResourceAsStream("/assets/simplelib/lang/" + lang + ".json")) {
                check(h, in != null, lang + " missing");
                JsonObject json = com.google.gson.JsonParser.parseReader(new java.io.InputStreamReader(in, java.nio.charset.StandardCharsets.UTF_8)).getAsJsonObject();
                check(h, json.has(com.simplelib.api.client.ui.UiShiftTooltip.KEY), lang + " has no " + com.simplelib.api.client.ui.UiShiftTooltip.KEY);
            } catch (java.io.IOException e) {
                throw new AssertionError(e);
            }
        }
        h.succeed();
    }

    /**
     * N49: the inventory brightness tones every palette colour (light = unchanged, dawn between, dark darkest), turns
     * the labels light on dark boxes, keeps the stored colours, and survives a save and load of its file.
     */
    private static void inventoryBrightness(GameTestHelper h) {
        com.simplelib.api.client.ui.UiBrightness before = com.simplelib.api.client.ui.UiBrightness.current();
        com.simplelib.api.client.ui.UiPalette inv = com.simplelib.api.client.ui.UiPalette.INVENTORY;
        try {
            com.simplelib.api.client.ui.UiBrightness.set(com.simplelib.api.client.ui.UiBrightness.LIGHT);
            check(h, inv.fill() == 0xFFC6C6C6 && inv.label() == 0xFF404040, "light changed the Vanilla inventory colours");
            double light = com.simplelib.api.client.ui.UiPalette.luminance(inv.fill());
            com.simplelib.api.client.ui.UiBrightness.set(com.simplelib.api.client.ui.UiBrightness.DAWN);
            double dawn = com.simplelib.api.client.ui.UiPalette.luminance(inv.fill());
            com.simplelib.api.client.ui.UiBrightness.set(com.simplelib.api.client.ui.UiBrightness.DARK);
            double dark = com.simplelib.api.client.ui.UiPalette.luminance(inv.fill());
            check(h, light > dawn && dawn > dark, "brightness order wrong: " + light + " / " + dawn + " / " + dark);
            check(h, dark < 100, "dark is not dark: " + dark);
            check(h, inv.label() == com.simplelib.api.client.ui.UiPalette.LIGHT_LABEL, "dark box keeps a dark label");
            check(h, com.simplelib.api.client.ui.UiPalette.luminance(inv.slot()) < com.simplelib.api.client.ui.UiPalette.luminance(inv.fill()),
                    "slots no longer sunk below the box");
            check(h, inv.equals(new com.simplelib.api.client.ui.UiPalette(0xFFC6C6C6, 0xFFFFFFFF, 0xFF555555, 0xFF8B8B8B, 0xFF373737, 0xFF404040)),
                    "the stored palette colours changed");
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("simplelib-brightness");
            com.simplelib.api.client.ui.UiBrightness.load(dir);
            check(h, com.simplelib.api.client.ui.UiBrightness.current() == com.simplelib.api.client.ui.UiBrightness.LIGHT, "missing file is not light");
            com.simplelib.api.client.ui.UiBrightness.set(com.simplelib.api.client.ui.UiBrightness.DAWN);
            com.simplelib.api.client.ui.UiBrightness.save();
            com.simplelib.api.client.ui.UiBrightness.set(com.simplelib.api.client.ui.UiBrightness.LIGHT);
            com.simplelib.api.client.ui.UiBrightness.load(dir);
            check(h, com.simplelib.api.client.ui.UiBrightness.current() == com.simplelib.api.client.ui.UiBrightness.DAWN, "dawn not saved and loaded");
            check(h, com.simplelib.api.client.ui.UiBrightness.parse("DARK") == com.simplelib.api.client.ui.UiBrightness.DARK
                    && com.simplelib.api.client.ui.UiBrightness.parse("nonsense") == com.simplelib.api.client.ui.UiBrightness.LIGHT, "parse");
            h.succeed();
        } catch (java.io.IOException e) {
            throw new RuntimeException(e);
        } finally {
            com.simplelib.api.client.ui.UiBrightness.set(before);
        }
    }

    /** Every warm option is clamped to its hard bounds (the crucible options moved to Simple Crucibles). */
    private static void configBounds(GameTestHelper h) {
        JsonObject json = new JsonObject();
        json.addProperty("warmDurationTicks", 5);
        json.addProperty("eatSpeedBonus", 0.9);
        json.addProperty("warmBaseTicks", 1_000_000);
        LibConfig.apply(json);
        check(h, LibConfig.warmDurationTicks == LibConfig.WARM_DURATION_MIN, "warm duration not clamped");
        check(h, LibConfig.eatSpeedBonus == LibConfig.EAT_BONUS_MAX, "eat bonus not clamped");
        check(h, LibConfig.warmBaseTicks == LibConfig.WARM_BASE_MAX, "warm base not clamped");
        LibConfig.reset();
        check(h, LibConfig.warmBaseTicks == LibConfig.WARM_BASE_DEFAULT && LibConfig.eatSpeedBonus == LibConfig.EAT_BONUS_DEFAULT, "defaults wrong");
        h.succeed();
    }

    /** Shared policy (framework ConfigFiles): a broken server config never crashes, it is moved to *.broken-<time> and defaults are written; keys SimpleLib does not own survive a rewrite (the crucible options before split stage 5). */
    private static void brokenConfigIsQuarantined(GameTestHelper h) {
        try {
            java.nio.file.Path dir = java.nio.file.Files.createTempDirectory("simplelib-broken");
            java.nio.file.Path file = dir.resolve(LibConfig.FILE);
            java.nio.file.Files.writeString(file, "{ \"warmBaseTicks\": 3,, }");
            LibConfig.load(dir);
            check(h, LibConfig.warmBaseTicks == LibConfig.WARM_BASE_DEFAULT, "defaults not used after a broken file");
            try (var files = java.nio.file.Files.list(dir)) {
                var names = files.map(x -> x.getFileName().toString()).toList();
                check(h, names.stream().anyMatch(n -> n.startsWith(LibConfig.FILE + ".broken-")), "broken file not kept: " + names);
            }
            com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(file));
            check(h, com.simplebuilding.framework.api.ConfigFiles.readOrQuarantine(dir.resolve("none.json"), t -> t, x -> { }) == null, "missing file must give null");
            // Unknown keys (the crucible options of the old file) are written back unchanged.
            java.nio.file.Files.writeString(file, "{ \"reinforcedSpeed\": 3, \"warmBaseTicks\": 200 }");
            LibConfig.load(dir);
            var rewritten = com.google.gson.JsonParser.parseString(java.nio.file.Files.readString(file)).getAsJsonObject();
            check(h, rewritten.has("reinforcedSpeed") && rewritten.get("reinforcedSpeed").getAsInt() == 3 && rewritten.get("warmBaseTicks").getAsInt() == 200,
                    "foreign keys lost: " + rewritten);
        } catch (java.io.IOException | RuntimeException e) {
            h.fail("broken config handling threw " + e);
            return;
        }
        LibConfig.reset();
        h.succeed();
    }
















    /** What breaking {@code state} at {@code rel} drops, sorted: "cauldron 1, iron_ingot 7". */
    private static String dropsOf(GameTestHelper h, BlockState state, BlockPos rel) {
        java.util.Map<String, Integer> out = new java.util.TreeMap<>();
        for (ItemStack stack : net.minecraft.world.level.block.Block.getDrops(state, h.getLevel(), h.absolutePos(rel), null)) {
            out.merge(net.minecraft.core.registries.BuiltInRegistries.ITEM.getKey(stack.getItem()).getPath(), stack.getCount(), Integer::sum);
        }
        StringBuilder line = new StringBuilder();
        out.forEach((k, v) -> line.append(line.isEmpty() ? "" : ", ").append(k).append(' ').append(v));
        return line.toString();
    }



    private static void warmStacksByMean(GameTestHelper h) {
        long now = h.getLevel().getGameTime();
        com.simplelib.warm.WarmMerge.tick(now);
        ItemStack warm = new ItemStack(Items.COOKED_BEEF, 2);
        warm.set(LibComponents.WARM, new Warm(now + 1000));
        ItemStack cold = new ItemStack(Items.COOKED_BEEF, 2);
        check(h, ItemStack.isSameItemSameComponents(warm, cold), "warm and cold food stack (owner: mean warmth)");
        warm.grow(cold.getCount());
        check(h, Warm.remaining(warm, now) == 500, "mean of 2 x 1000 and 2 x 0 ticks is 500 (owner 40: cold counts 0), got " + Warm.remaining(warm, now));
        ItemStack other = new ItemStack(Items.COOKED_PORKCHOP);
        check(h, !ItemStack.isSameItemSameComponents(warm, other), "different food never stacks");
        h.succeed();
    }

    private static void warmBundleInsulates(GameTestHelper h) {
        long now = h.getLevel().getGameTime();
        com.simplelib.warm.WarmMerge.tick(now);
        ItemStack warm = new ItemStack(Items.BAKED_POTATO, 3);
        warm.set(LibComponents.WARM, new Warm(now + 1000));
        net.minecraft.world.item.component.BundleContents.Mutable bundle =
                new net.minecraft.world.item.component.BundleContents.Mutable();
        check(h, bundle.tryInsert(warm) == 3 && warm.isEmpty(), "inserted");
        ItemStack out = bundle.removeOne();
        check(h, out.getCount() == 3 && Warm.remaining(out, now) == 1000 && !out.get(LibComponents.WARM).insulated(),
                "back out with its remaining time and normal cooling");
        ItemStack probe = new ItemStack(Items.BAKED_POTATO);
        probe.set(LibComponents.WARM, new Warm(now + 1000));
        Warm.insulate(probe, now);
        check(h, Warm.remaining(probe, now) == 4000, "inside a bundle it cools four times slower (12 000 -> 48 000 ticks)");
        h.succeed();
    }




    /**
     * Owner N15: hoppers top raised slots up to their container's limit - alone and as a double chest - while a
     * Vanilla chest keeps Vanilla's 64.
     */
    private static void hoppersFillRaisedSlots(GameTestHelper h) {
        var raised = com.simplelib.api.StackLimits.mirror(9, () -> 2);
        raised.setItem(0, new ItemStack(Items.COBBLESTONE, 127));
        ItemStack rest = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, raised, new ItemStack(Items.COBBLESTONE, 2), Direction.UP);
        check(h, raised.getItem(0).getCount() == 128 && raised.getItem(1).getCount() == 1 && rest.isEmpty(),
                "a x2 slot at 127 plus 2: " + raised.getItem(0).getCount() + "+" + raised.getItem(1).getCount() + " rest " + rest.getCount());
        var first = com.simplelib.api.StackLimits.mirror(1, () -> 2);
        var second = com.simplelib.api.StackLimits.mirror(1, () -> 2);
        first.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        second.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        var both = new net.minecraft.world.CompoundContainer(first, second);
        rest = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, both, new ItemStack(Items.COBBLESTONE, 1), Direction.UP);
        check(h, rest.isEmpty() && first.getItem(0).getCount() == 65, "a double x2 container of 64s is not full, got " + first.getItem(0).getCount());
        var vanilla = new net.minecraft.world.SimpleContainer(1);
        vanilla.setItem(0, new ItemStack(Items.COBBLESTONE, 64));
        rest = net.minecraft.world.level.block.entity.HopperBlockEntity.addItem(null, vanilla, new ItemStack(Items.COBBLESTONE, 1), Direction.UP);
        check(h, rest.getCount() == 1 && vanilla.getItem(0).getCount() == 64, "a Vanilla container stays at 64");
        h.succeed();
    }








    private LibTests() {}

    // Discoverability stage 1 (generated bodies, tools/discover/discover.py)
    static {
        ALL.put("discover_recipe_unlocks", com.simplelib.discover.LibDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplelib.discover.LibDiscover::behaviourHints);
        ALL.put("discover_first_pickup_tips", com.simplelib.discover.LibDiscover::firstPickupTips);
        WarpedWartTests.register();
    }

    /**
     * Owner 21.10.: tiers above Epic. The component sits on the stack (Vanilla rarity stays Epic), the name carries the
     * tier style as the child of every Vanilla wrapper (tooltip, chat item), the codec round-trips and the colours read
     * 4.5:1 on the dark tooltip background. Server thread: the mythic name stays one translatable text.
     */
    private static void simpleRarityNames(GameTestHelper h) {
        var cursed = new ItemStack(Items.STICK);
        cursed.set(LibComponents.RARITY, com.simplelib.api.SimpleRarity.CURSED);
        check(h, com.simplelib.api.SimpleRarity.of(cursed) == com.simplelib.api.SimpleRarity.CURSED, "component set");
        var name = cursed.getHoverName();
        check(h, name.getStyle().getColor() != null && name.getStyle().getColor().getValue() == 0xFF3030, "cursed red: " + name.getStyle());
        check(h, Integer.valueOf(0xFF000000).equals(name.getStyle().getShadowColor()), "cursed black shadow");
        var styled = cursed.getStyledHoverName();
        check(h, styled.getSiblings().size() == 1 && styled.getSiblings().get(0).getStyle().getColor().getValue() == 0xFF3030,
                "tooltip name keeps the tier colour as child: " + styled);
        boolean[] red = {false};
        cursed.getDisplayName().visit((style, text) -> {
            if (text.contains("Stick") && style.getColor() != null && style.getColor().getValue() == 0xFF3030) red[0] = true;
            return java.util.Optional.empty();
        }, net.minecraft.network.chat.Style.EMPTY);
        check(h, red[0], "chat item [name] red");

        var mythic = new ItemStack(Items.STICK);
        mythic.set(LibComponents.RARITY, com.simplelib.api.SimpleRarity.MYTHIC);
        var mName = mythic.getHoverName();
        check(h, mName.getStyle().getColor().getValue() == 0x5CF2DE && mName.getSiblings().get(0).getContents()
                instanceof net.minecraft.network.chat.contents.TranslatableContents, "mythic on the server: translatable, aquamarine");
        check(h, !com.simplelib.api.SimpleRarity.onClient(), "game tests run off the render thread");

        // owner 21.10. (b-rarity21b): tooltip letters, marked for the row-by-row overlay of the client
        for (var r : com.simplelib.api.SimpleRarity.values()) {
            var letters = r.letters("Mythic", 1234L, true);
            check(h, letters.getSiblings().size() == 6 && letters.getString().equals("Mythic"), r + ": letter by letter");
            check(h, Integer.valueOf(r.shadow()).equals(letters.getStyle().getShadowColor()), r + ": tier shadow");
            for (var letter : letters.getSiblings()) {
                check(h, com.simplelib.api.SimpleRarity.byMarker(letter.getStyle().getInsertion()) == r, r + ": marker " + letter.getStyle());
            }
            check(h, r.letters("Mythic", 0L, false).getSiblings().get(0).getStyle().getInsertion() == null, r + ": hotbar letters unmarked");
            check(h, r.pixel(2, 6, 3, 5000L) == r.pixel(2, 6, 3, 5000L), r + ": colours are a function of the time");
        }
        check(h, com.simplelib.api.SimpleRarity.byMarker("simplelib:rarity/cursedx") == null
                && com.simplelib.api.SimpleRarity.byMarker(null) == null, "only exact markers");

        // cursed: the letters burn from below (bottom row hotter than the top for every letter and time), the fire moves
        var fire = com.simplelib.api.SimpleRarity.CURSED;
        boolean moves = false;
        int hotter = 0, samples = 0;
        for (long t = 0; t < 20000L; t += 137L) {
            for (int i = 0; i < 12; i++, samples++) {
                double top = com.simplelib.api.client.ui.UiContrast.luminance(fire.pixel(i, 12, 0, t));
                double bottom = com.simplelib.api.client.ui.UiContrast.luminance(fire.pixel(i, 12, 7, t));
                check(h, bottom >= top, "fire: never cooler at the bottom, letter " + i + " t " + t);
                if (bottom > top) hotter++;
                if (fire.pixel(i, 12, 4, t) != fire.pixel(i, 12, 4, t + 400L)) moves = true;
            }
        }
        check(h, hotter * 10 >= samples * 9, "fire: bottom hotter than top in 90 % of the samples: " + hotter + "/" + samples);
        check(h, moves, "fire is animated");
        check(h, fire.pixel(0, 1, -3, 0L) == fire.pixel(0, 1, 0, 0L) && fire.pixel(0, 1, 9, 0L) == fire.pixel(0, 1, 7, 0L),
                "rows outside a letter count as its top/bottom row");
        var opal = com.simplelib.api.SimpleRarity.MYTHIC;
        check(h, opal.pixel(3, 8, 4, 0L) != opal.pixel(3, 8, 4, 1300L), "opal moves");
        check(h, com.simplelib.api.client.ui.UiContrast.luminance(opal.pixel(3, 8, 4, 3400L * 7 + 400L)) > 0.8
                && com.simplelib.api.client.ui.UiContrast.luminance(opal.pixel(3, 8, 4, 3400L * 7 + 2000L)) < 0.75, "a white sheen sweeps across");

        for (var r : com.simplelib.api.SimpleRarity.values()) {
            var json = com.simplelib.api.SimpleRarity.CODEC.encodeStart(com.mojang.serialization.JsonOps.INSTANCE, r).getOrThrow();
            check(h, com.simplelib.api.SimpleRarity.CODEC.parse(com.mojang.serialization.JsonOps.INSTANCE, json).getOrThrow() == r, "codec " + r);
        }
        check(h, net.minecraft.core.registries.BuiltInRegistries.DATA_COMPONENT_TYPE.getKey(LibComponents.RARITY)
                .equals(com.simplelib.SimpleLib.id("rarity")), "registered as simplelib:rarity");
        // readability (rule A7): every colour a letter pixel can take reads 4.5:1 on the dark tooltip background
        int tooltipBg = 0xFF100010;
        double worst = 99;
        for (var r : com.simplelib.api.SimpleRarity.values()) {
            worst = Math.min(worst, com.simplelib.api.client.ui.UiContrast.ratio(0xFF000000 | r.color(), tooltipBg));
            for (long t = 0; t < 7000L; t += 53L) {
                for (int row = 0; row < com.simplelib.api.SimpleRarity.ROWS; row++) {
                    for (int i = 0; i < 14; i++) worst = Math.min(worst, com.simplelib.api.client.ui.UiContrast.ratio(0xFF000000 | r.pixel(i, 14, row, t), tooltipBg));
                }
            }
        }
        check(h, worst >= com.simplelib.api.client.ui.UiContrast.MIN, "contrast of every letter pixel: worst " + worst);
        h.succeed();
    }
}
