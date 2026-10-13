package com.simplemobheads.drop;

import com.mojang.serialization.Codec;
import com.mojang.serialization.MapCodec;
import com.simplemobheads.SimpleMobHeads;
import com.simplemobheads.MobHeads;
import com.simplemobheads.config.MobHeadsConfig;
import com.simplemobheads.data.HeadDef;
import net.minecraft.world.level.storage.loot.LootContext;
import net.minecraft.world.level.storage.loot.predicates.LootItemCondition;

/**
 * Loot condition {@code simplemobheads:head_drop}: the server switches, read when the loot is rolled (no reload needed) -
 * master switch, creeper drops, the boss heads (wither default off, warden default on, owner 13.10.).
 */
public record HeadDropCondition(String head) implements LootItemCondition {
    public static final MapCodec<HeadDropCondition> CODEC = Codec.STRING.fieldOf("head").xmap(HeadDropCondition::new, HeadDropCondition::head);

    @Override
    public MapCodec<HeadDropCondition> codec() {
        return CODEC;
    }

    @Override
    public boolean test(LootContext context) {
        HeadDef def = MobHeads.head(head);
        return def != null && SimpleMobHeads.config().creeperDrops && allowed(def);
    }

    /** Whether the server lets this head drop at all (both drop ways). */
    public static boolean allowed(HeadDef def) {
        if (!SimpleMobHeads.on()) {
            return false;
        }
        MobHeadsConfig config = SimpleMobHeads.config();
        return switch (def.drop()) {
            case "wither" -> config.witherHead;
            case "warden" -> config.wardenHead;
            default -> true;
        };
    }
}
