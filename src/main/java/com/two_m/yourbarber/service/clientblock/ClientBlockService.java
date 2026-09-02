package com.two_m.yourbarber.service.clientblock;

import com.two_m.yourbarber.dto.clientblock.ClientBlockPostDTO;
import com.two_m.yourbarber.dto.clientblock.ClientBlockResponseDTO;
import java.util.List;

public interface ClientBlockService {

    ClientBlockResponseDTO block(Long barberId, ClientBlockPostDTO dto, Long requesterId);

    List<ClientBlockResponseDTO> list(Long barberId, Long requesterId);

    void unblock(Long barberId, Long clientId, Long requesterId);
}
