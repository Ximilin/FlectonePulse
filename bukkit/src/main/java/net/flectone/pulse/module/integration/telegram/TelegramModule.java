package net.flectone.pulse.module.integration.telegram;

import com.google.inject.Inject;
import com.google.inject.Injector;
import com.google.inject.Singleton;
import net.flectone.pulse.BuildConfig;
import net.flectone.pulse.BukkitFlectonePulse;
import net.flectone.pulse.file.Integration;
import net.flectone.pulse.file.Permission;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.model.FEntity;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.module.AbstractModule;
import net.flectone.pulse.platform.DependencyResolver;
import net.flectone.pulse.util.MessageTag;

import java.util.function.UnaryOperator;

@Singleton
public class TelegramModule extends AbstractModule {

    private final Integration.Telegram integration;
    private final Permission.Integration.Telegram permission;

    private final BukkitFlectonePulse bukkitFlectonePulse;
    private final Injector injector;

    private boolean isLoaded;

    @Inject
    public TelegramModule(FileManager fileManager,
                          BukkitFlectonePulse bukkitFlectonePulse,
                          Injector injector) {
        this.bukkitFlectonePulse = bukkitFlectonePulse;
        this.injector = injector;

        integration = fileManager.getIntegration().getTelegram();
        permission = fileManager.getPermission().getIntegration().getTelegram();

        addPredicate(fEntity -> fEntity instanceof FPlayer fPlayer && !fPlayer.is(FPlayer.Setting.TELEGRAM));
    }

    @Override
    public void reload() {
        registerModulePermission(permission);

        disconnect();

        loadLibraries();

        injector.getInstance(TelegramIntegration.class).hook();
    }

    @Override
    public boolean isConfigEnable() {
        return integration.isEnable();
    }

    public void loadLibraries() {
        if (isLoaded) return;
        DependencyResolver dependencyResolver = bukkitFlectonePulse.getDependencyResolver();

        dependencyResolver.getLibraryManager().loadLibrary(dependencyResolver.buildLibrary(
                "org.telegram", "telegrambots-longpolling", BuildConfig.TELEGRAMBOTS_VERSION, true, null, null)
                .build()
        );
        dependencyResolver.getLibraryManager().loadLibrary(dependencyResolver.buildLibrary(
                "org.telegram", "telegrambots-client", BuildConfig.TELEGRAMBOTS_VERSION,  true, null, null)
                .build()
        );
        dependencyResolver.getLibraryManager().loadLibrary(dependencyResolver.buildLibrary(
                "com.squareup.okhttp3", "okhttp", "5.0.0-alpha.14",  true, null, null)
                .build()
        );

        isLoaded = true;
    }

    public void sendMessage(FEntity sender, MessageTag messageTag, UnaryOperator<String> telegramString) {
        if (checkModulePredicates(sender)) return;

        injector.getInstance(TelegramIntegration.class).sendMessage(sender, messageTag, telegramString);
    }

    public void disconnect() {
        if (!isLoaded) return;

        injector.getInstance(TelegramIntegration.class).disconnect();
    }
}
