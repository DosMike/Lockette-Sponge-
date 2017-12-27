package de.dosmike.sponge.lockette.data;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.ValueFactory;
import org.spongepowered.api.data.value.mutable.Value;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileCache;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import de.dosmike.sponge.lockette.Lock;

public class LockData extends AbstractData<LockData, ImmutableLockData> {
	
	private static final ValueFactory vf = Sponge.getRegistry().getValueFactory();
	
	private Optional<String> oname;
	private Optional<UUID> ouuid;
	private List<UUID> puuids; 
	private List<String> pnames; 
	
	public LockData() {
		oname=Optional.empty();
		ouuid=Optional.empty();
		puuids=new LinkedList<>();
		pnames=new LinkedList<>();
		registerGettersAndSetters();
	}
	public LockData(DataView container) {
		registerGettersAndSetters();
		from(container);		
	}
	
	public Value<Optional<String>> getLockOwnerName() {
		return vf.createOptionalValue(LockKeys.LOCK_ONAME, oname.orElse(null));
	}
	public void setLockOwnerName(Optional<String> ownerName) {
		oname = ownerName;
	}
	public Value<Optional<UUID>> getLockOwnerID() {
		return vf.createOptionalValue(LockKeys.LOCK_OUUID, ouuid.orElse(null));
	}
	public void setLockOwnerID(Optional<UUID> ownerUUID) {
		ouuid = ownerUUID;
	}
	public Value<List<String>> getLockPermittedNames() {
		return vf.createListValue(LockKeys.LOCK_PNAMES, pnames);
	}
	public void setLockPermittedNames(List<String> permittedNames) {
		pnames = permittedNames;
	}
	public Value<List<UUID>> getLockPermittedIDs() {
		return vf.createListValue(LockKeys.LOCK_PUUIDS, puuids);
	}
	public void setLockPermittedIDs(List<UUID> permittedUUIDs) {
		puuids = permittedUUIDs;
	}
	
	public boolean isLocketteHolder() {
		return ouuid.isPresent();
	}
	public boolean isPermitted(UUID target) {
		return puuids.contains(target);
	}
	public void permit(GameProfile target) {
		if (puuids.contains(target)) return;
		puuids.add(target.getUniqueId());
		pnames.add(target.getName().orElse("?"));
	}
	public void deny(GameProfile target) {
		int  i = puuids.indexOf(target.getUniqueId());
		if (i<0) return;
		pnames.remove(i);
		puuids.remove(i);
	}
	public void setOwner(GameProfile owner) {
		ouuid = Optional.of(owner.getUniqueId());
		oname = Optional.of(owner.getName().orElse("?"));
	}
	
	public void update() {
		GameProfileCache cache = Sponge.getServer().getGameProfileManager().getCache();
		if (!ouuid.isPresent() || !oname.isPresent()) return;
		cache.getOrLookupById(ouuid.get()).ifPresent(profile->oname=Optional.of(profile.getName().orElse(oname.get())));;
		for (int i=0; i<puuids.size(); i++) {
			final int z = i;
			cache.getOrLookupById(puuids.get(i)).ifPresent(profile->pnames.set(z, profile.getName().orElse("?")));
		}
	}
	
	public Lock toLock(Location<World> reference) {
		return new Lock(reference, ouuid.get(), puuids);
	}
	
	@Override
	public Optional<LockData> fill(DataHolder dataHolder, MergeFunction overlap) {
		LockData ld = overlap.merge(copy(),
				from(dataHolder.toContainer()).orElse(new LockData()));
		set(LockKeys.LOCK_OUUID, ld.get(LockKeys.LOCK_OUUID).orElse(Optional.empty()));
		set(LockKeys.LOCK_ONAME, ld.get(LockKeys.LOCK_ONAME).orElse(Optional.empty()));
		set(LockKeys.LOCK_PUUIDS, ld.get(LockKeys.LOCK_PUUIDS).orElse(new LinkedList<>()));
		set(LockKeys.LOCK_PNAMES, ld.get(LockKeys.LOCK_PNAMES).orElse(new LinkedList<>()));
		return Optional.of(this);
	}

	@Override
	public Optional<LockData> from(DataContainer container) {
		return from((DataView)container);
		
	}
	
	public Optional<LockData> from(DataView view) {
		if (!view.contains(LockKeys.LOCK.getQuery())) {
//			throw new RuntimeException("Not Lock");
			return Optional.empty();
		}
		
		String name = view.getString(LockDataQueries.OWNER_NAME).orElse(null);
		if (name==null) oname = Optional.empty(); else oname = Optional.of(name);
		UUID uuid = view.getObject(LockDataQueries.OWNER_UUID, UUID.class).orElse(null);
		if (uuid==null) ouuid = Optional.empty(); else ouuid = Optional.of(uuid);
		
		pnames = view.getStringList(LockDataQueries.PERM_NAMES).orElse(new LinkedList<>());
		List<String> uuids = view.getStringList(LockDataQueries.PERM_UUIDS).orElse(new LinkedList<>());
		puuids = uuids.stream().map((uid)->UUID.fromString(uid)).collect(Collectors.toList());
		
		return Optional.of(this);
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
	public LockData copy() {
		return new LockData(toContainer());
	}

	@Override
	public ImmutableLockData asImmutable() {
		return new ImmutableLockData(toContainer());
	}

	@Override
	public int getContentVersion() {
		return 1;
	}

	@Override
	protected void registerGettersAndSetters() {
		registerFieldGetter(LockKeys.LOCK_OUUID, this::getLockOwnerID);
		registerFieldGetter(LockKeys.LOCK_ONAME, this::getLockOwnerName);
		registerFieldGetter(LockKeys.LOCK_PUUIDS, this::getLockPermittedIDs);
		registerFieldGetter(LockKeys.LOCK_PNAMES, this::getLockPermittedNames);
		registerFieldSetter(LockKeys.LOCK_OUUID, this::setLockOwnerID);
		registerFieldSetter(LockKeys.LOCK_ONAME, this::setLockOwnerName);
		registerFieldSetter(LockKeys.LOCK_PUUIDS, this::setLockPermittedIDs);
		registerFieldSetter(LockKeys.LOCK_PNAMES, this::setLockPermittedNames);
	}

}
