package de.dosmike.sponge.lockette;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.apache.commons.lang3.StringUtils;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.command.CommandResult;
import org.spongepowered.api.command.spec.CommandSpec;
import org.spongepowered.api.config.DefaultConfig;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.GameReloadEvent;
import org.spongepowered.api.event.game.state.GamePreInitializationEvent;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.event.game.state.GameStoppingEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.service.user.UserStorageService;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3i;
import com.google.common.reflect.TypeToken;
import com.google.inject.Inject;

import de.dosmike.sponge.lockette.data.ImmutableLockData;
import de.dosmike.sponge.lockette.data.LockData;
import de.dosmike.sponge.lockette.data.LockDataBuilder;
import de.dosmike.sponge.lockette.data.LockDataQueries;
import de.dosmike.sponge.lockette.data.LockKeys;
import ninja.leaping.configurate.ConfigurationNode;
import ninja.leaping.configurate.ConfigurationOptions;
import ninja.leaping.configurate.commented.CommentedConfigurationNode;
import ninja.leaping.configurate.loader.ConfigurationLoader;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializerCollection;
import ninja.leaping.configurate.objectmapping.serialize.TypeSerializers;

@Plugin(id = "dosmike_lockette", name = "Lockette", version = "1.2")
public class Lockette {
		
	/** Not doing anything, empty Java main */
	public static void main(String[] args) {
		System.out.println("This jar can not be run as executable!");
	}
	public static boolean verboseEvents=false;
	
	static void log(Object... message) {
		Text.Builder tb = Text.builder();
		tb.color(TextColors.AQUA);
		tb.append(Text.of("[Lockette] "));
		if (!(message[0] instanceof TextColor)) tb.color(TextColors.RESET);
		for (Object o : message) {
			if (o instanceof TextColor) 
				tb.color((TextColor)o);
			else
				tb.append(Text.of(o));
		}
		Sponge.getServer().getBroadcastChannel().send(tb.build());
	}

	Optional<UserStorageService> userStorage;
	public static Optional<User> getUser(UUID by) {
		 Optional<Player> onlinePlayer = Sponge.getServer().getPlayer(by);
	    if (onlinePlayer.isPresent()) {
	        return Optional.of((User)onlinePlayer.get());
	    }
	    return getInstance().userStorage.get().get(by);
	}
	
	@Listener
	public void onPreInit(GamePreInitializationEvent event) {
//		DataRegistration<LockData, ImmutableLockData> dr = DataRegistration.builder()
//			.dataClass(LockData.class)
//			.immutableClass(ImmutableLockData.class)
//			.builder(new LockDataBuilder())
//			.manipulatorId("lockdata")
//			.dataName("Lock Data")
//			.buildAndRegister(Sponge.getPluginManager().fromInstance(this).get());
//		Sponge.getDataManager().registerLegacyManipulatorIds("lockdata", dr);
		DataRegistration<LockData, ImmutableLockData> dr = DataRegistration.builder()
			.dataClass(LockData.class)
			.immutableClass(ImmutableLockData.class)
			.builder(new LockDataBuilder())
			.manipulatorId("lockdata")
			.dataName("Lock Data")
			.buildAndRegister(Sponge.getPluginManager().fromInstance(this).get());
		
		Sponge.getDataManager().registerLegacyManipulatorIds("dosmike_lockette:lockdata", dr);
		
		Sponge.getDataManager().registerBuilder(LockData.class, new LockDataBuilder());
	}
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		PluginContainer self = Sponge.getPluginManager().fromInstance(this).get();
		userStorage = Sponge.getServiceManager().provide(UserStorageService.class);
		log(TextColors.LIGHT_PURPLE, "Version " + self.getVersion().get() + " by " + StringUtils.join(self.getAuthors(), ", "));
		
		//add event listener
		Sponge.getEventManager().registerListeners(this, new CommonEventListener(Sponge.getPluginManager().fromInstance(this).get()));
		
		//load containers locked by plugins
		load();
		
		Sponge.getCommandManager().register(this, CommandSpec.builder()
				.executor((src, args)->{
					if (src instanceof Player) {
						Player p = ((Player) src);
						LockData ld = new LockData();
						ld.setOwner(p.getProfile());
						p.sendMessage(Text.of("Created "+ld.toContainer()));
						
						p.offer(/*LockKeys.LOCK,*/ ld);
						p.get(LockKeys.LOCK).ifPresent(l->p.sendMessage(Text.of("Set key to player")));
						
						Lockette.log("State: " + p.toContainer());
						
						p.sendMessage(Text.of("Player supports LockKey? ", p.supports(LockKeys.LOCK)));
						
						p.remove(LockKeys.LOCK);
						p.get(LockKeys.LOCK).ifPresent(l->p.sendMessage(Text.of("Was unable to remove Key")));
					}
					
					return CommandResult.success();
				})
				.build(), "lockette");
		
