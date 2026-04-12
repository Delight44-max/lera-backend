package com.lera.event;

import com.lera.model.Emergency;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EmergencyStatusChangedEvent extends ApplicationEvent {

    private final Emergency emergency;

    public EmergencyStatusChangedEvent(Object source, Emergency emergency) {
        super(source);
        this.emergency = emergency;
    }
}