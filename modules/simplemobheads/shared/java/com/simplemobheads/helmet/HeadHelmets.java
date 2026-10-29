package com.simplemobheads.helmet;

import com.simplemobheads.MobHeadType;
import com.simplemobheads.MobHeads;
import com.simplemobheads.SimpleMobHeads;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponentMap;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.component.PatchedDataComponentMap;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.resources.Identifier;
import net.minecraft.sounds.SoundEvents;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.Containers;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.item.equipment.Equippable;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SkullBlock;
import net.minecraft.world.level.block.entity.SkullBlockEntity;
import net.minecraft.world.level.block.state.BlockState;
import org.jspecify.annotations.Nullable;

/**
 * Helmets on placed heads (BS28b-1, owner 28.10.): right-click a head that has a human-sized face (the Vanilla skeleton, wither
 * skeleton, zombie and player heads, and the heads of {@code mobs.json} with a {@code helmet} size) with a helmet and it sits on
 * the head; an empty hand takes it off again, breaking the head drops it. The helmet is kept in the block entity components
 * ({@link MobHeads#HELMET}), the client draws it with the helmet's own armor model ({@code HeadHelmetRender}).
 *
 * <p>Wither skeleton skulls (BS28b-2, armored Wither): with Simple Mobs installed they only take the helmets up to Diamond
 * - Netherite and Enderite Helmets belong to the middle skull of the Wither figure, whose crown Simple Mobs puts on - and Simple
 * Mobs reads the helmets of the two outer skulls through the component {@code simplemobheads:helmet} (by registry name, no import).
 */
public final class HeadHelmets {
    private static final Identifier SUMMONER = Identifier.fromNamespaceAndPath("simplemobs", "blighted_wither");

    private HeadHelmets() {
    }

    /** A real helmet: it goes into the head slot and has an armor model to draw. */
    public static boolean isHelmet(ItemStack stack) {
        Equippable equippable = stack.get(DataComponents.EQUIPPABLE);
        return !stack.isEmpty() && equippable != null && equippable.slot() == EquipmentSlot.HEAD && equippable.assetId().isPresent();
    }

    /** Size of the helmet the head at this state wears against an 8 px head, 0 when the head wears none. */
    public static float size(BlockState state) {
        if (!(state.getBlock() instanceof AbstractSkullBlock skull)) {
            return 0.0F;
        }
        SkullBlock.Type type = skull.getType();
        if (type instanceof MobHeadType head) {
            return head.head().helmet();
        }
        return type == SkullBlock.Types.SKELETON || type == SkullBlock.Types.WITHER_SKELETON || type == SkullBlock.Types.ZOMBIE
                || type == SkullBlock.Types.PLAYER ? 1.0F : 0.0F;
    }

    public static boolean isWitherSkull(BlockState state) {
        return state.is(Blocks.WITHER_SKELETON_SKULL) || state.is(Blocks.WITHER_SKELETON_WALL_SKULL);
    }

    /** The helmets a wither skull may wear besides the Wither crown: leather, copper, chainmail, iron, gold, diamond. */
    public static boolean outerHelmet(ItemStack stack) {
        Item item = stack.getItem();
        return item == Items.LEATHER_HELMET || item == Items.COPPER_HELMET || item == Items.CHAINMAIL_HELMET
                || item == Items.IRON_HELMET || item == Items.GOLDEN_HELMET || item == Items.DIAMOND_HELMET;
    }

    /** Whether Simple Mobs (the Wither crown of the middle skull) is there. */
    public static boolean crownsAvailable() {
        return BuiltInRegistries.ENTITY_TYPE.containsKey(SUMMONER);
    }

    /** Whether this head takes this helmet now (size, helmet, the Wither skull rule; the one helmet per head is {@link #put}'s). */
    public static boolean accepts(BlockState state, ItemStack stack) {
        if (size(state) <= 0.0F || !isHelmet(stack)) {
            return false;
        }
        return !(isWitherSkull(state) && crownsAvailable() && !outerHelmet(stack));
    }

    public static @Nullable ItemStack get(Level level, BlockPos pos) {
        return level.getBlockEntity(pos) instanceof SkullBlockEntity skull ? skull.components().get(MobHeads.HELMET) : null;
    }

    /** Puts one helmet of {@code stack} on the head at {@code pos} (server). False when the head has one or does not take it. */
    public static boolean put(Level level, BlockPos pos, Player player, ItemStack stack) {
        BlockState state = level.getBlockState(pos);
        if (!SimpleMobHeads.on() || !accepts(state, stack) || get(level, pos) != null
                || !(level.getBlockEntity(pos) instanceof SkullBlockEntity skull)) {
            return false;
        }
        if (!level.isClientSide()) {
            ItemStack helmet = stack.copyWithCount(1);
            set(level, pos, state, skull, helmet);
            stack.consume(1, player);
            level.playSound(null, pos, equipSound(helmet), SoundSource.BLOCKS, 1.0F, 1.0F);
        }
        return true;
    }

    /** Takes the helmet off the head at {@code pos} into the player's hands/inventory (server). False when there is none. */
    public static boolean take(Level level, BlockPos pos, Player player) {
        ItemStack helmet = get(level, pos);
        if (helmet == null || !(level.getBlockEntity(pos) instanceof SkullBlockEntity skull)) {
            return false;
        }
        if (!level.isClientSide()) {
            set(level, pos, level.getBlockState(pos), skull, null);
            if (!player.getInventory().add(helmet.copy())) {
                Containers.dropItemStack(level, player.getX(), player.getY(), player.getZ(), helmet.copy());
            }
            level.playSound(null, pos, equipSound(helmet), SoundSource.BLOCKS, 1.0F, 0.8F);
        }
        return true;
    }

    /** A head that goes (broken, replaced) drops its helmet. */
    public static void dropFrom(SkullBlockEntity skull, BlockPos pos) {
        ItemStack helmet = skull.components().get(MobHeads.HELMET);
        Level level = skull.getLevel();
        if (helmet != null && level != null && !level.isClientSide()) {
            Containers.dropItemStack(level, pos.getX() + 0.5, pos.getY() + 0.5, pos.getZ() + 0.5, helmet.copy());
        }
    }

    /** Writes (or, with null, removes) the helmet in the components and sends the head to the clients. */
    private static void set(Level level, BlockPos pos, BlockState state, SkullBlockEntity skull, @Nullable ItemStack helmet) {
        // collectComponents includes the profile, note block sound and name: applyComponents would reset them otherwise
        PatchedDataComponentMap map = new PatchedDataComponentMap(DataComponentMap.EMPTY);
        map.setAll(skull.collectComponents());
        if (helmet == null) {
            map.remove(MobHeads.HELMET);
        } else {
            map.set(MobHeads.HELMET, helmet);
        }
        skull.applyComponents(DataComponentMap.EMPTY, map.asPatch());
        skull.setChanged();
        level.sendBlockUpdated(pos, state, state, Block.UPDATE_ALL);
    }

    private static net.minecraft.sounds.SoundEvent equipSound(ItemStack helmet) {
        Equippable equippable = helmet.get(DataComponents.EQUIPPABLE);
        return (equippable != null ? equippable.equipSound() : SoundEvents.ARMOR_EQUIP_GENERIC).value();
    }
}
