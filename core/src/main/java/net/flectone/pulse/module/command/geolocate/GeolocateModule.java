package net.flectone.pulse.module.command.geolocate;

import lombok.Getter;
import net.flectone.pulse.config.Command;
import net.flectone.pulse.config.Localization;
import net.flectone.pulse.config.Permission;
import net.flectone.pulse.database.dao.FPlayerDAO;
import net.flectone.pulse.manager.FPlayerManager;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.module.AbstractModuleCommand;
import net.flectone.pulse.util.CommandUtil;
import net.flectone.pulse.util.DisableAction;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

public abstract class GeolocateModule extends AbstractModuleCommand<Localization.Command.Geolocate> {

    private final String HTTP_URL = "http://ip-api.com/line/<ip>?fields=17031449";

    @Getter private final Command.Geolocate command;
    @Getter private final Permission.Command.Geolocate permission;

    private final FPlayerDAO fPlayerDAO;
    private final FPlayerManager fPlayerManager;
    private final CommandUtil commandUtil;

    public GeolocateModule(FileManager fileManager,
                           FPlayerDAO fPlayerDAO,
                           FPlayerManager fPlayerManager,
                           CommandUtil commandUtil) {
        super(localization -> localization.getCommand().getGeolocate(), null);

        this.fPlayerDAO = fPlayerDAO;
        this.fPlayerManager = fPlayerManager;
        this.commandUtil = commandUtil;

        command = fileManager.getCommand().getGeolocate();
        permission = fileManager.getPermission().getCommand().getGeolocate();

        addPredicate(this::checkCooldown);
        addPredicate(fPlayer -> checkDisable(fPlayer, fPlayer, DisableAction.YOU));
    }

    @Override
    public void onCommand(FPlayer fPlayer, Object arguments) {
        if (checkModulePredicates(fPlayer)) return;

        String playerName = commandUtil.getString(0, arguments);

        FPlayer fTarget = fPlayerDAO.getFPlayer(playerName);

        if (fTarget.isUnknown()) {
            builder(fPlayer)
                    .format(Localization.Command.Geolocate::getNullPlayer)
                    .sendBuilt();
            return;
        }

        String ip = fTarget.isOnline() ? fPlayerManager.getIp(fTarget) : fTarget.getIp();

        List<String> request = ip == null ? List.of() : readResponse(HTTP_URL.replace("<ip>", ip));
        if (request.isEmpty() || request.get(0).equals("fail")) {
            builder(fPlayer)
                    .format(Localization.Command.Geolocate::getNullOrError)
                    .sendBuilt();
            return;
        }

        builder(fTarget)
                .destination(command.getDestination())
                .receiver(fPlayer)
                .format(s -> s.getFormat()
                        .replace("<country>", request.get(1))
                        .replace("<region_name>", request.get(2))
                        .replace("<city>", request.get(3))
                        .replace("<timezone>", request.get(4))
                        .replace("<mobile>", request.get(5))
                        .replace("<proxy>", request.get(6))
                        .replace("<hosting>", request.get(7))
                        .replace("<query>", request.get(8))
                )
                .sound(getSound())
                .sendBuilt();
    }

    private List<String> readResponse(String url) {
        List<String> arrayList = new ArrayList<>();

        try {
            BufferedReader reader = new BufferedReader(new InputStreamReader((new URL(url)).openStream()));

            String line;
            while((line = reader.readLine()) != null) {
                arrayList.add(line);
            }

            reader.close();
        } catch (IOException ignored) {}

        return arrayList;
    }

    @Override
    public void reload() {
        registerModulePermission(permission);

        createCooldown(command.getCooldown(), permission.getCooldownBypass());
        createSound(command.getSound(), permission.getSound());

        getCommand().getAliases().forEach(commandUtil::unregister);

        createCommand();
    }

    @Override
    public boolean isConfigEnable() {
        return command.isEnable();
    }
}
