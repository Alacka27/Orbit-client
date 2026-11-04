package dev.alacka.orbit.profiles;
import dev.alacka.orbit.menu.GlassMenu;
import dev.alacka.orbit.menu.PlayerIdentity;
import com.mojang.authlib.GameProfile;
import java.nio.charset.StandardCharsets;
import java.util.UUID;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.Button;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.network.chat.Component;
public final class ProfileRowButton extends Button {
    private final String name,status;private final boolean launcher,selected;
    public ProfileRowButton(int x,int y,int w,int h,String name,boolean launcher,boolean selected,String status,OnPress action){
        super(x,y,w,h,Component.literal(name+". "+status),action,DEFAULT_NARRATION);this.name=name;this.launcher=launcher;this.selected=selected;this.status=status;
    }
    @Override protected void renderContents(GuiGraphics g,int mx,int my,float delta){
        GlassMenu.panel(g,getX(),getY(),getWidth(),getHeight(),6,selected?0x9b634281:isHoveredOrFocused()?0x88513c67:0x6849365e,selected?0xa9bd90e2:0x40987bb0);
        var skin=launcher?PlayerIdentity.skin():DefaultPlayerSkin.get(new GameProfile(UUID.nameUUIDFromBytes(("OfflinePlayer:"+name).getBytes(StandardCharsets.UTF_8)),name));
        PlayerFaceRenderer.draw(g,skin,getX()+8,getY()+(getHeight()-24)/2,24);
        var font=Minecraft.getInstance().font;
        GlassMenu.heading(g,font.plainSubstrByWidth(name,Math.max(40,getWidth()-145)),getX()+42,getY()+8,0xfffcf4ff);
        GlassMenu.text(g,launcher?"Аккаунт лаунчера":"Локальный профиль",getX()+42,getY()+21,0xffd9c7e8);
        GlassMenu.text(g,status,getX()+getWidth()-font.width(status)-9,getY()+(getHeight()-9)/2,0xffe9dcf4);
    }
}
