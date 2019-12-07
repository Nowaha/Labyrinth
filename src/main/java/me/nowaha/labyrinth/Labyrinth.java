package me.nowaha.labyrinth;

import com.earth2me.essentials.Essentials;
import com.earth2me.essentials.spawn.EssentialsSpawn;
import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.block.BlockFace;
import org.bukkit.block.BlockState;
import org.bukkit.entity.Player;
import org.bukkit.inventory.ItemFlag;
import org.bukkit.inventory.ItemStack;
import org.bukkit.material.Chest;
import org.bukkit.material.Lever;
import org.bukkit.plugin.java.JavaPlugin;
import org.bukkit.potion.Potion;
import org.bukkit.potion.PotionEffect;
import org.bukkit.potion.PotionEffectType;
import org.bukkit.potion.PotionType;

import java.util.*;

public final class Labyrinth extends JavaPlugin {

    LabyrinthCommand labyrinthCommand;
    ScoreboardHandler scoreboardHandler;
    Events events;
    ChatHandler chatHandler;

    Essentials essentials;
    EssentialsSpawn essentialsSpawn;

    Integer minPlayers = 2;

    List<UUID> playersInLobby = new ArrayList<>();

    List<UUID> survivors = new ArrayList<>();
    int initialSurvivors = 40;

    List<UUID> killers = new ArrayList<>();
    int initialKillers = 10;

    List<UUID> spectators = new ArrayList<>();

    int switches = 0;

    static GameState currentGameState = GameState.lobby;

    List<Location> activeLoot1Chests = new ArrayList<>();
    List<Location> activeLoot2Chests = new ArrayList<>();
    List<Location> activeLoot3Chests = new ArrayList<>();

    Map<Location, BlockFace> oldChestData = new HashMap<>();

    Map<UUID, ItemStack[]> oldItems = new HashMap<>();
    Map<UUID, ItemStack[]> oldArmor = new HashMap<>();

    public Boolean canJoin = false;

    public enum GameState {
        lobby,
        ingame,
        end
    }

    @Override
    public void onEnable() {
        // Initializing all the class values
        scoreboardHandler = new ScoreboardHandler(this);
        labyrinthCommand = new LabyrinthCommand(this);
        events = new Events(this);
        chatHandler = new ChatHandler(this);

        // Loading Essentials dependencies for spawn functionality.
        essentials = (Essentials) Bukkit.getPluginManager().getPlugin("Essentials");
        essentialsSpawn = (EssentialsSpawn) Bukkit.getPluginManager().getPlugin("EssentialsSpawn");

        // Register alternative classes as needed
        getServer().getPluginManager().registerEvents(events, this);
        getCommand("labyrinth").setExecutor(labyrinthCommand);

        saveResource("config.yml", false);
    }

    @Override
    public void onDisable() {
        List<UUID> players = new ArrayList<>(playersInLobby);
        players.addAll(killers);
        players.addAll(survivors);
        players.addAll(spectators);

        for (UUID id : players) {
            Player p = Bukkit.getPlayer(id);
            p.sendMessage("§cLabyrinth §7» The game ended prematurely.");
            p.teleport(essentialsSpawn.getSpawn("default"));
            p.setScoreboard(scoreboardHandler.sm.getMainScoreboard());

            p.getInventory().setContents(oldItems.get(p.getUniqueId()));
            p.getInventory().setArmorContents(oldArmor.get(p.getUniqueId()));

            for (PotionEffect effect :
                    p.getActivePotionEffects()) {
                p.removePotionEffect(effect.getType());
            }

            p.setHealthScale(20);
            p.setHealth(20);
            p.setFoodLevel(20);

            p.setWalkSpeed(0.2f);
        }

        for (Location location :
                oldBlocks.keySet()) {
            location.getBlock().setType(oldBlocks.get(location));
            location.getBlock().setData(oldBlockData.get(location));
        }

        for (Location location : events.leversHit) {
            BlockState state = location.getBlock().getState();
            Lever lever = (Lever) state.getData();
            lever.setPowered(false);
            state.setData(lever);
            state.update(true);
        }

        for (Location location : oldChestData.keySet()) {
            location.getBlock().setType(Material.CHEST);
            org.bukkit.block.Chest state = (org.bukkit.block.Chest) location.getBlock().getState();
            Chest chest = (Chest) state.getData();
            chest.setFacingDirection(oldChestData.get(location));
            state.setData(chest);
            state.getInventory().clear();
            state.update(true);
        }
    }

