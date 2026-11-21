package com.simplebuilding.modules.simpletweaks.claims;
import java.util.*;
import net.minecraft.core.*;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.*;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.Enemy;
import net.minecraft.world.entity.projectile.arrow.AbstractArrow;
import net.minecraft.world.entity.projectile.arrow.ThrownTrident;
import net.minecraft.world.item.*;
import net.minecraft.world.level.*;
import net.minecraft.world.phys.*;
import static com.simplebuilding.modules.simpletweaks.claims.ClaimTests.*;

/**
 * E1ANS F2 (owner 12.11.): what a visitor without rights may still do in foreign land. Monsters can be fought, own items and
 * experience orbs can be picked up, a refused trident or arrow goes back to the shooter exactly once. Animals and strangers' items
 * stay protected; every part has its option ({@code visitors.*}).
 */
final class VisitorClaimTests {
    private static final ClaimConfig.Visitors ALL_OFF = new ClaimConfig.Visitors(false, false, false, false);

    /** A claim of {@code owner} around {@code inside}; the visitor has no rights. */
    private record Land(BlockPos inside, ServerPlayer owner, ServerPlayer visitor) {}
    private static Land land(GameTestHelper h, Claims c, BlockPos inside) {
        var owner = player(h, inside.west(10)); var visitor = player(h, inside.west(10));
        yes(h, c.create(new ClaimStore.Key(h.getLevel().dimension().identifier().toString(), ChunkPos.pack(inside)), owner.getUUID(), 100), "Visitor claim fixture");
        return new Land(inside, owner, visitor);
    }
    private static <T extends Mob> T mob(GameTestHelper h, EntityType<T> type, BlockPos at) {
        var l = h.getLevel();
        T e = type.create(l, EntitySpawnReason.COMMAND);
        e.setNoAi(true); e.setPersistenceRequired(); e.setPos(Vec3.atBottomCenterOf(at)); l.addFreshEntity(e);
        h.runBeforeTestEnd(e::discard);
        return e;
    }

