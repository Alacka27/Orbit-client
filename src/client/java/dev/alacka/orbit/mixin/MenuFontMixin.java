package dev.alacka.orbit.mixin;

import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.menu.GlassMenu;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

/** Use the same provider for layout, text fields, list entries and rendering. */
@Mixin(Font.class)
public abstract class MenuFontMixin {
    @Unique private static final Identifier orbit$default = Identifier.fromNamespaceAndPath("minecraft", "default");
    @Unique private static final FontDescription orbit$ui = new FontDescription.Resource(Identifier.fromNamespaceAndPath("orbit", "ui"));
    @Unique private static final Map<GlyphSource, Boolean> orbit$checked = Collections.synchronizedMap(new WeakHashMap<>());
    @Shadow private GlyphSource getGlyphSource(FontDescription description) { throw new AssertionError(); }

    @Inject(method = "getGlyphSource", at = @At("HEAD"), cancellable = true)
    private void orbit$menuFont(FontDescription description, CallbackInfoReturnable<GlyphSource> ci) {
        if (!GlassMenu.active() || !(description instanceof FontDescription.Resource resource)
                || !resource.id().equals(orbit$default)) return;
        // The recursive lookup uses orbit:ui, so it does not enter this default-font branch.
        GlyphSource source = getGlyphSource(orbit$ui);
        boolean valid = orbit$checked.computeIfAbsent(source, candidate -> {
            float wide = candidate.getGlyph('W').info().getAdvance(false);
            float narrow = candidate.getGlyph('i').info().getAdvance(false);
            float cyrillicWide = candidate.getGlyph('Ж').info().getAdvance(false);
            float cyrillicNarrow = candidate.getGlyph('и').info().getAdvance(false);
            boolean readable = wide > narrow + 0.1f && cyrillicWide > cyrillicNarrow + 0.1f;
            if (!readable) OrbitClient.LOGGER.warn("[Orbit] UI font unavailable; retaining readable Minecraft font.");
            return readable;
        });
        if (valid) ci.setReturnValue(source);
    }
}
