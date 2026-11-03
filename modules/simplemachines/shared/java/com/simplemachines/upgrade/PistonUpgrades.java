package com.simplemachines.upgrade;

import com.simplebuilding.framework.api.Protection;
import com.simplemachines.SimpleMachines;
import com.simplemachines.item.MachinePartItem;
import java.util.List;
import java.util.function.Function;
import net.minecraft.commands.Commands;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.Registry;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.core.registries.Registries;
import net.minecraft.resources.Identifier;
import net.minecraft.resources.ResourceKey;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundEvent;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.Rarity;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockBehaviour;

/**
 * The piston upgrades of the Simple Machines tab (MACH32-MOD stage 2, docs/ai/PLAN-B-MACHINES02-2026-11-03.md): Reinforced and
 * Reinforced Sticky Piston (push limit 18, one paid unbreakable), Netherite and Enderite Piston (breakers with durability) and the
 * three heads. Until 03.11. SimpleBuilding's ({@code simplebuilding:*}, the ids move, {@link SimpleMachines#registerIdMoves}); numbers
 * and properties unchanged. The upgrade materials are found by id: {@code simplelib:netherite_nugget} (SimpleLib, required) and
 * {@code simpleores:enderite_nugget} (Simple Ores, optional: without it nothing repairs or makes the Enderite Piston).
 */
public final class PistonUpgrades {
    /** The SimpleLib GadgetActions event of a repaired breaker (SimpleBuilding's advancement "Good as New" listens). */
    public static final String REPAIR_EVENT = "piston_repair";
    /** Vanilla's 12 for the Reinforced Pistons ({@code PistonHandlerMixin}). */
    public static final int REINFORCED_PUSH_LIMIT = 18;
    public static final Identifier NETHERITE_NUGGET_ID = Identifier.fromNamespaceAndPath("simplelib", "netherite_nugget");
    public static final Identifier ENDERITE_NUGGET_ID = Identifier.fromNamespaceAndPath("simpleores", "enderite_nugget");
    /** Block paths (items: the four pistons). */
    public static final List<String> PISTONS = List.of("reinforced_piston", "reinforced_sticky_piston", "netherite_piston", "enderite_piston");
    public static final List<String> HEADS = List.of("reinforced_piston_head", "netherite_piston_head", "enderite_piston_head");

    public static Block REINFORCED_PISTON;
    public static Block REINFORCED_STICKY_PISTON;
    public static Block NETHERITE_PISTON;
    public static Block ENDERITE_PISTON;
    public static Block REINFORCED_PISTON_HEAD;
    public static Block NETHERITE_PISTON_HEAD;
    public static Block ENDERITE_PISTON_HEAD;
    public static Item REINFORCED_PISTON_ITEM;
    public static Item REINFORCED_STICKY_PISTON_ITEM;
    public static Item NETHERITE_PISTON_ITEM;
    public static Item ENDERITE_PISTON_ITEM;
    public static SoundEvent BORE_SOUND;

    private static boolean lateHooks;

    private PistonUpgrades() {}

    /**
     * Blocks and the bore sound. Both Reinforced Pistons start from glass like SimpleBuilding's did (Vanilla piston properties
     * would make a retracted mod piston immovable); the heads copy Vanilla's head (immovable, no loot) with the strength and sound
     * of their tier.
     */
    public static void registerBlocks() {
        Identifier sound = SimpleMachines.id("block.piston.bore");
        BORE_SOUND = Registry.register(BuiltInRegistries.SOUND_EVENT, sound, SoundEvent.createVariableRangeEvent(sound));
        REINFORCED_PISTON = block("reinforced_piston", Blocks.GLASS, p -> new ReinforcedPistonBlock(false, p.strength(1.5F).sound(SoundType.METAL)));
        REINFORCED_STICKY_PISTON = block("reinforced_sticky_piston", Blocks.GLASS, p -> new ReinforcedPistonBlock(true, p.strength(1.5F).sound(SoundType.METAL)));
        NETHERITE_PISTON = block("netherite_piston", Blocks.GLASS, p -> new NetheriteBreakerPistonBlock(p.strength(5.0F, 1200.0F).sound(SoundType.NETHERITE_BLOCK)));
        ENDERITE_PISTON = block("enderite_piston", Blocks.GLASS, p -> new EnderitePistonBlock(p.strength(6.0F, 1500.0F).sound(SoundType.NETHERITE_BLOCK)));
        REINFORCED_PISTON_HEAD = block("reinforced_piston_head", Blocks.PISTON_HEAD, p -> new ModPistonHeadBlock(ModPistonHeadBlock.Tier.REINFORCED, p.strength(1.5F).noLootTable().sound(SoundType.METAL)));
        NETHERITE_PISTON_HEAD = block("netherite_piston_head", Blocks.PISTON_HEAD, p -> new ModPistonHeadBlock(ModPistonHeadBlock.Tier.NETHERITE, p.strength(5.0F, 1200.0F).noLootTable().sound(SoundType.NETHERITE_BLOCK)));
        ENDERITE_PISTON_HEAD = block("enderite_piston_head", Blocks.PISTON_HEAD, p -> new ModPistonHeadBlock(ModPistonHeadBlock.Tier.ENDERITE, p.strength(6.0F, 1500.0F).noLootTable().sound(SoundType.NETHERITE_BLOCK)));
    }

