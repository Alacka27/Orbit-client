package dev.alacka.orbit.mixin;

import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.UiFonts;
import java.util.Collections;
import java.util.Map;
import java.util.WeakHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GlyphSource;
import net.minecraft.network.chat.FontDescription;
import net.minecraft.network.chat.FontDescription.Resource;
import net.minecraft.resources.Identifier;
import org.spongepowered.asm.mixin.Mixin;
import org.spongepowered.asm.mixin.Shadow;
import org.spongepowered.asm.mixin.Unique;
import org.spongepowered.asm.mixin.injection.At;
import org.spongepowered.asm.mixin.injection.Inject;
import org.spongepowered.asm.mixin.injection.callback.CallbackInfoReturnable;

@Mixin({Font.class})
public abstract class MenuFontMixin {
   @Unique
   private static final Identifier orbit$default = Identifier.fromNamespaceAndPath("minecraft", "default");
   @Unique
   private static final FontDescription orbit$ui = new Resource(UiFonts.UI);
   @Unique
   private static final Map<GlyphSource, Boolean> orbit$checked = Collections.synchronizedMap(new WeakHashMap<>());

   @Shadow
   private GlyphSource getGlyphSource(FontDescription description) {
      throw new AssertionError();
   }

   @Unique
   private static boolean orbit$readable(GlyphSource source) {
      return orbit$checked.computeIfAbsent(source, candidate -> {
         float wide = candidate.getGlyph(87).info().getAdvance(false);
         float narrow = candidate.getGlyph(105).info().getAdvance(false);
         float cyrillicWide = candidate.getGlyph(1046).info().getAdvance(false);
         float cyrillicNarrow = candidate.getGlyph(1080).info().getAdvance(false);
         boolean readable = wide > narrow + 0.1F && cyrillicWide > cyrillicNarrow + 0.1F;
         Minecraft minecraft = Minecraft.getInstance();
         if (!readable && minecraft != null && minecraft.isGameLoadFinished() && minecraft.getOverlay() == null) {
            OrbitClient.LOGGER.warn("[Orbit] A UI font variant is unavailable; using the next readable font.");
         }

         return readable;
      });
   }

   @Inject(
      method = {"getGlyphSource"},
      at = {@At("HEAD")},
      cancellable = true
   )
   private void orbit$menuFont(FontDescription description, CallbackInfoReturnable<GlyphSource> ci) {
      if (description instanceof Resource resource) {
         Identifier id = resource.id();
         boolean menuDefault = id.equals(orbit$default);
         boolean bold = id.equals(UiFonts.BOLD);
         if (menuDefault ? GlassMenu.active() : bold || id.equals(UiFonts.UI)) {
            GlyphSource variant = this.getGlyphSource(UiFonts.variant(bold, UiFonts.scale()));
            if (orbit$readable(variant)) {
               ci.setReturnValue(variant);
            } else {
               if (menuDefault) {
                  GlyphSource base = this.getGlyphSource(orbit$ui);
                  if (orbit$readable(base)) {
                     ci.setReturnValue(base);
                  }
               }
            }
         }
      }
   }
}
