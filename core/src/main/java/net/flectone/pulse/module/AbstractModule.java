package net.flectone.pulse.module;

import com.google.inject.Inject;
import com.google.inject.Injector;
import lombok.Getter;
import lombok.Setter;
import net.flectone.pulse.config.Permission;
import net.flectone.pulse.model.FEntity;
import net.flectone.pulse.util.PermissionUtil;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.function.Predicate;

@Getter
public abstract class AbstractModule {

    private final Set<Class<? extends AbstractModule>> children = new HashSet<>();
    private final Set<Predicate<FEntity>> predicates = new HashSet<>();

    @Inject private PermissionUtil permissionUtil;
    @Inject private Injector injector;

    private String modulePermission;

    @Setter private boolean enable;

    public AbstractModule() {
        addPredicate(fPlayer -> !isEnable());
        addPredicate(fPlayer -> !permissionUtil.has(fPlayer, modulePermission));
    }

    public abstract void reload();
    public abstract boolean isConfigEnable();

    public void registerModulePermission(Permission.IPermission permission) {
        registerPermission(permission);
        this.modulePermission = permission.getName();
    }

    public void registerPermission(Permission.IPermission permission) {
        if (permission == null) return;

        registerPermission(permission.getName(), permission.getType());
    }

    public void registerPermission(String name, Permission.Type type) {
        permissionUtil.register(name, type.name());
    }

    public void addChildren(Class<? extends AbstractModule> clazz) {
        children.add(clazz);
    }

    public void addPredicate(Predicate<FEntity> predicate) {
        predicates.add(predicate);
    }

    public boolean checkModulePredicates(FEntity entity) {
        for (Predicate<FEntity> predicate : predicates) {
            if (predicate.test(entity)) {
                return true;
            }
        }

        return false;
    }

    public Map<String, Integer> collectModuleStatuses() {
        return collectModuleStatuses(this.getClass());
    }

    public void reloadWithChildren() {
        reloadWithChildren(this.getClass(), AbstractModule::isConfigEnable);
    }

    private Map<String, Integer> collectModuleStatuses(Class<? extends AbstractModule> clazz) {
        AbstractModule module = injector.getInstance(clazz);

        Map<String, Integer> modules = new HashMap<>();

        modules.put(clazz.getSimpleName(), module.isEnable() ? 1 : 0);

        injector.getInstance(clazz)
                .getChildren()
                .forEach(subModule -> modules.putAll(collectModuleStatuses(subModule)));

        return modules;
    }

    private void reloadWithChildren(Class<? extends AbstractModule> clazz, Predicate<AbstractModule> predicate) {
        AbstractModule module = injector.getInstance(clazz);
        module.setEnable(predicate.test(module));

        if (module.isEnable()) {
            module.reload();
            module.getChildren().forEach(subModule -> reloadWithChildren(subModule, AbstractModule::isConfigEnable));
        } else {
            module.getChildren().forEach(subModule -> reloadWithChildren(subModule, m -> false));
        }
    }
}
