package de.dosmike.sponge.lockette;

import java.io.Serializable;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import ninja.leaping.configurate.objectmapping.Setting;
import ninja.leaping.configurate.objectmapping.serialize.ConfigSerializable;

@ConfigSerializable
public class PluginLock extends Lock {
	@Setting(comment="Plugin owning the lock")
	String pluginID;
	
	@Setting(comment="Additional (plugin) data")
	Map<String, Serializable> data;
	
	@Setting(comment="List of permitted individuals")
	Set<UUID> permitted;
	
	@Setting(comment="The primary block to protect")
	Location<World> target; //this value is reflected in the parents lock map
	
	@Setting(comment="Plugin lock state")
	boolean active=false;
	
	@Setting(comment="Users with this permission can always bypass the lock")
	String permission;
	
	public PluginLock(Object pluginInstance, Location<World> target) {
		this(pluginInstance, target, null);
	}
	public PluginLock(Object pluginInstance, Location<World> target, String permissionBypass) {
		Optional<PluginContainer> plugin = Sponge.getPluginManager().fromInstance(pluginInstance);
		if (!plugin.isPresent()) throw new RuntimeException("Invalid plugin instance");
		pluginID = plugin.get().getId();
		
		//TODO check if target is already locked
		this.target = target;
		
		permitted = new HashSet<>();
		data = new HashMap<>();
		permission = permissionBypass;
	}
	
	/* data manipulation */
	public void put(String key, Serializable value) {
		data.put(key, value);
	}
	public boolean containsKey(String key) {
		return data.containsKey(key);
	}
	public void remove(String key) {
		data.remove(key);
	}
	public Serializable get(String key) {
		return data.get(key);
	}
	public Set<String> getKeys() {
		return data.keySet();
	}
	
	/* permission manipulation */
	public void permitAccess(Player player) {
		permitted.add(player.getUniqueId());
	}
	public void revokeAccess(Player player) {
		permitted.remove(player.getUniqueId());
	}
	/** In order to access the target the source has to have access.<br>
	 * Access to the target is given if the following statement is true:
	 * <pre>hasPermission(bypassPermission) || !isLocked() || hasAccess(source)</pre> */
	public boolean hasAccess(Player player) {
		return permitted.contains(player.getUniqueId());
	}
	
	/* state manipulation */
	public void unlock() {
		active = true;
	}
	public void lock() {
		active = false;
	}
	/** If the lock is unlocked anyone will be able to access the target<br>
	 * otherwise only the permitted list can interact with the target.<br>
	 * Access to the target is given if the following statement is true:
	 * <pre>hasPermission(bypassPermission) || !isLocked() || hasAccess(source)</pre> */
	public boolean isLocked() {
		return active;
	}
	
	boolean canBypass(Player player) {
		return player.hasPermission(permission==null?"dosmike.lockette.check.if.user.is.op":permission);
	}
	
	public Location<World> getTarget() {
		return target;
	}
}
