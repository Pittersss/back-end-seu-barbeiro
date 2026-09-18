package com.two_m.yourbarber.service.pix;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class PixKeyNormalizationTest {

    @Test
    void formattedPhone_becomesE164() {
        assertThat(PixBrCodeGenerator.normalizeKey("(11) 98888-7777")).isEqualTo("+5511988887777");
        assertThat(PixBrCodeGenerator.normalizeKey("+55 11 98888-7777")).isEqualTo("+5511988887777");
    }

    @Test
    void formattedCpfAndCnpj_becomeDigitsOnly() {
        assertThat(PixBrCodeGenerator.normalizeKey("123.456.789-00")).isEqualTo("12345678900");
        assertThat(PixBrCodeGenerator.normalizeKey("12.345.678/0001-90")).isEqualTo("12345678000190");
    }

    @Test
    void emailAndRandomKey_passThroughLowercased() {
        assertThat(PixBrCodeGenerator.normalizeKey(" Foo@Bar.com ")).isEqualTo("foo@bar.com");
        assertThat(PixBrCodeGenerator.normalizeKey("123E4567-E89B-12D3-A456-426614174000"))
                .isEqualTo("123e4567-e89b-12d3-a456-426614174000");
    }

    @Test
    void bareDigits_phoneGetsCountryCode_validCpfDoesNot() {
        assertThat(PixBrCodeGenerator.normalizeKey("83991592600")).isEqualTo("+5583991592600");
        assertThat(PixBrCodeGenerator.normalizeKey("8399159260")).isEqualTo("+558399159260");
        assertThat(PixBrCodeGenerator.normalizeKey("5583991592600")).isEqualTo("+5583991592600");
        assertThat(PixBrCodeGenerator.normalizeKey("52998224725")).isEqualTo("52998224725");
        assertThat(PixBrCodeGenerator.normalizeKey("12345678000190")).isEqualTo("12345678000190");
    }
}
