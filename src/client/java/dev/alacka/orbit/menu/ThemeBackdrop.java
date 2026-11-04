package dev.alacka.orbit.menu;

import com.mojang.blaze3d.platform.NativeImage;
import dev.alacka.orbit.OrbitClient;
import dev.alacka.orbit.config.ClientSettings;
import dev.alacka.orbit.config.OrbitSettings;
import java.util.LinkedHashMap;
import net.fabricmc.fabric.api.resource.ResourceManagerHelper;
import net.fabricmc.fabric.api.resource.SimpleSynchronousResourceReloadListener;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.texture.DynamicTexture;
import net.minecraft.resources.Identifier;
import net.minecraft.server.packs.PackType;
import net.minecraft.server.packs.resources.ResourceManager;

/** One reusable texture; recolouring happens only when the accent changes. */
public final class ThemeBackdrop {
    private static final Identifier ORIGINAL=Identifier.fromNamespaceAndPath("orbit","clouds.png");
    private static final Identifier COLOURED=Identifier.fromNamespaceAndPath("orbit","runtime/theme_clouds");
    private static volatile int generation;
    private static int loadedGeneration=-1, colour=-1, width, height;
    private static int[] indices, palette;
    private static boolean registered;
    private ThemeBackdrop() {}
    public static void initialize(){
        ResourceManagerHelper.get(PackType.CLIENT_RESOURCES).registerReloadListener(new SimpleSynchronousResourceReloadListener(){
            @Override public Identifier getFabricId(){return Identifier.fromNamespaceAndPath("orbit","theme_background");}
            @Override public void onResourceManagerReload(ResourceManager manager){generation++;}
        });
    }
    public static Identifier texture(){
        Minecraft m=Minecraft.getInstance();int accent=OrbitSettings.theme().accent();
        if(loadedGeneration!=generation){
            if(registered)m.getTextureManager().release(COLOURED);
            registered=false;indices=null;palette=null;colour=-1;loadedGeneration=generation;
        }
        if(accent==ClientSettings.DEFAULT.accent())return ORIGINAL;
        if(colour==accent)return registered?COLOURED:ORIGINAL;
        try{
            if(indices==null)loadPalette(m);
            int[] colours=new int[palette.length];
            for(int i=0;i<palette.length;i++)colours[i]=UiPalette.cloudColor(palette[i],accent);
            NativeImage image=new NativeImage(width,height,false);
            for(int y=0,i=0;y<height;y++)for(int x=0;x<width;x++,i++)image.setPixel(x,y,colours[indices[i]]);
            if(registered)m.getTextureManager().release(COLOURED);
            registered=false;
            try{m.getTextureManager().register(COLOURED,new DynamicTexture(()->"Orbit theme background",image));registered=true;}
            catch(RuntimeException e){image.close();throw e;}
            colour=accent;return COLOURED;
        }catch(Exception e){
            colour=accent;OrbitClient.LOGGER.warn("[Orbit] Theme background unavailable; original retained",e);return ORIGINAL;
        }
    }
    private static void loadPalette(Minecraft m)throws java.io.IOException{
        try(var stream=m.getResourceManager().getResourceOrThrow(ORIGINAL).open();NativeImage image=NativeImage.read(stream)){
            width=image.getWidth();height=image.getHeight();int[] pixels=image.getPixels();indices=new int[pixels.length];
            LinkedHashMap<Integer,Integer> colours=new LinkedHashMap<>();
            for(int i=0;i<pixels.length;i++){
                Integer index=colours.get(pixels[i]);
                if(index==null){index=colours.size();colours.put(pixels[i],index);}
                indices[i]=index;
            }
            palette=colours.keySet().stream().mapToInt(Integer::intValue).toArray();
        }
    }
}
