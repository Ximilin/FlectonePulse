package net.flectone.pulse.module.message.gamemode;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.flectone.pulse.annotation.Async;
import net.flectone.pulse.config.Localization;
import net.flectone.pulse.config.Message;
import net.flectone.pulse.config.Permission;
import net.flectone.pulse.manager.FPlayerManager;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.registry.ListenerRegistry;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.module.AbstractModuleMessage;
import net.flectone.pulse.module.message.gamemode.listener.GamemodePacketListener;

import java.util.UUID;

@Singleton
public class GamemodeModule extends AbstractModuleMessage<Localization.Message.Gamemode> {

    private final Message.Gamemode message;
    private final Permission.Message.Gamemode permission;

    private final FPlayerManager fPlayerManager;
    private final ListenerRegistry listenerRegistry;

    @Inject
    public GamemodeModule(FileManager fileManager,
                          FPlayerManager fPlayerManager,
                          ListenerRegistry listenerRegistry) {
        super(localization -> localization.getMessage().getGamemode());

        this.fPlayerManager = fPlayerManager;
        this.listenerRegistry = listenerRegistry;

        message = fileManager.getMessage().getGamemode();
        permission = fileManager.getPermission().getMessage().getGamemode();
    }

    @Override
    public void reload() {
        registerModulePermission(permission);

        createSound(message.getSound(), permission.getSound());

        listenerRegistry.register(GamemodePacketListener.class);
    }

    @Override
    public boolean isConfigEnable() {
        return message.isEnable();
    }

    @Async
    public void send(UUID receiver, String key, String target) {
        FPlayer fPlayer = fPlayerManager.get(receiver);
        if (checkModulePredicates(fPlayer)) return;

        FPlayer fTarget = fPlayerManager.getOnline(target);
        if (fTarget.isUnknown()) return;

        boolean isSelf = fPlayer.equals(fTarget);

        // for sender
        builder(fTarget)
                .destination(message.getDestination())
                .receiver(fPlayer)
                .format(s -> getString(key, isSelf ? s.getSelf() : s.getOther()))
                .sound(getSound())
                .sendBuilt();

        // for receiver
        if (!isSelf) {
            builder(fTarget)
                    .destination(message.getDestination())
                    .format(s -> getString(key, s.getSelf()))
                    .sound(getSound())
                    .sendBuilt();
        }
    }

    private String getString(String key, Localization.Message.Gamemode.Type type) {
        return switch (key.split("\\.")[1]) {
            case "creative" -> type.getCreative();
            case "survival" -> type.getSurvival();
            case "adventure" -> type.getAdventure();
            case "spectator" -> type.getSpectator();
            default -> "";
        };
    }
}
