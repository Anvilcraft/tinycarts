package mods.immibis.subworlds.dw;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

public class DWTeleporter extends Teleporter {

	public WorldServer world;
	
	public DWTeleporter(WorldServer par1WorldServer) {
		super(par1WorldServer);
		world = par1WorldServer;
	}
	
	@Override
	public void placeInPortal(Entity par1Entity, double par2, double par4, double par6, float par8) {
		DWWorldProvider provider = (DWWorldProvider)world.provider;
		
		double x = provider.props.xsize/2;
		double y = 5;
		double z = provider.props.zsize/2;
		
		if(par1Entity instanceof EntityLivingBase)
			((EntityLivingBase)par1Entity).setPositionAndUpdate(x, y, z);
		else
			par1Entity.setPosition(x, y, z);
	}

}
