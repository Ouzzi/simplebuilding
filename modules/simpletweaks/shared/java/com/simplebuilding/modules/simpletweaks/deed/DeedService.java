package com.simplebuilding.modules.simpletweaks.deed;

import com.simplebuilding.modules.simpletweaks.claims.ClaimCode;
import com.simplebuilding.modules.simpletweaks.claims.ClaimRights;
import com.simplebuilding.modules.simpletweaks.claims.ClaimStore;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedActionPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedChunksPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedChunksRequest;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedClaimPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedEditPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedMap;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedResultPayload;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.item.ItemStack;

/**
 * Server side of the deed: what opens, what saves. <b>The client is never trusted</b>: the deed is re-read from the
 * player's hand, the owner comes from its component (display only), and the rights come from the ledger
 * ({@link Claims#applyCode}, which checks owner or manager, limits, cooldown and the whole code again).
 */
public final class DeedService {
    /** Minimum ticks between two deed packets of one player. */
    public static final int REQUEST_TICKS = 5;
    private static final Map<UUID,Integer> LAST = new HashMap<>();
    /** Chunk requests have their own clock: the map view asks right after an action answered. */
    private static final Map<UUID,Integer> LAST_CHUNKS = new HashMap<>();

    private DeedService() {}

    private static InteractionHand handOf(int hand) { return hand == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND; }

    /** The deed in {@code hand} (0 main, 1 off), or empty. */
    static ItemStack deedIn(ServerPlayer player, int hand) {
        if (hand < 0 || hand > 1) return ItemStack.EMPTY;
        ItemStack stack = player.getItemInHand(handOf(hand));
        return stack.is(ClaimDeedItem.item()) ? stack : ItemStack.EMPTY;
    }

    /** Tests only: forget the packet times (a game test sends several packets in one tick). */
    public static void resetThrottle() { LAST.clear(); LAST_CHUNKS.clear(); }

    /** True if this packet comes too soon after the previous one of the player (dropped). */
    static boolean throttled(ServerPlayer player) { return throttled(player, LAST); }

