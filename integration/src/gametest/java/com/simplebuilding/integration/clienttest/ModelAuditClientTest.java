package com.simplebuilding.integration.clienttest;

import java.util.ArrayList;
import java.util.List;

import net.fabricmc.fabric.api.client.gametest.v1.FabricClientGameTest;
import net.fabricmc.fabric.api.client.gametest.v1.context.ClientGameTestContext;
import net.minecraft.client.renderer.block.dispatch.BlockStateModel;
import net.minecraft.client.renderer.block.dispatch.BlockStateModelPart;
import net.minecraft.client.renderer.item.ItemStackRenderState;
import net.minecraft.client.renderer.item.MissingItemModel;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.util.RandomSource;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemDisplayContext;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.RenderShape;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Nachtrag 66 (2026-10-16): the owner still saw missing textures in game, and a static file check cannot
 * see what the client really baked. With every module loaded (this is the integration client, the same
 * set of mods the player gets), every item and block state of every namespace except {@code minecraft}
 * must resolve to a real model, and no sprite on it may be {@code minecraft:missingno}.
 *
 * <p>Items: the model the client picks for the item's {@code item_model} id must not be the
 * {@link MissingItemModel} fallback, and no layer's particle sprite may be the missing sprite.
 * Blocks: the baked model must not be the set's missing model, and no quad of any part (all six sides
 * and unculled) and no particle may use the missing sprite. Invisible blocks are skipped.
 *
 * <p>Run: {@code python3.12 tools/testrunner/run.py --targets audit-models-263}. The runner then also
 * scans the client log for the vanilla warnings of the same faults.
 */
public final class ModelAuditClientTest implements FabricClientGameTest {
    private static final Identifier MISSINGNO = Identifier.withDefaultNamespace("missingno");

    @Override
    public void runTest(ClientGameTestContext context) {
        try (var world = context.worldBuilder().create()) {
            world.getConnection().waitForClientboundPackets();
            context.runOnClient(client -> {
                List<String> found = new ArrayList<>();
                int items = 0;
                int states = 0;
                for (Item item : BuiltInRegistries.ITEM) {
                    Identifier id = BuiltInRegistries.ITEM.getKey(item);
                    ItemStack stack = new ItemStack(item);
                    if (id.getNamespace().equals("minecraft") || stack.isEmpty()) {
                        continue;
                    }
                    items++;
                    Identifier modelId = stack.get(DataComponents.ITEM_MODEL);
                    if (modelId != null && client.getModelManager().getItemModel(modelId) instanceof MissingItemModel) {
                        found.add("item " + id + ": no baked item model " + modelId);
                        continue;
                    }
                    ItemStackRenderState state = new ItemStackRenderState();
                    client.getItemModelResolver().updateForTopItem(state, stack, ItemDisplayContext.GUI, client.level,
                            client.player, 0);
                    for (int i = 0; i < 32 && !state.isEmpty(); i++) {
                        var material = state.pickParticleMaterial(RandomSource.create(i));
                        if (material != null && MISSINGNO.equals(material.sprite().contents().name())) {
                            found.add("item " + id + ": a layer draws the missing sprite");
                            break;
                        }
                    }
                }
                var models = client.getModelManager().getBlockStateModelSet();
                for (Block block : BuiltInRegistries.BLOCK) {
                    Identifier id = BuiltInRegistries.BLOCK.getKey(block);
                    if (id.getNamespace().equals("minecraft")) {
                        continue;
                    }
                    for (BlockState blockState : block.getStateDefinition().getPossibleStates()) {
                        if (blockState.getRenderShape() == RenderShape.INVISIBLE) {
                            continue;
                        }
                        states++;
                        BlockStateModel model = models.get(blockState);
                        if (model == models.missingModel()) {
                            found.add("block " + id + ": no baked model for " + blockState);
                            break;
                        }
                        if (usesMissingSprite(model)) {
                            found.add("block " + id + ": missing sprite on " + blockState);
                            break;
                        }
                    }
                }
                found.forEach(line -> System.out.println("[model-audit] FAULT " + line));
                System.out.println("[model-audit] items checked: " + items + ", block states checked: " + states
                        + ", faults: " + found.size());
                if (!found.isEmpty()) {
                    throw new AssertionError("model audit found " + found.size() + " faults: " + found);
                }
            });
            if (System.getenv("SB_TABSHOTS") != null) {
                tabShots(context, world);
            }
        }
    }

