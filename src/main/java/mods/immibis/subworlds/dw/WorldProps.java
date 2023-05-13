package mods.immibis.subworlds.dw;

import net.minecraft.nbt.NBTTagCompound;
import net.minecraft.world.chunk.IChunkProvider;

// POD structure with extra info about a DW world
public final class WorldProps {
	public int xsize = 48;
	public int ysize = 48;
	public int zsize = 48;
	
	// class must have constructor taking (WorldServer, DWWorldProvider)
	public Class<? extends IChunkProvider> generatorClass = DWChunkGenerator.class;
	
	public void read(NBTTagCompound tag) {
		xsize = tag.getInteger("xsize");
		ysize = tag.getInteger("ysize");
		zsize = tag.getInteger("zsize");
		try {
			generatorClass = Class.forName(tag.getString("genclass")).asSubclass(IChunkProvider.class);
		} catch(ClassNotFoundException e) {
			generatorClass = DWChunkGenerator.class;
		}
	}
	
	public void write(NBTTagCompound tag) {
		tag.setInteger("xsize", xsize);
		tag.setInteger("ysize", ysize);
		tag.setInteger("zsize", zsize);
		tag.setString("genclass", generatorClass.getName());
	}
}
