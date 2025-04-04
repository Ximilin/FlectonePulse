package net.flectone.pulse.util;

import com.google.gson.JsonElement;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.flectone.pulse.module.message.format.fixation.FixationModule;
import net.flectone.pulse.module.message.format.moderation.flood.FloodModule;
import net.flectone.pulse.util.logging.FLogger;
import net.flectone.pulse.model.FEntity;
import net.flectone.pulse.model.FPlayer;
import net.flectone.pulse.module.command.stream.StreamModule;
import net.flectone.pulse.module.integration.IntegrationModule;
import net.flectone.pulse.module.message.afk.AfkModule;
import net.flectone.pulse.module.message.format.FormatModule;
import net.flectone.pulse.module.message.format.color.ColorModule;
import net.flectone.pulse.module.message.format.emoji.EmojiModule;
import net.flectone.pulse.module.message.format.image.ImageModule;
import net.flectone.pulse.module.message.format.mention.MentionModule;
import net.flectone.pulse.module.message.format.moderation.caps.CapsModule;
import net.flectone.pulse.module.message.format.moderation.swear.SwearModule;
import net.flectone.pulse.module.message.format.name.NameModule;
import net.flectone.pulse.module.message.format.questionanswer.QuestionAnswerModule;
import net.flectone.pulse.module.message.format.spoiler.SpoilerModule;
import net.flectone.pulse.module.message.format.translate.TranslateModule;
import net.flectone.pulse.module.message.format.world.WorldModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.gson.GsonComponentSerializer;
import net.kyori.adventure.text.serializer.legacy.LegacyComponentSerializer;
import org.jetbrains.annotations.NotNull;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Singleton
public class ComponentUtil {

    private final PermissionUtil permissionUtil;
    private final MiniMessage miniMessage;
    private final FLogger fLogger;

    @Inject private IntegrationModule integrationModule;
    @Inject private ColorModule colorModule;
    @Inject private EmojiModule emojiModule;
    @Inject private FixationModule fixationModule;
    @Inject private SpoilerModule spoilerModule;
    @Inject private TranslateModule translateModule;
    @Inject private FormatModule formatModule;
    @Inject private MentionModule mentionModule;
    @Inject private CapsModule capsModule;
    @Inject private FloodModule floodModule;
    @Inject private SwearModule swearModule;
    @Inject private ImageModule imageModule;
    @Inject private WorldModule worldModule;
    @Inject private AfkModule afkModule;
    @Inject private StreamModule streamModule;
    @Inject private NameModule nameModule;
    @Inject private QuestionAnswerModule questionAnswerModule;

    @Inject
    public ComponentUtil(PermissionUtil permissionUtil,
                         MiniMessage miniMessage,
                         FLogger fLogger) {
        this.permissionUtil = permissionUtil;
        this.miniMessage = miniMessage;
        this.fLogger = fLogger;
    }

    public Builder builder(@NotNull String message) {
        return builder(FPlayer.UNKNOWN, message);
    }

    public Builder builder(@NotNull FEntity sender, @NotNull String message) {
        return new Builder(sender, sender, message);
    }

    public Builder builder(@NotNull FEntity sender, @NotNull FEntity receiver, @NotNull String message) {
        return new Builder(sender, receiver, message);
    }

    public class Builder {

        private final UUID processId;
        private final FEntity sender;
        private final FEntity receiver;
        private final String message;

        private String messageToTranslate;
        private boolean userMessage;
        private boolean mention;
        private boolean emoji = true;
        private boolean fixation = true;
        private boolean question = true;
        private boolean spoiler = true;
        private boolean translate;
        private boolean swear = true;
        private boolean caps = true;
        private boolean flood = true;
        private boolean formatting = true;
        private boolean url = true;
        private boolean image = true;
        private boolean colors = true;
        private boolean interactiveChat = true;
        private TagResolver[] tagResolvers;

        public Builder(FEntity sender, FEntity receiver, String message) {
            this.processId = UUID.randomUUID();
            this.sender = sender;
            this.receiver = receiver;
            this.message = message;
        }

        public Builder caps(boolean caps) {
            this.caps = caps;
            return this;
        }

        public Builder flood(boolean flood) {
            this.flood = flood;
            return this;
        }

        public Builder colors(boolean colors) {
            this.colors = colors;
            return this;
        }

        public Builder image(boolean image) {
            this.image = image;
            return this;
        }

        public Builder url(boolean url) {
            this.url = url;
            return this;
        }

        public Builder formatting(boolean formating) {
            this.formatting = formating;
            return this;
        }

        public Builder swear(boolean swear) {
            this.swear = swear;
            return this;
        }

        public Builder emoji(boolean emoji) {
            this.emoji = emoji;
            return this;
        }

        public Builder fixation(boolean fixation) {
            this.fixation = fixation;
            return this;
        }

        public Builder question(boolean question) {
            this.question = question;
            return this;
        }

