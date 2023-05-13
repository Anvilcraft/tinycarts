package mods.immibis.subworlds.dw;

import java.util.Set;

import net.minecraft.entity.Entity;
import net.minecraft.entity.EntityTracker;
import net.minecraft.entity.EntityTrackerEntry;
import net.minecraft.entity.player.EntityPlayerMP;
import net.minecraft.util.IntHashMap;
import net.minecraft.world.WorldServer;
import cpw.mods.fml.relauncher.ReflectionHelper;

public class DWUtils {
	@SuppressWarnings("unchecked")
	public static Set<EntityPlayerMP> getTrackingPlayers(Entity ent) {
		EntityTracker t = ((WorldServer)ent.worldObj).getEntityTracker();
		
		IntHashMap trackedEntityIDs = ReflectionHelper.getPrivateValue(EntityTracker.class, t, 3);
		EntityTrackerEntry entry = (EntityTrackerEntry)trackedEntityIDs.lookup(ent.getEntityId());
		return entry.trackingPlayers;
	}
}
