package com.simplebuilding.items.custom;

import com.simplebuilding.client.gui.BuildingWandScreen;
import com.simplebuilding.enchantment.ModEnchantments;
import net.minecraft.block.Block;
import net.minecraft.block.BlockState;
import net.minecraft.block.Blocks;
import net.minecraft.client.MinecraftClient;
import net.minecraft.component.DataComponentTypes;
import net.minecraft.component.type.BundleContentsComponent;
import net.minecraft.component.type.NbtComponent;
import net.minecraft.enchantment.EnchantmentHelper;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EquipmentSlot;
import net.minecraft.entity.player.PlayerEntity;
import net.minecraft.item.BlockItem;
import net.minecraft.item.Item;
import net.minecraft.item.ItemStack;
import net.minecraft.item.ItemUsageContext;
import net.minecraft.nbt.NbtCompound;
import net.minecraft.registry.Registries;
import net.minecraft.registry.RegistryKeys;
import net.minecraft.server.network.ServerPlayerEntity;
import net.minecraft.server.world.ServerWorld;
import net.minecraft.sound.BlockSoundGroup;
import net.minecraft.sound.SoundCategory;
import net.minecraft.sound.SoundEvent;
import net.minecraft.sound.SoundEvents;
import net.minecraft.util.ActionResult;
import net.minecraft.util.Hand;
import net.minecraft.util.hit.BlockHitResult;
import net.minecraft.util.math.BlockPos;
import net.minecraft.util.math.Direction;
import net.minecraft.world.World;

