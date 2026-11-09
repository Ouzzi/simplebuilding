package com.simplelib.test;

import com.google.gson.JsonParser;
import com.simplelib.api.ModpackGuide;
import com.simplelib.registry.LibItems;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.chat.Component;
import net.minecraft.network.chat.contents.TranslatableContents;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.CreativeModeTab;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.TooltipFlag;
import net.minecraft.world.item.component.TooltipDisplay;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.RecipeType;

/**
 * The Simple Modpack guide (b-guide01, docs/ai/PLAN-B-GUIDE01-2026-11-03.md): the item with its recipe and one-line tooltip,
 * the sections built from the mods' data (one mod alone, a super-mod with its sub-mods), the chapter unlock answer and the
 * creative tabs without the retired single guides. Runs in every SimpleLib suite, also next to one module alone.
 */
public final class ModpackGuideTests {
    private ModpackGuideTests() {}

    /** The item exists, book + crafting table make it, its tooltip is exactly "Your first steps into SimpleMods", using it runs the hooks. */
    public static void item(GameTestHelper h) {
        h.assertTrue(ModpackGuide.item() == LibItems.MODPACK_GUIDE && LibItems.MODPACK_GUIDE != null, "Simple Modpack guide not registered");
        CraftingInput input = CraftingInput.of(2, 1, List.of(new ItemStack(Items.BOOK), new ItemStack(Items.CRAFTING_TABLE)));
        ItemStack crafted = h.getLevel().getServer().getRecipeManager().getRecipeFor(RecipeType.CRAFTING, input, h.getLevel())
                .map(recipe -> recipe.value().assemble(input)).orElse(ItemStack.EMPTY);
        h.assertTrue(crafted.is(LibItems.MODPACK_GUIDE), "book + crafting table crafts " + crafted + ", not the Simple Modpack guide");
        h.assertTrue(LibItems.MODPACK_GUIDE.getDefaultMaxStackSize() == 1, "the guide stacks");
        List<Component> lines = new ArrayList<>();
        crafted.getItem().appendHoverText(crafted, net.minecraft.world.item.Item.TooltipContext.EMPTY, TooltipDisplay.DEFAULT, lines::add, TooltipFlag.NORMAL);
        h.assertTrue(lines.size() == 1 && lines.getFirst().getContents() instanceof TranslatableContents t
                && t.getKey().equals("tooltip.simplelib.modpack_guide"), "tooltip is not the one line: " + lines);
        String en = resource("/assets/simplelib/lang/en_us.json"), de = resource("/assets/simplelib/lang/de_de.json");
        h.assertTrue(en != null && en.contains("\"tooltip.simplelib.modpack_guide\": \"Your first steps into SimpleMods\"")
                && en.contains("\"item.simplelib.modpack_guide\": \"Simple Modpack\""), "English name/tooltip wrong");
        h.assertTrue(de != null && de.contains("\"tooltip.simplelib.modpack_guide\": \"Deine ersten Schritte in die SimpleMods\""), "German tooltip wrong");
        AtomicInteger used = new AtomicInteger();
        ModpackGuide.addUseHook(player -> used.incrementAndGet());
        var player = h.makeMockServerPlayerInLevel();
        h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
        player.setItemInHand(InteractionHand.MAIN_HAND, crafted);
        h.assertTrue(crafted.use(h.getLevel(), player, InteractionHand.MAIN_HAND).consumesAction(), "the guide does not open");
        h.assertTrue(used.get() == 1 && player.getMainHandItem().is(LibItems.MODPACK_GUIDE), "using the guide did not run the hooks once or lost it");
        h.assertTrue(ModpackGuide.isRetiredGuide(Identifier.parse("simplefun:guide_book"))
                && ModpackGuide.isRetiredGuide(Identifier.parse("simplebuilding:guide_book_tools"))
                && !ModpackGuide.isRetiredGuide(ModpackGuide.ITEM_ID) && !ModpackGuide.isRetiredGuide(Identifier.parse("minecraft:guide_book")),
                "retired guide ids recognized wrong");
        h.succeed();
    }

