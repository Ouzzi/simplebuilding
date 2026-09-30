package com.simplebuilding.guide;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.custom.GuideBookItem;
import com.simplebuilding.version.McVersion;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.resources.Identifier;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/** Server-issued reading session bound to the exact held stack, never to a client-supplied slot. */
public final class GuideUnlocks {
    private record Session(InteractionHand hand, ItemStack stack) {}
    private static final Map<ServerPlayer, Session> SESSIONS = new WeakHashMap<>();
    private GuideUnlocks() {}

    public static void open(ServerPlayer player, InteractionHand hand) {
        ItemStack stack = player.getItemInHand(hand);
        if (McVersion.MEGA_GUIDES && stack.getItem() instanceof GuideBookItem)
            SESSIONS.put(player, new Session(hand, stack));
    }

    public static boolean unlock(ServerPlayer player, int ordinal) {
        if (!McVersion.MEGA_GUIDES || ordinal < 0 || ordinal >= GuideBooks.Book.values().length) return false;
        Session session = SESSIONS.get(player);
        if (session == null || !player.isAlive() || player.containerMenu != player.inventoryMenu
                || player.getItemInHand(session.hand()) != session.stack()) return false;
        ItemStack stack = session.stack();
        if (!(stack.getItem() instanceof GuideBookItem guide)) return false;
        GuideBooks.Book topic = GuideBooks.Book.values()[ordinal];
        if (topic.isHub() || topic.shelf() != guide.book().shelf() || GuideBooks.inserted(stack, topic)
                || (GuideBooks.operatorOnly(topic) && !GuideBooks.isOperator(player))) return false;
        ItemStack key = ItemStack.EMPTY;
        for (int slot = 0; slot < player.getInventory().getContainerSize(); slot++) {
            ItemStack candidate = player.getInventory().getItem(slot);
            if (candidate.is(GuideBooks.keyItem(topic).asItem())) { key = candidate; break; }
        }
        if (key.isEmpty() && player.getOffhandItem().is(GuideBooks.keyItem(topic).asItem())) key = player.getOffhandItem();
        if (key.isEmpty()) return false;
        // Creative follows the same explicit exchange: a chapter always costs one real item.
        key.shrink(1);
        stack.set(ModDataComponentTypes.GUIDE_CHAPTERS, GuideBooks.mask(stack) | (1 << ordinal));
        player.getInventory().setChanged();
        player.inventoryMenu.broadcastChanges();
        player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        var advancement = player.level().getServer().getAdvancements().get(Identifier.fromNamespaceAndPath("simplebuilding", "guides/" + topic.itemName().substring("guide_book_".length())));
        if (advancement != null) player.getAdvancements().award(advancement, "unlocked");
        return true;
    }
}
