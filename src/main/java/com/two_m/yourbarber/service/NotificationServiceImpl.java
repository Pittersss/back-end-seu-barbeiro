package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.notification.NotificationResponseDTO;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.mapper.NotificationMapper;
import com.two_m.yourbarber.model.Appointment;
import com.two_m.yourbarber.model.Notification;
import com.two_m.yourbarber.model.enums.NotificationType;
import com.two_m.yourbarber.repository.NotificationRepository;
import com.two_m.yourbarber.repository.UserRepository;
import com.two_m.yourbarber.service.push.PushService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class NotificationServiceImpl implements NotificationService {

    private static final String PUSH_TITLE = "Seu Barbeiro";

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;
    private final PushService pushService;

    @Override
    @Transactional(readOnly = true)
    public List<NotificationResponseDTO> listForUser(Long userId) {
        return notificationRepository.findByRecipientIdOrderByCreatedAtDesc(userId).stream()
                .map(NotificationMapper::toDto)
                .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public long countUnread(Long userId) {
        return notificationRepository.countByRecipientIdAndReadFalse(userId);
    }

    @Override
    public void markRead(Long notificationId, Long userId) {
        Notification notification = findNotification(notificationId);
        if (!notification.getRecipient().getId().equals(userId)) {
            throw new ForbiddenOperationException("This notification does not belong to you");
        }
        notification.setRead(true);
        notificationRepository.save(notification);
    }

    @Override
    public void markAllRead(Long userId) {
        List<Notification> unread = notificationRepository.findByRecipientIdAndReadFalse(userId);
        unread.forEach(n -> n.setRead(true));
        notificationRepository.saveAll(unread);
    }

    @Override
    public void notify(
            Long recipientId, NotificationType type, String message, Appointment appointment) {
        Notification notification =
                Notification.builder()
                        .recipient(userRepository.getReferenceById(recipientId))
                        .type(type)
                        .message(message)
                        .appointment(appointment)
                        .build();
        notificationRepository.save(notification);
        pushService.sendToUser(recipientId, PUSH_TITLE, message);
    }

    private Notification findNotification(Long id) {
        return notificationRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found: " + id));
    }
}
