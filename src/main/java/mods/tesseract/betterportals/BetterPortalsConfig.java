package mods.tesseract.betterportals;

import net.minecraftforge.common.config.Config;
import net.minecraftforge.common.config.Config.Name;
import net.minecraftforge.common.config.Config.Comment;

@Config(modid = "betterportals")
public class BetterPortalsConfig {
    @Name(value = "Return Portal")
    @Comment(value = {"Enable Return Portal"})
    public static boolean RETURN_PORTAL = true;

    @Name(value = "Gateway Portal")
    @Comment(value = {"Enable Return Portal"})
    public static boolean GATEWAY_PORTAL = true;
}
