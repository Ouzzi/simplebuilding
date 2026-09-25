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
import net.minecraft.world.item.ItemStackTemplate;
import net.minecraft.world.item.component.BundleContents;
import net.minecraft.world.item.component.CustomData;
import net.minecraft.world.item.context.UseOnContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.SoundType;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.phys.BlockHitResult;
import net.minecraft.world.phys.Vec3;
import com.simplebuilding.util.WandPlacement;
import com.simplebuilding.util.WandUndo;

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

    /** Bauformen eines Klicks (NBT {@code Mode}). */
    public static final int MODE_SQUARE = 0;
    /** Abdeckung: nur vor Bloecken derselben Sorte wie der angeklickte, zusammenhaengend. */
    public static final int MODE_COVER = 1;
    /** Linear + Schleichen: eine Linie von der Klickseite weg. */
    public static final int MODE_LINE = 2;
    /** Bruecke: Rechtsklick in die Luft, vom Block unter den Fuessen geradeaus. */
    public static final int MODE_BRIDGE = 3;

    /** Laenge von Linie und Bruecke: doppelter Durchmesser der eingestellten Flaeche (Kupfer 6 ... Enderit 26). */
    public static int lineLength(int radius) {
        return 2 * (2 * Math.max(0, radius) + 1);
    }

    private int maxDiameter; // Maximaler Durchmesser (Tier-abhängig)

    private static class MaterialResult {
        ItemStack sourceStack;
        int bundleIndex;
        boolean fromBundle;
        // Getragener Rucksack: sourceStack ist der Rucksack, bundleIndex der Eintrag in seiner Komponente.
        boolean fromBackpack;
        BlockState stateToPlace;
        /** Ein Stueck des verbrauchten Items (mit Komponenten) - fuer den Platzier-Kontext. */
        ItemStack item = ItemStack.EMPTY;
        public void consume() {
            if (fromBackpack) BackpackItem.consumeOne(sourceStack, bundleIndex);
            else if (fromBundle) removeOneFromBundle(sourceStack, bundleIndex);
            else sourceStack.shrink(1);
        }
    }

    /**
     * Baumaterial ist jedes {@link BlockItem} ausser einem Rucksack: Rucksaecke sind BlockItems
     * (sie lassen sich abstellen), der Zauberstab darf sie aber nie als Baublock verbauen - er
     * wuerde sie samt Inhalt in einen leeren Block verwandeln.
     */
    private static boolean isBuildingBlock(ItemStack stack) {
        return stack.getItem() instanceof BlockItem && !(stack.getItem() instanceof BackpackItem);
    }

    /**
     * Der getragene Rucksack als letzte Materialquelle - wie bei Buendeln nur, wenn der Rucksack
     * selbst Meisterbauer traegt (Entscheidung des Mod-Autors: nicht schon durch Meisterbauer auf
     * dem Zauberstab).
     */
    private static ItemStack masterBuilderBackpack(Player player) {
        return BackpackItem.wornBackpackWith(player, ModEnchantments.MASTER_BUILDER);
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
     * Gibt eine Map zurück, die jeder Position den BlockState zuweist, der dort platziert würde -
     * dieselbe Form ({@link Plan}), dieselbe Ausrichtung ({@link WandPlacement}) und dieselbe
     * Farbpalette ({@link #paletteIndex}) wie beim Bau. Trefferpunkt: Mitte der Klickseite.
     */
    public static Map<BlockPos, BlockState> getPreviewStates(Level world, Player player, ItemStack wandStack, BlockPos originPos, Direction face, int maxDiameter) {
        Vec3 hit = new Vec3(0.5 + face.getStepX() * 0.5, 0.5 + face.getStepY() * 0.5, 0.5 + face.getStepZ() * 0.5);
        return getPreviewStates(world, player, wandStack, originPos, face, hit, maxDiameter);
    }

    /** Wie oben, mit der echten Trefferposition relativ zum angeklickten Block (Treppen-/Stufenhaelfte). */
    public static Map<BlockPos, BlockState> getPreviewStates(Level world, Player player, ItemStack wandStack, BlockPos originPos, Direction face, Vec3 hitRel, int maxDiameter) {
        CompoundTag nbt = wandStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        int userRadius = getConfiguredRadius(nbt, (maxDiameter - 1) / 2);
        Plan plan = Plan.forClick(world, player, wandStack, originPos, face, hitRel, userRadius, nbt.getIntOr("SettingsAxis", 0));
        return previewOf(world, player, wandStack, plan);
    }

    /** Vorschau der Bruecke (Rechtsklick in die Luft); leer ohne Bruecke-Verzauberung oder ohne Boden unter den Fuessen. */
    public static Map<BlockPos, BlockState> getBridgePreview(Level world, Player player, ItemStack wandStack, int maxDiameter) {
        if (!hasEnchantment(wandStack, world, ModEnchantments.BRIDGE) || player.isShiftKeyDown()) return new LinkedHashMap<>();
        CompoundTag nbt = wandStack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY).copyTag();
        Plan plan = Plan.forBridge(world, player, getConfiguredRadius(nbt, (maxDiameter - 1) / 2));
        return plan == null ? new LinkedHashMap<>() : previewOf(world, player, wandStack, plan);
    }

    private static Map<BlockPos, BlockState> previewOf(Level world, Player player, ItemStack wandStack, Plan plan) {
        Map<BlockPos, BlockState> previewMap = new LinkedHashMap<>();
        boolean hasMasterBuilder = hasEnchantment(wandStack, world, ModEnchantments.MASTER_BUILDER);
        boolean hasColorPalette = hasEnchantment(wandStack, world, ModEnchantments.COLOR_PALETTE);
        // Color Palette: jede Stelle nimmt den Eintrag, den paletteIndex ihr zuweist - genau wie der Bau.
        List<ItemStack> palette = hasColorPalette ? findAllBuildingBlocks(player, wandStack, hasMasterBuilder) : List.of();
        ItemStack first = hasColorPalette ? null : findFirstBlockStateClient(player, wandStack, hasMasterBuilder);
        if (hasColorPalette ? palette.isEmpty() : first == null) return previewMap;
        BlockState clicked = plan.clickedState(world);
        for (BlockPos pos : plan.all(world)) {
            ItemStack item = hasColorPalette ? palette.get(paletteIndex(pos, palette.size())) : first;
            BlockState state = WandPlacement.stateFor(world, player, item, pos, plan.placeFace, plan.hitRel, clicked);
            if (state != null) previewMap.put(pos, state);
        }
        return previewMap;
    }

    private static int getConfiguredRadius(CompoundTag nbt, int maxTierRadius) {
        int userRadius = nbt.contains("SettingsRadius") ? nbt.getIntOr("SettingsRadius", maxTierRadius) : maxTierRadius;
        if (userRadius < 0) return 0;
        return Math.min(userRadius, maxTierRadius);
    }

    // Client-Helper: Findet den ersten Baublock (ein Stueck, mit Komponenten), ohne ItemStack zu verändern
    private static ItemStack findFirstBlockStateClient(Player player, ItemStack wandStack, boolean hasMasterBuilder) {
        Level world = player.level();
        // 1. Offhand
        ItemStack off = checkStackIsBlockState(player.getOffhandItem(), world, hasMasterBuilder);
        if (off != null) return off;
        // 2. Hotbar
        for (int i = 0; i < 9; i++) {
            ItemStack res = checkStackIsBlockState(player.getInventory().getItem(i), world, hasMasterBuilder);
            if (res != null) return res;
        }
        // 3. Inv
        if (hasMasterBuilder) {
            for (int i = 9; i < player.getInventory().getNonEquipmentItems().size(); i++) {
                ItemStack res = checkStackIsBlockState(player.getInventory().getItem(i), world, hasMasterBuilder);
                if (res != null) return res;
            }
        }
        // 4. Getragener Rucksack mit Meisterbauer
        ItemStack backpack = masterBuilderBackpack(player);
        if (!backpack.isEmpty()) {
            int index = BackpackItem.findEntry(backpack, BuildingWandItem::isBuildingBlock);
            if (index >= 0) return BackpackItem.entryStack(backpack, index).copyWithCount(1);
        }
        return null;
    }

    /**
     * Der Baublock, den ein Klick jetzt nehmen wuerde (erster Fund in Nebenhand, Hotbar, mit
     * Meisterbauer Inventar, Meisterbauer-Rucksack), als ein Stueck; {@code null} ohne Material.
     */
    public static ItemStack firstBuildingStack(Player player, ItemStack wandStack) {
        return findFirstBlockStateClient(player, wandStack, hasEnchantment(wandStack, player.level(), ModEnchantments.MASTER_BUILDER));
    }

    /**
     * Die Farbpalette: alle mitgefuehrten Baubloecke in Suchreihenfolge (je Stapel ein Eintrag, zwei
     * Stapel derselben Sorte zaehlen also doppelt), je ein Stueck mit Komponenten.
     */
    public static List<ItemStack> paletteStacks(Player player, ItemStack wandStack) {
        return findAllBuildingBlocks(player, wandStack, hasEnchantment(wandStack, player.level(), ModEnchantments.MASTER_BUILDER));
    }

    /**
     * Welcher Paletten-Eintrag an einer Stelle gesetzt wird: ein Hash der Position, gleich in
     * Vorschau und Bau und stabil zwischen zwei Aufrufen (kein Flackern), in allen drei Achsen
     * gemischt - auch ein flacher Boden wird bunt.
     */
    public static int paletteIndex(BlockPos pos, int size) {
        return size <= 0 ? -1 : (int) Math.floorMod(net.minecraft.util.Mth.getSeed(pos), (long) size);
    }

    // Hilfsmethode: Holt ALLE Baublöcke für Color Palette
    private static List<ItemStack> findAllBuildingBlocks(Player player, ItemStack wandStack, boolean hasMasterBuilder) {
        List<ItemStack> blocks = new ArrayList<>();
        Level world = player.level();

        // Helper Lambda oder Loop
        // Offhand
        collectBlocksFromStack(player.getOffhandItem(), world, hasMasterBuilder, blocks);
        // Main Inventory
        int limit = hasMasterBuilder ? player.getInventory().getNonEquipmentItems().size() : 9;
        for (int i = 0; i < limit; i++) {
            collectBlocksFromStack(player.getInventory().getItem(i), world, hasMasterBuilder, blocks);
        }
        // Getragener Rucksack mit Meisterbauer
        ItemStack backpack = masterBuilderBackpack(player);
        if (!backpack.isEmpty()) {
            for (ItemStack s : BackpackItem.entryStacks(backpack)) {
                if (isBuildingBlock(s)) blocks.add(s.copyWithCount(1));
            }
        }
        return blocks;
    }

    private static void collectBlocksFromStack(ItemStack stack, Level world, boolean masterBuilder, List<ItemStack> list) {
        if (stack.isEmpty()) return;
        if (isBuildingBlock(stack)) {
            list.add(stack.copyWithCount(1));
        } else if (stack.getItem() instanceof ReinforcedBundleItem) {
            boolean bundleHasMB = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
            if (masterBuilder || bundleHasMB) {
                BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
                if (contents != null) {
                    for (ItemStackTemplate template : contents.items()) {
                        ItemStack s = template.create();
                        if (s.getItem() instanceof BlockItem) {
                            list.add(s.copyWithCount(1));
                        }
                    }
                }
            }
        }
    }

    private static ItemStack checkStackIsBlockState(ItemStack stack, Level world, boolean wandHasMasterBuilder) {
        if (stack.isEmpty()) return null;
        if (isBuildingBlock(stack)) {
            return stack.copyWithCount(1);
        }
        if (stack.getItem() instanceof ReinforcedBundleItem) {
            boolean bundleHasMasterBuilder = hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER);
            if (wandHasMasterBuilder || bundleHasMasterBuilder) {
                BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
                if (contents != null && !contents.isEmpty()) {
                    // Nimmt den ersten Block aus dem Bundle
                    for (ItemStackTemplate template : contents.items()) {
                        ItemStack s = template.create();
                        if (s.getItem() instanceof BlockItem) return s.copyWithCount(1);
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
        // 4. Getragener Rucksack mit Meisterbauer
        return findInBackpack(player, BuildingWandItem::isBuildingBlock);
    }

    /** Erster passender Eintrag im getragenen Meisterbauer-Rucksack, sonst null. */
    private static MaterialResult findInBackpack(Player player, java.util.function.Predicate<ItemStack> test) {
        ItemStack backpack = masterBuilderBackpack(player);
        if (backpack.isEmpty()) return null;
        int index = BackpackItem.findEntry(backpack, test);
        if (index < 0) return null;
        MaterialResult res = new MaterialResult();
        res.sourceStack = backpack;
        res.fromBackpack = true;
        res.bundleIndex = index;
        res.stateToPlace = ((BlockItem) BackpackItem.entryStack(backpack, index).getItem()).getBlock().defaultBlockState();
        res.item = BackpackItem.entryStack(backpack, index).copyWithCount(1);
        return res;
    }

    private MaterialResult checkStackIsBlock(ItemStack stack, Level world, boolean wandHasMasterBuilder) {
        if (stack.isEmpty()) return null;

        // Ist es ein Block?
        if (isBuildingBlock(stack)) {
            MaterialResult res = new MaterialResult();
            res.sourceStack = stack;
            res.fromBundle = false;
            res.stateToPlace = ((BlockItem) stack.getItem()).getBlock().defaultBlockState();
            res.item = stack.copyWithCount(1);
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
        for (ItemStackTemplate template : contents.items()) {
            ItemStack s = template.create();
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi) {
                MaterialResult res = new MaterialResult();
                res.sourceStack = bundle;
                res.fromBundle = true;
                res.bundleIndex = i;
                res.stateToPlace = bi.getBlock().defaultBlockState();
                res.item = s.copyWithCount(1);
                return res;
            }
            i++;
        }
        return null;
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
        // Getragener Rucksack mit Meisterbauer
        return findInBackpack(player, s -> isBuildingBlock(s) && ((BlockItem) s.getItem()).getBlock() == targetBlock);
    }

    private MaterialResult checkStackForSpecificBlock(ItemStack stack, Block targetBlock, ItemStack wandStack, Level world, boolean wandHasMasterBuilder) {
        if (stack.isEmpty()) return null;
        if (isBuildingBlock(stack) && stack.getItem() instanceof BlockItem bi && bi.getBlock() == targetBlock) {
            MaterialResult res = new MaterialResult(); res.sourceStack = stack; res.fromBundle = false; res.stateToPlace = bi.getBlock().defaultBlockState(); res.item = stack.copyWithCount(1); return res;
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
        for (ItemStackTemplate template : contents.items()) {
            ItemStack s = template.create();
            if (!s.isEmpty() && s.getItem() instanceof BlockItem bi && bi.getBlock() == targetBlock) {
                MaterialResult res = new MaterialResult(); res.sourceStack = bundle; res.fromBundle = true; res.bundleIndex = i; res.stateToPlace = bi.getBlock().defaultBlockState(); res.item = s.copyWithCount(1); return res;
            }
            i++;
        }
        return null;
    }

    // =====================================================================================
    // Materialquellen fuer den Blaupausen-Baumodus: dieselbe Suche wie oben (Nebenhand, Hotbar,
    // mit Meisterbauer das ganze Inventar, Buendel mit Meisterbauer auf Stab oder Buendel, der
    // getragene Meisterbauer-Rucksack), aber nach Item statt nach Block - eine Wandfackel kostet
    // eine Fackel, eine Tuer ihr Tuer-Item.
    // =====================================================================================

    /** Erste Quelle fuer ein Item, oder {@code null}. {@link Runnable#run()} verbraucht ein Stueck. */
    public static Runnable findSupply(Player player, ItemStack wand, Item item) {
        Level world = player.level();
        boolean mb = hasEnchantment(wand, world, ModEnchantments.MASTER_BUILDER);
        java.util.function.Predicate<ItemStack> test = s -> s.is(item) && !(s.getItem() instanceof BackpackItem);
        MaterialResult res = supplyIn(player.getOffhandItem(), test, world, mb);
        if (res != null) return res::consume;
        int limit = mb ? player.getInventory().getNonEquipmentItems().size() : 9;
        for (int i = 0; i < limit; i++) {
            res = supplyIn(player.getInventory().getItem(i), test, world, mb);
            if (res != null) return res::consume;
        }
        res = findInBackpack(player, test);
        return res != null ? res::consume : null;
    }

    /** Wie viele Stueck eines Items {@link #findSupply} insgesamt fande. */
    public static int countSupply(Player player, ItemStack wand, Item item) {
        Level world = player.level();
        boolean mb = hasEnchantment(wand, world, ModEnchantments.MASTER_BUILDER);
        int total = countIn(player.getOffhandItem(), item, world, mb);
        int limit = mb ? player.getInventory().getNonEquipmentItems().size() : 9;
        for (int i = 0; i < limit; i++) {
            total += countIn(player.getInventory().getItem(i), item, world, mb);
        }
        ItemStack backpack = masterBuilderBackpack(player);
        if (!backpack.isEmpty()) {
            for (ItemStack s : BackpackItem.entryStacks(backpack)) {
                if (s.is(item)) total += s.getCount();
            }
        }
        return total;
    }

    private static MaterialResult supplyIn(ItemStack stack, java.util.function.Predicate<ItemStack> test, Level world, boolean wandMb) {
        if (stack.isEmpty()) return null;
        if (test.test(stack)) {
            MaterialResult res = new MaterialResult();
            res.sourceStack = stack;
            return res;
        }
        if (stack.getItem() instanceof ReinforcedBundleItem && (wandMb || hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER))) {
            BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (contents == null) return null;
            int i = 0;
            for (ItemStackTemplate template : contents.items()) {
                if (test.test(template.create())) {
                    MaterialResult res = new MaterialResult();
                    res.sourceStack = stack;
                    res.fromBundle = true;
                    res.bundleIndex = i;
                    return res;
                }
                i++;
            }
        }
        return null;
    }

    private static int countIn(ItemStack stack, Item item, Level world, boolean wandMb) {
        if (stack.isEmpty()) return 0;
        if (stack.is(item)) return stack.getCount();
        int total = 0;
        if (stack.getItem() instanceof ReinforcedBundleItem && (wandMb || hasEnchantment(stack, world, ModEnchantments.MASTER_BUILDER))) {
            BundleContents contents = stack.get(DataComponents.BUNDLE_CONTENTS);
            if (contents != null) {
                for (ItemStackTemplate template : contents.items()) {
                    ItemStack s = template.create();
                    if (s.is(item)) total += s.getCount();
                }
            }
        }
        return total;
    }

    @Override
    public InteractionResult useOn(UseOnContext context) {
        if (context.getHand() != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        // Baustab in der Haupthand + Blaupause in der Nebenhand = Blaupausen-Baumodus.
        if (context.getPlayer() != null && context.getPlayer().getOffhandItem().getItem() instanceof BlueprintItem) {
            return com.simplebuilding.blueprint.BlueprintBuilder.useWand(context);
        }
        // Baustab in der Haupthand + Oktant mit Auswahl in der Nebenhand = Figur fuellen bzw. Dach.
        if (context.getPlayer() != null && com.simplebuilding.blueprint.ShapeFill.hasSelection(context.getPlayer().getOffhandItem())) {
            return com.simplebuilding.blueprint.ShapeFill.useWand(context);
        }
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
        var hitRel = context.getClickLocation().subtract(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());
        Plan plan = Plan.forClick(world, player, wandStack, clickedPos, clickedFace, hitRel,
                getConfiguredRadius(nbt, (this.maxDiameter - 1) / 2), nbt.getIntOr("SettingsAxis", 0));
        if (plan.steps() == 0) return InteractionResult.FAIL;
        nbt.putBoolean("Active", true);
        nbt.putInt("HungerCount", 0); // neuer Bauvorgang: Freibetrag von vorn (WandHunger)
        nbt.putInt("CurrentRadius", 0);
        nbt.putInt("Timer", 0);
        nbt.putInt("OriginX", clickedPos.getX());
        nbt.putInt("OriginY", clickedPos.getY());
        nbt.putInt("OriginZ", clickedPos.getZ());
        nbt.putInt("Face", clickedFace.ordinal());
        putBlock(nbt, BUILD_BLOCK_KEY, buildBlock);

        var hitPos = context.getClickLocation().subtract(clickedPos.getX(), clickedPos.getY(), clickedPos.getZ());
        nbt.putFloat("HitX", (float) hitPos.x); nbt.putFloat("HitY", (float) hitPos.y); nbt.putFloat("HitZ", (float) hitPos.z);
        plan.writeTo(nbt);
        setNbt(wandStack, nbt);
        WandUndo.begin(player, world);

        return InteractionResult.CONSUME;
    }

    /**
     * Rechtsklick in die Luft (Haupthand): Schleichen = die letzte Bau-Aktion rueckgaengig machen
     * ({@link WandUndo}); ohne Schleichen mit der Verzauberung <b>Bruecke</b> = eine Bruecke vom Block
     * unter den Fuessen aus geradeaus in Blickrichtung. Sonst PASS (die Nebenhand ist dran).
     */
    @Override
    public InteractionResult use(Level world, Player player, InteractionHand hand) {
        if (hand != InteractionHand.MAIN_HAND) return InteractionResult.PASS;
        ItemStack wandStack = player.getItemInHand(hand);
        if (player.isShiftKeyDown()) {
            if (world.isClientSide()) return InteractionResult.SUCCESS;
            // Was gerade noch baut, gehoert zur letzten Aktion: erst anhalten, dann zuruecknehmen.
            CompoundTag running = getOrInitNbt(wandStack);
            if (getBlockBoolean(running)) { running.putBoolean("Active", false); setNbt(wandStack, running); }
            com.simplebuilding.blueprint.BlueprintBuilder.cancel(player);
            if (WandUndo.undo(player, world) < 0) {
                player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("simplebuilding.wand.undo.nothing")
                        .withStyle(net.minecraft.ChatFormatting.GRAY));
            }
            return InteractionResult.SUCCESS;
        }
        if (!hasEnchantment(wandStack, world, ModEnchantments.BRIDGE)) return InteractionResult.PASS;
        if (world.isClientSide()) return InteractionResult.SUCCESS;

        boolean hasMasterBuilder = hasEnchantment(wandStack, world, ModEnchantments.MASTER_BUILDER);
        MaterialResult preview = findFirstBuildingBlock(player, wandStack, hasMasterBuilder);
        // Jede Absage sagt in der Aktionsleiste, warum: frueher blieb ein Klick auf flachem Boden
        // (die Testzentrale ist ueberall flach) stumm, und die Bruecke wirkte kaputt.
        if (preview == null && !player.getAbilities().instabuild) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("simplebuilding.wand.bridge.no_material").withStyle(net.minecraft.ChatFormatting.RED));
            return InteractionResult.FAIL;
        }
        CompoundTag nbt = getOrInitNbt(wandStack);
        int bridgeRadius = getConfiguredRadius(nbt, (this.maxDiameter - 1) / 2);
        Plan plan = Plan.forBridge(world, player, bridgeRadius);
        if (plan == null) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("simplebuilding.wand.bridge.no_ground").withStyle(net.minecraft.ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }
        if (plan.steps() == 0) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("simplebuilding.wand.bridge.no_gap", lineLength(bridgeRadius)).withStyle(net.minecraft.ChatFormatting.YELLOW));
            return InteractionResult.FAIL;
        }

        Block buildBlock = preview != null ? preview.stateToPlace.getBlock() : Blocks.AIR;
        nbt.putBoolean("Active", true);
        nbt.putInt("HungerCount", 0); // die Bruecke ist ein eigener Bauvorgang: Freibetrag von vorn (WandHunger)
        nbt.putInt("CurrentRadius", 0);
        nbt.putInt("Timer", 0);
        nbt.putInt("OriginX", plan.origin.getX());
        nbt.putInt("OriginY", plan.origin.getY());
        nbt.putInt("OriginZ", plan.origin.getZ());
        nbt.putInt("Face", plan.face.ordinal());
        putBlock(nbt, BUILD_BLOCK_KEY, buildBlock);
        nbt.putFloat("HitX", (float) plan.hitRel.x); nbt.putFloat("HitY", (float) plan.hitRel.y); nbt.putFloat("HitZ", (float) plan.hitRel.z);
        plan.writeTo(nbt);
        setNbt(wandStack, nbt);
        WandUndo.begin(player, world);
        return InteractionResult.SUCCESS;
    }

    @Override
    public void inventoryTick(ItemStack stack, ServerLevel world, Entity entity, EquipmentSlot slot) {
        if (!(entity instanceof ServerPlayer player)) return;
        // Grosse Blaupausen bauen ueber mehrere Ticks weiter (BlueprintBuilder-Auftrag).
        com.simplebuilding.blueprint.BlueprintBuilder.tick(world, player, stack, slot == EquipmentSlot.MAINHAND);
        CompoundTag nbt = getOrInitNbt(stack);
        if (!getBlockBoolean(nbt)) return;

        if (slot != EquipmentSlot.MAINHAND && slot != EquipmentSlot.OFFHAND) { nbt.putBoolean("Active", false); setNbt(stack, nbt); return; }
        // A build that was running when the world was saved by an older version: its block was
        // stored as a numeric registry id, which a Minecraft update shifts (26.3 added blocks).
        // Rather than build on with whatever block now has that number, stop; one click resumes.
        if (hasLegacyRawBlockIds(nbt)) {
            nbt.putBoolean("Active", false); nbt.remove(LEGACY_BUILD_BLOCK_KEY); nbt.remove(LEGACY_COVER_BLOCK_KEY);
            setNbt(stack, nbt); return;
        }

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

        Block targetBlock = readBlock(nbt, BUILD_BLOCK_KEY);

        // Wenn kein Color Palette, brauchen wir einen festen Block
        if (!hasColorPalette && targetBlock == Blocks.AIR) {
             MaterialResult res = findFirstBuildingBlock(player, stack, hasMasterBuilder);
             if (res != null) targetBlock = res.stateToPlace.getBlock();
             else { nbt.putBoolean("Active", false); setNbt(stack, nbt); return; }
        }

        // Settings
        int axisMode = nbt.getIntOr("SettingsAxis", 0);

        // Positionen berechnen: Flaeche, Abdeckung, Linie oder Bruecke (beim Klick festgelegt)
        Vec3 hitRel = new Vec3(nbt.getFloatOr("HitX", 0.5F), nbt.getFloatOr("HitY", 0.5F), nbt.getFloatOr("HitZ", 0.5F));
        Plan plan = Plan.read(nbt, originPos, face, hitRel, userRadius, axisMode);
        List<BlockPos> stepPositions = plan.step(world, currentRadius);
        BlockState clicked = plan.clickedState(world);

        for (BlockPos rawPos : stepPositions) {
            if (!world.getBlockState(rawPos).canBeReplaced()) continue;

            // Finde Material: mit Color Palette den Eintrag, den die Vorschau an dieser Stelle zeigt
            // (paletteIndex ueber die jetzt noch vorhandenen Stapel), sonst den Block vom Klick.
            Block want = targetBlock;
            if (hasColorPalette) {
                List<ItemStack> palette = findAllBuildingBlocks(player, stack, hasMasterBuilder);
                want = palette.isEmpty() ? null : ((BlockItem) palette.get(paletteIndex(rawPos, palette.size())).getItem()).getBlock();
            }
            MaterialResult material = want == null ? null : findSpecificMaterial(player, stack, want, hasMasterBuilder);

            if (material == null && !player.getAbilities().instabuild) {
                nbt.putBoolean("Active", false); setNbt(stack, nbt); return;
            }

            ItemStack placeItem = material != null ? material.item : new ItemStack(want != null ? want : Blocks.STONE);
            // Wie ein Spieler: ausgerichtet, verbunden, mit Komponenten; wo ein Spieler nicht setzen
            // koennte (kein Halt, Entity im Weg), bleibt die Stelle frei und kostet nichts.
            BlockState stateToPlace = WandPlacement.stateFor(world, player, placeItem, rawPos, plan.placeFace, plan.hitRel, clicked);
            if (stateToPlace == null) continue;

            if (world.setBlock(rawPos, stateToPlace, 3)) {
                WandPlacement.afterPlace(world, player, rawPos, stateToPlace, placeItem);
                WandUndo.record(player, world, rawPos, stateToPlace, placeItem.getItem(),
                        !player.getAbilities().instabuild && material != null ? 1 : 0);
                SoundType soundGroup = stateToPlace.getSoundType();
                world.playSound(null, rawPos, soundGroup.getPlaceSound(), SoundSource.BLOCKS, (soundGroup.getVolume() + 1.0F) / 2.0F, soundGroup.getPitch() * 0.8F);
                if (!player.getAbilities().instabuild && material != null) {
                    material.consume();
                    // Billed to the slot the wand is ticking in: it builds from the off hand too,
                    // and naming MAINHAND here made a break in the off hand take the main hand
                    // item's attribute modifiers with it (LivingEntity#onEquippedItemBroken).
                    // EXPERIMENTELL: Bloecke ueber dem Freibetrag des Klicks kosten Erschoepfung (WandHunger).
                    int hungerCount = nbt.getIntOr("HungerCount", 0) + 1;
                    nbt.putInt("HungerCount", hungerCount);
                    com.simplebuilding.util.WandHunger.exhaust(player, this, hungerCount);
                    stack.hurtAndBreak(1, player, slot);
                }
            }
        }

        if (currentRadius < plan.steps() - 1) {
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

    /**
     * Was ein Klick baut: Form, Ursprung und Schritte (je Schritt ein Ring bzw. ein Stueck Linie).
     * Der Server legt ihn beim Klick fest und liest ihn je Schritt aus dem NBT zurueck; die Vorschau
     * rechnet ihn aus demselben Klick. Beide sehen dieselben Stellen.
     *
     * <ul>
     *   <li><b>Flaeche</b> (Standard): quadratisch vor der Klickseite, Ring fuer Ring.</li>
     *   <li><b>Abdeckung</b>: dieselbe Flaeche, aber nur Stellen, hinter denen (gegen die Klickseite)
     *       ein Block derselben Sorte wie der angeklickte steht, und die mit dem Mittelfeld ueber
     *       solche Stellen zusammenhaengen (4er-Nachbarschaft). Die Achsen-Einstellung gilt hier nicht.</li>
     *   <li><b>Linie</b> (Linear + Schleichen): von der Klickseite gerade weg, {@link #lineLength} lang,
     *       endet vor dem ersten belegten Block.</li>
     *   <li><b>Bruecke</b>: vom Block unter den Fuessen waagerecht in Blickrichtung, gleiche Laenge,
     *       endet vor dem ersten belegten Block.</li>
     * </ul>
     * Linie und Bruecke werden in so vielen Schritten gebaut wie die Flaeche Ringe haette.
     */
    static final class Plan {
        final int mode;
        final BlockPos origin;
        final Direction face;
        /** Klickseite fuer den Platzier-Kontext (Ausrichtung). */
        final Direction placeFace;
        final Vec3 hitRel;
        final int radius;
        final int axis;
        final int length;
        final Block coverBlock;

        private Plan(int mode, BlockPos origin, Direction face, Direction placeFace, Vec3 hitRel, int radius, int axis, int length, Block coverBlock) {
            this.mode = mode;
            this.origin = origin;
            this.face = face;
            this.placeFace = placeFace;
            this.hitRel = hitRel;
            this.radius = radius;
            this.axis = axis;
            this.length = length;
            this.coverBlock = coverBlock;
        }

        static Plan forClick(Level level, Player player, ItemStack wand, BlockPos clicked, Direction face, Vec3 hitRel, int radius, int axis) {
            if (hasEnchantment(wand, level, ModEnchantments.COVER)) {
                return new Plan(MODE_COVER, clicked, face, face, hitRel, radius, 0, 0, level.getBlockState(clicked).getBlock());
            }
            if (player.isShiftKeyDown() && hasEnchantment(wand, level, ModEnchantments.LINEAR)) {
                return new Plan(MODE_LINE, clicked, face, face, hitRel, radius, 0, freeRun(level, clicked, face, lineLength(radius)), Blocks.AIR);
            }
            return new Plan(MODE_SQUARE, clicked, face, face, hitRel, radius, axis, 0, Blocks.AIR);
        }

        /**
         * Bruecke ab dem Block unter den Fuessen; {@code null}, wenn dort nichts Festes steht. Steht vor den
         * Fuessen noch Boden derselben Hoehe, beginnt die Bruecke an dessen Kante (hoechstens
         * {@link #lineLength} weit gesucht): wer ein paar Schritte vor dem Abgrund klickt, bekommt trotzdem
         * seine Bruecke. Frueher hiess "Boden direkt voraus" Laenge 0 - der Klick tat stumm nichts.
         */
        static Plan forBridge(Level level, Player player, int radius) {
            BlockPos start = player.getOnPos();
            if (level.getBlockState(start).canBeReplaced()) return null;
            Direction facing = player.getDirection();
            int max = lineLength(radius);
            int solid = 0;
            while (solid < max && !level.getBlockState(start.relative(facing, solid + 1)).canBeReplaced()) solid++;
            BlockPos edge = start.relative(facing, solid);
            int length = solid >= max ? 0 : freeRun(level, edge, facing, max);
            // Als setzte man jeden Block an die Stirnseite des vorigen, untere Haelfte (Stufen unten).
            Vec3 hit = new Vec3(0.5 + facing.getStepX() * 0.5, 0.25, 0.5 + facing.getStepZ() * 0.5);
            return new Plan(MODE_BRIDGE, edge, facing, facing, hit, radius, 0, length, Blocks.AIR);
        }

        /** Wie viele Stellen ab {@code from} in Richtung {@code dir} frei sind, hoechstens {@code max}. */
        private static int freeRun(Level level, BlockPos from, Direction dir, int max) {
            int n = 0;
            while (n < max && level.getBlockState(from.relative(dir, n + 1)).canBeReplaced()) n++;
            return n;
        }

        static Plan read(CompoundTag nbt, BlockPos origin, Direction face, Vec3 hitRel, int radius, int axis) {
            int mode = nbt.getIntOr("Mode", MODE_SQUARE);
            Block cover = readBlock(nbt, COVER_BLOCK_KEY);
            return new Plan(mode, origin, face, face, hitRel, radius, mode == MODE_SQUARE ? axis : 0, nbt.getIntOr("Length", 0), cover);
        }

        void writeTo(CompoundTag nbt) {
            nbt.putInt("Mode", mode);
            nbt.putInt("Length", length);
            putBlock(nbt, COVER_BLOCK_KEY, coverBlock);
        }

        /** Der angeklickte Block (fuer die Ausrichtungs-Uebernahme); bei der Bruecke keiner. */
        BlockState clickedState(Level level) {
            return mode == MODE_BRIDGE ? null : level.getBlockState(origin);
        }

        int steps() {
            int rings = Math.max(0, radius) + 1;
            if (mode == MODE_LINE || mode == MODE_BRIDGE) return Math.min(rings, length);
            return rings;
        }

        List<BlockPos> step(Level level, int k) {
            switch (mode) {
                case MODE_LINE, MODE_BRIDGE -> {
                    int steps = steps();
                    List<BlockPos> out = new ArrayList<>();
                    if (steps == 0) return out;
                    int chunk = (length + steps - 1) / steps;
                    for (int i = k * chunk; i < Math.min(length, (k + 1) * chunk); i++) out.add(origin.relative(face, i + 1));
                    return out;
                }
                case MODE_COVER -> {
                    Set<BlockPos> region = coverRegion(level);
                    List<BlockPos> out = new ArrayList<>();
                    for (BlockPos pos : calculatePositions(origin, face, k, 0)) if (region.contains(pos)) out.add(pos);
                    return out;
                }
                default -> {
                    return calculatePositions(origin, face, k, axis);
                }
            }
        }

        List<BlockPos> all(Level level) {
            List<BlockPos> out = new ArrayList<>();
            for (int k = 0; k < steps(); k++) out.addAll(step(level, k));
            return out;
        }

        /** Flutfuellung in der Ebene vor der Klickseite ueber Stellen, hinter denen ein Block der Klick-Sorte steht. */
        private Set<BlockPos> coverRegion(Level level) {
            Set<BlockPos> region = new HashSet<>();
            BlockPos start = origin.relative(face);
            int r = Math.max(0, radius);
            List<Direction> inPlane = new ArrayList<>();
            for (Direction d : Direction.values()) if (d.getAxis() != face.getAxis()) inPlane.add(d);
            Deque<BlockPos> queue = new ArrayDeque<>();
            region.add(start);
            queue.add(start);
            while (!queue.isEmpty()) {
                BlockPos p = queue.poll();
                for (Direction d : inPlane) {
                    BlockPos q = p.relative(d);
                    BlockPos off = q.subtract(start);
                    if (Math.abs(off.getX()) > r || Math.abs(off.getY()) > r || Math.abs(off.getZ()) > r) continue;
                    if (region.contains(q)) continue;
                    if (level.getBlockState(q.relative(face.getOpposite())).getBlock() != coverBlock) continue;
                    region.add(q);
                    queue.add(q);
                }
            }
            return region;
        }
    }

    private static void removeOneFromBundle(ItemStack bundle, int indexToRemove) {
        BundleContents contents = bundle.get(DataComponents.BUNDLE_CONTENTS);
        if (contents == null) return;
        List<ItemStack> newItems = new ArrayList<>();
        int i = 0;
        for (ItemStackTemplate template : contents.items()) {
            ItemStack s = template.create();
            if (i == indexToRemove) { ItemStack copy = s.copy(); copy.shrink(1); if (!copy.isEmpty()) newItems.add(copy); }
            else { newItems.add(s.copy()); }
            i++;
        }
        bundle.set(DataComponents.BUNDLE_CONTENTS, new BundleContents(newItems.stream().map(ItemStackTemplate::fromNonEmptyStack).toList()));
    }

    private boolean getBlockBoolean(CompoundTag nbt) { if (!nbt.contains("Active")) return false; return nbt.getBooleanOr("Active", false); }
    private int getBlockInt(CompoundTag nbt, String key) { if (!nbt.contains(key)) return 0; return nbt.getIntOr(key, 0); }
    /**
     * The running build's block and the cover mode's block, by registry name. Until 2026-09 they
     * were numeric registry ids ({@code BuildBlockRawId}/{@code CoverBlockRawId}), which are not
     * stable across Minecraft versions; see {@link #hasLegacyRawBlockIds}.
     */
    static final String BUILD_BLOCK_KEY = "BuildBlock";
    static final String COVER_BLOCK_KEY = "CoverBlock";
    static final String LEGACY_BUILD_BLOCK_KEY = "BuildBlockRawId";
    static final String LEGACY_COVER_BLOCK_KEY = "CoverBlockRawId";

    static void putBlock(CompoundTag nbt, String key, Block block) {
        nbt.putString(key, BuiltInRegistries.BLOCK.getKey(block).toString());
    }

    /** The named block, or air when absent or unknown. */
    static Block readBlock(CompoundTag nbt, String key) {
        return nbt.getString(key)
                .map(net.minecraft.resources.Identifier::tryParse)
                .flatMap(BuiltInRegistries.BLOCK::getOptional)
                .orElse(Blocks.AIR);
    }

    /** A build state written before the switch to names: numeric ids only. */
    static boolean hasLegacyRawBlockIds(CompoundTag nbt) {
        return (nbt.contains(LEGACY_BUILD_BLOCK_KEY) && !nbt.contains(BUILD_BLOCK_KEY))
                || (nbt.contains(LEGACY_COVER_BLOCK_KEY) && !nbt.contains(COVER_BLOCK_KEY));
    }

    private CompoundTag getOrInitNbt(ItemStack stack) { CustomData component = stack.get(DataComponents.CUSTOM_DATA); return component != null ? component.copyTag() : new CompoundTag(); }
    private void setNbt(ItemStack stack, CompoundTag nbt) { stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt)); }
}