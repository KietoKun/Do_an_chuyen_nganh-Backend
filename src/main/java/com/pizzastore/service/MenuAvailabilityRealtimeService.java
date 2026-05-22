package com.pizzastore.service;

import com.pizzastore.dto.MenuAvailabilityChangedEvent;
import com.pizzastore.entity.Branch;
import org.springframework.messaging.simp.SimpMessagingTemplate;
import org.springframework.stereotype.Service;

@Service
public class MenuAvailabilityRealtimeService {
    private final SimpMessagingTemplate messagingTemplate;

    public MenuAvailabilityRealtimeService(SimpMessagingTemplate messagingTemplate) {
        this.messagingTemplate = messagingTemplate;
    }

    public void publishChanged(Branch branch) {
        if (branch == null || branch.getId() == null) {
            return;
        }
        messagingTemplate.convertAndSend(
                "/topic/menu-availability/branches/" + branch.getId(),
                new MenuAvailabilityChangedEvent(branch.getId())
        );
    }
}
