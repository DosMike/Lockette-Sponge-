package de.dosmike.sponge.lockette;

import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.BlockSnapshot;
import org.spongepowered.api.block.BlockTypes;
import org.spongepowered.api.data.Transaction;
import org.spongepowered.api.entity.explosive.Explosive;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.event.Listener;
import org.spongepowered.api.event.block.ChangeBlockEvent;
import org.spongepowered.api.event.block.InteractBlockEvent;
import org.spongepowered.api.event.block.tileentity.ChangeSignEvent;
import org.spongepowered.api.event.network.ClientConnectionEvent;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

import com.flowpowered.math.vector.Vector3i;

public class CommonEventListener {
	static final Text locketteSignIdentifier = Text.of(TextColors.DARK_BLUE, "[Lockette]");
	
	PluginContainer owner;
	public CommonEventListener(PluginContainer plugin) {
		owner=plugin;
	}
	
	//not reliable for protection
	@Listener
	public void onBlockBreak(ChangeBlockEvent event) {
		if (event instanceof ChangeBlockEvent.Break) {
			event.getTransactions().forEach(trans -> {
				if (trans.getOriginal().getState().getType().equals(BlockTypes.WALL_SIGN)) {
					LockoutWarningManager.clearUndo(trans.getOriginal().getLocation().get());
				}
			});
		}
	}
	
	@Listener
	public void onExplosion(ExplosionEvent.Detonate event) {
		Optional<Player> source = event.getCause().first(Player.class);
		if (!source.isPresent() && event.getExplosion().getSourceExplosive().isPresent()) {
			Explosive e = event.getExplosion().getSourceExplosive().get();
			Optional<UUID> creator = e.getCreator();
			if (creator.isPresent()) source = Sponge.getServer().getPlayer(creator.get());
		}
		List<Location<World>> denied = new LinkedList<>();
		for (Location<World> block : event.getAffectedLocations())
			if (!Lockette.hasAccess(source.get(), block))
				denied.add(block);
		event.getAffectedLocations().removeAll(denied);
		if (!denied.isEmpty()) {
			source.get().sendMessage(Text.of("[Lockette] There are some blocks you may not blow up"));
		}
	}
	
	@Listener
	public void onChangeSign(ChangeSignEvent event) {
		LockScanner scanner = new LockScanner(event.getTargetTile().getWorld()); 
		Optional<Vector3i> target = 
				scanner.findLockettable(event.getTargetTile().getLocation().getBlockPosition());
		if (!target.isPresent()) return;
		
		List<Text> lines = event.getText().getListValue().get();
		Optional<Player> source = event.getCause().first(Player.class);
		if (lines.get(0).toPlain().equalsIgnoreCase("[private]")) {
			//something tries to lock this container
			if (source.isPresent()) {
				//a player tries to lock, check privileges
				if (!Lockette.hasAccess(source.get(), scanner.getExtentDelta(), target.get())) {
					source.get().sendMessage(Text.of("[Lockette] You are not permitted to add additional locks"));
					event.setCancelled(true);
					return;
				}
				//write at least the creators name on the sign if empty
				boolean empty = true;
				for (int i = 1; i < lines.size(); i++) {
					if (!lines.get(i).isEmpty()) empty = false;
				}
				if (empty) lines.set(1, Text.of(source.get().getName()));
				source.get().sendMessage(Text.of("[Lockette] The lock was successfully added"));
				LockoutWarningManager.checkLockout(source.get(), event.getTargetTile().getLocation());
			} else {
				Lockette.log("[Lockette] Locked by magic!");
			}

			//if the source is no player we assume something with admin rights is doing this (probably another plugin)
			lines.set(0, locketteSignIdentifier);
			event.getText().setElements(lines);
		}
	}
	
	@Listener
	public void onInteract(InteractBlockEvent event) {
		Optional<Location<World>> target = event.getTargetBlock().getLocation();
		if (!target.isPresent()) return; //skip dis - we dont't need nuthin
		Optional<Player> source = event.getCause().first(Player.class);
		if (!source.isPresent()) return; //no need to block?
		if (!Lockette.hasAccess(source.get(), target.get())) {
			source.get().sendMessage(Text.of("[Lockette] You may not do this"));
			event.setCancelled(true);
		}
	}
	
	@Listener
	public void onBlockPlace(ChangeBlockEvent.Place event) {
		Optional<Player> source = event.getCause().first(Player.class);
		if (!source.isPresent()) return; //no need to block?
		
		ExtentDelta.Builder<World> dd=null;
		for (Transaction<BlockSnapshot> t : event.getTransactions()) {
			if (dd == null) dd = ExtentDelta.builder(t.getOriginal().getLocation().get().getExtent());
			dd.addDelta(t.getFinal());
		}
		ExtentDelta<World> delta = dd.build();
		
		boolean blocked=false;
		for (Transaction<BlockSnapshot> t : event.getTransactions()) {
			Vector3i above = t.getOriginal().getPosition().add(0, 1, 0);
			if (!Lockette.hasAccess(source.get(), delta, above)) {
				t.setValid(false);
				blocked=true;
			}
		}
		if (blocked) source.get().sendMessage(Text.of("[Lockette] Some blocks were not placed as they would lock"));
	}
	
	@Listener
	public void onPlayerDisconnect(ClientConnectionEvent.Disconnect event) {
		LockoutWarningManager.clearUndo(event.getTargetEntity());
	}
}
