package dev.alacka.orbit.menu;

import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.config.CombatSettings;
import dev.alacka.orbit.config.OrbitSettings;
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

/** Right-click settings are a draft: Esc/Cancel never saves and never toggles a module. */
public final class ModuleSettingsScreen extends Screen implements OrbitWindow {
    private final Screen parent;
    private final boolean aura;
    private final WindowDrag drag;
    private CombatSettings draft;
    private ModuleSettingsLayout box;
    private Button save;
    private int page, controlCount;
    private String notice = "";

    public ModuleSettingsScreen(Screen parent, boolean aura) {
        super(Component.literal((aura ? "KillAura" : "TriggerBot") + " · настройки"));
        this.parent = parent;
        this.aura = aura;
        this.drag = new WindowDrag(aura ? "killAura" : "triggerBot");
        this.draft = OrbitSettings.get().combat();
    }

    @Override protected void init() {
        clearWidgets();
        box = ModuleSettingsLayout.of(width, height, drag.dx(), drag.dy());
        List<Control> options = new java.util.ArrayList<>();
        String module = aura ? "KillAura" : "TriggerBot";
        options.add((x,y,w,h) -> addRenderableWidget(Button.builder(
                Component.literal("Бинд: " + dev.alacka.orbit.input.ModuleBinds.label(module)),
                b -> minecraft.setScreen(new BindScreen(this, module))).bounds(x,y,w,h).build()));
        options.addAll(aura ? auraControls() : triggerControls());
        controlCount = options.size();
        int pages = box.pages(options.size());
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
        for (int i = 0; i < box.rows() && start+i < options.size(); i++)
            options.get(start+i).add(box.x()+12,box.controlY(i),box.width()-24,box.rowHeight());
        int buttonWidth = (box.width()-36)/3;
        addRenderableWidget(Button.builder(Component.literal("Сбросить"), b -> {
            if (aura) draft = draft.withAura(CombatSettings.Aura.DEFAULT.enabled(draft.aura().enabled()));
            else draft = draft.withTrigger(CombatSettings.Trigger.DEFAULT.enabled(draft.trigger().enabled()));
            notice = ""; init();
        }).bounds(box.x()+12,box.footerY(),buttonWidth,20).build());
        addRenderableWidget(Button.builder(Component.literal("Отмена"), b -> onClose())
                .bounds(box.x()+18+buttonWidth,box.footerY(),buttonWidth,20).build());
        save = addRenderableWidget(Button.builder(Component.literal("Сохранить"), b -> apply())
                .bounds(box.x()+24+2*buttonWidth,box.footerY(),buttonWidth,20).build());
    }

    private List<Control> auraControls() {
        var a = draft.aura();
        return List.of(
                choice("Приоритет",a.priority(),v -> draft=draft.withAura(draft.aura().priority(v)),v -> v.label),
                slider("Дальность",1,6,.1,a.range(),v -> draft=draft.withAura(draft.aura().range(v)),true,false),
                slider("Целей в цикле",1,5,1,a.maxTargets(),v -> draft=draft.withAura(draft.aura().maxTargets((int)v)),true,true),
                choice("Поворот",a.rotation(),v -> draft=draft.withAura(draft.aura().rotation(v)),v -> v.label),
                slider("Градусов за тик",5,90,5,a.rotationStep(),v -> draft=draft.withAura(draft.aura().rotationStep((int)v)),
                        a.rotation()==CombatSettings.Rotation.SMOOTH,true),
                choice("Цели",a.targets(),v -> draft=draft.withAura(draft.aura().targets(v)),v -> v.label),
                toggle("Не трогать команду",a.ignoreTeam(),v -> draft=draft.withAura(draft.aura().ignoreTeam(v))),
                toggle("Только с оружием",a.onlyWeapon(),v -> draft=draft.withAura(draft.aura().onlyWeapon(v))),
                toggle("Пауза при еде / щите",a.pauseUsingItem(),v -> draft=draft.withAura(draft.aura().pauseUsingItem(v))));
    }

