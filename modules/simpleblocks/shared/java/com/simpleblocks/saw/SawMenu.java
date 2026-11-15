package com.simpleblocks.saw;

import java.util.List;

/**
 * What {@link com.simpleblocks.saw.client.ShapecutterScreen} needs from its menu (N62): the cuts for the current input in button
 * order, the selected one, whether there is an input with cuts, and a hook for input changes. The Shapecutter
 * ({@link ShapecutterMenu}) and the Pulse/Auto Shapecutter ({@link ShapecutterMachineMenu}) share the one Vanilla-stonecutter screen.
 * Both keep the stonecutter's slots: index 0 the input, index {@link ShapecutterMenu#RESULT_SLOT} the result (or the preview).
 */
public interface SawMenu {
    List<Sawing.Entry> cuts();

    int selectedIndex();

    boolean hasInput();

    void registerUpdateListener(Runnable listener);
}
