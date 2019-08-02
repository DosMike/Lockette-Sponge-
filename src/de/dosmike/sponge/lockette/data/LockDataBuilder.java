package de.dosmike.sponge.lockette.data;

import java.util.Optional;

import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.DataManipulatorBuilder;
import org.spongepowered.api.data.persistence.AbstractDataBuilder;
import org.spongepowered.api.data.persistence.InvalidDataException;

public class LockDataBuilder extends AbstractDataBuilder<LockData> implements DataManipulatorBuilder<LockData, ImmutableLockData> {

	private static final int CONTENT_VERSION = 1;
	
	public LockDataBuilder() {
		super(LockData.class, CONTENT_VERSION);
	}

	@Override
	public LockData create() {
		return new LockData();
	}

	@Override
	public Optional<LockData> createFrom(DataHolder dataHolder) {
		Optional<LockData> old = create().fill(dataHolder);
		return old;
	}

	@Override
	protected Optional<LockData> buildContent(DataView container) throws InvalidDataException {
		Optional<LockData> old = create().from(container);
		return old;
	}
	
	
	
}