    private List<Control> triggerControls() {
        var t = draft.trigger();
        return List.of(
                choice("Версия PvP",t.mode(),v -> draft=draft.withTrigger(draft.trigger().mode(v)),v -> v.label),
                slider("Атак в секунду",1,20,1,t.cps(),v -> draft=draft.withTrigger(draft.trigger().cps((int)v)),
                        t.mode()==CombatSettings.AttackMode.CPS,true),
                slider("Дальность",1,6,.1,t.range(),v -> draft=draft.withTrigger(draft.trigger().range(v)),true,false),
                choice("Цели",t.targets(),v -> draft=draft.withTrigger(draft.trigger().targets(v)),v -> v.label),
                toggle("Не трогать команду",t.ignoreTeam(),v -> draft=draft.withTrigger(draft.trigger().ignoreTeam(v))),
                toggle("Только с оружием",t.onlyWeapon(),v -> draft=draft.withTrigger(draft.trigger().onlyWeapon(v))),
                toggle("Пауза при еде / щите",t.pauseUsingItem(),v -> draft=draft.withTrigger(draft.trigger().pauseUsingItem(v))));
    }

    @FunctionalInterface private interface Control { void add(int x,int y,int w,int h); }

    private <E extends Enum<E>> Control choice(String label, E value, Consumer<E> change,
                                                java.util.function.Function<E,String> text) {
        return (x,y,w,h) -> addRenderableWidget(Button.builder(Component.literal(label+": "+text.apply(value)), b -> {
            E[] values = value.getDeclaringClass().getEnumConstants();
            change.accept(values[(value.ordinal()+1)%values.length]); notice=""; init();
        }).bounds(x,y,w,h).build());
    }

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
            CombatSettings next = aura ? current.combat().withAura(draft.aura()) : current.combat().withTrigger(draft.trigger());
            OrbitSettings.save(current.combat(next));
            minecraft.setScreen(parent);
        } catch (Exception e) {
            notice = "Не удалось сохранить; подробности в журнале.";
            OrbitClient.LOGGER.error("[Orbit] Cannot save combat preferences",e);
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
        ModuleSettingsLayout next=ModuleSettingsLayout.of(width,height,drag.targetX(e.x()),drag.targetY(e.y()));
        ModuleSettingsLayout centre=ModuleSettingsLayout.of(width,height);
        WindowDrag.shift(this,next.x()-box.x(),next.y()-box.y());
        drag.applied(next.x()-centre.x(),next.y()-centre.y());
        box=next;
        return true;
    }
    @Override public boolean mouseReleased(MouseButtonEvent e) {
        if (!drag.dragging()) return super.mouseReleased(e);
        mouseDragged(e,0,0);drag.release();return true;
    }

    @Override public void render(GuiGraphics g,int mouseX,int mouseY,float delta) {
        // Screen renders the shared background before this method via MenuBackdropMixin.
        // A second backdrop here requests another blur and crashes when a world is loaded.
        GlassMenu.shadow(g,box.x(),box.y(),box.width(),box.height());
        GlassMenu.panel(g,box.x(),box.y(),box.width(),box.height(),12,GlassMenu.WINDOW_FILL,GlassMenu.WINDOW_BORDER);
        drag.render(this,g,mouseX,mouseY,box.x(),box.y(),box.width());
        GlassMenu.heading(g,aura?"KillAura":"TriggerBot",box.x()+12,box.y()+12,0xfffcf7ff);
        GlassMenu.text(g,font.plainSubstrByWidth(ModuleHelp.text(aura?"KillAura":"TriggerBot"),box.width()-24),
                box.x()+12,box.y()+(box.height()>=260?31:27),0xffddcbea);
        int pages=box.pages(controlCount);
        if (pages>1) g.drawCenteredString(font,(page+1)+"/"+pages,box.x()+box.width()-51,box.y()+16,0xfff5e9ff);
        String help = notice;
        int y=box.helpY();
        for (var line:font.split(Component.literal(help),box.width()-24)) {
            if (y+9>box.footerY()-5) break;
            g.drawString(font,line,box.x()+12,y,notice.isEmpty()?0xffdccae8:0xffffb5c1,false);
            y+=11;
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