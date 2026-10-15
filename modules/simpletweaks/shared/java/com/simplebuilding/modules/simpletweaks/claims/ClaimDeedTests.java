package com.simplebuilding.modules.simpletweaks.claims;

import com.simplebuilding.modules.simpletweaks.claims.ClaimRights.Rules;
import com.simplebuilding.modules.simpletweaks.deed.ClaimDeedItem;
import com.simplebuilding.modules.simpletweaks.deed.DeedCloningRecipe;
import com.simplebuilding.modules.simpletweaks.deed.DeedData;
import com.simplebuilding.modules.simpletweaks.deed.DeedService;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedClaimPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedEditPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedNet;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedOpenPayload;
import com.simplebuilding.modules.simpletweaks.deed.net.DeedResultPayload;
import io.netty.buffer.Unpooled;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.level.ChunkPos;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimRightsTests.roomy;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimRightsTests.seeded;

/** Phase 2 of the claim deed plan: the bound deed, the editor's server side (open, save, refusals) and copying. */
public final class ClaimDeedTests {
    private ClaimDeedTests() {}

    /** Records what the server sends while the body runs; {@code client} = whether the player's client can receive. */
    private static void sending(boolean client, List<CustomPacketPayload> sent, Runnable body) {
        var old = DeedNet.toClient;
        DeedNet.toClient = (p, payload) -> { if (client) sent.add(payload); return client; };
        try { body.run(); } finally { DeedNet.toClient = old; DeedService.resetThrottle(); }
    }

    private static ItemStack deed() { return new ItemStack(ClaimDeedItem.item()); }
    private static ItemStack boundDeed(UUID owner, String name, int generation) {
        var s = deed(); s.set(DeedData.TYPE, new DeedData(owner, name, generation)); return s;
    }
    private static DeedOpenPayload open(List<CustomPacketPayload> sent) { return (DeedOpenPayload) sent.get(sent.size() - 1); }
    private static DeedResultPayload result(List<CustomPacketPayload> sent) { return (DeedResultPayload) sent.get(sent.size() - 1); }
    private static BlockPos spot(GameTestHelper h) {
        var base = h.absolutePos(new BlockPos(2, 3, 2));
        return new BlockPos((base.getX() >> 4) * 16 + 16, base.getY(), (base.getZ() >> 4) * 16 + 8);
    }
    private static ClaimStore.Key keyAt(GameTestHelper h, BlockPos pos) { return new ClaimStore.Key(h.getLevel().dimension().identifier().toString(), ChunkPos.pack(pos)); }

    /** Item rules, codecs and packets: stack size 1, data validation, bounded codecs. */
    public static void component(GameTestHelper h) {
        var item = ClaimDeedItem.item();
        yes(h, new ItemStack(item).getMaxStackSize() == 1, "Deeds do not stack");
        var data = new DeedData(UUID.randomUUID(), "Ouzzi", 0);
        var stack = boundDeed(data.owner(), data.ownerName(), 0);
        var ops = h.getLevel().registryAccess().createSerializationContext(net.minecraft.nbt.NbtOps.INSTANCE);
        var loaded = ItemStack.CODEC.parse(ops, ItemStack.CODEC.encodeStart(ops, stack).getOrThrow()).getOrThrow();
        yes(h, data.equals(loaded.get(DeedData.TYPE)), "The binding survives a save and load");
        for (var bad : List.<java.util.function.Supplier<DeedData>>of(() -> new DeedData(UUID.randomUUID(), "x".repeat(17), 0), () -> new DeedData(UUID.randomUUID(), "A", 2), () -> new DeedData(UUID.randomUUID(), "A", -1), () -> new DeedData(null, "A", 0))) {
            boolean threw = false;
            try { bad.get(); } catch (IllegalArgumentException e) { threw = true; }
            yes(h, threw, "Invalid deed data is refused");
        }
        yes(h, data.copyable() && !data.copy().copyable() && data.copy().generation() == 1, "An original can be copied once; a copy cannot");
        // bounded packets
        var buf = Unpooled.buffer();
        DeedEditPayload.CODEC.encode(buf, new DeedEditPayload(1, 7, "x".repeat(ClaimCode.MAX_CHARS)));
        var edit = DeedEditPayload.CODEC.decode(buf);
        yes(h, edit.hand() == 1 && edit.revision() == 7 && edit.code().length() == ClaimCode.MAX_CHARS, "Edit packet round trip");
        boolean refused = false;
        try { DeedEditPayload.CODEC.encode(Unpooled.buffer(), new DeedEditPayload(0, 0, "x".repeat(ClaimCode.MAX_CHARS + 1))); } catch (RuntimeException e) { refused = true; }
        yes(h, refused, "4097 characters cannot even be encoded");
        var wire = Unpooled.buffer();
        net.minecraft.network.codec.ByteBufCodecs.VAR_INT.encode(wire, 0); net.minecraft.network.codec.ByteBufCodecs.INT.encode(wire, 0);
        net.minecraft.network.FriendlyByteBuf fb = new net.minecraft.network.FriendlyByteBuf(wire); fb.writeUtf("y".repeat(ClaimCode.MAX_CHARS + 1), 100000);
        boolean decoderRefused = false;
        try { DeedEditPayload.CODEC.decode(wire); } catch (RuntimeException e) { decoderRefused = true; }
        yes(h, decoderRefused, "A hostile client's 4097 characters are refused when decoding");
        var many = new ArrayList<DeedResultPayload.Err>();
        for (int i = 0; i < ClaimCode.MAX_ERRORS + 1; i++) many.add(new DeedResultPayload.Err(i, "syntax", "a"));
        boolean tooMany = false;
        try { DeedResultPayload.CODEC.encode(Unpooled.buffer(), new DeedResultPayload(false, 0, many)); } catch (RuntimeException e) { tooMany = true; }
        yes(h, tooMany, "At most 16 error lines per answer");
        var open = new DeedOpenPayload(DeedOpenPayload.EDIT, 0, data.owner(), "Ouzzi", "# c\nx: U\n", -5, ClaimRights.ALL, 3, 16, 8, -2, 7, "minecraft:overworld", "", List.of("Steve"), List.of("Alex"));
        var ob = Unpooled.buffer(); DeedOpenPayload.CODEC.encode(ob, open);
        yes(h, DeedOpenPayload.CODEC.decode(ob).equals(open), "Open packet round trip");
        h.succeed();
    }

