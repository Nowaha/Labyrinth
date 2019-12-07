package me.nowaha.labyrinth;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.EntityType;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.EventPriority;
import org.bukkit.event.Listener;
import org.bukkit.event.block.Action;
import org.bukkit.event.block.BlockBreakEvent;
import org.bukkit.event.block.BlockDamageEvent;
import org.bukkit.event.block.BlockPlaceEvent;
import org.bukkit.event.entity.EntityDamageByEntityEvent;
import org.bukkit.event.entity.EntityDamageEvent;
import org.bukkit.event.entity.FoodLevelChangeEvent;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.player.*;
import org.bukkit.material.Lever;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.UUID;

public class Events implements Listener {
    Labyrinth pl;

    public Events(Labyrinth main) {
        pl = main;
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamageByEntity(EntityDamageByEntityEvent e) {
        if (pl.currentGameState == Labyrinth.GameState.lobby) {
            // Prevent spectators from hurting & being hurt.
            if (pl.playersInLobby.contains(e.getEntity().getUniqueId()) || pl.playersInLobby.contains(e.getDamager().getUniqueId())) {
                e.setCancelled(true);
            }
        } else if (pl.currentGameState == Labyrinth.GameState.ingame) {
            if (!(e.getEntity() instanceof Player) || !(e.getDamager() instanceof Player)) return;

            // Prevent spectators from hurting & being hurt.
            if (pl.spectators.contains(e.getEntity().getUniqueId()) || pl.spectators.contains(e.getDamager().getUniqueId())) {
                e.setCancelled(true);
            }

            // Prevent teamkilling
            if (pl.survivors.contains(e.getEntity().getUniqueId()) && pl.survivors.contains(e.getDamager().getUniqueId())) {
                e.setCancelled(true);
            } else if (pl.killers.contains(e.getEntity().getUniqueId()) && pl.killers.contains(e.getDamager().getUniqueId())) {
                e.setCancelled(true);
            } else {
                Player player = (Player) e.getEntity();
                if (e.getFinalDamage() >= player.getHealth()) {
                    e.setCancelled(true);
                    e.setDamage(0);

                    player.setHealth(20);
                    player.teleport((Location) pl.getConfig().get("spawns.killer"));
                    for (PotionEffect eff :
                            player.getActivePotionEffects()) {
                        player.removePotionEffect(eff.getType());
                    }

                    player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 99999999, 1, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999999, 1, true, false));
                    player.addPotionEffect(new PotionEffect(PotionEffectType.SPEED, 999999999, 2, true, false));

                    player.sendMessage("       §a------------------------\n                 §fYou are a...\n                §8§lSPECTATOR\n§f \n    §fYou can §8watch §fthe game or §8leave§f. \n       §a------------------------");

                    player.getInventory().clear();
                    player.getInventory().setArmorContents(null);

                    player.setExp(0);
                    player.setTotalExperience(0);
                    player.setLevel(0);

                    Player damager = (Player) e.getDamager();

                    String message = "";
                    if (pl.killers.contains(damager.getUniqueId())) {
                        message += "§c[Killer] " + damager.getName() + " §ekilled ";
                    } else {
                        message += "§9[Survivor] " + damager.getName() + " §ekilled ";
                    }

                    if (pl.killers.contains(player.getUniqueId())) {
                        pl.killers.remove(player.getUniqueId());
                        message += "§c[Killer] " + player.getName()  + "§e!";
                    } else {
                        pl.survivors.remove(player.getUniqueId());
                        message += "§9[Survivor] " + player.getName() + "§e!";
                    }

                    pl.spectators.add(player.getUniqueId());
                    pl.survivors.remove(player);
                    pl.killers.remove(player);

                    List<UUID> playersInGame = new ArrayList<>(pl.killers);
                    playersInGame.addAll(pl.survivors);
                    playersInGame.addAll(pl.spectators);

                    for (UUID id : playersInGame) {
                        Player target = Bukkit.getPlayer(id);
                        target.sendMessage(message);
                        target.playSound(target.getLocation(), Sound.WITHER_SPAWN, 1, 1f);
                    }

                    pl.killerAmountChanged();
                    pl.survivorAmountChanged();
                }
            }
        } else if (pl.currentGameState == Labyrinth.GameState.end) {
            if (pl.spectators.contains(e.getDamager().getUniqueId()) || pl.spectators.contains(e.getEntity().getUniqueId())) {
                e.setCancelled(true);
                e.setDamage(0);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onEntityDamage(EntityDamageEvent e) {
        if (pl.playersInLobby.contains(e.getEntity().getUniqueId()) || pl.killers.contains(e.getEntity().getUniqueId()) || pl.spectators.contains(e.getEntity().getUniqueId())) {
            if (e.getCause().equals(EntityDamageEvent.DamageCause.FALL)) {
                e.setCancelled(true);
                e.setDamage(0);
                return;
            }

            if (pl.spectators.contains(e.getEntity().getUniqueId())) {
                e.setCancelled(true);
                e.setDamage(0);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onInventoryClick(InventoryClickEvent e) {
        if (pl.currentGameState == Labyrinth.GameState.ingame) {
            if (pl.killers.contains(e.getWhoClicked().getUniqueId()) || pl.spectators.contains(e.getWhoClicked().getUniqueId())) {
                e.setCancelled(true);
            }
        } else if (pl.currentGameState == Labyrinth.GameState.end) {
            if (pl.spectators.contains(e.getWhoClicked().getUniqueId())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onFoodLevelChange(FoodLevelChangeEvent e) {
        // Preventing killers, spectators or people in lobby from getting hunger.
        if (pl.spectators.contains(e.getEntity().getUniqueId()) || pl.killers.contains(e.getEntity().getUniqueId()) || pl.playersInLobby.contains(e.getEntity().getUniqueId())) {
            e.setCancelled(true);
            Player player = (Player) e.getEntity();
            player.setFoodLevel(20);
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockPlace(BlockPlaceEvent e) {
        if (e.getBlockPlaced().getType().equals(Material.CHEST)) {
            if (pl.labyrinthCommand.addingLootChests.getOrDefault(e.getPlayer().getUniqueId(), -1) != -1) {
                Integer tier = pl.labyrinthCommand.addingLootChests.getOrDefault(e.getPlayer().getUniqueId(), -1);

                switch (tier) {
                    case 1:
                        List<Location> locations1 = new ArrayList<>((Collection<? extends Location>) pl.getConfig().getList("chestLocations.tier1", new ArrayList<>()));
                        if (!locations1.contains(e.getBlockPlaced().getLocation()))
                            locations1.add(e.getBlockPlaced().getLocation());

                        pl.getConfig().set("chestLocations.tier1", locations1);
                        e.getPlayer().sendMessage("§cLabyrinth §7» §aAdded a tier 1 loot chest.");
                        break;
                    case 2:
                        List<Location> locations2 = new ArrayList<>((Collection<? extends Location>) pl.getConfig().getList("chestLocations.tier2", new ArrayList<>()));
                        if (!locations2.contains(e.getBlockPlaced().getLocation()))
                            locations2.add(e.getBlockPlaced().getLocation());

                        pl.getConfig().set("chestLocations.tier2", locations2);
                        e.getPlayer().sendMessage("§cLabyrinth §7» §aAdded a tier 2 loot chest.");
                        break;
                    case 3:
                        List<Location> locations3 = new ArrayList<>((Collection<? extends Location>) pl.getConfig().getList("chestLocations.tier3", new ArrayList<>()));
                        if (!locations3.contains(e.getBlockPlaced().getLocation()))
                            locations3.add(e.getBlockPlaced().getLocation());

                        pl.getConfig().set("chestLocations.tier3", locations3);
                        e.getPlayer().sendMessage("§cLabyrinth §7» §aAdded a tier 3 loot chest.");
                        break;
                }

                pl.saveConfig();
            }
        } else if (e.getBlockPlaced().getType().equals(Material.LEVER)) {
            if (pl.labyrinthCommand.addingSwitches.contains(e.getPlayer().getUniqueId())) {
                List<Location> switchLocations = new ArrayList<>((Collection<? extends Location>) pl.getConfig().getList("switchLocations", new ArrayList<>()));
                if (!switchLocations.contains(e.getBlockPlaced().getLocation()))
                    switchLocations.add(e.getBlockPlaced().getLocation());

                pl.getConfig().set("switchLocations", switchLocations);
                e.getPlayer().sendMessage("§cLabyrinth §7» §aAdded a lever switch.");

                pl.saveConfig();
            }
        }
    }

    @EventHandler(ignoreCancelled = true, priority = EventPriority.MONITOR)
    public void onBlockBreak(BlockBreakEvent e) {
        // Remove chests that are loot chests if they are destroyed.
        if (e.getBlock().getType().equals(Material.CHEST)) {
            List<Location> locations1 = new ArrayList<>((Collection<? extends Location>) pl.getConfig().getList("chestLocations.tier1", new ArrayList<>()));
            List<Location> locations2 = new ArrayList<>((Collection<? extends Location>) pl.getConfig().getList("chestLocations.tier2", new ArrayList<>()));
            List<Location> locations3 = new ArrayList<>((Collection<? extends Location>) pl.getConfig().getList("chestLocations.tier3", new ArrayList<>()));

            if (locations1.contains(e.getBlock().getLocation())) {
                locations1.remove(e.getBlock().getLocation());
                pl.getConfig().set("chestLocations.tier1", locations1);
                e.getPlayer().sendMessage("§cLabyrinth §7» §aRemoved a tier 1 loot chest.");
            } else if (locations2.contains(e.getBlock().getLocation())) {
                locations2.remove(e.getBlock().getLocation());
                pl.getConfig().set("chestLocations.tier2", locations2);
                e.getPlayer().sendMessage("§cLabyrinth §7» §aRemoved a tier 2 loot chest.");
            } else if (locations3.contains(e.getBlock().getLocation())) {
                locations3.remove(e.getBlock().getLocation());
                pl.getConfig().set("chestLocations.tier3", locations3);
                e.getPlayer().sendMessage("§cLabyrinth §7» §aRemoved a tier 3 loot chest.");
            }

            pl.saveConfig();
        } else if (e.getBlock().getType().equals(Material.LEVER)) {
            List<Location> switchLocations = new ArrayList<>((Collection<? extends Location>) pl.getConfig().getList("switchLocations", new ArrayList<>()));

            if (switchLocations.contains(e.getBlock().getLocation())) {
                switchLocations.remove(e.getBlock().getLocation());
                pl.getConfig().set("switchLocations", switchLocations);
                e.getPlayer().sendMessage("§cLabyrinth §7» §aRemoved a lever switch.");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerMove(PlayerMoveEvent e) {
        if (pl.currentGameState.equals(Labyrinth.GameState.ingame)) {
            if (pl.survivors.contains(e.getPlayer().getUniqueId())) {
                if (e.getPlayer().getLocation().subtract(0, 1, 0).getBlock().getType().equals(Material.GOLD_BLOCK)) {
                    pl.getLogger().info("gold block");
                    pl.chatHandler.sendMessageToAll("§cLabyrinth §7» §6§l" + e.getPlayer().getName() + " escaped the labyrinth!");
                    pl.endGame();
                }
            }
        }
    }

    List<Location> leversHit = new ArrayList<>();

    @EventHandler(ignoreCancelled = true)
    public void onPlayerInteract(PlayerInteractEvent e) {
        if (pl.currentGameState == Labyrinth.GameState.ingame) {
            if (pl.survivors.contains(e.getPlayer().getUniqueId())) {
                if (e.getAction() == Action.RIGHT_CLICK_BLOCK) {
                    if (e.getClickedBlock() != null) {
                        if (e.getClickedBlock().getType().equals(Material.LEVER)) {

                            // A survivor hit a switch in-game.

                            Lever lever = (Lever) e.getClickedBlock().getState().getData();
                            if (!lever.isPowered()) {
                                pl.switches++;
                                leversHit.add(e.getClickedBlock().getLocation());

                                if (pl.switches >= 3) {
                                    pl.chatHandler.sendMessageToAll("§6§lThe final switch has been toggled!");

                                    List<UUID> playersInGame = new ArrayList<>(pl.killers);
                                    playersInGame.addAll(pl.survivors);
                                    playersInGame.addAll(pl.spectators);

                                    for (UUID id : playersInGame) {
                                        Player target = Bukkit.getPlayer(id);
                                        target.sendTitle("§6§lALL SWITCHES ACTIVE", "§eEscape points activating in 10 seconds.");
                                        target.playSound(target.getLocation(), Sound.WITHER_SPAWN, 1, 1f);
                                    }

                                    Bukkit.getScheduler().scheduleSyncDelayedTask(pl, () -> {
                                        if (pl.currentGameState == Labyrinth.GameState.ingame) {

                                            List<UUID> playersInGame2 = new ArrayList<>(pl.killers);
                                            playersInGame2.addAll(pl.survivors);
                                            playersInGame2.addAll(pl.spectators);

                                            for (Location loc : new ArrayList<>((Collection<? extends Location>) pl.getConfig().getList("escapepoints"))) {
                                                pl.oldBlocks.put(loc, loc.getBlock().getType());
                                                pl.oldBlockData.put(loc, loc.getBlock().getData());

                                                loc.getBlock().setType(Material.GOLD_BLOCK);
                                            }
                                            
                                            for (UUID id : playersInGame2) {
                                                Player target = Bukkit.getPlayer(id);
                                                target.sendTitle("§6§lESCAPE POINTS ACTIVE", "§eYou can now escape. Find a golden block!");
                                                target.playSound(target.getLocation(), Sound.WITHER_SPAWN, 1, 0.1f);
                                            }
                                        }
                                    }, 10 * 20);
                                } else {
                                    List<UUID> playersInGame = new ArrayList<>(pl.killers);
                                    playersInGame.addAll(pl.survivors);
                                    playersInGame.addAll(pl.spectators);

                                    for (UUID id : playersInGame) {
                                        Player target = Bukkit.getPlayer(id);
                                        target.playSound(target.getLocation(), Sound.DOOR_OPEN, 1, 0f);
                                    }

                                    if (pl.switches < 2) {
                                        pl.chatHandler.sendMessageToAll("§6§lA switch has been toggled! §eThere are " + (3 - pl.switches) + " more left to go!");
                                    } else {
                                        pl.chatHandler.sendMessageToAll("§6§lA switch has been toggled! §eThere is one more left to go!");
                                    }
                                }
                            } else {
                                e.setCancelled(true);
                            }
                        }
                    }
                }
            } else {
                e.setCancelled(true);
            }
        } else if (pl.currentGameState == Labyrinth.GameState.end) {
            if (pl.spectators.contains(e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onBlockDamage(BlockDamageEvent e) {
        if (pl.labyrinthCommand.selectingKillerGate.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            List<Location> killergateblocks = new ArrayList<>();
            for (Object loc :
                    pl.getConfig().getList("killergateblocks", new ArrayList<>())) {
                killergateblocks.add((Location) loc);
            }

            if (killergateblocks.contains(e.getBlock().getLocation())) {
                killergateblocks.remove(e.getBlock().getLocation());
                e.getPlayer().sendMessage("§eRemoved block from killer gate.");
            } else {
                killergateblocks.add(e.getBlock().getLocation());
                e.getPlayer().sendMessage("§eAdded block to killer gate.");
            }

            pl.getConfig().set("killergateblocks", killergateblocks);
            pl.saveConfig();
        } else if (pl.labyrinthCommand.selectingSurvivorGate.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            List<Location> survivorgateblocks = new ArrayList<>();
            for (Object loc :
                    pl.getConfig().getList("survivorgateblocks", new ArrayList<>())) {
                survivorgateblocks.add((Location) loc);
            }

            if (survivorgateblocks.contains(e.getBlock().getLocation())) {
                survivorgateblocks.remove(e.getBlock().getLocation());
                e.getPlayer().sendMessage("§eRemoved block from survivor gate.");
            } else {
                survivorgateblocks.add(e.getBlock().getLocation());
                e.getPlayer().sendMessage("§eAdded block to survivor gate.");
            }

            pl.getConfig().set("survivorgateblocks", survivorgateblocks);
            pl.saveConfig();
        } else if (pl.labyrinthCommand.selectingEscapePoints.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
            List<Location> escapePoints = new ArrayList<>();
            for (Object loc :
                    pl.getConfig().getList("escapepoints", new ArrayList<>())) {
                escapePoints.add((Location) loc);
            }

            if (escapePoints.contains(e.getBlock().getLocation())) {
                escapePoints.remove(e.getBlock().getLocation());
                e.getPlayer().sendMessage("§eRemoved escape point block.");
            } else {
                escapePoints.add(e.getBlock().getLocation());
                e.getPlayer().sendMessage("§eAdded block to escape points.");
            }

            pl.getConfig().set("escapepoints", escapePoints);
            pl.saveConfig();
        } else {
            return;
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerPickupItem(PlayerPickupItemEvent e) {
        // Prevent pickups for killers & spectators.
        if (pl.killers.contains(e.getPlayer().getUniqueId()) || pl.spectators.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerDropItem(PlayerDropItemEvent e) {
        // Prevent dropping for killers & spectators.
        if (pl.killers.contains(e.getPlayer().getUniqueId()) || pl.spectators.contains(e.getPlayer().getUniqueId())) {
            e.setCancelled(true);
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerQuit(PlayerQuitEvent e) {
        if (pl.playersInLobby.contains(e.getPlayer().getUniqueId()) || pl.survivors.contains(e.getPlayer().getUniqueId()) || pl.killers.contains(e.getPlayer().getUniqueId()) || pl.spectators.contains(e.getPlayer().getUniqueId())) {
            pl.playerLeft(e.getPlayer());
            e.setQuitMessage("");
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onPlayerCommandPreprocess(PlayerCommandPreprocessEvent e) {
        // Exclude people with op or the labyrinth.commands permission from this check.
        if (e.getPlayer().isOp() || e.getPlayer().hasPermission("labyrinth.commands")) return;

        // Prevent people in-game from using any other command than the /lr leave command.
        if (pl.playersInLobby.contains(e.getPlayer().getUniqueId()) || pl.survivors.contains(e.getPlayer().getUniqueId()) || pl.killers.contains(e.getPlayer().getUniqueId()) || pl.spectators.contains(e.getPlayer().getUniqueId())) {
            if (Labyrinth.currentGameState == Labyrinth.GameState.end) {
                if (!pl.spectators.contains(e.getPlayer().getUniqueId())) {
                    return;
                }
            }
            if (!e.getMessage().equalsIgnoreCase("/lr leave") && !e.getMessage().equalsIgnoreCase("/labyrinth leave")) {
                e.setCancelled(true);
                e.getPlayer().sendMessage("You cannot use that command while in the labyrinth game!");
            }
        }
    }

    @EventHandler(ignoreCancelled = true)
    public void onAsyncPlayerChat(AsyncPlayerChatEvent e) {
        if (pl.currentGameState == Labyrinth.GameState.ingame) {
            if (pl.killers.contains(e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
                pl.chatHandler.sendMessageToSpectators("§c[Killer] " + e.getPlayer().getName() + ": " + e.getMessage());
                pl.chatHandler.sendMessageToKillers("§c[Killer] " + e.getPlayer().getName() + ": " + e.getMessage());
            } else if (pl.survivors.contains(e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
                pl.chatHandler.sendMessageToAll("§9[Survivor] " + e.getPlayer().getName() + ": " + e.getMessage());
            } else if (pl.spectators.contains(e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
                pl.chatHandler.sendMessageToSpectators("§8[Spectator] " + e.getPlayer().getName() + ": §7" + e.getMessage());
            }
        } else if (pl.currentGameState == Labyrinth.GameState.end) {
            if (pl.spectators.contains(e.getPlayer().getUniqueId())) {
                e.setCancelled(true);
                pl.chatHandler.sendMessageToSpectators("§8[Spectator] " + e.getPlayer().getName() + ": §7" + e.getMessage());
            }
        }
    }
}
