package com.simplebuildingtools.networking;

import com.simplebuildingtools.items.custom.BuildingWandItem;
import com.simplebuildingtools.items.custom.OctantItem;
import net.minecraft.core.component.DataComponents;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.item.component.CustomData;

/**
 * Server side of the tool packets (until 07.11.2026 in SimpleBuilding's {@code ModMessageHandlers}): octant manager and mouse
 * wheel, wand settings, blueprint editor and rotation. The server checks everything itself.
 */
public final class ModMessageHandlers {
    private ModMessageHandlers() {
    }

    /**
     * So weit (Bloecke, je Achse) darf eine Oktant-Ecke aus einem Paket hoechstens vom Spieler entfernt
     * liegen: die laengste Kante der groessten Stabstufe (256) plus etwas Luft, damit man an einer Ecke
     * stehend die gegenueberliegende eintippen kann. Weiter weg liegende Ecken verwirft der Server
     * (Audit 2026-09-26 #9).
     */
    public static final int OCTANT_CORNER_RANGE = 320;
    /** Groesster Betrag eines Scroll-Pakets; der Client schickt je Rastung 1. */
    public static final int OCTANT_MAX_SCROLL = 16;

    /** Liegt die Ecke in der Welt des Spielers (Bauhoehe) und in {@link #OCTANT_CORNER_RANGE} um ihn? */
    public static boolean octantCornerInRange(ServerPlayer player, int x, int y, int z) {
        net.minecraft.world.level.Level level = player.level();
        if (y < level.getMinY() || y > level.getMaxY()) {
            return false;
        }
        net.minecraft.core.BlockPos at = player.blockPosition();
        return Math.abs((long) x - at.getX()) <= OCTANT_CORNER_RANGE && Math.abs((long) y - at.getY()) <= OCTANT_CORNER_RANGE
                && Math.abs((long) z - at.getZ()) <= OCTANT_CORNER_RANGE;
    }

    private static <E extends Enum<E>> boolean isEnumName(Class<E> type, String name) {
        for (E value : type.getEnumConstants()) {
            if (value.name().equals(name)) {
                return true;
            }
        }
        return false;
    }

