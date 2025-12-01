package com.simplebuilding.items.custom;

import com.simplebuilding.component.ModDataComponentTypes;
import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

public class BuildingWandItem extends Item {

    public static final int BUILDING_WAND_SQUARE_COPPER = 3;
    public static final int BUILDING_WAND_SQUARE_IRON = 5;
    public static final int BUILDING_WAND_SQUARE_GOLD = 7;
    public static final int BUILDING_WAND_SQUARE_DIAMOND = 7;
    public static final int BUILDING_WAND_SQUARE_NETHERITE = 9;

    public static final int DURABILITY_MULTIPLAYER_WAND = 8;

    private int wandSquareDiameter;
    private SoundEvent placeSound = SoundEvents.BLOCK_STONE_PLACE;

    public BuildingWandItem(Settings settings) {
        super(settings);
        this.wandSquareDiameter = 1;
    }

    public int getWandSquareDiameter() {
        return this.wandSquareDiameter;
    }

    public void setWandSquareDiameter(int wandSquareDiameter) {
        this.wandSquareDiameter = wandSquareDiameter;
    }

    // possible enchantments
        // surface place (not with bridge) -> changes the placement from plane to surface 
            // base block normal, 
            // surrounding blocks maximal depth diviation from base block 2, 
            // next ring maximum diviation from previous is 1
        // line place -> placement on ine axis 
            // deafault horizontal to player
            // when near the block edge front or back change direction to front/back
            // with surface place same logic but only the line instead a sqare
        // bridge (not with surface place) -> if placing a block place on the side of the edge of that block a line of blocks
            // it targeting the front edge, place on the front side, if right the right side and so on 
        // master builder -> if enchanted allows to "connect" with enchanted bundle. 
            // allows to take blocks from masterbuilder enchanted bundle or from hotbar
            // priority -> enchanted bundle, offhand, hotbar 1-9
        // range - like other range implementation
        // unbreaking - vanilla
        // mending - vanilla

    // how it should work:
        // first detect the direction (which orientation)
        // then determine how many blocks can be placed (flood fill)
            // is dependent from free blocks and how many blocks in offhand (hotbar and ench. bundle when masterbuilder)
        // visualize what can be placed
        // on place click place from middleblock with little delay from block to block
        // damage the ammount of uses not ammount of blocks placed

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        BlockPos clickedPos = context.getBlockPos();
        Direction clickedFace = context.getSide();
        ItemStack wandStack = context.getStack();

        if (world.isClient() || player == null) return ActionResult.PASS;

        BlockState stateToExtend = world.getBlockState(clickedPos);
        Block blockToExtend = stateToExtend.getBlock();

        int radius = (this.wandSquareDiameter - 1) / 2;
        List<BlockPos> placementPositions = getBuildingPositions(clickedPos, clickedFace, this.wandSquareDiameter);

        int blocksPlaced = 0;

        // Check for Master Builder Enchantment on Wand
        boolean hasMasterBuilder = hasEnchantment(wandStack, world, ModEnchantments.MASTER_BUILDER);

        for (BlockPos targetPos : placementPositions) {
            BlockState targetState = world.getBlockState(targetPos);
            if (!targetState.isReplaceable()) continue;

            // --- Ressourcen finden ---
            ItemStack stackToPlace = ItemStack.EMPTY;
            ItemStack sourceStack = ItemStack.EMPTY; // Der Stack, von dem wir abziehen (Inventar oder Bundle)
            boolean fromBundle = false;
            int bundleIndex = -1; // Für Bundle removal

            // 1. Offhand Check (Priorität)
            ItemStack offHandStack = player.getOffHandStack();

            // Logik:
            // A. Ist Offhand ein Block? -> Nimm diesen.
            // B. Ist Offhand ein Bundle UND MasterBuilder aktiv? -> Nimm aus Bundle.

            if (offHandStack.getItem() instanceof BlockItem blockItem) {
                // Direkter Block in Offhand
                if (blockItem.getBlock() == blockToExtend) { // Sollte es nur denselben Block erweitern?
                    // Ja, Building Wands erweitern meistens das, was man anklickt.
                    // Wenn man aber in der Offhand was anderes hat, könnte man das nehmen wollen.
                    // Konvention: Wenn Offhand Item != clicked Block -> Trotzdem nehmen?
                    // Lass uns sagen: Wir nehmen das Offhand Item, EGAL was man klickt.
                    stackToPlace = offHandStack;
                    sourceStack = offHandStack;
                    stateToExtend = blockItem.getBlock().getDefaultState(); // State anpassen!
                }
            }
            else if (hasMasterBuilder && offHandStack.getItem() instanceof ReinforcedBundleItem) {
                // Bundle in Offhand + Master Builder
                boolean hasColorPalette = hasEnchantment(offHandStack, world, ModEnchantments.COLOR_PALETTE);

                // Suche Block im Bundle
                BundleContentsComponent contents = offHandStack.get(DataComponentTypes.BUNDLE_CONTENTS);
                if (contents != null && !contents.isEmpty()) {
                    if (hasColorPalette) {
                        // Zufällig
                        int randomIdx = world.random.nextInt(contents.size());
                        ItemStack randomStack = contents.get(randomIdx);
                        if (randomStack.getItem() instanceof BlockItem bi) {
                            stackToPlace = randomStack;
                            sourceStack = offHandStack;
                            fromBundle = true;
                            bundleIndex = randomIdx;
                            stateToExtend = bi.getBlock().getDefaultState();
                        }
                    } else {
                        // Erster Block (oder Selected)
                        int idx = contents.getSelectedStackIndex();
                        if (idx == -1) idx = 0;

                        // Wir suchen den ERSTEN BlockItem Stack
                        // (Oder wir nehmen strikt den selektierten?)
                        // Lass uns den selektierten nehmen.
                        if (idx < contents.size()) {
                            ItemStack selectedStack = contents.get(idx);
                            if (selectedStack.getItem() instanceof BlockItem bi) {
                                stackToPlace = selectedStack;
                                sourceStack = offHandStack;
                                fromBundle = true;
                                bundleIndex = idx;
                                stateToExtend = bi.getBlock().getDefaultState();
                            }
                        }
                    }
                }
            }

            // 2. Fallback: Inventar (Nur wenn Offhand nichts geliefert hat)
            if (stackToPlace.isEmpty()) {
                int slot = findSlotWithBlock(player, blockToExtend);
                if (slot != -1) {
                    sourceStack = player.getInventory().getStack(slot);
                    stackToPlace = sourceStack;
                }
            }

            // Wenn immer noch leer -> Abbruch (oder Creative Mode)
            if (stackToPlace.isEmpty() && !player.getAbilities().creativeMode) {
                break; // Keine Items mehr
            }

            // --- Platzieren ---
            if (world.setBlockState(targetPos, stateToExtend, 3)) {
                world.playSound(null, targetPos, placeSound, SoundCategory.BLOCKS, 1.0f, 1.0f);

                // Verbrauch
                if (!player.getAbilities().creativeMode) {
                    if (fromBundle) {
                        // Kompliziert: Ein Item aus dem Bundle entfernen
                        removeOneFromBundle(sourceStack, bundleIndex);
                    } else {
                        sourceStack.decrement(1);
                    }
                    wandStack.damage(1, (ServerPlayerEntity) player, EquipmentSlot.MAINHAND);
                }
                blocksPlaced++;
            }
        }

