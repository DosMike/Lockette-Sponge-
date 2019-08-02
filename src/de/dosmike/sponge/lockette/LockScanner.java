package de.dosmike.sponge.lockette;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.spongepowered.api.block.BlockState;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.block.tileentity.carrier.TileEntityCarrier;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.property.block.MatterProperty;
import org.spongepowered.api.data.property.block.MatterProperty.Matter;
import org.spongepowered.api.data.type.Hinge;
import org.spongepowered.api.data.type.Hinges;
import org.spongepowered.api.data.type.PortionTypes;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3i;

import de.dosmike.sponge.lockette.data.LockData;

public class LockScanner {
	static final Text locketteSignIdentifier = Text.of(TextColors.DARK_BLUE, "[Lockette]");
	
	ExtentDelta<World> delta;
	public LockScanner(ExtentDelta<World> delta) {
		this.delta = delta; 
	}
	public LockScanner(World world) {
		delta = ExtentDelta.builder(world).build();
	}
	private BlockState state(Vector3i at) {
		return delta.get(at).getState();
	}
	public World getExtent() {
		return delta.getExtent();
	}
	public ExtentDelta<World> getExtentDelta() {
		return delta;
	}
	
	/** returns all lockette signs including the door/container blocks */
	public Set<Vector3i> getInvolvedBlocks(Vector3i baseBlock) {
		Set<Vector3i> lockations = new HashSet<>();
		BlockState baseState = state(baseBlock);
		
		if (baseState.supports(Keys.OPEN)) { //door type blocks
			//see if we have a "big" door
			if (baseState.supports(Keys.HINGE_POSITION) &&
					baseState.supports(Keys.PORTION_TYPE)) {
				lockations.addAll(scanDoor(baseBlock));
				//door location / possible plugin lock
				if (baseState.get(Keys.PORTION_TYPE).orElse(PortionTypes.TOP).equals(PortionTypes.BOTTOM)) {
					lockations.add(baseBlock.add(0,1,0)); 
				} else {
					lockations.add(baseBlock);
				}
				
				//scan for a second door half (double doors)
				Hinge hinge = baseState.get(Keys.HINGE_POSITION).get();
				Direction dir = baseState.get(Keys.DIRECTION).get();
				Direction potential;
				if (dir.equals(Direction.NORTH)) potential = hinge.equals(Hinges.RIGHT)?Direction.WEST:Direction.EAST;
				else if (dir.equals(Direction.EAST)) potential = hinge.equals(Hinges.RIGHT)?Direction.NORTH:Direction.SOUTH;
				else if (dir.equals(Direction.SOUTH)) potential = hinge.equals(Hinges.RIGHT)?Direction.EAST:Direction.WEST;
				else if (dir.equals(Direction.WEST)) potential = hinge.equals(Hinges.RIGHT)?Direction.SOUTH:Direction.NORTH;
				else potential = Direction.NONE;
				if (potential != Direction.NONE) {
					Vector3i partner = baseBlock.add(potential.asBlockOffset());
					//the block is a door of the same type, looking the same direction with opposing hinge
					BlockState partnerState = state(partner);
					if (partnerState.getType().equals(baseState.getType()) &&
						partnerState.get(Keys.DIRECTION).orElse(dir.getOpposite()).equals(dir) &&
						!partnerState.get(Keys.HINGE_POSITION).orElse(hinge).equals(hinge)) {
						
						lockations.addAll(scanDoor(partner));
						if (partnerState.get(Keys.PORTION_TYPE).orElse(PortionTypes.TOP).equals(PortionTypes.BOTTOM)) {
							lockations.add(partner.add(0,1,0));
						} else {
							lockations.add(partner);
						}
					}
				}
			} else {
				lockations.addAll(scanContainer(baseBlock));
				lockations.add(baseBlock);
			}
		} else if (baseState.getType().equals(BlockTypes.WALL_SIGN)) {
			getSignData(baseBlock).ifPresent(data->{
				if (!data.isLocketteHolder()) {
					Lockette.log("No SignData (A) for Sign at " + baseBlock);
				} else {
					lockations.add(baseBlock);
					Optional<Vector3i> maybe = findLockettable(baseBlock);
					if (maybe.isPresent()) lockations.addAll(getInvolvedBlocks(maybe.get()));
				}
			});

		} else if (isBlockContainer(baseBlock)) {//is a container
			lockations.addAll(scanContainer(baseBlock));
			lockations.add(baseBlock);
			//find double chests
			if (baseState.getType().equals(BlockTypes.CHEST) || 
					baseState.getType().equals(BlockTypes.TRAPPED_CHEST)) {
				Direction dir = baseState.get(Keys.DIRECTION).get(); Vector3i rel; BlockState relState; 
				if (dir.equals(Direction.NORTH) || dir.equals(Direction.SOUTH)) {
					rel = baseBlock.add(Direction.EAST.asBlockOffset());
					relState = state(rel);
					if (relState.getType().equals(baseState.getType()) &&
						relState.get(Keys.DIRECTION).get().equals(dir)) {
						lockations.addAll(scanContainer(rel));
						lockations.add(rel);
					} else {
						rel = baseBlock.add(Direction.WEST.asBlockOffset());
						relState = state(rel);
						if (relState.getType().equals(baseState.getType()) &&
							relState.get(Keys.DIRECTION).get().equals(dir)) {
							lockations.addAll(scanContainer(rel));
							lockations.add(rel);
						}
					}
				} else if (dir.equals(Direction.EAST) || dir.equals(Direction.WEST)) {
					rel = baseBlock.add(Direction.NORTH.asBlockOffset());
					relState = state(rel);
					if (relState.getType().equals(baseState.getType()) &&
						relState.get(Keys.DIRECTION).get().equals(dir)) {
						lockations.addAll(scanContainer(rel));
						lockations.add(rel);
					} else {
						rel = baseBlock.add(Direction.SOUTH.asBlockOffset());
						relState = state(rel);
						if (relState.getType().equals(baseState.getType()) &&
							relState.get(Keys.DIRECTION).get().equals(dir)) {
							lockations.addAll(scanContainer(rel));
							lockations.add(rel);
						}
					}
				}
			}
		} else { //maybe someone is punching a block behind a sign next to a door?
			Set<Vector3i> maybe = findSignsFacingThisBlock(baseBlock, false);
			for (Vector3i perhaps : maybe) {
				if (state(perhaps).getType().equals(BlockTypes.WALL_SIGN))
					lockations.addAll(getInvolvedBlocks(perhaps));
			}
		}
		return lockations;
	}

