package com.simplebuilding.clientgametest;

import com.mojang.blaze3d.platform.InputConstants;
import com.mojang.math.Axis;
import com.simplebuilding.blocks.ModBlocks;
import com.simplebuilding.blocks.entity.custom.PlacedBundleBlockEntity;
import com.simplebuilding.blocks.entity.custom.PlacedTemplateBlockEntity;
import com.simplebuilding.client.render.BlockHighlightRenderer;
import com.simplebuilding.client.render.PlacedBundleRenderer;
import com.simplebuilding.items.ModItems;
import com.simplebuilding.util.PlacedBundles;
import java.util.List;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.blockentity.BlockEntityRenderer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.HitResult;
import net.minecraft.world.phys.Vec3;
import org.joml.Quaternionf;
import org.joml.Vector3f;

/**
 * Abgestelltes Buendel und abgelegter Oktant im echten Client (Besitzer 2026-09-29).
 *
 * <ol>
 *   <li><b>Schleichen + Mausrad</b> ueber einem abgestellten Buendel schaltet das gezeigte Item
 *       weiter (Rad nach unten: naechstes, nach oben: voriges) - ueber den echten Weg Maus ->
 *       {@code MouseMixin} -> Paket -> Server -> Block-Entity-Update -> Client. Die gewaehlte
 *       Hotbar-Stelle bleibt dabei stehen: das Mausrad-Ereignis erreicht Vanilla nicht.</li>
 *   <li><b>Das schwebende Item schaut zur Kamera</b>: der Render-State des echten Renderers dreht die
 *       Vorderseite (+Z) genau auf die uebergebene Kameraposition - auch auf eine zweite, wie sie ein
 *       anderer Client haette - und dreht sich mit der Zeit nicht.</li>
 *   <li><b>Abgelegter Oktant</b>: steht der eigene Spieler in der Liste der Block-Entity, zeichnet der
 *       Client dessen Auswahl samt Leuchten; nach dem Ausschalten nicht mehr.</li>
 * </ol>
 */
public final class PlacedBundleClientTest {

    private static final int SHIFT_KEY = InputConstants.KEY_LSHIFT;
    /** Das Buendel steht vor dem Spieler der Testszene (10.5 / 0 / 16.5) auf dem Boden. */
    private static final BlockPos BUNDLE = new BlockPos(10, 0, 18);
    /** Der abgelegte Oktant liegt seitlich, ausserhalb des Blickfelds - er darf trotzdem leuchten. */
    private static final BlockPos OCTANT = new BlockPos(14, 0, 12);
    private static final int HOTBAR_SLOT = 4;

    private PlacedBundleClientTest() {
    }

    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:stone", "survival");

        onServer(script, "place a reinforced bundle holding torches, apples and feathers", server -> {
            ServerLevel level = server.overworld();
            level.setBlock(BUNDLE, ModBlocks.PLACED_BUNDLE.defaultBlockState(), 3);
            ItemStack bundle = new ItemStack(ModItems.REINFORCED_BUNDLE);
            PlacedBundles.setContents(bundle, List.of(new ItemStack(Items.TORCH, 8), new ItemStack(Items.APPLE, 3),
                    new ItemStack(Items.FEATHER, 5)));
            if (level.getBlockEntity(BUNDLE) instanceof PlacedBundleBlockEntity be) {
                be.setBundle(bundle);
            }
        });
        script.awaitPackets();
        script.await("the client has the placed bundle with three items", 100,
                client -> bundle(client) != null && bundle(client).contents().size() == 3 && bundle(client).shownIndex() == 0,
                client -> "the client block entity at " + BUNDLE + " is " + (bundle(client) == null ? "missing"
                        : bundle(client).contents() + " showing " + bundle(client).shownIndex()));

        script.act("select a hotbar slot the wheel must not move", client ->
                client.player.getInventory().setSelectedSlot(HOTBAR_SLOT));
        // So weit hinunter, dass das Fadenkreuz mit den gesenkten Augen des Schleichens auf dem Bauch
        // des Buendels liegt und das schwebende Item darueber noch im Bild ist.
        script.command("tp @a 10.5 0.0 16.5 0.0 28.0");
        script.awaitPackets();
        script.harness("hold Shift to sneak", harness -> harness.holdKey(SHIFT_KEY));
        script.await("the player sneaks on both sides and looks at the bundle", 100,
                client -> client.player.isShiftKeyDown() && serverPlayer(client).isShiftKeyDown() && aimsAtBundle(client),
                TestScene::describeAim);

