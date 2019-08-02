package de.dosmike.sponge.lockette;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.extent.Extent;

import com.flowpowered.math.vector.Vector3i;

public class ExtentDelta<E extends Extent> {
	E ex;
	Map<Vector3i, BlockSnapshot> delta = new HashMap<>();
	
	private ExtentDelta(E extent) {
		ex=extent;
	}
	
	public BlockSnapshot get(Location<E> location) {
		if (delta.containsKey(location.getBlockPosition())) {
			return delta.get(location.getBlockPosition());
		} else {
			return ex.createSnapshot(location.getBlockPosition());
		}
	}
	public BlockSnapshot get(Vector3i location) {
		if (delta.containsKey(location)) {
			return delta.get(location);
		} else {
			return ex.createSnapshot(location);
		}
	}
	public E getExtent() {
		return ex;
	}
	
	
	
	
	public static class Builder<X extends Extent> {
		ExtentDelta<X> built; 
		private Builder(X forExtent) {
			built = new ExtentDelta<X>(forExtent);
		}
		public void addDelta(BlockSnapshot delta) {
			built.delta.put(delta.getPosition(), delta);
		}
		public void addDeltas(Collection<BlockSnapshot> deltas) {
			for (BlockSnapshot delta : deltas) built.delta.put(delta.getPosition(), delta);
		}
		public void addDeltas(BlockSnapshot... deltas) {
			for (BlockSnapshot delta : deltas) built.delta.put(delta.getPosition(), delta);
		}
		public ExtentDelta<X> build() {
			return built;
		}
	}
	public static <X extends Extent> Builder<X> builder(X forExtent) {
		return new Builder<X>(forExtent);
	}
}
