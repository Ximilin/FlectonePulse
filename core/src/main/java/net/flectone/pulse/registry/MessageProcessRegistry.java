package net.flectone.pulse.registry;

import com.google.inject.Inject;
import com.google.inject.Singleton;
import lombok.Getter;
import net.flectone.pulse.processor.MessageProcessor;

import java.util.LinkedHashSet;
import java.util.Set;
import java.util.TreeMap;

@Getter
@Singleton
public class MessageProcessRegistry {

    private final TreeMap<Integer, Set<MessageProcessor>> processors = new TreeMap<>();

    @Inject
    public MessageProcessRegistry() {
    }

    public void reload() {
        processors.clear();
    }

    public void register(int priority, MessageProcessor messageProcessor) {
        processors.computeIfAbsent(priority, i -> new LinkedHashSet<>())
                .add(messageProcessor);
    }

}