    private static boolean usesMissingSprite(BlockStateModel model) {
        List<BlockStateModelPart> parts = new ArrayList<>();
        model.collectParts(RandomSource.create(0L), parts);
        for (BlockStateModelPart part : parts) {
            if (MISSINGNO.equals(part.particleMaterial().sprite().contents().name())) {
                return true;
            }
            for (Direction side : new Direction[]{null, Direction.DOWN, Direction.UP, Direction.NORTH,
                    Direction.SOUTH, Direction.WEST, Direction.EAST}) {
                for (var quad : part.getQuads(side)) {
                    if (MISSINGNO.equals(quad.materialInfo().sprite().contents().name())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    /** Optional (env SB_TABSHOTS=1): a screenshot of every creative tab and of the inventory with the colour brush. */
    private static void tabShots(ClientGameTestContext context, net.fabricmc.fabric.api.client.gametest.v1.context.TestSingleplayerContext world) {
        var server = world.getServer();
        server.runCommand("gamemode creative @a");
        String[][] inks = {{"none", ""}, {"red", "give @a minecraft:red_dye 4"}, {"palette", "give @a simplebuilding:paint_box[simplebuilding:paint_box={counts:[0,0,0,0,0,0,0,0,0,0,0,0,0,3,0,0],selected:13}]"},
                {"white", "give @a minecraft:white_dye"}, {"black", "give @a minecraft:black_dye"}};
        for (String[] ink : inks) {
            server.runCommand("clear @a");
            server.runCommand("give @a simplebuilding:color_brush");
            if (!ink[1].isEmpty()) {
                server.runCommand(ink[1]);
            }
            context.waitTicks(10);
            context.setScreen(() -> new net.minecraft.client.gui.screens.inventory.InventoryScreen(context.computeOnClient(c -> c.player)));
            context.waitTicks(5);
            context.takeScreenshot("brush-" + ink[0]);
            context.setScreen(() -> null);
        }
        List<net.minecraft.world.item.CreativeModeTab> tabs = context.computeOnClient(c -> {
            var access = c.player.connection.registryAccess();
            net.minecraft.world.item.CreativeModeTabs.tryRebuildTabContents(c.player.connection.enabledFeatures(), true, access);
            return new ArrayList<>(net.minecraft.world.item.CreativeModeTabs.tabs());
        });
        context.setScreen(() -> context.computeOnClient(c -> new net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen(
                c.player, c.player.connection.enabledFeatures(), true)));
        int n = 0;
        for (var tab : tabs) {
            if (tab.getType() != net.minecraft.world.item.CreativeModeTab.Type.CATEGORY) {
                continue;
            }
            String name = tab.getDisplayName().getString().replaceAll("[^A-Za-z0-9]+", "_");
            context.runOnClient(c -> {
                try { // page switch from the Fabric creative-tab API, then the private vanilla selectTab (both by reflection)
                    var screen = c.gui.screen();
                    int page = (int) screen.getClass().getMethod("getPage", net.minecraft.world.item.CreativeModeTab.class).invoke(screen, tab);
                    screen.getClass().getMethod("switchToPage", int.class).invoke(screen, page);
                    var m = net.minecraft.client.gui.screens.inventory.CreativeModeInventoryScreen.class
                            .getDeclaredMethod("selectTab", net.minecraft.world.item.CreativeModeTab.class);
                    m.setAccessible(true);
                    m.invoke(screen, tab);
                } catch (ReflectiveOperationException e) {
                    throw new AssertionError(e);
                }
            });
            context.getInput().setCursorPos(2, 2);
            context.waitTicks(3);
            context.takeScreenshot(String.format("tab-%02d-%s", n++, name));
        }
        context.setScreen(() -> null);
    }
}
