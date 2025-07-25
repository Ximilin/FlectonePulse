package net.flectone.pulse.module.command.unwarn;

import com.google.gson.Gson;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import net.flectone.pulse.configuration.Command;
import net.flectone.pulse.configuration.Localization;
import net.flectone.pulse.configuration.Permission;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.model.Moderation;
import net.flectone.pulse.module.AbstractModuleCommand;
import net.flectone.pulse.registry.CommandRegistry;
import net.flectone.pulse.resolver.FileResolver;
import net.flectone.pulse.sender.ProxySender;
import net.flectone.pulse.service.FPlayerService;
import net.flectone.pulse.service.ModerationService;
import net.flectone.pulse.util.MessageTag;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.meta.CommandMeta;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@Singleton
public class UnwarnModule extends AbstractModuleCommand<Localization.Command.Unwarn> {

    @Getter private final Command.Unwarn command;
    private final Permission.Command.Unwarn permission;
    private final FPlayerService fPlayerService;
    private final ModerationService moderationService;
    private final CommandRegistry commandRegistry;
    private final ProxySender proxySender;
    private final Gson gson;

    @Inject
    public UnwarnModule(FileResolver fileResolver,
                        FPlayerService fPlayerService,
                        ModerationService moderationService,
                        CommandRegistry commandRegistry,
                        ProxySender proxySender,
                        Gson gson) {
        super(localization -> localization.getCommand().getUnwarn(), null);

        this.command = fileResolver.getCommand().getUnwarn();
        this.permission = fileResolver.getPermission().getCommand().getUnwarn();
        this.fPlayerService = fPlayerService;
        this.moderationService = moderationService;
        this.commandRegistry = commandRegistry;
        this.proxySender = proxySender;
        this.gson = gson;
    }

    @Override
    protected boolean isConfigEnable() {
        return command.isEnable();
    }

    @Override
    public void onEnable() {
        // if FPlayer.UNKNOWN (all-permissions) fails check (method will return true),
        // a moderation plugin is intercepting this command
        if (checkModulePredicates(FPlayer.UNKNOWN)) return;

        registerModulePermission(permission);

        createCooldown(command.getCooldown(), permission.getCooldownBypass());
        createSound(command.getSound(), permission.getSound());

        String commandName = getName(command);
        String promptPlayer = getPrompt().getPlayer();
        String promptId = getPrompt().getId();
        commandRegistry.registerCommand(manager ->
                manager.commandBuilder(commandName, command.getAliases(), CommandMeta.empty())
                        .permission(permission.getName())
                        .required(promptPlayer, commandRegistry.warnedParser())
                        .optional(promptId, commandRegistry.integerParser())
                        .handler(this)
        );
    }

    @Override
    public void execute(FPlayer fPlayer, CommandContext<FPlayer> commandContext) {
        if (checkModulePredicates(fPlayer)) return;
        if (checkCooldown(fPlayer)) return;

        String promptPlayer = getPrompt().getPlayer();
        String target = commandContext.get(promptPlayer);

        String promptId = getPrompt().getId();
        Optional<Integer> optionalId = commandContext.optional(promptId);
        int id = optionalId.orElse(-1);

        unwarn(fPlayer, target, id);
    }

    public void unwarn(FPlayer fPlayer, String target, int id) {
        if (checkModulePredicates(fPlayer)) return;

        FPlayer fTarget = fPlayerService.getFPlayer(target);
        if (fTarget.isUnknown()) {
            builder(fPlayer)
                    .format(Localization.Command.Unwarn::getNullPlayer)
                    .sendBuilt();
            return;
        }

        List<Moderation> warns = new ArrayList<>();

        if (id == -1) {
            warns.addAll(moderationService.getValidWarns(fTarget));
        } else {
            moderationService.getValidWarns(fTarget).stream()
                    .filter(warn -> warn.getId() == id)
                    .findAny()
                    .ifPresent(warns::add);
        }

        if (warns.isEmpty()) {
            builder(fPlayer)
                    .format(Localization.Command.Unwarn::getNotWarned)
                    .sendBuilt();
            return;
        }

        moderationService.remove(fTarget, warns);

        proxySender.send(fTarget, MessageTag.SYSTEM_WARN, dataOutputStream -> {});

        builder(fTarget)
                .tag(MessageTag.COMMAND_UNWARN)
                .destination(command.getDestination())
                .range(command.getRange())
                .filter(filter -> filter.isSetting(FPlayer.Setting.WARN))
                .format(unwarn -> unwarn.getFormat().replace("<moderator>", fPlayer.getName()))
                .proxy(output -> output.writeUTF(gson.toJson(fPlayer)))
                .integration(s -> s.replace("<moderator>", fPlayer.getName()))
                .sound(getSound())
                .sendBuilt();
    }
}
