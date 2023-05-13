package mods.immibis.subworlds.dw;


import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;

import mods.immibis.subworlds.mws.MWSClientWorld;
import mods.immibis.subworlds.mws.MWSManager;
import net.minecraft.client.Minecraft;
import net.minecraft.client.multiplayer.WorldClient;
import net.minecraft.client.particle.EffectRenderer;
import net.minecraft.client.renderer.EntityRenderer;
import net.minecraft.client.renderer.RenderGlobal;
import net.minecraft.client.renderer.RenderHelper;
import net.minecraft.client.renderer.culling.ClippingHelperImpl;
import net.minecraft.client.renderer.culling.Frustrum;
import net.minecraft.client.renderer.entity.RenderEntity;
import net.minecraft.client.renderer.texture.TextureMap;
import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.entity.player.EntityPlayer;
import net.minecraftforge.client.ForgeHooksClient;
import net.minecraftforge.client.MinecraftForgeClient;

import org.lwjgl.opengl.GL11;

public class DWEntityRenderer extends RenderEntity {
	private static List clipperStack = new ArrayList();
	private static Field clipperInstanceField;
	static {
		clipperInstanceField = ClippingHelperImpl.class.getDeclaredFields()[0];
		clipperInstanceField.setAccessible(true);
	}
	
