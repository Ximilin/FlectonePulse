package net.flectone.pulse.module.message.chat;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.flectone.pulse.annotation.Async;
import net.flectone.pulse.config.Localization;
import net.flectone.pulse.config.Message;
import net.flectone.pulse.database.dao.FPlayerDAO;
import net.flectone.pulse.manager.FPlayerManager;
import net.flectone.pulse.manager.FileManager;
import net.flectone.pulse.model.Cooldown;
import net.flectone.pulse.model.FEntity;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.module.command.spy.BukkitSpyModule;
import net.flectone.pulse.module.integration.IntegrationModule;
import net.flectone.pulse.module.message.bubble.BukkitBubbleModule;
import net.flectone.pulse.module.message.chat.listener.ChatListener;
import net.flectone.pulse.registry.BukkitListenerRegistry;
import net.flectone.pulse.scheduler.TaskScheduler;
import net.flectone.pulse.util.MessageTag;
import net.flectone.pulse.util.PermissionUtil;
import net.flectone.pulse.util.Range;
import net.flectone.pulse.util.TimeUtil;
import net.flectone.pulse.util.logging.FLogger;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;
import org.bukkit.event.EventPriority;
import org.bukkit.event.player.AsyncPlayerChatEvent;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.function.Predicate;
import java.util.stream.Collectors;

@Singleton
public class BukkitChatModule extends ChatModule {

    private final FPlayerDAO fPlayerDAO;
    private final FPlayerManager fPlayerManager;
    private final PermissionUtil permissionUtil;
    private final TaskScheduler taskScheduler;
    private final BukkitListenerRegistry bukkitListenerManager;
    private final IntegrationModule integrationModule;
    private final TimeUtil timeUtil;

    @Inject private BukkitBubbleModule bubbleModule;
    @Inject private BukkitSpyModule spyModule;

    @Inject
    public BukkitChatModule(FileManager fileManager,
                            FPlayerDAO fPlayerDAO,
                            FPlayerManager fPlayerManager,
                            TaskScheduler taskScheduler,
                            BukkitListenerRegistry bukkitListenerManager,
                            IntegrationModule integrationModule,
                            PermissionUtil permissionUtil,
                            TimeUtil timeUtil) {
        super(fileManager);

        this.fPlayerDAO = fPlayerDAO;
        this.fPlayerManager = fPlayerManager;
        this.taskScheduler = taskScheduler;
        this.bukkitListenerManager = bukkitListenerManager;
        this.integrationModule = integrationModule;
        this.permissionUtil = permissionUtil;
        this.timeUtil = timeUtil;
    }

    @Override
    public void reload() {
        super.reload();

        bukkitListenerManager.register(ChatListener.class, EventPriority.NORMAL);
    }

