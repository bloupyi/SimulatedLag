package fr.bloup.simulatedLag.commands;

import fr.bloup.simulatedLag.SimulatedLag;
import fr.bloup.simulatedLag.utils.DelayUtils;
import org.bukkit.Bukkit;
import org.bukkit.command.Command;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;

import java.util.List;
import java.util.stream.Collectors;

public class SimulatedLagCommand extends AbstractCommand {

    public SimulatedLagCommand(SimulatedLag plugin) {
        super(plugin);
    }

    @Override
    public String getPermission() {
        return "simulatedlag.admin";
    }

    @Override
    public boolean runCommand(CommandSender sender, Command rootCommand, String label, String[] args) {
        if (args.length == 0) {
            sender.sendMessage("[Lag] Usage: /" + label + " <player> [ping]");
            return true;
        }

        Player target = Bukkit.getPlayer(args[0]);
        if (target == null) {
            sender.sendMessage("[Lag] Player not found: " + args[0]);
            return true;
        }

        if (args.length > 1 && args[1].matches("-?\\d+")) {
            int ping = Integer.parseInt(args[1]);
            plugin.setPlayerTargetedLag(target, ping);
            sender.sendMessage("[Lag] Targeted Ping of " + target.getName() + " is now: " + ping);
        } else {
            sender.sendMessage("[Lag] " + target.getName() + "'s normal Ping: " + target.getPing());
            sender.sendMessage("[Lag] " + target.getName() + "'s targeted Ping: " + plugin.targetedPlayerLag.get(target));
            sender.sendMessage("[Lag] " + target.getName() + "'s packets delay: " + new DelayUtils(plugin).getDelayForPlayerLag(target));
        }

        return true;
    }

    @Override
    public List<String> onTabComplete(CommandSender sender, Command rootCommand, String label, String[] args) {
        return Bukkit.getOnlinePlayers().stream().map(Player::getName).collect(Collectors.toList());
    }
}
