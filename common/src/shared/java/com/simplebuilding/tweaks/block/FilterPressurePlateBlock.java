package com.simplebuilding.tweaks.block;

import com.simplebuilding.tweaks.SimpleTweaks;
import com.simplebuilding.tweaks.block.entity.FilterPlateBlockEntity;
import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.component.DataComponents;
import net.minecraft.world.Container;
import net.minecraft.world.entity.LivingEntity;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.Items;
import net.minecraft.world.level.BlockGetter;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.PressurePlateBlock;
import net.minecraft.world.level.block.entity.BarrelBlockEntity;
import net.minecraft.world.level.block.entity.BlockEntity;
import net.minecraft.world.level.block.state.BlockBehaviour;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockSetType;
import org.jetbrains.annotations.Nullable;

/**
 * Netherit-Druckplatte (Simple Tweaks) und neue Enderit-Druckplatte.
 * <ul>
 *   <li>Netherit: ohne Fass darunter reagiert sie auf jeden Spieler; mit Fass nur auf Spieler, die
 *       einen der Gegenstaende aus dem Fass im Inventar tragen (Whitelist).</li>
 *   <li>Enderit (neu): zusaetzlich ein Spielerschloss - der Besitzer loest immer aus; ohne Fass sonst
 *       niemand; mit Fass ausserdem jeder, dessen Name auf einem umbenannten Namensschild im Fass
 *       steht, oder wer einen Whitelist-Gegenstand traegt.</li>
 * </ul>
 */
public class FilterPressurePlateBlock extends PressurePlateBlock implements EntityBlock {
    private final boolean enderite;

    public FilterPressurePlateBlock(BlockSetType type, BlockBehaviour.Properties properties, boolean enderite) {
        super(type, properties);
        this.enderite = enderite;
    }

    public boolean isEnderite() {
        return enderite;
    }

    @Override
    public @Nullable BlockEntity newBlockEntity(BlockPos pos, BlockState state) {
        return new FilterPlateBlockEntity(pos, state);
    }

    @Override
    public void setPlacedBy(Level level, BlockPos pos, BlockState state, @Nullable LivingEntity by, ItemStack itemStack) {
        super.setPlacedBy(level, pos, state, by, itemStack);
        PadOwnership.onPlaced(level, pos, by);
    }

    @Override
    protected float getDestroyProgress(BlockState state, Player player, BlockGetter level, BlockPos pos) {
        return PadOwnership.destroyProgress(player, level, pos, PadOwnership.OWNER_PLATE, PadOwnership.STRANGER_PLATE,
                super.getDestroyProgress(state, player, level, pos));
    }

    @Override
    protected int getSignalStrength(Level level, BlockPos pos) {
        if (!SimpleTweaks.config().pads.enableFilterPlates) {
            return 0;
        }
        List<Player> players = level.getEntitiesOfClass(Player.class, TOUCH_AABB.move(pos), p -> !p.isSpectator());
        if (players.isEmpty()) {
            return 0;
        }
        BlockEntity below = level.getBlockEntity(pos.below());
        Container barrel = below instanceof BarrelBlockEntity barrelEntity ? barrelEntity : null;
        FilterPlateBlockEntity plate = level.getBlockEntity(pos) instanceof FilterPlateBlockEntity be ? be : null;
        for (Player player : players) {
            if (admits(player, barrel, plate)) {
                return 15;
            }
        }
        return 0;
    }

    /** Darf dieser Spieler die Platte ausloesen? */
    public boolean admits(Player player, @Nullable Container barrel, @Nullable FilterPlateBlockEntity plate) {
        if (enderite && plate != null && plate.isOwner(player)) {
            return true;
        }
        if (barrel == null) {
            return !enderite;
        }
        return (enderite && nameTagAdmits(player, barrel)) || whitelistAdmits(player, barrel);
    }

    private boolean whitelistAdmits(Player player, Container barrel) {
        for (int i = 0; i < barrel.getContainerSize(); i++) {
            ItemStack wanted = barrel.getItem(i);
            if (wanted.isEmpty() || (enderite && isNamedNameTag(wanted))) {
                continue;
            }
            if (player.getInventory().contains(stack -> ItemStack.isSameItem(stack, wanted))) {
                return true;
            }
        }
        return false;
    }

    private static boolean nameTagAdmits(Player player, Container barrel) {
        String name = player.getName().getString();
        for (int i = 0; i < barrel.getContainerSize(); i++) {
            ItemStack stack = barrel.getItem(i);
            if (isNamedNameTag(stack) && stack.getHoverName().getString().equalsIgnoreCase(name)) {
                return true;
            }
        }
        return false;
    }

    private static boolean isNamedNameTag(ItemStack stack) {
        return stack.is(Items.NAME_TAG) && stack.has(DataComponents.CUSTOM_NAME);
    }
}
