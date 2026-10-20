package com.simplebuilding.modules.simpletweaks.claims;

import com.simplebuilding.framework.api.Protection;
import com.simplebuilding.framework.api.Protection.Action;
import com.simplebuilding.modules.simpletweaks.claims.ClaimRights.Rules;
import java.nio.file.*;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.item.*;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.LeverBlock;
import net.minecraft.world.level.block.RepeaterBlock;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** Phase 1 of the claim deed plan: right bits, rights code parser, ledger v2 with migration, per-action hooks. */
public final class ClaimRightsTests {
    static final String OW = "minecraft:overworld";
    static final UUID OWNER = UUID.fromString("00000000-0000-4000-8000-000000000001");
    static final UUID STEVE = UUID.fromString("00000000-0000-4000-8000-000000000002");
    static final UUID ALEX = UUID.fromString("00000000-0000-4000-8000-000000000003");
    static final Map<String,UUID> NAMES = Map.of("Steve", STEVE, "Alex", ALEX);

    static ClaimCode.Parsed parse(String code) { return ClaimCode.parse(code, OWNER, OW, NAMES::get, 8); }
    static boolean has(ClaimCode.Parsed p, int line, String key) { return p.errors().stream().anyMatch(e -> e.line() == line && e.key().equals(key)); }

    /** Parser: valid code, roles, comments, sections, round trip through the generator. */
    public static void parserGood(GameTestHelper h) {
        UUID third = UUID.fromString("069a79f4-44e9-4726-a5be-fca90e38aaf5");
        var p = parse("# header\n*: U\nSteve: bdu   # lower case\nAlex: @member\n" + third + ": UC\n[12,-4]\nSteve: BDUC\n[nether 3,7]\n*: @guest\n");
        yes(h, p.ok() && p.errors().isEmpty(), "Good code parses: " + p.errors());
        yes(h, p.top().everyone() == ClaimRights.USE && p.top().members().get(third) == (ClaimRights.USE | ClaimRights.CONTAINER), "Everyone line; a UUID works like a name");
        yes(h, p.top().members().get(STEVE) == (ClaimRights.BUILD | ClaimRights.DESTROY | ClaimRights.USE), "Letters, case insensitive, comment stripped");
        yes(h, p.top().members().get(ALEX) == ClaimRights.LEGACY, "@member is BDUCRA (the old trusted rights)");
        yes(h, ClaimRights.role("guest") == ClaimRights.USE && ClaimRights.role("HELPER") == (ClaimRights.USE | ClaimRights.CONTAINER | ClaimRights.ANIMALS)
                && ClaimRights.role("manager") == ClaimRights.ALL && ClaimRights.role("boss") == -1, "Roles");
        var ow = new ClaimCode.At(OW, 12, -4);
        yes(h, p.sections().size() == 2 && p.sections().get(ow).members().get(STEVE) == (ClaimRights.BUILD | ClaimRights.DESTROY | ClaimRights.USE | ClaimRights.CONTAINER), "Section for a chunk of the current dimension");
        yes(h, p.sections().get(new ClaimCode.At("minecraft:the_nether", 3, 7)).everyone() == ClaimRights.USE && p.sectionLines().get(ow) == 6, "Section with dimension; source line kept");
        var dup = parse("Steve: BBU\nAlex:\n");
        yes(h, dup.ok() && dup.notes().stream().anyMatch(n -> n.key().equals("duplicate_letter")) && dup.notes().stream().anyMatch(n -> n.key().equals("no_rights") && n.line() == 2)
                && !dup.top().members().containsKey(ALEX), "Doubled letters and empty rights are hints, not errors");
        var back = parse(ClaimCode.generate(p.top(), p.sections()));
        yes(h, back.ok() && back.top().equals(p.top()) && back.sections().equals(p.sections()), "Generated code parses back to the same rules");
        yes(h, parse("").ok() && parse("# only a comment\n\n").top().isEmpty(), "Empty code is valid and grants nothing");
        yes(h, ClaimRights.letters(ClaimRights.ALL).equals("BDUCRAM") && ClaimRights.describe(ClaimRights.LEGACY).equals("@member") && ClaimRights.describe(3).equals("BD"), "Letters and role names");
        h.succeed();
    }

