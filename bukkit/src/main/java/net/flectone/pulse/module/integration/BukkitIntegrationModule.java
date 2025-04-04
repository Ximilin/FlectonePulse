package net.flectone.pulse.module.integration;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.model.FEntity;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.module.integration.discord.DiscordModule;
import net.flectone.pulse.module.integration.interactivechat.InteractiveChatModule;
import net.flectone.pulse.module.integration.luckperms.LuckPermsModule;
import net.flectone.pulse.module.integration.placeholderapi.PlaceholderAPIModule;
import net.flectone.pulse.module.integration.plasmovoice.PlasmoVoiceModule;
import net.flectone.pulse.module.integration.simplevoice.SimpleVoiceModule;
import net.flectone.pulse.module.integration.skinsrestorer.SkinsRestorerModule;
import net.flectone.pulse.module.integration.supervanish.SuperVanishModule;
import net.flectone.pulse.module.integration.tab.TABModule;
import net.flectone.pulse.module.integration.telegram.TelegramModule;
import net.flectone.pulse.module.integration.triton.TritonModule;
import net.flectone.pulse.module.integration.twitch.TwitchModule;
import net.flectone.pulse.module.integration.vault.VaultModule;
import net.flectone.pulse.util.MessageTag;
import net.flectone.pulse.util.ServerUtil;
import net.flectone.pulse.util.logging.FLogger;
import net.kyori.adventure.text.Component;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.metadata.MetadataValue;

import java.util.Collections;
import java.util.Set;
import java.util.function.UnaryOperator;

@Singleton
public class BukkitIntegrationModule extends IntegrationModule {

    private final Injector injector;

    @Inject
    public BukkitIntegrationModule(FileManager fileManager,
                                   FLogger fLogger,
                                   ServerUtil serverUtil,
                                   Injector injector) {
        super(fileManager, injector);

        this.injector = injector;

        if (serverUtil.hasProject("PlaceholderAPI")) {
            addChildren(PlaceholderAPIModule.class);
        }

        if (serverUtil.hasProject("Vault")) {
            addChildren(VaultModule.class);
        }

        if (serverUtil.hasProject("InteractiveChat")) {
            addChildren(InteractiveChatModule.class);
        }

        if (serverUtil.hasProject("LuckPerms")) {
            addChildren(LuckPermsModule.class);
        }

        if (serverUtil.hasProject("SuperVanish") || serverUtil.hasProject("PremiumVanish")) {
            addChildren(SuperVanishModule.class);
        }

        if (serverUtil.hasProject("SkinsRestorer")) {
            addChildren(SkinsRestorerModule.class);
        }

        if (serverUtil.hasProject("VoiceChat")) {
            addChildren(SimpleVoiceModule.class);
        }

        if (serverUtil.hasProject("PlasmoVoice")) {
            try {
                Class.forName("su.plo.voice.api.server.event.audio.source.ServerSourceCreatedEvent");

                addChildren(PlasmoVoiceModule.class);
            } catch (ClassNotFoundException e) {
                fLogger.warning("Update PlasmoVoice to the latest version");
            }
        }

        if (serverUtil.hasProject("TAB")) {
            addChildren(TABModule.class);
        }

        if (serverUtil.hasProject("Triton")) {
            addChildren(TritonModule.class);
        }
    }

    @Override
    public String checkMention(FEntity fSender, String message) {
        if (checkModulePredicates(fSender)) return message;

        if (getChildren().contains(InteractiveChatModule.class)) {
            return injector.getInstance(InteractiveChatModule.class).checkMention(fSender, message);
        }

        return message;
    }

    @Override
    public String markSender(FEntity fSender, String message) {
        if (checkModulePredicates(fSender)) return message;

        if (getChildren().contains(InteractiveChatModule.class)) {
            return injector.getInstance(InteractiveChatModule.class).markSender(fSender, message);
        }

        return message;
    }

    @Override
    public String setPlaceholders(FEntity sender, FEntity receiver, String message, boolean permission) {
        if (message == null) return null;
        if (checkModulePredicates(sender)) return message;
        if (checkModulePredicates(receiver)) return message;
        if (!getChildren().contains(PlaceholderAPIModule.class)) return message;

        return injector.getInstance(PlaceholderAPIModule.class).setPlaceholders(sender, receiver, message, permission);
    }

