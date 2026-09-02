package com.two_m.yourbarber.service.timeblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.timeblock.TimeBlockPostDTO;
import com.two_m.yourbarber.dto.timeblock.TimeBlockResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.TimeBlock;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.TimeBlockRepository;
import java.time.LocalDateTime;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class TimeBlockServiceImplTest {

    @Mock private TimeBlockRepository timeBlockRepository;
    @Mock private BarberRepository barberRepository;

    @InjectMocks private TimeBlockServiceImpl service;

    private Barber barber(long id) {
        Barber barber =
                Barber.builder()
                        .name("Barber")
                        .email("b@example.com")
                        .password("x")
                        .role(UserRole.BARBER)
                        .build();
        barber.setId(id);
        return barber;
    }

    @Test
    void create_self_saves() {
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber(1L)));
        when(timeBlockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        TimeBlockPostDTO dto =
                new TimeBlockPostDTO(
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(1).plusHours(2),
                        "Almoço");

        TimeBlockResponseDTO result = service.create(1L, dto, 1L);

        assertThat(result.getReason()).isEqualTo("Almoço");
        assertThat(result.getBarberId()).isEqualTo(1L);
    }

    @Test
    void create_notSelf_throwsForbidden() {
        TimeBlockPostDTO dto =
                new TimeBlockPostDTO(
                        LocalDateTime.now().plusDays(1),
                        LocalDateTime.now().plusDays(1).plusHours(2),
                        null);

        assertThrows(ForbiddenOperationException.class, () -> service.create(1L, dto, 2L));
        verify(timeBlockRepository, never()).save(any());
    }

    @Test
    void create_endBeforeStart_throws() {
        TimeBlockPostDTO dto =
                new TimeBlockPostDTO(
                        LocalDateTime.now().plusDays(2),
                        LocalDateTime.now().plusDays(1),
                        null);

        assertThrows(BusinessRuleException.class, () -> service.create(1L, dto, 1L));
    }

    @Test
    void delete_ownedBlock_deletes() {
        Barber barber = barber(1L);
        TimeBlock block =
                TimeBlock.builder()
                        .barber(barber)
                        .startsAt(LocalDateTime.now())
                        .endsAt(LocalDateTime.now().plusHours(1))
                        .build();
        block.setId(5L);
        when(timeBlockRepository.findById(5L)).thenReturn(Optional.of(block));

        service.delete(1L, 5L, 1L);

        verify(timeBlockRepository).delete(block);
    }

    @Test
    void delete_notSelf_throwsForbidden() {
        assertThrows(ForbiddenOperationException.class, () -> service.delete(1L, 5L, 2L));
        verify(timeBlockRepository, never()).delete(any());
    }
}
