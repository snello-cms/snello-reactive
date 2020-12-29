package io.snellocms.reactive.model.events;


import io.snellocms.reactive.model.Draggable;

import java.util.Map;

public class DraggableCreateUpdateEvent {
    public Draggable draggable;

    public DraggableCreateUpdateEvent(Draggable draggable) {
        this.draggable = draggable;
    }

    public DraggableCreateUpdateEvent(Map<String, Object> map) {
        this.draggable = new Draggable(map);
    }

    @Override
    public String toString() {
        return "DraggableCreateUpdateEvent{" +
                "draggable=" + draggable +
                '}';
    }
}
