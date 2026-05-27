package com.pizzastore.service.unit;

import com.pizzastore.dto.MenuAvailabilityChangedEvent;
import com.pizzastore.entity.Branch;
import com.pizzastore.service.MenuAvailabilityRealtimeService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.messaging.simp.SimpMessagingTemplate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class MenuAvailabilityRealtimeServiceTest {

    @Mock
    private SimpMessagingTemplate messagingTemplate;

    @Test
    void publishChangedShouldSendBranchAvailabilityEvent() {
        MenuAvailabilityRealtimeService service = new MenuAvailabilityRealtimeService(messagingTemplate);
        Branch branch = new Branch();
        branch.setId(10L);

        service.publishChanged(branch);

        ArgumentCaptor<MenuAvailabilityChangedEvent> captor =
                ArgumentCaptor.forClass(MenuAvailabilityChangedEvent.class);
        verify(messagingTemplate).convertAndSend(
                org.mockito.ArgumentMatchers.eq("/topic/menu-availability/branches/10"),
                captor.capture()
        );
        assertEquals("MENU_AVAILABILITY_CHANGED", captor.getValue().getEvent());
        assertEquals(10L, captor.getValue().getBranchId());
    }

    @Test
    void publishChangedShouldIgnoreMissingBranch() {
        MenuAvailabilityRealtimeService service = new MenuAvailabilityRealtimeService(messagingTemplate);

        service.publishChanged(null);
        service.publishChanged(new Branch());

        verify(messagingTemplate, never()).convertAndSend(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(Object.class)
        );
    }
}
