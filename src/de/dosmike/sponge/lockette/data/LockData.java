package de.dosmike.sponge.lockette.data;

import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.data.DataContainer;
import org.spongepowered.api.data.DataHolder;
import org.spongepowered.api.data.DataView;
import org.spongepowered.api.data.manipulator.mutable.common.AbstractData;
import org.spongepowered.api.data.merge.MergeFunction;
import org.spongepowered.api.data.value.ValueFactory;
import org.spongepowered.api.data.value.mutable.Value;

import de.dosmike.sponge.lockette.LockDataView;

public class LockData extends AbstractData<LockData, ImmutableLockData> {
	
	private static final ValueFactory vf = Sponge.getRegistry().getValueFactory();
	
	private LockDataView ldv;
	
	public LockData() {
		ldv=new LockDataView();
		registerGettersAndSetters();
	}
	public LockData(LockDataView ldv) {
		this.ldv = ldv; 
		registerGettersAndSetters();
	}
	
	public Value<LockDataView> getLockDataView() {
		return vf.createValue(LockKeys.LOCK, ldv);
	}
	public void setLockDataView(LockDataView lockData) {
		ldv = lockData;
	}
	
	@Override
	public Optional<LockData> fill(DataHolder dataHolder, MergeFunction overlap) {
		LockData ld = overlap.merge(copy(),
				from(dataHolder.toContainer()).orElse(new LockData()));
		Optional<LockDataView> optldv = ld.get(LockKeys.LOCK);
		if (optldv.isPresent()) {
			ld.set(LockKeys.LOCK, optldv.get());
		} else {
			ld.set(LockKeys.LOCK, new LockDataView());
		}
		return Optional.of(ld);
	}

	@Override
	public Optional<LockData> from(DataContainer container) {
		Optional<DataView> dv = container.getView(LockKeys.LOCK.getQuery());
		if (!dv.isPresent()) return Optional.empty();
		ldv = new LockDataView(dv.get());
		return Optional.of(this);
	}
	
	public Optional<LockData> from(DataView container) {
		Optional<DataView> dv = container.getView(LockKeys.LOCK.getQuery());
		if (dv.isPresent())
			ldv = new LockDataView(dv.get());
		return Optional.of(this);
	}

	@Override
	public LockData copy() {
		return new LockData(ldv.copy());
	}

	@Override
	public ImmutableLockData asImmutable() {
		return new ImmutableLockData(ldv);
	}

	@Override
	public int getContentVersion() {
		return 1;
	}

	@Override
	protected void registerGettersAndSetters() {
		registerFieldGetter(LockKeys.LOCK, this::getLockDataView);
		registerFieldSetter(LockKeys.LOCK, this::setLockDataView);
	}

}
