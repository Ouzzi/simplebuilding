package com.simplebuilding.items.custom;

import com.simplebuilding.enchantment.ModEnchantments;
import java.util.*;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.core.component.DataComponents;
import net.minecraft.core.registries.BuiltInRegistries;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.sounds.SoundSource;
import net.minecraft.world.InteractionHand;
import net.minecraft.world.InteractionResult;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.EquipmentSlot;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.Item;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;

import static com.simplebuilding.util.EnchantmentHelper.hasEnchantment;

public class BuildingWandItem extends Item {

    public static final int BUILDING_WAND_SQUARE_COPPER = 3; // Radius 1
    public static final int BUILDING_WAND_SQUARE_IRON = 5;   // Radius 2
    public static final int BUILDING_WAND_SQUARE_GOLD = 7;   // Radius 3
    public static final int BUILDING_WAND_SQUARE_DIAMOND = 9; // Radius 3
    public static final int BUILDING_WAND_SQUARE_NETHERITE = 11; // Radius 4
    public static final int BUILDING_WAND_SQUARE_ENDERITE = 13;

    public static final int DELAY_TICKS = 4; // Etwas schneller
    public static final int DELAY_TICKS_LINE = 2;

    private int maxDiameter; // Maximaler Durchmesser (Tier-abhängig)

    private static class MaterialResult {
        ItemStack sourceStack;
        int bundleIndex;
        boolean fromBundle;
        BlockState stateToPlace;
        public void consume() { if (fromBundle) removeOneFromBundle(sourceStack, bundleIndex); else sourceStack.shrink(1); }
    }

    public BuildingWandItem(Properties settings) {
        super(settings);
        this.maxDiameter = 3; // Standard (Copper), sollte extern gesetzt werden
    }

    public void setWandSquareDiameter(int diameter) {
        this.maxDiameter = diameter;
    }

    public int getWandSquareDiameter() {
        return this.maxDiameter;
    }

    /**
     * Gibt eine Map zurück, die jeder Position den BlockState zuweist, der dort platziert würde.
     * Berücksichtigt Color Palette (Zufall) und Inventar-Priorität.
     */
    public static Map<BlockPos, BlockState> getPreviewStates(Level world, Player player, ItemStack wandStack, BlockPos originPos, Direction face, int maxDiameter) {
        Map<BlockPos, BlockState> previewMap = new HashMap<>();

        // 1. Positionen berechnen
        CustomData comp = wandStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = comp.copyTag();
        int maxTierRadius = (maxDiameter - 1) / 2;
        int userRadius = getConfiguredRadius(nbt, maxTierRadius);
        int axisMode = nbt.getIntOr("SettingsAxis", 0);

        List<BlockPos> positions = collectPlacementPositions(originPos, face, userRadius, axisMode);

        if (positions.isEmpty()) return previewMap;

        boolean hasMasterBuilder = hasEnchantment(wandStack, world, ModEnchantments.MASTER_BUILDER);
        boolean hasColorPalette = hasEnchantment(wandStack, world, ModEnchantments.COLOR_PALETTE);

        if (hasColorPalette) {
            // --- Color Palette Logic: Sammle ALLE Blöcke und verteile sie zufällig ---
            List<BlockState> palette = findAllBuildingBlocks(player, wandStack, hasMasterBuilder);

            if (palette.isEmpty()) return previewMap;

            for (BlockPos pos : positions) {
                // Nutze die Position als Seed für Determinismus (kein Flackern)
                long seed = pos.asLong();
                int index = Math.abs((int)(seed % palette.size()));
                previewMap.put(pos, palette.get(index));
            }

        } else {
            // --- Standard Logic: Erster gefundener Block für alle ---
            // WICHTIG: Hier rufen wir die statische Client-taugliche Suche auf
            BlockState state = findFirstBlockStateClient(player, wandStack, hasMasterBuilder);

            if (state != null) {
                for (BlockPos pos : positions) {
                    previewMap.put(pos, state);
                }
            }
        }

        return previewMap;
    }

    private static int getConfiguredRadius(CompoundTag nbt, int maxTierRadius) {
        int userRadius = nbt.contains("SettingsRadius") ? nbt.getIntOr("SettingsRadius", maxTierRadius) : maxTierRadius;
        if (userRadius < 0) return 0;
        return Math.min(userRadius, maxTierRadius);
    }

    private static List<BlockPos> collectPlacementPositions(BlockPos originPos, Direction face, int userRadius, int axisMode) {
        List<BlockPos> positions = new ArrayList<>();
        for (int r = 0; r <= userRadius; r++) {
            positions.addAll(calculatePositions(originPos, face, r, axisMode));
        }
        return positions;
    }

