package com.simplebuilding.modules.simpletweaks.deed;

import com.simplebuilding.modules.simpletweaks.claims.ClaimCode;
import com.simplebuilding.modules.simpletweaks.claims.ClaimRights;
import com.simplebuilding.modules.simpletweaks.claims.ClaimStore;
import com.simplebuilding.modules.simpletweaks.claims.Claims;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedClaimPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedEditPayload;
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

    private DeedService() {}

    private static InteractionHand handOf(int hand) { return hand == 1 ? InteractionHand.OFF_HAND : InteractionHand.MAIN_HAND; }

    /** The deed in {@code hand} (0 main, 1 off), or empty. */
    static ItemStack deedIn(ServerPlayer player, int hand) {
        if (hand < 0 || hand > 1) return ItemStack.EMPTY;
        ItemStack stack = player.getItemInHand(handOf(hand));
        return stack.is(ClaimDeedItem.item()) ? stack : ItemStack.EMPTY;
    }

    /** True if this packet comes too soon after the previous one of the player (dropped). */
    static boolean throttled(ServerPlayer player) {
        int now = player.level().getServer().getTickCount();
        Integer last = LAST.put(player.getUUID(), now);
        if (LAST.size() > 4096) LAST.keySet().retainAll(player.level().getServer().getPlayerList().getPlayers().stream().map(ServerPlayer::getUUID).toList());
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

    static DeedOpenPayload snapshot(ServerPlayer player, int hand, ItemStack stack) {
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
                claims.config.maxClaimsPerPlayer(), claims.config.maxTrustedPlayers(), chunk.x(), chunk.z(), dimension, note, online, new ArrayList<>(known));
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
        boolean success = Claims.get(player.level().getServer()).claim(player);
        if (success) bind(player, stack);
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