    /** Right-click: unbound claims (screen first, direct without a client), bound opens the editor for owner/manager and a reader view for the rest. */
    public static void use(GameTestHelper h) {
        with(h, roomy(), c -> {
            var pos = spot(h).offset(2048, 0, 2048); var owner = player(h, pos); var stranger = player(h, pos);
            var item = ClaimDeedItem.item();
            // a client with the mod: the unbound deed first shows the confirmation sheet, claims only on request
            var sent = new ArrayList<CustomPacketPayload>();
            owner.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(item, 3));
            sending(true, sent, () -> {
                yes(h, item.use(h.getLevel(), owner, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "Use opens the sheet");
                yes(h, sent.size() == 1 && open(sent).mode() == DeedOpenPayload.CLAIM && open(sent).hand() == 0 && c.view().isEmpty(), "Unbound deed: claim sheet, nothing claimed yet");
                DeedService.handleClaim(new DeedClaimPayload(0), owner);
            });
            yes(h, c.view().size() == 1 && c.view().values().iterator().next().owner().equals(owner.getUUID()), "The sheet's answer claims the chunk");
            var rest = owner.getItemInHand(InteractionHand.MAIN_HAND);
            yes(h, rest.getCount() == 2 && !rest.has(DeedData.TYPE), "An old stack of three: one deed is used, two stay unbound");
            ItemStack bound = null;
            for (int i = 0; i < owner.getInventory().getContainerSize(); i++) { var s = owner.getInventory().getItem(i); if (s.has(DeedData.TYPE)) bound = s; }
            yes(h, bound != null && bound.getCount() == 1 && bound.get(DeedData.TYPE).owner().equals(owner.getUUID()) && bound.get(DeedData.TYPE).generation() == 0, "The used deed is bound to its owner");
            // a forged claim request: a bound deed of somebody else cannot claim
            var forgedBy = player(h, pos.offset(32, 0, 0));
            forgedBy.setItemInHand(InteractionHand.MAIN_HAND, boundDeed(owner.getUUID(), "Ouzzi", 0));
            DeedService.resetThrottle(); DeedService.handleClaim(new DeedClaimPayload(0), forgedBy);
            yes(h, c.view().size() == 1, "A deed bound to another player never claims for the holder");
            DeedService.resetThrottle(); DeedService.handleClaim(new DeedClaimPayload(5), forgedBy);
            DeedService.resetThrottle(); DeedService.handleClaim(new DeedClaimPayload(-1), forgedBy);
            yes(h, c.view().size() == 1, "Hand indexes outside 0..1 do nothing");
            // owner opens the editor with the generated code
            owner.setItemInHand(InteractionHand.MAIN_HAND, bound);
            var seen = new ArrayList<CustomPacketPayload>();
            sending(true, seen, () -> yes(h, item.use(h.getLevel(), owner, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS, "Bound deed opens"));
            var o = open(seen);
            yes(h, o.mode() == DeedOpenPayload.EDIT && o.rights() == ClaimRights.ALL && o.code().contains("Claim rights") && o.revision() == c.revision(owner.getUUID())
                    && o.landChunks() == 1 && o.maxChunks() == c.config.maxClaimsPerPlayer() && o.maxPeople() == c.config.maxTrustedPlayers(), "Owner: editor with the generated code and the ledger revision");
            // a stranger with a copy only gets a reader view and never the code
            stranger.setItemInHand(InteractionHand.MAIN_HAND, boundDeed(owner.getUUID(), "Ouzzi", 1));
            seen.clear();
            sending(true, seen, () -> item.use(h.getLevel(), stranger, InteractionHand.MAIN_HAND));
            yes(h, open(seen).mode() == DeedOpenPayload.READ && open(seen).code().isEmpty() && open(seen).rights() == 0 && open(seen).known().isEmpty(), "Stranger: read-only view, no code, no names");
            // a reader with U and a manager
            var key = c.view().keySet().iterator().next();
            var reader = player(h, pos); var manager = player(h, pos);
            var rules = new Rules(0, Map.of(reader.getUUID(), ClaimRights.USE | ClaimRights.CONTAINER, manager.getUUID(), ClaimRights.USE | ClaimRights.MANAGE));
            var seededC = seeded(h, temp(), key, owner.getUUID(), rules, Map.of());
            reader.setItemInHand(InteractionHand.MAIN_HAND, boundDeed(owner.getUUID(), "Ouzzi", 0));
            manager.setItemInHand(InteractionHand.MAIN_HAND, boundDeed(owner.getUUID(), "Ouzzi", 1));
            seen.clear();
            sending(true, seen, () -> { item.use(h.getLevel(), reader, InteractionHand.MAIN_HAND); item.use(h.getLevel(), manager, InteractionHand.OFF_HAND); });
            yes(h, ((DeedOpenPayload) seen.get(0)).mode() == DeedOpenPayload.READ && ((DeedOpenPayload) seen.get(0)).rights() == (ClaimRights.USE | ClaimRights.CONTAINER)
                    && ((DeedOpenPayload) seen.get(0)).code().isEmpty(), "Reader: sees what he may do (U C) and nothing else");
            yes(h, seen.size() == 1, "A deed in the other hand is not opened by a main-hand use");
            seen.clear();
            sending(true, seen, () -> item.use(h.getLevel(), manager, InteractionHand.MAIN_HAND));
            yes(h, open(seen).mode() == DeedOpenPayload.EDIT && open(seen).code().contains(reader.getUUID().toString()), "Manager: editor, with the people of the land");
            // without a client mod: the old direct behavior
            var plain = player(h, pos.offset(-64, 0, 64));
            plain.setItemInHand(InteractionHand.MAIN_HAND, deed());
            sending(false, new ArrayList<>(), () -> {
                yes(h, item.use(h.getLevel(), plain, InteractionHand.MAIN_HAND) == InteractionResult.SUCCESS && seededC.view().size() == 2, "No client: an unbound deed claims at once");
                yes(h, plain.getItemInHand(InteractionHand.MAIN_HAND).get(DeedData.TYPE).owner().equals(plain.getUUID()), "... and binds itself");
                yes(h, item.use(h.getLevel(), plain, InteractionHand.MAIN_HAND) == InteractionResult.FAIL, "No client: a bound deed does nothing");
            });
            Claims.install(h.getLevel().getServer(), c);
        });
        h.succeed();
    }

    /** The editor's save: owner, names and UUIDs, stale revision, refusals with lines, forged deeds, hands, rate and cooldown. */
    public static void edit(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server);
        var pos = spot(h).offset(4096, 0, 4096); var key = keyAt(h, pos);
        var owner = player(h, pos); var manager = player(h, pos); var stranger = player(h, pos); var other = player(h, pos);
        UUID o = owner.getUUID(), m = manager.getUUID(), friend = UUID.randomUUID(), buddy = UUID.randomUUID();
        var sent = new ArrayList<CustomPacketPayload>();
        try {
            var root = temp();
            var c = seeded(h, root, key, o, new Rules(0, Map.of(m, ClaimRights.USE | ClaimRights.MANAGE | ClaimRights.CONTAINER)), Map.of());
            owner.setItemInHand(InteractionHand.MAIN_HAND, boundDeed(o, "Ouzzi", 0));
            int revision = c.revision(o);
            String code = "# mine\n*: U\n" + friend + ": BDU # builder\n" + m + ": UCM\n";
            sending(true, sent, () -> DeedService.handleEdit(new DeedEditPayload(0, revision, code), owner));
            var r = result(sent);
            yes(h, r.ok() && r.errors().isEmpty() && r.revision() == c.revision(o) && r.revision() != revision, "Owner saves: ok, new revision");
            yes(h, c.rights(key.dimension(), friend, key.chunk()) == (ClaimRights.BUILD | ClaimRights.DESTROY | ClaimRights.USE) && c.lands().get(o).top().everyone() == ClaimRights.USE
                    && c.rights(key.dimension(), m, key.chunk()) == (ClaimRights.USE | ClaimRights.CONTAINER | ClaimRights.MANAGE), "The rules are active at once");
            yes(h, c.lands().get(o).code().equals(code), "UUID code is kept as typed, comments included");
            // a stale editor (opened before the change) is refused and changes nothing
            var after = c.lands();
            Claims.install(server, c = new Claims(roomy(), root, Map.of()));
            DeedService.resetThrottle();
            sent.clear();
            var again = c;
            sending(true, sent, () -> DeedService.handleEdit(new DeedEditPayload(0, revision, "*: B\n"), owner));
            yes(h, !result(sent).ok() && result(sent).errors().get(0).key().equals("stale") && again.lands().equals(after), "A stale revision is refused, the ledger stays");
            // refusals carry their line; nothing is changed
            sent.clear();
            int now = again.revision(o);
            sending(true, sent, () -> DeedService.handleEdit(new DeedEditPayload(0, now, "*: U\nSteve: Q\n" + new UUID(0, 0) + ": U\n"), owner));
            var bad = result(sent);
            yes(h, !bad.ok() && bad.errors().stream().anyMatch(e -> e.line() == 2 && e.key().equals("unknown_letter")) && bad.errors().stream().anyMatch(e -> e.line() == 3 && e.key().equals("null_uuid"))
                    && again.lands().equals(after), "Errors with their lines: " + bad.errors());
            // sections only for own chunks
            sent.clear();
            sending(true, sent, () -> DeedService.handleEdit(new DeedEditPayload(0, now, "[0,0]\n" + friend + ": U\n"), owner));
            yes(h, !result(sent).ok() && result(sent).errors().get(0).key().equals("foreign_chunk") && result(sent).errors().get(0).line() == 1, "A foreign chunk is refused with its source line");
            // hands and forged deeds
            sent.clear();
            sending(true, sent, () -> {
                DeedService.handleEdit(new DeedEditPayload(1, now, "*: B\n"), owner);
                DeedService.resetThrottle(); DeedService.handleEdit(new DeedEditPayload(7, now, "*: B\n"), owner);
                yes(h, sent.size() == 2 && sent.stream().allMatch(p -> !((DeedResultPayload) p).ok() && ((DeedResultPayload) p).errors().get(0).key().equals("no_deed")), "Empty or invalid hand: no_deed");
                stranger.setItemInHand(InteractionHand.MAIN_HAND, boundDeed(o, "Ouzzi", 0));
                DeedService.resetThrottle(); DeedService.handleEdit(new DeedEditPayload(0, now, "*: B\n"), stranger);
                yes(h, !result(sent).ok() && result(sent).errors().get(0).key().equals("denied") && again.lands().equals(after), "A forged deed of somebody else's land changes nothing");
                other.setItemInHand(InteractionHand.MAIN_HAND, deed());
                DeedService.resetThrottle(); DeedService.handleEdit(new DeedEditPayload(0, now, "*: B\n"), other);
                yes(h, result(sent).errors().get(0).key().equals("no_deed"), "An unbound deed has no land to edit");
            });
            // managers: copy of the deed, limited rights
            sent.clear();
            manager.setItemInHand(InteractionHand.MAIN_HAND, boundDeed(o, "Ouzzi", 1));
            String keepAll = "# mine\n*: U\n" + friend + ": BDU # builder\n" + m + ": UCM\n";
            sending(true, sent, () -> { DeedService.handleEdit(new DeedEditPayload(0, now, keepAll + buddy + ": B\n"), manager); });
            yes(h, !result(sent).ok() && result(sent).errors().get(0).key().equals("manager_limit"), "A manager cannot hand out rights he does not hold");
            sent.clear();
            sending(true, sent, () -> DeedService.handleEdit(new DeedEditPayload(0, now, keepAll + buddy + ": UC\n"), manager));
            yes(h, result(sent).ok() && again.rights(key.dimension(), buddy, key.chunk()) == (ClaimRights.USE | ClaimRights.CONTAINER), "A manager may add a person with rights he holds");
            DeedService.resetThrottle();
            // cooldown and rate limit: a second save right after a success is refused with the ticks left; a packet in the same tick is dropped
            sent.clear();
            int first = again.revision(o);
            sending(true, sent, () -> DeedService.handleEdit(new DeedEditPayload(0, first, keepAll), owner));
            yes(h, result(sent).ok(), "The owner saves again: " + result(sent).errors());
            DeedService.resetThrottle();
            sent.clear();
            int second = again.revision(o);
            sending(true, sent, () -> {
                DeedService.handleEdit(new DeedEditPayload(0, second, "*: U\n"), owner);
                yes(h, sent.size() == 1 && !result(sent).ok() && result(sent).errors().get(0).key().equals("cooldown") && Integer.parseInt(result(sent).errors().get(0).arg()) > 0, "Cooldown: refused with the ticks left");
                DeedService.handleEdit(new DeedEditPayload(0, second, "*: U\n"), owner);
                yes(h, sent.size() == 1, "A second packet in the same tick is dropped without an answer");
            });
            // claims off: nothing happens
            Claims.install(server, new Claims(ClaimConfig.OFF, temp(), Map.of()));
            sent.clear();
            sending(true, sent, () -> { DeedService.resetThrottle(); DeedService.handleEdit(new DeedEditPayload(0, 0, "x"), owner); });
            yes(h, result(sent).errors().get(0).key().equals("off"), "Claims off: the editor answers 'off'");
        } finally { Claims.install(server, previous); DeedService.resetThrottle(); }
        h.succeed();
    }

