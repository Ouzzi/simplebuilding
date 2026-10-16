"""Vanilla-derived textures of the simplecrucibles module: the Enderite crucible and barrel faces (split stage 5) and
the copper/Enderite buckets (stage 5b; soul lava and its bucket come from soul_lava_2026_10_09.py).

Drawn by crucible_art_v2_2026_10_05.py from the Vanilla textures; this module only hands them to
tools/textures/vanilla_derived.py --module simplecrucibles (build/vanilla-textures must be extracted first).
"""
import crucible_art_v2_2026_10_05 as art


def derived(jar):
    return art.derived_module(jar)
