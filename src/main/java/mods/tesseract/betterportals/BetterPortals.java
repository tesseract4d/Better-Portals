package mods.tesseract.betterportals;

import mods.tesseract.betterportals.block.BlockBetterPortal;
import net.minecraft.block.Block;
import net.minecraft.entity.Entity;
import net.minecraftforge.common.MinecraftForge;
import net.minecraftforge.event.RegistryEvent;
import net.minecraftforge.event.entity.living.LivingEvent;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.eventhandler.SubscribeEvent;
import org.apache.logging.log4j.Logger;

@Mod(name = "BetterPortals", modid = "betterportals")
@Mod.EventBusSubscriber
public class BetterPortals {
    public static Logger log;

    @Mod.EventHandler
    public void preInit(FMLPreInitializationEvent e) {
        log = e.getModLog();
        MinecraftForge.EVENT_BUS.register(this);
    }

    @SubscribeEvent
    public void registerBlocks(RegistryEvent.Register<Block> e) {
        e.getRegistry().register(new BlockBetterPortal().setHardness(-1.0F).setLightLevel(0.75F).setRegistryName("minecraft", "portal"));
    }

    @SubscribeEvent
    public static void onLivingUpdtaeEvent(LivingEvent.LivingUpdateEvent e) {
        Entity f = e.getEntityLiving();
        if (!f.inPortal && f.timeUntilPortal < 0) {
            f.timeUntilPortal = 0;
        }
    }
}
