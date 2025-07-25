package net.flectone.pulse.module.command.ping;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.flectone.pulse.adapter.PlatformPlayerAdapter;
import net.flectone.pulse.configuration.Command;
import net.flectone.pulse.configuration.Localization;
import net.flectone.pulse.configuration.Permission;
import net.flectone.pulse.resolver.FileResolver;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.module.AbstractModuleCommand;
import net.flectone.pulse.module.integration.IntegrationModule;
import net.flectone.pulse.registry.CommandRegistry;
import net.flectone.pulse.service.FPlayerService;
import org.incendo.cloud.context.CommandContext;
import org.incendo.cloud.meta.CommandMeta;

import java.util.Optional;

@Singleton
public class PingModule extends AbstractModuleCommand<Localization.Command.Ping> {

    private final Command.Ping command;
    private final Permission.Command.Ping permission;
    private final FPlayerService fPlayerService;
    private final CommandRegistry commandRegistry;
    private final IntegrationModule integrationModule;
    private final PlatformPlayerAdapter platformPlayerAdapter;

    @Inject
    public PingModule(FileResolver fileResolver,
                      FPlayerService fPlayerService,
                      CommandRegistry commandRegistry,
                      IntegrationModule integrationModule,
                      PlatformPlayerAdapter platformPlayerAdapter) {
        super(localization -> localization.getCommand().getPing(), null);

        this.command = fileResolver.getCommand().getPing();
        this.permission = fileResolver.getPermission().getCommand().getPing();
        this.fPlayerService = fPlayerService;
        this.commandRegistry = commandRegistry;
        this.integrationModule = integrationModule;
        this.platformPlayerAdapter = platformPlayerAdapter;
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

        String commandName = getName(command);
        String promptPlayer = getPrompt().getPlayer();
        commandRegistry.registerCommand(manager ->
                manager.commandBuilder(commandName, command.getAliases(), CommandMeta.empty())
                        .permission(permission.getName())
                        .optional(promptPlayer, commandRegistry.playerParser())
                        .handler(this)
        );

        addPredicate(this::checkCooldown);
    }

    @Override
    public void execute(FPlayer fPlayer, CommandContext<FPlayer> commandContext) {
        if (checkModulePredicates(fPlayer)) return;

        String promptPlayer = getPrompt().getPlayer();
        Optional<String> optionalTarget = commandContext.optional(promptPlayer);

        FPlayer fTarget = optionalTarget.isPresent() ? fPlayerService.getFPlayer(optionalTarget.get()) : fPlayer;
        if (!platformPlayerAdapter.isOnline(fTarget)
                || (!integrationModule.isVanishedVisible(fTarget, fPlayer) && !fPlayer.equals(fTarget))) {
            builder(fPlayer)
                    .format(Localization.Command.Ping::getNullPlayer)
                    .sendBuilt();
            return;
        }

        builder(fTarget)
                .receiver(fPlayer)
                .destination(command.getDestination())
                .format(Localization.Command.Ping::getFormat)
                .sound(getSound())
                .sendBuilt();
    }
}