    /**
     * Einstellungen aus dem Oktant-Bildschirm. Der Server uebernimmt nur, was passt: Ecken in Reichweite
     * ({@link #octantCornerInRange}), Form und Reihenfolge nur als bekannter Name, Ausrichtung nur 0-5;
     * alles andere bleibt, wie es am Item steht. Eine verworfene Ecke meldet die Aktionsleiste (der
     * Bildschirm zeigt es zusaetzlich rot an; Nach-Audit 2026-09-27 N16).
     */
    public static void handleOctantConfigure(OctantConfigurePayload payload, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof OctantItem) {
            CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag nbt = nbtComponent.copyTag();
            boolean refused = payload.pos1().filter(p -> !octantCornerInRange(player, p.getX(), p.getY(), p.getZ())).isPresent()
                    | payload.pos2().filter(p -> !octantCornerInRange(player, p.getX(), p.getY(), p.getZ())).isPresent();
            payload.pos1().filter(p -> octantCornerInRange(player, p.getX(), p.getY(), p.getZ()))
                    .ifPresent(p -> nbt.putIntArray("Pos1", new int[]{p.getX(), p.getY(), p.getZ()}));
            payload.pos2().filter(p -> octantCornerInRange(player, p.getX(), p.getY(), p.getZ()))
                    .ifPresent(p -> nbt.putIntArray("Pos2", new int[]{p.getX(), p.getY(), p.getZ()}));
            if (refused) {
                // Keine Bildschirmtexte bei Geraeten (Besitzer 2026-09-28): die Zusammenfassung im Oktant-Bildschirm
                // nennt den verworfenen Eckpunkt, hier nur ein Klang.
                player.level().playSound(null, player.blockPosition(), net.minecraft.sounds.SoundEvents.CHEST_LOCKED,
                        net.minecraft.sounds.SoundSource.PLAYERS, 0.6f, 1.2f);
            }
            if (payload.shapeName() != null && isEnumName(OctantItem.SelectionShape.class, payload.shapeName())) {
                nbt.putString("Shape", payload.shapeName());
            }
            nbt.putBoolean("Locked", payload.locked());
            if (payload.orientationOrdinal() >= 0 && payload.orientationOrdinal() <= 5) {
                nbt.putInt("Orientation", payload.orientationOrdinal());
            }
            nbt.putBoolean("Hollow", payload.hollow());
            nbt.putBoolean("LayerMode", payload.layerMode());
            if (payload.fillOrder() != null && isEnumName(OctantItem.FillOrder.class, payload.fillOrder())) {
                nbt.putString("FillOrder", payload.fillOrder());
            }
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    public static void handleOctantScroll(OctantScrollPayload payload, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (!(stack.getItem() instanceof OctantItem)) {
            return;
        }
        CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
        CompoundTag nbt = nbtComponent.copyTag();
        // A locked octant keeps its selection against the scroll packet too, not only in the
        // client's MouseMixin: a client that sends the packet anyway must not move the corners.
        // Unlocking goes through the manager (handleOctantConfigure), which stays open.
        if (nbt.getBooleanOr("Locked", false)) {
            return;
        }
        boolean changed = false;
        // Ein Paket mit riesigem Betrag liefe ueber (int) oder schoebe Ecken beliebig weit.
        int amount = Math.max(-OCTANT_MAX_SCROLL, Math.min(OCTANT_MAX_SCROLL, payload.amount()));

        if (payload.alt()) {
            String currentShapeName = nbt.getString("Shape").orElse("");
            OctantItem.SelectionShape currentShape = OctantItem.SelectionShape.CUBOID;
            if (!currentShapeName.isEmpty()) {
                try {
                    currentShape = OctantItem.SelectionShape.valueOf(currentShapeName);
                } catch (Exception ignored) {
                }
            }
            OctantItem.SelectionShape[] values = OctantItem.SelectionShape.values();
            int nextIndex = (currentShape.ordinal() + amount) % values.length;
            if (nextIndex < 0) {
                nextIndex += values.length;
            }
            nbt.putString("Shape", values[nextIndex].name());
            changed = true;
        } else {
            net.minecraft.core.Direction direction = player.getDirection();
            if (player.getXRot() < -60) {
                direction = net.minecraft.core.Direction.UP;
            } else if (player.getXRot() > 60) {
                direction = net.minecraft.core.Direction.DOWN;
            }
            int dx = direction.getStepX() * amount;
            int dy = direction.getStepY() * amount;
            int dz = direction.getStepZ() * amount;

            if (payload.control() && nbt.contains("Pos1")) {
                int[] p1 = nbt.getIntArray("Pos1").orElse(new int[0]);
                if (p1.length == 3 && octantCornerInRange(player, p1[0] + dx, p1[1] + dy, p1[2] + dz)) {
                    p1[0] += dx;
                    p1[1] += dy;
                    p1[2] += dz;
                    nbt.putIntArray("Pos1", p1);
                    changed = true;
                }
            }
            if (payload.shift() && nbt.contains("Pos2")) {
                int[] p2 = nbt.getIntArray("Pos2").orElse(new int[0]);
                if (p2.length == 3 && octantCornerInRange(player, p2[0] + dx, p2[1] + dy, p2[2] + dz)) {
                    p2[0] += dx;
                    p2[1] += dy;
                    p2[2] += dz;
                    nbt.putIntArray("Pos2", p2);
                    changed = true;
                }
            }
        }
        if (changed) {
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    public static void handleBuildingWandConfigure(BuildingWandConfigurePayload payload, ServerPlayer player) {
        ItemStack stack = player.getMainHandItem();
        if (stack.getItem() instanceof BuildingWandItem wand) {
            CustomData nbtComponent = stack.getOrDefault(DataComponents.CUSTOM_DATA, CustomData.EMPTY);
            CompoundTag nbt = nbtComponent.copyTag();
            // SBG-04: clamp what a manipulated client could send (radius 0..tier maximum, axis mode 0..3).
            nbt.putInt("SettingsRadius", Math.max(0, Math.min(payload.selectedRadius(), (wand.getWandSquareDiameter() - 1) / 2)));
            nbt.putInt("SettingsAxis", Math.max(0, Math.min(payload.axisMode(), 3)));
            stack.set(DataComponents.CUSTOM_DATA, CustomData.of(nbt));
        }
    }

    /**
     * Signieren parst den ganzen Code (bis 32 000 Zeichen). Jeder Spieler hat dafuer ein Budget an
     * Zeichen, das sich mit {@link #SIGN_REFILL_PER_TICK} je Tick wieder auffuellt: zwei volle Codes
     * sofort, danach einer je Sekunde. Ein Client, der Signier-Pakete in Schleife schickt, kann den
     * Server so nicht mehr mit Parsen beschaeftigen (Audit 2026-09-26 #11); Zwischenspeichern ohne
     * Signieren parst nicht und bleibt frei.
     */
    public static final int SIGN_BUDGET_CHARS = 2 * com.simplebuildingtools.blueprint.BlueprintCode.MAX_CODE_LENGTH;
    public static final int SIGN_REFILL_PER_TICK = com.simplebuildingtools.blueprint.BlueprintCode.MAX_CODE_LENGTH / 20;
    /** Budget je Spieler: {verfuegbare Zeichen, Spielzeit der letzten Abrechnung}; schwach, faellt mit dem Spieler weg. */
    private static final java.util.Map<ServerPlayer, long[]> SIGN_BUDGET = new java.util.WeakHashMap<>();

    /** Bucht {@code chars} Zeichen vom Signier-Budget des Spielers ab; {@code false} = zu viel auf einmal. */
    static boolean takeSignBudget(ServerPlayer player, int chars) {
        long now = player.level().getGameTime();
        synchronized (SIGN_BUDGET) {
            long[] budget = SIGN_BUDGET.computeIfAbsent(player, p -> new long[]{SIGN_BUDGET_CHARS, now});
            long elapsed = Math.max(0, now - budget[1]);
            budget[0] = Math.min(SIGN_BUDGET_CHARS, budget[0] + elapsed * SIGN_REFILL_PER_TICK);
            budget[1] = now;
            int cost = Math.max(1, chars);
            if (budget[0] < cost) {
                return false;
            }
            budget[0] -= cost;
            return true;
        }
    }

    /**
     * Neuer Code aus dem Editor. Wie beim Buch prueft der Server alles selbst: Slot (Hotbar oder
     * Nebenhand), eine unsignierte Blaupause darin, Laenge des Codes; beim Signieren zusaetzlich
     * Titel (1-32 Zeichen) und fehlerfreien, nicht leeren Code. Liegen mehrere leere Blaupausen
     * im Slot, bleibt die beschriebene dort und der Rest wird abgespalten. Der Editor schickt das
     * Paket entprellt waehrend des Tippens und beim Schliessen (Autospeichern); gespeichert wird
     * sofort am Item. Weist das Signier-Budget ab, sagt es die Aktionsleiste (Nach-Audit 2026-09-27
     * N16; frueher blieb der Klick auf "Signieren" stumm).
     */
    /**
     * Zugriff auf eine abgelegte Blaupause (der Block gehoert SimpleBuilding): {@code get} liefert den Stapel nur,
     * wenn der Spieler ihn dort bearbeiten darf (Reichweite, Rechte), sonst null. Ohne SimpleBuilding bleibt er
     * leer und abgelegte Blaupausen gibt es nicht (b-bundle131).
     */
    public interface PlacedAccess {
        @org.jetbrains.annotations.Nullable ItemStack get(ServerPlayer player, net.minecraft.core.BlockPos pos);

        void set(ServerPlayer player, net.minecraft.core.BlockPos pos, ItemStack stack);
    }

    private static @org.jetbrains.annotations.Nullable PlacedAccess placedAccess;

    public static void setPlacedAccess(@org.jetbrains.annotations.Nullable PlacedAccess access) {
        placedAccess = access;
    }

    public static void handleBlueprintEdit(BlueprintEditPayload payload, ServerPlayer player) {
        if (payload.placed().isPresent()) {
            PlacedAccess access = placedAccess;
            net.minecraft.core.BlockPos pos = payload.placed().get();
            ItemStack stack = access != null ? access.get(player, pos) : null;
            if (stack == null || !(stack.getItem() instanceof com.simplebuildingtools.items.custom.BlueprintItem)) {
                return;
            }
            com.simplebuildingtools.blueprint.BlueprintContent written = editedBlueprint(com.simplebuildingtools.items.custom.BlueprintItem.content(stack), payload, player);
            if (written != null) {
                ItemStack updated = stack.copyWithCount(1);
                updated.set(com.simplebuildingtools.component.BtComponents.BLUEPRINT, written);
                access.set(player, pos, updated);
            }
            return;
        }
        int slot = payload.slot();
        if (!(net.minecraft.world.entity.player.Inventory.isHotbarSlot(slot) || slot == net.minecraft.world.entity.player.Inventory.SLOT_OFFHAND)) {
            return;
        }
        ItemStack stack = player.getInventory().getItem(slot);
        if (!(stack.getItem() instanceof com.simplebuildingtools.items.custom.BlueprintItem)) {
            return;
        }
        com.simplebuildingtools.blueprint.BlueprintContent written = editedBlueprint(com.simplebuildingtools.items.custom.BlueprintItem.content(stack), payload, player);
        if (written == null) {
            return;
        }
        if (stack.getCount() > 1) {
            // Die beschriebene bleibt im Slot (dorthin gehen auch die naechsten Autospeicherungen),
            // der Rest des Stapels wandert ins Inventar oder faellt heraus.
            ItemStack rest = stack.copyWithCount(stack.getCount() - 1);
            stack.setCount(1);
            stack.set(com.simplebuildingtools.component.BtComponents.BLUEPRINT, written);
            if (!player.getInventory().add(rest)) {
                player.drop(rest, false, net.minecraft.util.Prediction.SERVER_ONLY);
            }
            return;
        }
        stack.set(com.simplebuildingtools.component.BtComponents.BLUEPRINT, written);
    }

    /** Der neue Inhalt nach den Regeln des Editors, oder null, wenn der Server die Aenderung abweist. */
    public static com.simplebuildingtools.blueprint.BlueprintContent editedBlueprint(com.simplebuildingtools.blueprint.BlueprintContent old,
            BlueprintEditPayload payload, ServerPlayer player) {
        if (old.signed()) {
            return null;
        }
        String code = payload.code().replace("\r", "");
        if (code.length() > com.simplebuildingtools.blueprint.BlueprintCode.MAX_CODE_LENGTH) {
            return null;
        }
        // Parsen kostet: Signieren und jeder Code mit pixels-Zeilen (BlueprintPicture.refusal parst ihn)
        // zahlen vorab aus dem Budget (Review F-02); gewoehnlicher Code bleibt frei.
        if (payload.sign()) {
            String t = payload.title().strip();
            if (t.isEmpty() || t.length() > com.simplebuildingtools.blueprint.BlueprintCode.MAX_TITLE_LENGTH) {
                return null;
            }
        }
        if ((payload.sign() || code.contains(com.simplebuildingtools.blueprint.BlueprintCode.PIXELS)) && !takeSignBudget(player, code.length())) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable("simplebuildingtools.blueprint.sign.too_fast")
                    .withStyle(net.minecraft.ChatFormatting.YELLOW));
            return null;
        }
        // Bild-Bauplan (pixels-Zeilen): Server-Schalter, Rechtestufe und Groesse (BlueprintPicture).
        String refusal = com.simplebuildingtools.blueprint.BlueprintPicture.refusal(player, code);
        if (refusal != null) {
            player.sendOverlayMessage(net.minecraft.network.chat.Component.translatable(refusal).withStyle(net.minecraft.ChatFormatting.YELLOW));
            return null;
        }
        if (payload.sign()) {
            String title = payload.title().strip();
            // Zwischengespeichert: der Bau (Tooltip, Vorschau, Baustab) fragt gleich denselben Code.
            com.simplebuildingtools.blueprint.BlueprintCode.ParseResult parsed = com.simplebuildingtools.blueprint.BlueprintCode.parseCached(code);
            if (!parsed.ok() || parsed.model().isEmpty()) {
                return null;
            }
            return new com.simplebuildingtools.blueprint.BlueprintContent(code, title, player.getName().getString(), true);
        }
        if (code.equals(old.code())) {
            return null;
        }
        return new com.simplebuildingtools.blueprint.BlueprintContent(code, old.title(), "", false);
    }

    /** Strg+Mausrad im Baumodus: nur mit Baustab in der Haupthand und Blaupause in der Nebenhand. */
    public static void handleBlueprintRotate(BlueprintRotatePayload payload, ServerPlayer player) {
        ItemStack blueprint = player.getOffhandItem();
        if (!(blueprint.getItem() instanceof com.simplebuildingtools.items.custom.BlueprintItem)
                || !(player.getMainHandItem().getItem() instanceof BuildingWandItem) || payload.amount() == 0) {
            return;
        }
        int steps = Math.floorMod(com.simplebuildingtools.blueprint.BlueprintBuilder.rotationSteps(blueprint) + Integer.signum(payload.amount()), 4);
        blueprint.set(com.simplebuildingtools.component.BtComponents.BLUEPRINT_ROTATION, steps);
    }

}
