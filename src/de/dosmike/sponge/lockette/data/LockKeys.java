package de.dosmike.sponge.lockette.data;

import com.google.common.reflect.TypeToken;
import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.value.mutable.ListValue;
import org.spongepowered.api.data.value.mutable.OptionalValue;
import org.spongepowered.api.data.value.mutable.Value;

import java.util.UUID;

public class LockKeys {

	public static final Key<Value<LockData>> LOCK = Key.builder()
			.id("dosmike_lockette:lockdata")
			.name("Lock Data")
			.query(DataQuery.of("Lockette"))
			.type(new TypeToken<Value<LockData>>() {})
			.build();

	public static final Key<OptionalValue<String>> LOCK_ONAME = Key.builder()
			.id("dosmike_lockette:lock_oname")
			.name("lock owner name")
			.query(LockDataQueries.OWNER_NAME)
			.type(new TypeToken<OptionalValue<String>>() {})
			.build();

	public static final Key<OptionalValue<UUID>> LOCK_OUUID = Key.builder()
			.id("dosmike_lockette:lock_ouuid")
			.name("lock owner id")
			.query(LockDataQueries.OWNER_NAME)
			.type(new TypeToken<OptionalValue<UUID>>() {})
			.build();

	public static final Key<ListValue<String>> LOCK_PNAMES = Key.builder()
			.id("dosmike_lockette:lock_pnames")
			.name("lock permitted names")
			.query(LockDataQueries.OWNER_NAME)
			.type(new TypeToken<ListValue<String>>() {})
			.build();

	public static final Key<ListValue<UUID>> LOCK_PUUIDS = Key.builder()
			.id("dosmike_lockette:lock_puuids")
			.name("lock permitted ids")
			.query(LockDataQueries.OWNER_NAME)
			.type(new TypeToken<ListValue<UUID>>() {})
			.build();

}
