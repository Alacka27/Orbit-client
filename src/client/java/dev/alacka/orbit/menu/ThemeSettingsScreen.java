package dev.alacka.orbit.menu;

import dev.alacka.orbit.config.ClientSettings;
import dev.alacka.orbit.config.OrbitSettings;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.EditBox;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.network.chat.Component;
import java.util.function.IntConsumer;

/** Changes are previewed live; Apply commits atomically, Back/Esc discards the draft. */
public final class ThemeSettingsScreen extends Screen {
    private final Screen parent;
    private ClientSettings draft;
    private ThemeLayout box;
    private EditBox hex;
    private boolean updating;
    private final java.util.List<ColorSlider> rgbSliders = new java.util.ArrayList<>();
    private String notice = "";
    public ThemeSettingsScreen(Screen parent) { super(Component.literal("Интерфейс Orbit")); this.parent = parent; draft = OrbitSettings.get(); }
    @Override protected void init() {
        clearWidgets(); rgbSliders.clear(); box = ThemeLayout.of(width,height); OrbitSettings.preview(draft);
        int x=box.x()+12, w=box.controlWidth();
        hex = new EditBox(font,x,box.hexY(),w-60,box.controlHeight(),Component.literal("Цвет HEX"));
        hex.setMaxLength(7); hex.setValue(String.format("#%06X",draft.accent()));
        hex.setResponder(s -> {
            if (updating) return;
            if (s.matches("#?[a-fA-F0-9]{6}")) {
                draft = draft.theme(Integer.parseInt(s.replace("#",""),16),draft.windowOpacity(),draft.buttonOpacity(),draft.animations());
                OrbitSettings.preview(draft); syncColors(); notice="";
            } else notice = "Цвет: #RRGGBB, например #945CDA";
        }); addRenderableWidget(hex);
        if (!box.compact()) {
            String[] labels={"Красный","Зелёный","Синий"};
            for (int i=0;i<3;i++) {
                int shift=(2-i)*8;
                rgbSliders.add(slider(x,box.y()+75+i*26,w,labels[i],(draft.accent()>>shift)&255,0,255,v -> {
                    int color=(draft.accent() & ~(255<<shift)) | v<<shift;
                    draft=draft.theme(color,draft.windowOpacity(),draft.buttonOpacity(),draft.animations());
                    updating=true; hex.setValue(String.format("#%06X",color)); updating=false; OrbitSettings.preview(draft); notice="";
                }));
            }
        }
        slider(x,box.opacityY(),w,"Плотность окна",draft.windowOpacity(),0,85,v -> { draft=draft.theme(draft.accent(),v,draft.buttonOpacity(),draft.animations()); OrbitSettings.preview(draft); });
        slider(x,box.opacityY()+box.step(),w,"Плотность кнопок",draft.buttonOpacity(),35,95,v -> { draft=draft.theme(draft.accent(),draft.windowOpacity(),v,draft.animations()); OrbitSettings.preview(draft); });
        addRenderableWidget(Button.builder(Component.literal("Анимации: "+(draft.animations()?"ON":"OFF")),b->{
            draft=draft.theme(draft.accent(),draft.windowOpacity(),draft.buttonOpacity(),!draft.animations()); OrbitSettings.preview(draft);
            b.setMessage(Component.literal("Анимации: "+(draft.animations()?"ON":"OFF")));
        }).bounds(x,box.opacityY()+2*box.step(),w,box.controlHeight()).build());
        if (box.preview()) {
            int[] colors={0xa15be0,0x6488e8,0xde729c,0xd2a054,0x58a69d};
            for (int i=0;i<colors.length;i++) { int color=colors[i];
                addRenderableWidget(new ColorSwatch(box.previewX()+i*23,box.y()+207,color,()->draft.accent(),b->{
                    draft=draft.theme(color,draft.windowOpacity(),draft.buttonOpacity(),draft.animations());
                    updating=true;hex.setValue(String.format("#%06X",color));updating=false;syncColors();OrbitSettings.preview(draft);notice="";
                }));
            }
        }
        int footerWidth=box.width()-24, third=(footerWidth-12)/3, y=box.footerY();
        addRenderableWidget(Button.builder(Component.literal("Применить"),b->{
            if (!hex.getValue().matches("#?[a-fA-F0-9]{6}")) { notice="Введите цвет в формате #RRGGBB"; return; }
            try { OrbitSettings.save(draft); minecraft.setScreen(parent); }
            catch (Exception e) { notice="Ошибка сохранения; файл не изменён"; }
        }).bounds(x,y,third,22).build());
        addRenderableWidget(Button.builder(Component.literal("Сбросить"),b->{
            var d=ClientSettings.DEFAULT; draft=draft.theme(d.accent(),d.windowOpacity(),d.buttonOpacity(),d.animations()); notice="Примените, чтобы сохранить"; init();
        }).bounds(x+third+6,y,third,22).build());
        addRenderableWidget(Button.builder(Component.translatable("gui.back"),b->onClose()).bounds(x+2*(third+6),y,footerWidth-2*(third+6),22).build());
    }
    private void syncColors() {
        for (int i=0;i<rgbSliders.size();i++) rgbSliders.get(i).sync(draft.accent()>>((2-i)*8)&255);
    }
    private ColorSlider slider(int x,int y,int width,String label,int initial,int min,int max,IntConsumer change) {
        return addRenderableWidget(new ColorSlider(x,y,width,box.controlHeight(),label,initial,min,max,change));
    }
    private static final class ColorSlider extends AbstractSliderButton {
        private final String label; private final int min,max; private final IntConsumer change;
        ColorSlider(int x,int y,int width,int height,String label,int initial,int min,int max,IntConsumer change) {
            super(x,y,width,height,Component.empty(),(initial-min)/(double)(max-min));
            this.label=label;this.min=min;this.max=max;this.change=change;updateMessage();
        }
        void sync(int n) { value=(n-min)/(double)(max-min);updateMessage(); }
        protected void updateMessage() { setMessage(Component.literal(label+": "+Math.round(min+value*(max-min))+(max==255?"":"%"))); }
        protected void applyValue() { change.accept((int)Math.round(min+value*(max-min))); }
    }
    @Override public void render(GuiGraphics g,int mx,int my,float delta) {
        renderBackground(g,mx,my,delta);
        GlassMenu.panel(g,box.x(),box.y(),box.width(),box.height(),12,GlassMenu.WINDOW_FILL,GlassMenu.WINDOW_BORDER);
        GlassMenu.heading(g,"Интерфейс",box.x()+12,box.y()+12,0xffffffff);
        if (!box.tiny()) GlassMenu.text(g,font.plainSubstrByWidth("Профиль: "+minecraft.getUser().getName()+" · HEX / RGB",box.width()-24),box.x()+12,box.y()+27,0xffeee4f8);
        // Exact selected colour swatch, not contrast-adjusted like button surfaces.
        g.fill(box.x()+box.controlWidth()-46,box.hexY()+3,box.x()+box.controlWidth(),box.hexY()+box.controlHeight()-3,0xff000000|draft.accent());
        if (box.preview()) renderPreview(g);
        if (!notice.isEmpty()) GlassMenu.text(g,font.plainSubstrByWidth(notice,box.width()-24),box.x()+12,box.noticeY(),0xffffffff);
        super.render(g,mx,my,delta);
    }
    private void renderPreview(GuiGraphics g) {
        int px=box.previewX(),py=box.y()+49,pw=box.previewWidth(),ph=142;
        GlassMenu.text(g,"Предпросмотр",px,py-15,0xffe9ddf4);
        g.enableScissor(px,py,px+pw,py+ph);
        g.blit(net.minecraft.client.renderer.RenderPipelines.GUI_TEXTURED,net.minecraft.resources.Identifier.fromNamespaceAndPath("orbit","clouds.png"),px,py,0f,0f,pw,ph,1600,900,1600,900,0xffffffff);
        GlassMenu.panel(g,px+10,py+24,pw-20,90,8,GlassMenu.WINDOW_FILL,GlassMenu.WINDOW_BORDER);
        GlassMenu.text(g,"Стеклянное окно",px+20,py+38,0xffffffff);
        int bw=(pw-48)/2;
        GlassMenu.panel(g,px+20,py+64,bw,22,5,0xdd9352cb,0xadc793e8);
        GlassMenu.panel(g,px+28+bw,py+64,bw,22,5,0x99513b62,0x747e6294);
        GlassMenu.text(g,"Кнопка",px+25,py+71,0xffffffff);
        GlassMenu.text(g,"Назад",px+33+bw,py+71,0xffffffff);
        g.disableScissor();
        GlassMenu.panel(g,px,py,pw,ph,6,0x00000000,0x899b7bb4);
        GlassMenu.text(g,"Сохраняется для этого профиля",px,box.y()+234,0xffe5d7f0);
    }
    @Override public void removed() { OrbitSettings.cancelPreview(); }
    @Override public void onClose() { OrbitSettings.cancelPreview(); minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
}
