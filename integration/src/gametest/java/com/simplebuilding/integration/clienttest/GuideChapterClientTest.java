package com.simplebuilding.integration.clienttest;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;

/**
 * b-guidechap14 (09.11.): an old mod handbook turns into the Simple Modpack guide; with SimpleLib's client switch
 * {@code guideOpensChapter} off it opens at the start page (spread 0), on at the first page of that mod's chapter.
 * SimpleLib only by reflection (like Vis24). Log lines {@code [guidechap] OK|FAULT}; run with {@code -PmoduleClientTest=guidechap}.
 */
public final class GuideChapterClientTest implements FabricClientGameTest {
    private static final String SCREENS = "com.simplelib.api.client.ModpackGuideScreens";
    private final List<String> faults = new ArrayList<>();

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            for (String command : new String[] {"gamemode creative @a", "time set noon", "weather clear", "gamerule spawn_mobs false"}) {
                world.getServer().runCommand(command);
            }
            List<String> books = new ArrayList<>();
            for (Identifier id : BuiltInRegistries.ITEM.keySet()) {
                if (id.getPath().equals("guide_book") && !id.getNamespace().equals("simplebuilding")) books.add(id.toString());
            }
            books.sort(null);
            System.out.println("[guidechap] INFO old module books: " + books);
            if (books.isEmpty()) faults.add("no old module guide book registered");
            // two books are enough: the first (alphabetical) and one in another section if there is one
            for (String book : books.subList(0, Math.min(2, books.size()))) {
                for (boolean on : new boolean[] {false, true}) check(context, world, book, on);
            }
            setSwitch(false);
        }
        faults.forEach(line -> System.out.println("[guidechap] FAULT " + line));
        System.out.println("[guidechap] faults: " + faults.size());
        if (!faults.isEmpty()) throw new AssertionError("guidechap: " + faults);
    }

    private void check(ClientGameTestContext context, TestSingleplayerContext world, String book, boolean on) {
        String name = book.replace(':', '-') + (on ? "-chapter" : "-start");
        try {
            setSwitch(on);
            world.getServer().runCommand("item replace entity @a weapon.mainhand with " + book);
            world.getConnection().waitForClientboundPackets();
            context.runOnClient(c -> c.player.getInventory().setSelectedSlot(0));
            context.waitTicks(5);
            // first use turns the old book into the Simple Modpack guide, the second opens it
            context.runOnClient(c -> c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND));
            context.waitTicks(10);
            context.runOnClient(c -> c.gameMode.useItem(c.player, InteractionHand.MAIN_HAND));
            context.waitFor(c -> c.gui.screen() != null, 60);
            context.waitTicks(5);
            String screen = context.computeOnClient(c -> c.gui.screen().getClass().getName());
            int spread = context.computeOnClient(c -> intField(c.gui.screen(), "spread"));
            int chapter = context.computeOnClient(c -> chapterOf(c.gui.screen(), book));
            context.getInput().setCursorPos(1, 1);
            context.takeScreenshot("guidechap-" + name);
            System.out.println(String.format(Locale.ROOT, "[guidechap] OK %s screen=%s spread=%d chapterStart=%d", name, screen, spread, chapter));
            if (on) {
                if (!screen.endsWith(".ModpackGuideScreen")) faults.add(name + ": not SimpleLib's chapter screen: " + screen);
                else if (chapter <= 0 || spread != chapter) faults.add(name + ": opened at spread " + spread + ", chapter starts at " + chapter);
            } else if (screen.endsWith(".ModpackGuideScreen") && spread != 0) {
                faults.add(name + ": switch off but opened at spread " + spread);
            }
        } catch (Throwable e) {
            faults.add(name + ": " + e);
        } finally {
            context.runOnClient(c -> c.setScreenAndShow(null));
            context.waitTicks(3);
        }
    }

    private static void setSwitch(boolean on) {
        try {
            Class.forName(SCREENS).getField("opensChapter").setBoolean(null, on);
        } catch (ReflectiveOperationException e) {
            throw new AssertionError("SimpleLib switch guideOpensChapter missing", e);
        }
    }

    /** -1 when the screen has no such field (SimpleBuilding's own book screen). */
    private static int intField(Object screen, String name) {
        try {
            Field field = screen.getClass().getDeclaredField(name);
            field.setAccessible(true);
            return field.getInt(screen);
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }

    /** First page index of {@code <ns>:guide} in SimpleLib's screen, -1 when absent. */
    private static int chapterOf(Object screen, String book) {
        try {
            Field section = screen.getClass().getDeclaredField("section");
            section.setAccessible(true);
            Object value = section.get(screen);
            if (value == null) return -1;
            List<?> chapters = (List<?>) value.getClass().getMethod("chapters").invoke(value);
            Field starts = screen.getClass().getDeclaredField("chapterStart");
            starts.setAccessible(true);
            int[] start = (int[]) starts.get(screen);
            String id = book.substring(0, book.indexOf(':')) + ":guide";
            for (int i = 0; i < chapters.size(); i++) {
                Object chapterId = chapters.get(i).getClass().getMethod("id").invoke(chapters.get(i));
                if (id.equals(String.valueOf(chapterId))) return start[i];
            }
            return -1;
        } catch (ReflectiveOperationException e) {
            return -1;
        }
    }
}