	public Set<Lock> getLocksFor(Vector3i baseBlock) {
			Set<Lock> result; // = new HashSet<>();
			Set<Vector3i> involved = getInvolvedBlocks(baseBlock);
			
			//ok, now we should have a whole bunch of sign locations, let's get to work
			result = signsToLocks(involved);

			return result;
		}
	
	public Set<Lock> signsToLocks(Set<Vector3i> locs) {
		Set<Lock> locks = new HashSet<>();
		for (Vector3i sign : locs) {
			BlockState state = state(sign);
			if (!state.getType().equals(BlockTypes.WALL_SIGN)) {
				Location<World> at = getExtent().getLocation(sign);
				if (Lockette.pluginLocks.containsKey(at)) 
					locks.add(Lockette.pluginLocks.get(at));
				continue;
			}
			
			Optional<LockData> data = getSignData(sign);
			if (!data.isPresent() || !data.get().isLocketteHolder()) {
				continue;
			}
			locks.add(data.get().toLock(delta.getExtent().getLocation(sign)));
		}
		return locks;
	}
	
	Set<Vector3i> scanContainer(Vector3i containerLocation) {
		Set<Vector3i> result = new HashSet<>();
		
		result.addAll(findSignsFacingThisBlock(containerLocation, true));
		Vector3i up=containerLocation.add(0, 1, 0);
		if (!isBlockContainer(up)) {
			BlockState upState = state(up);
			Optional<MatterProperty> p = upState.getType().getProperty(MatterProperty.class);
			if (p.isPresent() && p.get().getValue().equals(Matter.SOLID))
				result.addAll(findSignsFacingThisBlock(up, false));
			if (upState.getType().equals(BlockTypes.WALL_SIGN))
				result.add(up);
		}
		
		return result;
	}
	
	Set<Vector3i> scanDoor(Vector3i doorLocation) {
		Set<Vector3i> result = new HashSet<>();
		
		result.addAll(findSignsFacingThisBlock(doorLocation, true));
		//also look at the other door block and above if that's solid and not another door
		BlockState doorState = state(doorLocation);
		if (doorState.supports(Keys.PORTION_TYPE)) {
			if (doorState.get(Keys.PORTION_TYPE).get().equals(PortionTypes.TOP))
				result.addAll(findSignsFacingThisBlock(doorLocation.sub(0,1,0), true));
			else {
				result.addAll(findSignsFacingThisBlock(doorLocation.add(0,1,0), true));
				doorLocation = doorLocation.add(0,1,0);
				doorState = state(doorLocation);
			}
		}
		Optional<MatterProperty> p = state(doorLocation.add(0,1,0)).getType().getProperty(MatterProperty.class);
		if (p.isPresent() && p.get().getValue().equals(Matter.SOLID))
			result.addAll(findSignsFacingThisBlock(doorLocation.add(0,1,0), false));
		
		return result;
	}
	
