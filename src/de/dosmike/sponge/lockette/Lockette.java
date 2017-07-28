package de.dosmike.sponge.lockette;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.data.property.block.MatterProperty.Matter;
import org.spongepowered.api.data.type.Hinge;
import org.spongepowered.api.data.type.Hinges;
import org.spongepowered.api.data.type.PortionTypes;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.game.state.GameStartedServerEvent;
import org.spongepowered.api.plugin.Plugin;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColor;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

@Plugin(id = "dosmike_lockette", name = "Lockette", version = "1.0")
public class Lockette {
//	static final Text locketteSignIdentifier = Text.of(TextColors.DARK_BLUE, "[Lockette]");
	
	/** Not doing anything, empty Java main */
	public static void main(String[] args) {
		System.out.println("This jar can not be run as executable!");
	}
	public static boolean verboseEvents=false;
	
	public static void log(Object... message) {
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
	
	@Listener
	public void onServerStart(GameStartedServerEvent event) {
		log(TextColors.YELLOW, "Wellcome to the zone!");
		log(TextColors.YELLOW, "Registering commands and loading zones...");
		
		//add event listener
		Sponge.getEventManager().registerListeners(this, new CommonEventListener(Sponge.getPluginManager().fromInstance(this).get()));
		
		//register commands
//		CommandRegister.RegisterCommands(this);
		
		//load containers locked by plugins
		
		log(TextColors.YELLOW, "We're done loading");
	}
	
	
	static Map<Location<World>, PluginLock> pluginLocks = new HashMap<>();
	
	public static boolean hasAccess(Player source, Location<World> target) {
		return hasAccess(source, getLocksFor(target));
	}
	public static boolean hasAccess(Player source, Set<Lock> locks) {
		if (locks.isEmpty()) return true;
		for (Lock lock : locks) {
			if (lock.canBypass(source)) {
				log("Bypassed");
				return true;
			}
			if (!lock.isLocked()) {
				log("Lock inactive");
				return true;
			}
			if (lock.hasAccess(source)) {
				log("Permitted");
				return true;
			}
//			if (lock.canBypass(source) || !lock.isLocked() || lock.hasAccess(source)) return true;
		}
		return false;
	}
	
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
	
	/** returns all lockette signs including the door/container blocks */
	public static Set<Location<World>> getInvolvedBlocks(Location<World> baseBlock) {
		Set<Location<World>> lockations = new HashSet<>();
		
		if (baseBlock.getBlock().supports(Keys.OPEN)) { //door type blocks
			//see if we have a "big" door
			if (baseBlock.getBlock().supports(Keys.HINGE_POSITION) &&
				baseBlock.getBlock().supports(Keys.PORTION_TYPE)) {
//				log("Full Door");
				lockations.addAll(scanDoor(baseBlock));
				//door location / possible plugin lock
				if (baseBlock.getBlock().get(Keys.PORTION_TYPE).orElse(PortionTypes.TOP).equals(PortionTypes.BOTTOM)) {
					lockations.add(baseBlock.getRelative(Direction.UP)); 
					//if (pluginLocks.containsKey(baseBlock.getRelative(Direction.UP))) result.add(pluginLocks.get(baseBlock.getRelative(Direction.UP)));
				} else {
					lockations.add(baseBlock);
//					if (pluginLocks.containsKey(baseBlock)) result.add(pluginLocks.get(baseBlock));
				}
				
				//scan for a second door half (double doors)
				Hinge hinge = baseBlock.getBlock().get(Keys.HINGE_POSITION).get();
				Direction dir = baseBlock.getBlock().get(Keys.DIRECTION).get();
				Direction potential;
				if (dir.equals(Direction.NORTH)) potential = hinge.equals(Hinges.RIGHT)?Direction.WEST:Direction.EAST;
				else if (dir.equals(Direction.EAST)) potential = hinge.equals(Hinges.RIGHT)?Direction.NORTH:Direction.SOUTH;
				else if (dir.equals(Direction.SOUTH)) potential = hinge.equals(Hinges.RIGHT)?Direction.EAST:Direction.WEST;
				else if (dir.equals(Direction.WEST)) potential = hinge.equals(Hinges.RIGHT)?Direction.SOUTH:Direction.NORTH;
				else potential = Direction.NONE;
				if (potential != Direction.NONE) {
					Location<World> partner = baseBlock.getRelative(potential);
					//the block is a door of the same type, looking the same direction with opposing hinge
					if (partner.getBlockType().equals(baseBlock.getBlockType()) &&
						partner.getBlock().get(Keys.DIRECTION).orElse(dir.getOpposite()).equals(dir) &&
						!partner.getBlock().get(Keys.HINGE_POSITION).orElse(hinge).equals(hinge)) {
						
						lockations.addAll(scanDoor(partner));
						if (partner.getBlock().get(Keys.PORTION_TYPE).orElse(PortionTypes.TOP).equals(PortionTypes.BOTTOM)) {
							lockations.add(partner.getRelative(Direction.UP));
//							if (pluginLocks.containsKey(baseBlock.getRelative(Direction.UP))) result.add(pluginLocks.get(baseBlock.getRelative(Direction.UP)));
						} else {
							lockations.add(partner);
//							if (pluginLocks.containsKey(baseBlock)) result.add(pluginLocks.get(baseBlock));
						}
					}
				}
			} else { 
//				log("Door");
				lockations.addAll(scanContainer(baseBlock));
				lockations.add(baseBlock);
			}
		} else if (baseBlock.hasTileEntity()) {
			if (baseBlock.getTileEntity().get().supports(SignData.class) && 
				baseBlock.getBlockType().equals(BlockTypes.WALL_SIGN)) { //is a sign, yes, we need to protect the lockette signs
				SignData data = baseBlock.getTileEntity().get().get(SignData.class).get();
				if (isSignLocketteSign(data.getListValue().get())) {
					//get the data for the block we are attached to
//					lockations=getInvolvedBlocks(baseBlock.getRelative(baseBlock.getBlock().get(Keys.DIRECTION).get().getOpposite()));
					lockations.add(baseBlock); // if by event, still has original snapshot data?
					Optional<Location<World>> maybe = Lockette.findLockettable(baseBlock);
					if (maybe.isPresent()) lockations.addAll(getInvolvedBlocks(maybe.get()));
				}
			} else if (baseBlock.getTileEntity().get() instanceof TileEntityCarrier) {//is a container
				lockations.addAll(scanContainer(baseBlock));
//				if (pluginLocks.containsKey(baseBlock)) result.add(pluginLocks.get(baseBlock));
				lockations.add(baseBlock);
				//find double chests
				if (baseBlock.getBlockType().equals(BlockTypes.CHEST) || 
					baseBlock.getBlockType().equals(BlockTypes.TRAPPED_CHEST)) {
					Direction dir = baseBlock.getBlock().get(Keys.DIRECTION).get(); Location<World> rel; 
					if (dir.equals(Direction.NORTH) || dir.equals(Direction.SOUTH)) {
						rel = baseBlock.getRelative(Direction.EAST);
						if (rel.getBlockType().equals(baseBlock.getBlockType()) &&
							rel.getBlock().get(Keys.DIRECTION).get().equals(dir)) {
							lockations.addAll(scanContainer(rel));
							lockations.add(rel);
//							if (pluginLocks.containsKey(rel)) result.add(pluginLocks.get(rel));
						} else {
							rel = baseBlock.getRelative(Direction.WEST);
							if (rel.getBlockType().equals(baseBlock.getBlockType()) &&
								rel.getBlock().get(Keys.DIRECTION).get().equals(dir)) {
								lockations.addAll(scanContainer(rel));
								lockations.add(rel);
//								if (pluginLocks.containsKey(rel)) result.add(pluginLocks.get(rel));
							}
						}
					} else if (dir.equals(Direction.EAST) || dir.equals(Direction.WEST)) {
						rel = baseBlock.getRelative(Direction.NORTH);
						if (rel.getBlockType().equals(baseBlock.getBlockType()) &&
							rel.getBlock().get(Keys.DIRECTION).get().equals(dir)) {
							lockations.addAll(scanContainer(rel));
							lockations.add(rel);
//							if (pluginLocks.containsKey(rel)) result.add(pluginLocks.get(rel));
						} else {
							rel = baseBlock.getRelative(Direction.SOUTH);
							if (rel.getBlockType().equals(baseBlock.getBlockType()) &&
								rel.getBlock().get(Keys.DIRECTION).get().equals(dir)) {
								lockations.addAll(scanContainer(rel));
								lockations.add(rel);
//								if (pluginLocks.containsKey(rel)) result.add(pluginLocks.get(rel));
							}
						}
					}
				}
			}
		}
		return lockations;
	}
	
	public static Set<Lock> getLocksFor(Location<World> baseBlock) {
		Set<Lock> result; // = new HashSet<>();
		Set<Location<World>> involved = getInvolvedBlocks(baseBlock);
		
//		log(involved.size() + " potential locks");
		//ok, now we should have a whole bunch of sign locations, let's get to work
		result = signsToLocks(involved);
		log("Found ", result.size(), " locks");
		
		return result;
	}
	
	public static Set<Lock> signsToLocks(Set<Location<World>> locs) {
		Set<Lock> locks = new HashSet<>();
		for (Location<World> sign : locs) {
			if (!sign.getBlockType().equals(BlockTypes.WALL_SIGN)) {
				if (pluginLocks.containsKey(sign)) locks.add(pluginLocks.get(sign));
				log("No sign at " + sign.toString());
				continue;
			}
			List<Text> data = sign.getTileEntity().get().get(SignData.class).get().lines().get();
			if (isSignLocketteSign(data)) {
				log("Lockette at " + sign.toString());
				Set<UUID> named = new HashSet<>();
				for (int i = 1; i < data.size(); i++) {
					Optional<Player> player = Sponge.getServer().getPlayer(data.get(i).toPlain());
					if (player.isPresent()) named.add(player.get().getUniqueId());
				}
				locks.add(new Lock(sign, named));
			} log("Sign at " + sign.toString());
		}
		return locks;
	}
	
	static Set<Location<World>> scanContainer(Location<World> containerLocation) {
		Set<Location<World>> result = new HashSet<>();
		
		result.addAll(findSignsFacingThisBlock(containerLocation, true));
		Optional<MatterProperty> p = containerLocation.getRelative(Direction.UP).getBlockType().getProperty(MatterProperty.class);
		if (p.isPresent() && p.get().getValue().equals(Matter.SOLID))
			result.addAll(findSignsFacingThisBlock(containerLocation.getRelative(Direction.UP), false));
		if (containerLocation.getRelative(Direction.UP).getBlockType().equals(BlockTypes.WALL_SIGN))
			result.add(containerLocation.getRelative(Direction.UP));
		
		return result;
	}
	
	static Set<Location<World>> scanDoor(Location<World> doorLocation) {
		Set<Location<World>> result = new HashSet<>();
		
		result.addAll(findSignsFacingThisBlock(doorLocation, true));
		//also look at the other door block and above if that's solid and not another door
		if (doorLocation.getBlock().supports(Keys.PORTION_TYPE)) {
			if (doorLocation.getBlock().get(Keys.PORTION_TYPE).get().equals(PortionTypes.TOP))
				result.addAll(findSignsFacingThisBlock(doorLocation.getRelative(Direction.DOWN), true));
			else {
				result.addAll(findSignsFacingThisBlock(doorLocation.getRelative(Direction.UP), true));
				doorLocation = doorLocation.getRelative(Direction.UP);
			}
		}
		Optional<MatterProperty> p = doorLocation.getRelative(Direction.UP).getBlockType().getProperty(MatterProperty.class);
		if (p.isPresent() && p.get().getValue().equals(Matter.SOLID))
			result.addAll(findSignsFacingThisBlock(doorLocation.getRelative(Direction.UP), false));
		
		return result;
	}
	
	static Set<Location<World>> findSignsFacingThisBlock(Location<World> block, boolean extended) {
		Set<Location<World>> result = new HashSet<>();
		result.addAll(findSignsFacingDirection(block, Direction.NORTH, extended));
		result.addAll(findSignsFacingDirection(block, Direction.EAST, extended));
		result.addAll(findSignsFacingDirection(block, Direction.SOUTH, extended));
		result.addAll(findSignsFacingDirection(block, Direction.WEST, extended));
		return result;
	}
	static Set<Location<World>> findSignsFacingDirection(Location<World> block, Direction dir, boolean extended) {
		Set<Location<World>> result = new HashSet<>();
		Location<World> rel = block.getRelative(dir);
		if (rel.getBlockType().equals(BlockTypes.WALL_SIGN) && rel.getBlock().get(Keys.DIRECTION).get().equals(dir)) {
//			log("Found sign " + dir);
			result.add(rel); 
		} else if (extended) {
			//check if the block is solid...
			Optional<MatterProperty> p = rel.getBlockType().getProperty(MatterProperty.class);
			if (p.isPresent() && p.get().getValue().equals(Matter.SOLID)) {
				if (dir.equals(Direction.NORTH) || dir.equals(Direction.SOUTH)) {
					result.addAll(findSignsFacingDirection(rel, Direction.EAST, false));
					result.addAll(findSignsFacingDirection(rel, Direction.WEST, false));
				} else if (dir.equals(Direction.EAST) || dir.equals(Direction.WEST)) {
					result.addAll(findSignsFacingDirection(rel, Direction.NORTH, false));
					result.addAll(findSignsFacingDirection(rel, Direction.SOUTH, false));
				}
			}
		}
		return result;
	}
	
	static boolean isSignLocketteSign(List<Text> data) {
		if (data.isEmpty()) return false;
		Text head = data.get(0);
		String plain = head.toPlain();
		
		log(plain, " vs [Lockette]");
		return !head.equals(Text.of(plain)) && //line has format
				plain.equals("[Lockette]");
//		return plain.equals("[Lockette]");
	}
	public static boolean trySignIsLockette(Location<World> block) {
		if (!block.getBlockType().equals(BlockTypes.WALL_SIGN)) return false;
		return isSignLocketteSign(block.getTileEntity().get().get(SignData.class).get().getListValue().get());
	}
	
	public static Optional<Location<World>> findLockettable(Location<World> sign) {
		if (!sign.getBlockType().equals(BlockTypes.WALL_SIGN)) return Optional.empty();
		Direction toWall = sign.getBlock().get(Keys.DIRECTION).get().getOpposite();
		//block behind sign
		Location<World> scan = sign.getRelative(toWall);
		if (isBlockLockettable(scan)) return Optional.of(scan);
		//block below sign
		if (isBlockLockettable(sign.getRelative(Direction.DOWN))) return Optional.of(sign.getRelative(Direction.DOWN));
		//blocks left or right to block behind sign
		//TODO only if these relatives are hingeable
		if (toWall.equals(Direction.NORTH) || toWall.equals(Direction.SOUTH)) {
			if (isBlockLockettable(scan.getRelative(Direction.EAST))) return Optional.of(scan.getRelative(Direction.EAST));
			if (isBlockLockettable(scan.getRelative(Direction.WEST))) return Optional.of(scan.getRelative(Direction.WEST));
		} else if (toWall.equals(Direction.EAST) || toWall.equals(Direction.WEST)) {
			if (isBlockLockettable(scan.getRelative(Direction.NORTH))) return Optional.of(scan.getRelative(Direction.NORTH));
			if (isBlockLockettable(scan.getRelative(Direction.SOUTH))) return Optional.of(scan.getRelative(Direction.SOUTH));
		}
		return Optional.empty();
	}
	static boolean isBlockLockettable(Location<World> block) {
		if (block.getBlock().supports(Keys.OPEN)) return true;
		if (block.hasTileEntity() && ((block.getTileEntity().get()) instanceof TileEntityCarrier) ) return true;
		return false;
	}
}