    // ------------------------------------------------------------------ monsters
    static void monsters(GameTestHelper h) {
        var l = h.getLevel(); var inside = ToolClaimTests.boundary(h).above(80);
        with(h, c -> {
            var v = land(h, c, inside).visitor();
            var zombie = mob(h, EntityTypes.ZOMBIE, inside); var cow = mob(h, EntityTypes.COW, inside.south(3));
            var zombie2 = mob(h, EntityTypes.ZOMBIE, inside.north(3)); var cow2 = mob(h, EntityTypes.COW, inside.east(3));
            yes(h, zombie instanceof Enemy && !(cow instanceof Enemy), "Fixture: a monster and an animal");
            yes(h, Claims.allowAttack(v, zombie) && !Claims.allowAttack(v, cow), "A visitor may attack a monster, not an animal");
            yes(h, !Claims.allowEntity(v, zombie), "Using a monster (right click) still needs the animal right");
            float zh = zombie.getHealth(), ch = cow.getHealth();
            yes(h, zombie.hurtServer(l, l.damageSources().playerAttack(v), 2f) && zombie.getHealth() < zh, "A visitor hurts a monster in foreign land");
            yes(h, !cow.hurtServer(l, l.damageSources().playerAttack(v), 2f) && cow.getHealth() == ch, "An animal in foreign land stays protected");
            float z2 = zombie2.getHealth(), c2 = cow2.getHealth();
            v.attack(zombie2); v.attack(cow2);
            yes(h, zombie2.getHealth() < z2, "The real attack hits the monster");
            yes(h, cow2.getHealth() == c2, "The real attack on the animal is refused");
            var effect = new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 100);
            yes(h, zombie.addEffect(effect, v) && !cow.addEffect(new net.minecraft.world.effect.MobEffectInstance(net.minecraft.world.effect.MobEffects.SLOWNESS, 100), v),
                    "A harmful effect of a visitor reaches the monster, not the animal");
        });
        with(h, on().withVisitors(ALL_OFF), c -> {
            var v = land(h, c, inside).visitor();
            var zombie = mob(h, EntityTypes.ZOMBIE, inside); float zh = zombie.getHealth();
            yes(h, !Claims.allowAttack(v, zombie) && !zombie.hurtServer(l, l.damageSources().playerAttack(v), 2f) && zombie.getHealth() == zh,
                    "visitors.fightMonsters off: monsters are protected like animals");
        });
        h.succeed();
    }

    // ------------------------------------------------------------------ own items and experience
    static void pickup(GameTestHelper h) {
        var l = h.getLevel(); var inside = ToolClaimTests.boundary(h).east(3).above(90);
        with(h, c -> {
            var land = land(h, c, inside); var owner = land.owner(); var v = land.visitor();
            var mine = drop(h, inside, new ItemStack(Items.APPLE, 2)); mine.setThrower(v);
            var theirs = drop(h, inside, new ItemStack(Items.CARROT, 2)); theirs.setThrower(owner);
            var nobody = drop(h, inside, new ItemStack(Items.BREAD, 2));
            theirs.playerTouch(v); nobody.playerTouch(v);
            yes(h, !theirs.isRemoved() && !nobody.isRemoved() && v.getInventory().countItem(Items.CARROT) + v.getInventory().countItem(Items.BREAD) == 0,
                    "A visitor still cannot take the owner's items or items nobody dropped");
            mine.playerTouch(v);
            yes(h, mine.isRemoved() && v.getInventory().countItem(Items.APPLE) == 2, "A visitor takes back an item they dropped");
            // an own death drop: Vanilla gives it no thrower, the hook on LivingEntity#createItemStackToDrop does
            var death = v.createItemStackToDrop(new ItemStack(Items.GOLD_INGOT, 3), true, false);
            death.setPos(Vec3.atBottomCenterOf(inside)); death.setNoPickUpDelay(); l.addFreshEntity(death); h.runBeforeTestEnd(death::discard);
            yes(h, death.getOwner() != null && death.getOwner().getUUID().equals(v.getUUID()), "An own death drop carries its player as thrower");
            var stranger = player(h, inside.west(10));
            death.playerTouch(stranger);
            yes(h, !death.isRemoved() && stranger.getInventory().countItem(Items.GOLD_INGOT) == 0, "Another visitor cannot take it");
            death.playerTouch(v);
            yes(h, death.isRemoved() && v.getInventory().countItem(Items.GOLD_INGOT) == 3, "The player takes their own death drop back");
            var orb = new ExperienceOrb(l, inside.getX() + .5, inside.getY(), inside.getZ() + .5, 5);
            l.addFreshEntity(orb); h.runBeforeTestEnd(orb::discard); v.takeXpDelay = 0;
            orb.playerTouch(v);
            yes(h, orb.isRemoved(), "A visitor collects experience in foreign land");
        });
        with(h, on().withVisitors(ALL_OFF), c -> {
            var land = land(h, c, inside); var v = land.visitor();
            var mine = drop(h, inside, new ItemStack(Items.APPLE, 2)); mine.setThrower(v);
            var orb = new ExperienceOrb(l, inside.getX() + .5, inside.getY(), inside.getZ() + .5, 5);
            l.addFreshEntity(orb); h.runBeforeTestEnd(orb::discard); v.takeXpDelay = 0;
            mine.playerTouch(v); orb.playerTouch(v);
            yes(h, !mine.isRemoved() && v.getInventory().countItem(Items.APPLE) == 0, "visitors.ownItems off: own items are protected like the rest");
            yes(h, !orb.isRemoved(), "visitors.xp off: experience is protected like before");
            var death = v.createItemStackToDrop(new ItemStack(Items.GOLD_INGOT, 3), true, false);
            yes(h, death.getOwner() == null, "visitors.ownItems off: Vanilla death drops are left alone");
        });
        h.succeed();
    }
    private static ItemEntity drop(GameTestHelper h, BlockPos at, ItemStack stack) {
        var e = new ItemEntity(h.getLevel(), at.getX() + .5, at.getY(), at.getZ() + .5, stack);
        e.setNoPickUpDelay(); h.getLevel().addFreshEntity(e); h.runBeforeTestEnd(e::discard);
        return e;
    }

    // ------------------------------------------------------------------ projectiles
    /** Starts {@code projectile} 3 blocks west of {@code target} flying east and ticks it until it ends. */
    private static void shoot(Entity projectile, BlockPos target) {
        projectile.setNoGravity(true); projectile.setPos(target.getX() - 3, target.getY() + .7, target.getZ() + .5); projectile.setDeltaMovement(1, 0, 0);
        projectile.level().addFreshEntity(projectile);
        for (int t = 0; t < 8 && !projectile.isRemoved(); t++) projectile.tick();
        projectile.discard();
    }
    private static AbstractArrow arrow(GameTestHelper h, ServerPlayer shooter, AbstractArrow.Pickup pickup) {
        var a = EntityTypes.ARROW.create(h.getLevel(), EntitySpawnReason.COMMAND);
        a.setOwner(shooter); a.pickup = pickup;
        return a;
    }
    static void projectiles(GameTestHelper h) {
        // The targets lie east of the test structure; until they are entity-visible the projectile's hit query misses them.
        var l = h.getLevel(); var inside = ToolClaimTests.boundary(h).east(2).above(80); var chunk = ChunkPos.containing(inside);
        ClaimTests.entityTicking(h, List.of(chunk));
        var cow = mob(h, EntityTypes.COW, inside); var zombie = mob(h, EntityTypes.ZOMBIE, inside.south(3));
        h.startSequence().thenWaitUntil(() -> yes(h, ClaimTests.entityTicking(h, List.of(chunk)) && l.getEntitiesOfClass(Entity.class, new AABB(inside).inflate(5)).containsAll(List.of(cow, zombie)), "Projectile targets are not queryable yet"))
                .thenExecute(() -> with(h, c -> {
                    var v = land(h, c, inside).visitor(); var inv = v.getInventory();
                    float ch = cow.getHealth(), zh = zombie.getHealth();
                    // refused arrow with pickup allowed: removed, handed back once
                    var a1 = arrow(h, v, AbstractArrow.Pickup.ALLOWED); shoot(a1, inside);
                    yes(h, cow.getHealth() == ch && a1.isRemoved() && inv.countItem(Items.ARROW) == 1, "A refused arrow is gone and returns exactly once: " + inv.countItem(Items.ARROW));
                    // a second refusal of the same (already removed) projectile, e.g. a second hit result of one tick, gives nothing again
                    Claims.refuse(a1);
                    yes(h, inv.countItem(Items.ARROW) == 1, "No second item for the same projectile: " + inv.countItem(Items.ARROW));
                    // arrows that cannot be picked up (Infinity, creative) vanish as before
                    shoot(arrow(h, v, AbstractArrow.Pickup.CREATIVE_ONLY), inside); shoot(arrow(h, v, AbstractArrow.Pickup.DISALLOWED), inside);
                    yes(h, cow.getHealth() == ch && inv.countItem(Items.ARROW) == 1, "Creative and non-pickup arrows are not returned: " + inv.countItem(Items.ARROW));
                    // a trident
                    var trident = new ThrownTrident(l, v, new ItemStack(Items.TRIDENT)); trident.pickup = AbstractArrow.Pickup.ALLOWED; shoot(trident, inside);
                    yes(h, cow.getHealth() == ch && trident.isRemoved() && inv.countItem(Items.TRIDENT) == 1, "A refused trident comes back once: " + inv.countItem(Items.TRIDENT));
                    // no room: at the shooter's feet, with the shooter as thrower
                    for (int i = 0; i < 36; i++) inv.setItem(i, new ItemStack(Items.DIRT, 64));
                    int before = arrowsOnTheGround(l, v);
                    shoot(arrow(h, v, AbstractArrow.Pickup.ALLOWED), inside);
                    var dropped = l.getEntitiesOfClass(ItemEntity.class, v.getBoundingBox().inflate(2), e -> e.getItem().is(Items.ARROW));
                    dropped.forEach(e -> h.runBeforeTestEnd(e::discard));
                    yes(h, arrowsOnTheGround(l, v) == before + 1 && dropped.stream().anyMatch(e -> e.getOwner() != null && e.getOwner().getUUID().equals(v.getUUID())),
                            "With a full inventory the arrow lies at the shooter's feet, once");
                    for (int i = 0; i < 36; i++) inv.setItem(i, ItemStack.EMPTY);
                    // a monster is shot, not refused
                    int arrows = inv.countItem(Items.ARROW);
                    shoot(arrow(h, v, AbstractArrow.Pickup.ALLOWED), inside.south(3));
                    yes(h, zombie.getHealth() < zh && inv.countItem(Items.ARROW) == arrows, "A visitor shoots a monster: it is hurt, nothing is returned");
                }))
                .thenExecute(() -> with(h, on().withVisitors(ALL_OFF), c -> {
                    var v = land(h, c, inside).visitor(); var inv = v.getInventory();
                    float ch = cow.getHealth(), zh = zombie.getHealth();
                    shoot(arrow(h, v, AbstractArrow.Pickup.ALLOWED), inside);
                    shoot(arrow(h, v, AbstractArrow.Pickup.ALLOWED), inside.south(3));
                    yes(h, cow.getHealth() == ch && zombie.getHealth() == zh && inv.countItem(Items.ARROW) == 0,
                            "All visitor options off: arrows are refused and lost, monsters protected (health " + zombie.getHealth() + ")");
                }))
                .thenSucceed();
    }
    private static int arrowsOnTheGround(net.minecraft.server.level.ServerLevel l, ServerPlayer p) {
        return l.getEntitiesOfClass(ItemEntity.class, p.getBoundingBox().inflate(2), e -> e.getItem().is(Items.ARROW)).stream().mapToInt(e -> e.getItem().getCount()).sum();
    }

    // ------------------------------------------------------------------ config
    static void config(GameTestHelper h) {
        var d = ClaimConfig.DEFAULT.visitors();
        yes(h, d.fightMonsters() && d.ownItems() && d.xp() && d.returnProjectiles(), "Every visitor part is on by default");
        try {
            var p = temp().resolve("config.json");
            yes(h, ClaimConfig.load(p).visitors().equals(d) && java.nio.file.Files.readString(p).contains("\"visitors\""), "A new file carries the visitor section");
            java.nio.file.Files.writeString(p, "{\"configVersion\":" + ClaimConfig.CONFIG_VERSION + ",\"maxClaimsPerPlayer\":4}");
            yes(h, ClaimConfig.load(p).visitors().equals(d), "An existing file without the section takes the defaults");
            java.nio.file.Files.writeString(p, "{\"configVersion\":" + ClaimConfig.CONFIG_VERSION + ",\"visitors\":{\"xp\":false}}");
            var v = ClaimConfig.load(p).visitors();
            yes(h, !v.xp() && v.fightMonsters() && v.ownItems() && v.returnProjectiles(), "A single part can be switched off");
            java.nio.file.Files.writeString(p, "{\"configVersion\":" + ClaimConfig.CONFIG_VERSION + ",\"visitors\":{\"xp\":\"no\"}}");
            try { ClaimConfig.load(p); throw new AssertionError("Nonboolean visitor switch accepted"); } catch (IllegalStateException expected) {}
        } catch (java.io.IOException e) { throw new AssertionError(e); }
        h.succeed();
    }
    private VisitorClaimTests() {}
}