	Set<Vector3i> findSignsFacingThisBlock(Vector3i block, boolean extended) {
		Set<Vector3i> result = new HashSet<>();
		result.addAll(findSignsFacingDirection(block, Direction.NORTH, extended));
		result.addAll(findSignsFacingDirection(block, Direction.EAST, extended));
		result.addAll(findSignsFacingDirection(block, Direction.SOUTH, extended));
		result.addAll(findSignsFacingDirection(block, Direction.WEST, extended));
		return result;
	}
	
	Set<Vector3i> findSignsFacingDirection(Vector3i block, Direction dir, boolean extended) {
		Set<Vector3i> result = new HashSet<>();
		Vector3i rel = block.add(dir.asBlockOffset());
		BlockState relState = state(rel);
		if (relState.getType().equals(BlockTypes.WALL_SIGN) && relState.get(Keys.DIRECTION).get().equals(dir)) {
			result.add(rel);
		} else if (extended) {
			//check if the block is solid and not container...
			Optional<MatterProperty> p = relState.getType().getProperty(MatterProperty.class);
			if (p.isPresent() && p.get().getValue().equals(Matter.SOLID) && !isBlockContainer(rel)) {
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

	public boolean trySignIsLockette(Vector3i block) {
		if (!state(block).getType().equals(BlockTypes.WALL_SIGN)) return false;
		Optional<LockData> data = getSignData(block);
		return data.isPresent() && data.get().isLocketteHolder();
	}

	public Optional<Vector3i> findLockettable(Vector3i sign) {
		BlockState signState = state(sign);
		if (!signState.getType().equals(BlockTypes.WALL_SIGN)) return Optional.empty();
		Direction toWall = signState.get(Keys.DIRECTION).get().getOpposite();
		//block behind sign
		Vector3i scan = sign.add(toWall.asBlockOffset());
		if (isBlockLockettable(scan)) return Optional.of(scan);
		//block below sign
		if (isBlockLockettable(sign.sub(0,1,0))) return Optional.of(sign.sub(0,1,0));
		//blocks left or right to block behind sign
		//MAYBE only if these relatives are hingeable
		if (toWall.equals(Direction.NORTH) || toWall.equals(Direction.SOUTH)) {
			if (isBlockLockettable(scan.add(Direction.EAST.asBlockOffset()))) return Optional.of(scan.add(Direction.EAST.asBlockOffset()));
			if (isBlockLockettable(scan.add(Direction.WEST.asBlockOffset()))) return Optional.of(scan.add(Direction.WEST.asBlockOffset()));
		} else if (toWall.equals(Direction.EAST) || toWall.equals(Direction.WEST)) {
			if (isBlockLockettable(scan.add(Direction.NORTH.asBlockOffset()))) return Optional.of(scan.add(Direction.NORTH.asBlockOffset()));
			if (isBlockLockettable(scan.add(Direction.SOUTH.asBlockOffset()))) return Optional.of(scan.add(Direction.SOUTH.asBlockOffset()));
		}
		return Optional.empty();
	}

	/** from a snapshot (= extentdelta) we can't retrieve tileentities. So I have to go back to a Location
	 * Hacky, not at all dynamic or suitable for spongeforgem but for me that's ok */
	boolean isBlockContainer(Vector3i at) {
		Optional<TileEntity> loc = delta.getExtent().getTileEntity(at);
		if (!loc.isPresent()) return false;
		return loc.get() instanceof TileEntityCarrier;
	}
	/** from a snapshot (= extentdelta) we can't retrieve tileentities. So I have to go back to a Location
	 * Hacky, not at all dynamic or suitable for spongeforge but for me that's ok */
	Optional<LockData> getSignData(Vector3i at) {
		Optional<TileEntity> loc = delta.getExtent().getTileEntity(at);
		if (!loc.isPresent()) return Optional.empty();
		return DataWrapper.getLockKey(loc.get());
	}
	boolean isBlockLockettable(Vector3i block) {
		BlockState state = state(block);
		if (state.supports(Keys.OPEN)) return true;
		return isBlockContainer(block);
	}

}
