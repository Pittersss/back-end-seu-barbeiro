package com.two_m.yourbarber.service.clientblock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.two_m.yourbarber.dto.clientblock.ClientBlockPostDTO;
import com.two_m.yourbarber.dto.clientblock.ClientBlockResponseDTO;
import com.two_m.yourbarber.exception.DuplicateResourceException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.ClientBlock;
import com.two_m.yourbarber.model.enums.UserRole;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.ClientBlockRepository;
import com.two_m.yourbarber.repository.ClientRepository;
import com.two_m.yourbarber.service.SubscriptionService;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ClientBlockServiceImplTest {

    @Mock private ClientBlockRepository clientBlockRepository;
    @Mock private BarberRepository barberRepository;
    @Mock private ClientRepository clientRepository;
    @Mock private SubscriptionService subscriptionService;

    @InjectMocks private ClientBlockServiceImpl service;

    private Barber barber(long id) {
        Barber barber =
                Barber.builder().name("B").email("b@x.com").password("x").role(UserRole.BARBER).build();
        barber.setId(id);
        return barber;
    }

    private Client client(long id) {
        Client client =
                Client.builder()
                        .name("C")
                        .phone("999")
                        .email("c@x.com")
                        .password("x")
                        .role(UserRole.CLIENT)
                        .build();
        client.setId(id);
        return client;
    }

    @Test
    void block_new_saves() {
        when(clientBlockRepository.existsByBarberIdAndClientId(1L, 2L)).thenReturn(false);
        when(barberRepository.findById(1L)).thenReturn(Optional.of(barber(1L)));
        when(clientRepository.findById(2L)).thenReturn(Optional.of(client(2L)));
        when(clientBlockRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        ClientBlockResponseDTO result = service.block(1L, new ClientBlockPostDTO(2L, "no-show"), 1L);

        assertThat(result.getClientId()).isEqualTo(2L);
        assertThat(result.getClientPhone()).isEqualTo("999");
    }

    @Test
    void block_notSelf_throwsForbidden() {
        assertThrows(
                ForbiddenOperationException.class,
                () -> service.block(1L, new ClientBlockPostDTO(2L, null), 99L));
        verify(clientBlockRepository, never()).save(any());
    }

    @Test
    void block_alreadyBlocked_throwsDuplicate() {
        when(clientBlockRepository.existsByBarberIdAndClientId(1L, 2L)).thenReturn(true);

        assertThrows(
                DuplicateResourceException.class,
                () -> service.block(1L, new ClientBlockPostDTO(2L, null), 1L));
    }

    @Test
    void unblock_existing_deletes() {
        ClientBlock block = ClientBlock.builder().barber(barber(1L)).client(client(2L)).build();
        when(clientBlockRepository.findByBarberIdAndClientId(1L, 2L)).thenReturn(Optional.of(block));

        service.unblock(1L, 2L, 1L);

        verify(clientBlockRepository).delete(block);
    }

    @Test
    void unblock_missing_throwsNotFound() {
        when(clientBlockRepository.findByBarberIdAndClientId(1L, 2L)).thenReturn(Optional.empty());

        assertThrows(ResourceNotFoundException.class, () -> service.unblock(1L, 2L, 1L));
    }
}
