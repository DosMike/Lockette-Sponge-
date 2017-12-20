package de.dosmike.sponge.lockette;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataSerializable;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.profile.GameProfile;
import org.spongepowered.api.profile.GameProfileCache;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import de.dosmike.sponge.lockette.data.LockDataQueries;

public class LockDataView implements DataSerializable {
	
	private Optional<String> oname;
	private Optional<UUID> ouuid;
	private Map<UUID, String> permitted; 
	
	public LockDataView() {
		oname=Optional.empty();
		ouuid=Optional.empty();
		permitted=new HashMap<>();
	}
	public LockDataView(DataView container) {
		String name = container.getString(LockDataQueries.OWNER_NAME).orElse(null);
		if (name==null) oname = Optional.empty(); else oname = Optional.of(name);
		String uuid = container.getString(LockDataQueries.OWNER_UUID).orElse(null);
		if (uuid==null) ouuid = Optional.empty(); else ouuid = Optional.of(UUID.fromString(uuid));
		
		List<String> names = container.getStringList(LockDataQueries.PERM_NAMES).orElse(new LinkedList<>());
		List<String> uuids = container.getStringList(LockDataQueries.PERM_UUIDS).orElse(new LinkedList<>());
		
		if (names.size()!=uuids.size() || names.isEmpty()) {
			permitted = new HashMap<>();
			return;
		}
		for (int i = 0; i<names.size(); i++) {
			permitted.put(UUID.fromString(uuids.get(i)), names.get(i));
		}
	}
	
	public boolean isLocketteHolder() {
		return ouuid.isPresent();
	}
	
	public Optional<String> getOwnerName() {
		return oname;
	}
	public Optional<UUID> getOwnerUUID() {
		return ouuid;
	}
	public Optional<GameProfile> getOrLookupOwner() {
		if (ouuid.isPresent())
			return Sponge.getServer().getGameProfileManager().getCache().getOrLookupById(ouuid.get());
		return Optional.empty();
	}
	public Map<UUID, String> getPermitted() {
		return permitted;
	}
	
	public void permit(GameProfile target) {
		permitted.put(target.getUniqueId(), target.getName().orElse("?"));
	}
	public void deny(GameProfile target) {
		permitted.remove(target.getUniqueId());
	}
	public void setOwner(GameProfile owner) {
		ouuid = Optional.of(owner.getUniqueId());
		oname = Optional.of(owner.getName().orElse("?"));
	}
	
	public void update() {
		GameProfileCache cache = Sponge.getServer().getGameProfileManager().getCache();
		if (!ouuid.isPresent() || !oname.isPresent()) return;
		cache.getOrLookupById(ouuid.get()).ifPresent(profile->oname=Optional.of(profile.getName().orElse(oname.get())));;
		for (UUID id : permitted.keySet()) {
			cache.getOrLookupById(id).ifPresent(profile->permitted.put(id, profile.getName().orElse(permitted.get(id))));
		}
	}
	
	public Lock toLock(Location<World> reference) {
		List<UUID> perm = new ArrayList<>(permitted.size());
		perm.addAll(permitted.keySet());
		return new Lock(reference, ouuid.get(), perm);
	}
	

	public LockDataView copy() {
		LockDataView ldv = new LockDataView();
		if (oname.isPresent()) ldv.oname = Optional.of(oname.get());
		else ldv.oname = Optional.empty();
		if (ouuid.isPresent()) ldv.ouuid = Optional.of(ouuid.get());
		else ldv.oname = Optional.empty();
		for (Entry<UUID, String> e : permitted.entrySet()) {
			ldv.permitted.put(e.getKey(), e.getValue());
		}
		return ldv;
	}
	
	@Override
	public int getContentVersion() {
		return 1;
	}
	
	@Override
	public DataContainer toContainer() {
		List<String> names = new ArrayList<>(permitted.size());
		List<String> uuids = new ArrayList<>(permitted.size());
		for (Entry<UUID, String> s : permitted.entrySet()) {
			uuids.add(s.getKey().toString());
			names.add(s.getValue());
		}
		
		return DataContainer.createNew()
				.set(LockDataQueries.OWNER_NAME, oname)
				.set(LockDataQueries.OWNER_UUID, ouuid)
				.set(LockDataQueries.PERM_NAMES, names)
				.set(LockDataQueries.OWNER_UUID, uuids);
	}
}
