package net.flectone.pulse.module.message.format.moderation;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.flectone.pulse.file.Message;
import net.flectone.pulse.file.Permission;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.module.AbstractModule;
import net.flectone.pulse.module.message.format.moderation.caps.CapsModule;
import net.flectone.pulse.module.message.format.moderation.swear.SwearModule;

@Singleton
public class ModerationModule extends AbstractModule {

    private final Message.Format.Moderation message;
    private final Permission.Message.Format.Moderation permission;

    @Inject
    public ModerationModule(FileManager fileManager) {
        message = fileManager.getMessage().getFormat().getModeration();
        permission = fileManager.getPermission().getMessage().getFormat().getModeration();
    }

    @Override
    public void reload() {
        registerModulePermission(permission);

        addChildren(CapsModule.class);
        addChildren(SwearModule.class);
    }

    @Override
    public boolean isConfigEnable() {
        return message.isEnable();
    }

}
