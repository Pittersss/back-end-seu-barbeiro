package com.two_m.yourbarber.service.clientblock;

import com.two_m.yourbarber.dto.clientblock.ClientBlockPostDTO;
import com.two_m.yourbarber.dto.clientblock.ClientBlockResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.DuplicateResourceException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.mapper.ClientBlockMapper;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.Client;
import com.two_m.yourbarber.model.ClientBlock;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.ClientBlockRepository;
import com.two_m.yourbarber.repository.ClientRepository;
import com.two_m.yourbarber.service.SubscriptionService;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class ClientBlockServiceImpl implements ClientBlockService {

    private final ClientBlockRepository clientBlockRepository;
    private final BarberRepository barberRepository;
    private final ClientRepository clientRepository;
    private final SubscriptionService subscriptionService;

    @Override
    public ClientBlockResponseDTO block(Long barberId, ClientBlockPostDTO dto, Long requesterId) {
        assertSelf(barberId, requesterId);
        subscriptionService.assertActive(requesterId);
        if (barberId.equals(dto.getClientId())) {
            throw new BusinessRuleException("You cannot block yourself");
        }
        if (clientBlockRepository.existsByBarberIdAndClientId(barberId, dto.getClientId())) {
            throw new DuplicateResourceException("This client is already blocked");
        }

        Barber barber =
                barberRepository
                        .findById(barberId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Barber not found: " + barberId));
        Client client =
                clientRepository
                        .findById(dto.getClientId())
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Client not found: " + dto.getClientId()));

        ClientBlock block =
                ClientBlock.builder()
                        .barber(barber)
                        .client(client)
                        .reason(dto.getReason())
                        .build();
        return ClientBlockMapper.toDto(clientBlockRepository.save(block));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ClientBlockResponseDTO> list(Long barberId, Long requesterId) {
        assertSelf(barberId, requesterId);
        return clientBlockRepository.findByBarberIdOrderByCreatedAtDesc(barberId).stream()
                .map(ClientBlockMapper::toDto)
                .toList();
    }

    @Override
    public void unblock(Long barberId, Long clientId, Long requesterId) {
        assertSelf(barberId, requesterId);
        subscriptionService.assertActive(requesterId);
        ClientBlock block =
                clientBlockRepository
                        .findByBarberIdAndClientId(barberId, clientId)
                        .orElseThrow(
                                () -> new ResourceNotFoundException("This client is not blocked"));
        clientBlockRepository.delete(block);
    }

    private void assertSelf(Long barberId, Long requesterId) {
        if (!barberId.equals(requesterId)) {
            throw new ForbiddenOperationException("You can only manage your own blocked list");
        }
    }
}
