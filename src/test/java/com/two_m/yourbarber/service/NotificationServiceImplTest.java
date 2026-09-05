package com.two_m.yourbarber.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.notification.NotificationResponseDTO;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Notification;
import com.two_m.yourbarber.model.User;
import com.two_m.yourbarber.model.enums.NotificationType;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.NotificationRepository;
import com.two_m.yourbarber.repository.UserRepository;
import com.two_m.yourbarber.service.push.PushService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class NotificationServiceImplTest {

    @Mock private NotificationRepository notificationRepository;
    @Mock private UserRepository userRepository;
    @Mock private PushService pushService;

    @InjectMocks private NotificationServiceImpl notificationService;

    private User user(long id) {
        User user =
                User.builder()
                        .name("User")
                        .email("user" + id + "@example.com")
                        .password("x")
                        .role(UserRole.CLIENT)
                        .build();
        user.setId(id);
        return user;
    }

    private Notification notification(long id, User recipient, boolean read) {
        Notification notification =
                Notification.builder()
                        .recipient(recipient)
                        .type(NotificationType.APPOINTMENT_REQUESTED)
                        .message("msg")
                        .read(read)
                        .build();
        notification.setId(id);
        return notification;
    }

    @Test
    void notify_savesNotificationAndSendsPush() {
        when(userRepository.getReferenceById(5L)).thenReturn(user(5L));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.notify(5L, NotificationType.APPOINTMENT_CONFIRMED, "Confirmado", null);

        ArgumentCaptor<Notification> captor = ArgumentCaptor.forClass(Notification.class);
        verify(notificationRepository).save(captor.capture());
        assertThat(captor.getValue().getType()).isEqualTo(NotificationType.APPOINTMENT_CONFIRMED);
        assertThat(captor.getValue().getMessage()).isEqualTo("Confirmado");
        verify(pushService).sendToUser(eq(5L), any(), eq("Confirmado"));
    }

    @Test
    void listForUser_mapsToDto() {
        User recipient = user(1L);
        when(notificationRepository.findByRecipientIdOrderByCreatedAtDesc(1L))
                .thenReturn(List.of(notification(10L, recipient, false)));

        List<NotificationResponseDTO> result = notificationService.listForUser(1L);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getId()).isEqualTo(10L);
        assertThat(result.get(0).isRead()).isFalse();
    }

    @Test
    void countUnread_delegatesToRepository() {
        when(notificationRepository.countByRecipientIdAndReadFalse(1L)).thenReturn(3L);

        assertThat(notificationService.countUnread(1L)).isEqualTo(3L);
    }

    @Test
    void markRead_owner_marksAsRead() {
        User recipient = user(1L);
        Notification notification = notification(10L, recipient, false);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));
        when(notificationRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        notificationService.markRead(10L, 1L);

        assertThat(notification.isRead()).isTrue();
    }

    @Test
    void markRead_notOwner_throwsForbidden() {
        User recipient = user(1L);
        Notification notification = notification(10L, recipient, false);
        when(notificationRepository.findById(10L)).thenReturn(Optional.of(notification));

        assertThrows(
                ForbiddenOperationException.class,
                () -> notificationService.markRead(10L, 99L));
        verify(notificationRepository, never()).save(any());
    }

    @Test
    void markRead_missing_throwsNotFound() {
        when(notificationRepository.findById(10L)).thenReturn(Optional.empty());

        assertThrows(
                ResourceNotFoundException.class, () -> notificationService.markRead(10L, 1L));
    }

    @Test
    void markAllRead_marksEveryUnreadNotification() {
        User recipient = user(1L);
        List<Notification> unread =
                List.of(notification(10L, recipient, false), notification(11L, recipient, false));
        when(notificationRepository.findByRecipientIdAndReadFalse(1L)).thenReturn(unread);

        notificationService.markAllRead(1L);

        assertThat(unread).allMatch(Notification::isRead);
        verify(notificationRepository, times(1)).saveAll(unread);
    }
}