        // Rad nach unten: naechstes Item (Aepfel).
        scrollAndAwait(script, -1.0, 1, "one notch down");
        assertSlotUnchanged(script, "after one notch down");
        // Rad nach oben: zurueck zu den Fackeln, dann rueckwaerts ueber den Anfang zu den Federn.
        scrollAndAwait(script, 1.0, 0, "one notch up");
        scrollAndAwait(script, 1.0, 2, "a second notch up, past the first item");
        assertSlotUnchanged(script, "after three notches while sneaking");

        // Billboard: der Render-State zeigt mit +Z auf die Kamera, jede Kamera fuer sich.
        script.act("the floating item faces the camera of this client and of any other", client -> {
            PlacedBundleBlockEntity be = bundle(client);
            BlockEntityRenderer<PlacedBundleBlockEntity, PlacedBundleRenderer.State> renderer =
                    client.getBlockEntityRenderDispatcher().getRenderer(be);
            if (renderer == null) {
                throw new AssertionError("No renderer for the placed bundle block entity");
            }
            Vec3 item = PlacedBundleRenderer.itemCentre(be);
            Vec3 own = client.player.getEyePosition();
            PlacedBundleRenderer.State first = extract(renderer, be, 0.0F, own);
            if (!first.visible) {
                throw new AssertionError("The floating item is not shown although the player sneaks and aims at the bundle: "
                        + TestScene.describeAim(client));
            }
            assertFaces(first, item, own, "this client's camera");
            PlacedBundleRenderer.State later = extract(renderer, be, 0.9F, own);
            if (Math.abs(later.yaw - first.yaw) > 0.01F || Math.abs(later.pitch - first.pitch) > 0.01F) {
                throw new AssertionError("The floating item turns by itself between frames: yaw " + first.yaw + " -> " + later.yaw
                        + ", pitch " + first.pitch + " -> " + later.pitch);
            }
            // Eine zweite Kamera (ein anderer Spieler) auf der anderen Seite des Buendels.
            Vec3 other = item.add(-3.0, 1.5, 2.0);
            assertFaces(extract(renderer, be, 0.0F, other), item, other, "a second camera on the other side");
        });
        script.idle("let the floating item show", 10);
        script.shot("placed-bundle-a-floating-item");
        script.harness("release Shift", harness -> harness.releaseKey(SHIFT_KEY));

        // Ohne Schleichen scrollt das Rad wieder die Hotbar.
        script.await("the player stands up again", 60, client -> !client.player.isShiftKeyDown());
        script.harness("scroll without sneaking", harness -> harness.scroll(-1.0));
        script.awaitPackets();
        script.await("a plain scroll moves the hotbar again", 40,
                client -> client.player.getInventory().getSelectedSlot() != HOTBAR_SLOT,
                client -> "the selected slot stayed at " + client.player.getInventory().getSelectedSlot());
        script.check("the plain scroll left the top item alone", client -> bundle(client).shownIndex() == 2);

