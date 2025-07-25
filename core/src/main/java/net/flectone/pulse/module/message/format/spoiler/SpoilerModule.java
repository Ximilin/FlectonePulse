package net.flectone.pulse.module.message.format.spoiler;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.flectone.pulse.configuration.Localization;
import net.flectone.pulse.configuration.Message;
import net.flectone.pulse.configuration.Permission;
import net.flectone.pulse.context.MessageContext;
import net.flectone.pulse.processor.MessageProcessor;
import net.flectone.pulse.resolver.FileResolver;
import net.flectone.pulse.model.FEntity;
import net.flectone.pulse.module.AbstractModuleMessage;
import net.flectone.pulse.pipeline.MessagePipeline;
import net.flectone.pulse.registry.MessageProcessRegistry;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextReplacementConfig;
import net.kyori.adventure.text.minimessage.tag.Tag;
import net.kyori.adventure.text.minimessage.tag.resolver.TagResolver;
import net.kyori.adventure.text.serializer.plain.PlainTextComponentSerializer;

import static net.flectone.pulse.util.TagResolverUtil.emptyTagResolver;

@Singleton
public class SpoilerModule extends AbstractModuleMessage<Localization.Message.Format.Spoiler> implements MessageProcessor {

    private final Message.Format.Spoiler message;
    private final Permission.Message.Format.Spoiler permission;
    private final MessagePipeline messagePipeline;
    private final MessageProcessRegistry messageProcessRegistry;

    @Inject
    public SpoilerModule(FileResolver fileResolver,
                         MessagePipeline messagePipeline,
                         MessageProcessRegistry messageProcessRegistry) {
        super(localization -> localization.getMessage().getFormat().getSpoiler());

        this.message = fileResolver.getMessage().getFormat().getSpoiler();
        this.permission = fileResolver.getPermission().getMessage().getFormat().getSpoiler();
        this.messagePipeline = messagePipeline;
        this.messageProcessRegistry = messageProcessRegistry;
    }

    @Override
    public void onEnable() {
        registerModulePermission(permission);

        messageProcessRegistry.register(100, this);
    }

    @Override
    protected boolean isConfigEnable() {
        return message.isEnable();
    }

    @Override
    public void process(MessageContext messageContext) {
        if (!messageContext.isSpoiler()) return;

        messageContext.addTagResolvers(spoilerTag(messageContext.getSender(), messageContext.getReceiver(), messageContext.isUserMessage()));
    }

    private TagResolver spoilerTag(FEntity sender, FEntity receiver, boolean userMessage) {
        String tag = "spoiler";
        if (checkModulePredicates(sender)) return emptyTagResolver(tag);

        return TagResolver.resolver(tag, (argumentQueue, context) -> {
            Tag.Argument spoilerTag = argumentQueue.peek();
            if (spoilerTag == null) return Tag.selfClosingInserting(Component.empty());

            String spoilerText = spoilerTag.value();

            Component spoilerComponent = messagePipeline.builder(sender, receiver, spoilerText)
                    .userMessage(userMessage)
                    .build();

            int length = PlainTextComponentSerializer.plainText().serialize(spoilerComponent).length();

            Localization.Message.Format.Spoiler localization = resolveLocalization(receiver);

            Component component = Component.text(localization.getSymbol().repeat(length))
                    .hoverEvent(messagePipeline.builder(sender, receiver, localization.getHover())
                            .build()
                            .replaceText(TextReplacementConfig.builder().match("<message>")
                                    .replacement(spoilerComponent)
                                    .build()
                            )
                    )
                    .color(messagePipeline.builder(sender, receiver, message.getColor())
                            .build()
                            .color()
                    );

            return Tag.selfClosingInserting(component);
        });
    }
}
