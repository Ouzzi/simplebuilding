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
                var snapshot = call(type("ModelCatalogue"), "decode", "{\"policy\":{\"operatorsOnly\":false},\"models\":[{\"id\":\"renamed:tomato\",\"base_item\":\"minecraft:apple\",\"match_name\":\"Tomato\",\"model\":\"minecraft:golden_apple\",\"tags\":[\"food\",\"red\"],\"author\":\"Example\"}]}");
                set(type("Models"), "server", snapshot);
                @SuppressWarnings("unchecked") var sender = (java.util.function.Consumer<net.minecraft.server.level.ServerPlayer>)field(type("Models"), "send");
                server.getPlayerList().getPlayers().forEach(sender);
            });
            world.getConnection().waitForClientboundPackets();
            context.runOnClient(client -> {
                if (client.player == null || definitions().size() != 1) throw new AssertionError("World joined and authoritative catalogue received");
                client.setScreenAndShow(new InventoryScreen(client.player));
            });
            context.waitTicks(3); context.takeScreenshot("simplemodels-inventory-tab");
            context.runOnClient(client -> {
                var tabs = client.gui.screen().children().stream().filter(e -> e instanceof com.simplebuilding.modules.simplemodels.client.ModelsTab).count();
                if (tabs != 1) throw new AssertionError("Exactly one Models tab on the inventory, got " + tabs);
                client.gui.screen().resize(client.gui.screen().width, client.gui.screen().height); // rebuild path: the tab must survive a window resize
                if (client.gui.screen().children().stream().filter(e -> e instanceof com.simplebuilding.modules.simplemodels.client.ModelsTab).count() != 1)
                    throw new AssertionError("Models tab survives resize exactly once");
                click(client.gui.screen(), "Models");
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
