package de.dosmike.sponge.lockette.data;

import org.spongepowered.api.data.DataQuery;

public class LockDataQueries {

	public static final DataQuery OWNER_NAME = LockKeys.LOCK.getQuery().then("oname"); //DataQuery.of("oname");
	public static final DataQuery OWNER_UUID = LockKeys.LOCK.getQuery().then("ouuid"); //DataQuery.of("ouuid");
	public static final DataQuery PERM_NAMES = LockKeys.LOCK.getQuery().then("pnames"); //DataQuery.of("pnames");
	public static final DataQuery PERM_UUIDS = LockKeys.LOCK.getQuery().then("puuids"); //DataQuery.of("puuids");
	
	
	public static final DataQuery UNSAFE_LOCATION = 
			DataQuery.of("UnsafeData", "ForgeData", "SpongeData", "CustomManipulators");
	public static final DataQuery SAFE_LOCATION = 
			DataQuery.of("Data");
}
