package com.simplemobheads.easter;

import com.simplemobheads.SimpleMobHeads;
import net.minecraft.core.Holder;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.tags.TagKey;
import net.minecraft.world.entity.EntitySpawnReason;
import net.minecraft.world.entity.ai.attributes.AttributeInstance;
import net.minecraft.world.entity.ai.attributes.AttributeModifier;
import net.minecraft.world.entity.ai.attributes.Attributes;
import net.minecraft.world.entity.animal.rabbit.Rabbit;
import net.minecraft.world.level.ServerLevelAccessor;
import net.minecraft.world.level.biome.Biome;

/**
 * The killer bunny (EH21 KO-2 + owner: "bites lightly, is fast and always hostile"; docs/ai/PLAN-B-EH21-2026-10-22.md). Vanilla
 * has the variant ({@code evil}: always hunts players and wolves, never flees) but never spawns it. Here one naturally spawned
 * rabbit in {@code killerBunnyChance} (1 in 1000) in the biomes of {@code #simplemobheads:killer_bunny_spawns} (flower forest,
 * meadow) is born a killer bunny, with two lasting modifiers: its bite only does one heart (Vanilla 4) and it hops 1.5x as fast.
 * Its head is the rabbit head's killer bunny variant, dropped by a charged creeper like every head. Killer bunnies from commands
 * stay Vanilla.
 */
public final class KillerBunny {
    public static final TagKey<Biome> BIOMES = TagKey.create(Registries.BIOME, SimpleMobHeads.id("killer_bunny_spawns"));
    /** Vanilla's killer bunny bites for 3 + 5; with this it bites for 2 (one heart). */
    public static final double DAMAGE = -6.0;
    /** Movement x1.5. */
    public static final double SPEED = 0.5;
    public static final Identifier DAMAGE_ID = SimpleMobHeads.id("killer_bunny_damage");
    public static final Identifier SPEED_ID = SimpleMobHeads.id("killer_bunny_speed");

    private KillerBunny() {
    }

    /** {@code Rabbit#finalizeSpawn} ({@code RabbitSpawnMixin}): the rare roll; returns whether the rabbit became a killer bunny. */
    public static boolean onSpawn(Rabbit rabbit, ServerLevelAccessor level, EntitySpawnReason reason) {
        return onSpawn(rabbit, reason, level.getBiome(rabbit.blockPosition()), level.getRandom().nextFloat());
    }

    /** The roll with a given biome and roll in [0, 1) (tests). */
    public static boolean onSpawn(Rabbit rabbit, EntitySpawnReason reason, Holder<Biome> biome, float roll) {
        if (!eligible(reason, biome) || roll >= SimpleMobHeads.config().killerBunnyChance) {
            return false;
        }
        turn(rabbit);
        return true;
    }

    /** Switches on, a natural spawn (or world generation) and a killer bunny biome. */
    public static boolean eligible(EntitySpawnReason reason, Holder<Biome> biome) {
        return SimpleMobHeads.on() && SimpleMobHeads.config().killerBunnies
                && (reason == EntitySpawnReason.NATURAL || reason == EntitySpawnReason.CHUNK_GENERATION) && biome.is(BIOMES);
    }

    /** Makes {@code rabbit} a killer bunny: Vanilla's variant (goals, armor, name) plus the light bite and the speed. */
    public static void turn(Rabbit rabbit) {
        rabbit.setComponent(DataComponents.RABBIT_VARIANT, Rabbit.Variant.EVIL);
        AttributeInstance damage = rabbit.getAttribute(Attributes.ATTACK_DAMAGE);
        if (damage != null) {
            damage.addOrReplacePermanentModifier(new AttributeModifier(DAMAGE_ID, DAMAGE, AttributeModifier.Operation.ADD_VALUE));
        }
        AttributeInstance speed = rabbit.getAttribute(Attributes.MOVEMENT_SPEED);
        if (speed != null) {
            speed.addOrReplacePermanentModifier(new AttributeModifier(SPEED_ID, SPEED, AttributeModifier.Operation.ADD_MULTIPLIED_BASE));
        }
    }
}
