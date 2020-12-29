package io.snellocms.reactive.model.events;


import io.snellocms.reactive.model.Droppable;

import java.util.Map;

public class DroppableCreateUpdateEvent {
    public Droppable droppable;

    public DroppableCreateUpdateEvent(Droppable droppable) {
        this.droppable = droppable;
    }

    public DroppableCreateUpdateEvent(Map<String, Object> map) {
        this.droppable = new Droppable(map);
    }

    @Override
    public String toString() {
        return "DroppableCreateUpdateEvent{" +
                "droppable=" + droppable +
                '}';
    }
}
