package net.flectone.pulse.module.command.unmute;

import com.google.gson.Gson;
import lombok.Getter;
import net.flectone.pulse.config.Command;
import net.flectone.pulse.config.Localization;
import net.flectone.pulse.config.Permission;
import net.flectone.pulse.database.dao.FPlayerDAO;
import net.flectone.pulse.database.dao.ModerationDAO;
import net.flectone.pulse.manager.FPlayerManager;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.model.Moderation;
import net.flectone.pulse.module.AbstractModuleCommand;
import net.flectone.pulse.util.CommandUtil;
import net.flectone.pulse.util.MessageTag;

import java.util.ArrayList;
import java.util.List;

public abstract class UnmuteModule extends AbstractModuleCommand<Localization.Command.Unmute> {

    @Getter private final Command.Unmute command;
    @Getter private final Permission.Command.Unmute permission;

    private final FPlayerDAO fPlayerDAO;
    private final ModerationDAO moderationDAO;
    private final FPlayerManager fPlayerManager;
    private final CommandUtil commandUtil;
    private final Gson gson;

    public UnmuteModule(FileManager fileManager,
                        FPlayerDAO fPlayerDAO,
                        ModerationDAO moderationDAO,
                        FPlayerManager fPlayerManager,
                        CommandUtil commandUtil,
                        Gson gson) {
        super(localization -> localization.getCommand().getUnmute(), null);

        this.fPlayerDAO = fPlayerDAO;
        this.moderationDAO = moderationDAO;
        this.fPlayerManager = fPlayerManager;
        this.commandUtil = commandUtil;
        this.gson = gson;

        command = fileManager.getCommand().getUnmute();
        permission = fileManager.getPermission().getCommand().getUnmute();
    }

    @Override
    public void onCommand(FPlayer fPlayer, Object arguments) {
        if (checkModulePredicates(fPlayer)) return;
        if (checkCooldown(fPlayer)) return;

        String target = commandUtil.getString(0, arguments);
        int id = commandUtil.getByClassOrDefault(1, Integer.class, -1, arguments);

        unmute(fPlayer, target, id);
    }

    public void unmute(FPlayer fPlayer, String target, int id) {
        if (checkModulePredicates(fPlayer)) return;

        FPlayer fTarget = fPlayerDAO.getFPlayer(target);
        if (fTarget.isUnknown()) {
            builder(fPlayer)
                    .format(Localization.Command.Unmute::getNullPlayer)
                    .sendBuilt();
            return;
        }

        List<Moderation> mutes = new ArrayList<>();

        if (id == -1) {
            mutes.addAll(moderationDAO.getValid(fTarget, Moderation.Type.MUTE));
        } else {
            moderationDAO.getValid(fTarget, Moderation.Type.MUTE).stream()
                    .filter(moderation -> moderation.getId() == id)
                    .findAny()
                    .ifPresent(mutes::add);
        }

        if (mutes.isEmpty()) {
            builder(fPlayer)
                    .format(Localization.Command.Unmute::getNotMuted)
                    .sendBuilt();
            return;
        }

        for (Moderation mute : mutes) {
            moderationDAO.setInvalid(mute);
        }

        FPlayer localFTarget = fPlayerManager.get(fTarget.getUuid());

        if (!localFTarget.isUnknown()) {
            localFTarget.clearMutes(mutes);
        }

        builder(fTarget)
                .tag(MessageTag.COMMAND_UNMUTE)
                .destination(command.getDestination())
                .range(command.getRange())
                .filter(filter -> filter.isSetting(FPlayer.Setting.MUTE))
                .format(unmute -> unmute.getFormat().replace("<moderator>", fPlayer.getName()))
                .proxy(output -> output.writeUTF(gson.toJson(fPlayer)))
                .integration(s -> s.replace("<moderator>", fPlayer.getName()))
                .sound(getSound())
                .sendBuilt();
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
