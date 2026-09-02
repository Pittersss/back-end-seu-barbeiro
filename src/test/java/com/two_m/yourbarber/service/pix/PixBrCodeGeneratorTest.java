package com.two_m.yourbarber.service.pix;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

class PixBrCodeGeneratorTest {

    /**
     * Golden payload produced by this exact generator for fixed inputs. Its field layout
     * (00,01,26,52,53,54,58,59,60,62,63) and CRC field structure were validated against the
     * worked example in the Central Bank of Brazil's "Manual do BR Code" v2.0.1 (section 2.2),
     * and the CRC-16/CCITT-FFFF algorithm was independently verified against a known-good test
     * vector before being wired in (see crc16Hex_matchesKnownVector below).
     */
    private static final String GOLDEN_PAYLOAD =
            "00020101021226420014BR.GOV.BCB.PIX0120barbeiro@example.com520400005303986"
                    + "540545.005802BR5912SEU BARBEIRO6009SAO PAULO62100506APT1236304E22C";

    @Test
    void generate_knownInputs_matchesGoldenPayload() {
        String payload =
                PixBrCodeGenerator.generate(
                        "barbeiro@example.com",
                        "SEU BARBEIRO",
                        "SAO PAULO",
                        new BigDecimal("45.00"),
                        "APT123");

        assertThat(payload).isEqualTo(GOLDEN_PAYLOAD);
    }

    @Test
    void generate_startsWithPayloadFormatIndicatorAndContainsCrcFieldMarker() {
        String payload =
                PixBrCodeGenerator.generate(
                        "11999998888", "Barber", "BRASIL", BigDecimal.TEN, "APT1");

        assertThat(payload).startsWith("000201");
        assertThat(payload).contains("6304");
    }

    @Test
    void generate_blankTxId_usesNoReferenceLabelPlaceholder() {
        String payload =
                PixBrCodeGenerator.generate("11999998888", "Barber", "BRASIL", BigDecimal.TEN, null);

        assertThat(payload).contains("62070503***");
    }

    @Test
    void generate_amountIsFormattedWithTwoDecimalsAndDotSeparator() {
        String payload =
                PixBrCodeGenerator.generate(
                        "11999998888", "Barber", "BRASIL", new BigDecimal("7"), "APT1");

        assertThat(payload).contains("54047.00");
    }

    @Test
    void generate_stripsAccentsFromMerchantName() {
        String payload =
                PixBrCodeGenerator.generate(
                        "11999998888", "José Ferreira", "São Paulo", BigDecimal.TEN, "APT1");

        assertThat(payload).contains("Jose Ferreira");
        assertThat(payload).contains("Sao Paulo");
        assertThat(payload).doesNotContain("é").doesNotContain("ã");
    }

    @Test
    void generate_truncatesMerchantNameLongerThan25Chars() {
        String longName = "A".repeat(40);
        String payload =
                PixBrCodeGenerator.generate("11999998888", longName, "BRASIL", BigDecimal.TEN, "APT1");

        assertThat(payload).contains("5925" + "A".repeat(25));
        assertThat(payload).doesNotContain("A".repeat(26));
    }

    @Test
    void generate_sanitizesNonAlphanumericCharactersFromTxId() {
        String payload =
                PixBrCodeGenerator.generate(
                        "11999998888", "Barber", "BRASIL", BigDecimal.TEN, "APT-123/abc");

        assertThat(payload).contains("APT123abc");
        assertThat(payload).doesNotContain("APT-123");
    }

    @Test
    void generate_blankPixKey_throwsIllegalArgument() {
        assertThrows(
                IllegalArgumentException.class,
                () -> PixBrCodeGenerator.generate(" ", "Barber", "BRASIL", BigDecimal.TEN, "APT1"));
    }

    @Test
    void generate_blankMerchantName_throwsIllegalArgument() {
        assertThrows(
                IllegalArgumentException.class,
                () ->
                        PixBrCodeGenerator.generate(
                                "11999998888", " ", "BRASIL", BigDecimal.TEN, "APT1"));
    }

    @Test
    void crc16Hex_matchesKnownVector() {
        String sample =
                "00020126580014br.gov.bcb.pix0136123e4567-e12b-12d1-a456-426655440000"
                        + "5204000053039865802BR5913Fulano de Tal6008BRASILIA62070503***6304";

        assertThat(PixBrCodeGenerator.crc16Hex(sample)).isEqualTo("1D3D");
    }
}
