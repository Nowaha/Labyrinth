package me.nowaha.labyrinth;

import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

public class ChatHandler {
    Labyrinth pl;

    public ChatHandler(Labyrinth main) {
        pl = main;
    }

    public void sendMessageToAll(String message) {
        List<UUID> allPlayers = new ArrayList<>(pl.survivors);
        allPlayers.addAll(pl.spectators);
        allPlayers.addAll(pl.killers);

        for (UUID id : allPlayers) {
            Player p = Bukkit.getPlayer(id);
            p.sendMessage(message);
        }
    }

    public void sendMessageToKillers(String message) {
        for (UUID id : pl.killers) {
            Player p = Bukkit.getPlayer(id);
            p.sendMessage(message);
        }
    }

    public void sendMessageToSurvivors(String message) {
        for (UUID id : pl.survivors) {
            Player p = Bukkit.getPlayer(id);
            p.sendMessage(message);
        }
    }

    public void sendMessageToSpectators(String message) {
        for (UUID id : pl.spectators) {
            Player p = Bukkit.getPlayer(id);
            p.sendMessage(message);
        }
    }

    public void sendMessageToLobby(String message) {
        for (UUID id : pl.playersInLobby) {
            Player p = Bukkit.getPlayer(id);
            p.sendMessage(message);
        }
    }
}
