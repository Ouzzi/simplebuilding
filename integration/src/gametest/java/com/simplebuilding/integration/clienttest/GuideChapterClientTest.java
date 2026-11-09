package com.simplebuilding.integration.clienttest;

import com.mojang.blaze3d.platform.InputConstants;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

/**
 * b-guidechap14 / b-guide15 (09.11.): every module's chapter of the Simple Modpack guide, one by one, in survival with
 * SimpleLib's client switch {@code guideOpensChapter} on. A mod with an old handbook ({@code <ns>:guide_book}) goes the
 * player's way: chapter advancement revoked, the old book used (turns into the guide and opens the chapter), used again
 * (opens at the first page of that chapter, also after the server's unlock answer). A mod without a book (sub-mods, client
 * mods) gets a guide marked with its chapter. Plus: SimpleBuilding's own book (section mark), a guide without a mark (opens
 * the chapter last read), Home back to the contents, and the switch off (start page). One screenshot each.
 * SimpleLib only by reflection. Log lines {@code [guidechap] RESULT <chapter> ok|fehlt <reason> | <screenshot>}, then
 * {@code [guidechap] faults: <n>}; run with {@code -PmoduleClientTest=guidechap}.
 */
public final class GuideChapterClientTest implements FabricClientGameTest {
    private static final String GUIDE = "com.simplelib.api.ModpackGuide";
    private static final String SCREENS = "com.simplelib.api.client.ModpackGuideScreens";
    private final List<String> faults = new ArrayList<>();
    private int ok;

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            for (String command : new String[] {"gamemode survival @a", "time set noon", "weather clear", "gamerule spawn_mobs false",
                    "effect give @a resistance infinite 255 true", "effect give @a saturation infinite 255 true"}) {
                world.getServer().runCommand(command);
            }
            Map<String, Integer> chapters = context.computeOnClient(c -> chapters());
            System.out.println("[guidechap] INFO chapters: " + chapters.size() + " " + chapters.keySet());
            if (chapters.isEmpty()) faults.add("no chapter in the Simple Modpack guide");
            setSwitch(true);
            for (Map.Entry<String, Integer> entry : chapters.entrySet()) {
                String chapter = entry.getKey();
                Identifier book = Identifier.fromNamespaceAndPath(chapter.substring(0, chapter.indexOf(':')), "guide_book");
                checkChapter(context, world, chapter, entry.getValue(), BuiltInRegistries.ITEM.containsKey(book) ? book.toString() : null);
            }
            checkSimpleBuilding(context, world);
            checkUnmarked(context, world);
            checkSwitchOff(context, world);
            setSwitch(false);
        }
        faults.forEach(line -> System.out.println("[guidechap] FAULT " + line));
        System.out.println("[guidechap] ok: " + ok);
        System.out.println("[guidechap] faults: " + faults.size());
        if (!faults.isEmpty()) throw new AssertionError("guidechap: " + faults);
    }

    /** Opens one chapter the player's way and checks the guide stands on its first page. */
    private void checkChapter(ClientGameTestContext context, TestSingleplayerContext world, String chapter, int pages, String book) {
        String ns = chapter.substring(0, chapter.indexOf(':'));
        String shot = "guidechap-" + ns;
        try {
            if (pages <= 0) {
                result(chapter, false, "Kapitel ohne Seiten", "-");
                return;
            }
            if (book != null) {
                world.getServer().runCommand("advancement revoke @a only " + ns + ":guide/chapter");
                hold(context, world, book);
                use(context); // turns the old book into the guide (and opens its chapter)
                context.waitTicks(10);
            } else {
                hold(context, world, "simplelib:modpack_guide[minecraft:custom_data={simplelib_guide_chapter:\"" + chapter + "\"}]");
            }
            use(context);
            waitScreen(context);
            // the server's unlock answer may come a few ticks after the book opens
            try {
                context.waitFor(c -> start(c.gui.screen(), chapter) > 0 && intField(c.gui.screen(), "spread") == start(c.gui.screen(), chapter), 80);
            } catch (Throwable timeout) {
                // judged below
            }
            String screen = context.computeOnClient(c -> c.gui.screen() == null ? "none" : c.gui.screen().getClass().getSimpleName());
            int spread = context.computeOnClient(c -> intField(c.gui.screen(), "spread"));
            int start = context.computeOnClient(c -> start(c.gui.screen(), chapter));
            shoot(context, shot);
            String reason;
            if (!screen.equals("ModpackGuideScreen")) reason = "falscher Bildschirm " + screen;
            else if (start == -2) reason = "Kapitel nicht im Abschnitt";
            else if (start < 0) reason = "Kapitel gesperrt (Freischaltung fehlt)";
            else if (spread != start) reason = "öffnet Seite " + spread + " statt " + start;
            else reason = null;
            result(chapter, reason == null, reason != null ? reason
                    : (book != null ? "altes Buch getauscht" : "markiertes Handbuch") + ", " + pages + " S., ab Seite " + (start + 1), shot);
        } catch (Throwable e) {
            result(chapter, false, String.valueOf(e), shot);
        } finally {
            close(context);
        }
    }

    /** SimpleBuilding's book turns at login into a guide marked "simplebuilding": it opens SimpleBuilding's own book screen. */
    private void checkSimpleBuilding(ClientGameTestContext context, TestSingleplayerContext world) {
        String shot = "guidechap-simplebuilding";
        try {
            hold(context, world, "simplelib:modpack_guide[minecraft:custom_data={simplelib_guide_chapter:\"simplebuilding\"}]");
            use(context);
            waitScreen(context);
            context.waitTicks(5);
            String screen = context.computeOnClient(c -> c.gui.screen().getClass().getSimpleName());
            shoot(context, shot);
            boolean good = screen.equals("GuideBookScreen");
            result("simplebuilding", good, good ? "SB-Kern-Handbuch: eigener Bildschirm" : "falscher Bildschirm " + screen, shot);
        } catch (Throwable e) {
            result("simplebuilding", false, String.valueOf(e), shot);
        } finally {
            close(context);
        }
    }

    /** A guide without a mark (crafted, or turned long ago) opens the chapter last read; Home leads back to the contents. */
    private void checkUnmarked(ClientGameTestContext context, TestSingleplayerContext world) {
        String shot = "guidechap-unmarked";
        try {
            hold(context, world, "simplelib:modpack_guide");
            use(context);
            waitScreen(context);
            context.waitTicks(5);
            int spread = context.computeOnClient(c -> intField(c.gui.screen(), "spread"));
            shoot(context, shot);
            boolean good = spread > 0;
            result("ohne-markierung", good, good ? "gecraftet/alt: öffnet zuletzt gelesenes Kapitel (Seite " + (spread + 1) + ")"
                    : "gecraftet/alt: öffnet Startseite", shot);
            context.getInput().pressKey(InputConstants.KEY_HOME);
            context.waitTicks(3);
            int back = context.computeOnClient(c -> intField(c.gui.screen(), "spread"));
            shoot(context, "guidechap-overview");
            result("rueckweg-uebersicht", back == 0, back == 0 ? "Pos1/Lesezeichen Inhalt führt zur Übersicht" : "Pos1 blieb auf Seite " + back,
                    "guidechap-overview");
        } catch (Throwable e) {
            result("ohne-markierung", false, String.valueOf(e), shot);
        } finally {
            close(context);
        }
    }

    /** Switch off: a marked guide opens at the start (spread 0), as before GUIDE14. */
    private void checkSwitchOff(ClientGameTestContext context, TestSingleplayerContext world) {
        String shot = "guidechap-switch-off";
        try {
            setSwitch(false);
            hold(context, world, "simplelib:modpack_guide[minecraft:custom_data={simplelib_guide_chapter:\"simplefun:guide\"}]");
            use(context);
            waitScreen(context);
            context.waitTicks(5);
            String screen = context.computeOnClient(c -> c.gui.screen().getClass().getSimpleName());
            int spread = context.computeOnClient(c -> intField(c.gui.screen(), "spread"));
            shoot(context, shot);
            boolean good = !screen.equals("ModpackGuideScreen") || spread == 0;
            result("schalter-aus", good, good ? "Startseite (" + screen + ")" : "Schalter aus, aber Seite " + spread, shot);
        } catch (Throwable e) {
            result("schalter-aus", false, String.valueOf(e), shot);
        } finally {
            close(context);
            setSwitch(true);
        }
    }

    private void result(String what, boolean good, String reason, String shot) {
        System.out.println(String.format(Locale.ROOT, "[guidechap] RESULT %s %s %s | %s", what, good ? "ok" : "fehlt", reason, shot));
        if (good) ok++;
        else faults.add(what + ": " + reason);
    }

    private static void hold(ClientGameTestContext context, TestSingleplayerContext world, String item) {
        world.getServer().runCommand("item replace entity @a weapon.mainhand with " + item);
        world.getConnection().waitForClientboundPackets();
        context.runOnClient(c -> c.player.getInventory().setSelectedSlot(0));
        context.waitTicks(5);
    }

    private static void use(ClientGameTestContext context) {
        context.runOnClient(c -> c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND));
    }

    private static void waitScreen(ClientGameTestContext context) {
        context.waitFor(c -> c.gui.screen() != null, 60);
        context.waitTicks(3);
    }

    private static void shoot(ClientGameTestContext context, String name) {
        context.getInput().setCursorPos(1, 1);
        context.waitTicks(2);
        context.takeScreenshot(name);
    }

    private static void close(ClientGameTestContext context) {
        context.runOnClient(c -> c.setScreenAndShow(null));
        context.waitTicks(3);
    }

    private static void setSwitch(boolean on) {
        try {
            Class.forName(GUIDE).getField("opensChapter").setBoolean(null, on);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("SimpleLib switch guideOpensChapter missing", e);
        }
    }

    /** Every chapter of every section (client thread): id -> pages. */
    private static Map<String, Integer> chapters() {
        Map<String, Integer> out = new LinkedHashMap<>();
        try {
            List<?> sections = (List<?>) Class.forName(SCREENS).getMethod("reload").invoke(null);
            for (Object section : sections) {
                for (Object chapter : (List<?>) section.getClass().getMethod("chapters").invoke(section)) {
                    String id = String.valueOf(chapter.getClass().getMethod("id").invoke(chapter));
                    out.put(id, (Integer) chapter.getClass().getMethod("pages").invoke(chapter));
                }
            }
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("SimpleLib guide sections unreadable", e);
        }
        return out;
    }

    /** -1 when the screen has no such field (SimpleBuilding's own book screen). */
    private static int intField(Object screen, String name) {
        if (screen == null) return -1;
        try {
            Field field = screen.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(screen);
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    /** First page index of {@code chapter} in SimpleLib's screen: -1 locked/no pages, -2 not in the open section. */
    private static int start(Object screen, String chapter) {
        if (screen == null) return -2;
        try {
            Field section = screen.getClass().getDeclaredField("section");
            section.setAccessible(true);
            Object value = section.get(screen);
            if (value == null) return -2;
            List<?> chapters = (List<?>) value.getClass().getMethod("chapters").invoke(value);
            Field starts = screen.getClass().getDeclaredField("chapterStart");
            starts.setAccessible(true);
            int[] start = (int[]) starts.get(screen);
            for (int i = 0; i < chapters.size(); i++) {
                Object chapterId = chapters.get(i).getClass().getMethod("id").invoke(chapters.get(i));
                if (chapter.equals(String.valueOf(chapterId))) return i < start.length ? start[i] : -1;
            }
            return -2;
        } catch (ReflectiveOperationException e) {
            return -2;
        }
    }
}
