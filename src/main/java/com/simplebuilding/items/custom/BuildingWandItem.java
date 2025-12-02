package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.component.type.TooltipDisplayComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.item.tooltip.TooltipType;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.text.Text;
import net.minecraft.util.ActionResult;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

// TODO:
//  fix masterbuilder possibele
//  if something in way stop placing / if no enchantment that allows ignore stop placing



public class BuildingWandItem extends Item {

    public static final int BUILDING_WAND_SQUARE_COPPER = 3;
    public static final int BUILDING_WAND_SQUARE_IRON = 5;
    public static final int BUILDING_WAND_SQUARE_GOLD = 7;
    public static final int BUILDING_WAND_SQUARE_DIAMOND = 7;
    public static final int BUILDING_WAND_SQUARE_NETHERITE = 9;

    public static final int DURABILITY_MULTIPLAYER_WAND = 8;
    public static final int DELAY_TICKS = 6;      // 300ms
    public static final int DELAY_TICKS_LINE = 3; // 150ms (schneller)

    private int wandSquareDiameter;
    private SoundEvent placeSound = SoundEvents.BLOCK_STONE_PLACE;

    public BuildingWandItem(Settings settings) {
        super(settings);
        this.wandSquareDiameter = 1;
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
        if (context.getHand() != net.minecraft.util.Hand.MAIN_HAND) {
            return ActionResult.PASS;
        }

        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        BlockPos clickedPos = context.getBlockPos();
        Direction clickedFace = context.getSide();
        ItemStack wandStack = context.getStack();

        if (world.isClient() || player == null) return ActionResult.SUCCESS; // Success für Client Animation

        NbtCompound nbt = getOrInitNbt(wandStack);

        nbt.putBoolean("Active", true);
        nbt.putInt("CurrentRadius", 0);
        nbt.putInt("Timer", 0);

        nbt.putInt("OriginX", clickedPos.getX());
        nbt.putInt("OriginY", clickedPos.getY());
        nbt.putInt("OriginZ", clickedPos.getZ());
        nbt.putInt("Face", clickedFace.ordinal());

        // NEU: Speichere die Blickrichtung des Spielers beim Start (für Line Place / Bridge Konstanz)
        nbt.putInt("PlayerFacing", player.getHorizontalFacing().ordinal());

        // NEU: Speichere die genaue Hit-Position relativ zum Block (für Kanten-Erkennung)
        // Wir speichern float Werte (0.0 - 1.0)
        var hitPos = context.getHitPos().subtract(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());
        nbt.putFloat("HitX", (float) hitPos.x);
        nbt.putFloat("HitY", (float) hitPos.y);
        nbt.putFloat("HitZ", (float) hitPos.z);

        setNbt(wandStack, nbt);

        return ActionResult.CONSUME;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        // place blocks with delay for each layer

        if (!(entity instanceof ServerPlayerEntity player)) return;

        NbtCompound nbt = getOrInitNbt(stack);

        if (!getBlockBoolean(nbt, "Active", false)) return;

        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
            nbt.putBoolean("Active", false);
            setNbt(stack, nbt);
            return;
        }

        int timer = getBlockInt(nbt, "Timer", 0);
        if (timer > 0) {
            nbt.putInt("Timer", timer - 1);
            setNbt(stack, nbt);
            return;
        }

        // --- Aktion ausführen ---
        int currentRadius = getBlockInt(nbt, "CurrentRadius", 0);
        int maxRadius = (this.wandSquareDiameter - 1) / 2;

        // Enchantments für Radius-Anpassung
        boolean isBridge = hasEnchantment(stack, world, ModEnchantments.BRIDGE);
        boolean isLinePlace = hasEnchantment(stack, world, ModEnchantments.LINE_PLACE);

        if (isBridge || isLinePlace) {
             if (isBridge) maxRadius = this.wandSquareDiameter;
        }

