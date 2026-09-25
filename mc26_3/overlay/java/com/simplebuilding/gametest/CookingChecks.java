package com.simplebuilding.gametest;

/** MC 26.3 side of {@code CookingChecks}: fast machines cook the recipe at speed 2.0. */
final class CookingChecks {

    private CookingChecks() {
    }

    static int vanillaCookTime(int recipeCookingTime, boolean fastMachine) {
        // Same rounding as AbstractFurnaceBlockEntity#getTotalCookTime: ceil(time / multiplier).
        return fastMachine ? (int) Math.ceil(recipeCookingTime / 2.0F) : recipeCookingTime;
    }
}
