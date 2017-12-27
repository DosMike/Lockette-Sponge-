package de.dosmike.sponge.lockette.data;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.OptionalValue;
import org.spongepowered.api.data.value.mutable.Value;

import com.google.common.reflect.TypeToken;

public class LockKeys {

	@SuppressWarnings("serial")
	public static final Key<Value<LockData>> LOCK = KeyFactory.makeSingleKey(
			TypeToken.of(LockData.class),
			new TypeToken<Value<LockData>>() {},
			DataQuery.of("Lockette"),
			"dosmike_lockette:lockdata",
			"Lock Data");
	
	@SuppressWarnings("serial")
	static final Key<OptionalValue<String>> LOCK_ONAME = KeyFactory.makeOptionalKey(
			new TypeToken<Optional<String>>() {},
			new TypeToken<OptionalValue<String>>() {},
			LockDataQueries.OWNER_NAME,
			"dosmike_lockette:lock_oname",
			"lock owner name");
	
	@SuppressWarnings("serial")
	static final Key<OptionalValue<UUID>> LOCK_OUUID = KeyFactory.makeOptionalKey(
			new TypeToken<Optional<UUID>>() {},
			new TypeToken<OptionalValue<UUID>>() {},
			LockDataQueries.OWNER_UUID,
			"dosmike_lockette:lock_ouuid",
			"lock owner id");
	
	@SuppressWarnings("serial")
	static final Key<ListValue<String>> LOCK_PNAMES = KeyFactory.makeListKey(
			new TypeToken<List<String>>() {},
			new TypeToken<ListValue<String>>() {},
			LockDataQueries.PERM_NAMES,
			"dosmike_lockette:lock_pnames",
			"lock permitted names");
	
	@SuppressWarnings("serial")
	static final Key<ListValue<UUID>> LOCK_PUUIDS = KeyFactory.makeListKey(
			new TypeToken<List<UUID>>() {},
			new TypeToken<ListValue<UUID>>() {},
			LockDataQueries.PERM_UUIDS,
			"dosmike_lockette:lock_puuids",
			"lock permitted ids");
	
}