    /**
     * Sections from data: a sub-mod alone is its own section (Simple Fun alone shows "Simple Fun"); with its super-mod
     * installed it joins the super-mod's section after the owner's chapters; sections sort by order; files parse.
     */
    public static void sections(GameTestHelper h) {
        String fun = "{\"section\":\"simplebuilding\",\"name\":\"simplefun.guide.chapter\",\"icon\":\"minecraft:brick\",\"order\":100,"
                + "\"chapters\":[{\"id\":\"simplefun:guide\",\"title\":\"simplefun.guide.chapter\",\"icon\":\"minecraft:brick\","
                + "\"page_key\":\"simplefun.guide.page\",\"pages\":3,\"advancement\":\"simplefun:guide/chapter\",\"hint\":\"minecraft:brick\"}]}";
        String sb = "{\"section\":\"simplebuilding\",\"name\":\"simplebuilding.modpack_guide.section\",\"icon\":\"minecraft:bricks\","
                + "\"order\":0,\"screen\":true,\"chapters\":[]}";
        String qol = "{\"section\":\"simplequalityoflife\",\"name\":\"simplequalityoflife.guide.chapter\",\"icon\":\"minecraft:chest\",\"order\":10,"
                + "\"chapters\":[{\"id\":\"simplequalityoflife:guide\",\"title\":\"q\",\"icon\":\"minecraft:chest\",\"page_key\":\"q\",\"pages\":2}]}";
        var funOnly = ModpackGuide.sections(List.of(contribution("simplefun", fun)));
        h.assertTrue(funOnly.size() == 1 && funOnly.getFirst().id().equals("simplefun") && funOnly.getFirst().name().equals("simplefun.guide.chapter")
                && !funOnly.getFirst().screen() && funOnly.getFirst().chapters().size() == 1, "Simple Fun alone is not its own section: " + funOnly);
        var chapter = funOnly.getFirst().chapters().getFirst();
        h.assertTrue(chapter.pages() == 3 && Identifier.parse("simplefun:guide/chapter").equals(chapter.advancement())
                && Identifier.parse("minecraft:brick").equals(chapter.hint()), "chapter parsed wrong: " + chapter);
        var pack = ModpackGuide.sections(List.of(contribution("simplequalityoflife", qol), contribution("simplefun", fun), contribution("simplebuilding", sb)));
        h.assertTrue(pack.size() == 2 && pack.get(0).id().equals("simplebuilding") && pack.get(1).id().equals("simplequalityoflife"),
                "sections not grouped/sorted: " + pack.stream().map(ModpackGuide.Section::id).toList());
        h.assertTrue(pack.get(0).screen() && pack.get(0).chapters().size() == 1 && pack.get(0).chapters().getFirst().id().equals(chapter.id())
                && pack.get(0).name().equals("simplebuilding.modpack_guide.section"), "Simple Fun did not join SimpleBuilding's section");
        var lonelyOwner = ModpackGuide.sections(List.of(contribution("simpleaesthetics",
                "{\"section\":\"simpleaesthetics\",\"name\":\"a\",\"icon\":\"minecraft:painting\",\"chapters\":[]}")));
        h.assertTrue(lonelyOwner.isEmpty(), "a section without chapters and own screen is shown");
        h.succeed();
    }