        // Daten laden
        int ox = getBlockInt(nbt, "OriginX", 0);
        int oy = getBlockInt(nbt, "OriginY", 0);
        int oz = getBlockInt(nbt, "OriginZ", 0);
        BlockPos originPos = new BlockPos(ox, oy, oz);

        int faceInt = getBlockInt(nbt, "Face", 0);
        if (faceInt < 0 || faceInt >= Direction.values().length) faceInt = 0;
        Direction face = Direction.values()[faceInt];

        // NBT Daten lesen
        Direction playerFacing = Direction.values()[getBlockInt(nbt, "PlayerFacing", 0)];
        double hitX = nbt.getFloat("HitX").orElse(0.5f);
        double hitY = nbt.getFloat("HitY").orElse(0.5f);
        double hitZ = nbt.getFloat("HitZ").orElse(0.5f);

        BlockState stateToExtend = world.getBlockState(originPos);
        Block blockToExtend = stateToExtend.getBlock();

        ItemStack offHandStack = player.getOffHandStack();
        if (!offHandStack.isEmpty() && offHandStack.getItem() instanceof BlockItem bi) {
            blockToExtend = bi.getBlock();
            stateToExtend = bi.getBlock().getDefaultState();
        }

        // Positionen für diesen Schritt (Nutzt calculatePositions)
        List<BlockPos> stepPositions = calculatePositions(world, stack, originPos, face, currentRadius, playerFacing, hitX, hitY, hitZ, this.wandSquareDiameter);

        boolean hasMasterBuilder = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
        boolean placedAny = false;

        for (BlockPos targetPos : stepPositions) {
            BlockState targetState = world.getBlockState(targetPos);
            if (!targetState.isReplaceable()) continue;

            MaterialResult material = findMaterial(player, stack, blockToExtend, hasMasterBuilder);

            if (material == null && !player.getAbilities().creativeMode) {
                nbt.putBoolean("Active", false);
                setNbt(stack, nbt);
                return;
            }

            BlockState stateToPlace = material != null ? material.stateToPlace : stateToExtend;

           if (world.setBlockState(targetPos, stateToPlace, 3)) {
                BlockSoundGroup soundGroup = stateToPlace.getSoundGroup();
                world.playSound(null, targetPos, soundGroup.getPlaceSound(), SoundCategory.BLOCKS, (soundGroup.getVolume() + 1.0F) / 2.0F, soundGroup.getPitch() * 0.8F);

                if (!player.getAbilities().creativeMode && material != null) {
                    material.consume();
                    stack.damage(1, player, EquipmentSlot.MAINHAND);
                }
                placedAny = true;
            }
        }

        if (currentRadius < maxRadius) {
            nbt.putInt("CurrentRadius", currentRadius + 1);
            int delay = isLinePlace ? DELAY_TICKS_LINE : DELAY_TICKS;
            nbt.putInt("Timer", delay);
        } else {
            nbt.putBoolean("Active", false);
        }

        setNbt(stack, nbt);
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        boolean isLinePlace = false;

        if (context.getRegistryLookup() != null) {
            var registry = context.getRegistryLookup().getOptional(RegistryKeys.ENCHANTMENT);
            if (registry.isPresent()) {
                var linePlaceEntry = registry.get().getOptional(ModEnchantments.LINE_PLACE);
                if (linePlaceEntry.isPresent()) {
                    isLinePlace = EnchantmentHelper.getLevel(linePlaceEntry.get(), stack) > 0;
                }
            }
        }

