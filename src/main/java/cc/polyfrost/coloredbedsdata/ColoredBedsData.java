package cc.polyfrost.coloredbedsdata;

import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;

@net.minecraftforge.fml.common.Mod(modid = ColoredBedsData.MODID, name = ColoredBedsData.NAME, version = ColoredBedsData.VERSION)
public class ColoredBedsData {
    @net.minecraftforge.fml.common.Mod.Instance("@ID@")
    public static ColoredBedsData INSTANCE;
    public static final String MODID = "@ID@";
    public static final String NAME = "@NAME@";
    public static final String VERSION = "@VER@";

    // Register the config and commands.
    @Mod.EventHandler
    public void onInit(FMLInitializationEvent event) {
        BedLocationHandler.INSTANCE.initialize();
    }
}
