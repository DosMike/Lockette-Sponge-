package de.dosmike.sponge.lockette.data;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.manipulator.immutable.common.AbstractImmutableData;
import org.spongepowered.api.data.value.ValueFactory;
import org.spongepowered.api.data.value.immutable.ImmutableValue;

import de.dosmike.sponge.lockette.LockDataView;

public class ImmutableLockData extends AbstractImmutableData<ImmutableLockData, LockData> {

	public static final ValueFactory vf = Sponge.getRegistry().getValueFactory();
	
	private LockDataView ldv;
	
	public ImmutableLockData() {
		ldv=null;
		registerGetters();
	}
	public ImmutableLockData(LockDataView ldv) {
		this.ldv = ldv; 
		registerGetters();
	}
	
	public ImmutableValue<LockDataView> getLockDataView() {
		return vf.createValue(LockKeys.LOCK, ldv).asImmutable();
	}
	
	@Override
	public LockData asMutable() {
		return new LockData(ldv.copy());
	}

	@Override
	public int getContentVersion() {
		return 1;
	}

	@Override
	protected void registerGetters() {
		registerFieldGetter(LockKeys.LOCK, this::getLockDataView);
	}

}
