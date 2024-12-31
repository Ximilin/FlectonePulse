package net.flectone.pulse.util;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import org.bukkit.Bukkit;
import org.bukkit.Material;
import org.bukkit.Server;
import org.bukkit.inventory.ItemStack;
import org.bukkit.plugin.Plugin;
import org.bukkit.util.CachedServerIcon;
import org.jetbrains.annotations.NotNull;

import java.lang.reflect.Field;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Getter
@Singleton
public class BukkitServerUtil implements ServerUtil {

    public final static boolean IS_FOLIA;
    public final static boolean IS_PAPER;
    public final static boolean IS_1_20_6_OR_NEWER;

    static {
        IS_FOLIA = isFolia();
        IS_PAPER = isPaper();
        IS_1_20_6_OR_NEWER = getBukkitVersion() >= 20.6;
    }

    private final Plugin plugin;

    @Inject
    public BukkitServerUtil(Plugin plugin) {
        this.plugin = plugin;
    }

    @Override
    public String getMinecraftName(Object itemStack) {
        if (!(itemStack instanceof ItemStack is)) return "";

        try {

            if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_18)) {
                Material material = is.getType();
                return (material.isBlock() ? "block" : "item") + ".minecraft." + material.toString().toLowerCase();
            }

            Object nmsStack = is.getClass().getMethod("asNMSCopy", ItemStack.class).invoke(null, is);

            assert nmsStack != null;
            Object item = nmsStack.getClass().getMethod("getItem").invoke(nmsStack);

            return (String) item.getClass().getMethod("getName").invoke(item);
        } catch (Exception ex) {
            return "";
        }
    }

    @Override
    public String getTPS() {
        try {
            Server server = Bukkit.getServer();

            Field consoleField = server.getClass().getDeclaredField("console");
            consoleField.setAccessible(true);

            Object minecraftServer = consoleField.get(server);

            Field recentTps = minecraftServer.getClass().getSuperclass().getDeclaredField("recentTps");
            recentTps.setAccessible(true);

            double tps = Math.round(((double[]) recentTps.get(minecraftServer))[0] * 10.0)/10.0;

            return String.valueOf(Math.min(tps, 20.0));
        } catch (Throwable e) {
            return null;
        }
    }

    @Override
    public JsonElement getMOTD() {
        JsonObject jsonObject = new JsonObject();
        jsonObject.addProperty("text", Bukkit.getServer().getMotd());
        return jsonObject;
    }

    @Override
    public String getVersion() {
        return Bukkit.getMinecraftVersion();
    }

    @NotNull
    @Override
    public String getIcon() {
        CachedServerIcon serverIcon = Bukkit.getServerIcon();
        return serverIcon == null
                ? ""
                : serverIcon.getData() == null ? "" : serverIcon.getData();
    }

    @Override
    public int getMax() {
        return Bukkit.getMaxPlayers();
    }

    @Override
    public int getOnlineCount() {
        return Bukkit.getOnlinePlayers().size();
    }

    @Override
    public boolean hasProject(String projectName) {
        return Bukkit.getPluginManager().getPlugin(projectName) != null;
    }

    private static double getBukkitVersion() {
        double finalVersion = 0.0;
        Matcher m = Pattern.compile("1\\.(\\d+(\\.\\d+)?)").matcher(Bukkit.getVersion());
        if (m.find()) {
            try {
                finalVersion = Double.parseDouble(m.group(1));
            } catch (Exception ignored) {}
        }

        return finalVersion;
    }

    private static boolean isFolia() {
        try {
            Class.forName("io.papermc.paper.threadedregions.ThreadedRegionizer");
            return true;
        } catch (ClassNotFoundException ignored) {}

        return false;
    }

    private static boolean isPaper() {
        try {
            Class.forName("com.destroystokyo.paper.ParticleBuilder");
            return true;
        } catch (ClassNotFoundException ignored) {}

        return false;
    }
}
