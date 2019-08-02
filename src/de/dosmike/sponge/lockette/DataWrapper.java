package de.dosmike.sponge.lockette;

import java.util.List;
import java.util.Optional;

import org.spongepowered.api.Platform;
import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.DataRegistration;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.util.Direction;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import de.dosmike.sponge.lockette.data.ImmutableLockData;
import de.dosmike.sponge.lockette.data.LockData;
import de.dosmike.sponge.lockette.data.LockDataBuilder;
import de.dosmike.sponge.lockette.data.LockDataQueries;
import de.dosmike.sponge.lockette.data.LockKeys;

public class DataWrapper {
	public static final int SPONGE_API_MAJOR;
	static {
		int val = 5;
		try {
			val = new Integer(Sponge.getPluginManager().getPlugin(Platform.API_ID).get().getVersion().orElse("5.1").split("\\.")[0]);
		} catch (Exception e) {
		}
		SPONGE_API_MAJOR = val;
	}

	public static void registerKeys() {
		if (SPONGE_API_MAJOR < 7) {
			Lockette.log("Sorry, I can't figure this out");
		} else {
			DataRegistration<LockData, ImmutableLockData> dr = DataRegistration.builder()
				.dataClass(LockData.class)
				.immutableClass(ImmutableLockData.class)
				.builder(new LockDataBuilder())
				.id("lockdata")
				.name("Lock Data")
				.build();
			
			Sponge.getDataManager().registerLegacyManipulatorIds("lockdata", dr);
			
			Sponge.getDataManager().registerBuilder(LockData.class, new LockDataBuilder());
		}
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
