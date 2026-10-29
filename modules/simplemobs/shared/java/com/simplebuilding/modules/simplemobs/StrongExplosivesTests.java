package com.simplebuilding.modules.simplemobs;

import java.util.List;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.item.PrimedTnt;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.CraftingInput;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.phys.AABB;

/** Strong Gunpowder, Strong Rocket and Strong TNT (BS25-21; Fabric wrappers in ModuleGameTest). Standalone-safe: nothing here needs another mod. */
final class StrongExplosivesTests {
    private StrongExplosivesTests() {}

    @SuppressWarnings("unchecked")
    private static ItemStack craft(GameTestHelper h, String recipe, int width, int height, List<ItemStack> stacks) {
        var holder = h.getLevel().getServer().getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(MobsRegistry.ID, recipe)))
                .orElseThrow(() -> new IllegalStateException("missing recipe " + recipe));
        var r = (Recipe<CraftingInput>) holder.value();
        var input = CraftingInput.of(width, height, stacks);
        h.assertTrue(r.matches(input, h.getLevel()), "recipe matches " + recipe);
        return r.assemble(input);
    }

    private static float powerOf(PrimedTnt tnt) {
        try {
            var field = PrimedTnt.class.getDeclaredField("explosionPower");
            field.setAccessible(true);
            return field.getFloat(tnt);
        } catch (ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("strong_items", h -> {
            h.assertTrue(BuiltInRegistries.ITEM.getKey(StrongExplosives.GUNPOWDER).equals(Identifier.fromNamespaceAndPath(MobsRegistry.ID, "strong_gunpowder")),
                    "Strong Gunpowder registered");
            h.assertTrue(BuiltInRegistries.BLOCK.getKey(StrongExplosives.TNT_BLOCK).equals(Identifier.fromNamespaceAndPath(MobsRegistry.ID, "strong_tnt"))
                    && StrongExplosives.TNT.asItem() == StrongExplosives.TNT && new ItemStack(StrongExplosives.TNT).getItem() == StrongExplosives.TNT,
                    "Strong TNT block and item registered");
            h.assertTrue(StrongExplosives.TNT_BLOCK.defaultBlockState().hasProperty(StrongTntBlock.UNSTABLE)
                    && !StrongExplosives.TNT_BLOCK.defaultBlockState().getValue(StrongTntBlock.UNSTABLE), "stable by default like TNT");
            h.assertTrue(StrongExplosives.DEFAULT_POWER > 4.0f && MobsConfig.strongTntPower == StrongExplosives.DEFAULT_POWER, "stronger than Vanilla TNT (4) by default");
            ItemStack rocket = StrongExplosives.rocket(3);
            h.assertTrue(rocket.is(Items.FIREWORK_ROCKET) && rocket.getCount() == 3 && WitherWings.strongRocket(rocket)
                    && rocket.get(DataComponents.FIREWORKS).flightDuration() == WitherWings.STRONG_FLIGHT, "the strong rocket is a Vanilla rocket of flight 4");
            h.assertTrue(!WitherWings.strongRocket(new ItemStack(Items.FIREWORK_ROCKET)), "the Vanilla rocket stays normal");
            h.succeed();
        });
        add.accept("strong_recipes", h -> {
            ItemStack powder = craft(h, "strong_gunpowder", 2, 2, List.of(new ItemStack(Items.GUNPOWDER), new ItemStack(Items.GUNPOWDER),
                    new ItemStack(Items.GUNPOWDER), new ItemStack(Items.BLAZE_POWDER)));
            h.assertTrue(powder.is(StrongExplosives.GUNPOWDER) && powder.getCount() == 2, "3 gunpowder + 1 blaze powder -> 2 Strong Gunpowder: " + powder);
            ItemStack rockets = craft(h, "strong_firework_rocket", 2, 1, List.of(new ItemStack(Items.PAPER), new ItemStack(StrongExplosives.GUNPOWDER)));
            h.assertTrue(rockets.is(Items.FIREWORK_ROCKET) && rockets.getCount() == 3 && WitherWings.strongRocket(rockets), "paper + Strong Gunpowder -> 3 strong rockets: " + rockets);
            ItemStack gunpowder = new ItemStack(StrongExplosives.GUNPOWDER);
            ItemStack sand = new ItemStack(Items.SAND);
            ItemStack tnt = craft(h, "strong_tnt", 3, 3, List.of(gunpowder, sand, gunpowder, sand, gunpowder, sand, gunpowder, sand, gunpowder));
            h.assertTrue(tnt.is(StrongExplosives.TNT) && tnt.getCount() == 1, "5 Strong Gunpowder + 4 sand -> Strong TNT: " + tnt);
            h.succeed();
        });
        add.accept("strong_tnt_power", h -> {
            var snapshot = MobsConfig.toJson();
            try {
                BlockPos pos = h.absolutePos(new BlockPos(2, 2, 2));
                PrimedTnt vanilla = new PrimedTnt(h.getLevel(), pos.getX() + 0.5, pos.getY(), pos.getZ() + 0.5, null);
                h.assertTrue(powerOf(vanilla) == 4.0f, "Vanilla TNT power " + powerOf(vanilla));
                vanilla.setFuse(1);
                h.getLevel().addFreshEntity(vanilla);
                vanilla.tick();
                h.assertTrue(powerOf(vanilla) == 4.0f, "a Vanilla TNT keeps power 4 when it explodes: " + powerOf(vanilla));

                PrimedTnt strong = StrongTntBlock.primed(h.getLevel(), pos.above(2), null);
                h.assertTrue(strong.getBlockState().is(StrongExplosives.TNT_BLOCK), "the primed TNT wears the Strong TNT block");
                strong.setFuse(1);
                h.getLevel().addFreshEntity(strong);
                strong.tick();
                h.assertTrue(powerOf(strong) == MobsConfig.strongTntPower && powerOf(strong) > 4.0f, "the Strong TNT explodes with " + powerOf(strong));

                MobsConfig.strongTntPower = 9.0f;
                PrimedTnt custom = StrongTntBlock.primed(h.getLevel(), pos.above(4), null);
                custom.setFuse(1);
                h.getLevel().addFreshEntity(custom);
                custom.tick();
                h.assertTrue(powerOf(custom) == 9.0f, "the configured power applies: " + powerOf(custom));

                var json = new com.google.gson.JsonObject();
                json.addProperty("strongTntPower", 99);
                MobsConfig.apply(json);
                h.assertTrue(MobsConfig.strongTntPower == 16.0f, "power capped at 16: " + MobsConfig.strongTntPower);
                json.addProperty("strongTntPower", 1);
                MobsConfig.apply(json);
                h.assertTrue(MobsConfig.strongTntPower == 4.0f, "power at least 4: " + MobsConfig.strongTntPower);
            } finally {
                MobsConfig.apply(snapshot);
            }
            h.succeed();
        });
        add.accept("strong_tnt_lighting", h -> {
            BlockPos at = new BlockPos(2, 2, 2);
            BlockPos pos = h.absolutePos(at);
            // Redstone: a Strong TNT placed next to power is lit at once and removed.
            h.setBlock(at.east(), Blocks.REDSTONE_BLOCK);
            h.setBlock(at, StrongExplosives.TNT_BLOCK);
            List<PrimedTnt> lit = h.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(pos).inflate(2.0));
            h.assertTrue(lit.size() == 1 && lit.get(0).getBlockState().is(StrongExplosives.TNT_BLOCK), "powered Strong TNT primed: " + lit.size());
            h.assertTrue(h.getLevel().getBlockState(pos).isAir(), "the block is gone once it is lit");
            lit.forEach(PrimedTnt::discard);
            // Flint and steel by a player.
            h.setBlock(at.east(), Blocks.AIR);
            h.setBlock(at, StrongExplosives.TNT_BLOCK);
            var player = h.makeMockServerPlayerInLevel();
            h.runBeforeTestEnd(() -> h.getLevel().getServer().getPlayerList().remove(player));
            h.assertTrue(StrongTntBlock.prime(h.getLevel(), pos, player, new ItemStack(Items.FLINT_AND_STEEL)), "the flint and steel way lights it");
            h.getLevel().getEntitiesOfClass(PrimedTnt.class, new AABB(pos).inflate(2.0)).forEach(PrimedTnt::discard);
            h.setBlock(at, Blocks.AIR);
            h.succeed();
        });
    }
}
