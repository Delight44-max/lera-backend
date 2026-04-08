package com.lera.event;

import com.lera.model.Emergency;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class EmergencyResolvedEvent extends ApplicationEvent {
    private final Emergency emergency;

    public EmergencyResolvedEvent(Object source, Emergency emergency) {
        super(source);
        this.emergency = emergency;
    }
}