    @Override
    public boolean hasFPlayerPermission(FPlayer fPlayer, String permission) {
        if (!isEnable()) return false;

        boolean value = true;

        if (getChildren().contains(LuckPermsModule.class)) {
            value = injector.getInstance(LuckPermsModule.class).hasLuckPermission(fPlayer, permission);
        }

        if (getChildren().contains(VaultModule.class)) {
            value = value && injector.getInstance(VaultModule.class).hasVaultPermission(fPlayer, permission);
        }

        return value;
    }

    @Override
    public String getPrefix(FPlayer fPlayer) {
        if (!isEnable()) return null;

        if (getChildren().contains(LuckPermsModule.class)) {
            return injector.getInstance(LuckPermsModule.class).getPrefix(fPlayer);
        }

        if (getChildren().contains(VaultModule.class)) {
            return injector.getInstance(VaultModule.class).getPrefix(fPlayer);
        }

        return null;
    }

    @Override
    public String getSuffix(FPlayer fPlayer) {
        if (!isEnable()) return null;

        if (getChildren().contains(LuckPermsModule.class)) {
            return injector.getInstance(LuckPermsModule.class).getSuffix(fPlayer);
        }

        if (getChildren().contains(VaultModule.class)) {
            return injector.getInstance(VaultModule.class).getSuffix(fPlayer);
        }

        return null;
    }

    @Override
    public Set<String> getGroups() {
        if (!isEnable()) return Collections.emptySet();

        if (getChildren().contains(LuckPermsModule.class)) {
            return injector.getInstance(LuckPermsModule.class).getGroups();
        }

        if (getChildren().contains(VaultModule.class)) {
            return injector.getInstance(VaultModule.class).getGroups();
        }

        return Collections.emptySet();
    }

    @Override
    public int getGroupWeight(FPlayer fPlayer) {
        if (!isEnable()) return 0;
        if (!getChildren().contains(LuckPermsModule.class)) return 0;

        return injector.getInstance(LuckPermsModule.class).getGroupWeight(fPlayer);
    }

    @Override
    public String getTextureUrl(FEntity sender) {
        if (!isEnable()) return null;
        if (!getChildren().contains(SkinsRestorerModule.class)) return null;
        if (!(sender instanceof FPlayer fPlayer)) return null;

        return injector.getInstance(SkinsRestorerModule.class).getTextureUrl(fPlayer);
    }

    @Override
    public void sendMessage(FEntity sender, MessageTag messageTag, UnaryOperator<String> discordString) {
        if (getChildren().contains(DiscordModule.class)) {
            injector.getInstance(DiscordModule.class).sendMessage(sender, messageTag, discordString);
        }

        if (getChildren().contains(TwitchModule.class)) {
            injector.getInstance(TwitchModule.class).sendMessage(sender, messageTag, discordString);
        }

        if (getChildren().contains(TelegramModule.class)) {
            injector.getInstance(TelegramModule.class).sendMessage(sender, messageTag, discordString);
        }
    }

    @Override
    public boolean isVanished(FEntity sender) {
        if (getChildren().contains(SuperVanishModule.class)) {
            return injector.getInstance(SuperVanishModule.class).isVanished(sender);
        }

        Player player = Bukkit.getPlayer(sender.getUuid());
        if (player == null) return false;

        return player.getMetadata("vanished")
                .stream()
                .anyMatch(MetadataValue::asBoolean);
    }

    @Override
    public boolean isOtherTAB() {
        if (getChildren().contains(TABModule.class)) {
            return injector.getInstance(TABModule.class).isHooked();
        }

        return false;
    }

    @Override
    public String getTritonLocale(FPlayer fPlayer) {
        if (!isEnable()) return null;
        if (!getChildren().contains(TritonModule.class)) return null;

        return injector.getInstance(TritonModule.class).getLocale(fPlayer);
    }

    public boolean sendMessageWithInteractiveChat(FEntity fReceiver, Component message) {
        if (checkModulePredicates(fReceiver)) return false;

        if (getChildren().contains(InteractiveChatModule.class)) {
            return injector.getInstance(InteractiveChatModule.class).sendMessage(fReceiver, message);
        }

        return false;
    }
}
