package com.simplebuilding.entity;

import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.network.protocol.game.ClientboundBlockUpdatePacket;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.util.ProblemReporter;
import net.minecraft.world.entity.EntityType;
import net.minecraft.world.entity.MoverType;
import net.minecraft.world.entity.item.FallingBlockEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.context.DirectionalPlaceContext;
import net.minecraft.world.level.Level;
import net.minecraft.world.level.block.Block;
import net.minecraft.world.level.block.Blocks;
import net.minecraft.world.level.block.Fallable;
import net.minecraft.world.level.block.FallingBlock;
import net.minecraft.world.level.block.state.BlockState;
import net.minecraft.world.level.block.state.properties.BlockStateProperties;
import net.minecraft.world.level.gamerules.GameRules;
import net.minecraft.world.level.material.Fluids;
import net.minecraft.world.level.storage.TagValueInput;

/**
 * Ein Block, der nach oben fällt – das Spiegelbild von Vanillas fallendem Sand.
 *
 * <p>Warum eine eigene Entity und nicht der bisherige Weg: {@code LevitatingBlock}
 * hat den Block früher alle zwei Ticks eine Position weiter nach oben <em>gesetzt</em>.
 * Das ist ein Sprung pro Zehntelsekunde, keine Bewegung. Sand fällt dagegen als
 * Entity mit Beschleunigung; damit sich beides gleich anfühlt, muss das Aufsteigen
 * denselben Weg nehmen.
 *
 * <p>Geerbt wird von {@link FallingBlockEntity}, weil damit Rendering, Speichern,
 * Blockdaten und die Landeregeln schon da sind. Zwei Dinge muss diese Klasse
 * trotzdem selbst machen:
 *
 * <ul>
 *   <li><b>Die Schwerkraft umdrehen</b> – {@link #getDefaultGravity()} liefert einen
 *       negativen Wert. {@code Entity.applyGravity()} addiert {@code (0, -g, 0)} ohne
 *       Betrag oder Begrenzung, also steigt der Block mit derselben Kurve, mit der
 *       Sand fällt: Weg pro Tick {@code v = 0,98·v + 0,04}, Grenzwert
     *       2 Blöcke pro Tick (die gespeicherte Geschwindigkeit landet bei 1,96,
     *       weil der Luftwiderstand erst nach der Bewegung angewandt wird).</li>
 *   <li><b>Das Landen neu schreiben</b> – Vanillas {@code tick()} hängt das
 *       Zurückverwandeln an {@code onGround()}, und dieses Feld wird ausschließlich
 *       aus {@code verticalCollisionBelow} gesetzt, was wiederum {@code movement.y < 0}
 *       verlangt. Ein aufsteigender Block erfüllt das nie und würde ewig weiterfliegen.
 *       Deshalb ist {@link #tick()} hier gespiegelt statt geerbt.</li>
 * </ul>
 */
public class LevitatingBlockEntity extends FallingBlockEntity {

    /** Spiegelbild der +0,04 Fallbeschleunigung; applyGravity() addiert (0, -g, 0). */
    private static final double RISE_GRAVITY = -0.04D;

    /**
     * Vanillas Luftwiderstand, bewusst als Zahl statt über {@code getAirDrag()}:
     * diese Methode gibt es nur auf der 26.2-Linie. Auf 1.21.11 steht die 0,98
     * als Literal in {@code FallingBlockEntity}. So bleiben beide Kopien dieser
     * Datei wortgleich.
     */
    private static final double AIR_DRAG = 0.98D;

    public LevitatingBlockEntity(EntityType<? extends FallingBlockEntity> type, Level level) {
        super(type, level);
    }

    @Override
    protected double getDefaultGravity() {
        return RISE_GRAVITY;
    }

    /**
     * Gegenstück zu {@code FallingBlockEntity.fall(...)}.
     *
     * <p>Das Feld {@code blockState} ist privat und hat keinen Setter; der einzige
     * von einer Unterklasse erreichbare Schreiber ist
     * {@code readAdditionalSaveData(ValueInput)}. Der ist ein reiner Feldsetzer und
     * setzt nebenbei genau die Startwerte, die wir wollen (Zeit 0, dropItem true).
     * Deshalb läuft er zuerst, vor allem, was danach gesetzt wird.
     */
    public static LevitatingBlockEntity rise(Level level, BlockPos pos, BlockState state) {
        LevitatingBlockEntity entity = new LevitatingBlockEntity(ModEntities.LEVITATING_BLOCK, level);

        BlockState spawnState = state.hasProperty(BlockStateProperties.WATERLOGGED)
                ? state.setValue(BlockStateProperties.WATERLOGGED, Boolean.FALSE)
                : state;

        CompoundTag tag = new CompoundTag();
        tag.store("BlockState", BlockState.CODEC, spawnState);
        entity.readAdditionalSaveData(
                TagValueInput.create(ProblemReporter.DISCARDING, level.registryAccess(), tag));

        entity.blocksBuilding = true;
        entity.setPos(pos.getX() + 0.5D, pos.getY(), pos.getZ() + 0.5D);
        entity.setDeltaMovement(0.0D, 0.0D, 0.0D);
        entity.xo = entity.getX();
        entity.yo = entity.getY();
        entity.zo = entity.getZ();
        entity.setStartPos(entity.blockPosition());
        // Wie fall(): der Quellblock weicht seinem Fluidzustand, damit unter Wasser
        // kein Loch zurueckbleibt.
        level.setBlock(pos, state.getFluidState().createLegacyBlock(), 3);
        level.addFreshEntity(entity);
        return entity;
    }

