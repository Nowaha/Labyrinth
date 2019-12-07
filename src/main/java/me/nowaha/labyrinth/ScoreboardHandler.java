package me.nowaha.labyrinth;

import org.bukkit.Bukkit;
import org.bukkit.Location;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.scoreboard.*;

import java.util.*;

public class ScoreboardHandler {
    ScoreboardManager sm;

    Scoreboard lobbyScoreboard;
    Scoreboard gameScoreboard;

    Labyrinth pl;

    public ScoreboardHandler(Labyrinth main) {
        pl = main;

        sm = Bukkit.getScoreboardManager();
        gameScoreboard = sm.getNewScoreboard();

        createLobbyScoreboard();
        createGameScoreboard();
    }

    Integer lobbyUpdateID = -1;
    Integer gameUpdateID = -1;

    void createLobbyScoreboard() {
        lobbyScoreboard = sm.getNewScoreboard();

        Objective objective = lobbyScoreboard.registerNewObjective("LabyrinthLobby", "dummy");
        lobbyScoreboard.getObjective("LabyrinthLobby").setDisplayName("§c§lLabyrinth");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Team empty1 = lobbyScoreboard.registerNewTeam("empty1"); empty1.addEntry("  "); objective.getScore("  ").setScore(6);

        Team players = lobbyScoreboard.registerNewTeam("players");
        players.addEntry("Players: §a");
        objective.getScore("Players: §a").setScore(5);

        Team empty2 = lobbyScoreboard.registerNewTeam("empty2"); empty2.addEntry("   "); objective.getScore("   ").setScore(4);

        Team status = lobbyScoreboard.registerNewTeam("status");
        status.addEntry("§r");
        objective.getScore("§r").setScore(3);

        Team empty3 = lobbyScoreboard.registerNewTeam("empty3"); empty3.addEntry("    "); objective.getScore("    ").setScore(2);

        Team ip = lobbyScoreboard.registerNewTeam("ip"); ip.addEntry("§cblockoutmc.com"); objective.getScore("§cblockoutmc.com").setScore(1);

        lobbyUpdateID = Bukkit.getScheduler().scheduleSyncRepeatingTask(pl, new Runnable() {
            Integer tick = 0;
            Integer time = 20 * 30;
            Integer animTick = 0;

            @Override
            public void run() {
                if (pl.playersInLobby.size() == 0) return;

                if (pl.currentGameState != Labyrinth.GameState.lobby) return;

                animTick++;

                switch (animTick) {
                    case 0:
                        objective.setDisplayName("§c§lLabyrinth");
                        break;
                    case 41:
                        objective.setDisplayName("§4§lL§c§labyrinth");
                        break;
                    case 42:
                        objective.setDisplayName("§f§lL§4§la§c§lbyrinth");
                        break;
                    case 43:
                        objective.setDisplayName("§f§lLa§4§lb§c§lyrinth");
                        break;
                    case 44:
                        objective.setDisplayName("§f§lLab§4§ly§c§lrinth");
                        break;
                    case 45:
                        objective.setDisplayName("§f§lLaby§4§lr§c§linth");
                        break;
                    case 46:
                        objective.setDisplayName("§f§lLabyr§4§li§c§lnth");
                        break;
                    case 47:
                        objective.setDisplayName("§f§lLabyri§4§ln§c§lth");
                        break;
                    case 48:
                        objective.setDisplayName("§f§lLabyrin§4§lt§c§lh");
                        break;
                    case 49:
                        objective.setDisplayName("§f§lLabyrint§4§lh");
                        break;
                    case 50:
                        objective.setDisplayName("§f§lLabyrinth");
                        break;
                    case 55:
                        objective.setDisplayName("§4§lLabyrinth");
                        break;
                    case 60:
                        objective.setDisplayName("§c§lLabyrinth");
                        break;
                    case 65:
                        objective.setDisplayName("§4§lLabyrinth");
                        break;
                    case 70:
                        objective.setDisplayName("§c§lLabyrinth");
                        break;
                    case 75:
                        objective.setDisplayName("§4§lLabyrinth");
                        break;
                    case 80:
                        objective.setDisplayName("§c§lLabyrinth");
                        animTick = -1;
                        break;
                }


                if (pl.playersInLobby.size() < pl.minPlayers) {
                    if (tick <= 20) {
                        status.setPrefix("§6Waiting.");
                    } else if (tick <= 40) {
                        status.setPrefix("§6Waiting..");
                    } else if (tick <= 60) {
                        status.setPrefix("§6Waiting...");
                    } else {
                        tick = 0;
                    }

                    tick++;
                    time = 20 * 30;
                } else {
                    if (time == 200 || (time <= 105 && time % 20 == 0)) {
                        for (UUID uuid : pl.playersInLobby) {
                            Player player = Bukkit.getPlayer(uuid);
                            if (((int)Math.ceil(time / 20)) == 1) {
                                player.sendMessage("§cLabyrinth §7» §rGame starting in §a1 §fsecond.");
                            } else {
                                player.sendMessage("§cLabyrinth §7» §rGame starting in §a" + ((int)Math.ceil(time / 20)) + " §fseconds.");
                            }

                            player.playSound(player.getLocation(), Sound.NOTE_PLING, 10, 2);
                        }
                    }

                    String timeStr = ((int)Math.ceil(time / 20) + 1) + "";
                    status.setPrefix("Starting in " + timeStr + "s");
                    time--;

                    if (time <= 0) {
                        status.setPrefix("§aLoading...");
                        pl.beginGame();
                    }
                }

                players.setSuffix(pl.playersInLobby.size() + "/50");
            }
        }, 0, 1);
    }

