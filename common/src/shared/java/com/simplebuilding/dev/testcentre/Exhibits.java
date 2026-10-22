package com.simplebuilding.dev.testcentre;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.world.item.BlockItem;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.EmptyBlockGetter;
import net.minecraft.world.level.block.AbstractCauldronBlock;
import net.minecraft.world.level.block.AbstractChestBlock;
import net.minecraft.world.level.block.AbstractSkullBlock;
import net.minecraft.world.level.block.AnvilBlock;
import net.minecraft.world.level.block.BannerBlock;
import net.minecraft.world.level.block.BasePressurePlateBlock;
import net.minecraft.world.level.block.BaseTorchBlock;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.BrewingStandBlock;
import net.minecraft.world.level.block.ButtonBlock;
import net.minecraft.world.level.block.CampfireBlock;
import net.minecraft.world.level.block.CandleBlock;
import net.minecraft.world.level.block.CarpetBlock;
import net.minecraft.world.level.block.DoorBlock;
import net.minecraft.world.level.block.EndRodBlock;
import net.minecraft.world.level.block.EntityBlock;
import net.minecraft.world.level.block.FenceBlock;
import net.minecraft.world.level.block.FenceGateBlock;
import net.minecraft.world.level.block.FlowerPotBlock;
import net.minecraft.world.level.block.FrostedIceBlock;
import net.minecraft.world.level.block.HopperBlock;
import net.minecraft.world.level.block.IceBlock;
import net.minecraft.world.level.block.IronBarsBlock;
import net.minecraft.world.level.block.LanternBlock;
import net.minecraft.world.level.block.LeavesBlock;
import net.minecraft.world.level.block.LecternBlock;
import net.minecraft.world.level.block.LightningRodBlock;
import net.minecraft.world.level.block.ShulkerBoxBlock;
import net.minecraft.world.level.block.SlabBlock;
import net.minecraft.world.level.block.StairBlock;
import net.minecraft.world.level.block.StandingSignBlock;
import net.minecraft.world.level.block.TntBlock;
import net.minecraft.world.level.block.TrapDoorBlock;
import net.minecraft.world.level.block.WallBlock;
import net.minecraft.world.level.block.WeatheringCopper;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.AttachFace;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.block.state.properties.BooleanProperty;
import net.minecraft.world.level.block.state.properties.DoubleBlockHalf;
import net.minecraft.world.level.block.state.properties.Property;

/**
 * Wie ein Stapel in der Testzentrale gezeigt wird (TZ22-2, Besitzer 22.10.: "es leckt noch etwas, dadurch, dass es so
 * viele Item-Frames sind"): Was sich setzen laesst, steht als Block - in der Wand statt eines Rahmens
 * ({@link #wallState}) oder auf dem Boden einer Ausstellung ({@link #floorStates}); Rahmen bleiben fuer das
 * Nicht-Platzierbare (Eimer, Werkzeuge, verzauberte oder gefaerbte Stapel).
 *
 * <p>Gesetzt wird nur, was ohne Umgebung ruhig stehen bleibt: keine Pads mit Wirkung ({@link TestCentreLayout#frameOnly}),
 * keine Signalquellen (sie schalteten Befehlsbloecke und Kolben in der Naehe), kein TNT, keine Blitzableiter (die
 * Eisenstab-Station braucht die Blitze), nichts, was schmilzt, waechst oder zerfaellt (zufaellig tickende Bloecke; Laub
 * wird dauerhaft gesetzt, Kupfer darf altern, Kessel duerfen reifen), keine Bloecke, die ohne Decke aufsteigen.
 */
public final class Exhibits {

    private Exhibits() {
    }

    /** Der Block, der statt eines Rahmens in einer Wand stehen kann (volle Bloecke, Treppen, Stufen, Truhen ...), sonst null. */
    public static BlockState wallState(ItemStack stack) {
        BlockState state = base(stack);
        if (state == null) {
            return null;
        }
        Block block = state.getBlock();
        boolean shape = full(state) || block instanceof SlabBlock || block instanceof StairBlock || block instanceof WallBlock
                || block instanceof FenceBlock || block instanceof FenceGateBlock || block instanceof IronBarsBlock
                || block instanceof TrapDoorBlock || block instanceof AbstractChestBlock || block instanceof ShulkerBoxBlock;
        return shape ? state : null;
    }

