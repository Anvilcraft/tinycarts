package mods.immibis.subworlds.dw;


import java.util.Collections;
import java.util.List;
import java.util.Map;

import mods.immibis.subworlds.SubWorldsMod;
import net.minecraft.world.World;
import net.minecraftforge.common.ForgeChunkManager;
import net.minecraftforge.common.ForgeChunkManager.LoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.OrderedLoadingCallback;
import net.minecraftforge.common.ForgeChunkManager.Ticket;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class ChunkLoadingCallback implements LoadingCallback, OrderedLoadingCallback {

	@Override
	public void ticketsLoaded(List<Ticket> tickets, World world) {
	}
	
	@Override
	public List<Ticket> ticketsLoaded(List<Ticket> tickets, World world, int maxTicketCount) {
		// do not keep chunks loaded through world reloads
		return Collections.emptyList();
	}
	
	public ChunkLoadingCallback() {
		try {
			boolean overridesEnabled = ReflectionHelper.getPrivateValue(ForgeChunkManager.class, null, "overridesEnabled");
			
			Map<String, Integer> ticketConstraints = ReflectionHelper.getPrivateValue(ForgeChunkManager.class, null, "ticketConstraints");
			Map<String, Integer> chunkConstraints = ReflectionHelper.getPrivateValue(ForgeChunkManager.class, null, "chunkConstraints");
			
			if(!overridesEnabled) {
				ticketConstraints.clear();
				chunkConstraints.clear();
				ReflectionHelper.findField(ForgeChunkManager.class, "overridesEnabled").set(null, true);
			}
			
			String modid = SubWorldsMod.MODID;
			
			ticketConstraints.put(modid, Integer.MAX_VALUE);
			chunkConstraints.put(modid, Integer.MAX_VALUE);
			
		} catch(Exception e) {
			throw new RuntimeException(e);
		}
		
		ForgeChunkManager.setForcedChunkLoadingCallback(SubWorldsMod.INSTANCE, this);
	}

}
