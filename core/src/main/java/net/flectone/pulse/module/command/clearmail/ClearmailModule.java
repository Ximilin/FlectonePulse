package net.flectone.pulse.module.command.clearmail;

import com.google.inject.Inject;
import lombok.Getter;
import net.flectone.pulse.database.Database;
import net.flectone.pulse.file.Command;
import net.flectone.pulse.file.Localization;
import net.flectone.pulse.file.Permission;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.module.AbstractModuleCommand;
import net.flectone.pulse.module.command.mail.model.Mail;
import net.flectone.pulse.util.CommandUtil;

import java.sql.SQLException;
import java.util.Optional;

public abstract class ClearmailModule extends AbstractModuleCommand<Localization.Command.Clearmail> {

    @Getter
    private final Command.Clearmail command;
    @Getter
    private final Permission.Command.Clearmail permission;

    private final CommandUtil commandUtil;

    @Inject
    public ClearmailModule(FileManager fileManager,
                           CommandUtil commandUtil) {
        super(localization -> localization.getCommand().getClearmail(), null);

        this.commandUtil = commandUtil;

        command = fileManager.getCommand().getClearmail();
        permission = fileManager.getPermission().getCommand().getClearmail();

        addPredicate(this::checkCooldown);
    }

    @Override
    public void onCommand(Database database, FPlayer fPlayer, Object arguments) throws SQLException {
        if (checkModulePredicates(fPlayer)) return;

        int mailID = commandUtil.getInteger(0, arguments);
        Optional<Mail> optionalMail = database.getMails(fPlayer)
                .stream()
                .filter(mail -> mail.id() == mailID)
                .findAny();

        if (optionalMail.isEmpty()) {
            builder(fPlayer)
                    .format(Localization.Command.Clearmail::getNullMail)
                    .sendBuilt();
            return;
        }

        FPlayer fReceiver = database.getFPlayer(optionalMail.get().receiver());

        database.removeMail(optionalMail.get());

        builder(fReceiver)
                .receiver(fPlayer)
                .format(s -> s.getFormat().replaceFirst("<id>", String.valueOf(mailID)))
                .message(optionalMail.get().message())
                .sound(getSound())
                .sendBuilt();
    }

    @Override
    public void reload() {
        registerModulePermission(permission);

        createCooldown(command.getCooldown(), permission.getCooldownBypass());
        createSound(command.getSound(), permission.getSound());

        createCommand();
    }

    @Override
    public boolean isConfigEnable() {
        return command.isEnable();
    }
}