    /**
     * Was statt eines Rahmens vor einer Wand haengen kann (an der Stelle des Rahmens, am Wandblock dahinter, Front nach
     * Norden): Lichtscheiben, Knoepfe, Leitern und die Wandform von Fackeln, Koepfen, Bannern und Schildern
     * ({@code x_torch} zu {@code x_wall_torch}). Sonst null.
     */
    public static BlockState wallFrontState(ItemStack stack) {
        BlockState state = base(stack);
        if (state == null) {
            return null;
        }
        Block block = state.getBlock();
        if (block instanceof com.simplebuilding.blocks.custom.AbstractLightPaneBlock) {
            return state.setValue(com.simplebuilding.blocks.custom.AbstractLightPaneBlock.FACING, Direction.NORTH);
        }
        if (block instanceof ButtonBlock) {
            return state.setValue(ButtonBlock.FACE, AttachFace.WALL).setValue(ButtonBlock.FACING, Direction.NORTH);
        }
        if (block instanceof net.minecraft.world.level.block.LadderBlock) {
            return with(state, BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        }
        if (block instanceof BaseTorchBlock || block instanceof AbstractSkullBlock || block instanceof BannerBlock
                || block instanceof StandingSignBlock) {
            net.minecraft.resources.Identifier id = TcContext.id(block);
            String path = id.getPath();
            int cut = path.lastIndexOf('_');
            String wallPath = cut < 0 ? "wall_" + path : path.substring(0, cut) + "_wall" + path.substring(cut);
            Block wall = net.minecraft.core.registries.BuiltInRegistries.BLOCK
                    .getOptional(net.minecraft.resources.Identifier.fromNamespaceAndPath(id.getNamespace(), wallPath)).orElse(null);
            if (wall == null || wall.defaultBlockState().isSignalSource()) {
                return null;
            }
            return with(wall.defaultBlockState(), BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        }
        return null;
    }

    /** Ob der Stapel an einer Rahmenwand als Block gezeigt wird ({@link #wallState} oder {@link #wallFrontState}). */
    public static boolean onWall(ItemStack stack) {
        return wallState(stack) != null || wallFrontState(stack) != null;
    }

    /**
     * Die Bloecke, mit denen der Stapel auf dem Boden einer Ausstellung steht (unten zuerst; Tueren zwei hoch), oder eine
     * leere Liste, wenn er in einen Rahmen gehoert. Zusaetzlich zu {@link #wallState}: alles, was nur einen festen Boden
     * braucht (Laternen, Fackeln, Kerzen, Knoepfe und Druckplatten, Teppiche, Koepfe, Achtel, Kessel ...).
     */
    public static List<BlockState> floorStates(ItemStack stack) {
        if (stack.getItem() instanceof BlockItem item && stack.getComponentsPatch().isEmpty() && levitating(item.getBlock())) {
            // Steigt ohne Decke auf; unter Glas bleibt er liegen (wie bisher im Abschnitt "blocks").
            return List.of(item.getBlock().defaultBlockState(), net.minecraft.world.level.block.Blocks.GLASS.defaultBlockState());
        }
        BlockState wall = wallState(stack);
        if (wall != null) {
            return List.of(octets(wall));
        }
        if (!stack.isEmpty() && !(stack.getItem() instanceof BlockItem) && stack.getComponentsPatch().isEmpty()) {
            // Achtel-Items (Simple Octets) sind keine Block-Items; sie setzen die Zelle mit derselben Id.
            BlockState cell = net.minecraft.core.registries.BuiltInRegistries.BLOCK.getOptional(TcContext.id(stack.getItem()))
                    .map(Block::defaultBlockState).orElse(null);
            return cell != null && octet(cell) ? List.of(octets(cell)) : List.of();
        }
        BlockState state = base(stack);
        if (state == null) {
            return List.of();
        }
        Block block = state.getBlock();
        if (block instanceof DoorBlock) {
            return List.of(state.setValue(DoorBlock.HALF, DoubleBlockHalf.LOWER), state.setValue(DoorBlock.HALF, DoubleBlockHalf.UPPER));
        }
        if (block instanceof ButtonBlock) {
            return List.of(state.setValue(ButtonBlock.FACE, AttachFace.FLOOR));
        }
        if (block instanceof com.simplebuilding.blocks.custom.AbstractLightPaneBlock) {
            // Lichtscheiben haengen flach am Block hinter sich: auf dem Boden liegen sie nach oben.
            return List.of(state.setValue(com.simplebuilding.blocks.custom.AbstractLightPaneBlock.FACING, Direction.UP));
        }
        if (octet(state) || block instanceof LanternBlock || block instanceof BaseTorchBlock || block instanceof CandleBlock || block instanceof BasePressurePlateBlock || block instanceof CarpetBlock
                || block instanceof AbstractSkullBlock || block instanceof FlowerPotBlock || block instanceof CampfireBlock
                || block instanceof AnvilBlock || block instanceof HopperBlock || block instanceof AbstractCauldronBlock
                || block instanceof BrewingStandBlock || block instanceof LecternBlock || block instanceof EndRodBlock
                || block instanceof BannerBlock || block instanceof StandingSignBlock) {
            return List.of(octets(state));
        }
        return List.of();
    }

    /**
     * Wie ein Block ohne Item gezeigt wird (TZ22-1, Modul-Ausstellung): Achtel-Zellen als kleine Stufe, Bloecke mit Stufen
     * (Eigenschaft {@code stage}, etwa der halbfertige Tiegel) in einigen Stufen nebeneinander, sonst der Grundzustand.
     * Leer fuer Fluessigkeiten und alles, was {@link #base} nicht ruhig stehen laesst.
     */
    public static List<BlockState> blockStates(Block block) {
        if (block instanceof net.minecraft.world.level.block.LiquidBlock) {
            return List.of();
        }
        BlockState state = block.defaultBlockState();
        if (octet(state)) {
            return List.of(octets(state));
        }
        state = safe(block);
        if (state == null) {
            return List.of();
        }
        if (block.getStateDefinition().getProperty("cauldron") instanceof BooleanProperty cauldron) {
            state = state.setValue(cauldron, true);
        }
        if (block.getStateDefinition().getProperty("stage") instanceof net.minecraft.world.level.block.state.properties.IntegerProperty stage) {
            List<Integer> values = stage.getPossibleValues().stream().sorted().toList();
            java.util.LinkedHashSet<Integer> picked = new java.util.LinkedHashSet<>();
            for (int i = 0; i < 4; i++) {
                picked.add(values.get(i * (values.size() - 1) / 3));
            }
            List<BlockState> out = new java.util.ArrayList<>();
            for (int value : picked) {
                out.add(state.setValue(stage, value));
            }
            return out;
        }
        return List.of(state);
    }

    /** Der Grundzustand eines ruhig ausstellbaren Block-Items (Front zum Gang), sonst null. */
    private static BlockState base(ItemStack stack) {
        if (stack.isEmpty() || !(stack.getItem() instanceof BlockItem item) || !stack.getComponentsPatch().isEmpty()) {
            return null;
        }
        return safe(item.getBlock());
    }

    /** Der Grundzustand eines Blocks, der ruhig stehen bleibt (Front zum Gang), sonst null. */
    private static BlockState safe(Block block) {
        String id = TcContext.id(block).toString();
        if (TestCentreLayout.frameOnly(block) || TestCentreLayout.EXCLUDED.containsKey(id) || block instanceof TntBlock
                || block instanceof LightningRodBlock || block instanceof IceBlock || block instanceof FrostedIceBlock
                || levitating(block)) {
            return null;
        }
        try {
            BlockState state = facing(block.defaultBlockState());
            state = with(state, LeavesBlock.PERSISTENT, true);
            if (block instanceof EndRodBlock) {
                state = with(state, BlockStateProperties.FACING, Direction.UP);
            }
            // Zufaellig tickend: Kupfer darf altern, ein Kessel reift oder fuellt sich in sich selbst (Milch zu Kaese).
            if (state.isSignalSource() || state.isRandomlyTicking() && !(block instanceof WeatheringCopper)
                    && !(block instanceof AbstractCauldronBlock)) {
                return null;
            }
            return state;
        } catch (RuntimeException e) {
            return null;
        }
    }

    /** Steigt ohne Decke auf (SimpleBuilding 26.2, Simple Blocks 26.3: per Id, ohne Klassen-Import des Moduls). */
    private static boolean levitating(Block block) {
        return block instanceof com.simplebuilding.blocks.custom.LevitatingBlock || TcContext.id(block).getPath().startsWith("levitating_");
    }

    /** Ein voller Block (Kollision der ganze Wuerfel); was dabei scheitert, gilt nicht als voll. */
    private static boolean full(BlockState state) {
        try {
            return state.isCollisionShapeFullBlock(EmptyBlockGetter.INSTANCE, BlockPos.ZERO);
        } catch (RuntimeException e) {
            return false;
        }
    }

    /** Die Front zum Gang (Norden): waagrechte Blickrichtung, Ausrichtung in sechs Richtungen, Drehung stehender Bloecke. */
    private static BlockState facing(BlockState state) {
        state = with(state, BlockStateProperties.HORIZONTAL_FACING, Direction.NORTH);
        state = with(state, BlockStateProperties.FACING, Direction.NORTH);
        return with(state, BlockStateProperties.ROTATION_16, 8);
    }

    /** Achtel-Zellen (Simple Octets, ohne Klassen-Import: Eigenschaften {@code o<x><y><z>}) haben einen Achtel-Block. */
    private static boolean octet(BlockState state) {
        return state.getBlock().getStateDefinition().getProperty("o000") instanceof BooleanProperty;
    }

    /** Eine Achtel-Zelle zeigt eine kleine Stufe (untere Lage voll, ein Achtel oben) statt eines einzelnen Achtels. */
    private static BlockState octets(BlockState state) {
        if (!octet(state)) {
            return state;
        }
        for (String name : List.of("o000", "o100", "o001", "o101", "o011")) {
            if (state.getBlock().getStateDefinition().getProperty(name) instanceof BooleanProperty cell) {
                state = state.setValue(cell, true);
            }
        }
        return state;
    }

    /** Ob ein Block mit Block-Entity als Behaelter dient (dann ist ein Fuellen kein "Zeigen"). */
    static boolean container(BlockState state) {
        if (!(state.getBlock() instanceof EntityBlock entityBlock)) {
            return false;
        }
        try {
            return entityBlock.newBlockEntity(BlockPos.ZERO, state) instanceof net.minecraft.world.Container;
        } catch (RuntimeException e) {
            return true;
        }
    }

    private static <T extends Comparable<T>> BlockState with(BlockState state, Property<T> property, T value) {
        return state.hasProperty(property) && property.getPossibleValues().contains(value) ? state.setValue(property, value) : state;
    }
}
