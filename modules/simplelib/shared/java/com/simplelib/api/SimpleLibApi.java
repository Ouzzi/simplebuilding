package com.simplelib.api;

import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import net.minecraft.core.BlockPos;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.state.BlockState;
import org.jetbrains.annotations.Nullable;

/**
 * The only classes a bundling mod may import from SimpleLib (principle 6a). SimpleLib never names
 * its users; a partner registers what it adds here. Everything is plain Vanilla types plus the
 * nested interfaces below, so a partner never touches SimpleLib's implementation classes.
 * The crucible API moved to {@code com.simplecrucibles.api.SimpleCruciblesApi} (split stage 5).
 */
public final class SimpleLibApi {
    private static volatile boolean axeWaysDisabled;

    /**
     * A tool way on a block of a Simple mod (e.g. Simple Hammers' sledgehammer upgrade, the axe way of the crucibles): when
     * it returns true, a right click no longer opens the block's menu but reaches the item's {@code useOn}.
     */
    @FunctionalInterface
    public interface ToolUse {
        boolean wants(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand);
    }

    private static final List<ToolUse> TOOL_USES = new CopyOnWriteArrayList<>();

    public static void registerToolUse(ToolUse use) {
        TOOL_USES.add(use);
    }

    /** Whether a registered tool way takes this click instead of the block's menu. */
    public static boolean toolWants(BlockState state, Level level, BlockPos pos, Player player, InteractionHand hand) {
        for (ToolUse use : TOOL_USES) {
            if (use.wants(state, level, pos, player, hand)) return true;
        }
        return false;
    }

    /**
     * Principle 5a: a partner with a working mod way (Simple Hammers' sledgehammer) switches the
     * Vanilla axe ways off.
     */
    public static void disableAxeWays() {
        axeWaysDisabled = true;
    }

    public static boolean axeWaysEnabled() {
        return !axeWaysDisabled;
    }

    /** Whether {@code stack} is a hammer of any mod ({@code #simplelib:hammers}, filled by the hammer mod). */
    public static boolean isHammer(ItemStack stack) {
        return stack.is(InWorldUpgrades.HAMMERS);
    }

    /** Whether {@code stack} runs in-world upgrades: {@code #simplelib:upgrade_tools}, or an axe while the axe ways are on. */
    public static boolean isUpgradeTool(ItemStack stack) {
        return InWorldUpgrades.isUpgradeTool(stack);
    }

    /**
     * A partner's way to move a block entity's contents when an in-world upgrade turns a block into {@code targets}
     * (see {@link InWorldUpgrades#replace}).
     */
    public static void registerUpgradeHandler(java.util.function.Predicate<Block> targets, InWorldUpgrades.Handler handler) {
        InWorldUpgrades.registerHandler(targets, handler);
    }

    /** As above, for the blocks of a block tag. */
    public static void registerUpgradeHandler(net.minecraft.tags.TagKey<Block> targets, InWorldUpgrades.Handler handler) {
        InWorldUpgrades.registerHandler(targets, handler);
    }

    public static int warmingTicks() {
        return com.simplelib.config.LibConfig.warmBaseTicks;
    }

    public static boolean warmable(ItemStack input) {
        return com.simplelib.warm.Warm.warmable(input);
    }

    // ------------------------------------------------------------ shared materials (split stage 3d)

    /**
     * Requests a shared material group: {@code "cracked_diamond"} (diamond pebble, cracked diamond and its block, the
     * anvil crushing), {@code "netherite_nugget"}, {@code "enderite"} (raw enderite, layered raw enderite, scrap, ingot,
     * nugget, block), {@code "upgrade_templates"} (basic and enderite upgrade). The items are always registered; their
     * recipes (condition {@code simplelib:material_requested}) and the anvil crushing only exist once a loaded mod
     * requested the group. Call it in the mod constructor / initializer, before data packs load.
     */
    public static void requestMaterial(String group) {
        com.simplelib.registry.LibMaterials.request(group);
    }

    public static boolean materialRequested(String group) {
        return com.simplelib.registry.LibMaterials.requested(group);
    }

    /**
     * A partner feeds the material loot (Enderite in End city treasure) itself, with its own switches; SimpleLib's
     * own pools ({@code material/MaterialLoot}, added only for requested materials) stay out. Call in the constructor.
     */
    public static void materialLootByPartner() {
        com.simplelib.material.MaterialLoot.byPartner();
    }

    /**
     * The warped base potion (water bottle + warped wart, owner N71): the base the Simple mods brew their own potions from.
     * Null before the potion registry is filled. Brewing recipes name it as {@code simplelib:warped}.
     */
    public static net.minecraft.core.Holder<net.minecraft.world.item.alchemy.Potion> warpedPotion() {
        return com.simplelib.registry.LibPotions.WARPED;
    }

    /** The warped wart item (plantable on soul sand, brewing ingredient of the warped base potion). */
    public static net.minecraft.world.item.Item warpedWart() {
        return com.simplelib.registry.LibItems.WARPED_WART;
    }

    /** A shared material item by path ({@code "enderite_ingot"}); registers the materials first if needed (any load order). */
    public static net.minecraft.world.item.Item materialItem(String id) {
        return com.simplelib.registry.LibMaterials.item(id);
    }

    /** A shared material block by path ({@code "enderite_block"}, {@code "cracked_diamond_block"}). */
    public static Block materialBlock(String id) {
        return com.simplelib.registry.LibMaterials.block(id);
    }

    /** Whether automation at {@code source} may change {@code target} (claims). */
    @FunctionalInterface
    public interface AutomationCheck {
        boolean allows(ServerLevel level, BlockPos source, BlockPos target);
    }

    /** Who may let a falling anvil crush a diamond block (claims); null = default, framework {@code Protection}. */
    public static void setAutomationCheck(@Nullable AutomationCheck check) {
        com.simplelib.material.AnvilCrushing.setAutomationCheck(check);
    }

    /** Pebbles one crushed diamond block drops (wiki, JEI). */
    public static int anvilPebbles() {
        return com.simplelib.material.AnvilCrushing.PEBBLES;
    }

    private SimpleLibApi() {}
}
