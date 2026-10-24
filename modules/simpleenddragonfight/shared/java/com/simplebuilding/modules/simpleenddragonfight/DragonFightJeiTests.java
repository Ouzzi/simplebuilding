package com.simplebuilding.modules.simpleenddragonfight;

import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.storage.loot.LootParams;
import net.minecraft.world.level.storage.loot.LootTable;
import net.minecraft.world.level.storage.loot.parameters.LootContextParamSets;
import net.minecraft.world.level.storage.loot.parameters.LootContextParams;

/** JEI24: the JEI info pages of the Dragonmite's drop ({@link DragonFightJeiInfo}) against the server's loot tables (64 rolls each). */
public final class DragonFightJeiTests {
    private DragonFightJeiTests() {
    }

    public static void mobDropPages(GameTestHelper h) {
        for (Map.Entry<EntityType<?>, List<Item>> mob : DragonFightJeiInfo.drops().entrySet()) {
            Set<Item> rolled = roll(h, mob.getKey(), 64);
            h.assertTrue(!rolled.isEmpty() && mob.getValue().containsAll(rolled),
                    mob.getKey() + " drops " + rolled + ", the JEI page names " + mob.getValue());
        }
        h.assertTrue(!DragonFightJeiInfo.pages().isEmpty(), "a page per mob");
        h.succeed();
    }

    /** Every item the mob's loot table gives in {@code rolls} deaths (killed by a player without Looting). */
    static Set<Item> roll(GameTestHelper h, EntityType<?> type, int rolls) {
        Set<Item> out = new LinkedHashSet<>();
        var key = type.getDefaultLootTable().orElseThrow();
        LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(key);
        Entity entity = type.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        h.assertTrue(entity != null && table != LootTable.EMPTY, "loot table " + key.identifier() + " and mob present");
        entity.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
        // A player kill (loot conditions like killed_by_player), Looting 0.
        net.minecraft.world.entity.player.Player player = h.makeMockPlayer(net.minecraft.world.level.GameType.SURVIVAL);
        try {
            for (int i = 0; i < rolls; i++) {
                LootParams params = new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.THIS_ENTITY, entity)
                        .withParameter(LootContextParams.ORIGIN, entity.position())
                        .withParameter(LootContextParams.DAMAGE_SOURCE, h.getLevel().damageSources().playerAttack(player))
                        .withParameter(LootContextParams.ATTACKING_ENTITY, player)
                        .withParameter(LootContextParams.LAST_DAMAGE_PLAYER, player)
                        .create(LootContextParamSets.ENTITY);
                for (ItemStack stack : table.getRandomItems(params)) if (!stack.isEmpty()) out.add(stack.getItem());
            }
        } finally {
            entity.discard();
        }
        return out;
    }
}
