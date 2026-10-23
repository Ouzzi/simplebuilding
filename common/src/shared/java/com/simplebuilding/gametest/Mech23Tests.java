package com.simplebuilding.gametest;

import com.simplebuilding.items.ModItems;
import com.simplebuilding.items.custom.ThrownChipItem;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.gametest.framework.GameTestHelper;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.animal.pig.Pig;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.phys.AABB;

/** Owner requests B23 (23.10., docs/ai/PLAN-A-MECH23A-2026-10-23.md): throwable shards (M4), rarities (M5). */
public final class Mech23Tests {
    private Mech23Tests() {
    }

    /**
     * B23-M4: every shard throws like a snowball on a right click into the air (one used up, a flying chip carrying the
     * item), and a chip that hits a pig takes half a heart.
     */
    public static void chipsFlyAndHurtALittle(GameTestHelper helper) {
        if (ModItems.STONE_PEBBLE == null) {
            helper.succeed();
            return;
        }
        ServerPlayer player = helper.makeMockServerPlayerInLevel();
        List<Item> chips = new java.util.ArrayList<>(List.of(ModItems.STONE_PEBBLE, ModItems.FLINT_CHIP, ModItems.FIRE_CHIP, ModItems.ICE_CHIP,
                ModItems.OBSIDIAN_CHIP, ModItems.CRYING_OBSIDIAN_CHIP));
        if (ModItems.BLAZING_OBSIDIAN_CHIP != null) chips.add(ModItems.BLAZING_OBSIDIAN_CHIP);
        for (Item chip : chips) {
            helper.assertTrue(chip instanceof ThrownChipItem, chip + " is not throwable");
            ItemStack stack = new ItemStack(chip, 3);
            player.setItemInHand(InteractionHand.MAIN_HAND, stack);
            var result = chip.use(helper.getLevel(), player, InteractionHand.MAIN_HAND);
            helper.assertTrue(result.consumesAction() && player.getMainHandItem().getCount() == 2, chip + ": throw used " + result);
        }
        AABB around = player.getBoundingBox().inflate(4.0);
        var thrown = helper.getLevel().getEntitiesOfClass(ThrownChipItem.Chip.class, around);
        helper.assertTrue(thrown.size() == chips.size(), "one flying chip per throw: " + thrown.size());
        thrown.forEach(net.minecraft.world.entity.Entity::discard);
        // a chip dropped onto a pig: 1 damage
        BlockPos at = new BlockPos(2, 1, 2);
        Pig pig = helper.spawnWithNoFreeWill(EntityTypes.PIG, at);
        float before = pig.getHealth();
        var abs = helper.absolutePos(at);
        ThrownChipItem.Chip chip = new ThrownChipItem.Chip(helper.getLevel(), player, new ItemStack(ModItems.FLINT_CHIP));
        chip.setPos(abs.getX() + 0.5, abs.getY() + 2.5, abs.getZ() + 0.5);
        chip.shoot(0.0, -1.0, 0.0, 1.0F, 0.0F);
        helper.getLevel().addFreshEntity(chip);
        helper.succeedWhen(() -> {
            helper.assertTrue(Math.abs(pig.getHealth() - (before - ThrownChipItem.DAMAGE)) < 1.0E-4F, "pig health " + pig.getHealth() + " of " + before);
            helper.assertTrue(chip.isRemoved(), "the chip is gone after the hit");
        });
    }

    /** B23-M5: the building cores are at least as rare as a Nether star (Enderite: epic), the Warden tendril is rare. */
    public static void coresAndWardenTendrilAreRare(GameTestHelper helper) {
        Rarity star = new ItemStack(Items.NETHER_STAR).getRarity();
        for (Item core : List.of(ModItems.COPPER_CORE, ModItems.IRON_CORE, ModItems.GOLD_CORE, ModItems.DIAMOND_CORE,
                ModItems.NETHERITE_CORE, ModItems.ENDERITE_CORE)) {
            Rarity rarity = new ItemStack(core).getRarity();
            helper.assertTrue(rarity.ordinal() >= star.ordinal(), core + " is " + rarity + ", the Nether star " + star);
        }
        helper.assertTrue(new ItemStack(ModItems.ENDERITE_CORE).getRarity() == Rarity.EPIC, "enderite core stays epic");
        if (ModItems.WARDEN_TENDRIL != null) {
            helper.assertTrue(new ItemStack(ModItems.WARDEN_TENDRIL).getRarity() == Rarity.RARE, "warden tendril rare");
        }
        helper.succeed();
    }
}
