package com.two_m.yourbarber.service.timeblock;

import com.two_m.yourbarber.dto.timeblock.TimeBlockPostDTO;
import com.two_m.yourbarber.dto.timeblock.TimeBlockResponseDTO;
import java.util.List;

public interface TimeBlockService {

    TimeBlockResponseDTO create(Long barberId, TimeBlockPostDTO dto, Long requesterId);

    List<TimeBlockResponseDTO> listUpcoming(Long barberId);

    void delete(Long barberId, Long blockId, Long requesterId);
}
