package net.flectone.pulse.module.command.chatcolor;

import lombok.Getter;
import net.flectone.pulse.database.dao.ColorsDAO;
import net.flectone.pulse.database.dao.FPlayerDAO;
import net.flectone.pulse.config.Command;
import net.flectone.pulse.config.Localization;
import net.flectone.pulse.config.Message;
import net.flectone.pulse.config.Permission;
import net.flectone.pulse.manager.FPlayerManager;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.connector.ProxyConnector;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.module.AbstractModuleCommand;
import net.flectone.pulse.util.ColorUtil;
import net.flectone.pulse.util.CommandUtil;
import net.flectone.pulse.util.MessageTag;
import net.flectone.pulse.util.PermissionUtil;

import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;

public abstract class ChatcolorModule extends AbstractModuleCommand<Localization.Command.Chatcolor> {

    @Getter private final Message.Format.Color color;
    @Getter private final Command.Chatcolor command;
    @Getter private final Permission.Command.Chatcolor permission;

    private final FPlayerDAO fPlayerDAO;
    private final ColorsDAO colorsDAO;
    private final FPlayerManager fPlayerManager;
    private final PermissionUtil permissionUtil;
    private final ProxyConnector proxyConnector;
    private final CommandUtil commandUtil;
    private final ColorUtil colorUtil;

    public ChatcolorModule(FileManager fileManager,
                           FPlayerDAO fPlayerDAO,
                           ColorsDAO colorsDAO,
                           FPlayerManager fPlayerManager,
                           PermissionUtil permissionUtil,
                           ProxyConnector proxyConnector,
                           CommandUtil commandUtil,
                           ColorUtil colorUtil) {
        super(localization -> localization.getCommand().getChatcolor(), null);
        this.fPlayerDAO = fPlayerDAO;
        this.colorsDAO = colorsDAO;
        this.fPlayerManager = fPlayerManager;
        this.permissionUtil = permissionUtil;
        this.proxyConnector = proxyConnector;
        this.commandUtil = commandUtil;
        this.colorUtil = colorUtil;

        color = fileManager.getMessage().getFormat().getColor();
        command = fileManager.getCommand().getChatcolor();
        permission = fileManager.getPermission().getCommand().getChatcolor();

        addPredicate(this::checkCooldown);
    }

    @Override
    public void onCommand(FPlayer fPlayer, Object arguments) {
        if (checkModulePredicates(fPlayer)) return;

        String input = commandUtil.getString(0, arguments);

        if (permissionUtil.has(fPlayer, permission.getOther())) {
            String[] words = input.split(" ");

            String player = words[0];
            if (!player.startsWith("#")
                    && !player.startsWith("&")
                    && !player.equalsIgnoreCase("clear")) {

                FPlayer fTarget = fPlayerDAO.getFPlayer(player);

                if (fTarget.isUnknown()) {
                    builder(fPlayer)
                            .format(Localization.Command.Chatcolor::getNullPlayer)
                            .sendBuilt();
                    return;
                }

                colorsDAO.load(fTarget);

                proxyConnector.sendMessage(fTarget, MessageTag.COMMAND_CHATCOLOR, byteArrayDataOutput ->
                        byteArrayDataOutput.writeUTF(input)
                );

                if (words.length > 1) {
                    prepareInput(input.substring(player.length() + 1).trim(), fTarget);
                }

                return;
            }
        }

        prepareInput(input, fPlayer);
    }

    private void prepareInput(String input, FPlayer fPlayer) {
        Map<String, String> tagColorMap = color.getValues();

        if (input.equalsIgnoreCase("clear")) {
            fPlayer.getColors().clear();
            setColors(fPlayer, new HashMap<>(), null);
            return;
        }

        String[] colors = input.split(" ");

        if (colors.length != tagColorMap.size()
                || Arrays.stream(colors).anyMatch(color -> !color.startsWith("#")
                && !color.startsWith("&")
                && !colorUtil.getMinecraftList().contains(color)
                || color.startsWith("#") && color.length() != 7
                || color.startsWith("&") && color.length() != 2)) {

            builder(fPlayer)
                    .format(Localization.Command.Chatcolor::getNullColor)
                    .sendBuilt();
            return;
        }

        setColors(fPlayer, tagColorMap, colors);
    }

    private void setColors(FPlayer fPlayer, Map<String, String> tagColorMap, String[] colors) {
        int x = 0;
        for (Map.Entry<String, String> entry : tagColorMap.entrySet()) {

            colors[x] = colors[x].startsWith("&")
                    ? colorUtil.getLegacyHexMap().get(colors[x])
                    : !colors[x].startsWith("#")
                    ? colorUtil.getMinecraftHexMap().get(colors[x])
                    : colors[x];

            fPlayer.getColors().put(entry.getKey(), colors[x]);
            x++;
        }

        colorsDAO.save(fPlayer);

        FPlayer onlineFPlayer = fPlayerManager.get(fPlayer.getUuid());
        if (!onlineFPlayer.isUnknown()) {
            if (fPlayer.getColors().isEmpty()) {
                onlineFPlayer.getColors().clear();
            } else {
                onlineFPlayer.getColors().putAll(fPlayer.getColors());
            }
        }

        builder(fPlayer)
                .destination(command.getDestination())
                .format((fResolver, s) -> s.getFormat())
                .sound(getSound())
                .sendBuilt();
    }

    @Override
    public void reload() {
        registerModulePermission(permission);

        createCooldown(command.getCooldown(), permission.getCooldownBypass());
        createSound(command.getSound(), permission.getSound());

        registerPermission(permission.getOther());

        getCommand().getAliases().forEach(commandUtil::unregister);

        createCommand();
    }

    @Override
    public boolean isConfigEnable() {
        return command.isEnable();
    }
}
