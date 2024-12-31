package net.flectone.pulse.module.message.enchant.listener;

import com.github.retrooper.packetevents.event.PacketSendEvent;
import com.github.retrooper.packetevents.protocol.packettype.PacketType;
import com.github.retrooper.packetevents.wrapper.play.server.WrapperPlayServerSystemChatMessage;
import com.google.inject.Inject;
import com.google.inject.Singleton;
import net.flectone.pulse.listener.AbstractPacketListener;
import net.flectone.pulse.module.message.enchant.EnchantModule;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.TranslatableComponent;

@Singleton
public class EnchantPacketListener extends AbstractPacketListener {

    private final EnchantModule enchantModule;

    @Inject
    public EnchantPacketListener(EnchantModule enchantModule) {
        this.enchantModule = enchantModule;
    }

    @Override
    public void onPacketSend(PacketSendEvent event) {
        if (event.isCancelled()) return;
        if (event.getPacketType() != PacketType.Play.Server.SYSTEM_CHAT_MESSAGE) return;

        WrapperPlayServerSystemChatMessage wrapper = new WrapperPlayServerSystemChatMessage(event);
        Component component = wrapper.getMessage();
        if (!(component instanceof TranslatableComponent translatableComponent)) return;

        String key = translatableComponent.key();
        if (cancelMessageNotDelivered(event, key)) return;
        if (!key.startsWith("commands.enchant.success")) return;
        if (translatableComponent.args().size() < 2) return;
        if (!enchantModule.isEnable()) return;

        if (!(translatableComponent.args().get(0) instanceof TranslatableComponent enchantComponent)) return;

        String enchantKey = enchantComponent.key();

        if (enchantComponent.children().size() < 2) return;
        if (!(enchantComponent.children().get(1) instanceof TranslatableComponent levelComponent)) return;

        String levelKey = levelComponent.key();

        String count = null;
        String target = null;

        switch (key) {
            case "commands.enchant.success.single" -> {
                if (!(translatableComponent.args().get(1) instanceof TextComponent targetComponent)) return;
                target = targetComponent.content();
            }
            case "commands.enchant.success.multiple" -> {
                if (!(translatableComponent.args().get(1) instanceof TextComponent textComponent)) return;
                count = textComponent.content();
            }
            default -> {
                return;
            }
        }

        event.setCancelled(true);

        enchantModule.send(event.getUser().getUUID(), enchantKey, levelKey, target, count);
    }
}
