package com.simplebuilding.items.custom;

import com.simplebuilding.client.gui.BuildingWandScreen;
import com.simplebuilding.enchantment.ModEnchantments;
import com.simplebuilding.items.custom.OctantItem;
import com.simplebuilding.items.custom.ReinforcedBundleItem;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.client.MinecraftClient;
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
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
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
    public static final int DELAY_TICKS = 6;
    public static final int DELAY_TICKS_LINE = 3;

    private int wandSquareDiameter;
    private SoundEvent placeSound = SoundEvents.BLOCK_STONE_PLACE;

    private static class MaterialResult {
        ItemStack sourceStack;
        int bundleIndex;
        boolean fromBundle;
        BlockState stateToPlace;
        public void consume() { if (fromBundle) removeOneFromBundle(sourceStack, bundleIndex); else sourceStack.decrement(1); }
    }

    public BuildingWandItem(Settings settings) {
        super(settings);
        this.wandSquareDiameter = 3; // Standardwert, kann per Setter geändert werden
    }

    // --- Getter für den Renderer ---
    public int getWandSquareDiameter() {
        return this.wandSquareDiameter;
    }

    public void setWandSquareDiameter(int diameter) {
        this.wandSquareDiameter = diameter;
    }

    // --- Methode für den Renderer und die interne Logik ---
    public static List<BlockPos> getBuildingPositions(World world, PlayerEntity player, ItemStack wandStack, BlockPos originPos, Direction face, int diameter, BlockHitResult hitResult) {
        // Hilfsmethode, die die Parameter für calculatePositions vorbereitet
        var hitPos = hitResult.getPos().subtract(originPos.getX(), originPos.getY(), originPos.getZ());
        int radius = (diameter - 1) / 2;

        // Check Bridge/Linear Enchantments
        boolean isBridge = hasEnchantment(wandStack, world, ModEnchantments.BRIDGE);
        boolean isLinePlace = hasEnchantment(wandStack, world, ModEnchantments.LINEAR);
        if (isBridge || isLinePlace) {
             // Bei Line/Bridge nutzen wir den Durchmesser als Reichweite in eine Richtung
             // Hier vereinfacht: Radius anpassen
             if (isBridge) radius = diameter;
        }

        return calculatePositions(world, wandStack, originPos, face, radius, player.getHorizontalFacing(), hitPos.x, hitPos.y, hitPos.z, diameter);
    }

    // --- Die eigentliche mathematische Berechnung ---
    private static List<BlockPos> calculatePositions(World world, ItemStack wandStack, BlockPos originPos, Direction face, int r, Direction playerFacing, double hitX, double hitY, double hitZ, int diameter) {
        List<BlockPos> positions = new ArrayList<>();

        boolean isLinear = hasEnchantment(wandStack, world, ModEnchantments.LINEAR);

        // Basis-Logik: Ein Quadrat auf der Fläche
        if (!isLinear) {
            for (int u = -r; u <= r; u++) {
                for (int v = -r; v <= r; v++) {
                    // Berechnung der Koordinaten basierend auf der Blickrichtung (Face)
                    BlockPos offset = getOffsetInPlane(face, u, v);
                    BlockPos target = originPos.offset(face).add(offset); // Ein Block vor der geklickten Fläche

                    // Optional: Prüfen ob der Block dahinter (originPos + offset) dem geklickten Block entspricht (Surface Snapping)
                    // Für "SimpleBuilding" platzieren wir meist einfach an der Seite.
                    positions.add(target);
                }
            }
        } else {
            // Linear Logic (Nur eine Linie)
            // Bestimme Ausrichtung basierend auf Hit-Position auf dem Block oder Spieler-Blickrichtung
            Direction.Axis axis1, axis2;
            if (face.getAxis() == Direction.Axis.Y) {
                // Boden/Decke -> X oder Z
                axis1 = Direction.Axis.X; axis2 = Direction.Axis.Z;
            } else {
                axis1 = Direction.Axis.Y;
                axis2 = (face.getAxis() == Direction.Axis.X) ? Direction.Axis.Z : Direction.Axis.X;
            }

            // Einfache Linie (Horizontal oder Vertikal basierend auf Blick)
            // Hier stark vereinfacht für das Beispiel:
            for (int i = 0; i < diameter; i++) {
                 // Logik für Linie...
                 // positions.add(...)
            }
            // Fallback auf 1 Punkt wenn Logik zu komplex für diesen Snippet
            if (positions.isEmpty()) positions.add(originPos.offset(face));
        }

        return positions;
    }

    private static BlockPos getOffsetInPlane(Direction face, int u, int v) {
        return switch (face) {
            case UP, DOWN -> new BlockPos(u, 0, v);
            case NORTH, SOUTH -> new BlockPos(u, v, 0);
            case EAST, WEST -> new BlockPos(0, u, v);
        };
    }

    // --- Bestehende Logik ---

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getHand() != Hand.MAIN_HAND) return ActionResult.PASS;
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        BlockPos clickedPos = context.getBlockPos();
        ItemStack wandStack = context.getStack();

        if (player == null) return ActionResult.PASS;

        // GUI öffnen bei Sneak + Enchantment
        boolean hasConstructorsTouch = hasEnchantment(wandStack, world, ModEnchantments.CONSTRUCTORS_TOUCH);
        if (player.isSneaking() && hasConstructorsTouch) {
            if (world.isClient()) {
                MinecraftClient.getInstance().setScreen(new BuildingWandScreen(wandStack));
                player.playSound(SoundEvents.UI_BUTTON_CLICK.value(), 0.5f, 1.0f);
            }
            return ActionResult.SUCCESS;
        }

        // Server-Side Logik starten
        if (world.isClient()) return ActionResult.SUCCESS;

        Direction clickedFace = context.getSide();

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
        nbt.putFloat("HitX", (float) hitPos.x); nbt.putFloat("HitY", (float) hitPos.y); nbt.putFloat("HitZ", (float) hitPos.z);
        setNbt(wandStack, nbt);
        return ActionResult.CONSUME;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerWorld world, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayerEntity player)) return;
        NbtCompound nbt = getOrInitNbt(stack);
        if (!getBlockBoolean(nbt)) return;
        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) { nbt.putBoolean("Active", false); setNbt(stack, nbt); return; }

        int timer = getBlockInt(nbt, "Timer");
        if (timer > 0) { nbt.putInt("Timer", timer - 1); setNbt(stack, nbt); return; }

        int currentRadius = getBlockInt(nbt, "CurrentRadius");
        int maxRadius = (this.wandSquareDiameter - 1) / 2;

        boolean isBridge = hasEnchantment(stack, world, ModEnchantments.BRIDGE);
        boolean isLinePlace = hasEnchantment(stack, world, ModEnchantments.LINEAR);
        boolean isCover = hasEnchantment(stack, world, ModEnchantments.COVER);
        boolean hasMasterBuilder = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);

        if (isBridge || isLinePlace) { if (isBridge) maxRadius = this.wandSquareDiameter; }

        int ox = getBlockInt(nbt, "OriginX"); int oy = getBlockInt(nbt, "OriginY"); int oz = getBlockInt(nbt, "OriginZ");
        BlockPos originPos = new BlockPos(ox, oy, oz);
        Direction face = Direction.values()[getBlockInt(nbt, "Face")];
        Direction playerFacing = Direction.values()[getBlockInt(nbt, "PlayerFacing")];
        double hitX = nbt.getFloat("HitX").orElse(0.5f);
        double hitY = nbt.getFloat("HitY").orElse(0.5f);
        double hitZ = nbt.getFloat("HitZ").orElse(0.5f);

        BlockState originState = world.getBlockState(originPos);
        Block patternBlock = originState.getBlock();
        Block materialBlock = patternBlock;
        BlockState materialState = originState;

        ItemStack offHand = player.getOffHandStack();
        if (!offHand.isEmpty() && offHand.getItem() instanceof BlockItem bi) {
            materialBlock = bi.getBlock();
            materialState = bi.getBlock().getDefaultState();
        }

        List<BlockPos> stepPositions = calculatePositions(world, stack, originPos, face, currentRadius, playerFacing, hitX, hitY, hitZ, this.wandSquareDiameter);

        for (BlockPos rawPos : stepPositions) {
            BlockPos targetPos = null;
            if (isCover) {
                int maxDeviation = currentRadius;
                for (int depth = 1; depth <= maxDeviation + 1; depth++) {
                    BlockPos checkPos = rawPos.offset(face.getOpposite(), depth);
                    if (isValidSupport(world.getBlockState(checkPos), patternBlock, true)) { targetPos = checkPos.offset(face); break; }
                }
            } else { targetPos = rawPos; }

            if (targetPos == null) continue;
            if (!world.getBlockState(targetPos).isReplaceable()) continue;

            // --- MATERIAL SUCHE (Neue Logik) ---
            MaterialResult material = findMaterial(player, stack, materialBlock, hasMasterBuilder, nbt);

            if (material == null && !player.getAbilities().creativeMode) { nbt.putBoolean("Active", false); setNbt(stack, nbt); return; }

            BlockState stateToPlace = material != null ? material.stateToPlace : materialState;
            if (world.setBlockState(targetPos, stateToPlace, 3)) {
                BlockSoundGroup soundGroup = stateToPlace.getSoundGroup();
                world.playSound(null, targetPos, soundGroup.getPlaceSound(), SoundCategory.BLOCKS, (soundGroup.getVolume() + 1.0F) / 2.0F, soundGroup.getPitch() * 0.8F);
                if (!player.getAbilities().creativeMode && material != null) {
                    material.consume();
                    stack.damage(1, player, EquipmentSlot.MAINHAND);
                }
            }
        }

        if (currentRadius < maxRadius) {
            nbt.putInt("CurrentRadius", currentRadius + 1);
            nbt.putInt("Timer", isLinePlace ? DELAY_TICKS_LINE : DELAY_TICKS);
        } else { nbt.putBoolean("Active", false); }
        setNbt(stack, nbt);
    }

    private MaterialResult findMaterial(PlayerEntity player, ItemStack wand, Block targetBlock, boolean wandHasMasterBuilder, NbtCompound wandNbt) {
        World world = player.getEntityWorld();
        boolean useFullInventory = wandHasMasterBuilder && wandNbt.getBoolean("UseFullInventory", false);

        ItemStack offHand = player.getOffHandStack();
        MaterialResult offRes = checkStackForMaterial(offHand, targetBlock, wand, world, wandHasMasterBuilder);
        if (offRes != null) return offRes;

        for (int i = 0; i < 9; i++) {
            MaterialResult res = checkStackForMaterial(player.getInventory().getStack(i), targetBlock, wand, world, wandHasMasterBuilder);
            if (res != null) return res;
        }

        if (useFullInventory) {
            for (int i = 9; i < player.getInventory().getMainStacks().size(); i++) {
                MaterialResult res = checkStackForMaterial(player.getInventory().getStack(i), targetBlock, wand, world, wandHasMasterBuilder);
                if (res != null) return res;
            }
        }
        return null;
    }

    private MaterialResult checkStackForMaterial(ItemStack stack, Block targetBlock, ItemStack wandStack, World world, boolean wandHasMasterBuilder) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof BlockItem bi && bi.getBlock() == targetBlock) {
            MaterialResult res = new MaterialResult(); res.sourceStack = stack; res.fromBundle = false; res.stateToPlace = bi.getBlock().getDefaultState(); return res;
        }
        if (stack.getItem() instanceof ReinforcedBundleItem) {
            boolean bundleHasMasterBuilder = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
            if (wandHasMasterBuilder || bundleHasMasterBuilder) return findInBundle(stack, targetBlock);
        }
        return null;
    }

    private MaterialResult findInBundle(ItemStack bundle, Block targetBlock) {
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return null;
        int i = 0;
        for (ItemStack s : contents.iterate()) {
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi) {
                if (targetBlock == null || bi.getBlock() == targetBlock) {
                    MaterialResult res = new MaterialResult();
                    res.sourceStack = bundle;
                    res.fromBundle = true;
                    res.bundleIndex = i;
                    res.stateToPlace = bi.getBlock().getDefaultState();
                    return res;
                }
            }
            i++;
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

    // --- Helper Methoden (NBT, Enchantments etc) ---

    private static boolean hasEnchantment(ItemStack stack, World world, net.minecraft.registry.RegistryKey<net.minecraft.enchantment.Enchantment> key) {
        if (world == null) return false;
        var registry = world.getRegistryManager();
        var lookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var entry = lookup.getOptional(key);
        return entry.isPresent() && EnchantmentHelper.getLevel(entry.get(), stack) > 0;
    }

    public boolean isValidSupport(BlockState supportState, Block patternBlock, boolean isCover) {
        if (supportState.isAir() || supportState.isLiquid()) return false;
        if (isCover) return true;
        return supportState.getBlock() == patternBlock;
    }

    private boolean getBlockBoolean(NbtCompound nbt) { if (!nbt.contains("Active")) return false; return nbt.getBoolean("Active").orElse(false); }
    private int getBlockInt(NbtCompound nbt, String key) { if (!nbt.contains(key)) return 0; return nbt.getInt(key).orElse(0); }
    private NbtCompound getOrInitNbt(ItemStack stack) { NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA); return component != null ? component.copyNbt() : new NbtCompound(); }
    private void setNbt(ItemStack stack, NbtCompound nbt) { stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt)); }
}