    public static void registerItems() {
        REINFORCED_PISTON_ITEM = item("reinforced_piston", p -> new MachinePartItem(REINFORCED_PISTON, "reinforced_piston", p));
        REINFORCED_STICKY_PISTON_ITEM = item("reinforced_sticky_piston", p -> new MachinePartItem(REINFORCED_STICKY_PISTON, "reinforced_piston", p));
        NETHERITE_PISTON_ITEM = item("netherite_piston", p -> new NetheritePistonItem(NETHERITE_PISTON, p.fireResistant().rarity(Rarity.UNCOMMON)));
        ENDERITE_PISTON_ITEM = item("enderite_piston", p -> new NetheritePistonItem(ENDERITE_PISTON, p.fireResistant().rarity(Rarity.EPIC),
                "tooltip.simplemachines.enderite_piston"));
        // A hammer upgrade (Reinforced -> Netherite -> Enderite) gives a fresh breaker (SimpleLib in-world upgrades).
        com.simplelib.api.HammerActions.registerUpgradedState(state -> NetheriteBreakerPistonBlock.withDamage(state, 0));
    }

    /** The four pistons in tab order. */
    public static List<ItemStack> stacks() {
        return List.of(new ItemStack(REINFORCED_PISTON_ITEM), new ItemStack(REINFORCED_STICKY_PISTON_ITEM),
                new ItemStack(NETHERITE_PISTON_ITEM), new ItemStack(ENDERITE_PISTON_ITEM));
    }

    /**
     * Hooks that need other mods' items (registered in any order): the hand hint of both nuggets on a damaged breaker (SimpleLib
     * {@code GadgetActions}, asked by SimpleBuilding's transform hint). Called when a server starts and when the client starts;
     * runs once.
     */
    public static synchronized void registerLateHooks() {
        if (lateHooks) return;
        lateHooks = true;
        for (Item nugget : List.of(netheriteNugget(), enderiteNugget())) {
            if (nugget == Items.AIR) continue;
            com.simplelib.api.GadgetActions.registerItemHint(nugget, context -> NetheriteBreakerPistonBlock.canRepairWith(
                    context.getLevel().getBlockState(context.getClickedPos()), context.getItemInHand()));
        }
    }

    /** {@code simplelib:netherite_nugget}, or air without SimpleLib's material (never matches a stack in hand). */
    public static Item netheriteNugget() {
        return BuiltInRegistries.ITEM.getValue(NETHERITE_NUGGET_ID);
    }

    /** {@code simpleores:enderite_nugget}, or air without Simple Ores. */
    public static Item enderiteNugget() {
        return BuiltInRegistries.ITEM.getValue(ENDERITE_NUGGET_ID);
    }

    /**
     * Whether {@code player} may repair the piston at {@code pos} with {@code stack}: what a block placement asks (spectator,
     * adventure mode, spawn protection), framework {@link Protection} (claims) and the partner permissions of SimpleLib
     * {@code HammerActions} (SimpleBuilding's WorldPermissions).
     */
    public static boolean mayRepair(Level level, Player player, BlockPos pos, Direction face, ItemStack stack) {
        if (player.isSpectator() || !player.mayBuild() || !level.mayInteract(player, pos) || !player.mayUseItemAt(pos, face, stack)) {
            return false;
        }
        if (level instanceof ServerLevel server && player instanceof ServerPlayer serverPlayer && Protection.active(server.getServer())) {
            boolean admin = Commands.hasPermission(Commands.LEVEL_OWNERS).test(serverPlayer.createCommandSourceStack());
            var target = new Protection.Target(serverPlayer.getUUID(), server.dimension().identifier().toString(), pos.getX(), pos.getY(), pos.getZ(), admin);
            if (!Protection.allows(server.getServer(), target, Protection.Action.USE)) return false;
        }
        return com.simplelib.api.HammerActions.mayChange(level, player, pos);
    }

    private static Block block(String path, Block base, Function<BlockBehaviour.Properties, Block> factory) {
        ResourceKey<Block> key = ResourceKey.create(Registries.BLOCK, SimpleMachines.id(path));
        return Registry.register(BuiltInRegistries.BLOCK, key, factory.apply(BlockBehaviour.Properties.ofFullCopy(base).setId(key)));
    }

    private static Item item(String path, Function<Item.Properties, Item> factory) {
        ResourceKey<Item> key = ResourceKey.create(Registries.ITEM, SimpleMachines.id(path));
        return Registry.register(BuiltInRegistries.ITEM, key, factory.apply(new Item.Properties().setId(key)));
    }
}
