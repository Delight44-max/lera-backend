package com.lera.event;

import com.lera.model.Emergency;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EmergencyCancelledEvent extends ApplicationEvent {
    private final Emergency emergency;

    public EmergencyCancelledEvent(Object source, Emergency emergency) {
        super(source);
        this.emergency = emergency;
    }
}
