package com.two_m.yourbarber.service.pix;

import com.two_m.yourbarber.dto.pix.PixPreviewDTO;
import com.two_m.yourbarber.dto.pix.PixQrCodeResponseDTO;

public interface PixService {

    PixQrCodeResponseDTO generateQrCode(Long appointmentId, Long requesterId);

    /**
     * Builds a Pix BR Code straight from caller-supplied values, with no appointment or
     * database lookup. Backs the "Pix sandbox" screen used to eyeball a scannable code for an
     * arbitrary key.
     */
    PixQrCodeResponseDTO preview(PixPreviewDTO dto);
}
