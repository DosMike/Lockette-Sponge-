package de.dosmike.sponge.lockette;

import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Consumer;

import org.spongepowered.api.Sponge;
import org.spongepowered.api.block.tileentity.Sign;
import org.spongepowered.api.command.CommandSource;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.tileentity.SignData;
import org.spongepowered.api.entity.living.player.Player;
import org.spongepowered.api.entity.living.player.User;
import org.spongepowered.api.statistic.Statistics;
import org.spongepowered.api.text.BookView;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.action.TextActions;
import org.spongepowered.api.text.format.TextColors;

import com.flowpowered.math.vector.Vector3i;

import de.dosmike.sponge.lockette.data.LockKeys;

//"        [Lockette]"
//(19-Namelen-"Owner: "-len /2-1(good measure)*' ')"Owner: "+Name
//"   [Unlock]   [Permit]"

//Name
//"  [-] [>]"
//members auf der ersten seite als owner, 10 als viewer 

public class BookViewManager {
	
	public static abstract class LockCallback implements Consumer<CommandSource> {
		private Lock targetLock;
		private UUID otherUser;
		public Lock getTarget() { return targetLock; }
		public UUID getOther() { return otherUser; }
		public LockCallback(Lock targetLock) {
			this.targetLock = targetLock;
			this.otherUser = null;
		}
		public LockCallback(Lock targetLock, UUID otherUser) {
			this.targetLock = targetLock;
			this.otherUser = otherUser;
		}
		public abstract void accept(CommandSource t);
	}

	public static void displayMenuMembersView(Player viewer, Vector3i lockSign) {
		Set<Text> pages = new HashSet<>();
		LockScanner ls = new LockScanner(viewer.getWorld());
		ls.getSignData(lockSign).ifPresent(lockData->{
			if (!lockData.isLocketteHolder()) return;
			Collection<Lock> allLocks = ls.getLocksFor(lockSign);
			Lock thisLock = lockData.toLock(viewer.getWorld().getLocation(lockSign));
			allLocks.remove(thisLock);
			
			Text page = Text.of(TextColors.BLUE, "        [Lockette]", Text.NEW_LINE,
					space((16-lockData.getOwnerName().orElse("?").length())/2-1), TextColors.RESET, "Owner: ", lockData.getOwnerName().orElse("?"), Text.NEW_LINE); 
			
			int n=10;
			for (UUID entry : thisLock.permitted) {
				page = Text.of(page, (page.isEmpty()?Text.EMPTY:Text.NEW_LINE), TextColors.RESET, playerDecor(entry));
				
				if (n==0) {
					pages.add(page);
					page = Text.EMPTY;
					n=14;
				}
			}
			for (Lock lock : allLocks)
				for (UUID entry : lock.permitted) {
					page = Text.of(page, (page.isEmpty()?Text.EMPTY:Text.NEW_LINE), TextColors.DARK_GRAY, playerDecor(entry));
					
					if (n==0) {
						pages.add(page);
						page = Text.EMPTY;
						n=14;
					}
				}
		});
		BookView b = BookView.builder().author(LockScanner.locketteSignIdentifier)
				.title(LockScanner.locketteSignIdentifier)
				.addPages(pages)
				.build();
		viewer.sendBookView(b);
	}
	
