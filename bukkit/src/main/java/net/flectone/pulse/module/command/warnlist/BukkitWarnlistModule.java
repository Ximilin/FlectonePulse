package net.flectone.pulse.module.command.warnlist;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import dev.jorel.commandapi.arguments.ArgumentSuggestions;
import dev.jorel.commandapi.arguments.IntegerArgument;
import dev.jorel.commandapi.arguments.StringArgument;
import net.flectone.pulse.database.Database;
import net.flectone.pulse.logger.FLogger;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.model.Moderation;
import net.flectone.pulse.module.command.FCommand;
import net.flectone.pulse.platform.MessageSender;
import net.flectone.pulse.util.CommandUtil;
import net.flectone.pulse.util.ComponentUtil;
import net.flectone.pulse.util.TimeUtil;

import java.sql.SQLException;
import java.util.ArrayList;
import java.util.concurrent.CompletableFuture;

@Singleton
public class BukkitWarnlistModule extends WarnlistModule {

    private final Database database;
    private final FLogger fLogger;

    @Inject
    public BukkitWarnlistModule(FileManager fileManager,
                                ComponentUtil componentUtil,
                                CommandUtil commandUtil,
                                TimeUtil timeUtil,
                                MessageSender messageSender,
                                Database database,
                                FLogger fLogger) {
        super(fileManager, componentUtil, commandUtil, timeUtil, messageSender);

        this.database = database;
        this.fLogger = fLogger;
    }

    @Override
    public void createCommand() {
        String name = getCommand().getAliases().get(0);
        String promptPlayer = getPrompt().getPlayer();
        String promptNumber = getPrompt().getNumber();

        new FCommand(name)
                .withAliases(getCommand().getAliases())
                .withPermission(getPermission())
                .then(new IntegerArgument(promptNumber).setOptional(true)
                        .executesPlayer(this::executesFPlayerDatabase)
                )
                .then(new StringArgument(promptPlayer)
                        .includeSuggestions(ArgumentSuggestions.stringCollectionAsync(info -> CompletableFuture.supplyAsync(() -> {
                            try {
                                return database.getModerationsNames(Moderation.Type.WARN);
                            } catch (SQLException e) {
                                fLogger.warning(e);
                            }

                            return new ArrayList<>();
                        })))
                        .then(new IntegerArgument(promptNumber).setOptional(true)
                                .executesPlayer(this::executesFPlayerDatabase)
                        )
                )
                .override();
    }
}
