package com.two_m.yourbarber.service.push;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.model.PushSubscription;
import com.two_m.yourbarber.model.enums.PushPlatform;
import com.two_m.yourbarber.repository.PushSubscriptionRepository;
import com.two_m.yourbarber.repository.UserRepository;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class PushServiceImplTest {

    @Mock private PushSubscriptionRepository pushSubscriptionRepository;
    @Mock private UserRepository userRepository;

    private PushServiceImpl pushService;

    @BeforeEach
    void setUp() {
        pushService = spy(new PushServiceImpl(pushSubscriptionRepository, userRepository));
        ReflectionTestUtils.setField(pushService, "vapidPublicKey", "public-key");
        ReflectionTestUtils.setField(pushService, "vapidPrivateKey", "private-key");
        ReflectionTestUtils.setField(pushService, "vapidSubject", "mailto:test@example.com");
    }

    private PushSubscription webSubscription(long id, String endpoint) {
        return PushSubscription.builder()
                .id(id)
                .platform(PushPlatform.WEB)
                .endpoint(endpoint)
                .p256dh("p256dh")
                .authKey("auth")
                .build();
    }

    private PushSubscription expoSubscription(long id, String token) {
        return PushSubscription.builder()
                .id(id)
                .platform(PushPlatform.ANDROID)
                .expoPushToken(token)
                .build();
    }

    @Test
    void sendToUser_webSubscriptionExpired_deletesSubscription() throws Exception {
        PushSubscription subscription = webSubscription(1L, "https://push.example.com/abc");
        when(pushSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(subscription));
        doReturn(410).when(pushService).deliverWebPush(any(), any());

        pushService.sendToUser(1L, "title", "body");

        verify(pushSubscriptionRepository).deleteByEndpoint("https://push.example.com/abc");
    }

    @Test
    void sendToUser_webSubscriptionDelivered_keepsSubscription() throws Exception {
        PushSubscription subscription = webSubscription(1L, "https://push.example.com/abc");
        when(pushSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(subscription));
        doReturn(201).when(pushService).deliverWebPush(any(), any());

        pushService.sendToUser(1L, "title", "body");

        verify(pushSubscriptionRepository, never())
                .deleteByEndpoint(any());
    }

    @Test
    void sendToUser_expoSubscriptionExpired_deletesSubscription() throws Exception {
        PushSubscription subscription = expoSubscription(2L, "ExponentPushToken[xxx]");
        when(pushSubscriptionRepository.findByUserId(2L)).thenReturn(List.of(subscription));
        doReturn(404).when(pushService).deliverExpoPush(any());

        pushService.sendToUser(2L, "title", "body");

        verify(pushSubscriptionRepository).deleteByExpoPushToken("ExponentPushToken[xxx]");
    }

    @Test
    void sendToUser_deliveryThrows_doesNotPropagate() throws Exception {
        PushSubscription subscription = webSubscription(1L, "https://push.example.com/abc");
        when(pushSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(subscription));
        doThrow(new RuntimeException("network down"))
                .when(pushService)
                .deliverWebPush(any(), any());

        pushService.sendToUser(1L, "title", "body");

        verify(pushSubscriptionRepository, never())
                .deleteByEndpoint(any());
    }

    @Test
    void sendToUser_noVapidKeyConfigured_skipsWebPushWithoutError() throws Exception {
        ReflectionTestUtils.setField(pushService, "vapidPublicKey", "");
        PushSubscription subscription = webSubscription(1L, "https://push.example.com/abc");
        when(pushSubscriptionRepository.findByUserId(1L)).thenReturn(List.of(subscription));

        pushService.sendToUser(1L, "title", "body");

        verify(pushService, never()).deliverWebPush(any(), any());
    }
}