	public static void pushClipper() {
		try {
			clipperStack.add(clipperInstanceField.get(null));
			clipperInstanceField.set(null, new ClippingHelperImpl());
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}
	
	public static void popClipper() {
		try {
			clipperInstanceField.set(null, clipperStack.remove(clipperStack.size() - 1));
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
	}

	@Override
	public void doRender(Entity ent_, double x, double y, double z, float yaw, float partialTick) {
		
		DWEntity ent = (DWEntity)ent_;
		
		double entX = ent.prevPosX + (ent.posX - ent.prevPosX) * partialTick;
		double entY = ent.prevPosY + (ent.posY - ent.prevPosY) * partialTick;
		double entZ = ent.prevPosZ + (ent.posZ - ent.prevPosZ) * partialTick;
		
		GL11.glPushMatrix();
		GL11.glTranslated(x, y, z);
		
		ent.applyGLRotation(partialTick);
		
		int dimID = ent.getEnclosedDimensionClient();
		
		MWSClientWorld subWorld = dimID == 0 ? null : MWSManager.getClientWorld(dimID);
		if(subWorld == null)
			super.doRender(ent_, 0, 0, 0, yaw, partialTick);
		else
		{
			//System.out.println(ent.hashCode()+"\t"+ent.entityId+" at "+entX+" "+entY+" "+entZ+" "+ent.world.getBlockId(5, 0, 5));
			//System.out.println("Render world "+ent.world+" for "+ent.zeppelinID);
			//GL11.glTranslated(x - entX, y - entY, z - entZ);
			//GL11.glTranslated(entX - x, entY - y, entZ - z);
			
			DWWorldProvider pv = (DWWorldProvider)subWorld.provider;
			GL11.glTranslatef(-pv.props.xsize/2.0f, 0, -pv.props.zsize/2.0f);
			
			renderClientWorld(subWorld, partialTick, entX, entY, entZ, 0, 0, 0);
		}
		GL11.glPopMatrix();
		
	}

	// e_*: unused.
	// pl_*: centre of rendered area (we pretend the player is here)
	// pl_* is also the point in the world that will be rendered at 0,0,0.
	public static void renderClientWorld(MWSClientWorld world, float partialTick, double e_x, double e_y, double e_z, double pl_x, double pl_y, double pl_z) {
		Minecraft mc = Minecraft.getMinecraft();
		
		GL11.glColor3f(1, 1, 1);
		
		RenderGlobal oldRG = mc.renderGlobal;
		EntityRenderer oldER = mc.entityRenderer;
		WorldClient oldWorld = mc.theWorld;
		mc.renderGlobal = world.render;
		mc.entityRenderer = world.erender;
		mc.theWorld = world;
		int oldPass = MinecraftForgeClient.getRenderPass();
		pushClipper();
		

		// Pretend the player is at the origin (of the zeppelin world)
        EntityLivingBase pl = mc.renderViewEntity;
        double realX = pl.posX, realY = pl.posY, realZ = pl.posZ;
        double realPrevX = pl.prevPosX, realPrevY = pl.prevPosY, realPrevZ = pl.prevPosZ;
        double realLastX = pl.lastTickPosX, realLastY = pl.lastTickPosY, realLastZ = pl.lastTickPosZ;
        pl.posX = pl.prevPosX = pl.lastTickPosX = pl_x;
        pl.posY = pl.prevPosY = pl.lastTickPosY = pl_y;
        pl.posZ = pl.prevPosZ = pl.lastTickPosZ = pl_z;
        
        er_renderWorld(mc.entityRenderer, partialTick, 0);
        
        popClipper();
        ForgeHooksClient.setRenderPass(oldPass);
        mc.theWorld = oldWorld;
		mc.entityRenderer = oldER;
		mc.renderGlobal = oldRG;
		pl.posX = realX;
        pl.posY = realY;
        pl.posZ = realZ;
        pl.prevPosX = realPrevX;
        pl.prevPosY = realPrevY;
        pl.prevPosZ = realPrevZ;
        pl.lastTickPosX = realLastX;
        pl.lastTickPosY = realLastY;
        pl.lastTickPosZ = realLastZ;
	}

	private static void er_renderWorld(EntityRenderer self, float par1, long par2) {
		Minecraft mc = Minecraft.getMinecraft();
		
		mc.mcProfiler.startSection("lightTex");
		
		/*if (self.lightmapUpdateNeeded)
        {
            self.updateLightmap(par1);
        }*/

        GL11.glEnable(GL11.GL_CULL_FACE);
        GL11.glEnable(GL11.GL_DEPTH_TEST);

        if (mc.renderViewEntity == null)
        {
            mc.renderViewEntity = mc.thePlayer;
        }

        mc.mcProfiler.endStartSection("pick");
        //self.getMouseOver(par1);
        EntityLivingBase entitylivingbase = mc.renderViewEntity;
        RenderGlobal renderglobal = mc.renderGlobal;
        EffectRenderer effectrenderer = mc.effectRenderer;
        double d0 = entitylivingbase.lastTickPosX + (entitylivingbase.posX - entitylivingbase.lastTickPosX) * (double)par1;
        double d1 = entitylivingbase.lastTickPosY + (entitylivingbase.posY - entitylivingbase.lastTickPosY) * (double)par1;
        double d2 = entitylivingbase.lastTickPosZ + (entitylivingbase.posZ - entitylivingbase.lastTickPosZ) * (double)par1;
        mc.mcProfiler.endStartSection("center");

        //for (int j = 0; j < 2; ++j)
        //{
            /*if (mc.gameSettings.anaglyph)
            {
                anaglyphField = j;

                if (anaglyphField == 0)
                {
                    GL11.glColorMask(false, true, true, false);
                }
                else
                {
                    GL11.glColorMask(true, false, false, false);
                }
            }*/

            mc.mcProfiler.endStartSection("clear");
            //GL11.glViewport(0, 0, mc.displayWidth, mc.displayHeight);
            //self.updateFogColor(par1);
            //GL11.glClear(GL11.GL_COLOR_BUFFER_BIT | GL11.GL_DEPTH_BUFFER_BIT);
            GL11.glEnable(GL11.GL_CULL_FACE);
            mc.mcProfiler.endStartSection("camera");
            //self.setupCameraTransform(par1, j);
            //ActiveRenderInfo.updateRenderInfo(mc.thePlayer, mc.gameSettings.thirdPersonView == 2);
            mc.mcProfiler.endStartSection("frustrum");
            ClippingHelperImpl.getInstance();

            /*if (mc.gameSettings.renderDistance < 2)
            {
                self.setupFog(-1, par1);
                mc.mcProfiler.endStartSection("sky");
                renderglobal.renderSky(par1);
            }*/

            //GL11.glEnable(GL11.GL_FOG);
            //self.setupFog(1, par1);

            if (mc.gameSettings.ambientOcclusion != 0)
            {
                GL11.glShadeModel(GL11.GL_SMOOTH);
            }

            
            mc.mcProfiler.endStartSection("culling");
            Frustrum frustrum = new Frustrum();
            frustrum.setPosition(d0, d1, d2);
            mc.renderGlobal.clipRenderersByFrustum(frustrum, par1);
            

            //if (j == 0)
            //{
                mc.mcProfiler.endStartSection("updatechunks");
            
                while (!mc.renderGlobal.updateRenderers(entitylivingbase, false) && par2 != 0L)
                {
                    long k = par2 - System.nanoTime();
            
                    if (k < 0L || k > 1000000000L)
                    {
                        break;
                    }
                }
            //}

            //if (entitylivingbase.posY < 128.0D)
            //{
            //    self.renderCloudsCheck(renderglobal, par1);
            //}

            
            mc.mcProfiler.endStartSection("prepareterrain");
            //self.setupFog(0, par1);
            //GL11.glEnable(GL11.GL_FOG);
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);
            RenderHelper.disableStandardItemLighting();
            mc.mcProfiler.endStartSection("terrain");
            renderglobal.sortAndRender(entitylivingbase, 0, (double)par1);
            GL11.glShadeModel(GL11.GL_FLAT);
            EntityPlayer entityplayer;

            if (self.debugViewDirection == 0)
            {
            	/* XXX TODO: enable entity rendering inside DW's. renderEntities uses statics that need to be reset afterwards. 
            	RenderHelper.enableStandardItemLighting();
                mc.mcProfiler.endStartSection("entities");
                ForgeHooksClient.setRenderPass(0);
                renderglobal.renderEntities(entitylivingbase.getPosition(par1), frustrum, par1);
                ForgeHooksClient.setRenderPass(0);
                */
                
                /* Forge: Moved down
                self.enableLightmap((double)par1);
                mc.mcProfiler.endStartSection("litParticles");
                effectrenderer.renderLitParticles(entitylivingbase, par1);
                RenderHelper.disableStandardItemLighting();
                self.setupFog(0, par1);
                mc.mcProfiler.endStartSection("particles");
                effectrenderer.renderParticles(entitylivingbase, par1);
                self.disableLightmap((double)par1);
                */

                /*if (mc.objectMouseOver != null && entitylivingbase.isInsideOfMaterial(Material.water) && entitylivingbase instanceof EntityPlayer && !mc.gameSettings.hideGUI)
                {
                    entityplayer = (EntityPlayer)entitylivingbase;
                    GL11.glDisable(GL11.GL_ALPHA_TEST);
                    mc.mcProfiler.endStartSection("outline");
                    if (!ForgeHooksClient.onDrawBlockHighlight(renderglobal, entityplayer, mc.objectMouseOver, 0, entityplayer.inventory.getCurrentItem(), par1))
                    {
                        renderglobal.drawSelectionBox(entityplayer, mc.objectMouseOver, 0, par1);
                    }
                    GL11.glEnable(GL11.GL_ALPHA_TEST);
                }*/
            }

            
            GL11.glDisable(GL11.GL_BLEND);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA);
            GL11.glDepthMask(true);
            //self.setupFog(0, par1);
            GL11.glEnable(GL11.GL_BLEND);
            GL11.glDisable(GL11.GL_CULL_FACE);
            mc.getTextureManager().bindTexture(TextureMap.locationBlocksTexture);

            if (mc.gameSettings.fancyGraphics)
            {
                mc.mcProfiler.endStartSection("water");

                if (mc.gameSettings.ambientOcclusion != 0)
                {
                    GL11.glShadeModel(GL11.GL_SMOOTH);
                }

                GL11.glColorMask(false, false, false, false);
                int l = renderglobal.sortAndRender(entitylivingbase, 1, (double)par1);
            	
            	
                /*if (mc.gameSettings.anaglyph)
                {
                	if (anaglyphField == 0)
                    {
                        GL11.glColorMask(false, true, true, true);
                    }
                    else
                    {
                        GL11.glColorMask(true, false, false, true);
                    }
                }
                else*/
                
                {
                    GL11.glColorMask(true, true, true, true);
                }

                if (l > 0)
                {
                    renderglobal.renderAllRenderLists(1, (double)par1);
                }

                GL11.glShadeModel(GL11.GL_FLAT);
            }
            else
            {
                mc.mcProfiler.endStartSection("water");
                renderglobal.sortAndRender(entitylivingbase, 1, (double)par1);
            }


            
            /* XXX TODO: enable entity rendering inside DW's. renderEntities uses statics that need to be reset afterwards. 
            if (self.debugViewDirection == 0) //Only render if render pass 0 happens as well.
            {
                RenderHelper.enableStandardItemLighting();
                mc.mcProfiler.endStartSection("entities");
                ForgeHooksClient.setRenderPass(1);
                renderglobal.renderEntities(entitylivingbase.getPosition(par1), frustrum, par1);
                ForgeHooksClient.setRenderPass(-1);
                RenderHelper.disableStandardItemLighting();
            } */

            GL11.glDepthMask(true);
            GL11.glEnable(GL11.GL_CULL_FACE);
            GL11.glDisable(GL11.GL_BLEND);
            
            RenderHelper.enableStandardItemLighting();

            /*if (self.cameraZoom == 1.0D && entitylivingbase instanceof EntityPlayer && !mc.gameSettings.hideGUI && mc.objectMouseOver != null && !entitylivingbase.isInsideOfMaterial(Material.water))
            {
                entityplayer = (EntityPlayer)entitylivingbase;
                GL11.glDisable(GL11.GL_ALPHA_TEST);
                mc.mcProfiler.endStartSection("outline");
                if (!ForgeHooksClient.onDrawBlockHighlight(renderglobal, entityplayer, mc.objectMouseOver, 0, entityplayer.inventory.getCurrentItem(), par1))
                {
                    renderglobal.drawSelectionBox(entityplayer, mc.objectMouseOver, 0, par1);
                }
                GL11.glEnable(GL11.GL_ALPHA_TEST);
            }*/

            //mc.mcProfiler.endStartSection("destroyProgress");
            //GL11.glEnable(GL11.GL_BLEND);
            //GL11.glBlendFunc(GL11.GL_SRC_ALPHA, GL11.GL_ONE);
            //renderglobal.drawBlockDamageTexture(Tessellator.instance, entitylivingbase, par1);
            //GL11.glDisable(GL11.GL_BLEND);
            //mc.mcProfiler.endStartSection("weather");
            //self.renderRainSnow(par1);
            //GL11.glDisable(GL11.GL_FOG);

            //if (entitylivingbase.posY >= 128.0D)
            //{
            //    self.renderCloudsCheck(renderglobal, par1);
            //}

            //Forge: Moved section from above, now particles are the last thing to render.
            /*self.enableLightmap((double)par1);
            mc.mcProfiler.endStartSection("litParticles");
            effectrenderer.renderLitParticles(entitylivingbase, par1);
            RenderHelper.disableStandardItemLighting();
            //self.setupFog(0, par1);
            mc.mcProfiler.endStartSection("particles");
            effectrenderer.renderParticles(entitylivingbase, par1);
            self.disableLightmap((double)par1);*/
            //Forge: End Move

            //mc.mcProfiler.endStartSection("FRenderLast");
            //ForgeHooksClient.dispatchRenderLast(renderglobal, par1);

            //mc.mcProfiler.endStartSection("hand");

            //if (self.cameraZoom == 1.0D)
            //{
            //    GL11.glClear(GL11.GL_DEPTH_BUFFER_BIT);
            //    self.renderHand(par1, j);
            //}

            //if (!mc.gameSettings.anaglyph)
            //{
                mc.mcProfiler.endSection();
                //return;
            //}
        //}

        //GL11.glColorMask(true, true, true, false);
        //mc.mcProfiler.endSection();
	}

}