    @Override
    public void send(FPlayer fPlayer, Object chatEvent) {
        if (!(chatEvent instanceof AsyncPlayerChatEvent event)) return;
        if (checkModulePredicates(fPlayer)) return;

        if (checkMute(fPlayer)) {
            event.getRecipients().clear();
            event.setCancelled(true);
            return;
        }

        String eventMessage = event.getMessage();

        Message.Chat.Type playerChat = message.getTypes().getOrDefault(fPlayer.getSettingValue(FPlayer.Setting.CHAT), getPlayerChat(fPlayer, eventMessage));

        var configChatEntry = message.getTypes().entrySet()
                .stream()
                .filter(entry -> entry.getValue().equals(playerChat))
                .findAny();

        if (playerChat == null || !playerChat.isEnable() || configChatEntry.isEmpty()) {
            builder(fPlayer)
                    .format(Localization.Message.Chat::getNullChat)
                    .sendBuilt();
            event.getRecipients().clear();
            event.setCancelled(true);
            return;
        }

        String chatName = configChatEntry.get().getKey();

        if (cooldownMap.containsKey(chatName)) {
            Cooldown cooldown = cooldownMap.get(chatName);
            if (cooldown != null
                    && cooldown.isEnable()
                    && !permissionUtil.has(fPlayer, cooldown.getPermissionBypass())
                    && cooldown.isCooldown(fPlayer.getUuid())) {
                long timeLeft = cooldownMap.get(chatName).getTimeLeft(fPlayer);

                builder(fPlayer)
                        .format(timeUtil.format(fPlayer, timeLeft, getCooldownMessage(fPlayer)))
                        .sendBuilt();
                event.getRecipients().clear();
                event.setCancelled(true);
                return;
            }
        }

        String trigger = playerChat.getTrigger();

        if (trigger != null && !trigger.isEmpty() && eventMessage.startsWith(trigger)) {
            eventMessage = eventMessage.substring(trigger.length()).trim();
        }

        Player sender = Bukkit.getPlayer(fPlayer.getUuid());
        if (sender == null) return;

        Predicate<FPlayer> chatPermissionFilter = fReceiver -> permissionUtil.has(fReceiver, permission.getTypes().get(chatName));

        int chatRange = playerChat.getRange();

        // in local chat you can mention it too,
        // but I don't want to full support InteractiveChat
        String finalMessage = chatRange == Range.PROXY
                || chatRange == Range.SERVER
                || chatRange == Range.WORLD_NAME
                || chatRange == Range.WORLD_TYPE
                ? integrationModule.checkMention(fPlayer, eventMessage)
                : eventMessage;

        Builder builder = builder(fPlayer)
                .tag(MessageTag.CHAT)
                .destination(playerChat.getDestination())
                .range(chatRange)
                .filter(chatPermissionFilter)
                .format(message -> message.getTypes().get(chatName))
                .message(finalMessage)
                .proxy(output -> {
                    output.writeUTF(chatName);
                    output.writeUTF(finalMessage);
                })
                .integration(s -> s.replace("<message>", finalMessage))
                .sound(soundMap.get(chatName));

        List<FPlayer> recipients = builder.build();

        builder.send(recipients);

        List<UUID> recipientsUUID = recipients.stream()
                .filter(filterFPlayer -> !filterFPlayer.isUnknown())
                .map(FEntity::getUuid)
                .toList();

        spyModule.checkChat(fPlayer, chatName, finalMessage);

        int countRecipients = recipientsUUID.size();
        if (playerChat.isNullRecipient() && countRecipients < 2) {
            taskScheduler.runAsyncLater(() -> {
                Set<UUID> onlinePlayers = fPlayerDAO.getOnlineFPlayers()
                        .stream()
                        .map(FEntity::getUuid)
                        .collect(Collectors.toSet());

                if ((onlinePlayers.containsAll(recipientsUUID) && onlinePlayers.size() <= countRecipients)
                        || chatRange > -1) {
                    builder(fPlayer)
                            .format(Localization.Message.Chat::getNullRecipient)
                            .sendBuilt();
                }
            }, 5);
        }

        event.setMessage(finalMessage);
        event.setCancelled(playerChat.isCancel());
        event.getRecipients().clear();

        bubbleModule.add(fPlayer, eventMessage);
    }

    @Inject private FLogger fLogger;

    @Async
    public void send(FEntity fPlayer, String chatName, String string) {
        if (checkModulePredicates(fPlayer)) return;

        var optionalChat = message.getTypes().entrySet().stream()
                .filter(chat -> chat.getKey().equals(chatName))
                .findAny();

        if (optionalChat.isEmpty()) return;

        String playerChatName = optionalChat.get().getKey();
        Message.Chat.Type playerChat = optionalChat.get().getValue();

        Predicate<FPlayer> filter = rangeFilter(fPlayer, playerChat.getRange()).and(fReceiver -> {
            if (!permissionUtil.has(fReceiver, permission.getTypes().get(playerChatName))) return false;

            return Bukkit.getPlayer(fReceiver.getUuid()) != null;
        });

        builder(fPlayer)
                .range(Range.SERVER)
                .destination(playerChat.getDestination())
                .filter(filter)
                .format(s -> s.getTypes().get(playerChatName))
                .message(string)
                .sound(getSound())
                .sendBuilt();
    }
}
