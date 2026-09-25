package com.simplebuilding.gametest;

/**
 * How long a vanilla machine cooks, MC 26.2 side (twin in mc26_3/overlay/java). On 26.2 the
 * blasting and smoking recipes carry the machine's time themselves (half the furnace time). 26.3
 * gives them the furnace time and makes the blast furnace and smoker twice as fast through the fuel
 * (minecraft:block/fast_cooking); the machine time is then half the recipe time.
 */
final class CookingChecks {

    private CookingChecks() {
    }

    /** Ticks a vanilla machine needs for a recipe; {@code fastMachine} for blast furnace and smoker. */
    static int vanillaCookTime(int recipeCookingTime, boolean fastMachine) {
        return recipeCookingTime;
    }
}
