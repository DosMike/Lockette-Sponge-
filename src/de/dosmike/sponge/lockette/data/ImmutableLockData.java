package de.dosmike.sponge.lockette.data;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.data.value.ValueFactory;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

public class ImmutableLockData extends AbstractImmutableData<ImmutableLockData, LockData> {

	public static final ValueFactory vf = Sponge.getRegistry().getValueFactory();
	
	private Optional<String> oname;
	private Optional<UUID> ouuid;
	private List<UUID> puuids; 
	private List<String> pnames;
	
	public ImmutableLockData() {
		oname=Optional.empty();
		ouuid=Optional.empty();
		puuids=new LinkedList<>();
		pnames=new LinkedList<>();
		registerGetters();
	}
	public ImmutableLockData(DataView container) {
		String name = container.getString(LockDataQueries.OWNER_NAME).orElse(null);
		if (name==null) oname = Optional.empty(); else oname = Optional.of(name);
		UUID uuid = container.getObject(LockDataQueries.OWNER_UUID, UUID.class).orElse(null);
		if (uuid==null) ouuid = Optional.empty(); else ouuid = Optional.of(uuid);
		
		pnames = container.getStringList(LockDataQueries.PERM_NAMES).orElse(new LinkedList<>());
		List<String> uuids = container.getStringList(LockDataQueries.PERM_UUIDS).orElse(new LinkedList<>());
		puuids = uuids.stream().map((uid)->UUID.fromString(uid)).collect(Collectors.toList());
		
//		Logger.getAnonymousLogger().info("Immutable const... "+toContainer());
		registerGetters();
	}
	
	public ImmutableValue<Optional<String>> getLockOwnerName() {
		return vf.createOptionalValue(LockKeys.LOCK_ONAME, oname.orElse(null)).asImmutable();
	}
	public ImmutableValue<Optional<UUID>> getLockOwnerID() {
		return vf.createOptionalValue(LockKeys.LOCK_OUUID, ouuid.orElse(null)).asImmutable();
	}
	public ImmutableValue<List<String>> getLockPermittedNames() {
		return vf.createListValue(LockKeys.LOCK_PNAMES, pnames).asImmutable();
	}
	public ImmutableValue<List<UUID>> getLockPermittedIDs() {
		return vf.createListValue(LockKeys.LOCK_PUUIDS, puuids).asImmutable();
	}
	
	@Override
	public DataContainer toContainer() {
		if (!ouuid.isPresent()) 
			return DataContainer.createNew();
		
		return DataContainer.createNew()
			.set(LockDataQueries.OWNER_NAME, oname.get())
			.set(LockDataQueries.OWNER_UUID, ouuid.get())
			.set(LockDataQueries.PERM_NAMES, pnames)
			.set(LockDataQueries.PERM_UUIDS, puuids.stream()
					.map((uid)->uid.toString())
					.collect(Collectors.toList()));
	}
	
	@Override
	public LockData asMutable() {
		return new LockData(toContainer());
	}
	

	@Override
	public int getContentVersion() {
		return 1;
	}

	@Override
	protected void registerGetters() {
		registerFieldGetter(LockKeys.LOCK_OUUID, this::getLockOwnerID);
		registerFieldGetter(LockKeys.LOCK_ONAME, this::getLockOwnerName);
		registerFieldGetter(LockKeys.LOCK_PUUIDS, this::getLockPermittedIDs);
		registerFieldGetter(LockKeys.LOCK_PNAMES, this::getLockPermittedNames);
	}

}
