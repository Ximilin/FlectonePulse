package net.flectone.pulse.module.integration.minimotd;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.flectone.pulse.configuration.Integration;
import net.flectone.pulse.configuration.Permission;
import net.flectone.pulse.module.AbstractModule;
import net.flectone.pulse.module.message.status.StatusModule;
import net.flectone.pulse.resolver.FileResolver;

@Singleton
public class MiniMOTDModule extends AbstractModule {

    private final Integration.MiniMOTD integration;
    private final Permission.Integration.MiniMOTD permission;
    private final MiniMOTDIntegration miniMOTDIntegration;
    private final StatusModule statusModule;

    @Inject
    public MiniMOTDModule(FileResolver fileResolver,
                          MiniMOTDIntegration miniMOTDIntegration,
                          StatusModule statusModule) {
        this.integration = fileResolver.getIntegration().getMinimotd();
        this.permission = fileResolver.getPermission().getIntegration().getMinimotd();
        this.miniMOTDIntegration = miniMOTDIntegration;
        this.statusModule = statusModule;
    }

    @Override
    public void onEnable() {
        registerModulePermission(permission);

        miniMOTDIntegration.hook();

        statusModule.addPredicate(fPlayer -> integration.isDisableFlectonepulseStatus() && isHooked());
    }

    @Override
    public void onDisable() {
        miniMOTDIntegration.unhook();
    }

    @Override
    protected boolean isConfigEnable() {
        return integration.isEnable();
    }

    public boolean isHooked() {
        return miniMOTDIntegration.isHooked();
    }
}