import java.util.*;

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
        public void consume() { if (fromBundle) removeOneFromBundle(sourceStack, bundleIndex); else sourceStack.decrement(1); }
    }

    public BuildingWandItem(Settings settings) {
        super(settings);
        this.maxDiameter = 3; // Standard (Copper), sollte extern gesetzt werden
    }

    public void setWandSquareDiameter(int diameter) {
        this.maxDiameter = diameter;
    }

    public int getWandSquareDiameter() {
        return this.maxDiameter;
    }

    // --- Material Finding Logic (Priorität: Offhand -> Hotbar -> Inventory) ---

    /**
     * Gibt eine Map zurück, die jeder Position den BlockState zuweist, der dort platziert würde.
     * Berücksichtigt Color Palette (Zufall) und Inventar-Priorität.
     */
    public static Map<BlockPos, BlockState> getPreviewStates(World world, PlayerEntity player, ItemStack wandStack, BlockPos originPos, Direction face, int maxDiameter) {
        Map<BlockPos, BlockState> previewMap = new HashMap<>();

        // 1. Positionen berechnen
        NbtComponent comp = wandStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = comp.copyNbt();
        int maxTierRadius = (maxDiameter - 1) / 2;
        int userRadius = nbt.contains("SettingsRadius") ? nbt.getInt("SettingsRadius", maxTierRadius) : maxTierRadius;
        if (userRadius > maxTierRadius) userRadius = maxTierRadius;
        int axisMode = nbt.getInt("SettingsAxis", 0);

        List<BlockPos> positions = new ArrayList<>();
        for (int r = 0; r <= userRadius; r++) {
            positions.addAll(calculatePositions(originPos, face, r, axisMode));
        }

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

    // Client-Helper: Findet den ersten BlockState, ohne ItemStack zu verändern
    private static BlockState findFirstBlockStateClient(PlayerEntity player, ItemStack wandStack, boolean hasMasterBuilder) {
        World world = player.getEntityWorld();
        // 1. Offhand
        BlockState off = checkStackIsBlockState(player.getOffHandStack(), wandStack, world, hasMasterBuilder);
        if (off != null) return off;
        // 2. Hotbar
        for (int i = 0; i < 9; i++) {
            BlockState res = checkStackIsBlockState(player.getInventory().getStack(i), wandStack, world, hasMasterBuilder);
            if (res != null) return res;
        }
        // 3. Inv
        if (hasMasterBuilder) {
            for (int i = 9; i < player.getInventory().getMainStacks().size(); i++) {
                BlockState res = checkStackIsBlockState(player.getInventory().getStack(i), wandStack, world, hasMasterBuilder);
                if (res != null) return res;
            }
        }
        return null;
    }

    // Hilfsmethode: Holt ALLE Baublöcke für Color Palette
    private static List<BlockState> findAllBuildingBlocks(PlayerEntity player, ItemStack wandStack, boolean hasMasterBuilder) {
        List<BlockState> blocks = new ArrayList<>();
        World world = player.getEntityWorld();

        // Helper Lambda oder Loop
        // Offhand
        collectBlocksFromStack(player.getOffHandStack(), wandStack, world, hasMasterBuilder, blocks);
        // Main Inventory
        int limit = hasMasterBuilder ? player.getInventory().getMainStacks().size() : 9;
        for (int i = 0; i < limit; i++) {
            collectBlocksFromStack(player.getInventory().getStack(i), wandStack, world, hasMasterBuilder, blocks);
        }
        return blocks;
    }

    private static void collectBlocksFromStack(ItemStack stack, ItemStack wand, World world, boolean masterBuilder, List<BlockState> list) {
        if (stack.isEmpty()) return;
        if (stack.getItem() instanceof BlockItem bi) {
            list.add(bi.getBlock().getDefaultState());
        } else if (stack.getItem() instanceof ReinforcedBundleItem) {
            boolean bundleHasMB = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
            if (masterBuilder || bundleHasMB) {
                BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
                if (contents != null) {
                    for (ItemStack s : contents.iterate()) {
                        if (s.getItem() instanceof BlockItem bi) {
                            list.add(bi.getBlock().getDefaultState());
                        }
                    }
                }
            }
        }
    }

    private static BlockState checkStackIsBlockState(ItemStack stack, ItemStack wandStack, World world, boolean wandHasMasterBuilder) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof BlockItem bi) {
            return bi.getBlock().getDefaultState();
        }
        if (stack.getItem() instanceof ReinforcedBundleItem) {
            boolean bundleHasMasterBuilder = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
            if (wandHasMasterBuilder || bundleHasMasterBuilder) {
                BundleContentsComponent contents = stack.get(DataComponentTypes.BUNDLE_CONTENTS);
                if (contents != null && !contents.isEmpty()) {
                    // Nimmt den ersten Block aus dem Bundle
                    for (ItemStack s : contents.iterate()) {
                        if (s.getItem() instanceof BlockItem bi) return bi.getBlock().getDefaultState();
                    }
                }
            }
        }
        return null;
    }

    // --- Bestehende Server Logic (Unverändert wichtig für das tatsächliche Platzieren) ---

    private MaterialResult findFirstBuildingBlock(PlayerEntity player, ItemStack wandStack, boolean hasMasterBuilder) {
        World world = player.getEntityWorld();

        // 1. Offhand
        MaterialResult offHandRes = checkStackIsBlock(player.getOffHandStack(), wandStack, world, hasMasterBuilder);
        if (offHandRes != null) return offHandRes;

        // 2. Hotbar (0-8)
        for (int i = 0; i < 9; i++) {
            MaterialResult res = checkStackIsBlock(player.getInventory().getStack(i), wandStack, world, hasMasterBuilder);
            if (res != null) return res;
        }

        // 3. Main Inventory (nur wenn Master Builder)
        if (hasMasterBuilder) {
            for (int i = 9; i < player.getInventory().getMainStacks().size(); i++) {
                MaterialResult res = checkStackIsBlock(player.getInventory().getStack(i), wandStack, world, hasMasterBuilder);
                if (res != null) return res;
            }
        }
        return null;
    }

    // Prüft, ob ein Stack ein Block oder ein gültiges Bundle ist, und gibt das Resultat zurück
    private MaterialResult checkStackIsBlock(ItemStack stack, ItemStack wandStack, World world, boolean wandHasMasterBuilder) {
        if (stack.isEmpty()) return null;

        // Ist es ein Block?
        if (stack.getItem() instanceof BlockItem bi) {
            MaterialResult res = new MaterialResult();
            res.sourceStack = stack;
            res.fromBundle = false;
            res.stateToPlace = bi.getBlock().getDefaultState();
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
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return null;

        int i = 0;
        for (ItemStack s : contents.iterate()) {
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi) {
                MaterialResult res = new MaterialResult();
                res.sourceStack = bundle;
                res.fromBundle = true;
                res.bundleIndex = i;
                res.stateToPlace = bi.getBlock().getDefaultState();
                return res;
            }
            i++;
        }
        return null;
    }

    // Für den tatsächlichen Bauvorgang, wenn Color Palette aktiv ist,
    // müssen wir hier zufällig wählen oder den spezifischen Block suchen.
    // Da "InventoryTick" Positionen abarbeitet, nutzen wir hier eine vereinfachte Logik:
    // Wenn Color Palette aktiv ist, suchen wir irgendeinen Block.
    // Wenn nicht, suchen wir den spezifischen "TargetBlock".

    private MaterialResult findMaterialForPlacement(PlayerEntity player, ItemStack wand, Block targetBlock, boolean wandHasMasterBuilder, boolean colorPaletteActive) {
        // Wenn Color Palette aktiv ist, ist targetBlock egal, wir nehmen den nächsten verfügbaren.
        if (colorPaletteActive) {
            return findFirstBuildingBlock(player, wand, wandHasMasterBuilder);
        } else {
            return findSpecificMaterial(player, wand, targetBlock, wandHasMasterBuilder);
        }
    }

    private MaterialResult findSpecificMaterial(PlayerEntity player, ItemStack wand, Block targetBlock, boolean wandHasMasterBuilder) {
        World world = player.getEntityWorld();

        ItemStack offHand = player.getOffHandStack();
        MaterialResult offRes = checkStackForSpecificBlock(offHand, targetBlock, wand, world, wandHasMasterBuilder);
        if (offRes != null) return offRes;

        for (int i = 0; i < 9; i++) {
            MaterialResult res = checkStackForSpecificBlock(player.getInventory().getStack(i), targetBlock, wand, world, wandHasMasterBuilder);
            if (res != null) return res;
        }

        if (wandHasMasterBuilder) {
            for (int i = 9; i < player.getInventory().getMainStacks().size(); i++) {
                MaterialResult res = checkStackForSpecificBlock(player.getInventory().getStack(i), targetBlock, wand, world, wandHasMasterBuilder);
                if (res != null) return res;
            }
        }
        return null;
    }

    private MaterialResult checkStackForSpecificBlock(ItemStack stack, Block targetBlock, ItemStack wandStack, World world, boolean wandHasMasterBuilder) {
        if (stack.isEmpty()) return null;
        if (stack.getItem() instanceof BlockItem bi && bi.getBlock() == targetBlock) {
            MaterialResult res = new MaterialResult(); res.sourceStack = stack; res.fromBundle = false; res.stateToPlace = bi.getBlock().getDefaultState(); return res;
        }
        if (stack.getItem() instanceof ReinforcedBundleItem) {
            boolean bundleHasMasterBuilder = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
            if (wandHasMasterBuilder || bundleHasMasterBuilder) return findInBundleSpecific(stack, targetBlock);
        }
        return null;
    }

    private MaterialResult findInBundleSpecific(ItemStack bundle, Block targetBlock) {
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null || contents.isEmpty()) return null;
        int i = 0;
        for (ItemStack s : contents.iterate()) {
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock() == targetBlock) {
                MaterialResult res = new MaterialResult(); res.sourceStack = bundle; res.fromBundle = true; res.bundleIndex = i; res.stateToPlace = bi.getBlock().getDefaultState(); return res;
            }
            i++;
        }
        return null;
    }

    // --- Interaction ---

    @Override
    public ActionResult useOnBlock(ItemUsageContext context) {
        if (context.getHand() != Hand.MAIN_HAND) return ActionResult.PASS;
        World world = context.getWorld();
        PlayerEntity player = context.getPlayer();
        BlockPos clickedPos = context.getBlockPos();
        ItemStack wandStack = context.getStack();

        if (player == null) return ActionResult.PASS;
        if (world.isClient()) return ActionResult.SUCCESS;

        // Prüfen, ob wir überhaupt ein Material haben, bevor wir starten
        boolean hasMasterBuilder = hasEnchantment(wandStack, world, ModEnchantments.MASTER_BUILDER);
        MaterialResult preview = findFirstBuildingBlock(player, wandStack, hasMasterBuilder);

        if (preview == null && !player.getAbilities().creativeMode) return ActionResult.FAIL;

        Block buildBlock = preview != null ? preview.stateToPlace.getBlock() : Blocks.AIR;

        Direction clickedFace = context.getSide();
        NbtCompound nbt = getOrInitNbt(wandStack);
        nbt.putBoolean("Active", true);
        nbt.putInt("CurrentRadius", 0);
        nbt.putInt("Timer", 0);
        nbt.putInt("OriginX", clickedPos.getX());
        nbt.putInt("OriginY", clickedPos.getY());
        nbt.putInt("OriginZ", clickedPos.getZ());
        nbt.putInt("Face", clickedFace.ordinal());
        nbt.putInt("BuildBlockRawId", Registries.BLOCK.getRawId(buildBlock));

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

        int maxTierRadius = (this.maxDiameter - 1) / 2;
        int userRadius = nbt.contains("SettingsRadius") ? nbt.getInt("SettingsRadius", maxTierRadius) : maxTierRadius;
        if (userRadius > maxTierRadius) userRadius = maxTierRadius;

        int currentRadius = getBlockInt(nbt, "CurrentRadius");

        boolean isLinePlace = hasEnchantment(stack, world, ModEnchantments.LINEAR);
        boolean hasMasterBuilder = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
        boolean hasColorPalette = hasEnchantment(stack, world, ModEnchantments.COLOR_PALETTE);

        int ox = getBlockInt(nbt, "OriginX"); int oy = getBlockInt(nbt, "OriginY"); int oz = getBlockInt(nbt, "OriginZ");
        BlockPos originPos = new BlockPos(ox, oy, oz);
        Direction face = Direction.values()[getBlockInt(nbt, "Face")];

        int blockId = nbt.getInt("BuildBlockRawId", Registries.BLOCK.getRawId(Blocks.AIR));
        Block targetBlock = Registries.BLOCK.get(blockId);

        // Wenn kein Color Palette, brauchen wir einen festen Block
        if (!hasColorPalette && targetBlock == Blocks.AIR) {
             MaterialResult res = findFirstBuildingBlock(player, stack, hasMasterBuilder);
             if (res != null) targetBlock = res.stateToPlace.getBlock();
             else { nbt.putBoolean("Active", false); setNbt(stack, nbt); return; }
        }

        // Settings
        int axisMode = nbt.getInt("SettingsAxis", 0);

        // Positionen berechnen
        List<BlockPos> stepPositions = calculatePositions(originPos, face, currentRadius, axisMode);

        for (BlockPos rawPos : stepPositions) {
            if (!world.getBlockState(rawPos).isReplaceable()) continue;

            // Finde Material: Bei Color Palette irgendeins, sonst spezifisch
            MaterialResult material = findMaterialForPlacement(player, stack, targetBlock, hasMasterBuilder, hasColorPalette);

            if (material == null && !player.getAbilities().creativeMode) {
                nbt.putBoolean("Active", false); setNbt(stack, nbt); return;
            }

            BlockState stateToPlace = material != null ? material.stateToPlace : (hasColorPalette ? Blocks.STONE.getDefaultState() : targetBlock.getDefaultState());

            if (world.setBlockState(rawPos, stateToPlace, 3)) {
                BlockSoundGroup soundGroup = stateToPlace.getSoundGroup();
                world.playSound(null, rawPos, soundGroup.getPlaceSound(), SoundCategory.BLOCKS, (soundGroup.getVolume() + 1.0F) / 2.0F, soundGroup.getPitch() * 0.8F);
                if (!player.getAbilities().creativeMode && material != null) {
                    material.consume();
                    stack.damage(1, player, EquipmentSlot.MAINHAND);
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

    // --- Calculation Logic ---

    // Statische Methode für Renderer und Logic
    public static List<BlockPos> getBuildingPositions(World world, PlayerEntity player, ItemStack wandStack, BlockPos originPos, Direction face, int maxDiameter, BlockHitResult hitResult) {
        // NBT lesen für Settings
        NbtComponent comp = wandStack.getOrDefault(DataComponentTypes.CUSTOM_DATA, NbtComponent.DEFAULT);
        NbtCompound nbt = comp.copyNbt();

        int maxTierRadius = (maxDiameter - 1) / 2;
        int userRadius = nbt.contains("SettingsRadius") ? nbt.getInt("SettingsRadius", maxTierRadius) : maxTierRadius;
        if (userRadius > maxTierRadius) userRadius = maxTierRadius;

        int axisMode = nbt.getInt("SettingsAxis", 0);

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
        BlockPos placeOrigin = originPos.offset(face);

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
                positions.add(placeOrigin.add(offset));
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

    // --- Helper ---

    private static void removeOneFromBundle(ItemStack bundle, int indexToRemove) {
        BundleContentsComponent contents = bundle.get(DataComponentTypes.BUNDLE_CONTENTS);
        if (contents == null) return;
        List<ItemStack> newItems = new ArrayList<>();
        int i = 0;
        for (ItemStack s : contents.iterate()) {
            if (i == indexToRemove) { ItemStack copy = s.copy(); copy.decrement(1); if (!copy.isEmpty()) newItems.add(copy); }
            else { newItems.add(s.copy()); }
            i++;
        }
        bundle.set(DataComponentTypes.BUNDLE_CONTENTS, new BundleContentsComponent(newItems));
    }

    private static boolean hasEnchantment(ItemStack stack, World world, net.minecraft.registry.RegistryKey<net.minecraft.enchantment.Enchantment> key) {
        if (world == null) return false;
        var registry = world.getRegistryManager();
        var lookup = registry.getOrThrow(RegistryKeys.ENCHANTMENT);
        var entry = lookup.getOptional(key);
        return entry.isPresent() && EnchantmentHelper.getLevel(entry.get(), stack) > 0;
    }

    private boolean getBlockBoolean(NbtCompound nbt) { if (!nbt.contains("Active")) return false; return nbt.getBoolean("Active", false); }
    private int getBlockInt(NbtCompound nbt, String key) { if (!nbt.contains(key)) return 0; return nbt.getInt(key, 0); }
    private NbtCompound getOrInitNbt(ItemStack stack) { NbtComponent component = stack.get(DataComponentTypes.CUSTOM_DATA); return component != null ? component.copyNbt() : new NbtCompound(); }
    private void setNbt(ItemStack stack, NbtCompound nbt) { stack.set(DataComponentTypes.CUSTOM_DATA, NbtComponent.of(nbt)); }
}