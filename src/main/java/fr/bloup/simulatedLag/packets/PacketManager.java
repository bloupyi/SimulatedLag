package fr.bloup.simulatedLag.packets;

import fr.bloup.simulatedLag.SimulatedLag;
import fr.bloup.simulatedLag.utils.DelayUtils;
import fr.bloup.simulatedLag.version.VersionHandler;
import io.netty.channel.*;
import lombok.RequiredArgsConstructor;

import org.bukkit.entity.Player;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

@RequiredArgsConstructor
public class PacketManager {
    private final SimulatedLag plugin;

    private final DelayUtils delayUtils;
    private final VersionHandler versionHandler;
    private final ScheduledExecutorService executorService = Executors.newSingleThreadScheduledExecutor();

    /**
     * Intercepts incoming and outgoing packets for a specific player.
     * Adds a custom handler to the player's network channel.
     *
     * @param player The player whose packets should be intercepted.
     */
    public void interceptPlayerPackets(Player player) {
        Channel channel = versionHandler.getPlayerChannel(player);

        if (channel == null) {
            plugin.log.warning("[SimulatedLag] Impossible de récupérer le canal du joueur" + player.getName());
            return;
        }

        ChannelDuplexHandler handler = new ChannelDuplexHandler() {
            @Override
            public void channelRead(ChannelHandlerContext ctx, Object msg) {
                // Process incoming packets
                processPacket(ctx, msg, player, super::channelRead);
            }

            @Override
            public void write(ChannelHandlerContext ctx, Object msg, ChannelPromise promise) {
                // Process outgoing packets
                processPacket(ctx, msg, player, (ctx1, msg1) -> super.write(ctx1, msg1, promise));
            }
        };

        // Add our handler before the default packet handler
        channel.pipeline().addBefore("packet_handler", player.getName(), handler);
    }

    /**
     * Removes the custom packet handler from the player's network channel.
     *
     * @param player The player to remove the handler for.
     */
    public void removePlayerFromChannel(Player player) {
        Channel channel = versionHandler.getPlayerChannel(player);

        if (channel != null && channel.pipeline().get(player.getName()) != null) {
            channel.pipeline().remove(player.getName());
        }
    }

    /**
     * Processes packets, applying a delay before forwarding them if necessary.
     * Ping-related packets are ignored.
     *
     * @see DelayUtils#getDelayForPlayerLag(Player) Used to determine the delay based on the player's ping before scheduling packet processing.
     *
     * @param ctx     The Netty channel context.
     * @param msg     The packet being processed.
     * @param player  The player associated with the packet.
     * @param handler The packet handler function.
     */
    private void processPacket(ChannelHandlerContext ctx, Object msg, Player player, PacketHandler handler) {
        Runnable task = () -> {
            try {
                handler.handle(ctx, msg);
            } catch (Exception e) {
                e.printStackTrace();
            }
        };

        if (isPacket(msg) && !isPingPacket(msg)) {
            executorService.schedule(task, delayUtils.getDelayForPlayerLag(player), TimeUnit.MILLISECONDS);
        } else {
            task.run();
        }
    }

    /**
     * Determines whether the intercepted message is a Minecraft protocol packet.
     * NMS packet classes live in the {@code net.minecraft.network.protocol} package on both
     * Paper (Mojang mappings) and Spigot, so detecting by package name avoids depending on the
     * NMS jar at compile time.
     *
     * @param msg The intercepted message.
     * @return True if the message is a protocol packet, false otherwise.
     */
    private boolean isPacket(Object msg) {
        return msg.getClass().getName().startsWith("net.minecraft.network.protocol.");
    }

    @FunctionalInterface
    private interface PacketHandler  {
        void handle(ChannelHandlerContext ctx, Object msg) throws Exception;
    }

    /**
     * Determines if a packet is related to player ping (such as KeepAlive packets).
     * These packets should not be delayed.
     *
     * @param msg The packet to check.
     * @return True if the packet is a ping-related packet, false otherwise.
     */
    private boolean isPingPacket(Object msg) {
        String className = msg.getClass().getName();
        return className.contains("KeepAlive") || className.toLowerCase().contains("ping");
    }
}