    @Override
    public void tick() {
        BlockState state = this.getBlockState();
        if (state.isAir()) {
            this.discard();
            return;
        }
        Block block = state.getBlock();
        this.time++;
        this.applyGravity();
        this.move(MoverType.SELF, this.getDeltaMovement());
        this.applyEffectsFromBlocks();
        this.handlePortal();

        if (this.level() instanceof ServerLevel serverLevel
                && (this.isAlive() || this.forceTickAfterTeleportToDuplicate)) {

            BlockPos blockPos = this.blockPosition();

            // Gegenstück zu onGround(): eine Decke ist eine senkrechte Kollision,
            // die NICHT nach unten geht.
            boolean ceilingHit = this.verticalCollision && !this.verticalCollisionBelow;

            if (ceilingHit) {
                BlockState currentState = this.level().getBlockState(blockPos);
                this.setDeltaMovement(this.getDeltaMovement().multiply(0.7D, -0.5D, 0.7D));

                if (!currentState.is(Blocks.MOVING_PISTON)) {
                    boolean mayReplace = currentState.canBeReplaced(new DirectionalPlaceContext(
                            this.level(), blockPos, Direction.UP, ItemStack.EMPTY, Direction.DOWN));

                    // Gespiegelt: Vanilla fragt isFree(unten), wir fragen isFree(oben).
                    boolean wouldContinueRising =
                            FallingBlock.isFree(this.level().getBlockState(blockPos.above()));
                    boolean wouldSurvive = state.canSurvive(this.level(), blockPos) && !wouldContinueRising;

                    if (mayReplace && wouldSurvive) {
                        if (state.hasProperty(BlockStateProperties.WATERLOGGED)
                                && this.level().getFluidState(blockPos).is(Fluids.WATER)) {
                            state = state.setValue(BlockStateProperties.WATERLOGGED, Boolean.TRUE);
                        }
                        if (this.level().setBlock(blockPos, state, 3)) {
                            serverLevel.getChunkSource().chunkMap.sendToTrackingPlayers(this,
                                    new ClientboundBlockUpdatePacket(blockPos,
                                            this.level().getBlockState(blockPos)));
                            this.discard();
                            if (block instanceof Fallable fallable) {
                                fallable.onLand(this.level(), blockPos, state, currentState, this);
                            }
                        } else if (this.dropItem
                                && serverLevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
                            this.discard();
                            this.callOnBrokenAfterFall(block, blockPos);
                            this.spawnAtLocation(serverLevel, block);
                        }
                    } else {
                        this.discard();
                        if (this.dropItem
                                && serverLevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
                            this.callOnBrokenAfterFall(block, blockPos);
                            this.spawnAtLocation(serverLevel, block);
                        }
                    }
                }
            } else if (blockPos.getY() > this.level().getMaxY()
                    || blockPos.getY() < this.level().getMinY()
                    || this.time > 600) {
                // Am Baulimit zerbricht der Block und fällt als Gegenstand – das
                // Gegenstück dazu, dass Sand in der Leere verloren geht.
                //
                // Vanilla verlangt hier zusätzlich time > 100. Diese Schranke ist
                // bewusst weg, damit der Block AM Limit zerbricht und nicht erst
                // weit darüber. Von y=64 aus fällt sie nicht auf: bis zum Limit
                // bei y=319 vergehen rund 176 Ticks. Ein Block aber, der schon
                // dicht unter dem Limit startet, würde damit erst bei Tick 101
                // zerbrechen – und bis dahin ist er rund 115 Blöcke weit oberhalb
                // des Limits weitergeflogen.
                //
                // Die minY-Klausel ist nur ein Netz, die time > 600 Vanillas eigene
                // Notbremse gegen Ausreißer.
                if (this.dropItem && serverLevel.getGameRules().get(GameRules.ENTITY_DROPS)) {
                    this.spawnAtLocation(serverLevel, block);
                }
                this.discard();
            }
        }

        this.setDeltaMovement(this.getDeltaMovement().scale(AIR_DRAG));
    }
}
