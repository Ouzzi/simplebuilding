package com.simplelib.material;

import com.simplelib.SimpleLib;
import com.simplelib.compat.jei.MobDrop;
import com.simplelib.registry.LibMaterials;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.function.BooleanSupplier;
import net.minecraft.network.chat.Component;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.entity.EntityTypes;
import net.minecraft.world.entity.boss.wither.WitherBoss;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;

/**
 * WF25-10 (owner 25.10., docs/ai/PLAN-B-STRONG07-2026-11-07.md): the Vanilla Wither's Strong Upgrade drop, moved here
 * with the template from Simple Beacons. Only the Vanilla Wither ({@code minecraft:wither}, not the stronger Withers
 * of other mods) and only while a loaded mod requested the material group {@code strong_upgrade}. While a mod offers its
 * own way to the first template ({@link #way}, e.g. a recipe with its boss loot) the Wither drops one shard (nine make a
 * template, owner 21.10.); without one it drops the whole template instead - never both.
 */
public final class StrongUpgradeDrops {
    public static final String GROUP = "strong_upgrade";
    private static final List<BooleanSupplier> WAYS = new CopyOnWriteArrayList<>();

    private StrongUpgradeDrops() {}

    /** A mod with its own way to the first template announces it; {@code on} = that way is active now (mod switched on). */
    public static void way(BooleanSupplier on) {
        WAYS.add(on);
    }

    /** Whether a loaded, switched-on mod offers its own way to the first template. */
    public static boolean wayOn() {
        for (BooleanSupplier on : WAYS) {
            try {
                if (on.getAsBoolean()) return true;
            } catch (RuntimeException e) {
                SimpleLib.LOG.warn("Strong Upgrade way check failed", e);
            }
        }
        return false;
    }

    /** What the Vanilla Wither drops now: one shard while a way is on, else the template; empty while nobody requested the group. */
    public static ItemStack witherDrop() {
        if (!LibMaterials.requested(GROUP)) return ItemStack.EMPTY;
        return new ItemStack(LibMaterials.item(wayOn() ? "strong_upgrade_shard" : "strong_upgrade_template"));
    }

    /** Called by {@code WitherStrongUpgradeMixin} after the Wither's own death loot (the Nether Star). */
    public static void witherDied(WitherBoss wither, ServerLevel level) {
        if (wither.getType() != EntityTypes.WITHER) return;
        ItemStack drop = witherDrop();
        if (drop.isEmpty()) return;
        var entity = wither.spawnAtLocation(level, drop);
        if (entity != null) entity.setExtendedLifetime();
    }

    /** The recipe viewer's "Mob Drops" entry (AUDM24-3), the drop as it is now. */
    public static List<MobDrop> viewerDrops() {
        ItemStack drop = witherDrop();
        if (drop.isEmpty()) return List.of();
        String note = drop.is(LibMaterials.item("strong_upgrade_shard")) ? "jei.simplelib.note.wither_shard" : "jei.simplelib.note.wither_template";
        return List.of(new MobDrop(SimpleLib.MOD_ID + "/wither_strong_upgrade", EntityTypes.WITHER, new ItemStack(Items.NETHER_STAR),
                ItemStack.EMPTY, List.of(drop), List.of(Component.translatable(note))));
    }
}
