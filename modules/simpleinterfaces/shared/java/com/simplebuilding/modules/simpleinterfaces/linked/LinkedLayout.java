package com.simplebuilding.modules.simpleinterfaces.linked;

import com.simplelib.api.client.ui.UiBoxes;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import org.jetbrains.annotations.Nullable;

/**
 * N65 (docs/ai/KONZEPT-N65-LINKED-2026-11-15.md §4/§5/§11): where the marked containers go on an open container screen -
 * a pure function of the screen's measures (no client classes, testable on a server like {@code BoxLayout}).
 *
 * <ul>
 * <li>A station with the player's inventory: <b>A</b> {@link Mode#BELOW} - the station down to its inventory area, below it
 * the marked box (where the inventory was), 2 px, a hotbar strip; tabs left of the box [marked ...][inventory] (the 27 main
 * slots sit behind the last tab). Does A not fit: <b>C</b> {@link Mode#BESIDE} - the box left of the unchanged screen, the
 * group centred by moving {@code leftPos} (only {@link Input#clean} screens with the recipe book closed); else A scrolling
 * ({@link Mode#BELOW_SCROLL}). With the recipe book open: A, tabs on the right, no sideways shift. Clean screens are centred
 * vertically by moving {@code topPos}; the others keep Vanilla's place.</li>
 * <li>The inventory screen (E) or a screen without player slots: the box below the image (group centred by {@code topPos}),
 * else beside, else below scrolling.</li>
 * <li>Two double chests (own storage >= 6 rows and a marked one >= 6 rows, or several marks next to a double chest): C side by
 * side when the GUI is at least {@link #MIN_BESIDE_WIDTH} wide and exactly one container is marked; else <b>B</b>
 * {@link Mode#TABS} - side tabs [opened][marked ...] switch one storage area.</li>
 * </ul>
 * Every box keeps the N62 container margin (docs/ai/PRINZIPIEN.md A7): {@link UiBoxes#MARGIN} px of fill between its light
 * inner line and its outermost slots, the title its own margin above; 177 wide, 182 with the scroll bar in its own column.
 * All rectangles are relative to the screen image after the shift ({@code leftPos + dx}, {@code topPos + dy}).
 */
public final class LinkedLayout {
    public static final int SLOT = 18;
    /** Box edge to the first slot's item position: frame + margin. */
    public static final int EDGE = UiBoxes.FRAME + UiBoxes.MARGIN;
    /** Box top to the first slot row of a titled box (the title keeps its own margin). */
    public static final int TITLE = 18;
    /** A slot drawn 16x16 plus its light edge below and right. */
    public static final int SLOT_SIZE = 17;
    /** Width of a 9-column box: 3 px fill left and right of the slots. */
    public static final int WIDTH = 2 * EDGE + 8 * SLOT + SLOT_SIZE;
    /** The scroll bar column: 2 px bar, 3 px fill on both sides. */
    public static final int SCROLL_X = EDGE + 8 * SLOT + SLOT_SIZE + UiBoxes.MARGIN, SCROLL_BAR = 2;
    public static final int WIDTH_SCROLL = SCROLL_X + SCROLL_BAR + UiBoxes.MARGIN + UiBoxes.FRAME;
    /** The hotbar strip: one untitled row. */
    public static final int HOTBAR_HEIGHT = EDGE + SLOT_SIZE + UiBoxes.MARGIN + UiBoxes.FRAME_BOTTOM;
    /** Divider between two boxes and margin to the window edge. */
    public static final int GAP = 2, WINDOW = 2;
    /** Tabs: square tiles, one per entry, {@link #TAB_PITCH} apart, {@link #GAP} beside the box. */
    public static final int TAB = 24, TAB_PITCH = 26;
    /** Rows of the player's main inventory (behind the inventory tab). */
    public static final int INVENTORY_ROWS = 3;
    /** A double chest's rows. */
    public static final int DOUBLE_ROWS = 6;
    /** Narrowest GUI for two double chests side by side (177 + 2 + 176 + window margins). */
    public static final int MIN_BESIDE_WIDTH = 360;

