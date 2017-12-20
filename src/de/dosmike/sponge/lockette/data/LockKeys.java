package de.dosmike.sponge.lockette.data;

import org.spongepowered.api.data.DataQuery;
import org.spongepowered.api.data.key.Key;
import org.spongepowered.api.data.key.KeyFactory;
import org.spongepowered.api.data.value.mutable.Value;

import com.google.common.reflect.TypeToken;

import de.dosmike.sponge.lockette.LockDataView;

public class LockKeys {

	@SuppressWarnings("serial")
	public static final Key<Value<LockDataView>> LOCK = KeyFactory.makeSingleKey(
			TypeToken.of(LockDataView.class),
			new TypeToken<Value<LockDataView>>() {},
			DataQuery.of("Lockette"),
			"lockette:lock",
			"lock");
	
}