        return blocksPlaced > 0 ? ActionResult.SUCCESS : ActionResult.PASS;
    }

    // --- Helper für Bundle ---

    private void removeOneFromBundle(ItemStack bundle, int indexToRemove) {
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) return;

        List<ItemStack> newItems = new ArrayList<>();
        int i = 0;
        for (ItemStack s : contents.iterate()) {
            if (i == indexToRemove) {
                ItemStack copy = s.copy();
                copy.decrement(1);
                if (!copy.isEmpty()) {
                    newItems.add(copy);
                }
            } else {
                newItems.add(s.copy());
            }
            i++;
        }
        bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(newItems));
    }

    private boolean hasEnchantment(ItemStack stack, World world, net.minecraft.registry.RegistryKey<net.minecraft.enchantment.Enchantment> key) {
        var registry = world.getRegistryManager();
        var lookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var entry = lookup.getOptional(key);
        return entry.isPresent() && EnchantmentHelper.getLevel(entry.get(), stack) > 0;
    }

    // --- Platzierungs-Logik ---

    public static List<BlockPos> getBuildingPositions(BlockPos originPos, Direction face, int diameter) {
        List<BlockPos> positions = new ArrayList<>();
        int radius = (diameter - 1) / 2;

        // Der Wand erweitert die Fläche.
        // Wenn ich auf die OBERSEITE (UP) klicke, will ich auf der gleichen Y-Höhe bleiben?
        // NEIN. Ein Building Wand baut normalerweise *an die Seite dran*, auf die man klickt.
        // Beispiel: Ich klicke auf die OBERSEITE eines Bodens. Der Wand baut eine 3x3 Schicht OBEN DRAUF.

        // Zentrum der neuen Schicht:
        BlockPos centerPos = originPos.offset(face);

        for (int u = -radius; u <= radius; u++) {
            for (int v = -radius; v <= radius; v++) {
                BlockPos targetPos = null;

                // Koordinaten-Mapping basierend auf der Fläche
                if (face == Direction.UP || face == Direction.DOWN) {
                    // Fläche ist X/Z. u=X, v=Z
                    targetPos = centerPos.add(u, 0, v);
                }
                else if (face == Direction.NORTH || face == Direction.SOUTH) {
                    // Fläche ist X/Y. u=X, v=Y
                    targetPos = centerPos.add(u, v, 0);
                }
                else if (face == Direction.EAST || face == Direction.WEST) {
                    // Fläche ist Y/Z. u=Z, v=Y (oder andersrum)
                    targetPos = centerPos.add(0, v, u);
                }

                if (targetPos != null) {
                    positions.add(targetPos);
                }
            }
        }
        return positions;
    }

    private int findSlotWithBlock(PlayerEntity player, Block blockToFind) {
        for (int i = 0; i < player.getInventory().size(); i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (!stack.isEmpty() && stack.getItem() instanceof BlockItem blockItem) {
                if (blockItem.getBlock() == blockToFind) {
                    return i;
                }
            }
        }
        return -1;
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        textConsumer.accept(Text.translatable("tooltip.simplebuilding.wand_size", (wandSquareDiameter + "x" + wandSquareDiameter)).formatted(net.minecraft.util.Formatting.GRAY));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }

    public void setPlaceSound(SoundEvent placeSound) {
        this.placeSound = placeSound;
    }
}