package com.two_m.yourbarber.service.barber;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.barber.BarberPostPutDTO;
import com.two_m.yourbarber.dto.barber.BarberResponseDTO;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.service.SubscriptionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class BarberServiceImplTest {

    @Mock private BarberRepository barberRepository;
    @Mock private SubscriptionService subscriptionService;

    @InjectMocks private BarberServiceImpl barberService;

    private Barber barber(long id) {
        Barber barber =
                Barber.builder()
                        .name("John")
                        .email("john@example.com")
                        .password("encoded")
                        .role(UserRole.BARBER)
                        .phone("111")
                        .pixKey("pix")
                        .available(true)
                        .delayTolerance(5)
                        .workStartHour(9)
                        .workEndHour(18)
                        .build();
        barber.setId(id);
        return barber;
    }

    @Test
    void getBarber_found_returnsDto() {
        Barber barber = barber(1L);
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber));

        BarberResponseDTO result = barberService.getBarber(1L);

        assertThat(result.getId()).isEqualTo(1L);
        assertThat(result.getEmail()).isEqualTo("john@example.com");
    }

    @Test
    void getBarber_notFound_throws() {
        when(barberRepository.findById(99L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> barberService.getBarber(99L));
    }

    @Test
    void updateBarber_self_updatesFields() {
        Barber barber = barber(1L);
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber));
        when(barberRepository.save(any(Barber.class))).thenAnswer(inv -> inv.getArgument(0));

        BarberPostPutDTO dto =
                new BarberPostPutDTO("New Name", "222", "new-pix", 10, 8, 20, null, null);
        BarberResponseDTO result = barberService.updateBarber(1L, dto, 1L);

        assertThat(result.getName()).isEqualTo("New Name");
        assertThat(result.getPhone()).isEqualTo("222");
        assertThat(result.getPixKey()).isEqualTo("new-pix");
        assertThat(result.getDelayTolerance()).isEqualTo(10);
        assertThat(result.getWorkStartHour()).isEqualTo(8);
        assertThat(result.getWorkEndHour()).isEqualTo(20);
    }

    @Test
    void updateBarber_invalidWorkingHours_throws() {
        Barber barber = barber(1L);
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber));

        BarberPostPutDTO dto =
                new BarberPostPutDTO("John", "111", "pix", 0, 18, 9, null, null);

        assertThrows(
                com.two_m.yourbarber.exception.BusinessRuleException.class,
                () -> barberService.updateBarber(1L, dto, 1L));
        verify(barberRepository, never()).save(any());
    }

    @Test
    void toggleAvailability_self_flips() {
        Barber barber = barber(1L);
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber));
        when(barberRepository.save(any(Barber.class))).thenAnswer(inv -> inv.getArgument(0));

        BarberResponseDTO result = barberService.toggleAvailability(1L, 1L);

        assertThat(result.isAvailable()).isFalse();
    }

    @Test
    void updateBarber_notSelf_throwsForbidden() {
        Barber barber = barber(1L);
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber));

        BarberPostPutDTO dto =
                new BarberPostPutDTO("New Name", "222", "new-pix", 10, 8, 20, null, null);

        assertThrows(
                ForbiddenOperationException.class,
                () -> barberService.updateBarber(1L, dto, 2L));
        verify(barberRepository, never()).save(any());
    }

    @Test
    void deleteBarber_self_deletes() {
        Barber barber = barber(1L);
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber));

        barberService.deleteBarber(1L, 1L);

        verify(barberRepository).delete(barber);
    }

    @Test
    void deleteBarber_notSelf_throwsForbidden() {
        Barber barber = barber(1L);
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber));

        assertThrows(
                ForbiddenOperationException.class, () -> barberService.deleteBarber(1L, 2L));
        verify(barberRepository, never()).delete(any());
    }
}
