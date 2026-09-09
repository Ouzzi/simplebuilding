package com.simplebuilding.clientgametest;

/**
 * A value one step produces and a later step reads.
 *
 * <p>Straight-line client tests hand values around in local variables, because the test thread
 * runs the whole body. A step list cannot: the steps are all registered <em>before</em> any of
 * them runs, so a value that only exists at run time has nowhere to live. This is that somewhere.
 *
 * <p>Reading it before it has been filled throws with the name in the message, which is the whole
 * point - the alternative is a null that travels quietly into a comparison and makes it pass.
 */
public final class Later<T> {

    private final String what;
    private T value;

    public Later(String what) {
        this.what = what;
    }

    public void set(T value) {
        this.value = value;
    }

    public boolean isSet() {
        return value != null;
    }

    public T get() {
        if (value == null) {
            throw new IllegalStateException(what + " is not available yet - the step that produces "
                    + "it has not run. In a step list every value is registered before anything "
                    + "runs, so this one has to be read in a LATER step than the one that fills it.");
        }
        return value;
    }
}
