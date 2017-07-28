package de.dosmike.sponge.lockette;

import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Optional;
import java.util.Set;
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
import org.spongepowered.api.event.cause.Cause;
import org.spongepowered.api.event.world.ExplosionEvent;
import org.spongepowered.api.plugin.PluginContainer;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.world.Location;
import org.spongepowered.api.world.World;

public class CommonEventListener {
	static final Text locketteSignIdentifier = Text.of(TextColors.DARK_BLUE, "[Lockette]");
	
	PluginContainer owner;
	public CommonEventListener(PluginContainer plugin) {
		owner=plugin;
	}
	
	@Listener
	public void onBlockBreak(ChangeBlockEvent.Break event) {
		if (event.getCause().contains(owner)) {
			Lockette.log("Change by Lockette (Authorised break lock)");
			return;
		}
		
		Optional<Player> source = event.getCause().first(Player.class);
		if (!source.isPresent()) {
			Lockette.log("Change not caused by player");
			return;
		}
		Lockette.log("Change caused by " + source.get().getName());
		
		Set<Location<World>> directlyAffected = new HashSet<>();
		Set<Location<World>> targets = new HashSet<>();
		Set<Lock> locks = new HashSet<>();
		List<Transaction<BlockSnapshot>> trans = event.getTransactions();
		for (Transaction<BlockSnapshot> tran : trans) {
			if (tran.isValid()) {
				Optional<Location<World>> src = tran.getOriginal().getLocation();
				if (src.isPresent()) {
					targets.addAll(Lockette.getInvolvedBlocks(src.get()));
					directlyAffected.add(src.get());
				}
			}
		}
		locks.addAll(Lockette.signsToLocks(targets));
		if (locks.isEmpty()) {
			Lockette.log("No locks found");
			return;
		}
		
		if (!Lockette.hasAccess(source.get(), locks)) {
			Lockette.log("Source not authorized!");
			event.setCancelled(true);
		}
		
		Cause breakAllLocks = Cause.source(owner).suggestNamed("Authorized break lock", event.getCause()).build();
		targets.removeAll(directlyAffected);
		for (Location<World> target : targets) {
			target.setBlockType(BlockTypes.AIR, breakAllLocks);
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
		Optional<Location<World>> target = Lockette.findLockettable(event.getTargetTile().getLocation());
		if (!target.isPresent()) return;
		
		List<Text> lines = event.getText().getListValue().get();
		Optional<Player> source = event.getCause().first(Player.class);
		if (lines.get(0).toPlain().equalsIgnoreCase("[private]")) {
			//something tries to lock this container
			if (source.isPresent()) {
				//a player tries to lock, check privileges
				if (!Lockette.hasAccess(source.get(), target.get())) {
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
			} else {
				Lockette.log("[Lockette] Locked by magic!");
			}
			//Lockette.log(event.getCause().toString());

			//if the source is no player we assume something with admin rights is doing this (probably another plugin)
			lines.set(0, locketteSignIdentifier);
			event.getText().setElements(lines);
		}
	}
	
	@Listener
	public void onInteract(InteractBlockEvent.Secondary event) {
		Optional<Location<World>> target = event.getTargetBlock().getLocation();
		if (!target.isPresent()) return; //skip dis - we dont't need nuthin
		Optional<Player> source = event.getCause().first(Player.class);
		if (!source.isPresent()) return; //no need to block?
		if (!Lockette.hasAccess(source.get(), target.get())) {
			source.get().sendMessage(Text.of("[Lockette] You may not do this"));
			event.setCancelled(true);
		}
	}
}
