package net.flectone.pulse.module.command.mail;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.arguments.GreedyStringArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.flectone.pulse.database.dao.FPlayerDAO;
import net.flectone.pulse.database.dao.IgnoreDAO;
import net.flectone.pulse.database.dao.MailDAO;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.module.command.FCommand;
import net.flectone.pulse.module.command.tell.TellModule;
import net.flectone.pulse.module.integration.IntegrationModule;
import net.flectone.pulse.util.BukkitCommandUtil;

@Singleton
public class BukkitMailModule extends MailModule {

    private final BukkitCommandUtil commandUtil;

    @Inject
    public BukkitMailModule(FileManager fileManager,
                            TellModule tellModule,
                            IntegrationModule integrationModule,
                            FPlayerDAO fPlayerDAO,
                            IgnoreDAO ignoreDAO,
                            MailDAO mailDAO,
                            BukkitCommandUtil commandUtil) {
        super(fileManager, tellModule, integrationModule, fPlayerDAO, ignoreDAO, mailDAO, commandUtil);

        this.commandUtil = commandUtil;
    }

    @Override
    public void createCommand() {
        String promptPlayer = getPrompt().getPlayer();
        String promptMessage = getPrompt().getMessage();

        new FCommand(getName(getCommand()))
                .withAliases(getCommand().getAliases())
                .withPermission(getPermission())
                .then(new StringArgument(promptPlayer)
                        .includeSuggestions(commandUtil.argumentFPlayers(true))
                        .then(new GreedyStringArgument(promptMessage)
                                .executesPlayer(this::executesFPlayer))
                )
                .override();
    }
}