    /**
     * Screens whose own background and widgets follow {@code leftPos/topPos} (checked in the 26.3 sources): moving the screen
     * for C is safe. Exact class names, so other mods' subclasses stay where they are.
     */
    public static final Set<String> CLEAN = Set.of("InventoryScreen", "CraftingScreen", "FurnaceScreen", "BlastFurnaceScreen",
            "SmokerScreen", "LoomScreen", "SmithingScreen", "StonecutterScreen");
    /** Screens Vanilla draws from {@code width/height}; clean only while the Simple style draws them (from {@code leftPos}). */
    public static final Set<String> CLEAN_STYLED = Set.of("ContainerScreen", "ShulkerBoxScreen", "HopperScreen", "DispenserScreen",
            "CrafterScreen");
    private static final String VANILLA = "net.minecraft.client.gui.screens.inventory.";

    /** Whether the screen class {@code className} may be moved ({@code styled}: the Simple style draws it right now). */
    public static boolean clean(String className, boolean styled) {
        if (!className.startsWith(VANILLA)) return false;
        String name = className.substring(VANILLA.length());
        return CLEAN.contains(name) || styled && CLEAN_STYLED.contains(name);
    }

    private LinkedLayout() {
    }

    public enum Mode {
        /** A: station, marked box (or the inventory behind its tab), hotbar strip; E: the box below the image. */
        BELOW,
        /** C: the box left of the screen, the group centred. */
        BESIDE,
        /** A scrolling: not every row fits. */
        BELOW_SCROLL,
        /** B: side tabs switch the storage area between the opened and the marked containers. */
        TABS
    }

    /** What a tab (and the box while it is open) shows. */
    public enum Tab {
        MARKED, INVENTORY, OPENED
    }

    public record Rect(int x, int y, int width, int height) {
        public int right() {
            return x + width;
        }

        public int bottom() {
            return y + height;
        }
    }

    /**
     * The screen as it opened: GUI size, image size, {@code leftPos/topPos} after its own {@code init} (recipe book moves),
     * the item y of the player's first inventory row ({@code -1}: none in the image), the opened container's own storage rows
     * (a 9-wide storage, else 0), whether it is the inventory screen (E), whether it may move ({@link #clean}), the slot counts
     * of the marked containers in order and the open tab.
     */
    public record Input(int guiWidth, int guiHeight, int imageWidth, int imageHeight, int leftPos, int topPos, int inventoryTop,
            int ownRows, boolean inventoryScreen, boolean clean, List<Integer> marked, int tab) {
        public Input {
            marked = List.copyOf(marked);
        }

        /** Same screen, another open tab. */
        public Input withTab(int tab) {
            return new Input(guiWidth, guiHeight, imageWidth, imageHeight, leftPos, topPos, inventoryTop, ownRows, inventoryScreen, clean, marked, tab);
        }
    }

    /**
     * The result: mode, shift of {@code leftPos/topPos}, the box of the open tab ({@code null} on B's opened tab) with what it
     * shows ({@code part} = index of the marked container, -1 for inventory/opened), its visible rows and whether it scrolls,
     * the hotbar strip (A), the tabs with their kinds (none with a single entry), and the band of the screen's own background
     * that stays visible: {@code y >= bgTop && y < bgBottom} (image coordinates; A cuts below the station, B's marked tab
     * above the inventory).
     */
    public record Layout(Mode mode, int dx, int dy, @Nullable Rect box, Tab shows, int part, int rows, boolean scroll,
            @Nullable Rect hotbar, List<Rect> tabs, List<Tab> tabKinds, int tab, int bgTop, int bgBottom) {
        /** Item x of column {@code column} in the box. */
        public int slotX(int column) {
            return box.x() + EDGE + column * SLOT;
        }

        /** Item y of visible row {@code row} in the box. */
        public int slotY(int row) {
            return box.y() + TITLE + row * SLOT;
        }

        /** Item x of hotbar slot {@code i} in the strip. */
        public int hotbarX(int i) {
            return hotbar.x() + EDGE + i * SLOT;
        }

        public int hotbarY() {
            return hotbar.y() + EDGE;
        }
    }

    public static int rows(int size) {
        return (size + 8) / 9;
    }

    /** Height of a titled box with {@code rows} rows. */
    public static int height(int rows) {
        return TITLE + (rows - 1) * SLOT + SLOT_SIZE + UiBoxes.MARGIN + UiBoxes.FRAME_BOTTOM;
    }