    /** Parser: every error kind with its line, and the hard limits. */
    public static void parserBad(GameTestHelper h) {
        String uuid = "069a79f4-44e9-4726-a5be-fca90e38aaf5";
        var p = parse("*: U\nSteve: BXU\nAlex: @boss\nTomm: U\nbad line\n" + uuid.replace("a5be", "zzzz") + ": U\n" + new UUID(0, 0) + ": U\n"
                + OWNER + ": U\nSteve: U\n*: M\nSteve: M x\n[1,2\n[3,4]\n[3,4]\n");
        yes(h, !p.ok(), "Bad code is refused");
        yes(h, has(p, 2, "unknown_letter") && p.errors().get(0).arg().equals("X"), "Unknown letter on its line");
        yes(h, has(p, 3, "unknown_role") && has(p, 4, "unknown_name") && has(p, 5, "syntax") && has(p, 6, "bad_uuid") && has(p, 7, "null_uuid") && has(p, 8, "owner"), "Role, unknown name, syntax, bad/null UUID, owner");
        yes(h, has(p, 10, "everyone_manage") && has(p, 11, "unknown_letter") && has(p, 12, "bad_section") && has(p, 14, "duplicate_section"), "Manage for everyone, letters after a space, malformed and doubled section");
        var dupSection = parse("[3,4]\n[3,4]\n");
        yes(h, has(dupSection, 2, "duplicate_section"), "Doubled section");
        var dupPerson = parse("Steve: U\nSteve: B\n");
        yes(h, has(dupPerson, 2, "duplicate"), "Doubled person");
        yes(h, has(parse("Steve: U\n*: U\n*: B\n"), 3, "duplicate"), "Doubled everyone line");
        var many = new StringBuilder();
        for (int i = 0; i < 9; i++) many.append(new UUID(1, i + 1)).append(": U\n");
        yes(h, has(parse(many.toString()), 9, "too_many"), "More persons than the configured cap");
        var sections = new StringBuilder();
        for (int i = 0; i <= ClaimCode.MAX_SECTIONS; i++) sections.append('[').append(i).append(",0]\n");
        yes(h, has(parse(sections.toString()), ClaimCode.MAX_SECTIONS + 1, "too_many_sections"), "More than 32 sections");
        yes(h, has(parse("[99999999,0]\n"), 1, "bad_section") && has(parse("[x,y]\n"), 1, "bad_section") && has(parse("[nether 1]\n"), 1, "bad_section")
                && has(parse("[Bad Dim! 1,2]\n"), 1, "bad_section"), "Coordinates and dimension names are validated");
        yes(h, has(parse("Steve: U\u0001\n"), 1, "bad_char"), "Control characters");
        // hard limits come before parsing
        var longCode = parse("#".repeat(ClaimCode.MAX_CHARS + 1));
        yes(h, !longCode.ok() && has(longCode, 0, "too_long"), "4097 characters refused");
        yes(h, parse(("#".repeat(79) + "\n").repeat(51)).ok() && parse("#".repeat(80) + "\n" + "#".repeat(ClaimCode.MAX_CHARS - 81)).errors().get(0).key().equals("line_too_long"), "4080 characters in short lines accepted; the line limit applies on its own");
        var lines = parse("\n".repeat(ClaimCode.MAX_LINES));
        yes(h, !lines.ok() && has(lines, ClaimCode.MAX_LINES + 1, "too_many_lines"), "201 lines refused");
        yes(h, parse("\n".repeat(ClaimCode.MAX_LINES - 1)).ok(), "200 lines accepted");
        yes(h, has(parse("# " + "x".repeat(ClaimCode.MAX_LINE) + "\n"), 1, "line_too_long") && parse("#" + "x".repeat(ClaimCode.MAX_LINE - 1)).ok(), "Line longer than 96 characters");
        var flood = parse("a b\n".repeat(60));
        yes(h, flood.errors().size() == ClaimCode.MAX_ERRORS, "At most 16 errors are collected: " + flood.errors().size());
        yes(h, !ClaimCode.parse(null, OWNER, OW, NAMES::get, 8).ok(), "Null code");
        h.succeed();
    }

    // ---- hook matrix ----
    static final int P_BUILD = 1, P_DESTROY = 2, P_USE = 4, P_CONTAINER = 8, P_REDSTONE = 16, P_ANIMALS = 32;

