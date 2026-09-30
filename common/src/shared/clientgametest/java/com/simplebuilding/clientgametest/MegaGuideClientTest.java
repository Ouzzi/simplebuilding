package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.simplebuilding.client.guide.GuideBookScreen;
import com.simplebuilding.guide.GuideBooks;
import com.simplebuilding.networking.GuideUnlockPayload;
import com.simplebuilding.platform.ClientNetworking;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.client.input.MouseButtonInfo;
import net.minecraft.world.item.Items;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.List;

/** Real book input, C2S exchange and component/inventory synchronization, only on the main line. */
public final class MegaGuideClientTest {
    private MegaGuideClientTest() {}
    public static void inWorld(Script script) {
        if (!com.simplebuilding.version.McVersion.MEGA_GUIDES) return;
        TestScene.build(script, "minecraft:stone", "creative");
        script.command("item replace entity @a weapon.mainhand with simplebuilding:guide_book");
        script.command("tp @a 10.5 0.0 16.5 0.0 -90.0");
        script.awaitPackets();
        script.idle("let the book reach the client", 10);
        script.harness("open the held mega guide", h -> h.pressMouse(InputConstants.MOUSE_BUTTON_RIGHT));
        script.await("book opened", 100, c -> c.gui.screen() instanceof GuideBookScreen);
        script.act("click locked Building without its key item", c -> {
            GuideBookScreen screen = (GuideBookScreen)c.gui.screen();
            click(screen, (screen.width - 292) / 2 + 300, Math.max(0, (screen.height - 180) / 2) + 12 + 3 * 20 + 8);
            if (field(screen, "pendingUnlock") != GuideBooks.Book.BUILDING) throw new AssertionError("locked tab did not open the in-book prompt");
            if (confirmPosition(screen) >= 0) throw new AssertionError("missing item offered a confirmation");
        });
        script.command("give @a minecraft:brick 2");
        script.awaitPackets();
        script.await("confirmation appears when required item arrives", 100, c -> confirmPosition((GuideBookScreen)c.gui.screen()) >= 0);
        script.shot("mega-guide-confirm");
        script.act("confirm inside the book", c -> {
            GuideBookScreen screen = (GuideBookScreen)c.gui.screen();
            click(screen, (screen.width - 292) / 2 + 17 + 40,
                    Math.max(0, (screen.height - 180) / 2) + 15 + confirmPosition(screen) + 8);
        });
        script.await("server chapter and consumed item synchronize", 100, c ->
                GuideBooks.inserted(c.player.getMainHandItem(), GuideBooks.Book.BUILDING)
                        && c.player.getInventory().countItem(Items.BRICK) == 1
                        && field(c.gui.screen(), "pendingUnlock") == null
                        && field(c.gui.screen(), "book") == GuideBooks.Book.BUILDING);
        script.shot("mega-guide-unlocked");
        script.act("repeat the same client packet", c -> ClientNetworking.send(new GuideUnlockPayload(GuideBooks.Book.BUILDING.ordinal())));
        script.awaitPackets();
        script.idle("let duplicate request settle", 10);
        script.act("duplicate consumes nothing and keeps the screen open", c -> {
            if (c.player.getInventory().countItem(Items.BRICK) != 1 || !(c.gui.screen() instanceof GuideBookScreen))
                throw new AssertionError("duplicate consumed an item or closed the book");
            c.gui.setScreen(null);
        });
    }

    private static Object field(Object value, String name) throws Exception {
        Field field = value.getClass().getDeclaredField(name); field.setAccessible(true); return field.get(value);
    }
    private static Object call(Object value, String name) throws Exception {
        Method method = value.getClass().getDeclaredMethod(name); method.setAccessible(true); return method.invoke(value);
    }
    private static int confirmPosition(GuideBookScreen screen) throws Exception {
        List<?> pages = (List<?>)field(screen, "pages");
        for (Object placed : (List<?>)pages.getFirst()) {
            Object element = call(placed, "element");
            if (element.getClass().getSimpleName().equals("UnlockAction") && (boolean)call(element, "confirm")) return (int)call(placed, "y");
        }
        return -1;
    }
    private static void click(GuideBookScreen screen, double x, double y) {
        screen.mouseClicked(new MouseButtonEvent(x, y, new MouseButtonInfo(InputConstants.MOUSE_BUTTON_LEFT, 0)), false);
    }
}
