package fr.bloup.simulatedLag.version;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;


public interface VersionHandler {
    Channel getPlayerChannel(Player player);
}
