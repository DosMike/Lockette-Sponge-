package de.dosmike.sponge.lockette;

import java.util.List;
import java.util.UUID;

import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class Lock {
	
	List<UUID> permitted;
	UUID owner;
	Location<World> target; //this value is reflected in the parents lock map
	
	Lock() {}
	public Lock(Location<World> target, UUID owner, List<UUID> permitted) {
		this.owner = owner;
		this.permitted = permitted;
		this.target = target;
	}
	
	public boolean isOwner(UUID player) {
		return owner.equals(player);
	}
	
	/** In order to access the target the source has to have access.<br>
	 * Access to the target is given if the following statement is true:
	 * <pre>hasPermission(bypassPermission) || !isLocked() || hasAccess(source)</pre> */
	public boolean hasAccess(UUID player) {
		return owner.equals(player)||permitted.contains(player);
	}
	
	/** If the lock is unlocked anyone will be able to access the target<br>
	 * otherwise only the permitted list can interact with the target.<br>
	 * Access to the target is given if the following statement is true:
	 * <pre>hasPermission(bypassPermission) || !isLocked() || hasAccess(source)</pre> */
	public boolean isLocked() {
		return true;
	}
	
	boolean canBypass(UUID player) {
		return Lockette.getInstance().userStorage.get().get(player).map(u->u.hasPermission("dosmike.lockette.check.if.user.is.op")).orElse(false);
	}
	
	public Location<World> getTarget() {
		return target;
	}
	
	public boolean equals(Object object) {
		if (!(object instanceof Lock)) return false;
		Lock other = (Lock)object;
		return other.target.equals(target) && 
				((owner == null && other.owner == null) || (owner != null && owner.equals(other.owner))) &&
				(permitted.size() == other.permitted.size() && permitted.containsAll(other.permitted));
	}
}
