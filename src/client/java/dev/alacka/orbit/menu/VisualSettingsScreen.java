package dev.alacka.orbit.menu;

import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.config.OrbitSettings;
import dev.alacka.orbit.config.VisualSettings;
import java.util.List;
import java.util.Locale;
import java.util.function.Consumer;
import java.util.function.DoubleConsumer;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.AbstractButton;
import net.minecraft.client.gui.components.AbstractSliderButton;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.input.MouseButtonEvent;
import net.minecraft.network.chat.Component;

/** Visual-module drafts share the established settings layout and background owner. */
public final class VisualSettingsScreen extends Screen implements OrbitWindow {
    private final Screen parent;
    private final String module;
    private final WindowDrag drag;
    private VisualSettings draft;
    private ModuleSettingsLayout box;
    private Button save;
    private int page, controlCount;
    private String notice = "";

    public VisualSettingsScreen(Screen parent, String module) {
        super(Component.literal(module + " · настройки"));
        if (!VisualSettings.MODULES.contains(module) && !module.equals("Trajectories"))
            throw new IllegalArgumentException("Unknown visual module: " + module);
        this.parent = parent;
        this.module = module;
        this.drag = new WindowDrag("visual." + module);
        this.draft = OrbitSettings.get().visual();
    }

    @Override protected void init() {
        clearWidgets();
        box = ModuleSettingsLayout.of(width, height, drag.dx(), drag.dy());
        List<Control> options = new java.util.ArrayList<>();
        options.add((x,y,w,h) -> addRenderableWidget(Button.builder(
                Component.literal("Бинд: " + dev.alacka.orbit.input.ModuleBinds.label(module)),
                b -> minecraft.setScreen(new BindScreen(this, module))).bounds(x,y,w,h).build()));
        options.addAll(controls());
        controlCount = options.size();
        int pages = box.pages(controlCount);
        page = Math.clamp(page, 0, pages - 1);
        if (pages > 1) {
            Button back = Button.builder(Component.literal("<"), b -> { page--; init(); })
                    .bounds(box.x()+box.width()-92,box.y()+10,22,20).build();
            back.active = page > 0;
            addRenderableWidget(back);
            Button next = Button.builder(Component.literal(">"), b -> { page++; init(); })
                    .bounds(box.x()+box.width()-32,box.y()+10,22,20).build();
            next.active = page < pages-1;
            addRenderableWidget(next);
        }
        int start = page * box.rows();
        for (int i=0; i<box.rows() && start+i<controlCount; i++)
            options.get(start+i).add(box.x()+12,box.controlY(i),box.width()-24,box.rowHeight());
        int w = (box.width()-36)/3;
        addRenderableWidget(Button.builder(Component.literal("Сбросить"), b -> {
            draft = draft.settingsFrom(module,VisualSettings.DEFAULT); notice = ""; init();
        }).bounds(box.x()+12,box.footerY(),w,20).build());
        addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> onClose())
                .bounds(box.x()+18+w,box.footerY(),w,20).build());
        save = addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> apply())
                .bounds(box.x()+24+2*w,box.footerY(),w,20).build());
    }

    private List<Control> controls() {
        List<Control> options = new java.util.ArrayList<>();
        switch (module) {
            case "TargetHUD" -> options.add(slider("Показывать после удара, сек",1,10,1,draft.targetSeconds(),v -> draft=draft.number("targetSeconds",(int)v),true,true));
            case "DamageNumbers" -> {
                options.add(toggle("Показывать лечение",draft.healing(),v -> draft=draft.option("healing",v)));
                options.add(slider("Время чисел, тиков",12,60,1,draft.numberTicks(),v -> draft=draft.number("numberTicks",(int)v),true,true));
            }
            case "HitParticles", "CriticalHitEffect" -> options.add(slider("Частиц при попадании",2,32,1,draft.particleCount(),v -> draft=draft.number("particleCount",(int)v),true,true));
            case "SeeInvisibles" -> options.add(toggle("Также невидимые мобы",draft.seeMobs(),v -> draft=draft.option("seeMobs",v)));
            case "Trajectories" -> {
                options.add(toggle("Градиент по времени полёта",draft.trajectoryGradient(),v -> draft=draft.option("trajectoryGradient",v)));
                options.add(toggle("Время до первого попадания",draft.trajectoryTime(),v -> draft=draft.option("trajectoryTime",v)));
            }
            case "ShieldStatus", "TotemCounter" -> { }
            default -> throw new IllegalStateException(module);
        }
        if (!module.equals("Trajectories")) options.add(slider("Общая дальность визуала",8,64,1,draft.range(),v -> draft=draft.number("range",(int)v),true,true));
        if (module.equals("TotemCounter")) options.add((x,y,w,h) -> addRenderableWidget(Button.builder(Component.literal("Сбросить замеченные срабатывания"), b -> {
            dev.alacka.orbit.modules.VisualModules.clearPopCounts(); notice="Счётчики срабатываний сброшены";
        }).bounds(x,y,w,h).build()));
        return options;
    }

    @FunctionalInterface private interface Control { void add(int x,int y,int w,int h); }

    private Control toggle(String label, boolean value, Consumer<Boolean> change) {
        return (x,y,w,h) -> addRenderableWidget(Button.builder(Component.literal(label+": "+(value?"Да":"Нет")), b -> {
            change.accept(!value); notice=""; init();
        }).bounds(x,y,w,h).build());
    }
    private Control slider(String label,double min,double max,double step,double value,
                           DoubleConsumer change,boolean enabled,boolean integer) {
        return (x,y,w,h) -> {
            var slider = new SettingSlider(x,y,w,h,label,min,max,step,value,change,integer);
            slider.active=enabled;
            addRenderableWidget(slider);
        };
    }
    private void apply() {
        try {
            var current = OrbitSettings.get();
            OrbitSettings.save(current.withVisual(current.visual().settingsFrom(module,draft)));
            minecraft.setScreen(parent);
        } catch (Exception e) {
            notice = "Не удалось сохранить; подробности в журнале.";
            OrbitClient.LOGGER.error("[Orbit] Cannot save visual preferences",e);
        }
    }

    @Override public boolean accent(AbstractButton button) { return button==save; }
    @Override public void onClose() { minecraft.setScreen(parent); }
    @Override public boolean isPauseScreen() { return false; }
    @Override public boolean mouseClicked(MouseButtonEvent e,boolean doubleClick) {
        return drag.press(this,e,box.x(),box.y(),box.width()) || super.mouseClicked(e,doubleClick);
    }
    @Override public boolean mouseDragged(MouseButtonEvent e,double dx,double dy) {
        if (!drag.dragging()) return super.mouseDragged(e,dx,dy);
        var next=ModuleSettingsLayout.of(width,height,drag.targetX(e.x()),drag.targetY(e.y()));
        var centre=ModuleSettingsLayout.of(width,height);
        WindowDrag.shift(this,next.x()-box.x(),next.y()-box.y());
        drag.applied(next.x()-centre.x(),next.y()-centre.y());
        box=next; return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        if (!drag.dragging()) return super.mouseReleased(e);
        mouseDragged(e,0,0); drag.release(); return true;
    }

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta) {
        // MenuBackdropMixin already renders the background. Never request a second blur.
        GlassMenu.shadow(g,box.x(),box.y(),box.width(),box.height());
        GlassMenu.panel(g,box.x(),box.y(),box.width(),box.height(),12,GlassMenu.WINDOW_FILL,GlassMenu.WINDOW_BORDER);
        drag.render(this,g,mouseX,mouseY,box.x(),box.y(),box.width());
        GlassMenu.heading(g,module,box.x()+12,box.y()+12,0xfffcf7ff);
        GlassMenu.text(g,font.plainSubstrByWidth(ModuleHelp.text(module),box.width()-24),
                box.x()+12,box.y()+(box.height()>=260?31:27),0xffddcbea);
        int pages=box.pages(controlCount);
        if (pages>1) g.drawCenteredString(font,(page+1)+"/"+pages,box.x()+box.width()-51,box.y()+16,0xfff5e9ff);
        String help = notice.isEmpty() ? ModuleHelp.limit(module) : notice;
        int y=box.helpY();
        for (var line:font.split(Component.literal(help),box.width()-24)) {
            if (y+9>box.footerY()-5) break;
            g.drawString(font,line,box.x()+12,y,notice.isEmpty()?0xffdccae8:0xffffb5c1,false); y+=11;
        }
        super.render(g,mouseX,mouseY,delta);
    }

    private static final class SettingSlider extends AbstractSliderButton {
        private final String label;
        private final double min,max,step;
        private final DoubleConsumer change;
        private final boolean integer;
        SettingSlider(int x,int y,int w,int h,String label,double min,double max,double step,
                      double initial,DoubleConsumer change,boolean integer) {
            super(x,y,w,h,Component.empty(),(initial-min)/(max-min));
            this.label=label;this.min=min;this.max=max;this.step=step;this.change=change;this.integer=integer;
            updateMessage();
        }
        private double selected() {
            return Math.clamp(Math.round((min+Math.round(value*(max-min)/step)*step)*1000.0)/1000.0,min,max);
        }
        @Override protected void updateMessage() {
            setMessage(Component.literal(label+": "+(integer?Integer.toString((int)selected()):String.format(Locale.ROOT,"%.1f",selected()))));
        }
        @Override protected void applyValue() { change.accept(selected()); }
    }
}