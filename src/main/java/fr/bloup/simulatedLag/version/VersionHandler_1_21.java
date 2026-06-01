package fr.bloup.simulatedLag.version;

import io.netty.channel.Channel;
import org.bukkit.entity.Player;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class VersionHandler_1_21 implements VersionHandler {
    /**
     * Retrieves the Netty network channel of a player for version 1.21.x.
     * <p>
     * The channel is located through reflection (ServerPlayer -> ServerGamePacketListenerImpl
     * -> Connection -> Channel) so the code does not depend on obfuscated field names or on the
     * versioned CraftBukkit package. This works on both Paper (Mojang mappings) and Spigot.
     *
     * @param player The player whose channel should be retrieved.
     * @return The player's Netty channel.
     */
    @Override
    public Channel getPlayerChannel(Player player) {
        try {
            Method getHandle = player.getClass().getMethod("getHandle");
            Object serverPlayer = getHandle.invoke(player);

            Object packetListener = getFieldBySimpleType(serverPlayer, "ServerGamePacketListenerImpl");
            Object connection = getFieldBySimpleType(packetListener, "Connection");

            return (Channel) getFieldByAssignable(connection, Channel.class);
        } catch (Exception e) {
            throw new RuntimeException("Unable to retrieve Netty channel for player " + player.getName(), e);
        }
    }

    /**
     * Finds, in the class hierarchy of {@code obj}, the value of the first field whose declared
     * type has the given simple name.
     */
    private Object getFieldBySimpleType(Object obj, String typeSimpleName) throws IllegalAccessException {
        Class<?> current = obj.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (field.getType().getSimpleName().equals(typeSimpleName)) {
                    field.setAccessible(true);
                    return field.get(obj);
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("No field of type " + typeSimpleName + " found in " + obj.getClass());
    }

    /**
     * Finds, in the class hierarchy of {@code obj}, the value of the first field whose declared
     * type is assignable to {@code type}.
     */
    private Object getFieldByAssignable(Object obj, Class<?> type) throws IllegalAccessException {
        Class<?> current = obj.getClass();
        while (current != null) {
            for (Field field : current.getDeclaredFields()) {
                if (type.isAssignableFrom(field.getType())) {
                    field.setAccessible(true);
                    return field.get(obj);
                }
            }
            current = current.getSuperclass();
        }
        throw new IllegalStateException("No field assignable to " + type + " found in " + obj.getClass());
    }
}
