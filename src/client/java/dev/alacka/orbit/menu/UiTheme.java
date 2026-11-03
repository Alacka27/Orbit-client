package dev.alacka.orbit.menu;

import dev.alacka.orbit.config.OrbitSettings;

/** Shared live-preview palette. Background image, white text and warnings stay untouched. */
public final class UiTheme {
    private UiTheme() {}
    public static int tint(int argb,boolean surface) { return UiPalette.tint(argb,surface,OrbitSettings.theme()); }
    public static int accent() { return 0xff000000 | OrbitSettings.theme().accent(); }
}