    public void playerLeft(Player player) {
        if (currentGameState == GameState.lobby) {
            if (!playersInLobby.contains(player.getUniqueId())) {
                player.sendMessage("§cLabyrinth §7» You're not in the lobby.");
                return;
            }

            player.setScoreboard(scoreboardHandler.sm.getMainScoreboard());

            chatHandler.sendMessageToLobby("§cLabyrinth §7» §r" + player.getName() + " left the lobby. (§a" + (playersInLobby.size() - 1) + "/50§r)");

            player.getInventory().setContents(oldItems.get(player.getUniqueId()));
            player.getInventory().setArmorContents(oldArmor.get(player.getUniqueId()));
            oldItems.remove(player.getUniqueId());
            oldArmor.remove(player.getUniqueId());

            player.setLevel(0);
            player.setExp(0);
            player.setTotalExperience(0);

            player.setWalkSpeed(0.2f);

            for (PotionEffect effect :
                    player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.setHealthScale(20);
            player.setHealth(20);
            player.setFoodLevel(20);

            playersInLobby.remove(player.getUniqueId());
            player.teleport(essentialsSpawn.getSpawn(essentials.getUser(player.getUniqueId()).getGroup()));
        } else if (currentGameState == GameState.ingame) {
            if (!survivors.contains(player.getUniqueId()) && !killers.contains(player.getUniqueId()) && !spectators.contains(player.getUniqueId())) {
                player.sendMessage("§cLabyrinth §7» You're not in the game.");
                return;
            }

            player.setExp(0);
            player.setTotalExperience(0);
            player.setLevel(0);

            player.setScoreboard(scoreboardHandler.sm.getMainScoreboard());

            if (survivors.contains(player.getUniqueId())) {
                chatHandler.sendMessageToAll("§9[Survivor] " + player.getName() + " §rleft the game.");

                survivors.remove(player.getUniqueId());
                survivorAmountChanged();
            } else if (killers.contains(player.getUniqueId())) {
                chatHandler.sendMessageToAll("§c[Killer] " + player.getName() + " §rleft the game.");

                killers.remove(player.getUniqueId());
                killerAmountChanged();
            } else if (killers.contains(player.getUniqueId())) {
                chatHandler.sendMessageToSpectators("§8[Spectator] " + player.getName() + " §rleft the game.");

                spectators.remove(player.getUniqueId());
            }

            for (PotionEffect effect :
                    player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.setHealthScale(20);
            player.setHealth(20);
            player.setFoodLevel(20);

            player.setWalkSpeed(0.2f);

            player.getInventory().setContents(oldItems.get(player.getUniqueId()));
            player.getInventory().setArmorContents(oldArmor.get(player.getUniqueId()));
            oldItems.remove(player.getUniqueId());
            oldArmor.remove(player.getUniqueId());

            playersInLobby.remove(player.getUniqueId());
            player.teleport(essentialsSpawn.getSpawn(essentials.getUser(player.getUniqueId()).getGroup()));
        } else if (currentGameState == GameState.end) {
            if (!spectators.contains(player.getUniqueId())) {
                player.sendMessage("§cLabyrinth §7» You're not in the game.");
                return;
            }

            for (PotionEffect effect :
                    player.getActivePotionEffects()) {
                player.removePotionEffect(effect.getType());
            }
            player.setHealthScale(20);
            player.setHealth(20);
            player.setFoodLevel(20);

            player.setExp(0);
            player.setTotalExperience(0);
            player.setLevel(0);

            player.setWalkSpeed(0.2f);

            player.setScoreboard(scoreboardHandler.sm.getMainScoreboard());

            player.getInventory().setContents(oldItems.get(player.getUniqueId()));
            player.getInventory().setArmorContents(oldArmor.get(player.getUniqueId()));
            oldItems.remove(player.getUniqueId());
            oldArmor.remove(player.getUniqueId());

            spectators.remove(player.getUniqueId());
            player.teleport(essentialsSpawn.getSpawn(essentials.getUser(player.getUniqueId()).getGroup()));
        }
    }

    Random random = new Random();

    Integer refillTask1 = -1;
    Integer refillTask2 = -1;

    void beginGame() {
        currentGameState = Labyrinth.GameState.ingame;

        // Clear the current lists
        activeLoot1Chests.clear();
        activeLoot2Chests.clear();
        activeLoot3Chests.clear();
        oldChestData.clear();
        oldBlocks.clear();
        oldBlockData.clear();
        survivors.clear();
        killers.clear();
        spectators.clear();

        // --- START LOOT CHESTS ---

        List<Location> locations1 = new ArrayList<>((Collection<? extends Location>) getConfig().getList("chestLocations.tier1", new ArrayList<>()));
        List<Location> locations2 = new ArrayList<>((Collection<? extends Location>) getConfig().getList("chestLocations.tier2", new ArrayList<>()));
        List<Location> locations3 = new ArrayList<>((Collection<? extends Location>) getConfig().getList("chestLocations.tier3", new ArrayList<>()));

        for (int i = 0; i < Math.floor((float) locations1.size() * 0.8f); i++) {
            Location loc = locations1.get(random.nextInt(locations1.size()));
            if (loc.getBlock().getType() != Material.CHEST) {
                i--;
                locations1.remove(loc);
                continue;
            }

            oldChestData.put(loc, ((Chest)loc.getBlock().getState().getData()).getFacing());

            activeLoot1Chests.add(loc);
            locations1.remove(loc);
        }

        for (Location remainingLoc : locations1) {
            oldChestData.put(remainingLoc, ((Chest)remainingLoc.getBlock().getState().getData()).getFacing());
            remainingLoc.getBlock().setType(Material.AIR);
        }

        for (int i = 0; i < Math.floor((float) locations2.size() * 0.5f); i++) {
            Location loc = locations2.get(random.nextInt(locations2.size()));
            if (loc.getBlock().getType() != Material.CHEST) {
                i--;
                locations2.remove(loc);
                continue;
            }

            oldChestData.put(loc, ((Chest)loc.getBlock().getState().getData()).getFacing());

            activeLoot2Chests.add(loc);
            locations2.remove(loc);
        }

        for (Location remainingLoc : locations2) {
            oldChestData.put(remainingLoc, ((Chest)remainingLoc.getBlock().getState().getData()).getFacing());
            remainingLoc.getBlock().setType(Material.AIR);
        }

        for (Location remainingLoc : locations3) {
            oldChestData.put(remainingLoc, ((Chest)remainingLoc.getBlock().getState().getData()).getFacing());
            activeLoot3Chests.add(remainingLoc);
        }

        if (refillTask1 != -1) {
            Bukkit.getScheduler().cancelTask(refillTask1);
        }if (refillTask2 != -1) {
            Bukkit.getScheduler().cancelTask(refillTask2);
        }

        refillTask1 = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Location loc :
                    activeLoot1Chests) {
                try {
                    org.bukkit.block.Chest chest = (org.bukkit.block.Chest) loc.getBlock().getState();
                    chest.getInventory().clear();
                    chest.getInventory().setItem(
                            random.nextInt(chest.getInventory().getSize()),
                            stringToItem(getConfig().getStringList("lowLoot").get(random.nextInt(getConfig().getStringList("lowLoot").size())))
                    );

                    chest.getInventory().setItem(
                            random.nextInt(chest.getInventory().getSize()),
                            stringToItem(getConfig().getStringList("lowLoot").get(random.nextInt(getConfig().getStringList("lowLoot").size())))
                    );
                } catch (Exception ignored) {}
            }
        }, 0, 20 * 20);

