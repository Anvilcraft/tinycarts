package mods.immibis.subworlds.mws;


import mods.immibis.subworlds.dw.DWWorldProvider;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.network.NetHandlerPlayClient;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.world.EnumDifficulty;
import net.minecraft.world.WorldSettings;
import cpw.mods.fml.relauncher.ReflectionHelper;
import cpw.mods.fml.relauncher.Side;
import cpw.mods.fml.relauncher.SideOnly;

@SideOnly(Side.CLIENT)
public class MWSClientWorld extends WorldClient {
	public int dimension;
	
	//private Chunk blankChunk;
	
	public boolean isDead = false;

	public MWSClientWorld(int dimID) {
		// need a valid NetClientHandler for construction so grab one from the real world
		super((NetHandlerPlayClient)ReflectionHelper.getPrivateValue(WorldClient.class, Minecraft.getMinecraft().theWorld, 0),
				new WorldSettings(Minecraft.getMinecraft().theWorld.getWorldInfo()),
				dimID,
				EnumDifficulty.PEACEFUL, // difficulty
				Minecraft.getMinecraft().theWorld.theProfiler);
		
		// but now we don't need it any more
		ReflectionHelper.setPrivateValue(WorldClient.class, this, null, 0);
		
		dimension = dimID;
		
		isRemote = true;
		
		Minecraft mc = Minecraft.getMinecraft();
		
		render = new RenderGlobal(mc); /*{
			@Override
			public void renderAllRenderLists(int par1, double par2) {
				super.renderAllRenderLists(par1, par2);
				System.out.println(par1+" "+par2+" "+((java.util.List)ReflectionHelper.getPrivateValue(RenderGlobal.class, this, "glRenderLists")).size());
			}
		};*/
		final EntityRenderer normalER = mc.entityRenderer;
		erender = new EntityRenderer(mc, mc.getResourceManager()) {
			
			// use the normal lightmap, not our separate one
			@Override public void disableLightmap(double par1) {normalER.disableLightmap(par1);}
			@Override public void enableLightmap(double par1) {normalER.enableLightmap(par1);}
		};
		
		render.setWorldAndLoadRenderers(this);
		
		//blankChunk = getChunkFromChunkCoords(0, 0);
		
		if(provider instanceof DWWorldProvider) {
			DWWorldProvider pv = (DWWorldProvider)provider;
			
			for(int x = -1; x <= pv.props.xsize; x++)
				for(int z = -1; z <= pv.props.zsize; z++)
					doPreChunk(x, z, true);
		}
	}
	
	public RenderGlobal render;
	public EntityRenderer erender;

	public void zeppelinTick() {
		updateEntities();
	}

	public void unloadChunk(int x, int z) {
		doPreChunk(x, z, false);
	}
}
