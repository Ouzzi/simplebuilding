package com.simplebuilding.clientgametest;

import com.simplebuilding.util.ExtraHead;
import com.simplebuilding.util.HeadLooks;
import java.util.function.Consumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.entity.EntityRenderer;
import net.minecraft.client.renderer.entity.state.AvatarRenderState;
import net.minecraft.client.renderer.entity.state.EntityRenderState;
import net.minecraft.core.component.DataComponentType;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.SkullBlock;

/**
 * Owner N55 (26.3): the head slot handles heads like the helmet slot. Pictures from the front: a cow head in the helmet
 * slot and the same head in the extra slot (same size, as big as the cow's own head), both slots full (the extra head's
 * model is drawn, the helmet slot's head is not), a cat head with a variant, a big head (ghast), a head under an iron
 * helmet. Asserted: the render state of the player shows no helmet-slot head while the extra slot holds one, and the
 * worn size the mob head look reports. Takes both heads off again.
 */
public final class HeadSlotClientTest {
    private HeadSlotClientTest() {
    }

    public static void inWorld(Script script) {
        TestScene.build(script, "minecraft:stone", "survival");
        script.command("execute as @a at @s run tp @s ~ ~ ~ 0 0");
        script.awaitPackets();
        script.act("look at the player from the front", client -> client.options.setCameraType(net.minecraft.client.CameraType.THIRD_PERSON_FRONT));

        wear(script, "a cow head in the helmet slot", head("cow_head"), ItemStack.EMPTY, ItemStack.EMPTY);
        script.act("the helmet slot's mod head is in the render state", client -> {
            AvatarRenderState state = renderState(client);
            if (!(state.wornHeadType instanceof SkullBlock.Type)) {
                throw new AssertionError("a cow head in the helmet slot is not in the render state");
            }
        });
        script.shot("n55-helmet-slot-cow");
        wear(script, "a cow head in the extra slot", ItemStack.EMPTY, head("cow_head"), ItemStack.EMPTY);
        script.shot("n55-extra-slot-cow");
        script.act("worn mob head sizes", client -> {
            float cow = edge(head("cow_head"));
            float ghast = edge(head("ghast_head"));
            float zombie = edge(new ItemStack(Items.ZOMBIE_HEAD));
            if (Math.abs(cow - 10.0F) > 0.01F || ghast < 12.0F || zombie != 0.0F) {
                throw new AssertionError("worn edges: cow " + cow + " (10 = the mob's own head), ghast " + ghast + ", Vanilla zombie " + zombie + " (0 = unchanged)");
            }
        });

        wear(script, "both slots: a skeleton skull in the helmet slot, a zombie head in the extra slot",
                new ItemStack(Items.SKELETON_SKULL), new ItemStack(Items.ZOMBIE_HEAD), ItemStack.EMPTY);
        script.act("the extra head has precedence in the render state", client -> {
            AvatarRenderState state = renderState(client);
            if (state.wornHeadType != null || !state.headItem.isEmpty()) {
                throw new AssertionError("the helmet slot's head is still drawn next to the extra head: " + state.wornHeadType);
            }
        });
        script.shot("n55-both-slots-extra-wins");
        wear(script, "only the skeleton skull in the helmet slot", new ItemStack(Items.SKELETON_SKULL), ItemStack.EMPTY, ItemStack.EMPTY);
        script.act("the helmet slot's head is drawn again without an extra head", client -> {
            if (renderState(client).wornHeadType != SkullBlock.Types.SKELETON) {
                throw new AssertionError("the skeleton skull left the render state with nothing in the extra slot");
            }
        });
        script.shot("n55-helmet-slot-skeleton");

        ItemStack siamese = head("cat_head");
        setVariant(siamese, "siamese");
        wear(script, "a Siamese cat head in the extra slot", ItemStack.EMPTY, siamese, ItemStack.EMPTY);
        script.shot("n55-extra-slot-cat-siamese");
        wear(script, "a ghast head in the extra slot", ItemStack.EMPTY, head("ghast_head"), ItemStack.EMPTY);
        script.shot("n55-extra-slot-ghast");
        wear(script, "a silverfish head in the extra slot", ItemStack.EMPTY, head("silverfish_head"), ItemStack.EMPTY);
        script.shot("n55-extra-slot-silverfish");
        wear(script, "an iron helmet alone", ItemStack.EMPTY, ItemStack.EMPTY, new ItemStack(Items.IRON_HELMET));
        script.shot("n55-iron-helmet-alone");
        wear(script, "a zombie head under an iron helmet", ItemStack.EMPTY, new ItemStack(Items.ZOMBIE_HEAD), new ItemStack(Items.IRON_HELMET));
        script.shot("n55-extra-slot-zombie-iron-helmet");
        wear(script, "a cow head under an iron helmet", ItemStack.EMPTY, head("cow_head"), new ItemStack(Items.IRON_HELMET));
        script.shot("n55-extra-slot-cow-iron-helmet");

        wear(script, "nothing", ItemStack.EMPTY, ItemStack.EMPTY, ItemStack.EMPTY);
        script.act("back to first person", client -> client.options.setCameraType(net.minecraft.client.CameraType.FIRST_PERSON));
    }

    private static ItemStack head(String path) {
        Item item = BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplemobheads", path));
        return new ItemStack(item);
    }

    @SuppressWarnings("unchecked")
    private static void setVariant(ItemStack stack, String variant) {
        DataComponentType<String> type = (DataComponentType<String>) BuiltInRegistries.DATA_COMPONENT_TYPE.getValue(Identifier.fromNamespaceAndPath("simplemobheads", "variant"));
        stack.set(type, variant);
    }

    private static float edge(ItemStack head) {
        if (head.getItem() instanceof BlockItem item && item.getBlock() instanceof AbstractSkullBlock skull) {
            return HeadLooks.skullEdge(skull.getType());
        }
        return 0.0F;
    }

    @SuppressWarnings({"rawtypes", "unchecked"})
    private static AvatarRenderState renderState(Minecraft client) {
        EntityRenderer renderer = client.getEntityRenderDispatcher().getRenderer(client.player);
        EntityRenderState state = renderer.createRenderState();
        renderer.extractRenderState(client.player, state, 1.0F);
        return (AvatarRenderState) state;
    }

    /** Sets the helmet slot, the extra slot and (an iron helmet) the helmet slot over a head; waits until the client sees it. */
    private static void wear(Script script, String what, ItemStack helmetSlot, ItemStack extra, ItemStack armor) {
        onServer(script, "wear " + what, server -> {
            ServerPlayer player = server.getPlayerList().getPlayers().getFirst();
            player.setItemSlot(EquipmentSlot.HEAD, armor.isEmpty() ? helmetSlot.copy() : armor.copy());
            ExtraHead.set(player, extra.copy());
        });
        script.awaitPackets();
        script.await("the client sees " + what, 100, client -> {
            ItemStack seen = ExtraHead.get(client.player);
            return ItemStack.matches(seen, extra) && ItemStack.isSameItem(client.player.getItemBySlot(EquipmentSlot.HEAD), armor.isEmpty() ? helmetSlot : armor);
        }, client -> "the client's extra head is " + ExtraHead.get(client.player) + ", helmet slot " + client.player.getItemBySlot(EquipmentSlot.HEAD));
        script.idle("let " + what + " settle", 20);
    }

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
