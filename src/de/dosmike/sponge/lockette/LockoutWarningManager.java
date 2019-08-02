package de.dosmike.sponge.lockette;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.block.tileentity.TileEntity;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.profile.property.ProfileProperty;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.google.common.collect.Multimap;

import de.dosmike.sponge.lockette.data.LockData;

//delete?
public class LockoutWarningManager {

	static final String profileKey = "dosmike_lockette";
	static final String profileUndoLocation = "undo_location";
	static final String profileUndoTime = "undo_time";
	
	static void checkLockout(Player source, Location<World> target) {
		Sponge.getScheduler().createTaskBuilder().delayTicks(1).execute(()->{
			if (!Lockette.hasAccess(source.getUniqueId(), target)) {
				Multimap<String, ProfileProperty> pp = source.getProfile().getPropertyMap();
				pp.removeAll(profileKey);
				pp.put(profileKey, ProfileProperty.of(profileUndoLocation, LockSerializer.loc2str(target)));
				pp.put(profileKey, ProfileProperty.of(profileUndoTime, String.valueOf(System.currentTimeMillis())));
				LockoutWarningManager.createWarning(source);
			}
		}).submit(Lockette.getInstance());
	}
	
	static void createWarning(Player source) {
		source.sendMessage(Text.of("[Lockette] ", TextColors.RED,"The lock you just places prevents you from accessing the container/door"));
		try {
			Optional<ProfileProperty> maybeAt = getProperty(source, profileKey, profileUndoLocation);
			if (!maybeAt.isPresent()) {
				source.sendMessage(Text.of("Sadly I can not provide UNDO for this action (1)"));
				return;
			}
			Location<World> at = LockSerializer.str2loc(maybeAt.get().getValue());
			Optional<TileEntity> maybeSign = at.getTileEntity();
			if (!maybeSign.isPresent() || !(maybeSign.get() instanceof Sign)) {
				source.sendMessage(Text.of("Sadly I can not provide UNDO for this action (2)"));
				return;
			}
			Sign sign = (Sign)maybeSign.get();
			LockData signdata = new LockData(sign.toContainer());
			if (signdata.isLocketteHolder()) {
				List<Text> actions = new LinkedList<>();
				actions.add(Text.builder("UNDO").color(TextColors.RED)
					.onClick(TextActions.executeCallback(cs -> {
						if (!(cs instanceof Player)) return;
						Player player = (Player)cs;
						callbackUndo(player);
					}))
					.onHover(TextActions.showText(Text.of("Makes this a normal sign")))
					.build());
				actions.add(Text.builder("ADD ME").color(TextColors.GREEN)
						.onClick(TextActions.executeCallback(cs -> {
							if (!(cs instanceof Player)) return;
							Player player = (Player)cs;
							callbackAdd(player);
						}))
						.onHover(TextActions.showText(Text.of("Add your name to the list")))
						.build());
				Text.Builder builder = Text.builder();
				for (Text act : actions) {
					builder.append(Text.of(" [", act, "] "));
				}
				source.sendMessage(Text.of("If this was an accident you have these options:"));
				source.sendMessage(builder.build());
			}
		} catch (Exception e) {
			source.sendMessage(Text.of("Sadly I can not provide UNDO for this action"));
			e.printStackTrace();
		}
	}
	
	static void callbackUndo(Player player) {
		Collection<ProfileProperty> pp = player.getProfile().getPropertyMap().get(profileKey);
		Optional<ProfileProperty> when = getProperty(pp, profileUndoTime);
		if (!when.isPresent() || System.currentTimeMillis() - Long.parseLong(when.get().getValue()) > 60000) {
			player.sendMessage(Text.of("[Lockette] Sorry, this UNDO expired"));
			return;
		}
		Location<World> at = LockSerializer.str2loc(getProperty(pp, profileUndoLocation).get().getValue());
		Sign sign = (Sign)at.getTileEntity().get();
		
		DataWrapper.removeLockKey(sign);
		
		SignData data = sign.getSignData();
		data.setElement(0, Text.of());
		sign.offer(data);
		player.sendMessage(Text.of("[Lockette] Your action was undone"));
		clearUndo(player);
	}
	static void callbackAdd(Player player) {
		Collection<ProfileProperty> pp = player.getProfile().getPropertyMap().get(profileKey);
		Optional<ProfileProperty> when = getProperty(pp, profileUndoTime);
		if (!when.isPresent() || System.currentTimeMillis() - Long.parseLong(when.get().getValue()) > 60000) {
			player.sendMessage(Text.of("[Lockette] Sorry, this UNDO expired"));
			return;
		}
		Location<World> at = LockSerializer.str2loc(getProperty(pp, profileUndoLocation).get().getValue());
		Sign sign = (Sign)at.getTileEntity().get();
		
		DataWrapper.getLockKey(sign).ifPresent(ldata->{
			ldata.permit(player.getProfile());
			sign.offer(/*LockKeys.LOCK,*/ ldata);
			SignData data = sign.getSignData();
			data.setElement(data.getListValue().indexOf(Text.of()), Text.of(player.getName()));
			sign.offer(data);
			player.sendMessage(Text.of("[Lockette] Your name was added to the lock"));
		});
		clearUndo(player);
	}
	
	static void clearUndo(Player player) {
		Multimap<String, ProfileProperty> pp = player.getProfile().getPropertyMap();
		pp.removeAll(profileKey);		
	}
	static void clearUndo(Location<World> at) {
		Sponge.getServer().getOnlinePlayers().forEach(player -> {
			Optional<ProfileProperty> saved = getProperty(player, profileKey, profileUndoLocation);
			if (saved.isPresent() && at.equals(LockSerializer.str2loc(saved.get().getValue()))) { 
				clearUndo(player);
				player.sendMessage(Text.of("[Lockette] Your undo expired!"));
			}
		});
	}
	
	private static Optional<ProfileProperty> getProperty(Player source, String key, String name) {
		Multimap<String, ProfileProperty> pp = source.getProfile().getPropertyMap();
		if (!pp.containsKey(key)) return Optional.empty();
		return getProperty(pp.get(key), name);
	}
	private static Optional<ProfileProperty> getProperty(Collection<ProfileProperty> properties, String name) {
		for (ProfileProperty pp : properties)
			if (pp.getName().equals(name)) return Optional.of(pp);
		return Optional.empty();
	}
}
