package com.two_m.yourbarber.service.timeblock;

import com.two_m.yourbarber.dto.timeblock.TimeBlockPostDTO;
import com.two_m.yourbarber.dto.timeblock.TimeBlockResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.mapper.TimeBlockMapper;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.model.TimeBlock;
import com.two_m.yourbarber.repository.BarberRepository;
import com.two_m.yourbarber.repository.TimeBlockRepository;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
@Transactional
public class TimeBlockServiceImpl implements TimeBlockService {

    private final TimeBlockRepository timeBlockRepository;
    private final BarberRepository barberRepository;

    @Override
    public TimeBlockResponseDTO create(Long barberId, TimeBlockPostDTO dto, Long requesterId) {
        if (!barberId.equals(requesterId)) {
            throw new ForbiddenOperationException("You can only manage your own schedule");
        }
        if (!dto.getEndsAt().isAfter(dto.getStartsAt())) {
            throw new BusinessRuleException("O fim do bloqueio deve ser depois do início.");
        }
        if (dto.getEndsAt().isBefore(LocalDateTime.now())) {
            throw new BusinessRuleException("O bloqueio não pode ficar no passado.");
        }

        Barber barber =
                barberRepository
                        .findById(barberId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Barber not found: " + barberId));

        TimeBlock block =
                TimeBlock.builder()
                        .barber(barber)
                        .startsAt(dto.getStartsAt())
                        .endsAt(dto.getEndsAt())
                        .reason(dto.getReason())
                        .build();
        return TimeBlockMapper.toDto(timeBlockRepository.save(block));
    }

    @Override
    @Transactional(readOnly = true)
    public List<TimeBlockResponseDTO> listUpcoming(Long barberId) {
        return timeBlockRepository
                .findByBarberIdAndEndsAtAfterOrderByStartsAt(barberId, LocalDateTime.now())
                .stream()
                .map(TimeBlockMapper::toDto)
                .toList();
    }

    @Override
    public void delete(Long barberId, Long blockId, Long requesterId) {
        if (!barberId.equals(requesterId)) {
            throw new ForbiddenOperationException("You can only manage your own schedule");
        }
        TimeBlock block =
                timeBlockRepository
                        .findById(blockId)
                        .orElseThrow(
                                () ->
                                        new ResourceNotFoundException(
                                                "Time block not found: " + blockId));
        if (!block.getBarber().getId().equals(barberId)) {
            throw new ForbiddenOperationException("This block belongs to another barber");
        }
        timeBlockRepository.delete(block);
    }
}
