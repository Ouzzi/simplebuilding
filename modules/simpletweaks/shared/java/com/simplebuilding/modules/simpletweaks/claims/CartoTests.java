package com.simplebuilding.modules.simpletweaks.claims;

import com.simplebuilding.modules.simpletweaks.claims.ClaimRights.Rules;
import com.simplebuilding.modules.simpletweaks.deed.ClaimDeedItem;
import com.simplebuilding.modules.simpletweaks.deed.DeedCartography;
import com.simplebuilding.modules.simpletweaks.deed.DeedData;
import com.simplebuilding.modules.simpletweaks.table.ClaimTableBlock;
import java.util.Map;
import java.util.UUID;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.inventory.CartographyTableMenu;
import net.minecraft.world.inventory.ContainerLevelAccess;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.MapItem;
import net.minecraft.world.level.ChunkPos;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/** SP22-8 branch 3: deeds at the cartography table (own deed, copies, the wrapper) and the old claim table's conversion. */
public final class CartoTests {
    private CartoTests() {}

    private static ItemStack blank() { return new ItemStack(ClaimDeedItem.item()); }
    private static ItemStack bound(UUID owner, String name, int generation) {
        var s = blank(); s.set(DeedData.TYPE, new DeedData(owner, name, generation)); return s;
    }
    private static CartographyTableMenu menu(GameTestHelper h, ServerPlayer p) {
        return new CartographyTableMenu(1, p.getInventory(), ContainerLevelAccess.create(h.getLevel(), h.absolutePos(new BlockPos(1, 2, 1))));
    }
    private static void clear(CartographyTableMenu m) { m.getSlot(0).set(ItemStack.EMPTY); m.getSlot(1).set(ItemStack.EMPTY); }

    /** An empty deed on top gives the player's own deed (always), the empty deed is used up; anything below: nothing. */
    public static void own(GameTestHelper h) {
        with(h, c -> {
            var p = player(h, h.absolutePos(new BlockPos(1, 2, 1)));
            var m = menu(h, p);
            m.getSlot(0).set(blank());
            ItemStack out = m.getSlot(2).getItem();
            DeedData data = out.get(DeedData.TYPE);
            yes(h, out.is(ClaimDeedItem.item()) && out.getCount() == 1 && data != null && data.owner().equals(p.getUUID()) && data.generation() == 0
                    && !data.ownerName().isEmpty(), "Empty deed on top: the player's own deed (original) " + data);
            m.getSlot(2).onTake(p, out);
            yes(h, m.getSlot(0).getItem().isEmpty(), "Taking it uses up the empty deed");
            yes(h, m.getSlot(2).getItem().isEmpty(), "Nothing left to take");
            m.getSlot(0).set(blank()); m.getSlot(1).set(new ItemStack(Items.PAPER));
            yes(h, m.getSlot(2).getItem().isEmpty(), "Empty deed with paper below: no result");
            m.getSlot(1).set(blank());
            yes(h, m.getSlot(2).getItem().isEmpty(), "Two empty deeds: no result");
            // A player who already holds land still gets a deed (always a way back to one's own deed).
            clear(m);
            yes(h, c.create(new ClaimStore.Key(h.getLevel().dimension().identifier().toString(), ChunkPos.pack(p.blockPosition())), p.getUUID(), 0), "Seed land");
            m.getSlot(0).set(blank());
            yes(h, p.getUUID().equals(m.getSlot(2).getItem().get(DeedData.TYPE).owner()), "With land: still the own deed");
            clear(m);
        });
        // Claims switched off: the table gives nothing for deeds.
        var server = h.getLevel().getServer(); var previous = Claims.get(server);
        try {
            Claims.install(server, null);
            var p = player(h, h.absolutePos(new BlockPos(1, 2, 1)));
            var m = menu(h, p);
            m.getSlot(0).set(blank());
            yes(h, m.getSlot(2).getItem().isEmpty(), "Claims off: no deed from the table");
            clear(m);
        } finally { Claims.install(server, previous); }
        h.succeed();
    }

