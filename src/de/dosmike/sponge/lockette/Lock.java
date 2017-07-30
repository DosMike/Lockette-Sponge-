package de.dosmike.sponge.lockette;

import java.util.List;
import java.util.UUID;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class Lock {
	List<UUID> permitted;
	
	Location<World> target; //this value is reflected in the parents lock map
	
	Lock() {}
	Lock(Location<World> target, List<UUID> permitted) {
		this.permitted = permitted;
		this.target = target;
	}
	
	/** In order to access the target the source has to have access.<br>
	 * Access to the target is given if the following statement is true:
	 * <pre>hasPermission(bypassPermission) || !isLocked() || hasAccess(source)</pre> */
	public boolean hasAccess(Player player) {
		return permitted.contains(player.getUniqueId());
	}
	
	/** If the lock is unlocked anyone will be able to access the target<br>
	 * otherwise only the permitted list can interact with the target.<br>
	 * Access to the target is given if the following statement is true:
	 * <pre>hasPermission(bypassPermission) || !isLocked() || hasAccess(source)</pre> */
	public boolean isLocked() {
		return true;
	}
	
	boolean canBypass(Player player) {
		return player.hasPermission("dosmike.lockette.check.if.user.is.op");
	}
	
	public Location<World> getTarget() {
		return target;
	}
}