    /** Copying like Vanilla books: original + empty deed; the original stays; a copy cannot be copied. */
    public static void copy(GameTestHelper h) {
        var recipe = new DeedCloningRecipe();
        UUID o = UUID.randomUUID();
        var original = boundDeed(o, "Ouzzi", 0); var blank = deed(); var copy = boundDeed(o, "Ouzzi", 1);
        CraftingInput good = CraftingInput.of(2, 1, List.of(original, blank));
        yes(h, recipe.matches(good, h.getLevel()), "Bound original + empty deed match");
        yes(h, recipe.matches(CraftingInput.of(2, 1, List.of(blank, original)), h.getLevel()), "In any order");
        var made = recipe.assemble(good);
        yes(h, made.is(ClaimDeedItem.item()) && made.getCount() == 1 && made.get(DeedData.TYPE).equals(new DeedData(o, "Ouzzi", 1)), "The result is bound to the same owner, generation 1");
        var left = recipe.getRemainingItems(good);
        yes(h, left.get(0).has(DeedData.TYPE) && left.get(0).getCount() == 1 && left.get(1).isEmpty(), "The original stays in the grid, the empty deed is used up");
        yes(h, !recipe.matches(CraftingInput.of(2, 1, List.of(copy, blank)), h.getLevel()), "A copy cannot be copied");
        yes(h, !recipe.matches(CraftingInput.of(2, 1, List.of(deed(), deed())), h.getLevel()), "Two empty deeds do not copy");
        yes(h, !recipe.matches(CraftingInput.of(2, 1, List.of(original, boundDeed(UUID.randomUUID(), "B", 0))), h.getLevel()), "Two bound deeds do not copy");
        yes(h, !recipe.matches(CraftingInput.of(3, 1, List.of(original, blank, deed())), h.getLevel()), "Three deeds do not copy");
        yes(h, !recipe.matches(CraftingInput.of(2, 1, List.of(original, new ItemStack(net.minecraft.world.item.Items.PAPER))), h.getLevel()), "Other items do not copy");
        yes(h, !recipe.matches(CraftingInput.of(1, 1, List.of(original)), h.getLevel()), "A single deed does not copy");
        // the craftable blank
        var recipes = h.getLevel().getServer().getRecipeManager();
        var manager = recipes.getRecipes().stream().map(holder -> holder.value()).filter(r -> r instanceof net.minecraft.world.item.crafting.ShapelessRecipe).map(r -> (net.minecraft.world.item.crafting.ShapelessRecipe) r)
                .filter(r -> r.assemble(CraftingInput.of(3, 1, List.of(new ItemStack(net.minecraft.world.item.Items.PAPER), new ItemStack(net.minecraft.world.item.Items.INK_SAC), new ItemStack(net.minecraft.world.item.Items.HONEYCOMB)))).is(ClaimDeedItem.item())).count();
        yes(h, manager == 1, "Paper + ink sac + honeycomb craft one empty deed");
        h.succeed();
    }
}