    /** Own deed + empty deed = a copy (original stays); a manager may copy; another person's deed or a copy gives nothing. */
    public static void copy(GameTestHelper h) {
        var server = h.getLevel().getServer(); var previous = Claims.get(server);
        try {
            var pos = h.absolutePos(new BlockPos(1, 2, 1));
            var owner = player(h, pos); var manager = player(h, pos); var stranger = player(h, pos);
            var key = new ClaimStore.Key(h.getLevel().dimension().identifier().toString(), ChunkPos.pack(pos.offset(2048, 0, 2048)));
            ClaimRightsTests.seeded(h, temp(), key, owner.getUUID(), new Rules(0, Map.of(manager.getUUID(), ClaimRights.USE | ClaimRights.MANAGE,
                    stranger.getUUID(), ClaimRights.USE | ClaimRights.BUILD)), Map.of());
            var m = menu(h, owner);
            m.getSlot(0).set(bound(owner.getUUID(), "Ouzzi", 0));
            m.getSlot(1).set(blank());
            ItemStack copy = m.getSlot(2).getItem();
            DeedData data = copy.get(DeedData.TYPE);
            yes(h, data != null && data.owner().equals(owner.getUUID()) && data.generation() == 1 && "Ouzzi".equals(data.ownerName()), "Owner: a copy (generation 1)");
            m.getSlot(2).onTake(owner, copy);
            yes(h, m.getSlot(1).getItem().isEmpty() && m.getSlot(0).getItem().get(DeedData.TYPE).generation() == 0, "Only the empty deed is used; the original stays");
            clear(m);
            m.getSlot(0).set(bound(owner.getUUID(), "Ouzzi", 1)); m.getSlot(1).set(blank());
            yes(h, m.getSlot(2).getItem().isEmpty(), "A copy cannot be copied");
            clear(m);
            var mm = menu(h, manager);
            mm.getSlot(0).set(bound(owner.getUUID(), "Ouzzi", 0)); mm.getSlot(1).set(blank());
            var managed = mm.getSlot(2).getItem().get(DeedData.TYPE);
            yes(h, managed != null && managed.owner().equals(owner.getUUID()) && managed.generation() == 1, "A manager (M) may copy (C8)");
            clear(mm);
            var sm = menu(h, stranger);
            sm.getSlot(0).set(bound(owner.getUUID(), "Ouzzi", 0)); sm.getSlot(1).set(blank());
            yes(h, sm.getSlot(2).getItem().isEmpty(), "Somebody else's deed (rights without M): no copy");
            sm.getSlot(1).set(ItemStack.EMPTY);
            yes(h, sm.getSlot(2).getItem().isEmpty(), "Somebody else's deed alone: no result, it is not changed");
            clear(sm);
            yes(h, BuiltInRegistries.RECIPE_SERIALIZER.containsKey(Identifier.fromNamespaceAndPath("simpletweaks", "claim_deed_copy")), "The crafting table copy stays (C9)");
        } finally { Claims.install(server, previous); }
        h.succeed();
    }

    /** The wrapper: without a deed the table works as before; slots take deeds; shift-click targets. */
    public static void wrap(GameTestHelper h) {
        with(h, c -> {
            var p = player(h, h.absolutePos(new BlockPos(1, 2, 1)));
            var m = menu(h, p);
            yes(h, m.getSlot(0).mayPlace(blank()) && m.getSlot(0).mayPlace(bound(p.getUUID(), "P", 0)), "Top takes empty and bound deeds");
            yes(h, m.getSlot(1).mayPlace(blank()) && !m.getSlot(1).mayPlace(bound(p.getUUID(), "P", 0)), "Below takes only empty deeds");
            yes(h, !m.getSlot(2).mayPlace(blank()), "Nothing goes into the result");
            ItemStack map = MapItem.create(h.getLevel(), 0, 0, (byte) 0, false, false);
            m.getSlot(0).set(map); m.getSlot(1).set(new ItemStack(Items.PAPER));
            ItemStack zoomed = m.getSlot(2).getItem();
            yes(h, zoomed.is(Items.FILLED_MAP), "Without a deed the table still zooms maps (the wrapper passes it on): " + zoomed);
            clear(m);
            yes(h, DeedCartography.quickMoveTarget(blank(), ItemStack.EMPTY) == 0 && DeedCartography.quickMoveTarget(blank(), blank()) == 1
                    && DeedCartography.quickMoveTarget(bound(p.getUUID(), "P", 0), ItemStack.EMPTY) == 0
                    && DeedCartography.quickMoveTarget(new ItemStack(Items.PAPER), ItemStack.EMPTY) == -1, "Shift-click targets");
        });
        h.succeed();
    }

    /** The old claim table: use and placing turn it into a cartography table plus an empty deed; breaking drops both. */
    public static void legacy(GameTestHelper h) {
        var level = h.getLevel();
        var pos = h.absolutePos(new BlockPos(1, 2, 1)); var pos2 = h.absolutePos(new BlockPos(3, 2, 1));
        try {
            var p = player(h, pos.east());
            level.setBlock(pos, ClaimTableBlock.block().defaultBlockState(), Block.UPDATE_ALL);
            var result = level.getBlockState(pos).useWithoutItem(level, p, new BlockHitResult(Vec3.atCenterOf(pos), Direction.UP, pos, false));
            yes(h, result.consumesAction() && level.getBlockState(pos).is(Blocks.CARTOGRAPHY_TABLE), "Use: the old table becomes a cartography table");
            var drops = level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(2));
            yes(h, drops.stream().anyMatch(e -> DeedCartography.isBlank(e.getItem())), "Use: an empty deed falls out");
            drops.forEach(ItemEntity::discard);
            var placer = player(h, pos2.east());
            var stack = new ItemStack(ClaimTableBlock.item());
            level.setBlock(pos2, ClaimTableBlock.block().defaultBlockState(), Block.UPDATE_ALL);
            ClaimTableBlock.block().setPlacedBy(level, pos2, level.getBlockState(pos2), placer, stack);
            yes(h, level.getBlockState(pos2).is(Blocks.CARTOGRAPHY_TABLE), "Placing the old item sets a cartography table");
            yes(h, placer.getInventory().contains(s -> DeedCartography.isBlank(s)), "Placing gives the empty deed back");
            var loot = Block.getDrops(ClaimTableBlock.block().defaultBlockState(), level, pos2, null);
            yes(h, loot.stream().anyMatch(s -> s.is(Items.CARTOGRAPHY_TABLE)) && loot.stream().anyMatch(DeedCartography::isBlank) && loot.size() == 2,
                    "Breaking drops a cartography table and an empty deed: " + loot);
        } finally {
            level.setBlock(pos, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.setBlock(pos2, Blocks.AIR.defaultBlockState(), Block.UPDATE_ALL);
            level.getEntitiesOfClass(ItemEntity.class, new AABB(pos).inflate(4)).forEach(ItemEntity::discard);
        }
        h.succeed();
    }
}
