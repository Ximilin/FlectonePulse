package net.flectone.pulse.module.command.maintenance;

import com.github.retrooper.packetevents.protocol.player.User;
import com.github.retrooper.packetevents.wrapper.status.server.WrapperStatusServerResponse;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import com.google.inject.name.Named;
import lombok.Getter;
import net.flectone.pulse.checker.PermissionChecker;
import net.flectone.pulse.configuration.Command;
import net.flectone.pulse.configuration.Localization;
import net.flectone.pulse.configuration.Permission;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.model.event.Event;
import net.flectone.pulse.model.event.player.PlayerPreLoginEvent;
import net.flectone.pulse.module.AbstractModuleCommand;
import net.flectone.pulse.module.command.maintenance.listener.MaintenancePacketListener;
import net.flectone.pulse.pipeline.MessagePipeline;
import net.flectone.pulse.registry.CommandRegistry;
import net.flectone.pulse.registry.EventProcessRegistry;
import net.flectone.pulse.registry.ListenerRegistry;
import net.flectone.pulse.resolver.FileResolver;
import net.flectone.pulse.sender.PacketSender;
import net.flectone.pulse.service.FPlayerService;
import net.flectone.pulse.util.FileUtil;
import net.kyori.adventure.text.Component;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.meta.CommandMeta;

import java.io.File;
import java.nio.file.Path;

@Singleton
public class MaintenanceModule extends AbstractModuleCommand<Localization.Command.Maintenance> {

    @Getter private final Command.Maintenance command;
    private final Permission.Command.Maintenance permission;
    private final FileResolver fileResolver;
    private final FPlayerService fPlayerService;
    private final PermissionChecker permissionChecker;
    private final ListenerRegistry listenerRegistry;
    private final Path iconPath;
    private final FileUtil fileUtil;
    private final MessagePipeline messagePipeline;
    private final CommandRegistry commandRegistry;
    private final PacketSender packetSender;
    private final EventProcessRegistry eventProcessRegistry;

    private String icon;

    @Inject
    public MaintenanceModule(FileResolver fileResolver,
                             FPlayerService fPlayerService,
                             PermissionChecker permissionChecker,
                             ListenerRegistry listenerRegistry,
                             @Named("projectPath") Path projectPath,
                             FileUtil fileUtil,
                             CommandRegistry commandRegistry,
                             MessagePipeline messagePipeline,
                             PacketSender packetSender,
                             EventProcessRegistry eventProcessRegistry) {
        super(module -> module.getCommand().getMaintenance(), null);

        this.command = fileResolver.getCommand().getMaintenance();
        this.permission = fileResolver.getPermission().getCommand().getMaintenance();
        this.fileResolver = fileResolver;
        this.fPlayerService = fPlayerService;
        this.permissionChecker = permissionChecker;
        this.commandRegistry = commandRegistry;
        this.listenerRegistry = listenerRegistry;
        this.iconPath = projectPath.resolve("images");
        this.fileUtil = fileUtil;
        this.messagePipeline = messagePipeline;
        this.packetSender = packetSender;
        this.eventProcessRegistry = eventProcessRegistry;
    }

    @Override
    protected boolean isConfigEnable() {
        return command.isEnable();
    }

    @Override
    public void onEnable() {
        registerModulePermission(permission);

        createCooldown(command.getCooldown(), permission.getCooldownBypass());
        createSound(command.getSound(), permission.getSound());

        registerPermission(permission.getJoin());

        listenerRegistry.register(MaintenancePacketListener.class);

        File file = new File(iconPath.toString() + File.separator + "maintenance.png");

        if (!file.exists()) {
            fileUtil.saveResource("images" + File.separator + "maintenance.png");
        }

        icon = fileUtil.convertIcon(file);

        if (command.isTurnedOn()) {
            kickOnlinePlayers(FPlayer.UNKNOWN);
        }

        String commandName = getName(command);
        commandRegistry.registerCommand(manager ->
                manager.commandBuilder(commandName, command.getAliases(), CommandMeta.empty())
                        .permission(permission.getName())
                        .handler(this)
        );

        addPredicate(this::checkCooldown);

        eventProcessRegistry.registerHandler(Event.Type.PLAYER_PRE_LOGIN, event -> {
            PlayerPreLoginEvent playerPreLoginEvent = (PlayerPreLoginEvent) event;
            FPlayer fPlayer = playerPreLoginEvent.getPlayer();

            if (isAllowed(fPlayer)) return;

            playerPreLoginEvent.setAllowed(false);

            fPlayerService.loadSettings(fPlayer);
            fPlayerService.loadColors(fPlayer);

            String reasonMessage = resolveLocalization(fPlayer).getKick();
            Component reason = messagePipeline.builder(fPlayer, reasonMessage).build();

            playerPreLoginEvent.setKickReason(reason);
        });
    }

    @Override
    public void execute(FPlayer fPlayer, CommandContext<FPlayer> commandContext) {
        if (checkModulePredicates(fPlayer)) return;

        boolean turned = !command.isTurnedOn();

        command.setTurnedOn(turned);
        fileResolver.save();

        builder(fPlayer)
                .destination(command.getDestination())
                .format(s -> turned ? s.getFormatTrue() : s.getFormatFalse())
                .sendBuilt();

        if (turned) {
            kickOnlinePlayers(fPlayer);
        }
    }

    public void sendStatus(User user) {
        if (!isEnable()) return;
        if (!command.isTurnedOn()) return;

        FPlayer fPlayer = fPlayerService.getFPlayer(user.getAddress().getAddress());
        fPlayerService.loadColors(fPlayer);

        JsonObject responseJson = new JsonObject();

        Localization.Command.Maintenance localizationMaintenance = resolveLocalization(fPlayer);

        responseJson.add("version", getVersionJson(localizationMaintenance.getServerVersion()));
        responseJson.add("players", getPlayersJson());

        responseJson.add("description", messagePipeline.builder(fPlayer, localizationMaintenance.getServerDescription()).jsonSerializerBuild());
        responseJson.addProperty("favicon", "data:image/png;base64," + (icon == null ? "" : icon));
        responseJson.addProperty("enforcesSecureChat", false);

        WrapperStatusServerResponse wrapperStatusServerResponse = new WrapperStatusServerResponse(responseJson);
        user.sendPacket(wrapperStatusServerResponse);
    }

    public boolean isAllowed(FPlayer fPlayer) {
        if (!isEnable()) return true;
        if (!command.isTurnedOn()) return true;

        return permissionChecker.check(fPlayer, permission.getJoin());
    }

    private JsonElement getVersionJson(String message) {
        JsonObject jsonObject = new JsonObject();

        jsonObject.addProperty("name", message);
        jsonObject.addProperty("protocol", -1);

        return jsonObject;
    }

    private JsonElement getPlayersJson() {
        JsonObject playersJson = new JsonObject();

        playersJson.addProperty("max", -1);
        playersJson.addProperty("online", -1);

        playersJson.add("sample", new JsonArray());

        return playersJson;
    }

    private void kickOnlinePlayers(FPlayer fSender) {
        fPlayerService.getFPlayers()
                .stream()
                .filter(filter -> !permissionChecker.check(filter, permission.getJoin()))
                .forEach(fReceiver -> {
                    Component component = messagePipeline.builder(fSender, fReceiver, resolveLocalization(fReceiver).getKick()).build();
                    fPlayerService.kick(fReceiver, component);
                });
    }
}
