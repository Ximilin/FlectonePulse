package net.flectone.pulse.module.integration.interactivechat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.flectone.pulse.config.Integration;
import net.flectone.pulse.config.Permission;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.model.FEntity;
import net.flectone.pulse.module.AbstractModule;
import net.kyori.adventure.text.Component;

@Singleton
public class InteractiveChatModule extends AbstractModule {

    private final Integration.Interactivechat integration;
    private final Permission.Integration.Interactivechat permission;
    private final InteractiveChatIntegration interactiveChatIntegration;

    @Inject
    public InteractiveChatModule(FileManager fileManager,
                                 InteractiveChatIntegration interactiveChatIntegration) {
        this.interactiveChatIntegration = interactiveChatIntegration;
        integration = fileManager.getIntegration().getInteractivechat();
        permission = fileManager.getPermission().getIntegration().getInteractivechat();
    }

    @Override
    public void reload() {
        registerModulePermission(permission);
        interactiveChatIntegration.hook();
    }

    @Override
    public boolean isConfigEnable() {
        return integration.isEnable();
    }


    public String checkMention(FEntity fSender, String message) {
        if (checkModulePredicates(fSender)) return message;

        return interactiveChatIntegration.checkMention(fSender, message);
    }

    public String markSender(FEntity fSender, String message) {
        if (checkModulePredicates(fSender)) return message;

        return interactiveChatIntegration.markSender(fSender, message);
    }

    public boolean sendMessage(FEntity fReceiver, Component message) {
        if (checkModulePredicates(fReceiver)) return false;

        return interactiveChatIntegration.sendMessage(fReceiver, message);
    }

}
