package com.two_m.yourbarber.service.push;

import com.two_m.yourbarber.dto.push.PushSubscriptionRequestDTO;
import com.two_m.yourbarber.dto.push.PushUnsubscribeRequestDTO;

public interface PushService {

    /** Sends a push notification to every device/browser the user has subscribed with. */
    void sendToUser(Long userId, String title, String body);

    String getVapidPublicKey();

    void subscribe(Long userId, PushSubscriptionRequestDTO dto);

    void unsubscribe(PushUnsubscribeRequestDTO dto);
}