		log(TextColors.LIGHT_PURPLE, "Thank you for using this plugin!");
	}
	
	
	static Map<Location<World>, PluginLock> pluginLocks = new HashMap<>();
	
	/** Retrieve all locks you plugin has placed, that are still valid (target was not destroyed) */
	public static Set<PluginLock> getPluginLocks(Object yourPlugin) {
		Set<PluginLock> locks = new HashSet<>();
		Optional<PluginContainer> plugin = Sponge.getPluginManager().fromInstance(yourPlugin);
		if (!plugin.isPresent()) throw new RuntimeException("Invalid plugin instance");
		String id = plugin.get().getId();
		
		for (PluginLock lock : pluginLocks.values()) {
			if (lock.pluginID.equals(id)) locks.add(lock);
		}
		
		return locks;
	}
	
	private static boolean addPluginLock(Location<World> target, PluginLock lock) {
		if (!new LockScanner(
				target.getExtent())
				.isBlockLockettable(
						target.getBlockPosition()))
			throw new IllegalArgumentException("Tried to lock non-lockettable block");
		if (pluginLocks.containsKey(target)) 
			return false;
		pluginLocks.put(target, lock);
		return true;
	}
	/** add a plugin lock to the location, the location has to be a lockettable block,
	 * this means have a tileEntityCarrier or support the key OPEN, otherwise a IllegalArgumentException will be thrown.<br>
	 * This is done because the lock would not do anything otherwise.<br>
	 * If a plugin is already registered for this location this function will return false. It returns true if the lock was added. */
	public static boolean addPluginLock(PluginLock lock) {
		return addPluginLock(lock.getTarget(), lock);
	}
	
	static Optional<PluginLock> getPluginLockAt(Location<World> target) {
		if (pluginLocks.containsKey(target))
			return Optional.of(pluginLocks.get(target));
		else
			return Optional.empty();
	}
	/** This returns a PluginLock for the given location only if your plugin owns the lock for security reasons.<br>
	 * * Throws IllegalArgumentException if your yourPlugin is not a plugin instance */
	public static Optional<PluginLock> getPluginLockAt(Object yourPlugin, Location<World> target) {
		Optional<PluginLock> maybe = getPluginLockAt(target);
		if (maybe.isPresent()) {
			Optional<PluginContainer> plugin = Sponge.getPluginManager().fromInstance(yourPlugin);
			if (!plugin.isPresent()) throw new RuntimeException("Invalid plugin instance");
			if (!plugin.get().getId().equals(maybe.get().pluginID)) return Optional.empty();
		}
		return maybe;
	}
	/** Tries to remove a lock from a lockettable, returns true on success, false if no plugin lock was in place or you do not own the lock.<br>
	 * Throws IllegalArgumentException if your yourPlugin is not a plugin instance */
	public static boolean removePluginLock(Object yourPlugin, PluginLock lock) {
		Optional<PluginContainer> plugin = Sponge.getPluginManager().fromInstance(yourPlugin);
		if (!plugin.isPresent()) throw new RuntimeException("Invalid plugin instance");
		if (!plugin.get().getId().equals(lock.pluginID)) return false;
		pluginLocks.remove(lock.getTarget());
		return true;
	}
	/** Tries to remove a lock from a lockettable, returns true on success, false if no plugin lock was in place or you do not own the lock.<br>
	 * Throws IllegalArgumentException if your yourPlugin is not a plugin instance */
	public static boolean removePluginLock(Object yourPlugin, Location<World> target) {
		Optional<PluginLock> maybe = getPluginLockAt(target);
		if (!maybe.isPresent()) return false;
		Optional<PluginContainer> plugin = Sponge.getPluginManager().fromInstance(yourPlugin);
		if (!plugin.isPresent()) throw new RuntimeException("Invalid plugin instance");
		if (!plugin.get().getId().equals(maybe.get().pluginID)) return false;
		pluginLocks.remove(target);
		return true;
	}
	
	public static boolean hasAccess(Player source, ExtentDelta<World> delta, Location<World> target) {
//		if (!target.getExtent().equals(delta.getExtent())) throw new IllegalArgumentException("Target not in delta extent");
		return hasAccess(source, new LockScanner(delta).getLocksFor(target.getBlockPosition()));
	}
	public static boolean hasAccess(Player source, ExtentDelta<World> delta, Vector3i target) {
//		if (!target.getExtent().equals(delta.getExtent())) throw new IllegalArgumentException("Target not in delta extent");
		return hasAccess(source, new LockScanner(delta).getLocksFor(target));
	}
	public static boolean hasAccess(Player source, Location<World> target) {
		return hasAccess(source, new LockScanner(target.getExtent()).getLocksFor(target.getBlockPosition()));
	}
	public static boolean hasAccess(Player source, Set<Lock> locks) {
		if (locks.isEmpty()) return true;
		for (Lock lock : locks) {
//			if (lock.canBypass(source)) {
//				log("Bypassed");
//				return true;
//			}
//			if (!lock.isLocked()) {
//				log("Unlocked");
//				return true;
//			}
//			if (lock.hasAccess(source)) {
//				log("Permitted");
//				return true;
//			}
			if (lock.canBypass(source) || !lock.isLocked() || lock.hasAccess(source)) return true;
		}
		return false;
	}
	
	public static boolean isFullOwner(Player source, ExtentDelta<World> delta, Vector3i target) {
//		if (!target.getExtent().equals(delta.getExtent())) throw new IllegalArgumentException("Target not in delta extent");
		return isFullOwner(source, new LockScanner(delta).getLocksFor(target));
	}
	public static boolean isFullOwner(Player source, Set<Lock> locks) {
		if (locks.isEmpty()) return true;
		for (Lock lock : locks)
			if (!lock.isOwner(source)) return false;
		return true;
	}
	
	
	@Inject
	@DefaultConfig(sharedRoot = true)
	private ConfigurationLoader<CommentedConfigurationNode> configManager;
	private static TypeToken<Map<Location<World>,  PluginLock>> PluginLocksSerializationToken = new TypeToken<Map<Location<World>,  PluginLock>>() {
		private static final long serialVersionUID = 4717646288515804540L;
	};
	
	@Listener
	public void reload(GameReloadEvent event) {
		load();
	}
	
	@Listener
	public void shutdown(GameStoppingEvent event) {
		save();
	}
	
	/** save all pluginlocks to file */
	public static void save() {
		ConfigurationLoader<CommentedConfigurationNode> loader = getInstance().configManager;
		TypeSerializerCollection customSerializer = TypeSerializers.getDefaultSerializers().newChild();
		customSerializer.registerType(PluginLocksSerializationToken, new LockMapSerializer());
		customSerializer.registerType(TypeToken.of(PluginLock.class), new LockSerializer());
		ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(customSerializer);
		ConfigurationNode root = loader.createEmptyNode(options);
		try {
			root.setValue(PluginLocksSerializationToken, pluginLocks);
			loader.save(root);
		} catch (Exception e) {
			Lockette.log(TextColors.RED, "Unable to save plugin locks!");
			e.printStackTrace();
		}
	}
	
	private void load() {
		TypeSerializerCollection customSerializer = TypeSerializers.getDefaultSerializers().newChild();
		customSerializer.registerType(PluginLocksSerializationToken, new LockMapSerializer());
		customSerializer.registerType(TypeToken.of(PluginLock.class), new LockSerializer());
		ConfigurationOptions options = ConfigurationOptions.defaults().setSerializers(customSerializer);
		
		try {
			pluginLocks = configManager.load(options).getValue(PluginLocksSerializationToken);
			if (pluginLocks == null) pluginLocks = new HashMap<>(); 
		} catch (Exception e) {
			Lockette.log(TextColors.RED, "Unable to load plugin locks!");
			e.printStackTrace();
			pluginLocks = new HashMap<>();
		}
	}
	
	static Lockette getInstance() {
		return (Lockette)Sponge.getPluginManager().getPlugin("dosmike_lockette").get().getInstance().get();
	}
	
	
	//=== = = = The following two functions are Unsafe NBT helpers, as Sponge does not correctly mirror the custom NBTs into the safe space as of Dec '17 = = = ===

	public static Optional<LockData> getLockKey(TileEntity te) {
		Optional<LockData> old = te.get(LockKeys.LOCK);
		if (old.isPresent()) return old;
		
		Optional<List<DataView>> oviews = te.toContainer().getViewList(LockDataQueries.UNSAFE_LOCATION);
		if (!oviews.isPresent()) return Optional.empty();
		List<DataView> views = oviews.get();
		for (DataView view : views) {
			Optional<String> oid = view.getString(DataQuery.of("ManipulatorId"));
			if (!oid.isPresent()) continue;
			String id = oid.get();
			if (!id.equals("dosmike_lockette:lockdata")) continue;
			Optional<DataView> omdata = view.getView(DataQuery.of("ManipulatorData"));
			if (omdata.isPresent())
				return Optional.of(new LockData(omdata.get()));
		}
		return Optional.empty();
	}
	public static void removeLockKey(TileEntity te) {
		//removing doesn't seem to work event through container manipulation/setraw, so I just create a new sign...
		
		Direction d = te.getBlock().get(Keys.DIRECTION).get();
		Location<World> at = te.getLocation();
		
		at.removeBlock();
		at.setBlockType(BlockTypes.WALL_SIGN);
		at.tryOffer(Keys.DIRECTION, d);
	}
	
	
}