    // Client-Helper: Findet den ersten BlockState, ohne ItemStack zu verändern
    private static BlockState findFirstBlockStateClient(Player player, ItemStack wandStack, boolean hasMasterBuilder) {
        Level world = player.level();
        // 1. Offhand
        BlockState off = checkStackIsBlockState(player.getOffhandItem(), world, hasMasterBuilder);
        if (off != null) return off;
        // 2. Hotbar
        for (int i = 0; i < 9; i++) {
            BlockState res = checkStackIsBlockState(player.getInventory().getItem(i), world, hasMasterBuilder);
            if (res != null) return res;
        }
        // 3. Inv
        if (hasMasterBuilder) {
            for (int i = 9; i < player.getInventory().getNonEquipmentItems().size(); i++) {
                BlockState res = checkStackIsBlockState(player.getInventory().getItem(i), world, hasMasterBuilder);
                if (res != null) return res;
            }
        }
        return null;
    }

    // Hilfsmethode: Holt ALLE Baublöcke für Color Palette
    private static List<BlockState> findAllBuildingBlocks(Player player, ItemStack wandStack, boolean hasMasterBuilder) {
        List<BlockState> blocks = new ArrayList<>();
        Level world = player.level();

        // Helper Lambda oder Loop
        // Offhand
        collectBlocksFromStack(player.getOffhandItem(), world, hasMasterBuilder, blocks);
        // Main Inventory
        int limit = hasMasterBuilder ? player.getInventory().getNonEquipmentItems().size() : 9;
        for (int i = 0; i < limit; i++) {
            collectBlocksFromStack(player.getInventory().getItem(i), world, hasMasterBuilder, blocks);
        }
        return blocks;
    }

