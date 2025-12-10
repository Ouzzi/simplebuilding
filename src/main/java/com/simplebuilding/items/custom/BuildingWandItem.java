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
    public static final int DELAY_TICKS_LINE = 3; // 150ms

    private int wandSquareDiameter;
    private SoundEvent placeSound = SoundEvents.BLOCK_STONE_PLACE;

    private static class MaterialResult {
        ItemStack sourceStack; int bundleIndex; boolean fromBundle; BlockState stateToPlace;
        public void consume() { if (fromBundle) removeOneFromBundle(sourceStack, bundleIndex); else sourceStack.decrement(1); }
    }

    public BuildingWandItem(Settings settings) {
        super(settings);
        this.wandSquareDiameter = 1;
    }

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getHand() != net.minecraft.util.Hand.MAIN_HAND) return ActionResult.PASS;

        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        BlockPos clickedPos = context.getBlockPos();
        Direction clickedFace = context.getSide();
        ItemStack wandStack = context.getStack();

        if (world.isClient() || player == null) return ActionResult.SUCCESS;

        NbtCompound nbt = getOrInitNbt(wandStack);

        nbt.putBoolean("Active", true);
        nbt.putInt("CurrentRadius", 0);
        nbt.putInt("Timer", 0);

        nbt.putInt("OriginX", clickedPos.getX());
        nbt.putInt("OriginY", clickedPos.getY());
        nbt.putInt("OriginZ", clickedPos.getZ());
        nbt.putInt("Face", clickedFace.ordinal());
        nbt.putInt("PlayerFacing", player.getHorizontalFacing().ordinal());

        var hitPos = context.getHitPos().subtract(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());
        nbt.putFloat("HitX", (float) hitPos.x);
        nbt.putFloat("HitY", (float) hitPos.y);
        nbt.putFloat("HitZ", (float) hitPos.z);

        setNbt(wandStack, nbt);
        return ActionResult.CONSUME;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayerEntity player)) return;

        NbtCompound nbt = getOrInitNbt(stack);
        if (!getBlockBoolean(nbt)) return;

        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) {
            nbt.putBoolean("Active", false);
            setNbt(stack, nbt);
            return;
        }

        int timer = getBlockInt(nbt, "Timer");
        if (timer > 0) {
            nbt.putInt("Timer", timer - 1);
            setNbt(stack, nbt);
            return;
        }

        // --- Ausführung ---
        int currentRadius = getBlockInt(nbt, "CurrentRadius");
        int maxRadius = (this.wandSquareDiameter - 1) / 2;

        boolean isBridge = hasEnchantment(stack, world, ModEnchantments.BRIDGE);
        boolean isLinePlace = hasEnchantment(stack, world, ModEnchantments.LINEAR);
        boolean isCover = hasEnchantment(stack, world, ModEnchantments.COVER);

        if (isBridge || isLinePlace) {
             if (isBridge) maxRadius = this.wandSquareDiameter;
        }

        int ox = getBlockInt(nbt, "OriginX");
        int oy = getBlockInt(nbt, "OriginY");
        int oz = getBlockInt(nbt, "OriginZ");
        BlockPos originPos = new BlockPos(ox, oy, oz);

        int faceInt = getBlockInt(nbt, "Face");
        if (faceInt < 0 || faceInt >= Direction.values().length) faceInt = 0;
        Direction face = Direction.values()[faceInt];

        Direction playerFacing = Direction.values()[getBlockInt(nbt, "PlayerFacing")];

        double hitX = nbt.getFloat("HitX").orElse(0.5f);
        double hitY = nbt.getFloat("HitY").orElse(0.5f);
        double hitZ = nbt.getFloat("HitZ").orElse(0.5f);

        // A. Pattern Block (Ursprungsblock als Referenz für die Validierung)
        BlockState originState = world.getBlockState(originPos);
        Block patternBlock = originState.getBlock();

        // B. Material Block (Was wir platzieren wollen)
        Block materialBlock = patternBlock;
        BlockState materialState = originState;

        ItemStack offHandStack = player.getOffHandStack();
        if (!offHandStack.isEmpty() && offHandStack.getItem() instanceof BlockItem bi) {
            materialBlock = bi.getBlock();
            materialState = bi.getBlock().getDefaultState();
        }

        List<BlockPos> stepPositions = calculatePositions(world, stack, originPos, face, currentRadius, playerFacing, hitX, hitY, hitZ, this.wandSquareDiameter);
        boolean hasMasterBuilder = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
        boolean placedAny = false;

        for (BlockPos rawPos : stepPositions) {
            BlockPos targetPos = null;

            if (isCover) {
                // COVER MODUS: Prüfen, ob eine Oberfläche existiert (Löcher füllen / Terrain folgen)
                // Deviation skaliert mit dem Radius (1 Block pro Ebene)
                int maxDeviation = currentRadius;

                for (int depth = 1; depth <= maxDeviation + 1; depth++) {
                    BlockPos checkPos = rawPos.offset(face.getOpposite(), depth);
                    BlockState checkState = world.getBlockState(checkPos);

                    // Validierung gegen Pattern Block (nur wenn Oberfläche da ist)
                    if (isValidSupport(checkState, patternBlock, true)) {
                        targetPos = checkPos.offset(face);
                        break;
                    }
                }
            } else {
                // SQUARE MODUS (Default): Platzieren im Raster, auch in der Luft
                // Wir nehmen die berechnete Position direkt an, ohne Support-Check.
                targetPos = rawPos;
            }

            // Wenn keine Position gefunden wurde oder der Zielblock nicht ersetzbar ist (z.B. Stein), überspringen
            if (targetPos == null) continue;
            if (!world.getBlockState(targetPos).isReplaceable()) continue;

            MaterialResult material = findMaterial(player, stack, materialBlock, hasMasterBuilder);
            if (material == null && !player.getAbilities().creativeMode) {
                nbt.putBoolean("Active", false);
                setNbt(stack, nbt);
                return;
            }

            BlockState stateToPlace = material != null ? material.stateToPlace : materialState;

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
            nbt.putInt("Timer", isLinePlace ? DELAY_TICKS_LINE : DELAY_TICKS);
        } else {
            nbt.putBoolean("Active", false);
        }
        setNbt(stack, nbt);
    }


    public static List<BlockPos> getBuildingPositions(World world, PlayerEntity player, ItemStack wandStack, BlockPos originPos, Direction face, int diameter, BlockHitResult hitResult) {
        List<BlockPos> positions = new ArrayList<>();
        if (!(wandStack.getItem() instanceof BuildingWandItem wandItem)) return positions;

        int radius = (diameter - 1) / 2;
        boolean isBridge = hasEnchantment(wandStack, world, ModEnchantments.BRIDGE);
        boolean isLinePlace = hasEnchantment(wandStack, world, ModEnchantments.LINEAR);
        boolean isCover = hasEnchantment(wandStack, world, ModEnchantments.COVER);
        int maxSteps = (isBridge || isLinePlace) ? diameter : radius;

        var vec = hitResult.getPos().subtract(originPos.getX(), originPos.getY(), originPos.getZ());
        double hitX = vec.x; double hitY = vec.y; double hitZ = vec.z;

        // Pattern Block (Origin) holen
        BlockState originState = world.getBlockState(originPos);
        Block patternBlock = originState.getBlock();

        for (int r = 0; r <= maxSteps; r++) {
            List<BlockPos> stepPositions = calculatePositions(world, wandStack, originPos, face, r, player.getHorizontalFacing(), hitX, hitY, hitZ, diameter);

            for (BlockPos rawPos : stepPositions) {
                BlockPos targetPos = null;

                if (isCover) {
                    // COVER MODUS: Nur wenn Support da ist (Vorschau muss Logik matchen)
                    // Deviation skaliert mit dem Radius (1 Block pro Ebene)
                    int maxDeviation = r;

                    for (int depth = 1; depth <= maxDeviation + 1; depth++) {
                        BlockPos checkPos = rawPos.offset(face.getOpposite(), depth);
                        BlockState checkState = world.getBlockState(checkPos);

                        if (wandItem.isValidSupport(checkState, patternBlock, true)) {
                            targetPos = checkPos.offset(face);
                            break;
                        }
                    }
                } else {
                    // SQUARE MODUS: Auch in der Luft anzeigen
                    targetPos = rawPos;
                }

                if (targetPos != null && world.getBlockState(targetPos).isReplaceable()) {
                    positions.add(targetPos);
                }
            }
        }
        return positions;
    }

    private static List<BlockPos> calculatePositions(World world, ItemStack wandStack, BlockPos originPos, Direction face, int r, Direction playerFacing, double hitX, double hitY, double hitZ, int diameter) {
        List<BlockPos> positions = new ArrayList<>();
        boolean isBridge = hasEnchantment(wandStack, world, ModEnchantments.BRIDGE);
        boolean isLinePlace = hasEnchantment(wandStack, world, ModEnchantments.LINEAR);

        if (isBridge) {
            Direction buildDir = face;
            if (face == Direction.UP || face == Direction.DOWN) {
                if (hitX < 0.2) buildDir = Direction.WEST;
                else if (hitX > 0.8) buildDir = Direction.EAST;
                else if (hitZ < 0.2) buildDir = Direction.NORTH;
                else if (hitZ > 0.8) buildDir = Direction.SOUTH;
            }
            BlockPos stepCenter = originPos.offset(buildDir, r + 1);

            if (isLinePlace) {positions.add(stepCenter);
            } else {
                int widthRadius = (diameter - 1) / 2;
                Direction.Axis widthAxis;
                if (buildDir.getAxis() == Direction.Axis.Y) {
                    widthAxis = (playerFacing.getAxis() == Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
                } else {
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
        if (isLinePlace) {
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
            if (r == 0) positions.add(centerPos);
            else {
                positions.add(getPosOnAxis(centerPos, axis, r));
                positions.add(getPosOnAxis(centerPos, axis, -r));
            }
            return positions;
        }

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

    private MaterialResult findMaterial(PlayerEntity player, ItemStack wandStack, Block targetBlock, boolean hasMasterBuilder) {
        World world = player.getEntityWorld();
        ItemStack offHand = player.getOffHandStack();
        if (!offHand.isEmpty()) {
            if (hasMasterBuilder && offHand.getItem() instanceof ReinforcedBundleItem) {
                MaterialResult res = findInBundle(offHand, targetBlock, wandStack, world);
                if (res != null) return res;
            } else if (offHand.getItem() instanceof BlockItem bi && bi.getBlock() == targetBlock) {
                MaterialResult res = new MaterialResult();
                res.sourceStack = offHand;
                res.fromBundle = false;
                res.stateToPlace = bi.getBlock().getDefaultState();
                return res;
            }
        }
        if (hasMasterBuilder) {
            for (int i = 0; i < 9; i++) {
                ItemStack stack = player.getInventory().getStack(i);
                if (stack.getItem() instanceof ReinforcedBundleItem) {
                     MaterialResult res = findInBundle(stack, targetBlock, wandStack, world);
                     if (res != null) return res;
                }
            }
        }
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
        boolean hasColorPalette = hasEnchantment(wand, world, ModEnchantments.COLOR_PALETTE);
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return null;
        if (hasColorPalette) {
            List<Integer> validIndices = new ArrayList<>();
            int i = 0;
            for (ItemStack s : contents.iterate()) {
                if (!s.isEmpty() && s.getItem() instanceof BlockItem) validIndices.add(i);
                i++;
            }
            if (validIndices.isEmpty()) return null;
            int randomIndex = validIndices.get(world.random.nextInt(validIndices.size()));
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
        } else {
            int i = 0; int firstValidIndex = -1; BlockItem firstValidBlock = null;
            for (ItemStack s : contents.iterate()) {
                if (!s.isEmpty() && s.getItem() instanceof BlockItem bi) {
                    if (bi.getBlock() == targetBlock) {
                        MaterialResult res = new MaterialResult();
                        res.sourceStack = bundle;
                        res.fromBundle = true;
                        res.bundleIndex = i;
                        res.stateToPlace = bi.getBlock().getDefaultState();
                        return res;
                    }
                    if (firstValidIndex == -1) { firstValidIndex = i; firstValidBlock = bi; }
                }
                i++;
            }
            if (firstValidIndex != -1) {
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
            if (i == indexToRemove) { ItemStack copy = s.copy(); copy.decrement(1); if (!copy.isEmpty()) newItems.add(copy); } else { newItems.add(s.copy()); }
            i++;
        }
        bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(newItems));
    }

    @Override
    public void appendTooltip(ItemStack stack, TooltipContext context, TooltipDisplayComponent displayComponent, Consumer<Text> textConsumer, TooltipType type) {
        boolean isLinePlace = false;
        if (context.getRegistryLookup() != null) {
            var registry = context.getRegistryLookup().getOptional(RegistryKeys.ENCHANTMENT);
            if (registry.isPresent()) {
                var linePlaceEntry = registry.get().getOptional(ModEnchantments.LINEAR);
                if (linePlaceEntry.isPresent()) isLinePlace = EnchantmentHelper.getLevel(linePlaceEntry.get(), stack) > 0;
            }
        }
        if (isLinePlace) textConsumer.accept(Text.translatable("tooltip.simplebuilding.building_wand.line_size", wandSquareDiameter).formatted(net.minecraft.util.Formatting.GRAY));
        else textConsumer.accept(Text.translatable("tooltip.simplebuilding.building_wand.size", wandSquareDiameter, wandSquareDiameter).formatted(net.minecraft.util.Formatting.GRAY));
        super.appendTooltip(stack, context, displayComponent, textConsumer, type);
    }

    private static boolean hasEnchantment(ItemStack stack, World world, net.minecraft.registry.RegistryKey<net.minecraft.enchantment.Enchantment> key) {
        if (world == null) return false;
        var registry = world.getRegistryManager();
        var lookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var entry = lookup.getOptional(key);
        return entry.isPresent() && EnchantmentHelper.getLevel(entry.get(), stack) > 0;
    }
    public boolean isValidSupport(BlockState supportState, Block patternBlock, boolean isCover) {
        if (supportState.isAir() || supportState.isLiquid()) return false;

        if (isCover) {
            return true;
        } else {
            return supportState.getBlock() == patternBlock;
        }
    }

    public int getWandSquareDiameter() { return this.wandSquareDiameter; }
    public void setWandSquareDiameter(int wandSquareDiameter) { this.wandSquareDiameter = wandSquareDiameter; }
    private static BlockPos getPosOnAxis(BlockPos center, Direction.Axis axis, int offset) {
        if (axis == Direction.Axis.X) return center.add(offset, 0, 0);
        if (axis == Direction.Axis.Y) return center.add(0, offset, 0);
        if (axis == Direction.Axis.Z) return center.add(0, 0, offset);
        return center;
    }
    private boolean getBlockBoolean(NbtCompound nbt) { if (!nbt.contains("Active")) return false; return nbt.getBoolean("Active").orElse(false); }
    private int getBlockInt(NbtCompound nbt, String key) { if (!nbt.contains(key)) return 0; return nbt.getInt(key).orElse(0); }
    private NbtCompound getOrInitNbt(ItemStack stack) { NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA); return component != null ? component.copyNbt() : new NbtCompound(); }
    private void setNbt(ItemStack stack, NbtCompound nbt) { stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt)); }
    public SoundEvent getPlaceSound() {return placeSound;}
    public void setPlaceSound(SoundEvent placeSound) {this.placeSound = placeSound;}
}