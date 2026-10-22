package com.simplebuilding.modules.simplemodels;
import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AnvilScreen;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.inventory.AnvilMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.SimpleMenuProvider;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.resources.Identifier;
import net.minecraft.network.chat.Component;
import java.util.List;
import com.simplebuilding.modules.simplemodels.client.ModelBrowser;
import com.simplebuilding.modules.simplemodels.client.ModelPreviewTooltip;
import com.simplebuilding.modules.simplemodels.client.ModelsTab;
public final class ModuleClientSmoke implements FabricClientGameTest {
    private static Class<?> type(String name) {
        try { return Class.forName("com.simplebuilding.modules.simplemodels." + name); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Object call(Object target, String method, Object... args) {
        try {
            var cls = target instanceof Class<?> c ? c : target.getClass();
            return cls.getMethod(method, java.util.Arrays.stream(args).map(Object::getClass).toArray(Class<?>[]::new))
                    .invoke(target instanceof Class<?> ? null : target, args);
        } catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static Object field(Object target, String name) {
        try { return (target instanceof Class<?> c ? c : target.getClass()).getField(name).get(target instanceof Class<?> ? null : target); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    private static void set(Object target, String name, Object value) {
        try { (target instanceof Class<?> c ? c : target.getClass()).getField(name).set(target instanceof Class<?> ? null : target, value); }
        catch (ReflectiveOperationException e) { throw new AssertionError(e); }
    }
    /** Seven more models on a second base item: two pages, filter and sort have something to do (SM21 b/c). */
    private static final String WANDS = java.util.stream.IntStream.range(0, 7).mapToObj(i -> (char)('a' + i)).map(c ->
            ",{\"id\":\"renamed:wand_" + c + "\",\"base_item\":\"minecraft:stick\",\"match_name\":\"Wand " + Character.toUpperCase(c)
                    + "\",\"model\":\"minecraft:blaze_rod\",\"tags\":[\"rod\"],\"author\":\"Example\"}").collect(java.util.stream.Collectors.joining());
    private static ModelsTab tab(Screen screen) {
        var tabs = screen.children().stream().filter(ModelsTab.class::isInstance).map(ModelsTab.class::cast).toList();
        if (tabs.size() != 1) throw new AssertionError("Exactly one Models tab on " + screen.getClass().getSimpleName() + ", got " + tabs.size());
        return tabs.getFirst();
    }
    private static Object catalogue() { return field(type("client.ModelBrowser"), "catalogue"); }
    private static List<?> definitions() { return (List<?>)call(catalogue(), "models"); }
    private static void click(Screen screen, String label) {
        var button = screen.children().stream().filter(e -> e instanceof AbstractButton b && b.getMessage().getString().equals(label))
                .map(e -> (AbstractButton)e).findFirst().orElseThrow(() -> new AssertionError("Missing button: " + label));
        if (!button.active) throw new AssertionError("Disabled button: " + label);
        button.onPress(null);
    }
    @Override public void runTest(ClientGameTestContext context) {
        if (!net.fabricmc.loader.api.FabricLoader.getInstance().isModLoaded("simplemodels")) throw new AssertionError("Module did not boot");
        context.takeScreenshot("simplemodels-title");
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets(); world.getConnection().waitForChunksRender();
            world.getServer().runOnServer(server -> {
                var snapshot = call(type("ModelCatalogue"), "decode", "{\"policy\":{\"operatorsOnly\":false},\"models\":[{\"id\":\"renamed:tomato\",\"base_item\":\"minecraft:apple\",\"match_name\":\"Tomato\",\"model\":\"minecraft:golden_apple\",\"tags\":[\"food\",\"red\"],\"author\":\"Example\"}" + WANDS + "]}");
                set(type("Models"), "server", snapshot);
                @SuppressWarnings("unchecked") var sender = (java.util.function.Consumer<net.minecraft.server.level.ServerPlayer>)field(type("Models"), "send");
                server.getPlayerList().getPlayers().forEach(sender);
            });
            world.getConnection().waitForClientboundPackets();
            context.runOnClient(client -> {
                if (client.player == null || definitions().size() != 8) throw new AssertionError("World joined and authoritative catalogue received");
                client.setScreenAndShow(new InventoryScreen(client.player));
            });
            context.waitTicks(3); context.takeScreenshot("simplemodels-inventory-tab");
            context.runOnClient(client -> {
                var tabs = client.gui.screen().children().stream().filter(e -> e instanceof com.simplebuilding.modules.simplemodels.client.ModelsTab).count();
                if (tabs != 1) throw new AssertionError("Exactly one Models tab on the inventory, got " + tabs);
                client.gui.screen().resize(client.gui.screen().width, client.gui.screen().height); // rebuild path: the tab must survive a window resize
                if (client.gui.screen().children().stream().filter(e -> e instanceof com.simplebuilding.modules.simplemodels.client.ModelsTab).count() != 1)
                    throw new AssertionError("Models tab survives resize exactly once");
            });
            // SM21 f: with the recipe book open the bookmark moves to the top edge and leaves the book's edge free.
            context.runOnClient(client -> {
                var screen = client.gui.screen(); var tab = tab(screen);
                screen.children().stream().filter(e -> e instanceof net.minecraft.client.gui.components.ImageButton).map(e -> (AbstractButton)e)
                        .findFirst().orElseThrow(() -> new AssertionError("Recipe book button")).onPress(null);
                tab.place();
                var box = (com.simplebuilding.modules.simplemodels.mixin.ModelContainerAccessor)screen;
                if (screen.width >= 379) {
                    if (!tab.onTop()) throw new AssertionError("Open recipe book puts the tab on the top edge");
                    int bookRight = (screen.width - 147) / 2 - 86 + 147;
                    if (tab.getX() < bookRight || tab.getX() < box.simplemodels$left() || tab.getY() + tab.getHeight() > box.simplemodels$top() + 4)
                        throw new AssertionError("Tab clear of the recipe book, above the inventory frame");
                }
            });
            context.waitTicks(3); context.takeScreenshot("simplemodels-recipe-book");
            context.runOnClient(client -> {
                var screen = client.gui.screen(); var tab = tab(screen);
                screen.children().stream().filter(e -> e instanceof net.minecraft.client.gui.components.ImageButton).map(e -> (AbstractButton)e).findFirst().orElseThrow().onPress(null);
                tab.place();
                if (tab.onTop() || tab.getX() >= ((com.simplebuilding.modules.simplemodels.mixin.ModelContainerAccessor)screen).simplemodels$left())
                    throw new AssertionError("Closed recipe book: tab back on the left edge");
                click(screen, "Models");
            });
            context.waitTicks(3);
            // SM21 b/c/d: filter by base item, sort, wheel paging, tooltip preview.
            context.runOnClient(client -> {
                var browser = (ModelBrowser)client.gui.screen();
                if (browser.filtered().size() != 8 || !browser.filtered().getFirst().match_name().equals("Tomato")) throw new AssertionError("Name A-Z first");
                browser.setBaseFilter("minecraft:stick"); browser.setSort(ModelQuery.Sort.NAME_DESC);
                if (browser.filtered().size() != 7 || !browser.filtered().getFirst().match_name().equals("Wand G")) throw new AssertionError("Stick filter, Z-A");
                if (browser.tooltip(browser.filtered().getFirst()).stream().noneMatch(ModelPreviewTooltip.class::isInstance)) throw new AssertionError("Tooltip carries the model preview");
            });
            context.waitTicks(3); context.takeScreenshot("simplemodels-browser-filter");
            context.runOnClient(client -> {
                var browser = (ModelBrowser)client.gui.screen();
                if (browser.pages() < 2) throw new AssertionError("Seven models need two pages");
                browser.mouseScrolled(1, 1, 0, -1);
                if (browser.page() != 1) throw new AssertionError("Wheel down turns the page");
                browser.mouseScrolled(1, 1, 0, 1); browser.mouseScrolled(1, 1, 0, 1);
                if (browser.page() != 0) throw new AssertionError("Wheel up turns back and stops on the first page");
                browser.setBaseFilter(ModelQuery.ALL); browser.setSort(ModelQuery.Sort.NAME_ASC);
            });
            context.waitTicks(3);
            context.runOnClient(client -> {
                var screen = client.gui.screen();
                var search = screen.children().stream().filter(e -> e instanceof EditBox).map(e -> (EditBox)e).findFirst().orElseThrow();
                search.setFocused(true); search.setValue("FOOD");
                if (((List<?>)call(screen, "filtered")).size() != 1) throw new AssertionError("Search matches tags");
            });
            context.waitTicks(3);
            context.runOnClient(client -> click(client.gui.screen(), "Tomato"));
            context.waitTicks(3); context.takeScreenshot("simplemodels-browser");
            context.runOnClient(client -> com.simplelib.api.client.ui.UiStyleToggle.setEnabled(false));
            context.waitTicks(3); context.takeScreenshot("simplemodels-browser-vanilla");
            context.runOnClient(client -> com.simplelib.api.client.ui.UiStyleToggle.setEnabled(true));
            context.runOnClient(client -> {
                var screen = client.gui.screen();
                var input = (ItemStack)call(screen, "preview", definitions().getFirst());
                if (!input.get(DataComponents.ITEM_MODEL).equals(Identifier.parse("minecraft:golden_apple"))) throw new AssertionError("Preview uses actual item_model");
                set(call(catalogue(), "policy"), "enabled", false);
                if (!((ItemStack)call(type("client.ModelBrowser"), "displayStack", input)).get(DataComponents.ITEM_MODEL).equals(new ItemStack(Items.APPLE).get(DataComponents.ITEM_MODEL))) throw new AssertionError("Disabled server shows true base item");
                if (!input.get(DataComponents.ITEM_MODEL).equals(Identifier.parse("minecraft:golden_apple"))) throw new AssertionError("Render policy never mutates inventory");
                set(call(catalogue(), "policy"), "enabled", true);
                click(screen, "Server Settings");
            });
            context.waitTicks(3); context.takeScreenshot("simplemodels-settings");
            context.runOnClient(client -> click(client.gui.screen(), "Import Help"));
            context.waitTicks(3); context.takeScreenshot("simplemodels-help");
            context.runOnClient(client -> client.setScreenAndShow(null));
            // SM21 e: the creative inventory has the bookmark too.
            var mode = new net.minecraft.world.level.GameType[1];
            world.getServer().runOnServer(server -> { var p = server.getPlayerList().getPlayers().getFirst();
                mode[0] = p.gameMode(); p.setGameMode(net.minecraft.world.level.GameType.CREATIVE); });
            world.getConnection().waitForClientboundPackets();
            context.runOnClient(client -> client.setScreenAndShow(new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(
                    client.player, client.player.connection.enabledFeatures(), false)));
            context.waitTicks(3); context.takeScreenshot("simplemodels-creative-tab");
            context.runOnClient(client -> {
                if (!(client.gui.screen() instanceof net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen)) throw new AssertionError("Creative inventory opens");
                if (tab(client.gui.screen()).onTop()) throw new AssertionError("Creative tab on the left edge");
                client.setScreenAndShow(null);
            });
            world.getServer().runOnServer(server -> server.getPlayerList().getPlayers().getFirst().setGameMode(mode[0]));
            world.getConnection().waitForClientboundPackets();
            world.getServer().runOnServer(server -> {
                var player = server.getPlayerList().getPlayers().getFirst();
                player.openMenu(new SimpleMenuProvider((id, inv, who) -> new AnvilMenu(id, inv, ContainerLevelAccess.NULL), Component.translatable("container.repair")));
                player.containerMenu.getSlot(0).set(new ItemStack(Items.APPLE)); player.containerMenu.broadcastChanges();
            });
            world.getConnection().waitForClientboundPackets(); context.waitTicks(3);
            context.runOnClient(client -> {
                if (!(client.gui.screen() instanceof AnvilScreen)) throw new AssertionError("Anvil opens");
                click(client.gui.screen(), "Models");
            });
            context.waitTicks(3);
            context.runOnClient(client -> click(client.gui.screen(), "Tomato"));
            context.waitTicks(3);
            context.runOnClient(client -> click(client.gui.screen(), "Assign"));
            context.waitTicks(10); world.getConnection().waitForClientboundPackets();
            world.getServer().runOnServer(server -> {
                var out = server.getPlayerList().getPlayers().getFirst().containerMenu.getSlot(2).getItem();
                if (!Identifier.parse("minecraft:golden_apple").equals(out.get(DataComponents.ITEM_MODEL))) throw new AssertionError("Vanilla rename packet reaches approved server anvil assignment");
                if (out.has(DataComponents.CUSTOM_NAME)) throw new AssertionError("Assignment preserves visible base identity");
            });
            context.takeScreenshot("simplemodels-anvil");
            context.runOnClient(client -> client.setScreenAndShow(null));
        }
    }
}
