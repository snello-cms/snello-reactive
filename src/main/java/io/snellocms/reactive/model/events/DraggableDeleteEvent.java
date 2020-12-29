package io.snellocms.reactive.model.events;

public class DraggableDeleteEvent {

    public String uuid;

    public DraggableDeleteEvent(String uuid) {
        this.uuid = uuid;
    }

    @Override
    public String toString() {
        return "DraggableDeleteEvent{" +
                "uuid=" + uuid +
                '}';
    }
}
