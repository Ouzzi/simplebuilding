package com.simplebuilding.guide;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.items.custom.GuideBookItem;
import com.simplebuilding.networking.GuideStatePayload;
import com.simplebuilding.platform.PlatformServices;
import com.simplebuilding.version.McVersion;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.item.ItemStack;

/**
 * Which guide tabs ({@link GuideTabs}) are open for a player (26.3, plan P5/P6).
 *
 * <p>Server: a {@link GuideTabs.Access#RECIPES} tab opens as soon as one of its gate recipes is in
 * the player's recipe book. {@link #refresh} checks that whenever recipes are awarded (every unlock
 * path, from recipe advancements to crafting and {@code /recipe give}, runs through
 * {@code ServerPlayer#awardRecipes}, hooked in {@code GuideRecipeUnlockMixin}), at login and when a
 * guide is used. An opened tab is remembered as a player entity tag, which survives death and
 * relogging, so it stays open even when the recipe is taken away; its advancement (if any) is awarded
 * too, for quests and the advancement screen. Nothing is consumed, nothing has to be clicked.
 *
 * <p>Migration: tabs inserted into a book the old way ({@code simplebuilding:guide_chapters}) and
 * tabs whose advancement the player already has open for the player who reads or carries that book;
 * the component is then removed, so each copy shows its reader's own state.
 *
 * <p>Client: {@link #receive} keeps what the server sent ({@link GuideStatePayload}); the screen and
 * the tooltip read {@link #clientOpen}.
 */
public final class GuideUnlocks {
    /** Prefix of the entity tag that remembers an opened tab: {@code simplebuilding.guide_tab.<namespace>.<path>}. */
    public static final String TAG_PREFIX = "simplebuilding.guide_tab.";

    private static final Map<ServerPlayer, List<Identifier>> SENT = new WeakHashMap<>();
    private static volatile Set<Identifier> client = Set.of();
    private static volatile int clientVersion;

    private GuideUnlocks() {
    }

    public static String tag(GuideTabs.Tab tab) {
        return TAG_PREFIX + tab.id().getNamespace() + "." + tab.id().getPath().replace('/', '.');
    }

    // =====================================================================================
    // Server
    // =====================================================================================

    /** Whether this tab is open for this player right now (no side effects). */
    public static boolean isOpen(ServerPlayer player, GuideTabs.Tab tab) {
        return switch (tab.access()) {
            case ALWAYS -> true;
            case OPERATOR -> GuideBooks.isOperator(player);
            case RECIPES -> player.entityTags().contains(tag(tab)) || advancementDone(player, tab) || knowsGate(player, tab);
        };
    }

    private static boolean knowsGate(ServerPlayer player, GuideTabs.Tab tab) {
        for (var key : tab.gateKeys()) if (player.getRecipeBook().contains(key)) return true;
        return false;
    }

    private static boolean advancementDone(ServerPlayer player, GuideTabs.Tab tab) {
        if (tab.advancement() == null || player.level().getServer() == null) return false;
        var holder = player.level().getServer().getAdvancements().get(tab.advancement());
        return holder != null && player.getAdvancements().getOrStartProgress(holder).isDone();
    }

    /**
     * Opens every tab whose condition holds, remembers it and tells the client if anything changed.
     * {@code announce}: a newly opened tab turns a page audibly (not at login).
     */
    public static List<Identifier> refresh(ServerPlayer player, boolean announce) {
        if (!McVersion.MEGA_GUIDES) return List.of();
        List<Identifier> open = new ArrayList<>();
        boolean fresh = false;
        for (GuideTabs.Tab tab : GuideTabs.all()) {
            if (!isOpen(player, tab)) continue;
            open.add(tab.id());
            if (tab.access() == GuideTabs.Access.RECIPES && player.addTag(tag(tab))) fresh = true;
            award(player, tab);
        }
        if (fresh && announce) {
            player.level().playSound(null, player.blockPosition(), SoundEvents.BOOK_PAGE_TURN, SoundSource.PLAYERS, 1.0F, 1.0F);
        }
        if (!open.equals(SENT.get(player))) send(player, open);
        return open;
    }

    private static void award(ServerPlayer player, GuideTabs.Tab tab) {
        if (tab.advancement() == null || player.level().getServer() == null) return;
        var holder = player.level().getServer().getAdvancements().get(tab.advancement());
        if (holder == null || player.getAdvancements().getOrStartProgress(holder).isDone()) return;
        for (String criterion : holder.value().criteria().keySet()) player.getAdvancements().award(holder, criterion);
    }

    private static void send(ServerPlayer player, List<Identifier> open) {
        if (player.connection != null && PlatformServices.canSendToPlayer(player, GuideStatePayload.ID)) {
            PlatformServices.sendToPlayer(player, new GuideStatePayload(open));
            SENT.put(player, List.copyOf(open));
        }
    }

    /** What the server last sent this player (tests). */
    public static List<Identifier> sent(ServerPlayer player) {
        return SENT.getOrDefault(player, List.of());
    }

    /** Old book masks: every chapter inserted into this book opens for this player; the book forgets the mask. */
    public static void migrate(ServerPlayer player, ItemStack stack) {
        if (!McVersion.MEGA_GUIDES || !(stack.getItem() instanceof GuideBookItem)) return;
        int mask = GuideBooks.mask(stack);
        if (mask == 0) return;
        for (GuideBooks.Book book : GuideBooks.Book.topics()) {
            GuideTabs.Tab tab = GuideBooks.tab(book);
            if ((mask & (1 << book.ordinal())) != 0 && tab != null && tab.access() == GuideTabs.Access.RECIPES) player.addTag(tag(tab));
        }
        stack.remove(ModDataComponentTypes.GUIDE_CHAPTERS);
    }

    /** At login: migrate the books the player carries, then open and send. */
    public static void onJoin(ServerPlayer player) {
        if (!McVersion.MEGA_GUIDES) return;
        var inventory = player.getInventory();
        for (int slot = 0; slot < inventory.getContainerSize(); slot++) migrate(player, inventory.getItem(slot));
        SENT.remove(player);
        refresh(player, false);
    }

    /** A guide was used: migrate it and catch up (an /op since login opens Server Admin here). */
    public static void onUse(ServerPlayer player, ItemStack stack) {
        migrate(player, stack);
        refresh(player, true);
    }

    // =====================================================================================
    // Client
    // =====================================================================================

    /** Client thread: the server's list of open tabs. */
    public static void receive(List<Identifier> tabs) {
        client = Set.copyOf(tabs);
        clientVersion++;
    }

    public static boolean clientOpen(Identifier tab) {
        return client.contains(tab);
    }

    /** Changes whenever the server sends a new list (the screen relayouts then). */
    public static int clientVersion() {
        return clientVersion;
    }
}