        refillTask2 = Bukkit.getScheduler().scheduleSyncRepeatingTask(this, () -> {
            for (Location loc :
                    activeLoot2Chests) {
                try {
                    org.bukkit.block.Chest chest = (org.bukkit.block.Chest) loc.getBlock().getState();
                    chest.getInventory().clear();
                    chest.getInventory().setItem(
                            random.nextInt(chest.getInventory().getSize()),
                            stringToItem(getConfig().getStringList("mediumLoot").get(random.nextInt(getConfig().getStringList("mediumLoot").size())))
                    );

                    chest.getInventory().setItem(
                            random.nextInt(chest.getInventory().getSize()),
                            stringToItem(getConfig().getStringList("mediumLoot").get(random.nextInt(getConfig().getStringList("mediumLoot").size())))
                    );
                } catch (Exception ignored) {}
            }
        }, 0, 20 * 30);

        for (Location loc : activeLoot3Chests) {
            try {
                org.bukkit.block.Chest chest = (org.bukkit.block.Chest) loc.getBlock().getState();
                chest.getInventory().setItem(
                        random.nextInt(chest.getInventory().getSize()),
                        stringToItem(getConfig().getStringList("highLoot").get(random.nextInt(getConfig().getStringList("highLoot").size())))
                );
            } catch (Exception ignored) {
                ignored.printStackTrace();
            }
        }

