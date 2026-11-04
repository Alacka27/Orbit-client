package dev.alacka.orbit.menu;
import java.util.function.IntSupplier;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.network.chat.Component;
public final class ColorSwatch extends Button {
    private final int color; private final IntSupplier selected;
    public ColorSwatch(int x,int y,int color,IntSupplier selected,OnPress action) {
        super(x,y,17,17,Component.literal(String.format("Цвет #%06X",color)),action,DEFAULT_NARRATION);this.color=color;this.selected=selected;
    }
    @Override protected void renderContents(GuiGraphics g,int mx,int my,float delta) {
        // Exact RGB swatch: do not pass it through the readable button-surface palette.
        GlassMenu.panel(g,getX(),getY(),17,17,5,0x751f142c,(selected.getAsInt()==color||isHoveredOrFocused())?0xffe0b8ff:0x606c567e);
        g.fill(getX()+4,getY()+4,getX()+13,getY()+13,0xff000000|color);
    }
}
