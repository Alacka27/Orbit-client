package dev.alacka.orbit.menu;
import com.mojang.authlib.GameProfile;
import dev.alacka.orbit.profiles.ProfileLaunch;
import dev.alacka.orbit.profiles.ProfileStore;
import java.util.UUID;
import java.util.function.Supplier;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.components.PlayerFaceRenderer;
import net.minecraft.client.resources.DefaultPlayerSkin;
import net.minecraft.world.entity.player.PlayerSkin;
/** Real current identity; never implies that an offline profile has authenticated. */
public final class PlayerIdentity {
    private static UUID id;private static Supplier<PlayerSkin> lookup;
    private PlayerIdentity(){}
    public static PlayerSkin skin() {
        var m=Minecraft.getInstance();if(m.player!=null)return m.player.getSkin();
        var user=m.getUser();
        if(!user.getProfileId().equals(id)||lookup==null){
            id=user.getProfileId();var profile=new GameProfile(id,user.getName());
            lookup=ProfileLaunch.active().equals(ProfileStore.LAUNCHER)?m.getSkinManager().createLookup(profile,true):()->DefaultPlayerSkin.get(profile);
        }
        return lookup.get();
    }
    public static void draw(GuiGraphics g,int x,int y,int width) {
        var m=Minecraft.getInstance();PlayerFaceRenderer.draw(g,skin(),x,y+2,20);
        GlassMenu.text(g,m.font.plainSubstrByWidth(m.getUser().getName(),Math.max(20,width-26)),x+26,y+2,0xfffcf6ff);
        String mode=ProfileLaunch.active().equals(ProfileStore.LAUNCHER)?"Аккаунт лаунчера":"Локальный профиль";
        g.pose().pushMatrix();g.pose().translate(x+26,y+14);g.pose().scale(.8f,.8f);
        GlassMenu.text(g,m.font.plainSubstrByWidth(mode,Math.max(20,(int)((width-26)/.8))),0,0,0xffd9cbe7);g.pose().popMatrix();
    }
}
