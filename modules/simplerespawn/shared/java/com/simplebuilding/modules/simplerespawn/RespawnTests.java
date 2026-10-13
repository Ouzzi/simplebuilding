package com.simplebuilding.modules.simplerespawn;

import com.google.gson.JsonObject;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.UUID;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.network.Connection;
import net.minecraft.network.protocol.PacketFlow;
import net.minecraft.network.protocol.game.ServerboundPlayerLoadedPacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.server.network.CommonListenerCookie;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.Pose;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.monster.zombie.Zombie;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.GameType;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

/**
 * Test bodies shared by the Fabric @GameTest wrappers and the NeoForge/Forge registration. The players are real
 * {@link ServerPlayer}s placed through {@code PlayerList#placeNewPlayer} with an embedded connection (a mock player from
 * {@code GameTestHelper} is creative or has no connection), always two of them so "another player is online" holds.
 */
public final class RespawnTests {
    public static final Map<String, Consumer<GameTestHelper>> ALL = new LinkedHashMap<>();

    private RespawnTests() {}

    /** Time limit of a test (the revive needs 5 s of holding). */
    public static int maxTicks(String name) {
        return name.contains("revive_gives") ? 300 : 100;
    }

    private static void add(String name, Consumer<GameTestHelper> body) {
        ALL.put(name, body);
    }

    /** A survival player in the test world that can take damage at once; removed (logout) when the test ends. */
    static ServerPlayer player(GameTestHelper h, String name, double x, double z) {
        ServerLevel level = h.getLevel();
        var cookie = CommonListenerCookie.createInitial(new com.mojang.authlib.GameProfile(UUID.randomUUID(), name), false);
        ServerPlayer player = new ServerPlayer(level.getServer(), level, cookie.gameProfile(), cookie.clientInformation());
        Connection connection = new Connection(PacketFlow.SERVERBOUND);
        new io.netty.channel.embedded.EmbeddedChannel(connection);
        level.getServer().getPlayerList().placeNewPlayer(connection, player, cookie);
        Vec3 at = h.absoluteVec(new Vec3(x, 1.0, z));
        player.snapTo(at.x, at.y, at.z, 0f, 0f);
        player.setGameMode(GameType.SURVIVAL);
        player.connection.handleAcceptPlayerLoad(new ServerboundPlayerLoadedPacket());
        h.runBeforeTestEnd(() -> {
            if (!player.isRemoved()) level.getServer().getPlayerList().remove(player);
        });
        return player;
    }

    static void lethal(GameTestHelper h, ServerPlayer player) {
        player.setInvulnerableTime(0);
        player.damageCooldownTime = 0;
        player.hurtServer(h.getLevel(), player.damageSources().generic(), 1000f);
    }

    static ServerPlayer downedPair(GameTestHelper h, String name) {
        ServerPlayer victim = player(h, name, 1.5, 1.5);
        player(h, name + "-mate", 3.5, 1.5);
        lethal(h, victim);
        h.assertTrue(RespawnServer.isDowned(victim) && victim.isAlive() && victim.getHealth() == 1f, "lethal damage knocks down (alive, 1 health)");
        return victim;
    }

