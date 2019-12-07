package me.nowaha.labyrinth;

import org.bukkit.GameMode;
import org.bukkit.Location;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.bukkit.potion.PotionEffect;

import java.util.*;

public class LabyrinthCommand implements CommandExecutor {

    /*
        This class handles the /labyrinth or /lr command.
    */


    Labyrinth pl;

    public LabyrinthCommand(Labyrinth _main) {
        pl = _main;
    }

    List<UUID> selectingSurvivorGate = new ArrayList<>();
    List<UUID> selectingKillerGate = new ArrayList<>();
    List<UUID> selectingEscapePoints = new ArrayList<>();
    List<UUID> addingSwitches = new ArrayList<>();
    Map<UUID, Integer> addingLootChests = new HashMap<>();

    @Override
    public boolean onCommand(CommandSender sender, Command command, String label, String[] args) {
        if (!(sender instanceof Player)) return true;

        Player player = (Player) sender;
        if (args.length == 0) {
            player.sendMessage("§eLabyrinth");

            player.sendMessage(" §a/" + label + " join §f- Join the game.");
            player.sendMessage(" §a/" + label + " leave §f- Leave the game.");
            if (!player.hasPermission("labyrinth.admin")) return true;
            player.sendMessage(" §a/" + label + " start §f- Start the game.");
            player.sendMessage(" §a/" + label + " reload §f- Reload the configuration file.");
            player.sendMessage("");
            player.sendMessage(" §a/" + label + " setkillerspawn §f- Set the killer spawn loc to your loc.");
            player.sendMessage(" §a/" + label + " setplayerspawn §f- Set the player spawn loc to your loc.");
            player.sendMessage("");
            player.sendMessage(" §a/" + label + " selectsurvivorgate §f- Hit blocks to add/remove.");
            player.sendMessage(" §a/" + label + " selectkillergate §f- Hit blocks to add/remove.");
            player.sendMessage(" §a/" + label + " selectescapepoints §f- Hit blocks to add/remove.");
            player.sendMessage("");
            player.sendMessage(" §a/" + label + " addlootchests <1-3> §f- Place chests to add to tier <1-3>.");
            player.sendMessage(" §a/" + label + " addswitches  §f- Place levers as to work as switches.");
        } else if (args[0].equalsIgnoreCase("reload")) {
            if (!player.hasPermission("labyrinth.admin")) return true;

            player.sendMessage("§eReloading the configuration file...");
            pl.reloadConfig();
            player.sendMessage("§aDone!");
        } else if (args[0].equalsIgnoreCase("start")) {
            if (!player.hasPermission("labyrinth.host") && !player.hasPermission("labyrinth.admin")) return true;
            if (!pl.canJoin) {
                pl.canJoin = true;
                player.sendMessage("§aPeople can now join the event. " + pl.minPlayers + " needed to start.");
            } else {
                player.sendMessage("§cThere game is already open.");
            }
        } else if (args[0].equalsIgnoreCase("setkillerspawn")) {
            if (!player.hasPermission("labyrinth.admin")) return true;

            pl.getConfig().set("spawns.killer", player.getLocation());
            pl.saveConfig();
            player.sendMessage("§aSet the killer spawn location to your location.");
        } else if (args[0].equalsIgnoreCase("setplayerspawn")) {
            if (!player.hasPermission("labyrinth.admin")) return true;

            pl.getConfig().set("spawns.player", player.getLocation());
            pl.saveConfig();
            player.sendMessage("§aSet the player spawn location to your location.");
        } else if (args[0].equalsIgnoreCase("selectsurvivorgate")) {
            if (!player.hasPermission("labyrinth.admin")) return true;

            if (selectingSurvivorGate.contains(player.getUniqueId())) {
                selectingSurvivorGate.remove(player.getUniqueId());
                player.sendMessage("§aStopped editing survivor gate blocks.");
            } else {
                selectingSurvivorGate.add(player.getUniqueId());
                selectingEscapePoints.add(player.getUniqueId());
                selectingKillerGate.remove(player.getUniqueId());
                player.sendMessage("§aNow editing survivor gate blocks.");
            }
        } else if (args[0].equalsIgnoreCase("selectkillergate")) {
            if (!player.hasPermission("labyrinth.admin")) return true;

            if (selectingKillerGate.contains(player.getUniqueId())) {
                selectingKillerGate.remove(player.getUniqueId());
                player.sendMessage("§aStopped editing killer gate blocks.");
            } else {
                selectingKillerGate.add(player.getUniqueId());
                selectingEscapePoints.add(player.getUniqueId());
                selectingSurvivorGate.remove(player.getUniqueId());
                player.sendMessage("§aNow editing killer gate blocks.");
            }
        } else if (args[0].equalsIgnoreCase("selectescapepoints")) {
            if (!player.hasPermission("labyrinth.admin")) return true;

            if (selectingEscapePoints.contains(player.getUniqueId())) {
                selectingEscapePoints.remove(player.getUniqueId());
                player.sendMessage("§aStopped editing escape point blocks.");
            } else {
                selectingEscapePoints.add(player.getUniqueId());
                selectingSurvivorGate.remove(player.getUniqueId());
                selectingKillerGate.remove(player.getUniqueId());
                player.sendMessage("§aNow editing escape point blocks.");
            }
        } else if (args[0].equalsIgnoreCase("addlootchests")) {
            if (!player.hasPermission("labyrinth.admin")) return true;

            if (addingLootChests.containsKey(player.getUniqueId())) {
                addingLootChests.remove(player.getUniqueId());
                player.sendMessage("§aStopped adding loot chests.");
            } else {
                int tier = Integer.parseInt(args[1]);

                if (tier > 3 || tier < 1) {
                    player.sendMessage("§cInvalid tier.");
                    return true;
                }

                addingLootChests.put(player.getUniqueId(), tier);
                player.sendMessage("§aPlace chests to add them to the tier " + tier + " loot chest pool.");
            }
        } else if (args[0].equalsIgnoreCase("addswitches")) {
            if (!player.hasPermission("labyrinth.admin")) return true;

            if (addingSwitches.contains(player.getUniqueId())) {
                addingSwitches.remove(player.getUniqueId());
                player.sendMessage("§aStopped adding switches.");
            } else {
                addingSwitches.add(player.getUniqueId());
                player.sendMessage("§aPlace levers to add them to the switches pool.");
            }
        } else if (args[0].equalsIgnoreCase("join")) {
            if (!pl.canJoin) {
                player.sendMessage("§cLabyrinth §7» There is no Labyrinth game hosted.");
                return true;
            }

            if (pl.currentGameState == Labyrinth.GameState.lobby) {
                if (!pl.playersInLobby.contains(player.getUniqueId())) {
                    if (pl.playersInLobby.size() >= 50) {
                        player.sendMessage("§cLabyrinth §7» The lobby is full (50/50).");
                        return true;
                    }

                    // Reset player's properties
                    if (true) {
                        pl.oldItems.put(player.getUniqueId(), player.getInventory().getContents());
                        pl.oldArmor.put(player.getUniqueId(), player.getInventory().getArmorContents());

                        player.getInventory().clear();
                        player.getInventory().setArmorContents(null);
                        for (PotionEffect effect :
                                player.getActivePotionEffects()) {
                            player.removePotionEffect(effect.getType());
                        }
                        player.setHealthScale(20);
                        player.setHealth(20);
                        player.setFoodLevel(20);
                        player.setGameMode(GameMode.SURVIVAL);
                        player.setWalkSpeed(0.2f);
                        player.setExp(0);
                        player.setTotalExperience(0);
                        player.setLevel(0);
                        player.setSaturation(3f);
                    }

                    pl.playersInLobby.add(player.getUniqueId());
                    player.setScoreboard(pl.scoreboardHandler.lobbyScoreboard);
                    player.teleport((Location) pl.getConfig().get("spawns.player"));

                    pl.chatHandler.sendMessageToLobby("§cLabyrinth §7» §r" + player.getName() + " joined the lobby! (§a" + pl.playersInLobby.size() + "/50§r)");
                } else {
                    player.sendMessage("§cLabyrinth §7» You're already in the lobby.");
                }
            } else {
                player.sendMessage("§cLabyrinth §7» This game has already begun.");
            }
        } else if (args[0].equalsIgnoreCase("leave")) {
            pl.playerLeft(player);
        }
        return true;
    }
}