        // Abgelegter Oktant: eingeblendet fuer den eigenen Spieler -> gezeichnet; ausgeschaltet -> nicht.
        onServer(script, "put down a locked octant and switch its outline on for the player", server -> {
            ServerLevel level = server.overworld();
            level.setBlock(OCTANT, ModBlocks.PLACED_SMITHING_TEMPLATE.defaultBlockState(), 3);
            ItemStack octant = new ItemStack(ModItems.OCTANT);
            CompoundTag nbt = new CompoundTag();
            nbt.putIntArray("Pos1", new int[]{12, 0, 11});
            nbt.putIntArray("Pos2", new int[]{15, 2, 13});
            nbt.putBoolean("Locked", true);
            octant.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
            if (level.getBlockEntity(OCTANT) instanceof PlacedTemplateBlockEntity be) {
                be.setTemplate(octant);
                be.toggleOutlineViewer(server.getPlayerList().getPlayers().get(0).getUUID());
            }
        });
        script.awaitPackets();
        script.await("the client draws the placed octant it switched on", 100,
                client -> BlockHighlightRenderer.placedOctantsShown == 1,
                client -> "placed octants drawn: " + BlockHighlightRenderer.placedOctantsShown
                        + ", client block entity " + client.level.getBlockEntity(OCTANT));
        onServer(script, "switch the outline off again", server -> {
            if (server.overworld().getBlockEntity(OCTANT) instanceof PlacedTemplateBlockEntity be) {
                be.toggleOutlineViewer(server.getPlayerList().getPlayers().get(0).getUUID());
            }
        });
        script.awaitPackets();
        script.await("the client stops drawing the placed octant", 100,
                client -> BlockHighlightRenderer.placedOctantsShown == 0,
                client -> "placed octants drawn: " + BlockHighlightRenderer.placedOctantsShown);
        script.command("setblock " + OCTANT.getX() + " " + OCTANT.getY() + " " + OCTANT.getZ() + " minecraft:air");
        script.command("setblock " + BUNDLE.getX() + " " + BUNDLE.getY() + " " + BUNDLE.getZ() + " minecraft:air");
        script.command("kill @e[type=!minecraft:player]", true);
        script.awaitPackets();
    }

    // ------------------------------------------------------------------------------------------

    private static void scrollAndAwait(Script script, double amount, int expectedIndex, String what) {
        script.harness("scroll the wheel by " + amount, harness -> harness.scroll(amount));
        script.awaitPackets();
        script.await("the top item is number " + expectedIndex + " " + what, 60,
                client -> bundle(client).shownIndex() == expectedIndex,
                client -> "after " + what + " the client shows item " + bundle(client).shownIndex() + " ("
                        + bundle(client).shownItem() + "), the server " + serverBundleIndex(client)
                        + "; " + TestScene.describeAim(client));
    }

    private static void assertSlotUnchanged(Script script, String when) {
        script.check("the hotbar selection stayed on slot " + HOTBAR_SLOT + " " + when,
                client -> client.player.getInventory().getSelectedSlot() == HOTBAR_SLOT);
    }

    private static PlacedBundleRenderer.State extract(BlockEntityRenderer<PlacedBundleBlockEntity, PlacedBundleRenderer.State> renderer,
                                                      PlacedBundleBlockEntity be, float partialTicks, Vec3 camera) {
        PlacedBundleRenderer.State state = renderer.createRenderState();
        renderer.extractRenderState(be, state, partialTicks, camera, null);
        return state;
    }

    /** Die Drehung des Renderers (erst Gier, dann Neigung) bildet +Z auf die Richtung zur Kamera ab. */
    private static void assertFaces(PlacedBundleRenderer.State state, Vec3 item, Vec3 camera, String which) {
        Quaternionf rotation = new Quaternionf(Axis.YP.rotationDegrees(state.yaw)).mul(Axis.XP.rotationDegrees(-state.pitch));
        Vector3f front = rotation.transform(new Vector3f(0.0F, 0.0F, 1.0F));
        Vec3 toCamera = camera.subtract(item).normalize();
        double dot = front.x() * toCamera.x + front.y() * toCamera.y + front.z() * toCamera.z;
        if (dot < 0.999) {
            throw new AssertionError("The floating item does not face " + which + ": its front points to " + front
                    + ", the camera lies in direction " + toCamera + " (yaw " + state.yaw + ", pitch " + state.pitch + ")");
        }
    }

    private static boolean aimsAtBundle(Minecraft client) {
        return client.hitResult instanceof BlockHitResult hit && client.hitResult.getType() == HitResult.Type.BLOCK
                && hit.getBlockPos().equals(BUNDLE);
    }

    private static PlacedBundleBlockEntity bundle(Minecraft client) {
        return client.level != null && client.level.getBlockEntity(BUNDLE) instanceof PlacedBundleBlockEntity be ? be : null;
    }

    private static String serverBundleIndex(Minecraft client) {
        MinecraftServer server = client.getSingleplayerServer();
        return server != null && server.overworld().getBlockEntity(BUNDLE) instanceof PlacedBundleBlockEntity be
                ? "shows item " + be.shownIndex() : "has no bundle";
    }

    private static ServerPlayer serverPlayer(Minecraft client) {
        MinecraftServer server = client.getSingleplayerServer();
        if (server == null || server.getPlayerList().getPlayers().size() != 1) {
            throw new AssertionError("Expected exactly one player on an integrated server");
        }
        return server.getPlayerList().getPlayers().get(0);
    }

    /** Server-Arbeit gehoert auf den Server-Thread; der naechste Schritt wartet auf ihr Ergebnis. */
    private static void onServer(Script script, String name, Consumer<MinecraftServer> work) {
        script.act(name, client -> {
            MinecraftServer server = client.getSingleplayerServer();
            if (server == null) {
                throw new AssertionError("There is no integrated server");
            }
            server.execute(() -> work.accept(server));
        });
    }
}
