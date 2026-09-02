package com.two_m.yourbarber.service.barber;

import com.two_m.yourbarber.dto.barber.BarberPostPutDTO;
import com.two_m.yourbarber.dto.barber.BarberResponseDTO;
import com.two_m.yourbarber.exception.BusinessRuleException;
import com.two_m.yourbarber.exception.ForbiddenOperationException;
import com.two_m.yourbarber.exception.ResourceNotFoundException;
import com.two_m.yourbarber.mapper.BarberMapper;
import com.two_m.yourbarber.model.Barber;
import com.two_m.yourbarber.repository.BarberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class BarberServiceImpl implements BarberService {

    private final BarberRepository barberRepository;

    @Override
    public BarberResponseDTO getBarber(Long id) {
        return BarberMapper.toDto(findBarber(id));
    }

    @Override
    public BarberResponseDTO updateBarber(Long id, BarberPostPutDTO dto, Long requesterId) {
        Barber barber = findBarber(id);
        assertSelf(barber, requesterId);
        validateWorkingHours(dto);

        barber.setName(dto.getName());
        barber.setPhone(dto.getPhone());
        barber.setPixKey(dto.getPixKey());
        barber.setDelayTolerance(dto.getDelayTolerance());
        barber.setWorkStartHour(dto.getWorkStartHour());
        barber.setWorkEndHour(dto.getWorkEndHour());
        barber.setBreakStartHour(dto.getBreakStartHour());
        barber.setBreakEndHour(dto.getBreakEndHour());

        return BarberMapper.toDto(barberRepository.save(barber));
    }

    @Override
    public BarberResponseDTO toggleAvailability(Long id, Long requesterId) {
        Barber barber = findBarber(id);
        assertSelf(barber, requesterId);
        barber.setAvailable(!barber.isAvailable());
        return BarberMapper.toDto(barberRepository.save(barber));
    }

    @Override
    public void deleteBarber(Long id, Long requesterId) {
        Barber barber = findBarber(id);
        assertSelf(barber, requesterId);
        barberRepository.delete(barber);
    }

    private void validateWorkingHours(BarberPostPutDTO dto) {
        if (dto.getWorkStartHour() >= dto.getWorkEndHour()) {
            throw new BusinessRuleException(
                    "O horário de abertura deve ser antes do horário de fechamento.");
        }
        Integer breakStart = dto.getBreakStartHour();
        Integer breakEnd = dto.getBreakEndHour();
        if ((breakStart == null) != (breakEnd == null)) {
            throw new BusinessRuleException("O intervalo precisa de início e fim.");
        }
        if (breakStart != null) {
            if (breakStart >= breakEnd
                    || breakStart < dto.getWorkStartHour()
                    || breakEnd > dto.getWorkEndHour()) {
                throw new BusinessRuleException(
                        "O intervalo deve estar dentro do horário de atendimento.");
            }
        }
    }

    private void assertSelf(Barber barber, Long requesterId) {
        if (!barber.getId().equals(requesterId)) {
            throw new ForbiddenOperationException("You can only manage your own profile");
        }
    }

    private Barber findBarber(Long id) {
        return barberRepository
                .findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Barber not found: " + id));
    }
}
