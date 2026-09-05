package com.two_m.yourbarber.service.push;

import com.two_m.yourbarber.dto.push.PushSubscriptionRequestDTO;
import com.two_m.yourbarber.dto.push.PushUnsubscribeRequestDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.model.PushSubscription;
import com.two_m.yourbarber.model.enums.PushPlatform;
import com.two_m.yourbarber.repository.PushSubscriptionRepository;
import com.two_m.yourbarber.repository.UserRepository;
import jakarta.annotation.PostConstruct;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.security.Security;
import java.time.Duration;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Slf4j
@Transactional
public class PushServiceImpl implements PushService {

    private static final String EXPO_PUSH_URL = "https://exp.host/--/api/v2/push/send";
    private static final HttpClient HTTP_CLIENT =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();

    private final PushSubscriptionRepository pushSubscriptionRepository;
    private final UserRepository userRepository;

    @Value("${webpush.vapid.public-key}")
    private String vapidPublicKey;

    @Value("${webpush.vapid.private-key}")
    private String vapidPrivateKey;

    @Value("${webpush.vapid.subject}")
    private String vapidSubject;

    @PostConstruct
    void registerBouncyCastle() {
        Security.addProvider(new BouncyCastleProvider());
    }

    @Override
    @Transactional(readOnly = true)
    public String getVapidPublicKey() {
        return vapidPublicKey;
    }

    @Override
    public void subscribe(Long userId, PushSubscriptionRequestDTO dto) {
        if (dto.getPlatform() == PushPlatform.WEB) {
            if (dto.getEndpoint() == null || dto.getEndpoint().isBlank()) {
                throw new BusinessRuleException("Web push subscriptions require an endpoint");
            }
            PushSubscription subscription =
                    pushSubscriptionRepository
                            .findByEndpoint(dto.getEndpoint())
                            .orElseGet(PushSubscription::new);
            subscription.setUser(userRepository.getReferenceById(userId));
            subscription.setPlatform(PushPlatform.WEB);
            subscription.setEndpoint(dto.getEndpoint());
            subscription.setP256dh(dto.getP256dh());
            subscription.setAuthKey(dto.getAuthKey());
            pushSubscriptionRepository.save(subscription);
        } else {
            if (dto.getExpoPushToken() == null || dto.getExpoPushToken().isBlank()) {
                throw new BusinessRuleException(
                        "Native push subscriptions require an Expo push token");
            }
            PushSubscription subscription =
                    pushSubscriptionRepository
                            .findByExpoPushToken(dto.getExpoPushToken())
                            .orElseGet(PushSubscription::new);
            subscription.setUser(userRepository.getReferenceById(userId));
            subscription.setPlatform(dto.getPlatform());
            subscription.setExpoPushToken(dto.getExpoPushToken());
            pushSubscriptionRepository.save(subscription);
        }
    }

    @Override
    public void unsubscribe(PushUnsubscribeRequestDTO dto) {
        if (dto.getEndpoint() != null && !dto.getEndpoint().isBlank()) {
            pushSubscriptionRepository.deleteByEndpoint(dto.getEndpoint());
        }
        if (dto.getExpoPushToken() != null && !dto.getExpoPushToken().isBlank()) {
            pushSubscriptionRepository.deleteByExpoPushToken(dto.getExpoPushToken());
        }
    }

    @Override
    public void sendToUser(Long userId, String title, String body) {
        List<PushSubscription> subscriptions = pushSubscriptionRepository.findByUserId(userId);
        for (PushSubscription subscription : subscriptions) {
            try {
                if (subscription.getPlatform() == PushPlatform.WEB) {
                    sendWebPush(subscription, title, body);
                } else {
                    sendExpoPush(subscription, title, body);
                }
            } catch (Exception ex) {
                log.warn("Failed to deliver push notification (subscription {})",
                        subscription.getId(), ex);
            }
        }
    }

    private void sendWebPush(PushSubscription subscription, String title, String body)
            throws Exception {
        if (vapidPublicKey == null || vapidPublicKey.isBlank()) {
            return;
        }
        String payload =
                "{\"title\":\"" + jsonEscape(title) + "\",\"body\":\"" + jsonEscape(body) + "\"}";
        int status = deliverWebPush(subscription, payload);
        if (status == 404 || status == 410) {
            pushSubscriptionRepository.deleteByEndpoint(subscription.getEndpoint());
        }
    }

    private void sendExpoPush(PushSubscription subscription, String title, String body)
            throws Exception {
        String payload =
                "{\"to\":\""
                        + jsonEscape(subscription.getExpoPushToken())
                        + "\",\"title\":\""
                        + jsonEscape(title)
                        + "\",\"body\":\""
                        + jsonEscape(body)
                        + "\"}";
        int status = deliverExpoPush(payload);
        if (status == 404 || status == 410) {
            pushSubscriptionRepository.deleteByExpoPushToken(subscription.getExpoPushToken());
        }
    }

    /** Package-visible seam so tests can stub the actual VAPID/HTTP call. */
    protected int deliverWebPush(PushSubscription subscription, String payload) throws Exception {
        nl.martijndwars.webpush.Notification notification =
                new nl.martijndwars.webpush.Notification(
                        subscription.getEndpoint(),
                        subscription.getP256dh(),
                        subscription.getAuthKey(),
                        payload);
        nl.martijndwars.webpush.PushService webPushClient =
                new nl.martijndwars.webpush.PushService(
                        vapidPublicKey, vapidPrivateKey, vapidSubject);
        org.apache.http.HttpResponse response = webPushClient.send(notification);
        return response.getStatusLine().getStatusCode();
    }

    /** Package-visible seam so tests can stub the actual Expo HTTP call. */
    protected int deliverExpoPush(String payload) throws Exception {
        HttpRequest request =
                HttpRequest.newBuilder()
                        .uri(URI.create(EXPO_PUSH_URL))
                        .header("Content-Type", "application/json")
                        .header("Accept", "application/json")
                        .POST(HttpRequest.BodyPublishers.ofString(payload))
                        .build();
        HttpResponse<String> response =
                HTTP_CLIENT.send(request, HttpResponse.BodyHandlers.ofString());
        return response.statusCode();
    }

    private String jsonEscape(String value) {
        return value == null ? "" : value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