    public static Layout compute(Input in) {
        int n = in.marked().size();
        if (n == 0) throw new IllegalArgumentException("nothing marked");
        int maxRows = 0;
        for (int size : in.marked()) maxRows = Math.max(maxRows, rows(size));
        boolean book = in.leftPos() != (in.guiWidth() - in.imageWidth()) / 2;
        boolean movable = in.clean() && !book;
        boolean standalone = in.inventoryScreen() || in.inventoryTop() < 0;
        if (!standalone && in.ownRows() >= DOUBLE_ROWS && (maxRows >= DOUBLE_ROWS || n > 1)) {
            if (n == 1 && movable && in.guiWidth() >= MIN_BESIDE_WIDTH) {
                Layout c = beside(in, maxRows, true);
                if (fits(c, in)) return c;
            }
            return tabs(in);
        }
        // vertical centring also with the recipe book open (it only shifts sideways); C needs the book closed
        boolean movableY = in.clean();
        if (standalone) {
            Layout below = standaloneBelow(in, maxRows, movableY, false);
            if (fits(below, in)) return below;
        } else {
            Layout a = stationBelow(in, maxRows, movableY, book, false);
            if (fits(a, in)) return a;
        }
        if (movable) {
            Layout c = beside(in, maxRows, false);
            if (fits(c, in)) return c;
        }
        return standalone ? standaloneBelow(in, maxRows, movableY, true) : stationBelow(in, maxRows, movableY, book, true);
    }

    /** The open marked part for modes without an inventory tab. */
    private static int part(Input in) {
        return Math.clamp(in.tab(), 0, in.marked().size() - 1);
    }

    /** A: station, box (the open tab), hotbar strip. The shift and the fit use the tallest tab, so switching never moves the group. */
    private static Layout stationBelow(Input in, int maxRows, boolean movable, boolean book, boolean scrolling) {
        int n = in.marked().size();
        int cut = in.inventoryTop() - EDGE;
        int boxX = (in.imageWidth() - WIDTH) / 2;
        int areaRows = Math.max(maxRows, INVENTORY_ROWS);
        int tabCount = n + 1;
        int tabsHeight = (tabCount - 1) * TAB_PITCH + TAB;
        int fixed = cut + HOTBAR_HEIGHT + GAP;
        int visible = areaRows;
        if (scrolling) {
            int room = movable ? in.guiHeight() - 2 * WINDOW : in.guiHeight() - WINDOW - Math.max(WINDOW, in.topPos());
            visible = Math.clamp((room - fixed - height(1)) / SLOT + 1, 1, areaRows);
        }
        int groupBottom = Math.max(fixed + height(visible), cut + tabsHeight);
        int dy = movable ? (in.guiHeight() - groupBottom) / 2 - in.topPos() : 0;
        int tab = Math.clamp(in.tab(), 0, n);
        boolean inventory = tab == n;
        int contentRows = inventory ? INVENTORY_ROWS : rows(in.marked().get(tab));
        int shown = Math.min(contentRows, visible);
        boolean scroll = contentRows > shown;
        Rect box = new Rect(boxX, cut, scroll ? WIDTH_SCROLL : WIDTH, height(shown));
        Rect hotbar = new Rect(boxX, box.bottom() + GAP, WIDTH, HOTBAR_HEIGHT);
        // tabs left of the box; with the recipe book open (it lies left of the image) or no room on the left: on the right
        boolean right = book || in.leftPos() + boxX - GAP - TAB < WINDOW;
        int tabX = right ? boxX + WIDTH_SCROLL + GAP : boxX - GAP - TAB;
        List<Rect> tabs = new ArrayList<>();
        List<Tab> kinds = new ArrayList<>();
        for (int i = 0; i < tabCount; i++) {
            tabs.add(new Rect(tabX, cut + i * TAB_PITCH, TAB, TAB));
            kinds.add(i < n ? Tab.MARKED : Tab.INVENTORY);
        }
        return new Layout(scrolling && visible < areaRows ? Mode.BELOW_SCROLL : Mode.BELOW, 0, dy, box, inventory ? Tab.INVENTORY : Tab.MARKED,
                inventory ? -1 : tab, shown, scroll, hotbar, tabs, kinds, tab, Integer.MIN_VALUE, cut);
    }