        public Builder spoiler(boolean spoiler) {
            this.spoiler = spoiler;
            return this;
        }

        public Builder translate(boolean translate) {
            this.translate = translate;
            return this;
        }

        public Builder translate(String messageToTranslate, boolean translate) {
            this.messageToTranslate = messageToTranslate;
            this.translate = translate;
            return this;
        }

        public Builder userMessage(boolean userMessage) {
            this.userMessage = userMessage;
            return this;
        }

        public Builder mention(boolean mention) {
            this.mention = mention;
            return this;
        }

        public Builder interactiveChat(boolean interactiveChat) {
            this.interactiveChat = interactiveChat;
            return this;
        }

        public Builder tagResolvers(TagResolver... tagResolvers) {
            this.tagResolvers = tagResolvers;
            return this;
        }

        // need refactor logic formatting
        // mb chain or eventbus pattern
        public Component build() {
            String message = integrationModule.setPlaceholders(
                    sender,
                    receiver,
                    // InteractiveChat integration
                    interactiveChat ? integrationModule.markSender(sender, this.message) : this.message,
                    userMessage
            );

            List<TagResolver> tagResolverList = new ArrayList<>();
            if (tagResolvers != null) {
                tagResolverList.addAll(List.of(tagResolvers));
            }

            if (emoji) {
                message = emojiModule.replace(sender, message);
                tagResolverList.add(emojiModule.emojiTag(sender, receiver));
            }

            if (question && userMessage) {
                message = questionAnswerModule.replace(sender, message);
                tagResolverList.add(questionAnswerModule.questionAnswerTag(processId, sender, receiver));
            }

            if (spoiler) {
                tagResolverList.add(spoilerModule.spoilerTag(sender, receiver, userMessage));
            }

            if (translate) {
                message = message.replace("<message_to_translate>", messageToTranslate == null ? "" : messageToTranslate);
                tagResolverList.add(translateModule.translateTag(sender, receiver));
            }

            if (formatting) {

                formatModule.getTagResolverMap()
                        .entrySet()
                        .stream()
                        .filter(entry -> formatModule.isCorrectTag(entry.getKey(), sender, userMessage))
                        .forEach(entry -> tagResolverList.add(entry.getValue()));

                tagResolverList.add(formatModule.pingTag(sender, receiver));
                tagResolverList.add(formatModule.tpsTag(sender, receiver));
                tagResolverList.add(formatModule.onlineTag(sender, receiver));
                tagResolverList.add(formatModule.coordsTag(sender, receiver));
                tagResolverList.add(formatModule.statsTag(sender, receiver));
                tagResolverList.add(formatModule.skinTag(sender, receiver));
                tagResolverList.add(formatModule.itemTag(sender, receiver));

                if (url) {
                    tagResolverList.add(formatModule.urlTag(sender, receiver));
                }

                if (userMessage) {
                    message = formatModule.replaceAll(sender, receiver, message);
                }
            }

            if (mention) {
                message = mentionModule.replace(sender, message);
                tagResolverList.add(mentionModule.mentionTag(processId, sender, receiver));
            }

            if (caps && userMessage) {
                message = capsModule.replace(sender, message);
            }

            if (flood && userMessage) {
                message = floodModule.replace(sender, message);
            }

            if (swear) {
                tagResolverList.add(swearModule.swearTag(sender, receiver));

                if (userMessage) {
                    message = swearModule.replace(sender, message);
                }
            }

            if (image && !userMessage) {
                tagResolverList.add(imageModule.imageTag(sender, receiver));
            }

            if (fixation && userMessage) {
                message = fixationModule.replace(sender, message);
            }

            if (!userMessage || permissionUtil.has(sender, formatModule.getPermission().getAll().getName())) {
                tagResolverList.add(colorModule.colorTag(colorModule.getMessage().isUseRecipientColors() ? receiver : sender));

                tagResolverList.add(worldModule.worldTag(sender));
                tagResolverList.add(afkModule.afkTag(sender));
                tagResolverList.add(streamModule.streamTag(sender));
                tagResolverList.add(nameModule.playerTag(sender));
                tagResolverList.add(nameModule.displayTag(sender, receiver));
                tagResolverList.add(nameModule.vaultSuffixTag(sender, receiver));
                tagResolverList.add(nameModule.vaultPrefixTag(sender, receiver));

                if (colors) {
                    message = FlectoneMiniTranslator.toMini(message);
                }
            }

            try {
                return miniMessage.deserialize(message.replace("§", "&"), tagResolverList.toArray(new TagResolver[0]));
            } catch (Exception e) {
                fLogger.warning(e);
            }

            return Component.empty();
        }

        public String serialize() {
            return MiniMessage.miniMessage().serialize(build());
        }

        public String legacySerialize() {
            return LegacyComponentSerializer.legacySection().serialize(build());
        }

        public JsonElement serializeToTree() {
            return GsonComponentSerializer.gson().serializeToTree(build());
        }
    }
}