    /** The server answers which chapter advancements are done: none before, the awarded one after; unknown ids are never done. */
    public static void chapterJump(GameTestHelper h) {
        // b-guidechap14: a guide turned from an old book names its chapter in custom_data; the client finds the section holding it.
        ItemStack crafted = ModpackGuide.stack();
        h.assertTrue(ModpackGuide.chapterOf(crafted) == null, "a crafted guide claims a chapter");
        ItemStack turned = crafted.copy();
        net.minecraft.nbt.CompoundTag tag = new net.minecraft.nbt.CompoundTag();
        tag.putString(ModpackGuide.CHAPTER_TAG, "simplefun:guide");
        turned.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(tag));
        Identifier chapter = ModpackGuide.chapterOf(turned);
        h.assertTrue(Identifier.parse("simplefun:guide").equals(chapter), "chapter not read back: " + chapter);
        // b-guide15: SimpleBuilding's turned book carries its section id (no colon): a mark, but no chapter
        ItemStack sbBook = crafted.copy();
        net.minecraft.nbt.CompoundTag sbTag = new net.minecraft.nbt.CompoundTag();
        sbTag.putString(ModpackGuide.CHAPTER_TAG, "simplebuilding");
        sbBook.set(net.minecraft.core.component.DataComponents.CUSTOM_DATA, net.minecraft.world.item.component.CustomData.of(sbTag));
        h.assertTrue("simplebuilding".equals(ModpackGuide.markOf(sbBook)) && ModpackGuide.chapterOf(sbBook) == null
                && ModpackGuide.markOf(crafted) == null, "section mark misread");
        String fun = "{\"section\":\"simplebuilding\",\"name\":\"f\",\"icon\":\"minecraft:brick\",\"chapters\":[{\"id\":\"simplefun:guide\","
                + "\"title\":\"f\",\"icon\":\"minecraft:brick\",\"page_key\":\"f\",\"pages\":3}]}";
        String sb = "{\"section\":\"simplebuilding\",\"name\":\"s\",\"icon\":\"minecraft:bricks\",\"order\":0,\"screen\":true,\"chapters\":[]}";
        String qol = "{\"section\":\"simplequalityoflife\",\"name\":\"q\",\"icon\":\"minecraft:chest\",\"order\":10,"
                + "\"chapters\":[{\"id\":\"simplequalityoflife:guide\",\"title\":\"q\",\"icon\":\"minecraft:chest\",\"page_key\":\"q\",\"pages\":2}]}";
        var sections = ModpackGuide.sections(List.of(contribution("simplebuilding", sb), contribution("simplefun", fun),
                contribution("simplequalityoflife", qol)));
        var found = ModpackGuide.sectionOf(sections, chapter);
        h.assertTrue(found != null && found.id().equals("simplebuilding"), "simplefun:guide not found in the SimpleBuilding section: " + found);
        var other = ModpackGuide.sectionOf(sections, Identifier.parse("simplequalityoflife:guide"));
        h.assertTrue(other != null && other.id().equals("simplequalityoflife"), "QoL chapter in the wrong section: " + other);
        h.assertTrue(ModpackGuide.sectionOf(sections, Identifier.parse("simplemissing:guide")) == null
                && ModpackGuide.sectionOf(sections, null) == null, "an unknown chapter found a section");
        for (String locale : List.of("en_us", "de_de")) {
            String lang = resource("/assets/simplelib/lang/" + locale + ".json");
            h.assertTrue(lang != null && lang.contains("\"text.autoconfig.simplelib.option.guideOpensChapter\"")
                    && lang.contains(locale.equals("en_us") ? "Default: false." : "Standard: aus.") && lang.contains("\"simplelib.tab.guide\""),
                    locale + " lacks the guideOpensChapter switch");
        }
        h.succeed();
    }

    public static void unlocks(GameTestHelper h) {
        var player = h.makeMockServerPlayerInLevel();
        h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
        Identifier id = Identifier.parse("simplelib:recipes/misc/modpack_guide");
        var holder = h.getLevel().getServer().getAdvancements().get(id);
        h.assertTrue(holder != null, "test advancement missing: " + id);
        for (String criterion : holder.value().criteria().keySet()) player.getAdvancements().revoke(holder, criterion);
        Identifier unknown = Identifier.parse("simplelib:no/such/advancement");
        h.assertTrue(ModpackGuide.done(player, List.of(id, unknown)).isEmpty(), "done before it was awarded");
        player.getAdvancements().award(holder, holder.value().criteria().keySet().iterator().next());
        h.assertTrue(ModpackGuide.done(player, List.of(unknown, id)).equals(List.of(id)), "the awarded chapter advancement is not reported");
        List<Identifier> many = new ArrayList<>();
        for (int i = 0; i < ModpackGuide.MAX_QUERY; i++) many.add(unknown);
        many.add(id);
        h.assertTrue(ModpackGuide.done(player, many).isEmpty(), "a query beyond " + ModpackGuide.MAX_QUERY + " ids is answered");
        h.succeed();
    }

    /** Creative tabs: the guide stands in a tab, no retired single guide stands in any tab (the search tab included). */
    public static void creativeTabs(GameTestHelper h) {
        net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(h.getLevel().enabledFeatures(), true, h.getLevel().registryAccess());
        // Other suites rebuild single tabs by hand (buildContents, without the rules); the rules run again as after a full build.
        com.simplelib.api.CreativeTabPolicy.apply();
        boolean shown = false;
        List<String> retired = new ArrayList<>();
        for (CreativeModeTab tab : BuiltInRegistries.CREATIVE_MODE_TAB) {
            for (ItemStack stack : tab.getDisplayItems()) {
                shown |= stack.is(LibItems.MODPACK_GUIDE);
                if (ModpackGuide.isRetiredGuide(BuiltInRegistries.ITEM.getKey(stack.getItem()))) retired.add(BuiltInRegistries.ITEM.getKey(stack.getItem()).toString());
            }
            for (ItemStack stack : tab.getSearchTabDisplayItems()) {
                if (ModpackGuide.isRetiredGuide(BuiltInRegistries.ITEM.getKey(stack.getItem()))) retired.add(BuiltInRegistries.ITEM.getKey(stack.getItem()) + " (search)");
            }
        }
        h.assertTrue(shown, "the Simple Modpack guide stands in no creative tab");
        h.assertTrue(retired.isEmpty(), "retired single guides still in creative tabs: " + retired);
        h.succeed();
    }

    private static ModpackGuide.Contribution contribution(String namespace, String json) {
        return ModpackGuide.parse(namespace, JsonParser.parseString(json).getAsJsonObject());
    }

    private static String resource(String path) {
        try (InputStream in = ModpackGuideTests.class.getResourceAsStream(path)) {
            return in == null ? null : new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            return null;
        }
    }
}
