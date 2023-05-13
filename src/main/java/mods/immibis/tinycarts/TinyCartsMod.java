package mods.immibis.tinycarts;

import java.lang.ref.WeakReference;

import mods.immibis.cobaltite.AssignedBlock;
import mods.immibis.cobaltite.AssignedItem;
import mods.immibis.cobaltite.CobaltiteMod;
import mods.immibis.cobaltite.ModBase;
import mods.immibis.core.api.APILocator;
import mods.immibis.core.api.FMLModInfo;
import mods.immibis.subworlds.ExitTeleporter;
import mods.immibis.subworlds.dw.DWWorldProvider;
import net.minecraft.entity.Entity;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.init.Items;
import net.minecraft.item.ItemStack;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.management.ServerConfigurationManager;
import net.minecraft.util.ChatComponentText;
import net.minecraft.world.WorldServer;
import cpw.mods.fml.client.registry.RenderingRegistry;
import cpw.mods.fml.common.Mod;
import cpw.mods.fml.common.Mod.EventHandler;
import cpw.mods.fml.common.event.FMLInitializationEvent;
import cpw.mods.fml.common.event.FMLPreInitializationEvent;
import cpw.mods.fml.common.event.FMLServerStartingEvent;
import cpw.mods.fml.common.registry.EntityRegistry;
import cpw.mods.fml.common.registry.GameRegistry;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

/**
 * IMPORTANT SMALL DETAIL: -X EITHER FORWARD OR BACKWARD, -Z IS SIDEWAYS.
 */
@Mod(modid="TinyCarts", name="TinyCarts", version="0.2", dependencies="required-after:SubWorlds")
@CobaltiteMod()
@FMLModInfo(authors="immibis", description="", url="")
public class TinyCartsMod extends ModBase {
	@AssignedItem(id="awesomecart")
	public static ItemMinecartAwesome itemCart;
	
	@AssignedBlock(id="transparentbedrock")
	public static BlockTransparentBedrock transparentBedrock;
	
	@EventHandler
	public void init(FMLInitializationEvent evt) {
		EntityRegistry.registerModEntity(EntityMinecartAwesome.class, "tinycarts.cart", 0, this, 50, 1, true);
		EntityRegistry.registerModEntity(EntityMinecartAwesome.DWSubEntity.class, "tinycarts.cart.dwsub", 1, this, 50, 1, true);
		
		APILocator.getNetManager().listen(new NetworkHandler());
	}
	
	@Override
	protected void addRecipes() throws Exception {
		GameRegistry.addRecipe(new ItemStack(itemCart), "D D", "DDD", 'D', Items.diamond);
	}
	
	@EventHandler
	public void onServerStart(FMLServerStartingEvent evt) {
		evt.registerServerCommand(new CommandExitCart());
	}
	
	@Override
	@SideOnly(Side.CLIENT)
	protected void clientInit() throws Exception {
		RenderingRegistry.registerEntityRenderingHandler(EntityMinecartAwesome.class, new RenderMinecartAwesome());
	}
	
	@EventHandler
	public void __init(FMLInitializationEvent evt) {super._init(evt);}
	@EventHandler
	public void __preinit(FMLPreInitializationEvent evt) {super._preinit(evt);}

	private static long lastRFCWarnTime = 0;
	public static void removeFromCart(Entity ent, boolean ignoreIfNotInCart) {
		if(ent.worldObj.isRemote) {
			assert false : "removeFromCart - on client world!";
			return;
		}
		
		if(!(ent.worldObj.provider instanceof DWWorldProvider)) {
			assert ignoreIfNotInCart : "removeFromCart - not in DW world!";
			return;
		}
		
		DWWorldProvider wp = (DWWorldProvider)ent.worldObj.provider;
		if(wp.props.generatorClass != InteriorChunkGen.class) {
			assert ignoreIfNotInCart : "removeFromCart - not in TinyCart world!";
			return;
		}
		
		// XXX this way of getting the entity is hacky
		WeakReference<EntityMinecartAwesome.DWSubEntity> dwsub_ref = EntityMinecartAwesome.entitiesByInternalID.get(wp.dimensionId);
		
		EntityMinecartAwesome.DWSubEntity dwsub = dwsub_ref == null ? null : dwsub_ref.get();
		if(dwsub == null) {
			if(lastRFCWarnTime - System.nanoTime() > 5000) {
				System.out.println("[TinyCarts] Potential problem - entity is leaving a cart, but we can't find the outside of the cart! Entity is "+ent+", internal world ID is "+ent.worldObj.provider.dimensionId+". This message will only be printed once every 5 seconds, maximum.");
				lastRFCWarnTime = System.nanoTime();
			}
			if(ent instanceof EntityPlayer)
				((EntityPlayer)ent).addChatMessage(new ChatComponentText("Uh oh! Outside of the cart could not be found! Cannot teleport you, sorry."));
			return;
		}
		
		WorldServer outsideWorld = (WorldServer)dwsub.worldObj;
		double x = dwsub.posX;
		double y = dwsub.posY;
		double z = dwsub.posZ;
		
		x += (outsideWorld.rand.nextDouble() + 1) * (outsideWorld.rand.nextBoolean() ? -1 : 1);
		z += (outsideWorld.rand.nextDouble() + 1) * (outsideWorld.rand.nextBoolean() ? -1 : 1);
		
		ServerConfigurationManager scm = MinecraftServer.getServer().getConfigurationManager();
		if(ent instanceof EntityPlayerMP)
			scm.transferPlayerToDimension((EntityPlayerMP)ent, outsideWorld.provider.dimensionId, new ExitTeleporter(outsideWorld, x, y, z));
		//else
		//	scm.transferEntityToWorld(ent, par2, par3WorldServer, par4WorldServer)
        
	}
}