    /** Which real server hooks let this player act in the claimed chunk: place, break, lever, chest, repeater, cow. */
    static int probes(GameTestHelper h, ServerPlayer guest, BlockPos ground) {
        var level = h.getLevel(); var top = ground.above(); int out = 0;
        var hand = InteractionHand.MAIN_HAND;
        level.setBlock(ground, Blocks.STONE.defaultBlockState(), 2); level.setBlock(top, Blocks.AIR.defaultBlockState(), 2);
        var hit = new BlockHitResult(Vec3.atCenterOf(ground).add(0, .5, 0), Direction.UP, ground, false);
        guest.setItemInHand(hand, new ItemStack(Items.COBBLESTONE));
        guest.gameMode.useItemOn(guest, level, guest.getMainHandItem(), hand, hit);
        if (level.getBlockState(top).is(Blocks.COBBLESTONE)) out |= P_BUILD;
        level.setBlock(top, Blocks.AIR.defaultBlockState(), 2); guest.setItemInHand(hand, ItemStack.EMPTY);
        boolean hook = Claims.allowBlock(guest, level, ground, Action.DESTROY);
        if (guest.gameMode.destroyBlock(ground)) out |= P_DESTROY;
        else if (hook) com.mojang.logging.LogUtils.getLogger().warn("[claims-rights] destroyBlock refused although the claim hook allows it: {} {}", level.getBlockState(ground), guest.gameMode.getGameModeForPlayer());
        level.setBlock(ground, Blocks.STONE.defaultBlockState(), 2);
        level.setBlock(top, Blocks.LEVER.defaultBlockState().setValue(LeverBlock.FACE, net.minecraft.world.level.block.state.properties.AttachFace.FLOOR), 2);
        var topHit = new BlockHitResult(Vec3.atCenterOf(top), Direction.UP, top, false);
        guest.gameMode.useItemOn(guest, level, ItemStack.EMPTY, hand, topHit);
        if (level.getBlockState(top).getValue(LeverBlock.POWERED)) out |= P_USE;
        level.setBlock(top, Blocks.CHEST.defaultBlockState(), 2);
        if (guest.gameMode.useItemOn(guest, level, ItemStack.EMPTY, hand, topHit).consumesAction()) { out |= P_CONTAINER; guest.closeContainer(); }
        level.setBlock(top, Blocks.REPEATER.defaultBlockState(), 2);
        guest.gameMode.useItemOn(guest, level, ItemStack.EMPTY, hand, topHit);
        if (level.getBlockState(top).getValue(RepeaterBlock.DELAY) == 2) out |= P_REDSTONE;
        level.setBlock(top, Blocks.AIR.defaultBlockState(), 2);
        var cow = net.minecraft.world.entity.EntityTypes.COW.create(level, net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        cow.setPos(top.getX() + .5, top.getY(), top.getZ() + .5); level.addFreshEntity(cow);
        guest.setItemInHand(hand, new ItemStack(Items.BUCKET));
        if (guest.interactOn(cow, hand, Vec3.ZERO).consumesAction()) out |= P_ANIMALS;
        cow.discard(); guest.setItemInHand(hand, ItemStack.EMPTY);
        level.setBlock(ground, Blocks.AIR.defaultBlockState(), 2);
        return out;
    }
    /** Test config with room for several persons per scope (the shared one allows two). */
    static ClaimConfig roomy() { return new ClaimConfig(true, 8, 8, 10, 20, false, 0, ClaimConfig.DEFAULT.dimensions()); }
    static Claims seeded(GameTestHelper h, Path root, ClaimStore.Key key, UUID owner, Rules top, Map<ClaimStore.Key,Rules> sections) {
        try {
            new ClaimStore(root.resolve("simpletweaks-claims.json"), Map.of()).replace(Map.of(key, new ClaimStore.Claim(owner)),
                    Map.of(owner, new ClaimStore.Land(owner, "", top, sections)));
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        var c = new Claims(roomy(), root, Map.of()); Claims.install(h.getLevel().getServer(), c); return c;
    }

    /**
     * A cell in the chunk next to the test's own that no entity occupies. The chunk lies outside the 8x8x8 structure, so neighbouring
     * tests (and mock players left behind there) share the same cell and the placement probe was sometimes blocked by them.
     */
    static BlockPos freeGround(net.minecraft.server.level.ServerLevel level, BlockPos base) {
        int x0 = (base.getX() >> 4) * 16 + 16, z0 = (base.getZ() >> 4) * 16;
        for (int dz = 2; dz < 15; dz += 3) for (int dx = 2; dx < 12; dx += 3) {
            var c = new BlockPos(x0 + dx, base.getY(), z0 + dz);
            if (level.getEntities((net.minecraft.world.entity.Entity) null, new net.minecraft.world.phys.AABB(c).inflate(1.5, 0, 1.5).expandTowards(0, 3, 0), e -> true).isEmpty()) return c;
        }
        return new BlockPos(x0 + 8, base.getY(), z0 + 8);
    }
    /** Every right letter allows exactly its own actions; a stranger, manage-only and legacy @member behave as documented. */
    public static void actionMatrix(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server); var level = h.getLevel();
        var base = h.absolutePos(new BlockPos(2, 3, 2)); var ground = freeGround(level, base);
        var key = new ClaimStore.Key(level.dimension().identifier().toString(), ChunkPos.pack(ground));
        var guest = player(h, ground.east(3)); UUID g = guest.getUUID();
        try {
            int[] bits = {ClaimRights.BUILD, ClaimRights.DESTROY, ClaimRights.USE, ClaimRights.CONTAINER, ClaimRights.REDSTONE, ClaimRights.ANIMALS};
            int[] probe = {P_BUILD, P_DESTROY, P_USE, P_CONTAINER, P_REDSTONE, P_ANIMALS};
            for (int i = 0; i < bits.length; i++) {
                seeded(h, temp(), key, OWNER, new Rules(0, Map.of(g, bits[i])), Map.of());
                int got = probes(h, guest, ground);
                yes(h, got == probe[i], "Only right " + ClaimRights.letters(bits[i]) + " allows exactly its own action (probes " + got + ", expected " + probe[i] + ", hooks " + Claims.allowBlock(guest, level, ground, Action.DESTROY) + ")");
            }
            seeded(h, temp(), key, OWNER, Rules.EMPTY, Map.of());
            yes(h, probes(h, guest, ground) == 0, "A stranger is blocked on every action");
            seeded(h, temp(), key, OWNER, new Rules(0, Map.of(g, ClaimRights.MANAGE)), Map.of());
            yes(h, probes(h, guest, ground) == 0, "Manage alone allows no world action");
            seeded(h, temp(), key, OWNER, new Rules(0, Map.of(g, ClaimRights.LEGACY)), Map.of());
            yes(h, probes(h, guest, ground) == 63, "@member (the old trusted rights) allows everything it did before");
            seeded(h, temp(), key, OWNER, new Rules(ClaimRights.USE, Map.of()), Map.of());
            yes(h, probes(h, guest, ground) == P_USE, "The everyone line applies to a player who is not listed");
            seeded(h, temp(), key, OWNER, new Rules(ClaimRights.USE, Map.of(g, ClaimRights.CONTAINER)), Map.of());
            yes(h, probes(h, guest, ground) == (P_USE | P_CONTAINER), "Everyone rights and personal rights add up");
            seeded(h, temp(), key, OWNER, new Rules(0, Map.of(g, ClaimRights.LEGACY)), Map.of(key, new Rules(0, Map.of(g, ClaimRights.USE))));
            yes(h, probes(h, guest, ground) == P_USE, "A chunk section replaces the top rules for that chunk");
            seeded(h, temp(), key, OWNER, Rules.EMPTY, Map.of(key, Rules.EMPTY));
            var owner = player(h, ground.west(3)); owner.setUUID(OWNER);
            yes(h, probes(h, owner, ground) == 63, "The owner always has everything");
            // a click that falls through to an item (axe, hoe, flint and steel) is strict, whatever the block class allowed
            level.setBlock(ground, Blocks.STONE.defaultBlockState(), 2);
            var axeHit = new BlockHitResult(Vec3.atCenterOf(ground).add(0, .5, 0), Direction.UP, ground, false);
            for (int rights : new int[]{ClaimRights.USE | ClaimRights.CONTAINER | ClaimRights.REDSTONE | ClaimRights.ANIMALS, ClaimRights.BUILD, ClaimRights.DESTROY, ClaimRights.BUILD | ClaimRights.DESTROY}) {
                seeded(h, temp(), key, OWNER, new Rules(0, Map.of(g, rights)), Map.of());
                var axe = new ItemStack(Items.IRON_AXE);
                var result = axe.useOn(new net.minecraft.world.item.context.UseOnContext(level, guest, InteractionHand.MAIN_HAND, axe, axeHit));
                yes(h, (result == InteractionResult.FAIL) == ((rights & ClaimRights.STRICT) != ClaimRights.STRICT), "Item use on a block needs B and D (rights " + ClaimRights.letters(rights) + " -> " + result + ")");
            }
            level.setBlock(ground, Blocks.AIR.defaultBlockState(), 2);
            // framework contract: an action-less caller is strict (build and destroy), actions map to their letter
            var c = seeded(h, temp(), key, OWNER, new Rules(0, Map.of(g, ClaimRights.BUILD)), Map.of());
            var target = new Protection.Target(g, level.dimension().identifier().toString(), ground.getX(), ground.getY(), ground.getZ(), false);
            yes(h, Protection.allows(server, target, Action.BUILD) && !Protection.allows(server, target, Action.DESTROY) && !Protection.allows(server, target) && !Protection.allows(server, target, Action.ANY),
                    "Old callers (no action) need both B and D; B alone only builds");
            seeded(h, temp(), key, OWNER, new Rules(0, Map.of(g, ClaimRights.BUILD | ClaimRights.DESTROY)), Map.of());
            yes(h, Protection.allows(server, target) && Claims.allow(guest, level, ground) && Claims.allow(level, g, ground) && !Protection.allows(server, target, Action.USE), "B and D pass the strict check; U is still missing");
            yes(h, Protection.allows(server, new Protection.Target(g, level.dimension().identifier().toString(), ground.getX() + 4096, ground.getY(), ground.getZ(), false), Action.CONTAINER), "Unclaimed land allows every action");
            yes(h, !Protection.allows(server, new Protection.Target(null, level.dimension().identifier().toString(), ground.getX(), ground.getY(), ground.getZ(), false), Action.USE), "No actor never has rights on claimed land");
        } finally { Claims.install(server, previous); }
        h.succeed();
    }

    /** applyCode: all or nothing, owner and manager limits, hard limits, persistence. */
    public static void applyCode(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server); var level = h.getLevel();
        var base = h.absolutePos(new BlockPos(2, 3, 2)); var pos = new BlockPos((base.getX() >> 4) * 16 + 16, base.getY(), (base.getZ() >> 4) * 16 + 8);
        var key = new ClaimStore.Key(level.dimension().identifier().toString(), ChunkPos.pack(pos));
        var owner = player(h, pos); var other = player(h, pos); var manager = player(h, pos); var plain = player(h, pos);
        var root = temp(); UUID o = owner.getUUID(), m = manager.getUUID(), x = other.getUUID();
        UUID member = UUID.randomUUID(), second = UUID.randomUUID();
        try {
            var members = new HashMap<UUID,Integer>(); members.put(m, ClaimRights.USE | ClaimRights.MANAGE | ClaimRights.CONTAINER); members.put(member, ClaimRights.USE);
            var c = seeded(h, root, key, o, new Rules(0, members), Map.of());
            yes(h, c.applyCode(owner, "").ok() && c.lands().get(o).top().isEmpty(), "Owner saves an empty code, which removes all rights");
            var rootGood = temp(); c = seeded(h, rootGood, key, o, Rules.EMPTY, Map.of());
            String good = "*: U\n" + x + ": BDU\n" + member + ": @helper\n[" + ChunkPos.getX(key.chunk()) + "," + ChunkPos.getZ(key.chunk()) + "]\n" + x + ": C\n";
            var before = c.lands();
            var bad = c.applyCode(owner, good + "Nobody: U\n" + x + ": Q\n");
            yes(h, !bad.ok() && bad.errors().stream().anyMatch(e -> e.key().equals("unknown_name") && e.line() == 6) && bad.errors().stream().anyMatch(e -> e.key().equals("unknown_letter") && e.line() == 7), "Errors name their lines: " + bad.errors());
            yes(h, c.lands().equals(before), "A rejected code changes nothing");
            // fresh instance = fresh cooldown map
            c = new Claims(roomy(), rootGood, Map.of()); Claims.install(server, c);
            var longRejected = c.applyCode(owner, "#".repeat(ClaimCode.MAX_CHARS + 1));
            yes(h, !longRejected.ok() && longRejected.errors().get(0).key().equals("too_long") && c.lands().equals(before), "Owner: 4097 characters refused by the server");
            var foreign = c.applyCode(owner, "[0,0]\n" + x + ": U\n");
            yes(h, !foreign.ok() && foreign.errors().get(0).key().equals("foreign_chunk") && foreign.errors().get(0).line() == 1, "A section only for chunks the owner holds");
            var ok = c.applyCode(owner, good);
            yes(h, ok.ok(), "Valid code saved: " + ok.errors());
            yes(h, c.rights(key.dimension(), x, key.chunk()) == ClaimRights.CONTAINER && c.rights(key.dimension(), member, key.chunk()) == 0 && c.lands().get(o).top().everyone() == ClaimRights.USE,
                    "Rules active at once: the section replaces the top rules (also the everyone line) for its chunk");
            yes(h, !c.applyCode(owner, "").ok() && c.applyCode(owner, "").errors().get(0).key().equals("cooldown"), "Cooldown holds across edits of one player");
            var reloaded = new Claims(roomy(), rootGood, Map.of());
            yes(h, reloaded.lands().equals(c.lands()) && reloaded.lands().get(o).code().equals(good), "Code and rules survive a reload");
            // strangers and managers
            Claims.install(server, reloaded);
            yes(h, !reloaded.applyCode(plain, o, "*: B\n").ok() && reloaded.applyCode(plain, o, "*: B\n").errors().get(0).key().equals("denied"), "Somebody without M cannot edit the land");
            // manager
            c = seeded(h, temp(), key, o, new Rules(0, Map.of(m, ClaimRights.USE | ClaimRights.MANAGE | ClaimRights.CONTAINER, member, ClaimRights.USE)), Map.of());
            String head = m + ": " + ClaimRights.letters(ClaimRights.USE | ClaimRights.MANAGE | ClaimRights.CONTAINER) + "\n";
            yes(h, c.applyCode(manager, o, head + member + ": UC\n").ok() && c.rights(key.dimension(), member, key.chunk()) == (ClaimRights.USE | ClaimRights.CONTAINER), "A manager may raise a person up to his own rights");
            for (var attempt : List.of(head + member + ": UB\n", head + member + ": UM\n", head + member + ": U\n*: U\n", head + member + ": U\n" + second + ": B\n", head + member + ": U\n[" + ChunkPos.getX(key.chunk()) + "," + ChunkPos.getZ(key.chunk()) + "]\n" + member + ": U\n",
                    m + ": UC\n" + member + ": U\n", m + ": UMCB\n" + member + ": U\n")) {
                c = seeded(h, temp(), key, o, new Rules(0, Map.of(m, ClaimRights.USE | ClaimRights.MANAGE | ClaimRights.CONTAINER, member, ClaimRights.USE)), Map.of());
                var old = c.lands(); var r = c.applyCode(manager, o, attempt);
                yes(h, !r.ok() && r.errors().get(0).key().equals("manager_limit") && c.lands().equals(old), "Manager limit refuses: " + attempt.replace("\n", " | "));
            }
            c = seeded(h, temp(), key, o, new Rules(0, Map.of(m, ClaimRights.MANAGE, member, ClaimRights.USE | ClaimRights.MANAGE)), Map.of());
            yes(h, !c.applyCode(manager, o, m + ": M\n" + member + ": U\n").ok(), "A manager cannot touch another manager");
            c = seeded(h, temp(), key, o, new Rules(0, Map.of(m, ClaimRights.MANAGE | ClaimRights.USE)), Map.of());
            yes(h, c.applyCode(owner, member + ": @manager\n").ok() && c.rights(key.dimension(), member, key.chunk()) == ClaimRights.ALL, "The owner may hand out manage");
            // no land / locked
            Claims.install(server, new Claims(on(), temp(), Map.of()));
            yes(h, Claims.get(server).applyCode(owner, "").errors().get(0).key().equals("no_land"), "No claims, no land to edit");
        } finally { Claims.install(server, previous); }
        h.succeed();
    }