        if (isLinePlace) {
            textConsumer.accept(Text.translatable("tooltip.simplebuilding.building_wand.line_size", wandSquareDiameter)
                    .formatted(net.minecraft.util.Formatting.GRAY));
        } else {
            textConsumer.accept(Text.translatable("tooltip.simplebuilding.building_wand.size", wandSquareDiameter, wandSquareDiameter)
                    .formatted(net.minecraft.util.Formatting.GRAY));
        }

        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }


    private static List<BlockPos> calculatePositions(World world, ItemStack wandStack, BlockPos originPos, Direction face, int r, Direction playerFacing, double hitX, double hitY, double hitZ, int diameter) {
        List<BlockPos> positions = new ArrayList<>();
        boolean isBridge = hasEnchantment(wandStack, world, ModEnchantments.BRIDGE);
        boolean linePlace = hasEnchantment(wandStack, world, ModEnchantments.LINE_PLACE);

        // --- BRIDGE MODE ---
        if (isBridge) {
            Direction buildDir = face;
            // Kanten-Erkennung
            if (face == Direction.UP || face == Direction.DOWN) {
                if (hitX < 0.2) buildDir = Direction.WEST;
                else if (hitX > 0.8) buildDir = Direction.EAST;
                else if (hitZ < 0.2) buildDir = Direction.NORTH;
                else if (hitZ > 0.8) buildDir = Direction.SOUTH;
            }

            BlockPos stepCenter = originPos.offset(buildDir, r + 1);

            if (linePlace) {
                // Nur Linie
                positions.add(stepCenter);
            } else {
                // Fläche (Diameter breit, 1 tief)
                int widthRadius = (diameter - 1) / 2;
                Direction.Axis widthAxis;

                if (buildDir.getAxis() == Direction.Axis.Y) {
                    widthAxis = (playerFacing.getAxis() == Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
                } else {
                    // Senkrecht zur Bau-Richtung (horizontal)
                    widthAxis = (buildDir.getAxis() == Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
                }

                for (int u = -widthRadius; u <= widthRadius; u++) {
                    BlockPos pos = null;
                    if (widthAxis == Direction.Axis.X) pos = stepCenter.add(u, 0, 0);
                    else if (widthAxis == Direction.Axis.Z) pos = stepCenter.add(0, 0, u);
                    else if (widthAxis == Direction.Axis.Y) pos = stepCenter.add(0, u, 0);

                    if (pos != null) positions.add(pos);
                }
            }
            return positions;
        }

        BlockPos centerPos = originPos.offset(face);

        // --- LINE PLACE MODE ---
        if (linePlace) {
            Direction.Axis axis;
            if (face.getAxis() == Direction.Axis.Y) {
                boolean nearEdgeZ = (hitZ < 0.2 || hitZ > 0.8);
                boolean nearEdgeX = (hitX < 0.2 || hitX > 0.8);
                if (nearEdgeZ) axis = Direction.Axis.Z;
                else if (nearEdgeX) axis = Direction.Axis.X;
                else axis = (playerFacing.getAxis() == Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
            } else {
                if (hitY < 0.2 || hitY > 0.8) axis = Direction.Axis.Y;
                else axis = (face.getAxis() == Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
            }

            if (r == 0) {
                positions.add(centerPos);
            } else {
                positions.add(getPosOnAxis(centerPos, axis, r));
                positions.add(getPosOnAxis(centerPos, axis, -r));
            }
            return positions;
        }

        // --- SURFACE PLACE (Standard) ---
        if (r == 0) {
            positions.add(centerPos);
            return positions;
        }
        for (int u = -r; u <= r; u++) {
            for (int v = -r; v <= r; v++) {
                if (Math.abs(u) != r && Math.abs(v) != r) continue;
                BlockPos targetPos = null;
                if (face.getAxis() == Direction.Axis.Y) targetPos = centerPos.add(u, 0, v);
                else if (face.getAxis() == Direction.Axis.Z) targetPos = centerPos.add(u, v, 0);
                else if (face.getAxis() == Direction.Axis.X) targetPos = centerPos.add(0, v, u);
                if (targetPos != null) positions.add(targetPos);
            }
        }
        return positions;
    }

    private static class MaterialResult {
        ItemStack sourceStack; int bundleIndex; boolean fromBundle; BlockState stateToPlace;
        public void consume() { if (fromBundle) removeOneFromBundle(sourceStack, bundleIndex); else sourceStack.decrement(1); }
    }

    private MaterialResult findMaterial(PlayerEntity player, ItemStack wandStack, Block targetBlock, boolean hasMasterBuilder) {
        World world = player.getEntityWorld();
        ItemStack offHand = player.getOffHandStack();

        // 1. PRIO: Offhand
        if (!offHand.isEmpty()) {
            // A. Bundle in Offhand (nur mit Master Builder) -> Höchste Prio
            if (hasMasterBuilder && offHand.getItem() instanceof ReinforcedBundleItem) {
                MaterialResult res = findInBundle(offHand, targetBlock, wandStack, world);
                if (res != null) return res; // Gefunden!
            }
            // B. Block in Offhand
            else if (offHand.getItem() instanceof BlockItem bi && bi.getBlock() == targetBlock) {
                MaterialResult res = new MaterialResult();
                res.sourceStack = offHand;
                res.fromBundle = false;
                res.stateToPlace = bi.getBlock().getDefaultState();
                return res;
            }
        }

        // 2. PRIO: Hotbar Bundles (Master Builder)
        if (hasMasterBuilder) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() instanceof ReinforcedBundleItem) {
                     MaterialResult res = findInBundle(stack, targetBlock, wandStack, world);
                     if (res != null) return res;
                }
            }
        }

        // 3. PRIO: Hotbar Items
        for (int i = 0; i < 9; i++) {
            ItemStack stack = player.getInventory().getStack(i);
            if (stack.getItem() instanceof BlockItem bi && bi.getBlock() == targetBlock) {
                MaterialResult res = new MaterialResult();
                res.sourceStack = stack;
                res.fromBundle = false;
                res.stateToPlace = bi.getBlock().getDefaultState();
                return res;
            }
        }

        return null;
    }

    private MaterialResult findInBundle(ItemStack bundle, Block targetBlock, ItemStack wand, World world) {
        // if bundle has color palette, then select random building blocks
        // else from first to last

        boolean hasColorPalette = hasEnchantment(wand, world, ModEnchantments.COLOR_PALETTE);
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);

        if (contents == null || contents.isEmpty()) return null;

        // --- MODUS 1: COLOR PALETTE (Zufall) ---
        if (hasColorPalette) {
            // Wir sammeln erst alle Indizes, an denen sich Baublöcke befinden
            List<Integer> validIndices = new ArrayList<>();
            int i = 0;
            for (ItemStack s : contents.iterate()) {
                if (!s.isEmpty() && s.getItem() instanceof BlockItem) {
                    validIndices.add(i);
                }
                i++;
            }

            // Wenn keine Blöcke im Bundle sind -> Abbruch
            if (validIndices.isEmpty()) return null;

            // Einen zufälligen Index aus der Liste wählen
            int randomIndex = validIndices.get(world.random.nextInt(validIndices.size()));

            // Das Item an diesem Index holen
            i = 0;
            for (ItemStack s : contents.iterate()) {
                if (i == randomIndex && s.getItem() instanceof BlockItem bi) {
                    MaterialResult res = new MaterialResult();
                    res.sourceStack = bundle;
                    res.fromBundle = true;
                    res.bundleIndex = randomIndex;
                    res.stateToPlace = bi.getBlock().getDefaultState();
                    return res;
                }
                i++;
            }
        }
        // --- MODUS 2: STANDARD (Master Builder Priorität) ---
        else {
            int i = 0;
            int firstValidIndex = -1;
            BlockItem firstValidBlock = null;

            for (ItemStack s : contents.iterate()) {
                if (!s.isEmpty() && s.getItem() instanceof BlockItem bi) {
                    // A. PRIORITÄT: Exakter Match mit dem Zielblock
                    if (bi.getBlock() == targetBlock) {
                        MaterialResult res = new MaterialResult();
                        res.sourceStack = bundle;
                        res.fromBundle = true;
                        res.bundleIndex = i;
                        res.stateToPlace = bi.getBlock().getDefaultState();
                        return res;
                    }

                    // B. FALLBACK: Merke dir den ersten gültigen Block, falls wir keinen Match finden
                    if (firstValidIndex == -1) {
                        firstValidIndex = i;
                        firstValidBlock = bi;
                    }
                }
                i++;
            }

            // Wenn kein exakter Match gefunden wurde, nimm den ersten Block (Fallback)
            if (firstValidIndex != -1 && firstValidBlock != null) {
                MaterialResult res = new MaterialResult();
                res.sourceStack = bundle;
                res.fromBundle = true;
                res.bundleIndex = firstValidIndex;
                res.stateToPlace = firstValidBlock.getBlock().getDefaultState();
                return res;
            }
        }

        return null;
    }

    private static void removeOneFromBundle(ItemStack bundle, int indexToRemove) {
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) return;
        List<ItemStack> newItems = new ArrayList<>();
        int i = 0;
        for (ItemStack s : contents.iterate()) {
            if (i == indexToRemove) {
                ItemStack copy = s.copy();
                copy.decrement(1);
                if (!copy.isEmpty()) newItems.add(copy);
            } else {
                newItems.add(s.copy());
            }
            i++;
        }
        bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(newItems));
    }


    public static List<BlockPos> getBuildingPositions(World world, PlayerEntity player, ItemStack wandStack, BlockPos originPos, Direction face, int diameter) {
        // determines where the blocks should be placed

        List<BlockPos> positions = new ArrayList<>();
        int radius = (diameter - 1) / 2;
        boolean isBridge = hasEnchantment(wandStack, world, ModEnchantments.BRIDGE);
        boolean isLinePlace = hasEnchantment(wandStack, world, ModEnchantments.LINE_PLACE);
        int maxSteps = (isBridge || isLinePlace) ? diameter : radius; // Bei Bridge/Line Place ist r == Länge (Durchmesser)

        // Simuliere HitPos für Client Renderer
        double hitX = 0.5, hitY = 0.5, hitZ = 0.5;
        var hit = player.raycast(20, 0, false);
        if (hit instanceof BlockHitResult bhr) {
            var vec = bhr.getPos().subtract(originPos.getX(), originPos.getY(), originPos.getZ());
            hitX = vec.x; hitY = vec.y; hitZ = vec.z;
        }

        for (int r = 0; r <= maxSteps; r++) {
            positions.addAll(calculatePositions(world, wandStack, originPos, face, r, player.getHorizontalFacing(), hitX, hitY, hitZ, diameter));
        }
        return positions;
    }

    private static boolean hasEnchantment(ItemStack stack, World world, net.minecraft.registry.RegistryKey<net.minecraft.enchantment.Enchantment> key) {
        if (world == null) return false;
        var registry = world.getRegistryManager();
        var lookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var entry = lookup.getOptional(key);
        return entry.isPresent() && EnchantmentHelper.getLevel(entry.get(), stack) > 0;
    }

    public int getWandSquareDiameter() {
        return this.wandSquareDiameter;
    }

    public void setWandSquareDiameter(int wandSquareDiameter) {
        this.wandSquareDiameter = wandSquareDiameter;
    }

    private static BlockPos getPosOnAxis(BlockPos center, Direction.Axis axis, int offset) {
        if (axis == Direction.Axis.X) return center.add(offset, 0, 0);
        if (axis == Direction.Axis.Y) return center.add(0, offset, 0);
        if (axis == Direction.Axis.Z) return center.add(0, 0, offset);
        return center;
    }

    private boolean getBlockBoolean(NbtCompound nbt, String key, boolean fallback) {
        if (!nbt.contains(key)) return fallback;
        return nbt.getBoolean(key).orElse(fallback);
    }

    private int getBlockInt(NbtCompound nbt, String key, int fallback) {
        if (!nbt.contains(key)) return fallback;
        return nbt.getInt(key).orElse(fallback);
    }

    private NbtCompound getOrInitNbt(ItemStack stack) {
        NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA);
        return component != null ? component.copyNbt() : new NbtCompound();
    }

    private void setNbt(ItemStack stack, NbtCompound nbt) {
        stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt));
    }

}