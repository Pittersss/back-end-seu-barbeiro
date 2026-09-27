package com.two_m.yourbarber.service.push;

import com.two_m.yourbarber.dto.push.PushSubscriptionRequestDTO;
import com.two_m.yourbarber.dto.push.PushUnsubscribeRequestDTO;

public interface PushService {

    /** Sends a push notification to every device/browser the user has subscribed with. */
    void sendToUser(Long userId, String title, String body);

    /** Same, and tapping the notification opens {@code url} (an app path like "/appointments"). */
    void sendToUser(Long userId, String title, String body, String url);

    /**
     * Sends a test notification to every subscribed device of the user and returns how many
     * deliveries the push services accepted. Powers the "send me a test" button, which is the
     * quickest way to tell a broken subscription from a working one.
     */
    int sendTest(Long userId);

    String getVapidPublicKey();

    void subscribe(Long userId, PushSubscriptionRequestDTO dto);

    void unsubscribe(PushUnsubscribeRequestDTO dto);
}
