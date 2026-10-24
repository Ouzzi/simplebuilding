package com.simplebuilding.modules.simpleend;

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

/** JEI24: the JEI info pages of Simple End's mob drops ({@link EndJeiInfo}) against the server's loot tables (64 rolls each). */
public final class EndJeiTests {
    private EndJeiTests() {
    }

    public static void mobDropPages(GameTestHelper h) {
        for (Map.Entry<EntityType<?>, List<Item>> mob : EndJeiInfo.drops().entrySet()) {
            Set<Item> rolled = roll(h, mob.getKey(), 64);
            h.assertTrue(!rolled.isEmpty() && mob.getValue().containsAll(rolled),
                    mob.getKey() + " drops " + rolled + ", the JEI page names " + mob.getValue());
        }
        h.assertTrue(!EndJeiInfo.pages().isEmpty(), "a page per mob");
        h.succeed();
    }

    /** Every item the mob's loot table gives in {@code rolls} deaths (no killer, Looting 0). */
    static Set<Item> roll(GameTestHelper h, EntityType<?> type, int rolls) {
        Set<Item> out = new LinkedHashSet<>();
        var key = type.getDefaultLootTable().orElseThrow();
        LootTable table = h.getLevel().getServer().reloadableRegistries().getLootTable(key);
        Entity entity = type.create(h.getLevel(), net.minecraft.world.entity.EntitySpawnReason.COMMAND);
        h.assertTrue(entity != null && table != LootTable.EMPTY, "loot table " + key.identifier() + " and mob present");
        entity.setPos(h.absoluteVec(new net.minecraft.world.phys.Vec3(1, 2, 1)));
        try {
            for (int i = 0; i < rolls; i++) {
                LootParams params = new LootParams.Builder(h.getLevel()).withParameter(LootContextParams.THIS_ENTITY, entity)
                        .withParameter(LootContextParams.ORIGIN, entity.position())
                        .withParameter(LootContextParams.DAMAGE_SOURCE, h.getLevel().damageSources().generic())
                        .create(LootContextParamSets.ENTITY);
                for (ItemStack stack : table.getRandomItems(params)) out.add(stack.getItem());
            }
        } finally {
            entity.discard();
        }
        return out;
    }
}
