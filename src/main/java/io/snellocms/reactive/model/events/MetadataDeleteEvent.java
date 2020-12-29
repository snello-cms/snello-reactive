package io.snellocms.reactive.model.events;

public class MetadataDeleteEvent {

    public String uuid;

    public MetadataDeleteEvent(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "MetadataDeleteEvent{" +
                "uuid=" + uuid +
                '}';
    }
}
