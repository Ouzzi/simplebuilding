package com.simplebuilding.modules.simplemobs;

import java.util.function.BiConsumer;
import java.util.function.Consumer;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.projectile.FireworkRocketEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.crafting.Recipe;
import net.minecraft.world.item.crafting.SmithingRecipeInput;
import net.minecraft.world.phys.Vec3;

/** Wither/Nether Wings GameTests (EL24-1, Fabric wrappers in ModuleGameTest). Standalone-safe: the recipe test follows Simple Beacons. */
final class WingsTests {
    private WingsTests() {}

    private static ItemStack smith(GameTestHelper h, String recipe, ItemStack template, ItemStack base, ItemStack addition) {
        var holder = h.getLevel().getServer().getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(MobsRegistry.ID, recipe)))
                .orElseThrow(() -> new IllegalStateException("missing recipe " + recipe));
        @SuppressWarnings("unchecked") var r = (Recipe<SmithingRecipeInput>) holder.value();
        var input = new SmithingRecipeInput(template, base, addition);
        h.assertTrue(r.matches(input, h.getLevel()), "recipe matches " + recipe);
        return r.assemble(input);
    }

    private static boolean recipe(GameTestHelper h, String id) {
        return h.getLevel().getServer().getRecipeManager()
                .byKey(ResourceKey.create(Registries.RECIPE, Identifier.fromNamespaceAndPath(MobsRegistry.ID, id))).isPresent();
    }

    /** Speed a rocket holds along the look direction: Vanilla push (+ the wings' extra) and the glide drag 0.99, until it settles. */
    static double steadySpeed(double factor) {
        Vec3 look = new Vec3(0, 0, 1), v = Vec3.ZERO;
        for (int i = 0; i < 400; i++) {
            Vec3 vanilla = v.add(look.x * 0.1 + (look.x * 1.5 - v.x) * 0.5, look.y * 0.1 + (look.y * 1.5 - v.y) * 0.5,
                    look.z * 0.1 + (look.z * 1.5 - v.z) * 0.5);
            v = WitherWings.boost(vanilla, look, factor).scale(0.99);
        }
        return v.length();
    }

    static void addAll(BiConsumer<String, Consumer<GameTestHelper>> add) {
        add.accept("wings_items", h -> {
            for (var item : java.util.List.of(WitherWings.WITHER, WitherWings.NETHER, WitherWings.END)) {
                ItemStack s = new ItemStack(item);
                h.assertTrue(s.has(DataComponents.GLIDER), "glides: " + item);
                var eq = s.get(DataComponents.EQUIPPABLE);
                h.assertTrue(eq != null && eq.slot() == EquipmentSlot.CHEST && eq.assetId().isPresent()
                        && eq.assetId().get().identifier().getNamespace().equals(MobsRegistry.ID), "chest, own wings look: " + item);
                h.assertTrue(s.isValidRepairItem(new ItemStack(Items.PHANTOM_MEMBRANE)) && !s.isValidRepairItem(new ItemStack(Items.LEATHER)),
                        "repaired with phantom membrane like the Elytra: " + item);
                h.assertTrue(s.has(DataComponents.DAMAGE_RESISTANT), "fire-proof like Netherite: " + item);
                h.assertTrue(com.simplelib.api.SimpleRarity.of(s) == com.simplelib.api.SimpleRarity.CURSED, "cursed like heart and gem: " + item);
                h.assertTrue(BuiltInRegistries.ITEM.getKey(item).getNamespace().equals(MobsRegistry.ID), "registered: " + item);
            }
            h.assertTrue(new ItemStack(Items.ELYTRA).getMaxDamage() == 432, "Vanilla Elytra 432");
            h.assertTrue(new ItemStack(WitherWings.WITHER).getMaxDamage() == 576 && new ItemStack(WitherWings.NETHER).getMaxDamage() == 768,
                    "durability 576 / 768 (Elytra 432, +1/3 per stage)");
            h.assertTrue(new ItemStack(WitherWings.END).getMaxDamage() == 960 && WitherWings.configFactor(new ItemStack(WitherWings.END)) == 6.0f,
                    "End Wings (stage 3): 960 durability, x6");
            h.assertTrue(!recipe(h, "end_wings_smithing") && !recipe(h, "end_wings_smithing_without_simplebeacons"), "End Wings are not craftable yet");
            h.succeed();
        });
        add.accept("wings_recipes", h -> {
            boolean beacons = BuiltInRegistries.ITEM.containsKey(Identifier.fromNamespaceAndPath("simplebeacons", "strong_upgrade_template"));
            h.assertTrue(recipe(h, "wither_wings_smithing") == beacons && recipe(h, "nether_wings_smithing") == beacons,
                    "Strong Upgrade recipes only with Simple Beacons (loaded: " + beacons + ")");
            h.assertTrue(recipe(h, "wither_wings_smithing_without_simplebeacons") == !beacons
                    && recipe(h, "nether_wings_smithing_without_simplebeacons") == !beacons, "Netherite Upgrade way only without Simple Beacons");
            String suffix = beacons ? "_smithing" : "_smithing_without_simplebeacons";
            ItemStack template = beacons ? new ItemStack(BuiltInRegistries.ITEM.getValue(Identifier.fromNamespaceAndPath("simplebeacons", "strong_upgrade_template")))
                    : new ItemStack(Items.NETHERITE_UPGRADE_SMITHING_TEMPLATE);
            ItemStack elytra = new ItemStack(Items.ELYTRA);
            elytra.setDamageValue(17);
            ItemStack wither = smith(h, "wither_wings" + suffix, template, elytra, new ItemStack(WitherMobs.HEART));
            h.assertTrue(wither.is(WitherWings.WITHER) && wither.getDamageValue() == 17, "Elytra + Nether Heart -> Wither Wings, wear kept: " + wither);
            ItemStack nether = smith(h, "nether_wings" + suffix, template, wither, new ItemStack(WitherMobs.GEM));
            h.assertTrue(nether.is(WitherWings.NETHER), "Wither Wings + Red Nether Gem -> Nether Wings");
            var wrong = new SmithingRecipeInput(template, new ItemStack(Items.ELYTRA), new ItemStack(WitherMobs.GEM));
            h.assertTrue(h.getLevel().getServer().getRecipeManager().getRecipeFor(net.minecraft.world.item.crafting.RecipeType.SMITHING, wrong, h.getLevel())
                    .map(r -> !r.value().assemble(wrong).is(WitherWings.NETHER)).orElse(true), "the gem does not skip stage 1");
            h.succeed();
        });
        add.accept("wings_speed_cap", h -> {
            double vanilla = steadySpeed(1), wither = steadySpeed(2), nether = steadySpeed(4);
            h.assertTrue(Math.abs(vanilla - 1.666) < 0.01, "Vanilla rocket top speed ~1.67 blocks/tick: " + vanilla);
            h.assertTrue(Math.abs(wither / vanilla - 2) < 1.0E-3 && Math.abs(nether / vanilla - 4) < 1.0E-3,
                    "top speed x2 / x4: " + wither + " / " + nether);
            Vec3 v = new Vec3(0.3, -0.2, 0.9), look = new Vec3(0, 0, 1);
            h.assertTrue(WitherWings.boost(v, look, 1.0) == v, "factor 1 leaves Vanilla untouched");
            h.succeed();
        });
        add.accept("wings_rocket_tick", h -> {
            float oldWither = MobsConfig.witherWingsSpeedFactor;
            try {
                double[] got = new double[3];
                ItemStack[] chest = {new ItemStack(Items.ELYTRA), new ItemStack(WitherWings.WITHER), new ItemStack(WitherWings.NETHER)};
                for (int i = 0; i < 3; i++) {
                    var player = h.makeMockServerPlayerInLevel();
                    player.setItemSlot(EquipmentSlot.CHEST, chest[i]);
                    player.setXRot(0f);
                    player.setYRot(0f);
                    player.startFallFlying();
                    player.setDeltaMovement(Vec3.ZERO);
                    h.assertTrue(player.isFallFlying(), "gliding");
                    var rocket = new FireworkRocketEntity(h.getLevel(), new ItemStack(Items.FIREWORK_ROCKET), player);
                    rocket.tick();
                    got[i] = player.getDeltaMovement().length();
                    rocket.discard();
                    player.discard();
                }
                h.assertTrue(Math.abs(got[0] - WitherWings.VANILLA_PUSH) < 1.0E-6, "Vanilla first push 0.85: " + got[0]);
                h.assertTrue(Math.abs(got[1] - 2 * got[0]) < 1.0E-6 && Math.abs(got[2] - 4 * got[0]) < 1.0E-6,
                        "pushed towards x2 / x4: " + got[1] + " / " + got[2]);
                h.assertTrue(WitherWings.factor(chest[1]) == 2.0f, "server stamped its factor into the worn wings");
                // the server's config wins over the client's defaults: a changed factor is stamped on the next push
                MobsConfig.witherWingsSpeedFactor = 3.0f;
                WitherWings.stamp(chest[1]);
                h.assertTrue(chest[1].get(DataComponents.CUSTOM_DATA).copyTag().getFloatOr(WitherWings.STAMP, 0f) == 3.0f
                        && WitherWings.factor(chest[1]) == 3.0f, "stamp follows the server config");
                MobsConfig.witherWingsSpeedFactor = 50f;
                h.assertTrue(WitherWings.configFactor(chest[1]) == WitherWings.MAX_FACTOR, "capped at x8");
                h.assertTrue(WitherWings.factor(new ItemStack(Items.ELYTRA)) == 1.0f, "Vanilla Elytra stays x1");
            } finally {
                MobsConfig.witherWingsSpeedFactor = oldWither;
            }
            h.succeed();
        });
    }
}
