package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.client.guide.GuideBookScreen;
import com.simplebuilding.guide.GuideBooks;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

/** Real book input: a locked tab opens by itself once its gate recipe is learned (P5/P6), only on the main line. */
public final class MegaGuideClientTest {
    private MegaGuideClientTest() {}
    public static void inWorld(Script script) {
        if (!com.simplebuilding.version.McVersion.MEGA_GUIDES) return;
        TestScene.build(script, "minecraft:stone", "creative");
        script.command("item replace entity @a weapon.mainhand with simplebuilding:guide_book");
        script.command("recipe take @a *");
        // The scene awards every mod advancement (no toasts later); a done chapter advancement opens its tab, so take Building's back.
        script.command("advancement revoke @a only " + GuideBooks.tab(GuideBooks.Book.BUILDING).advancement());
        script.act("forget an opened Building tab on the server", c -> {
            var server = c.getSingleplayerServer();
            String tag = com.simplebuilding.guide.GuideUnlocks.tag(GuideBooks.tab(GuideBooks.Book.BUILDING));
            server.execute(() -> server.getPlayerList().getPlayers().forEach(player -> player.removeTag(tag)));
        });
        script.command("tp @a 10.5 0.0 16.5 0.0 -90.0");
        script.awaitPackets();
        script.idle("let the book reach the client", 10);
        script.harness("open the held mega guide", h -> h.pressMouse(InputConstants.MOUSE_BUTTON_RIGHT));
        script.await("book opened", 100, c -> c.gui.screen() instanceof GuideBookScreen);
        script.await("Building is locked for a reader without its recipes", 100, c -> !available(c.gui.screen(), GuideBooks.Book.BUILDING));
        script.act("click locked Building: nothing opens, nothing is asked", c -> {
            GuideBookScreen screen = (GuideBookScreen)c.gui.screen();
            clickBuilding(screen);
            if (field(screen, "book") != GuideBooks.Book.GUIDE) throw new AssertionError("a locked tab opened");
        });
        script.shot("mega-guide-locked");
        script.command("recipe give @a " + GuideBooks.gates(GuideBooks.Book.BUILDING).getFirst());
        script.awaitPackets();
        script.await("Building opens by itself once its recipe is learned", 100, c -> available(c.gui.screen(), GuideBooks.Book.BUILDING));
        script.act("click the opened Building tab", c -> clickBuilding((GuideBookScreen)c.gui.screen()));
        script.await("Building is shown", 100, c -> field(c.gui.screen(), "book") == GuideBooks.Book.BUILDING);
        script.shot("mega-guide-unlocked");
        script.act("the book stays open and nothing was taken", c -> {
            if (!(c.gui.screen() instanceof GuideBookScreen)) throw new AssertionError("the book closed");
            if (!c.player.getMainHandItem().is(com.simplebuilding.items.ModItems.GUIDE_BOOK)) throw new AssertionError("the guide left the hand");
            c.gui.setScreen(null);
        });
    }

    private static boolean available(Object screen, GuideBooks.Book book) throws Exception {
        Method method = GuideBookScreen.class.getDeclaredMethod("available", GuideBooks.Book.class);
        method.setAccessible(true);
        return (boolean) method.invoke(screen, book);
    }
    private static void clickBuilding(GuideBookScreen screen) {
        click(screen, (screen.width - 292) / 2 + 300, Math.max(0, (screen.height - 180) / 2) + 12 + 3 * 20 + 8);
    }
    private static Object field(Object value, String name) throws Exception {
        Field field = value.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(value);
    }
    private static void click(GuideBookScreen screen, double x, double y) {
        screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
    }
}