	public static void displayMenuOwnerView(Player viewer, Vector3i lockSign) {
		Set<Text> pages = new HashSet<>();
		LockScanner ls = new LockScanner(viewer.getWorld());
		ls.getSignData(lockSign).ifPresent(lockData->{
			if (!lockData.isLocketteHolder()) return;
			Collection<Lock> allLocks = ls.getLocksFor(lockSign);
			Lock thisLock = lockData.toLock(viewer.getWorld().getLocation(lockSign));
			allLocks.remove(thisLock);
			
			Text page = Text.of(TextColors.BLUE, "        [Lockette]", Text.NEW_LINE,
					space((16-lockData.getOwnerName().orElse("?").length())/2-1), TextColors.RESET, "Owner: ", lockData.getOwnerName().orElse("?"), Text.NEW_LINE); 
			page = Text.of(page, space(3), "[", TextColors.RED, Text.builder("Unlock")
					.onClick(TextActions.executeCallback(new LockCallback(thisLock) {
						@Override
						public void accept(CommandSource t) {
							if (t instanceof Player) breakLock((Player)t, getTarget());
						}
					})).build(), "]   [", TextColors.RED, Text.builder("Permit")
					.onClick(TextActions.executeCallback(new LockCallback(thisLock) {
						@Override
						public void accept(CommandSource t) {
							if (t instanceof Player) breakLock((Player)t, getTarget());
						}
					})).build()
					);
			
			int n=5;
			for (UUID entry : thisLock.permitted) {
				page = Text.of(page, (page.isEmpty()?Text.EMPTY:Text.NEW_LINE), TextColors.RESET, playerDecor(entry), Text.NEW_LINE,
						"  ", Text.builder("[-]").color(TextColors.RED).onHover(TextActions.showText(Text.of("Remove Acces")))
								.onClick(TextActions.executeCallback(new LockCallback(thisLock, entry) {
									@Override
									public void accept(CommandSource t) {
										if (t instanceof Player) denyUserLock((Player)t, getTarget(), getOther());
									}
								})).build(),
								" ",
								Text.builder("[-]").color(TextColors.RED).onHover(TextActions.showText(Text.of("Transfer Ownership")))
								.onClick(TextActions.executeCallback(new LockCallback(thisLock, entry) {
									@Override
									public void accept(CommandSource t) {
										if (t instanceof Player) transferLock((Player)t, getTarget(), getOther());
									}
								})).build()
						);
				
				if (n==0) {
					pages.add(page);
					page = Text.EMPTY;
					n=7;
				}
			}
			n*=2; //other locks can not be edited through this one
			for (Lock lock : allLocks)
				for (UUID entry : lock.permitted) {
					page = Text.of(page, (page.isEmpty()?Text.EMPTY:Text.NEW_LINE), TextColors.DARK_GRAY, playerDecor(entry));
					
					if (n==0) {
						pages.add(page);
						page = Text.EMPTY;
						n=14;
					}
				}
		});
		BookView b = BookView.builder().author(LockScanner.locketteSignIdentifier)
				.title(LockScanner.locketteSignIdentifier)
				.addPages(pages)
				.build();
		viewer.sendBookView(b);
	}
	
	public static void displayMenuOwnerPermit(Player viewer, Vector3i lockSign) {
		Set<Text> pages = new HashSet<>();
		LockScanner ls = new LockScanner(viewer.getWorld());
		ls.getSignData(lockSign).ifPresent(lockData->{
			if (!lockData.isLocketteHolder()) return;
			Set<Lock> allLocks = ls.getLocksFor(lockSign);
			Lock thisLock = lockData.toLock(viewer.getWorld().getLocation(lockSign));
			allLocks.remove(thisLock);
			
			Text page = Text.of(TextColors.BLUE, "        [Lockette]", Text.NEW_LINE,
					space(3), TextColors.RESET, "   Add user to Lock", Text.NEW_LINE); 
			
			
			int n=10;
			for (Player oplayer : Sponge.getServer().getOnlinePlayers()) {
				page = Text.of(page, (page.isEmpty()?Text.EMPTY:Text.NEW_LINE), (thisLock.permitted.contains(oplayer.getUniqueId()) || Lockette.hasAccess(oplayer, allLocks))
						? Text.of(TextColors.DARK_GRAY, "[ ] ", playerDecor(oplayer))
						: Text.of(Text.builder("[+]").color(TextColors.DARK_GREEN).onHover(TextActions.showText(Text.of("Grant Acces")))
							.onClick(TextActions.executeCallback(new LockCallback(thisLock, oplayer.getUniqueId()) {
								@Override
								public void accept(CommandSource t) {
									if (t instanceof Player) allowUserLock((Player)t, getTarget(), getOther());
								}
							})).build(),
							" ", playerDecor(oplayer))
						);
				
				if (n==0) {
					pages.add(page);
					page = Text.EMPTY;
					n=14;
				}
			}
			for (Lock lock : allLocks)
				for (UUID entry : lock.permitted) {
					page = Text.of(page, (page.isEmpty()?Text.EMPTY:Text.NEW_LINE), TextColors.DARK_GRAY, playerDecor(entry));
					
					if (n==0) {
						pages.add(page);
						page = Text.EMPTY;
						n=14;
					}
				}
		});
		BookView b = BookView.builder().author(LockScanner.locketteSignIdentifier)
				.title(LockScanner.locketteSignIdentifier)
				.addPages(pages)
				.build();
		viewer.sendBookView(b);
	}
	
