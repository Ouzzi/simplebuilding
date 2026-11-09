package com.simplebuilding.modules.simplemodels.editor;

/**
 * Editor stage 2 (b-models13): where the parts of the editor screen go, as plain arithmetic (tested in
 * {@code editor_layout}). Owner choice MODELS11 = A (Blockbench classic): toolbar on top, cube list left, viewport in the
 * middle, properties right, status line at the bottom. The "code only" switch shows code and live preview side by side,
 * equal width (MODELS12 variant A, the default while the owner has not answered). Small windows fold the side panels
 * away (properties first, then the cube list) so the viewport keeps {@link #MIN_VIEW} pixels.
 */
public record ModelEditorLayout(boolean codeOnly, Rect toolbar, Rect outliner, Rect view, Rect properties, Rect code,
                                Rect problems, Rect status) {
    public static final int MARGIN = 6, GAP = 3, TOOLBAR_H = 20, STATUS_H = 11, PROBLEMS_H = 22, MIN_VIEW = 120;

    public record Rect(int x, int y, int w, int h) {
        public static final Rect NONE = new Rect(0, 0, 0, 0);
        public boolean contains(double px, double py) { return w > 0 && h > 0 && px >= x && px < x + w && py >= y && py < y + h; }
        public int right() { return x + w; }
        public int bottom() { return y + h; }
        public boolean visible() { return w > 0 && h > 0; }
    }

    public static ModelEditorLayout of(int width, int height, boolean codeOnly) {
        Rect toolbar = new Rect(MARGIN, MARGIN, width - 2 * MARGIN, TOOLBAR_H);
        int top = toolbar.bottom() + GAP, bottom = height - MARGIN - STATUS_H - GAP;
        int bodyH = Math.max(0, bottom - top);
        Rect status = new Rect(MARGIN, height - MARGIN - STATUS_H, width - 2 * MARGIN, STATUS_H);
        int inner = width - 2 * MARGIN;
        if (codeOnly) {
            int codeW = (inner - GAP) / 2;
            Rect code = new Rect(MARGIN, top, codeW, Math.max(0, bodyH - PROBLEMS_H - GAP));
            Rect problems = new Rect(MARGIN, code.bottom() + GAP, codeW, PROBLEMS_H);
            Rect view = new Rect(code.right() + GAP, top, inner - codeW - GAP, bodyH);
            return new ModelEditorLayout(true, toolbar, Rect.NONE, view, Rect.NONE, code, problems, status);
        }
        int listW = Math.clamp(inner * 18 / 100, 84, 124), propsW = Math.clamp(inner * 22 / 100, 104, 140);
        if (inner - listW - propsW - 2 * GAP < MIN_VIEW) propsW = 0;
        if (inner - listW - (propsW > 0 ? propsW + GAP : 0) - GAP < MIN_VIEW) listW = 0;
        Rect outliner = listW > 0 ? new Rect(MARGIN, top, listW, bodyH) : Rect.NONE;
        int viewX = MARGIN + (listW > 0 ? listW + GAP : 0);
        int viewW = inner - (listW > 0 ? listW + GAP : 0) - (propsW > 0 ? propsW + GAP : 0);
        Rect view = new Rect(viewX, top, viewW, bodyH);
        Rect properties = propsW > 0 ? new Rect(view.right() + GAP, top, propsW, bodyH) : Rect.NONE;
        return new ModelEditorLayout(false, toolbar, outliner, view, properties, Rect.NONE, Rect.NONE, status);
    }
}
