package net.flectone.pulse.listener;

import com.github.retrooper.packetevents.event.PacketListener;
import com.github.retrooper.packetevents.event.PacketReceiveEvent;
import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.protocol.packettype.PacketTypeCommon;
import com.github.retrooper.packetevents.protocol.player.UserProfile;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerDisconnect;
import com.github.retrooper.packetevents.wrapper.login.server.WrapperLoginServerLoginSuccess;
import com.github.retrooper.packetevents.wrapper.play.client.WrapperPlayClientSettings;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerChatMessage;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.google.inject.Inject;
import com.google.inject.Provider;
import com.google.inject.Singleton;
import net.flectone.pulse.annotation.Async;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.model.event.message.TranslatableMessageEvent;
import net.flectone.pulse.module.integration.IntegrationModule;
import net.flectone.pulse.processor.PlayerPreLoginProcessor;
import net.flectone.pulse.provider.PacketProvider;
import net.flectone.pulse.registry.EventProcessRegistry;
import net.flectone.pulse.sender.PacketSender;
import net.flectone.pulse.service.FPlayerService;
import net.flectone.pulse.util.MinecraftTranslationKeys;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TranslatableComponent;

import java.util.UUID;

@Singleton
public class BasePacketListener implements PacketListener {

    private final FPlayerService fPlayerService;
    private final Provider<IntegrationModule> integrationModuleProvider;
    private final EventProcessRegistry eventProcessRegistry;
    private final PacketProvider packetProvider;
    private final PacketSender packetSender;
    private final PlayerPreLoginProcessor playerPreLoginProcessor;

    @Inject
    public BasePacketListener(FPlayerService fPlayerService,
                              Provider<IntegrationModule> integrationModuleProvider,
                              EventProcessRegistry eventProcessRegistry,
                              PacketProvider packetProvider,
                              PacketSender packetSender,
                              PlayerPreLoginProcessor playerPreLoginProcessor) {
        this.fPlayerService = fPlayerService;
        this.integrationModuleProvider = integrationModuleProvider;
        this.eventProcessRegistry = eventProcessRegistry;
        this.packetProvider = packetProvider;
        this.packetSender = packetSender;
        this.playerPreLoginProcessor = playerPreLoginProcessor;
    }

    @Override
    public void onPacketReceive(PacketReceiveEvent event) {
        PacketTypeCommon packetType = event.getPacketType();

        if (packetType != PacketType.Play.Client.CLIENT_SETTINGS
                && packetType != PacketType.Configuration.Client.CLIENT_SETTINGS) return;

        UUID uuid = event.getUser().getUUID();
        if (uuid == null) return;

        FPlayer fPlayer = fPlayerService.getFPlayer(uuid);

        String locale = getLocale(fPlayer, event);

        if (locale.equals(fPlayer.getSettingValue(FPlayer.Setting.LOCALE))) return;
        if (!fPlayer.isUnknown()) {
            fPlayerService.saveOrUpdateSetting(fPlayer, FPlayer.Setting.LOCALE, locale);
            return;
        }

        // first time player joined, wait for it to be added
        updateLocaleLater(uuid, locale);
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.isCancelled()) return;

        // only for 1.20.2 and newer versions
        // because there is a configuration stage and there are no problems with evet.setСancelled(True)
        if (event.getPacketType() == PacketType.Login.Server.LOGIN_SUCCESS
                && packetProvider.getServerVersion().isNewerThanOrEquals(ServerVersion.V_1_20_2)) {

            WrapperLoginServerLoginSuccess wrapper = new WrapperLoginServerLoginSuccess(event);
            UserProfile userProfile = wrapper.getUserProfile();

            UUID uuid = userProfile.getUUID();
            if (uuid == null) return;

            String playerName = userProfile.getName();
            if (playerName == null) return;

            event.setCancelled(true);

            playerPreLoginProcessor.processAsyncLogin(uuid, playerName,
                    loginEvent -> packetSender.send(uuid, new WrapperLoginServerLoginSuccess(uuid, playerName)),
                    loginEvent -> packetSender.send(uuid, new WrapperLoginServerDisconnect(loginEvent.getKickReason()))
            );
        }

        TranslatableComponent translatableComponent = parseTranslatableComponent(event);
        if (translatableComponent == null) return;

        MinecraftTranslationKeys key = MinecraftTranslationKeys.fromString(translatableComponent.key());

        // skip minecraft warning
        if (key == MinecraftTranslationKeys.MULTIPLAYER_MESSAGE_NOT_DELIVERED) {
            event.setCancelled(true);
            return;
        }

        eventProcessRegistry.processEvent(new TranslatableMessageEvent(key, translatableComponent, event));
    }

    private TranslatableComponent parseTranslatableComponent(PacketSendEvent event) {
        Component component = null;

        if (event.getPacketType() == PacketType.Play.Server.CHAT_MESSAGE) {
            WrapperPlayServerChatMessage wrapper = new WrapperPlayServerChatMessage(event);
            component = wrapper.getMessage().getChatContent();
        } else if (event.getPacketType() == PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) {
            WrapperPlayServerSystemChatMessage wrapper = new WrapperPlayServerSystemChatMessage(event);
            component = wrapper.getMessage();
        }

        if (component instanceof TranslatableComponent translatableComponent) {
            return translatableComponent;
        }

        return null;
    }

    @Async(delay = 40L)
    public void updateLocaleLater(UUID uuid, String locale) {
        FPlayer newFPlayer = fPlayerService.getFPlayer(uuid);
        fPlayerService.saveOrUpdateSetting(newFPlayer, FPlayer.Setting.LOCALE, locale);
    }

    private String getLocale(FPlayer fPlayer, PacketReceiveEvent event) {
        String locale = integrationModuleProvider.get().getTritonLocale(fPlayer);
        if (locale == null) {
            WrapperPlayClientSettings wrapperPlayClientSettings = new WrapperPlayClientSettings(event);
            locale = wrapperPlayClientSettings.getLocale();
        }

        return locale;
    }
}