    /** E (or no player slots): the box below the whole image; tabs left of it with several marks. */
    private static Layout standaloneBelow(Input in, int maxRows, boolean movable, boolean scrolling) {
        int n = in.marked().size();
        int top = in.imageHeight() + GAP;
        int boxX = (in.imageWidth() - WIDTH) / 2;
        int tabsHeight = n > 1 ? (n - 1) * TAB_PITCH + TAB : 0;
        int visible = maxRows;
        if (scrolling) {
            int room = movable ? in.guiHeight() - 2 * WINDOW : in.guiHeight() - WINDOW - Math.max(WINDOW, in.topPos());
            visible = Math.clamp((room - top - height(1)) / SLOT + 1, 1, maxRows);
        }
        int groupBottom = top + Math.max(height(visible), tabsHeight);
        int dy = movable ? (in.guiHeight() - groupBottom) / 2 - in.topPos() : 0;
        int part = part(in);
        int contentRows = rows(in.marked().get(part));
        int shown = Math.min(contentRows, visible);
        boolean scroll = contentRows > shown;
        Rect box = new Rect(boxX, top, scroll ? WIDTH_SCROLL : WIDTH, height(shown));
        boolean book = in.leftPos() != (in.guiWidth() - in.imageWidth()) / 2;
        boolean right = book || in.leftPos() + boxX - GAP - TAB < WINDOW;
        List<Rect> tabs = markedTabs(n, right ? boxX + WIDTH_SCROLL + GAP : boxX - GAP - TAB, top);
        return new Layout(scrolling && visible < maxRows ? Mode.BELOW_SCROLL : Mode.BELOW, 0, dy, box, Tab.MARKED, part, shown, scroll, null,
                tabs, kinds(tabs.size(), Tab.MARKED), part, Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /** C: the box left of the screen (top aligned with the image, moved up into the window if needed), the group centred. */
    private static Layout beside(Input in, int maxRows, boolean always) {
        int n = in.marked().size();
        int part = part(in);
        int contentRows = rows(in.marked().get(part));
        int room = in.guiHeight() - 2 * WINDOW;
        int fitRows = Math.max(1, (room - height(1)) / SLOT + 1);
        int shown = always ? Math.min(contentRows, fitRows) : contentRows;
        boolean scroll = contentRows > shown;
        int boxWidth = scroll || always && maxRows > fitRows ? WIDTH_SCROLL : WIDTH;
        int tabsWidth = n > 1 ? TAB + GAP : 0;
        int groupWidth = tabsWidth + boxWidth + GAP + in.imageWidth();
        int left = (in.guiWidth() - groupWidth) / 2 + tabsWidth + boxWidth + GAP;
        int dx = left - in.leftPos();
        int height = height(shown);
        int tabsHeight = n > 1 ? (n - 1) * TAB_PITCH + TAB : 0;
        int y = 0;
        int tall = Math.max(height(always ? Math.min(maxRows, fitRows) : maxRows), tabsHeight);
        if (in.topPos() + tall > in.guiHeight() - WINDOW) y = in.guiHeight() - WINDOW - in.topPos() - tall;
        if (in.topPos() + y < WINDOW) y = WINDOW - in.topPos();
        Rect box = new Rect(-(boxWidth + GAP), y, boxWidth, height);
        List<Rect> tabs = markedTabs(n, box.x() - GAP - TAB, y);
        return new Layout(Mode.BESIDE, dx, 0, box, Tab.MARKED, part, shown, scroll, null, tabs, kinds(tabs.size(), Tab.MARKED), part,
                Integer.MIN_VALUE, Integer.MAX_VALUE);
    }

    /** B: tabs left of the image [opened][marked ...]; a marked tab shows its box over the opened container's storage area. */
    private static Layout tabs(Input in) {
        int n = in.marked().size();
        int tab = Math.clamp(in.tab(), 0, n);
        int boxX = (in.imageWidth() - WIDTH) / 2;
        List<Rect> tabs = new ArrayList<>();
        List<Tab> kinds = new ArrayList<>();
        boolean right = in.leftPos() + Math.min(0, boxX) - GAP - TAB < WINDOW;
        int tabX = right ? Math.max(in.imageWidth(), boxX + WIDTH_SCROLL) + GAP : Math.min(0, boxX) - GAP - TAB;
        for (int i = 0; i <= n; i++) {
            tabs.add(new Rect(tabX, i * TAB_PITCH, TAB, TAB));
            kinds.add(i == 0 ? Tab.OPENED : Tab.MARKED);
        }
        if (tab == 0) return new Layout(Mode.TABS, 0, 0, null, Tab.OPENED, -1, 0, false, null, tabs, kinds, 0, Integer.MIN_VALUE, Integer.MAX_VALUE);
        int contentRows = rows(in.marked().get(tab - 1));
        // the storage area ends above the player's first row (its dark top edge one px above the item)
        int fitRows = Math.max(1, (in.inventoryTop() - GAP - height(1)) / SLOT + 1);
        int shown = Math.min(contentRows, fitRows);
        boolean scroll = contentRows > shown;
        Rect box = new Rect(boxX, 0, scroll ? WIDTH_SCROLL : WIDTH, height(shown));
        return new Layout(Mode.TABS, 0, 0, box, Tab.MARKED, tab - 1, shown, scroll, null, tabs, kinds, tab, box.bottom(), Integer.MAX_VALUE);
    }

    private static List<Rect> markedTabs(int n, int x, int y) {
        List<Rect> tabs = new ArrayList<>();
        if (n > 1) for (int i = 0; i < n; i++) tabs.add(new Rect(x, y + i * TAB_PITCH, TAB, TAB));
        return tabs;
    }

    private static List<Tab> kinds(int count, Tab kind) {
        List<Tab> out = new ArrayList<>();
        for (int i = 0; i < count; i++) out.add(kind);
        return out;
    }

    /**
     * Window bounds {x0, y0, x1, y1} of everything drawn: the visible part of the image, box, hotbar strip and tabs. For the
     * fit and the tests.
     */
    public static int[] bounds(Layout l, Input in) {
        int left = in.leftPos() + l.dx(), top = in.topPos() + l.dy();
        int x0 = 0, y0 = 0, x1 = in.imageWidth();
        int y1 = l.bgBottom() == Integer.MAX_VALUE ? in.imageHeight() : Math.min(in.imageHeight(), l.bgBottom());
        List<Rect> all = new ArrayList<>(l.tabs());
        if (l.box() != null) all.add(l.box());
        if (l.hotbar() != null) all.add(l.hotbar());
        for (Rect r : all) {
            x0 = Math.min(x0, r.x());
            y0 = Math.min(y0, r.y());
            x1 = Math.max(x1, r.right());
            y1 = Math.max(y1, r.bottom());
        }
        return new int[] {left + x0, top + y0, left + x1, top + y1};
    }

    /** Whether everything lies inside the window with {@link #WINDOW} px to spare. */
    public static boolean fits(Layout l, Input in) {
        int[] b = bounds(l, in);
        return b[0] >= WINDOW && b[1] >= WINDOW && b[2] <= in.guiWidth() - WINDOW && b[3] <= in.guiHeight() - WINDOW;
    }

    /**
     * Fill measured back from the box and the hotbar strip: box left, right (to the slots, or to the scroll bar and from it),
     * bottom; strip left, right, top, bottom ({@code -1} when absent). Each must be {@link UiBoxes#MARGIN}.
     */
    public static int[] margins(Layout l) {
        int[] out = {-1, -1, -1, -1, -1, -1, -1, -1};
        if (l.box() != null) {
            Rect b = l.box();
            out[0] = l.slotX(0) - (b.x() + UiBoxes.FRAME);
            int slotsRight = l.slotX(8) + SLOT_SIZE;
            out[1] = l.scroll() ? b.x() + SCROLL_X - slotsRight == UiBoxes.MARGIN ? b.right() - UiBoxes.FRAME - (b.x() + SCROLL_X + SCROLL_BAR) : -2
                    : b.right() - UiBoxes.FRAME - slotsRight;
            out[2] = b.bottom() - UiBoxes.FRAME_BOTTOM - (l.slotY(l.rows() - 1) + SLOT_SIZE);
        }
        if (l.hotbar() != null) {
            Rect h = l.hotbar();
            out[3] = l.hotbarX(0) - (h.x() + UiBoxes.FRAME);
            out[4] = h.right() - UiBoxes.FRAME - (l.hotbarX(8) + SLOT_SIZE);
            out[5] = l.hotbarY() - (h.y() + UiBoxes.FRAME);
            out[6] = h.bottom() - UiBoxes.FRAME_BOTTOM - (l.hotbarY() + SLOT_SIZE);
        }
        return out;
    }
}
