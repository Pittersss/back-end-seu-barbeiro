package com.two_m.yourbarber.service;

import com.two_m.yourbarber.dto.joinrequest.JoinRequestDecisionDTO;
import com.two_m.yourbarber.dto.joinrequest.JoinRequestPostDTO;
import com.two_m.yourbarber.dto.joinrequest.JoinRequestResponseDTO;
import java.util.List;

public interface JoinRequestService {

    JoinRequestResponseDTO requestToJoin(Long shopId, JoinRequestPostDTO dto, Long barberId);

    List<JoinRequestResponseDTO> listRequests(Long shopId, Long ownerId);

    JoinRequestResponseDTO decideRequest(
            Long shopId, Long requestId, JoinRequestDecisionDTO decision, Long ownerId);
}
