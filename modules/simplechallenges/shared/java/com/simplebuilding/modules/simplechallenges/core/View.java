package com.simplebuilding.modules.simplechallenges.core;

import com.simplebuilding.modules.simplechallenges.unlock.UnlockGrid;

/** What the hooks need to know: the free cells of a dimension (null = this challenge does not cover it) and whether creative players ignore the borders. */
public interface View {
    UnlockGrid grid(String dimension);

    boolean exemptCreative();
}