    void createGameScoreboard() {
        gameScoreboard = sm.getNewScoreboard();

        Objective objective = gameScoreboard.registerNewObjective("LabyrinthGame", "dummy");
        objective.setDisplayName("§c§lLabyrinth");
        objective.setDisplaySlot(DisplaySlot.SIDEBAR);

        Team empty1 = gameScoreboard.registerNewTeam("empty1"); empty1.addEntry("  "); objective.getScore("  ").setScore(11);

        Team survivors = gameScoreboard.registerNewTeam("survivors");
        survivors.addEntry("Survivors:");
        objective.getScore("Survivors:").setScore(10);

        Team survivorsValue = gameScoreboard.registerNewTeam("survivorsValue");
        survivorsValue.addEntry("§8> §9");
        objective.getScore("§8> §9").setScore(9);

        Team empty4 = gameScoreboard.registerNewTeam("empty4"); empty4.addEntry("      "); objective.getScore("      ").setScore(8);

        Team killers = gameScoreboard.registerNewTeam("killers");
        killers.addEntry("Killers:");
        objective.getScore("Killers:").setScore(7);

        Team killersValue = gameScoreboard.registerNewTeam("killersValue");
        killersValue.addEntry("§8> §c");
        objective.getScore("§8> §c").setScore(6);

        Team empty2 = gameScoreboard.registerNewTeam("empty2"); empty2.addEntry("   "); objective.getScore("   ").setScore(5);

        Team switches = gameScoreboard.registerNewTeam("switches");
        switches.addEntry("Switches:");
        objective.getScore("Switches:").setScore(4);

        Team switchesValue = gameScoreboard.registerNewTeam("switchesValue");
        switchesValue.addEntry("§8> §e");
        objective.getScore("§8> §e").setScore(3);

        Team empty3 = gameScoreboard.registerNewTeam("empty3"); empty3.addEntry("    "); objective.getScore("    ").setScore(2);

        Team ip = gameScoreboard.registerNewTeam("ip"); ip.addEntry("§cblockoutmc.com"); objective.getScore("§cblockoutmc.com").setScore(1);

        gameUpdateID = Bukkit.getScheduler().scheduleSyncRepeatingTask(pl, new Runnable() {

            Integer animTick = 0;

            @Override
            public void run() {
                if (pl.currentGameState == Labyrinth.GameState.ingame || pl.currentGameState == Labyrinth.GameState.end) {
                    survivorsValue.setSuffix(pl.survivors.size() + "/" + pl.initialSurvivors);
                    killersValue.setSuffix(pl.killers.size() + "/" + pl.initialKillers);

                    if (pl.switches >= 3) {
                        switchesValue.setSuffix("§6§l" + pl.switches + "/3");
                    } else {
                        switchesValue.setSuffix(pl.switches + "/3");
                    }

                    animTick++;

                    switch (animTick) {
                        case 0:
                            objective.setDisplayName("§c§lLabyrinth");
                            break;
                        case 41:
                            objective.setDisplayName("§4§lL§c§labyrinth");
                            break;
                        case 42:
                            objective.setDisplayName("§f§lL§4§la§c§lbyrinth");
                            break;
                        case 43:
                            objective.setDisplayName("§f§lLa§4§lb§c§lyrinth");
                            break;
                        case 44:
                            objective.setDisplayName("§f§lLab§4§ly§c§lrinth");
                            break;
                        case 45:
                            objective.setDisplayName("§f§lLaby§4§lr§c§linth");
                            break;
                        case 46:
                            objective.setDisplayName("§f§lLabyr§4§li§c§lnth");
                            break;
                        case 47:
                            objective.setDisplayName("§f§lLabyri§4§ln§c§lth");
                            break;
                        case 48:
                            objective.setDisplayName("§f§lLabyrin§4§lt§c§lh");
                            break;
                        case 49:
                            objective.setDisplayName("§f§lLabyrint§4§lh");
                            break;
                        case 50:
                            objective.setDisplayName("§f§lLabyrinth");
                            break;
                        case 55:
                            objective.setDisplayName("§4§lLabyrinth");
                            break;
                        case 60:
                            objective.setDisplayName("§c§lLabyrinth");
                            break;
                        case 65:
                            objective.setDisplayName("§4§lLabyrinth");
                            break;
                        case 70:
                            objective.setDisplayName("§c§lLabyrinth");
                            break;
                        case 75:
                            objective.setDisplayName("§4§lLabyrinth");
                            break;
                        case 80:
                            objective.setDisplayName("§c§lLabyrinth");
                            animTick = -1;
                            break;
                    }
                }
            }
        }, 0, 1);
    }

}