    /** Commands stay per chunk: trust with rights makes a chunk section, untrust removes it. */
    public static void commands(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server); var level = h.getLevel();
        var base = h.absolutePos(new BlockPos(2, 3, 2)); var pos = new BlockPos((base.getX() >> 4) * 16 + 16, base.getY(), (base.getZ() >> 4) * 16 + 8);
        var key = new ClaimStore.Key(level.dimension().identifier().toString(), ChunkPos.pack(pos));
        var owner = player(h, pos); UUID o = owner.getUUID(); var root = temp();
        UUID a = UUID.randomUUID(), b = UUID.randomUUID();
        try {
            var c = seeded(h, root, key, o, Rules.EMPTY, Map.of()); var d = ClaimAccessTests.dispatcher(roomy());
            yes(h, ClaimAccessTests.execute(d, owner, "claim trust " + a) == 1 && c.rights(key.dimension(), a, key.chunk()) == ClaimRights.LEGACY, "Plain trust still grants @member");
            Claims.install(server, c = new Claims(roomy(), root, Map.of()));
            yes(h, ClaimAccessTests.execute(d, owner, "claim trust " + a + " BDU") == 1 && c.rights(key.dimension(), a, key.chunk()) == (ClaimRights.BUILD | ClaimRights.DESTROY | ClaimRights.USE), "Trust with letters changes the rights");
            Claims.install(server, c = new Claims(roomy(), root, Map.of()));
            yes(h, ClaimAccessTests.execute(d, owner, "claim trust " + b + " @helper") == 1 && c.rights(key.dimension(), b, key.chunk()) == ClaimRights.role("helper"), "Trust with a role");
            yes(h, c.lands().get(o).sections().containsKey(key) && c.lands().get(o).top().isEmpty(), "A per-chunk command writes a section for that chunk only");
            var code = c.lands().get(o).code(); var parsed = ClaimCode.parse(code, o, key.dimension(), n -> null, 8);
            yes(h, parsed.ok() && parsed.sections().get(key.at()).equals(c.lands().get(o).sections().get(key)), "The regenerated code matches the stored rules");
            Claims.install(server, c = new Claims(roomy(), root, Map.of()));
            for (String bad : List.of("M", "@manager", "BDX", "@boss", "X")) {
                int r = ClaimAccessTests.execute(d, owner, "claim trust " + UUID.randomUUID() + " " + bad);
                yes(h, r <= 0, "Per-chunk trust refuses rights '" + bad + "'");
            }
            yes(h, ClaimAccessTests.execute(d, owner, "claim info") == 1 && ClaimAccessTests.execute(d, owner, "claim code") == 1, "info and code answer");
            Claims.install(server, c = new Claims(roomy(), root, Map.of()));
            yes(h, ClaimAccessTests.execute(d, owner, "claim untrust " + a) == 1 && c.rights(key.dimension(), a, key.chunk()) == 0 && c.rights(key.dimension(), b, key.chunk()) != 0, "Untrust removes one person");
            Claims.install(server, c = new Claims(roomy(), root, Map.of()));
            yes(h, ClaimAccessTests.execute(d, owner, "claim untrust " + b) == 1 && !c.lands().isEmpty() && c.lands().get(o).sections().isEmpty(), "Last person removed: the empty section is dropped");
            Claims.install(server, c = new Claims(roomy(), root, Map.of()));
            yes(h, c.unclaim(owner, false) && c.lands().isEmpty(), "Unclaiming the last chunk drops the land");
        } finally { Claims.install(server, previous); }
        h.succeed();
    }

    static String v1(String owner, String chunk, String... trusted) {
        var list = new StringBuilder();
        for (var t : trusted) list.append(list.length() > 0 ? "," : "").append('"').append(t).append('"');
        return "{\"dimension\":\"minecraft:overworld\",\"chunk\":" + chunk + ",\"Owner\":\"" + owner + "\",\"Whitelist\":[" + list + "]}";
    }

    /** v1 to v2: no rights are gained or lost, the v1 file is kept, unknown data fails closed. */
    public static void migration(GameTestHelper h) {
        try {
            var dir = temp(); var file = dir.resolve("simpletweaks-claims.json");
            UUID a = UUID.randomUUID(), b = UUID.randomUUID(), c = UUID.randomUUID(), g1 = UUID.randomUUID(), g2 = UUID.randomUUID(), g3 = UUID.randomUUID();
            String text = "{\"version\":1,\"claims\":[" + String.join(",",
                    v1(a.toString(), "1", g1.toString(), g2.toString()), v1(a.toString(), "2", g2.toString(), g1.toString()),
                    v1(b.toString(), "3", g1.toString()), v1(b.toString(), "4", g2.toString(), g3.toString()), v1(b.toString(), "5"),
                    v1(c.toString(), "6")) + "]}";
            Files.writeString(file, text); byte[] original = Files.readAllBytes(file);
            var store = new ClaimStore(file, Map.of());
            ClaimStore.Key k1 = new ClaimStore.Key(OW, 1), k2 = new ClaimStore.Key(OW, 2), k3 = new ClaimStore.Key(OW, 3), k4 = new ClaimStore.Key(OW, 4), k5 = new ClaimStore.Key(OW, 5), k6 = new ClaimStore.Key(OW, 6);
            yes(h, Arrays.equals(original, Files.readAllBytes(file)) && !Files.exists(dir.resolve("simpletweaks-claims.v1.json")), "Loading v1 writes nothing");
            yes(h, store.view().size() == 6 && store.lands().size() == 2, "Owner without trusted players needs no land record");
            yes(h, store.rights(k1, g1) == ClaimRights.LEGACY && store.rights(k2, g2) == ClaimRights.LEGACY && store.rights(k1, UUID.randomUUID()) == 0, "Equal lists become top rules with @member");
            yes(h, store.lands().get(a).sections().isEmpty() && store.lands().get(a).top().members().size() == 2, "Common list is the top part, no sections");
            yes(h, store.rights(k3, g1) == ClaimRights.LEGACY && store.rights(k3, g2) == 0 && store.rights(k4, g2) == ClaimRights.LEGACY && store.rights(k4, g3) == ClaimRights.LEGACY
                    && store.rights(k4, g1) == 0 && store.rights(k5, g1) == 0 && store.rights(k6, g1) == 0, "Different lists become one section per chunk, nothing leaks across chunks");
            yes(h, store.rights(k1, g1) != 0 && (store.rights(k1, g1) & ClaimRights.MANAGE) == 0 && store.rights(k1, a) == ClaimRights.ALL && store.rights(new ClaimStore.Key(OW, 99), g3) == ClaimRights.ALL, "Nobody gains M; unclaimed land stays open");
            for (var land : store.lands().values()) {
                var parsed = ClaimCode.parse(land.code(), land.owner(), OW, n -> null, 64);
                var at = new HashMap<ClaimCode.At,Rules>(); land.sections().forEach((k, r) -> at.put(k.at(), r));
                yes(h, parsed.ok() && parsed.top().equals(land.top()) && parsed.sections().equals(at), "Migrated code parses back to the migrated rules: " + parsed.errors());
            }
            // first write: v2 + one backup of the original
            store.replace(store.view());
            var back = dir.resolve("simpletweaks-claims.v1.json");
            yes(h, Files.exists(back) && Arrays.equals(original, Files.readAllBytes(back)), "First v2 write keeps the v1 file byte for byte");
            yes(h, Files.readString(file).contains("\"version\":2"), "The ledger is now version 2");
            var again = new ClaimStore(file, Map.of());
            yes(h, again.view().equals(store.view()) && again.lands().equals(store.lands()), "v2 round trip is lossless");
            store.replace(store.view()); yes(h, Arrays.equals(original, Files.readAllBytes(back)), "Later writes never touch the backup");
            // oversize migrated code: structure stays authoritative
            var big = new StringBuilder("{\"version\":1,\"claims\":["); UUID bigOwner = UUID.randomUUID(); var guests = new ArrayList<UUID>();
            for (int i = 0; i < 40; i++) { guests.add(UUID.randomUUID()); big.append(i > 0 ? "," : "").append(v1(bigOwner.toString(), Integer.toString(100 + i), guests.get(i).toString())); }
            Path bigFile = temp().resolve("simpletweaks-claims.json"); Files.writeString(bigFile, big.append("]}").toString());
            var bigStore = new ClaimStore(bigFile, Map.of());
            boolean all = true;
            for (int i = 0; i < 40; i++) all &= bigStore.rights(new ClaimStore.Key(OW, 100 + i), guests.get(i)) == ClaimRights.LEGACY && bigStore.rights(new ClaimStore.Key(OW, 100 + (i + 1) % 40), guests.get(i)) == 0;
            yes(h, all && bigStore.lands().get(bigOwner).sections().size() == 40 && bigStore.lands().get(bigOwner).code().isEmpty(), "40 sections exceed the editor limit: rules kept, code left empty");
            // fail closed
            for (String broken : List.of("{\"version\":3,\"claims\":[],\"lands\":[]}", "{\"version\":2,\"claims\":[],\"lands\":[{\"Owner\":\"" + a + "\",\"code\":\"\",\"everyone\":255,\"members\":[],\"sections\":[]}]}",
                    "{\"version\":2,\"claims\":[],\"lands\":[{\"Owner\":\"" + a + "\",\"code\":\"\",\"everyone\":64,\"members\":[],\"sections\":[]}]}",
                    "{\"version\":2,\"claims\":[],\"lands\":[{\"Owner\":\"" + a + "\",\"code\":\"\",\"everyone\":0,\"members\":[{\"id\":\"" + g1 + "\",\"rights\":0}],\"sections\":[]}]}")) {
                Path bad = temp().resolve("simpletweaks-claims.json"); Files.writeString(bad, broken);
                try { new ClaimStore(bad, Map.of()); throw new AssertionError("Accepted: " + broken); } catch (java.io.IOException | RuntimeException expected) {}
            }
            var lockRoot = temp(); Files.writeString(lockRoot.resolve("simpletweaks-claims.json"), "{\"version\":2,\"claims\":[],\"lands\":[{\"Owner\":\"x\"}]}");
            var locked = new Claims(on(), lockRoot, Map.of());
            yes(h, locked.locked() && !locked.allowed(OW, UUID.randomUUID(), 0, ClaimRights.USE), "Invalid v2 data locks the claims (fail closed)");
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        h.succeed();
    }
}
