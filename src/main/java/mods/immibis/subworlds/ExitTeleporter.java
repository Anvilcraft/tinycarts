package mods.immibis.subworlds;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityLivingBase;
import net.minecraft.world.Teleporter;
import net.minecraft.world.WorldServer;

public class ExitTeleporter extends Teleporter {
	public WorldServer world;
	public double x, y, z;
	
	public ExitTeleporter(WorldServer par1WorldServer, double x, double y, double z) {
		super(par1WorldServer);
		world = par1WorldServer;
		this.x = x;
		this.y = y;
		this.z = z;
	}
	
	@Override
	public void placeInPortal(Entity par1Entity, double par2, double par4, double par6, float par8) {
		if(par1Entity instanceof EntityLivingBase)
			((EntityLivingBase)par1Entity).setPositionAndUpdate(x, y, z);
		else
			par1Entity.setPosition(x, y, z);
	}
}