    private static void collectBlocksFromStack(ItemStack stack, Level world, boolean masterBuilder, List<BlockState> list) {
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof BlockItem bi) {
            list.add(bi.getBlock().defaultBlockState());
        } else if (stack.getItem() instanceof ReinforcedBundleItem) {
            boolean bundleHasMB = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
            if (masterBuilder || bundleHasMB) {
                BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
                if (contents != null) {
                    for (ItemStack s : contents.itemsCopy()) {
                        if (s.getItem() instanceof BlockItem bi) {
                            list.add(bi.getBlock().defaultBlockState());
                        }
                    }
                }
            }
        }
    }

    private static BlockState checkStackIsBlockState(ItemStack stack, Level world, boolean wandHasMasterBuilder) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof BlockItem bi) {
            return bi.getBlock().defaultBlockState();
        }
        if (stack.getItem() instanceof ReinforcedBundleItem) {
            boolean bundleHasMasterBuilder = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
            if (wandHasMasterBuilder || bundleHasMasterBuilder) {
                BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
                if (contents != null && !contents.isEmpty()) {
                    // Nimmt den ersten Block aus dem Bundle
                    for (ItemStack s : contents.itemsCopy()) {
                        if (s.getItem() instanceof BlockItem bi) return bi.getBlock().defaultBlockState();
                    }
                }
            }
        }
        return null;
    }

    private MaterialResult findFirstBuildingBlock(Player player, ItemStack wandStack, boolean hasMasterBuilder) {
        Level world = player.level();

        // 1. Offhand
        MaterialResult offHandRes = checkStackIsBlock(player.getOffhandItem(), world, hasMasterBuilder);
        if (offHandRes != null) return offHandRes;

        // 2. Hotbar (0-8)
        for (int i = 0; i < 9; i++) {
            MaterialResult res = checkStackIsBlock(player.getInventory().getItem(i), world, hasMasterBuilder);
            if (res != null) return res;
        }

        // 3. Main Inventory (nur wenn Master Builder)
        if (hasMasterBuilder) {
            for (int i = 9; i < player.getInventory().getNonEquipmentItems().size(); i++) {
                MaterialResult res = checkStackIsBlock(player.getInventory().getItem(i), world, hasMasterBuilder);
                if (res != null) return res;
            }
        }
        return null;
    }

    private MaterialResult checkStackIsBlock(ItemStack stack, Level world, boolean wandHasMasterBuilder) {
        if (stack.isEmpty()) return null;

        // Ist es ein Block?
        if (stack.getItem() instanceof BlockItem bi) {
            MaterialResult res = new MaterialResult();
            res.sourceStack = stack;
            res.fromBundle = false;
            res.stateToPlace = bi.getBlock().defaultBlockState();
            return res;
        }

        // Ist es ein Bundle?
        if (stack.getItem() instanceof ReinforcedBundleItem) {
            boolean bundleHasMasterBuilder = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
            // Bundles dürfen genutzt werden, wenn Wand ODER Bundle MasterBuilder hat
            if (wandHasMasterBuilder || bundleHasMasterBuilder) {
                return findFirstBlockInBundle(stack);
            }
        }
        return null;
    }

    private MaterialResult findFirstBlockInBundle(ItemStack bundle) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return null;

        int i = 0;
        for (ItemStack s : contents.itemsCopy()) {
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi) {
                MaterialResult res = new MaterialResult();
                res.sourceStack = bundle;
                res.fromBundle = true;
                res.bundleIndex = i;
                res.stateToPlace = bi.getBlock().defaultBlockState();
                return res;
            }
            i++;
        }
        return null;
    }

    private MaterialResult findMaterialForPlacement(Player player, ItemStack wand, Block targetBlock, boolean wandHasMasterBuilder, boolean colorPaletteActive) {
        // Wenn Color Palette aktiv ist, ist targetBlock egal, wir nehmen den nächsten verfügbaren.
        if (colorPaletteActive) {
            return findFirstBuildingBlock(player, wand, wandHasMasterBuilder);
        } else {
            return findSpecificMaterial(player, wand, targetBlock, wandHasMasterBuilder);
        }
    }

    private MaterialResult findSpecificMaterial(Player player, ItemStack wand, Block targetBlock, boolean wandHasMasterBuilder) {
        Level world = player.level();

        ItemStack offHand = player.getOffhandItem();
        MaterialResult offRes = checkStackForSpecificBlock(offHand, targetBlock, wand, world, wandHasMasterBuilder);
        if (offRes != null) return offRes;

        for (int i = 0; i < 9; i++) {
            MaterialResult res = checkStackForSpecificBlock(player.getInventory().getItem(i), targetBlock, wand, world, wandHasMasterBuilder);
            if (res != null) return res;
        }

        if (wandHasMasterBuilder) {
            for (int i = 9; i < player.getInventory().getNonEquipmentItems().size(); i++) {
                MaterialResult res = checkStackForSpecificBlock(player.getInventory().getItem(i), targetBlock, wand, world, wandHasMasterBuilder);
                if (res != null) return res;
            }
        }
        return null;
    }

    private MaterialResult checkStackForSpecificBlock(ItemStack stack, Block targetBlock, ItemStack wandStack, Level world, boolean wandHasMasterBuilder) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof BlockItem bi && bi.getBlock() == targetBlock) {
            MaterialResult res = new MaterialResult(); res.sourceStack = stack; res.fromBundle = false; res.stateToPlace = bi.getBlock().defaultBlockState(); return res;
        }
        if (stack.getItem() instanceof ReinforcedBundleItem) {
            boolean bundleHasMasterBuilder = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
            if (wandHasMasterBuilder || bundleHasMasterBuilder) return findInBundleSpecific(stack, targetBlock);
        }
        return null;
    }

    private MaterialResult findInBundleSpecific(ItemStack bundle, Block targetBlock) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return null;
        int i = 0;
        for (ItemStack s : contents.itemsCopy()) {
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock() == targetBlock) {
                MaterialResult res = new MaterialResult(); res.sourceStack = bundle; res.fromBundle = true; res.bundleIndex = i; res.stateToPlace = bi.getBlock().defaultBlockState(); return res;
            }
            i++;
        }
        return null;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        Level world = context.getLevel();
        Player player = context.getPlayer();
        BlockPos clickedPos = context.getClickedPos();
        ItemStack wandStack = context.getItemInHand();

        if (player == null) return InteractionResult.PASS;
        if (world.isClientSide()) return InteractionResult.SUCCESS;

        // Prüfen, ob wir überhaupt ein Material haben, bevor wir starten
        boolean hasMasterBuilder = hasEnchantment(wandStack, world, ModEnchantments.MASTER_BUILDER);
        MaterialResult preview = findFirstBuildingBlock(player, wandStack, hasMasterBuilder);

        if (preview == null && !player.getAbilities().instabuild) return InteractionResult.FAIL;

        Block buildBlock = preview != null ? preview.stateToPlace.getBlock() : Blocks.AIR;

        Direction clickedFace = context.getClickedFace();
        CompoundTag nbt = getOrInitNbt(wandStack);
        nbt.putBoolean("Active", true);
        nbt.putInt("CurrentRadius", 0);
        nbt.putInt("Timer", 0);
        nbt.putInt("OriginX", clickedPos.getX());
        nbt.putInt("OriginY", clickedPos.getY());
        nbt.putInt("OriginZ", clickedPos.getZ());
        nbt.putInt("Face", clickedFace.ordinal());
        nbt.putInt("BuildBlockRawId", BuiltInRegistries.BLOCK.getId(buildBlock));

        var hitPos = context.getClickLocation().subtract(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());
        nbt.putFloat("HitX", (float) hitPos.x); nbt.putFloat("HitY", (float) hitPos.y); nbt.putFloat("HitZ", (float) hitPos.z);
        setNbt(wandStack, nbt);

        return InteractionResult.CONSUME;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player)) return;
        CompoundTag nbt = getOrInitNbt(stack);
        if (!getBlockBoolean(nbt)) return;

        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) { nbt.putBoolean("Active", false); setNbt(stack, nbt); return; }

        int timer = getBlockInt(nbt, "Timer");
        if (timer > 0) { nbt.putInt("Timer", timer - 1); setNbt(stack, nbt); return; }

        int maxTierRadius = (this.maxDiameter - 1) / 2;
        int userRadius = nbt.contains("SettingsRadius") ? nbt.getIntOr("SettingsRadius", maxTierRadius) : maxTierRadius;
        if (userRadius > maxTierRadius) userRadius = maxTierRadius;

        int currentRadius = getBlockInt(nbt, "CurrentRadius");

        boolean isLinePlace = hasEnchantment(stack, world, ModEnchantments.LINEAR);
        boolean hasMasterBuilder = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
        boolean hasColorPalette = hasEnchantment(stack, world, ModEnchantments.COLOR_PALETTE);

        int ox = getBlockInt(nbt, "OriginX"); int oy = getBlockInt(nbt, "OriginY"); int oz = getBlockInt(nbt, "OriginZ");
        BlockPos originPos = new BlockPos(ox, oy, oz);
        Direction face = Direction.values()[getBlockInt(nbt, "Face")];

        int blockId = nbt.getIntOr("BuildBlockRawId", BuiltInRegistries.BLOCK.getId(Blocks.AIR));
        Block targetBlock = BuiltInRegistries.BLOCK.byId(blockId);

        // Wenn kein Color Palette, brauchen wir einen festen Block
        if (!hasColorPalette && targetBlock == Blocks.AIR) {
             MaterialResult res = findFirstBuildingBlock(player, stack, hasMasterBuilder);
             if (res != null) targetBlock = res.stateToPlace.getBlock();
             else { nbt.putBoolean("Active", false); setNbt(stack, nbt); return; }
        }

        // Settings
        int axisMode = nbt.getIntOr("SettingsAxis", 0);

        // Positionen berechnen
        List<BlockPos> stepPositions = calculatePositions(originPos, face, currentRadius, axisMode);

        for (BlockPos rawPos : stepPositions) {
            if (!world.getBlockState(rawPos).canBeReplaced()) continue;

            // Finde Material: Bei Color Palette irgendeins, sonst spezifisch
            MaterialResult material = findMaterialForPlacement(player, stack, targetBlock, hasMasterBuilder, hasColorPalette);

            if (material == null && !player.getAbilities().instabuild) {
                nbt.putBoolean("Active", false); setNbt(stack, nbt); return;
            }

            BlockState stateToPlace = material != null ? material.stateToPlace : (hasColorPalette ? Blocks.STONE.defaultBlockState() : targetBlock.defaultBlockState());

            if (world.setBlock(rawPos, stateToPlace, 3)) {
                SoundType soundGroup = stateToPlace.getSoundType();
                world.playSound(null, rawPos, soundGroup.getPlaceSound(), SoundSource.BLOCKS, (soundGroup.getVolume() + 1.0F) / 2.0F, soundGroup.getPitch() * 0.8F);
                if (!player.getAbilities().instabuild && material != null) {
                    material.consume();
                    stack.hurtAndBreak(1, player, EquipmentSlot.MAINHAND);
                }
            }
        }

        if (currentRadius < userRadius) {
            nbt.putInt("CurrentRadius", currentRadius + 1);
            nbt.putInt("Timer", isLinePlace ? DELAY_TICKS_LINE : DELAY_TICKS);
        } else {
            nbt.putBoolean("Active", false);
        }
        setNbt(stack, nbt);
    }

    public static List<BlockPos> getBuildingPositions(Level world, Player player, ItemStack wandStack, BlockPos originPos, Direction face, int maxDiameter, BlockHitResult hitResult) {
        // NBT lesen für Settings
        CustomData comp = wandStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = comp.copyTag();

        int maxTierRadius = (maxDiameter - 1) / 2;
        int userRadius = nbt.contains("SettingsRadius") ? nbt.getIntOr("SettingsRadius", maxTierRadius) : maxTierRadius;
        if (userRadius > maxTierRadius) userRadius = maxTierRadius;

        int axisMode = nbt.getIntOr("SettingsAxis", 0);

        // Wir berechnen ALLE Positionen auf einmal für den Renderer
        List<BlockPos> allPositions = new ArrayList<>();
        for (int r = 0; r <= userRadius; r++) {
            allPositions.addAll(calculatePositions(originPos, face, r, axisMode));
        }
        return allPositions;
    }

    private static List<BlockPos> calculatePositions(BlockPos originPos, Direction face, int currentRadius, int axisMode) {
        List<BlockPos> positions = new ArrayList<>();

        // Basis-Punkt: Ein Block VOR der geklickten Seite (dort wo platziert wird)
        BlockPos placeOrigin = originPos.relative(face);

        // Achsen-Logik
        // 0 = Face-Aligned (Standard Wand Verhalten: Plane perpendicular to Face)
        // 1 = X Plane (Baut entlang Y/Z, flach auf X) - unüblich für "Wand", eher "Octant", aber gewünscht.
        //     Wenn User X wählt, will er wahrscheinlich eine Wand auf der X-Achse bauen.
        //     Hier interpretieren wir "Axis X" als: Baut in der Ebene definiert durch Y und Z (Normal = X).
        // 2 = Y Plane (Baut Boden/Decke, Ebene X/Z).
        // 3 = Z Plane (Baut Wand, Ebene X/Y).

        Direction.Axis buildAxis;
        if (axisMode == 1) buildAxis = Direction.Axis.X;
        else if (axisMode == 2) buildAxis = Direction.Axis.Y;
        else if (axisMode == 3) buildAxis = Direction.Axis.Z;
        else buildAxis = face.getAxis(); // Default: Achse der Blickrichtung

        // Um eine Fläche zu füllen, iterieren wir über die beiden ANDEREN Achsen.
        // currentRadius definiert den Ring (Hohlquadrat) dieses Schrittes.

        int r = currentRadius;
        if (r == 0) {
            positions.add(placeOrigin);
            return positions;
        }

        // Iteriere von -r bis +r für beide Offset-Achsen
        // Wir nehmen nur den Rand (Ring), wenn wir Tick-basiert bauen
        // Da die Methode aber "StepPositions" heißt, generieren wir hier den Rand für Radius r.

        for (int u = -r; u <= r; u++) {
            for (int v = -r; v <= r; v++) {
                // Nur den Rand hinzufügen (Optimierung für Animation/Ticking)
                if (Math.abs(u) != r && Math.abs(v) != r) continue;

                BlockPos offset = getOffsetForAxis(buildAxis, u, v);
                positions.add(placeOrigin.offset(offset));
            }
        }

        return positions;
    }

    private static BlockPos getOffsetForAxis(Direction.Axis axis, int u, int v) {
        // Definiert die Ebene senkrecht zur Achse
        return switch (axis) {
            case Y -> new BlockPos(u, 0, v); // Ebene X/Z (Boden)
            case Z -> new BlockPos(u, v, 0); // Ebene X/Y (Wand)
            case X -> new BlockPos(0, u, v); // Ebene Y/Z (Wand)
        };
    }

    private static void removeOneFromBundle(ItemStack bundle, int indexToRemove) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) return;
        List<ItemStack> newItems = new ArrayList<>();
        int i = 0;
        for (ItemStack s : contents.itemsCopy()) {
            if (i == indexToRemove) { ItemStack copy = s.copy(); copy.shrink(1); if (!copy.isEmpty()) newItems.add(copy); }
            else { newItems.add(s.copy()); }
            i++;
        }
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(List.copyOf(newItems)));
    }

    private boolean getBlockBoolean(CompoundTag nbt) { if (!nbt.contains("Active")) return false; return nbt.getBooleanOr("Active", false); }
    private int getBlockInt(CompoundTag nbt, String key) { if (!nbt.contains(key)) return 0; return nbt.getIntOr(key, 0); }
    private CompoundTag getOrInitNbt(ItemStack stack) { CustomData component = stack.get(DataComponents.CUSTOM_DATA); return component != null ? component.copyTag() : new CompoundTag(); }
    private void setNbt(ItemStack stack, CompoundTag nbt) { stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt)); }
}