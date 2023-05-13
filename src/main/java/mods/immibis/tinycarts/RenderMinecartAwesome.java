package mods.immibis.tinycarts;

import net.minecraft.client.renderer.entity.RenderMinecart;
import net.minecraft.entity.item.EntityMinecart;
import net.minecraft.util.ResourceLocation;

public class RenderMinecartAwesome extends RenderMinecart {
	private static final ResourceLocation texture = new ResourceLocation("tinycarts", "textures/entity/minecart.png");
	
	@Override
	protected ResourceLocation getEntityTexture(EntityMinecart par1EntityMinecart) {
		return texture;
	}
}