        // --- END LOOT CHESTS ---


        // Remove the 3 switches that should stay from the list.

        List<Location> switchLocs = new ArrayList<>((Collection<? extends Location>) getConfig().getList("switchLocations", new ArrayList<>()));
        for (int i = 0; i < 3; i++) {
            Location loc = switchLocs.get(random.nextInt(switchLocs.size()));
            switchLocs.remove(loc);
        }

        // Delete the remaining ones.

        for (Location loc :
                switchLocs) {
            oldBlocks.put(loc, loc.getBlock().getType());
            oldBlockData.put(loc, loc.getBlock().getData());
            loc.getBlock().setType(Material.AIR);
        }

        // Get the amount of killers needed. About 1 killer per 5 players.
        Integer killerAmount = (int) Math.floor(playersInLobby.size() / 5);
        if (killerAmount < 1) {
            killerAmount = 1;
        }

        // Clone the players in the lobby.
        List<UUID> playersToSort = new ArrayList<>(playersInLobby);

        // Loop through the amount of killers. For every index choose a random player from the playersToSort list.
        // To make sure nobody leaves DURING the sorting process, we also test if they are still online.
        // If they aren't anymore, we go back 1 index and try again with a different person.
        for (int i = 0; i < killerAmount; i++) {
            UUID chosen = playersToSort.get(random.nextInt(playersToSort.size()));
            Player player = Bukkit.getPlayer(chosen);
            if (player != null) {
                killers.add(chosen);
                playersToSort.remove(chosen);

                player.getInventory().setItem(0, new ItemStackBuilder(Material.WOOD_SWORD).unbreakable(true).itemFlags(ItemFlag.HIDE_UNBREAKABLE, ItemFlag.HIDE_ATTRIBUTES).displayName("§cKiller's Dagger").build());
                player.getInventory().setHelmet(new ItemStackBuilder(Material.DIAMOND_HELMET).unbreakable(true).itemFlags(ItemFlag.HIDE_UNBREAKABLE).displayName("§cKiller's Helmet").build());
                player.getInventory().setChestplate(new ItemStackBuilder(Material.CHAINMAIL_CHESTPLATE).unbreakable(true).itemFlags(ItemFlag.HIDE_UNBREAKABLE).displayName("§cKiller's Chestplate").build());
                player.getInventory().setLeggings(new ItemStackBuilder(Material.CHAINMAIL_LEGGINGS).unbreakable(true).itemFlags(ItemFlag.HIDE_UNBREAKABLE).displayName("§cKiller's Leggings").build());
                player.getInventory().setBoots(new ItemStackBuilder(Material.DIAMOND_BOOTS).unbreakable(true).itemFlags(ItemFlag.HIDE_UNBREAKABLE).displayName("§cKiller's Boots").build());

                player.addPotionEffect(new PotionEffect(PotionEffectType.INVISIBILITY, 999999999, 200));
                player.addPotionEffect(new PotionEffect(PotionEffectType.SLOW, 999999999, 1));
                player.addPotionEffect(new PotionEffect(PotionEffectType.JUMP, 999999999, 129));

                player.setScoreboard(scoreboardHandler.gameScoreboard);
                player.sendMessage("     §a------------------------\n§f\n               §fYou are a...\n                 §c§lKILLER\n§f \n §fYou have to §ckill §fall the survivors and\n     §cprevent §fthem from §cescaping§f. \n§f\n     §a------------------------");

                player.teleport((Location) getConfig().get("spawns.killer"));
            } else {
                i--;
                playersToSort.remove(chosen);
            }
        }

        // All the people who remain are not killers and are thus survivors.
        survivors.addAll(playersToSort);

        for (UUID survivorUUID :
                survivors) {

            Player player = Bukkit.getPlayer(survivorUUID);
            if (player != null) {
                player.setScoreboard(scoreboardHandler.gameScoreboard);
                player.sendMessage("       §a------------------------\n                 §fYou are a...\n                 §9§lSURVIVOR\n§f \n    §fYou have to to hit all the §9switches\n §fand gather §9loot §fto survive and §9escape§f. \n       §a------------------------");

                player.addPotionEffect(new PotionEffect(PotionEffectType.BLINDNESS, 999999, 247));
            } else {
                survivors.remove(survivorUUID);
            }
        }

