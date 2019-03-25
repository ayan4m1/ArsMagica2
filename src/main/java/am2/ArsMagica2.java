package am2;

import am2.api.ArsMagicaAPI;
import am2.api.affinity.Affinity;
import am2.common.CommonProxy;
import am2.common.commands.CommandArsMagica;
import am2.common.config.AMConfig;
import am2.common.config.SpellPartConfiguration;
import am2.common.packet.MessageBoolean;
import am2.common.packet.MessageCapabilities;
import net.minecraftforge.common.ForgeModContainer;
import net.minecraftforge.fluids.FluidRegistry;
import net.minecraftforge.fml.common.Mod;
import net.minecraftforge.fml.common.Mod.EventHandler;
import net.minecraftforge.fml.common.Mod.Instance;
import net.minecraftforge.fml.common.SidedProxy;
import net.minecraftforge.fml.common.event.FMLInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPostInitializationEvent;
import net.minecraftforge.fml.common.event.FMLPreInitializationEvent;
import net.minecraftforge.fml.common.event.FMLServerStartingEvent;
import net.minecraftforge.fml.common.network.NetworkRegistry;
import net.minecraftforge.fml.common.network.simpleimpl.SimpleNetworkWrapper;
import net.minecraftforge.fml.relauncher.Side;

import java.io.File;


@Mod(modid=ArsMagica2.MODID, version=ArsMagica2.VERSION, guiFactory=ArsMagica2.GUIFACTORY, canBeDeactivated=false, acceptedMinecraftVersions = "[1.12.0,1.12.2]")
public class ArsMagica2 {

	public static final String MODID = "arsmagica2";
	public static final String VERSION = "GRADLE:VERSION";
	public static final String GUIFACTORY = "am2.client.config.AMGuiFactory";
	public static SimpleNetworkWrapper network;

	@SidedProxy(clientSide="am2.client.ClientProxy", serverSide="am2.common.CommonProxy", modId=MODID)
	public static CommonProxy proxy;

	@Instance(MODID)
	public static ArsMagica2 instance;
	public static AMConfig config;
	public static SpellPartConfiguration disabledSkills;
	private File configDir;

	static {
		new ArsMagicaAPI();
		Affinity.registerAffinities();
		if (!FluidRegistry.isUniversalBucketEnabled())
			FluidRegistry.enableUniversalBucket();
	}

	@EventHandler
	public void preInit (FMLPreInitializationEvent e) {
		configDir = new File(e.getModConfigurationDirectory(), "ArsMagica2");
		config = new AMConfig(new File(configDir, "am2.cfg"));
		//config = new AMConfig(new File(e.getModConfigurationDirectory() + "\\ArsMagica2\\am2.cfg"));
		disabledSkills = new SpellPartConfiguration(new File(configDir, "skills.cfg"));
		proxy.preInit();
		network = NetworkRegistry.INSTANCE.newSimpleChannel("AM2");
		network.registerMessage(MessageBoolean.IceBridgeHandler.class, MessageBoolean.class, 1, Side.SERVER);
		network.registerMessage(MessageCapabilities.class, MessageCapabilities.class, 3, Side.SERVER);
	}

	@EventHandler
	public void init (FMLInitializationEvent e) {
		proxy.init();
	}

	@EventHandler
	public void postInit (FMLPostInitializationEvent e) {
		proxy.postInit();
	}

	@EventHandler
	public void serverLoad(FMLServerStartingEvent e) {
		e.registerServerCommand(new CommandArsMagica());
	}



	public String getVersion() {
		return VERSION;
	}

}