    static {
        add("config_bounds", h -> {
            JsonObject json = new JsonObject();
            json.addProperty("bleedOutSeconds", 99999);
            json.addProperty("reviveSeconds", 0);
            json.addProperty("reviveHealth", 50);
            json.addProperty("crawlSpeed", -3.0);
            json.addProperty("secondsLostPerDamage", Double.NaN);
            JsonObject before = RespawnConfig.toJson();
            try {
                RespawnConfig.apply(json);
                h.assertTrue(RespawnConfig.bleedOutSeconds == 600 && RespawnConfig.reviveSeconds == 1 && RespawnConfig.reviveHealth == 20
                        && RespawnConfig.crawlSpeed == 0.0 && RespawnConfig.secondsLostPerDamage == 0.0, "values clamped");
                RespawnConfig.apply(new JsonObject());
                h.assertTrue(RespawnConfig.bleedOutSeconds == 60 && RespawnConfig.reviveSeconds == 5 && RespawnConfig.reviveHealth == 6
                        && RespawnConfig.reviveFood == 0 && RespawnConfig.onlyWithOtherPlayers && RespawnConfig.mobsIgnoreDowned, "defaults: 60 s, 5 s, 3 hearts, 0 food");
                h.assertTrue(RespawnConfig.KEYS.size() == RespawnConfig.toJson().size(), "every key written");
            } finally {
                RespawnConfig.apply(before);
            }
            h.succeed();
        });
        add("alone_is_normal_death", h -> {
            h.assertTrue(!RespawnServer.qualifies(true, 0), "alone with the default config: Vanilla death");
            h.assertTrue(RespawnServer.qualifies(true, 1) && RespawnServer.qualifies(false, 0), "with a mate, or always by config");
            h.assertTrue(RespawnServer.ticksLost(4f, 1.0) == 80 && RespawnServer.ticksLost(0.5f, 1.0) == 10 && RespawnServer.ticksLost(3f, 0.0) == 0,
                    "one second per point of damage");
            h.succeed();
        });
        add("downed_instead_of_death", h -> {
            ServerPlayer victim = downedPair(h, "downed");
            h.assertTrue(victim.entityTags().contains(RespawnServer.TAG), "crash tag set");
            h.assertTrue(victim.getAttribute(Attributes.JUMP_STRENGTH).getValue() == 0.0, "no jumping");
            h.assertTrue(victim.getAttribute(Attributes.MOVEMENT_SPEED).hasModifier(RespawnServer.SPEED_ID), "crawl slowdown");
            h.assertTrue(RespawnServer.state(victim).ticksLeft == RespawnConfig.bleedOutSeconds * 20, "full bleed-out time");
            h.runAfterDelay(2, () -> {
                h.assertTrue(victim.getPose() == Pose.SWIMMING, "lying (swimming pose)");
                h.assertTrue(!victim.canBeSeenAsEnemy(), "monsters ignore a downed player");
                Zombie zombie = net.minecraft.world.entity.EntityTypes.ZOMBIE.create(h.getLevel(), EntitySpawnReason.COMMAND);
                h.assertTrue(zombie != null && !zombie.canAttack(victim), "a zombie may not attack the downed player");
                h.succeed();
            });
        });
        add("totem_and_void_stay_vanilla", h -> {
            ServerPlayer holder = player(h, "totem", 1.5, 1.5);
            ServerPlayer voidVictim = player(h, "void", 3.5, 1.5);
            holder.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.TOTEM_OF_UNDYING));
            lethal(h, holder);
            h.assertTrue(!RespawnServer.isDowned(holder) && holder.isAlive() && holder.getMainHandItem().isEmpty(), "the totem works first");
            voidVictim.setInvulnerableTime(0);
        voidVictim.damageCooldownTime = 0;
            voidVictim.hurtServer(h.getLevel(), voidVictim.damageSources().fellOutOfWorld(), 1000f);
            h.assertTrue(!RespawnServer.isDowned(voidVictim) && voidVictim.isDeadOrDying(), "the void kills at once");
            h.succeed();
        });
        add("bleed_out_is_death", h -> {
            ServerPlayer victim = downedPair(h, "bleed");
            victim.getInventory().add(new ItemStack(Items.DIAMOND, 3));
            RespawnServer.state(victim).ticksLeft = 3;
            h.runAfterDelay(6, () -> {
                h.assertTrue(!RespawnServer.isDowned(victim) && victim.isDeadOrDying(), "time ran out: dead");
                h.assertTrue(!victim.entityTags().contains(RespawnServer.TAG), "tag removed");
                h.succeed();
            });
        });
        add("damage_shortens_bleed_out", h -> {
            ServerPlayer victim = downedPair(h, "hurt");
            int before = RespawnServer.state(victim).ticksLeft;
            lethal(h, victim);
            h.assertTrue(!RespawnServer.isDowned(victim) && victim.isDeadOrDying(), "1000 damage while downed uses up the time: dead");
            ServerPlayer second = downedPair(h, "hurt2");
            second.setInvulnerableTime(0);
        second.damageCooldownTime = 0;
            second.hurtServer(h.getLevel(), second.damageSources().generic(), 5f);
            h.assertTrue(RespawnServer.isDowned(second) && RespawnServer.state(second).ticksLeft == before - 100 && second.getHealth() == 1f,
                    "5 damage costs 5 seconds, still down at 1 health");
            h.succeed();
        });
        add("give_up_is_death", h -> {
            ServerPlayer victim = downedPair(h, "giveup");
            var server = h.getLevel().getServer();
            server.getCommands().performPrefixedCommand(victim.createCommandSourceStack(), "simplerespawn giveup");
            h.assertTrue(!RespawnServer.isDowned(victim) && victim.isDeadOrDying(), "give up: dead");
            h.succeed();
        });
        add("revive_gives_three_hearts_no_food", h -> {
            ServerPlayer victim = downedPair(h, "revive");
            ServerPlayer helper = player(h, "helper", 2.0, 1.5);
            victim.getFoodData().setFoodLevel(18);
            int[] held = {0};
            h.onEachTick(() -> {
                if (RespawnServer.isDowned(victim)) {
                    InteractionResult result = helper.interactOn(victim, InteractionHand.MAIN_HAND, Vec3.ZERO);
                    h.assertTrue(result.consumesAction(), "the right-click on the body is used");
                    held[0]++;
                }
            });
            h.succeedWhen(() -> {
                h.assertTrue(!RespawnServer.isDowned(victim), "still downed");
                h.assertTrue(held[0] >= RespawnConfig.reviveSeconds * 20, "revived too fast: " + held[0]);
                h.assertValueEqual(victim.getHealth(), 6f, "health after revive");
                h.assertValueEqual(victim.getFoodData().getFoodLevel(), 0, "food after revive");
                h.assertTrue(victim.hasEffect(net.minecraft.world.effect.MobEffects.RESISTANCE), "short resistance");
                h.assertTrue(victim.getAttribute(Attributes.JUMP_STRENGTH).getValue() > 0.0, "can jump again");
            });
        });
        add("revive_breaks_off", h -> {
            ServerPlayer victim = downedPair(h, "far");
            ServerPlayer helper = player(h, "farhelper", 2.0, 1.5);
            helper.interactOn(victim, InteractionHand.MAIN_HAND, Vec3.ZERO);
            h.runAfterDelay(3, () -> {
                h.assertTrue(RespawnServer.state(victim).progress > 0, "revive started");
                Vec3 away = h.absoluteVec(new Vec3(7.5, 1.0, 7.5));
                helper.snapTo(away.x, away.y, away.z, 0f, 0f);
                helper.interactOn(victim, InteractionHand.MAIN_HAND, Vec3.ZERO);
            });
            h.runAfterDelay(6, () -> {
                h.assertTrue(RespawnServer.isDowned(victim) && RespawnServer.state(victim).reviver == null, "too far away: the revive stops");
                h.succeed();
            });
        });
        add("logout_is_death_without_dupe", h -> {
            ServerPlayer victim = downedPair(h, "logout");
            victim.getInventory().add(new ItemStack(Items.EMERALD, 7));
            BlockPos at = victim.blockPosition();
            h.getLevel().getServer().getPlayerList().remove(victim);
            h.assertTrue(!RespawnServer.isDowned(victim) && victim.isDeadOrDying(), "logout while downed: dead");
            h.assertTrue(victim.getInventory().countItem(Items.EMERALD) == 0, "nothing stays in the saved inventory");
            int dropped = h.getLevel().getEntitiesOfClass(ItemEntity.class, new AABB(at).inflate(4))
                    .stream().filter(e -> e.getItem().is(Items.EMERALD)).mapToInt(e -> e.getItem().getCount()).sum();
            h.assertValueEqual(dropped, 7, "emeralds dropped in the world");
            h.succeed();
        });
        add("crash_tag_is_death", h -> {
            ServerPlayer victim = player(h, "crash", 1.5, 1.5);
            player(h, "crash-mate", 3.5, 1.5);
            victim.addTag(RespawnServer.TAG);
            h.succeedWhen(() -> h.assertTrue(victim.isDeadOrDying() && !victim.entityTags().contains(RespawnServer.TAG),
                    "a player saved while downed dies when seen again"));
        });
        add("downed_cannot_act", h -> {
            ServerPlayer victim = downedPair(h, "act");
            ServerPlayer other = player(h, "act-target", 2.0, 1.5);
            victim.setItemInHand(InteractionHand.MAIN_HAND, new ItemStack(Items.DIAMOND_SWORD));
            float health = other.getHealth();
            victim.attack(other);
            h.assertTrue(other.getHealth() == health, "no attack while downed");
            h.assertTrue(victim.interactOn(other, InteractionHand.MAIN_HAND, Vec3.ZERO) == InteractionResult.PASS, "no interaction while downed");
            h.assertTrue(RespawnServer.blocksActions(victim) && !RespawnServer.blocksActions(other), "packets of the downed player are refused");
            victim.setGameMode(GameType.CREATIVE);
            h.runAfterDelay(2, () -> {
                h.assertTrue(!RespawnServer.isDowned(victim), "creative ends the downed state");
                h.succeed();
            });
        });
        add("guide_book", com.simplebuilding.modules.simplerespawn.guide.RespawnGuide::gameTest);
        add("no_simplebuilding_needed", h -> {
            boolean sb = net.minecraft.core.registries.BuiltInRegistries.ITEM.keySet().stream().anyMatch(id -> id.getNamespace().equals("simplebuilding"));
            if (!sb) SimpleRespawn.LOG.info("[standalone] Simple Respawn runs without SimpleBuilding");
            h.assertTrue(SimpleRespawn.on(), "module switched on");
            h.succeed();
        });
    }

    // Discoverability stage 1 (generated bodies, tools/discover/discover.py)
    static {
        ALL.put("discover_recipe_unlocks", com.simplebuilding.modules.simplerespawn.discover.RespawnDiscover::recipeUnlocks);
        ALL.put("discover_behaviour_hints", com.simplebuilding.modules.simplerespawn.discover.RespawnDiscover::behaviourHints);
    }
}
