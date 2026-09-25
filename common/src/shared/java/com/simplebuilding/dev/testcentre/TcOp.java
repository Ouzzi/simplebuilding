package com.simplebuilding.dev.testcentre;

import java.util.List;
import net.minecraft.core.BlockPos;
import net.minecraft.core.Direction;
import net.minecraft.network.chat.Component;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.block.state.BlockState;

/**
 * Ein Bauschritt der Testzentrale, in Koordinaten relativ zu seinem Abschnitt (y = 0 ist die Hoehe,
 * auf der man steht; der Boden liegt bei y = -1).
 *
 * <p>Die Zentrale wird zuerst vollstaendig als Liste solcher Schritte geplant ({@link TestCentreLayout})
 * und erst danach in eine Welt geschrieben ({@link TestCentreBuilder}). Der Abdeckungstest liest nur
 * die Planung - er braucht dafuer keine Welt und sieht genau das, was gebaut wuerde.
 */
public sealed interface TcOp {

    /** Die Position, an der der Schritt wirkt (lokal oder, nach {@link #moved}, absolut). */
    BlockPos pos();

    /** Derselbe Schritt, um {@code offset} verschoben. */
    TcOp moved(BlockPos offset);

    /** Setzt einen Block. */
    record Place(BlockPos pos, BlockState state) implements TcOp {
        @Override
        public TcOp moved(BlockPos offset) {
            return new Place(pos.offset(offset), state);
        }
    }

    /** Ein Rahmen im Luftblock {@code pos}, der nach {@code facing} zeigt (haengt also am Block dahinter). */
    record Frame(BlockPos pos, Direction facing, ItemStack stack) implements TcOp {
        @Override
        public TcOp moved(BlockPos offset) {
            return new Frame(pos.offset(offset), facing, stack);
        }
    }

    /**
     * Ein Ruestungsstaender mit Armen, ohne Schwerkraft. {@code gear} in der Reihenfolge Kopf, Brust,
     * Beine, Fuesse, Haupthand, Nebenhand; leere Stapel lassen den Platz frei.
     */
    record Stand(BlockPos pos, float yaw, List<ItemStack> gear, Component name) implements TcOp {
        @Override
        public TcOp moved(BlockPos offset) {
            return new Stand(pos.offset(offset), yaw, gear, name);
        }
    }

    /** Ein Wandschild, das nach {@code facing} zeigt; bis zu vier Zeilen. */
    record Sign(BlockPos pos, Direction facing, List<Component> lines) implements TcOp {
        @Override
        public TcOp moved(BlockPos offset) {
            return new Sign(pos.offset(offset), facing, lines);
        }
    }

    /** Fuellt den Behaelter an {@code pos} (der Block muss vorher gesetzt sein), Slot fuer Slot. */
    record Fill(BlockPos pos, List<ItemStack> contents) implements TcOp {
        @Override
        public TcOp moved(BlockPos offset) {
            return new Fill(pos.offset(offset), contents);
        }
    }

    /**
     * Ein Befehlsblock, der nach {@code facing} zeigt, mit Knopf davor und einem Schild darueber.
     * Der Befehl kann absolute Koordinaten enthalten; die kennt die Planung erst mit dem Ursprung.
     */
    record Command(BlockPos pos, Direction facing, String command, List<Component> label) implements TcOp {
        @Override
        public TcOp moved(BlockPos offset) {
            return new Command(pos.offset(offset), facing, command, label);
        }
    }
}