        initialKillers = killers.size();
        initialSurvivors = survivors.size();
        switches = 0;

        // Clear the players on the lobby.
        playersInLobby.clear();

        for (Object loc :
                getConfig().getList("survivorgateblocks", new ArrayList<>())) {
            Location location = (Location) loc;
            oldBlocks.put(location, location.getBlock().getType());
            oldBlockData.put(location, location.getBlock().getData());

            location.getBlock().setType(Material.AIR);
        }

        for (Object loc :
                getConfig().getList("killergateblocks", new ArrayList<>())) {
            Location location = (Location) loc;
            oldBlocks.put(location, location.getBlock().getType());
            oldBlockData.put(location, location.getBlock().getData());
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            if (currentGameState == Labyrinth.GameState.ingame) {
                List<UUID> playersInGame = new ArrayList<>(survivors);
                playersInGame.addAll(killers);

                for (UUID id :
                        playersInGame) {
                    Player player = Bukkit.getPlayer(id);
                    player.sendMessage("§c§lATTENTION! §cKillers will be released in 25 seconds.");
                    player.sendTitle("§c§lATTENTION!", "§cKillers will be released in 25 seconds.");
                    player.playSound(player.getLocation(), Sound.WITHER_HURT, 1, 1.5f);
                }

                Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                    if (currentGameState == Labyrinth.GameState.ingame) {
                        List<UUID> playersInGame2 = new ArrayList<>(survivors);
                        playersInGame2.addAll(killers);

                        for (UUID id :
                                playersInGame2) {
                            Player player = Bukkit.getPlayer(id);
                            player.sendMessage("§c§lATTENTION! §cKillers will be released in 10 seconds.");
                            player.sendTitle("§c§lATTENTION!", "§cKillers will be released in 10 seconds.");
                            player.playSound(player.getLocation(), Sound.WITHER_HURT, 1, 1.5f);
                        }

                        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
                            if (currentGameState == Labyrinth.GameState.ingame) {
                                List<UUID> playersInGame3 = new ArrayList<>(survivors);
                                playersInGame3.addAll(killers);

                                for (UUID id :
                                        playersInGame3) {
                                    Player player = Bukkit.getPlayer(id);
                                    player.sendMessage("§c§lATTENTION! §cKillers are now roaming the area.");
                                    player.sendTitle("§c§lATTENTION!", "§cKillers are now roaming the area.");
                                    player.playSound(player.getLocation(), Sound.WITHER_SPAWN, 1, 0.1f);
                                }

                                for (Object loc :
                                        getConfig().getList("killergateblocks", new ArrayList<>())) {
                                    ((Location)loc).getBlock().setType(Material.AIR);
                                }
                            }
                        }, 20 * 15);
                    }
                }, 20 * 15);
            }
        }, 20 * 5);
    }

    Map<Location, Material> oldBlocks = new HashMap<>();
    Map<Location, Byte> oldBlockData = new HashMap<>();

    void endGame() {
        canJoin = false;
        currentGameState = GameState.end;

        for (Location location :
                oldBlocks.keySet()) {
            location.getBlock().setType(oldBlocks.get(location));
            location.getBlock().setData(oldBlockData.get(location));
        }

        for (Location location : oldChestData.keySet()) {
            location.getBlock().setType(Material.CHEST);
            org.bukkit.block.Chest state = (org.bukkit.block.Chest) location.getBlock().getState();
            Chest chest = (Chest) state.getData();
            chest.setFacingDirection(oldChestData.get(location));
            state.setData(chest);
            state.getInventory().clear();
            state.update(true);
        }

        Bukkit.getScheduler().cancelTask(refillTask1);
        refillTask1 = -1;
        Bukkit.getScheduler().cancelTask(refillTask2);
        refillTask2 = -1;

        spectators.addAll(killers);
        spectators.addAll(survivors);

        if (killers.size() == 0) {
            // All killers have died. Survivors won!
            chatHandler.sendMessageToSpectators("      §a------------------------\n§f \n              §fThe winners were...\n                 §9§lSURVIVORS!\n§f \n      §fAll §ckillers §fwere exterminated.\n§f \n      §a------------------------");

            String winners = "§eWinners: §f";
            for (UUID survivor :
                    survivors) {
                if (Bukkit.getPlayer(survivor) != null) {
                    winners += Bukkit.getPlayer(survivor).getName() + ", ";
                }
            }

            winners = winners.substring(0, winners.length() - 2) + "§e.";

            chatHandler.sendMessageToSpectators("");
            chatHandler.sendMessageToSpectators(winners);
        } else if (survivors.size() == 0) {
            // All survivors have died. Killers won!
            chatHandler.sendMessageToSpectators("      §a------------------------\n§f \n             §fThe winners were...\n                 §a§lKILLERS!\n§f \n   §fAll §9survivors §fwere exterminated.\n§f \n      §a------------------------");

            String winners = "§eWinners: §f";
            for (UUID killer :
                    killers) {
                if (Bukkit.getPlayer(killer) != null) {
                    winners += Bukkit.getPlayer(killer).getName() + ", ";
                }
            }

            winners = winners.substring(0, winners.length() - 2) + "§e.";

            chatHandler.sendMessageToSpectators("");
            chatHandler.sendMessageToSpectators(winners);
        } else if (switches >= 3) {
            for (UUID player :
                    spectators) {
                if (Bukkit.getPlayer(player) != null) {
                    Bukkit.getPlayer(player).sendMessage("      §a------------------------\n§f \n              §fThe winners were...\n                 §9§lSURVIVORS!\n§f \n    §fA §9survivor §fescaped the labyrinth.\n§f \n      §a------------------------");
                }
            }

            String winners = "§eWinners: §f";
            for (UUID survivor :
                    survivors) {
                if (Bukkit.getPlayer(survivor) != null) {
                    winners += Bukkit.getPlayer(survivor).getName() + ", ";
                }
            }

            winners = winners.substring(0, winners.length() - 2) + "§e.";

            chatHandler.sendMessageToSpectators("");
            chatHandler.sendMessageToSpectators(winners);
        }

        Bukkit.getScheduler().scheduleSyncDelayedTask(this, () -> {
            killers.clear();
            survivors.clear();

            List<UUID> specs = new ArrayList<>(spectators);

            for (UUID player :
                    specs) {
                if (Bukkit.getPlayer(player) != null) {
                    playerLeft(Bukkit.getPlayer(player));
                }
            }

            currentGameState = GameState.lobby;
        }, 200);

        for (Location location : events.leversHit) {
            BlockState state = location.getBlock().getState();
            Lever lever = (Lever) state.getData();
            lever.setPowered(false);
            state.setData(lever);
            state.update(true);
        }

        events.leversHit.clear();
    }

    void killerAmountChanged() {
        if (killers.size() == 0) {
            // All killers have died. Survivors won!
            endGame();
        }
    }

    void survivorAmountChanged() {
        if (survivors.size() == 0) {
            // All survivors have died. Killers won!
            endGame();
        }
    }

    ItemStack stringToItem(String string) {
        if (string.split("x").length > 1) {
            try {
                if (string.split("\\:").length > 1) {
                    short durability = (short) Integer.parseInt(string.split("\\:")[1]);
                    Integer amount = Integer.parseInt(string.split("x")[0]);
                    Material type = Material.valueOf(string.split("x")[1].split("\\:")[0]);

                    return new ItemStackBuilder(type).amount(amount).durability((short) (type.getMaxDurability() - durability)).build();
                } else {
                    Integer amount = Integer.parseInt(string.split("x")[0]);
                    if (string.split("x")[1].equalsIgnoreCase("INSTAHEALTH")) {
                        Potion potion = new Potion(PotionType.INSTANT_HEAL, 2, false);
                        ItemStack item = new ItemStack(Material.POTION);
                        potion.apply(item);
                        return item;
                    }

                    Material type = Material.valueOf(string.split("x")[1]);

                    return new ItemStackBuilder(type).amount(amount).build();
                }
            } catch (Exception ex) {
                ex.printStackTrace();

                return new ItemStackBuilder(Material.PAPER).displayName("Failed to load item.").lore(new String[] {"Data received was " + string}).build();
            }
        } else {
            return new ItemStackBuilder(Material.PAPER).displayName("Failed to load item.").lore(new String[] {"Data received was " + string}).build();
        }
    }
}
