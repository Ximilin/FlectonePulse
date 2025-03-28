package net.flectone.pulse.module.message.tab.playerlist;

import com.github.retrooper.packetevents.PacketEvents;
import com.github.retrooper.packetevents.manager.server.ServerVersion;
import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfo;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerPlayerInfoUpdate;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.flectone.pulse.annotation.Async;
import net.flectone.pulse.config.Localization;
import net.flectone.pulse.config.Message;
import net.flectone.pulse.config.Permission;
import net.flectone.pulse.manager.FPlayerManager;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.model.Ticker;
import net.flectone.pulse.module.AbstractModuleMessage;
import net.flectone.pulse.scheduler.TaskScheduler;
import net.flectone.pulse.util.ComponentUtil;
import net.flectone.pulse.util.PacketEventsUtil;
import net.kyori.adventure.text.Component;

@Singleton
public class PlayerlistnameModule extends AbstractModuleMessage<Localization.Message.Tab.Playerlistname> {

    private final Message.Tab.Playerlistname message;
    private final Permission.Message.Tab.Playerlistname permission;

    private final FPlayerManager fPlayerManager;
    private final ComponentUtil componentUtil;
    private final PacketEventsUtil packetEventsUtil;
    private final TaskScheduler taskScheduler;

    @Inject
    public PlayerlistnameModule(FPlayerManager fPlayerManager,
                                FileManager fileManager,
                                ComponentUtil componentUtil,
                                PacketEventsUtil packetEventsUtil,
                                TaskScheduler taskScheduler) {
        super(module -> module.getMessage().getTab().getPlayerlistname());

        this.fPlayerManager = fPlayerManager;
        this.componentUtil = componentUtil;
        this.packetEventsUtil = packetEventsUtil;
        this.taskScheduler = taskScheduler;

        message = fileManager.getMessage().getTab().getPlayerlistname();
        permission = fileManager.getPermission().getMessage().getTab().getPlayerlistname();
    }

    @Override
    public void reload() {
        registerModulePermission(permission);

        Ticker ticker = message.getTicker();
        if (ticker.isEnable()) {
            taskScheduler.runAsyncTicker(this::send, ticker.getPeriod());
        }
    }

    @Async
    public void update() {
        if (!isEnable()) return;

        fPlayerManager.getFPlayers().stream().filter(FPlayer::isOnline).forEach(this::send);
    }

    public void send(FPlayer fPlayer) {
        if (checkModulePredicates(fPlayer)) return;

        fPlayerManager.getFPlayers().stream()
                .filter(FPlayer::isOnline)
                .forEach(fReceiver -> updatePlayerlistname(fPlayer, fReceiver));
    }

    @Override
    public boolean isConfigEnable() {
        return message.isEnable();
    }

    private void updatePlayerlistname(FPlayer fPlayer, FPlayer fReceiver) {
        User user = packetEventsUtil.getUser(fPlayer);
        if (user == null) return;

        Component name = componentUtil.builder(fPlayer, fReceiver, resolveLocalization(fReceiver).getFormat())
                .userMessage(false)
                .build();

        if (PacketEvents.getAPI().getServerManager().getVersion().isNewerThanOrEquals(ServerVersion.V_1_19_4)) {
            WrapperPlayServerPlayerInfoUpdate.PlayerInfo playerInfo = new WrapperPlayServerPlayerInfoUpdate.PlayerInfo(
                    user.getProfile(),
                    true,
                    fPlayerManager.getPing(fPlayer),
                    fPlayerManager.getGamemode(fPlayer),
                    name,
                    null
            );

            packetEventsUtil.sendPacket(fReceiver, new WrapperPlayServerPlayerInfoUpdate(WrapperPlayServerPlayerInfoUpdate.Action.UPDATE_DISPLAY_NAME, playerInfo));
            return;
        }


        WrapperPlayServerPlayerInfo.PlayerData playerData = new WrapperPlayServerPlayerInfo.PlayerData(
                name,
                user.getProfile(),
                fPlayerManager.getGamemode(fPlayer),
                fPlayerManager.getPing(fPlayer)
        );

        packetEventsUtil.sendPacket(fReceiver, new WrapperPlayServerPlayerInfo(WrapperPlayServerPlayerInfo.Action.UPDATE_DISPLAY_NAME, playerData));
    }
}