    private static boolean throttled(ServerPlayer player, Map<UUID,Integer> clock) {
        int now = player.level().getServer().getTickCount();
        Integer last = clock.put(player.getUUID(), now);
        if (clock.size() > 4096) clock.keySet().retainAll(player.level().getServer().getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).toList());
        return last != null && now >= last && now - last < REQUEST_TICKS;
    }

    /** An unbound deed, or one bound to a player who holds no land (any more): it claims. */
    static boolean claims(Claims claims, ServerPlayer player, DeedData data) {
        return data == null || (data.owner().equals(player.getUUID()) && !claims.hasLand(data.owner()));
    }

    /** Opens the screen for the deed in {@code hand}; false if the player's client cannot show it. */
    public static boolean open(ServerPlayer player, int hand) {
        var server = player.level().getServer();
        if (!Claims.enabled(server)) return false;
        ItemStack stack = deedIn(player, hand);
        if (stack.isEmpty()) return false;
        return DeedNet.send(player, snapshot(player, hand, stack));
    }

    static DeedOpenPayload snapshot(ServerPlayer player, int hand, ItemStack stack) { return snapshot(player, hand, stack, ""); }

    static DeedOpenPayload snapshot(ServerPlayer player, int hand, ItemStack stack, String result) {
        var server = player.level().getServer();
        var claims = Claims.get(server);
        DeedData data = stack.get(DeedData.TYPE);
        var chunk = player.chunkPosition();
        String dimension = player.level().dimension().identifier().toString();
        UUID me = player.getUUID();
        int mode = DeedOpenPayload.READ;
        String note = "", code = "";
        UUID ownerId = data == null ? me : data.owner();
        String ownerName = data == null ? player.getPlainTextName() : data.ownerName();
        int rights = 0;
        var land = claims.lands().get(ownerId);
        if (claims.locked()) {
            note = "locked";
        } else if (claims(claims, player, data)) {
            mode = DeedOpenPayload.CLAIM;
            ownerId = me;
            ownerName = player.getPlainTextName();
        } else if (!claims.hasLand(ownerId)) {
            note = "no_land";
        } else {
            String live = Claims.nameOf(server, ownerId);
            if (live != null && live.length() <= DeedData.MAX_NAME) ownerName = live;
            boolean manager = me.equals(ownerId) || (land != null && (land.top().of(me) & ClaimRights.MANAGE) != 0);
            var key = new ClaimStore.Key(dimension, chunk.pack());
            var here = claims.view().get(key);
            rights = me.equals(ownerId) ? ClaimRights.ALL
                    : here != null && here.owner().equals(ownerId) && land != null ? land.rights(key, me) : land == null ? 0 : land.top().of(me);
            if (manager) {
                String shown = claims.editorCode(server, ownerId);
                if (shown == null) note = "too_complex";
                else { mode = DeedOpenPayload.EDIT; code = shown; }
            }
        }
        var online = new ArrayList<String>();
        for (var other : server.getPlayerList().getPlayers()) {
            String name = other.getPlainTextName();
            if (online.size() < 64 && !other.getUUID().equals(ownerId) && !other.getUUID().equals(me) && plain(name)) online.add(name);
        }
        var known = new LinkedHashSet<String>();
        if (land != null && mode == DeedOpenPayload.EDIT) {
            var ids = new LinkedHashSet<UUID>(land.top().members().keySet());
            land.sections().values().forEach(r -> ids.addAll(r.members().keySet()));
            for (var id : ids) {
                String name = Claims.nameOf(server, id);
                if (name != null && plain(name) && !online.contains(name) && known.size() < 64) known.add(name);
            }
        }
        return new DeedOpenPayload(mode, hand, ownerId, ownerName, code, claims.revision(ownerId), rights, claims.chunksOf(ownerId),
                claims.budget(ownerId), claims.config.maxTrustedPlayers(), chunk.x(), chunk.z(), dimension, note, online, new ArrayList<>(known),
                mode == DeedOpenPayload.CLAIM || claims.locked() ? DeedMap.NONE : mapOf(player, claims, ownerId, result));
    }

    /** Any right of {@code actor} in the land of {@code owner} (the owner has all): may see the land's chunks (C7). */
    static boolean seesLand(Claims claims, UUID owner, UUID actor) {
        if (owner.equals(actor)) return true;
        var land = claims.lands().get(owner);
        if (land == null) return false;
        if (land.top().of(actor) != 0) return true;
        for (var rules : land.sections().values()) if (rules.of(actor) != 0) return true;
        return false;
    }

    /** The map part of the snapshot (SP22-8 §4.5): nothing for a holder without rights. */
    static DeedMap mapOf(ServerPlayer player, Claims claims, UUID owner, String result) {
        UUID me = player.getUUID();
        if (!claims.hasLand(owner) || !seesLand(claims, owner, me)) return DeedMap.NONE;
        boolean owns = owner.equals(me);
        var counts = new java.util.TreeMap<String,Integer>();
        claims.view().forEach((k, c) -> { if (c.owner().equals(owner)) counts.merge(k.dimension(), 1, Integer::sum); });
        var tabs = new ArrayList<DeedMap.Tab>();
        for (String vanilla : com.simplebuilding.modules.simpletweaks.claims.ClaimConfig.LEGACY_DIMENSIONS) tabs.add(new DeedMap.Tab(vanilla, counts.getOrDefault(vanilla, 0)));
        counts.forEach((d, n) -> { if (!com.simplebuilding.modules.simpletweaks.claims.ClaimConfig.LEGACY_DIMENSIONS.contains(d) && tabs.size() < DeedMap.MAX_TABS) tabs.add(new DeedMap.Tab(d, n)); });
        var standing = new ClaimStore.Key(player.level().dimension().identifier().toString(), player.chunkPosition().pack());
        var at = claims.view().get(standing);
        int here = at != null ? (at.owner().equals(owner) ? DeedMap.HERE_OWN : DeedMap.HERE_NO) : owns && claims.claimKey(player) != null ? DeedMap.HERE_FREE : DeedMap.HERE_NO;
        int buy = DeedMap.OFF, next = 0, levels = 0;
        var costs = new ArrayList<DeedMap.Cost>();
        if (owns && claims.config.expansionEnabled()) {
            int budget = claims.budget(owner);
            var tier = claims.config.expansionTiers().stream().filter(t -> t.chunks() > budget).findFirst().orElse(null);
            if (tier == null) buy = DeedMap.MAX;
            else if (tier.chunks() > claims.config.maxClaimsPerPlayer()) buy = DeedMap.LIMIT;
            else {
                buy = DeedMap.BUY; next = tier.chunks(); levels = tier.levels();
                tier.items().stream().limit(DeedMap.MAX_COSTS).forEach(c -> costs.add(new DeedMap.Cost(c.id(), c.count())));
            }
        }
        boolean maps = com.simplebuilding.modules.simpletweaks.claims.ClaimMaps.present();
        return new DeedMap(true, owns, owns ? claims.credits(owner) : 0, here, buy, next, levels, costs, maps, tabs, result.length() > 32 ? result.substring(0, 32) : result);
    }

    /**
     * Map view: the chunks of the deed owner's land in one dimension, with pictures, in pages of 64. Only for the
     * owner or a holder with any right in the land (C7); never other owners' chunks. A request too soon is dropped.
     */
    public static void handleChunks(DeedChunksRequest payload, ServerPlayer player) {
        if (throttled(player, LAST_CHUNKS)) return;
        var server = player.level().getServer();
        if (!Claims.enabled(server)) return;
        ItemStack stack = deedIn(player, payload.hand());
        DeedData data = stack.isEmpty() ? null : stack.get(DeedData.TYPE);
        var claims = Claims.get(server);
        if (data == null || claims.locked() || !seesLand(claims, data.owner(), player.getUUID())) return;
        var chunks = new ArrayList<Long>();
        claims.view().forEach((k, c) -> { if (c.owner().equals(data.owner()) && k.dimension().equals(payload.dimension())) chunks.add(k.chunk()); });
        chunks.sort(java.util.Comparator.comparingInt((Long c) -> net.minecraft.world.level.ChunkPos.getZ(c)).thenComparingInt(c -> net.minecraft.world.level.ChunkPos.getX(c)));
        int max = DeedChunksPayload.PAGE * DeedChunksPayload.MAX_PAGES;
        if (chunks.size() > max) chunks.subList(max, chunks.size()).clear();
        var pictures = ClaimPictures.get(server);
        long now = server.overworld().getGameTime();
        int pages = Math.max(1, (chunks.size() + DeedChunksPayload.PAGE - 1) / DeedChunksPayload.PAGE);
        for (int page = 0; page < pages; page++) {
            var tiles = new ArrayList<DeedChunksPayload.Tile>();
            for (long c : chunks.subList(page * DeedChunksPayload.PAGE, Math.min(chunks.size(), (page + 1) * DeedChunksPayload.PAGE))) {
                var picture = pictures == null ? null : pictures.picture(new ClaimStore.Key(payload.dimension(), c));
                int age = picture == null ? DeedChunksPayload.NO_PICTURE : (int) Math.max(0, Math.min(Integer.MAX_VALUE, now - picture.time()));
                tiles.add(new DeedChunksPayload.Tile(net.minecraft.world.level.ChunkPos.getX(c), net.minecraft.world.level.ChunkPos.getZ(c), age,
                        picture == null ? new byte[0] : picture.pixels()));
            }
            if (!DeedNet.send(player, new DeedChunksPayload(payload.dimension(), page, pages, tiles))) return;
        }
    }

    /**
     * A button of the map view: claim here (credit or buy), buy ahead, give up a chunk. Only the owner holding their own
     * deed. Every outcome answers with a fresh snapshot naming the result, plus the usual cue.
     */
    public static void handleAction(DeedActionPayload payload, ServerPlayer player) {
        if (throttled(player)) return;
        var server = player.level().getServer();
        if (!Claims.enabled(server)) return;
        ItemStack stack = deedIn(player, payload.hand());
        DeedData data = stack.isEmpty() ? null : stack.get(DeedData.TYPE);
        if (data == null || !data.owner().equals(player.getUUID())) return;
        var claims = Claims.get(server);
        boolean ok;
        String result;
        switch (payload.action()) {
            case DeedActionPayload.CLAIM_HERE -> {
                var key = claims.claimKey(player);
                var r = claims.buyAndClaim(player);
                ok = r == Claims.Expand.OK;
                result = "claim." + r.name().toLowerCase(java.util.Locale.ROOT);
                if (ok && key != null) ClaimPictures.takeNow(player.level(), key);
            }
            case DeedActionPayload.BUY -> {
                var r = claims.expand(player);
                ok = r == Claims.Expand.OK;
                result = "buy." + r.name().toLowerCase(java.util.Locale.ROOT);
            }
            case DeedActionPayload.UNCLAIM -> {
                ClaimStore.Key key;
                try { key = new ClaimStore.Key(payload.dimension(), payload.chunk()); } catch (IllegalArgumentException e) { return; }
                ok = claims.unclaim(player.getUUID(), key, server.overworld().getGameTime());
                result = ok ? "unclaim.ok" : claims.cooldownLeft(player.getUUID(), server.overworld().getGameTime()) > 0 ? "unclaim.cooldown" : "unclaim.failed";
            }
            default -> { return; }
        }
        cue(player, ok);
        DeedNet.send(player, snapshot(player, payload.hand(), stack, result));
    }

    private static boolean plain(String name) {
        if (name.isEmpty() || name.length() > 16) return false;
        for (int i = 0; i < name.length(); i++) {
            char c = name.charAt(i);
            if (!(c >= 'a' && c <= 'z' || c >= 'A' && c <= 'Z' || c >= '0' && c <= '9' || c == '_')) return false;
        }
        return true;
    }

    /** Save from the editor. Always answers; a refused save changes nothing. */
    public static void handleEdit(DeedEditPayload payload, ServerPlayer player) {
        if (throttled(player)) return;
        var server = player.level().getServer();
        ItemStack stack = deedIn(player, payload.hand());
        DeedData data = stack.isEmpty() ? null : stack.get(DeedData.TYPE);
        if (!Claims.enabled(server)) { reply(player, false, 0, List.of(new ClaimCode.Error(0, "off", ""))); return; }
        var claims = Claims.get(server);
        if (data == null) { reply(player, false, 0, List.of(new ClaimCode.Error(0, "no_deed", ""))); return; }
        if (payload.revision() != claims.revision(data.owner())) {
            reply(player, false, claims.revision(data.owner()), List.of(new ClaimCode.Error(0, "stale", "")));
            return;
        }
        var result = claims.applyCode(player, data.owner(), payload.code());
        if (result.ok()) player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP,
                net.minecraft.sounds.SoundSource.PLAYERS, .4f, 1.2f);
        reply(player, result.ok(), claims.revision(data.owner()), result.errors());
    }

    private static void reply(ServerPlayer player, boolean ok, int revision, List<ClaimCode.Error> errors) {
        var list = new ArrayList<DeedResultPayload.Err>();
        for (var e : errors) {
            if (list.size() >= ClaimCode.MAX_ERRORS) break;
            list.add(new DeedResultPayload.Err(Math.max(0, e.line()), e.key().length() > 32 ? e.key().substring(0, 32) : e.key(),
                    e.arg().length() > 64 ? e.arg().substring(0, 64) : e.arg()));
        }
        DeedNet.send(player, new DeedResultPayload(ok, revision, list));
    }

    /** Claim from the confirmation sheet. */
    public static void handleClaim(DeedClaimPayload payload, ServerPlayer player) {
        if (throttled(player) || !Claims.enabled(player.level().getServer())) return;
        ItemStack stack = deedIn(player, payload.hand());
        if (stack.isEmpty() || !claims(Claims.get(player.level().getServer()), player, stack.get(DeedData.TYPE))) return;
        claimAndBind(player, payload.hand(), stack);
    }

    /** Right-click without a usable screen: an unbound deed claims at once, a bound one does nothing. */
    public static boolean useWithoutScreen(ServerPlayer player, int hand) {
        ItemStack stack = deedIn(player, hand);
        var claims = Claims.get(player.level().getServer());
        if (stack.isEmpty() || claims == null || !claims(claims, player, stack.get(DeedData.TYPE))) { cue(player, false); return false; }
        return claimAndBind(player, hand, stack);
    }

    private static boolean claimAndBind(ServerPlayer player, int hand, ItemStack stack) {
        var claims = Claims.get(player.level().getServer());
        var key = claims.claimKey(player);
        boolean success = claims.claim(player);
        if (success) { bind(player, stack); if (key != null) ClaimPictures.takeNow(player.level(), key); }
        cue(player, success);
        return success;
    }

    /** Binds one deed of the stack to the player; an old stack of several is split (the bound one goes to the inventory). */
    private static void bind(ServerPlayer player, ItemStack stack) {
        var data = new DeedData(player.getUUID(), player.getPlainTextName().length() > DeedData.MAX_NAME ? player.getPlainTextName().substring(0, DeedData.MAX_NAME) : player.getPlainTextName(), 0);
        if (stack.getCount() <= 1) { stack.set(DeedData.TYPE, data); return; }
        ItemStack one = stack.split(1);
        one.set(DeedData.TYPE, data);
        if (!player.getInventory().add(one)) player.drop(one, false, net.minecraft.util.Prediction.SERVER_ONLY);
    }

    private static void cue(ServerPlayer player, boolean success) {
        // Same cue family as SimpleBuilding and the other modules: failure is the dry dispenser click.
        if (success) player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.EXPERIENCE_ORB_PICKUP, net.minecraft.sounds.SoundSource.PLAYERS, .4f, 1f);
        else player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.DISPENSER_FAIL, net.minecraft.sounds.SoundSource.PLAYERS, .5f, 1.2f);
        player.level().sendParticles(success ? net.minecraft.core.particles.ParticleTypes.HAPPY_VILLAGER : net.minecraft.core.particles.ParticleTypes.SMOKE,
                player.getX(), player.getY() + 1, player.getZ(), success ? 6 : 4, .3, .3, .3, 0);
    }
}