	private static Text playerDecor(UUID player) {
		Optional<User> u = Lockette.getUser(player);
		String lastSeen = u.get().isOnline()?"Online":
			DateFormat.getDateInstance().format(new Date(
					u.get().get(Keys.STATISTICS).get().get(Statistics.LEAVE_GAME).longValue()
					));
		return Text.builder(u.get().getName())
				.onHover(TextActions.showText(Text.of(
						TextColors.WHITE, "Last Online: ", (u.get().isOnline()?TextColors.GREEN:TextColors.GRAY), lastSeen, Text.NEW_LINE,
						TextColors.GRAY, player
						)))
				.build();
	}
	private static Text playerDecor(User player) {
		String lastSeen = player.isOnline()?"Online":
			DateFormat.getDateInstance().format(new Date(
					player.get(Keys.STATISTICS).get().get(Statistics.LEAVE_GAME).longValue()
					));
		return Text.builder(player.getName())
				.onHover(TextActions.showText(Text.of(
						TextColors.WHITE, "Last Online: ", (player.isOnline()?TextColors.GREEN:TextColors.GRAY), lastSeen, Text.NEW_LINE,
						TextColors.GRAY, player.getUniqueId()
						)))
				.build();
	}
	
	private static String space(int n) {
		String res = "";
		for (;n>0; n--) res+=' ';
		return res;
	}
	
	public static void breakLock(Player player, Lock lock) {
		if (lock.isOwner(player)) {
			lock.getTarget().getTileEntity().ifPresent(ent -> {
				if (ent instanceof Sign) {
					ent.get(LockKeys.LOCK).ifPresent(data->{
						ent.remove(LockKeys.LOCK);
						
						SignData sd = ((Sign)ent).getSignData();
						sd.setElement(0, Text.of());
						ent.offer(sd);
						
						player.sendMessage(Text.of("[Lockette] The lock was removed"));
					});
				}
			});
		}
	}
	public static void transferLock(Player player, Lock lock, UUID other) {
		if (lock.isOwner(player)) {
			lock.getTarget().getTileEntity().ifPresent(ent -> {
				if (ent instanceof Sign) {
					ent.get(LockKeys.LOCK).ifPresent(data->{
						Lockette.getUser(other).ifPresent(newOwner->{
							data.deny(newOwner.getProfile());
							data.setOwner(newOwner.getProfile());
							data.permit(player.getProfile());
							data.update();
							ent.offer(LockKeys.LOCK, data);
							
							SignData sd = ((Sign)ent).getSignData();
							sd.setElement(1, Text.of(data.getOwnerName().orElse("?")));
							ent.offer(sd);
							
							player.sendMessage(Text.of("[Lockette] Ownership was transfered to ", playerDecor(data.getOwnerUUID().get())));
							displayMenuMembersView(player, lock.getTarget().getBlockPosition());
						});
					});
				}
			});
		}
	}
	public static void allowUserLock(Player player, Lock lock, UUID other) {
		if (lock.isOwner(player)) {
			lock.getTarget().getTileEntity().ifPresent(ent -> {
				if (ent instanceof Sign) {
					ent.get(LockKeys.LOCK).ifPresent(data->{
						Lockette.getUser(other).ifPresent(newOwner->{
							if (data.getPermitted().containsKey(other)) {
								player.sendMessage(Text.of("[Lockette] The player ", playerDecor(other), " already has access to this"));
							} else {
								data.permit(player.getProfile());
								data.update();
								ent.offer(LockKeys.LOCK, data);
								
								player.sendMessage(Text.of("[Lockette] Access to this was granted for ", playerDecor(other)));
								displayMenuOwnerPermit(player, lock.getTarget().getBlockPosition());
							}
						});
					});
					
				}
			});
		}
	}
	public static void denyUserLock(Player player, Lock lock, UUID other) {
		if (lock.isOwner(player)) {
			lock.getTarget().getTileEntity().ifPresent(ent -> {
				if (ent instanceof Sign) {
					ent.get(LockKeys.LOCK).ifPresent(data->{
						Lockette.getUser(other).ifPresent(newOwner->{
							if (!data.getPermitted().containsKey(other)) {
								player.sendMessage(Text.of("[Lockette] The player ", playerDecor(other), " has no access to this"));
							} else {
								data.deny(player.getProfile());
								data.update();
								ent.offer(LockKeys.LOCK, data);
								
								player.sendMessage(Text.of("[Lockette] Access to this has been revoked for ", playerDecor(other)));
								displayMenuOwnerView(player, lock.getTarget().getBlockPosition());
							}
						});						
					});
				}
			});
		}
	}
}
