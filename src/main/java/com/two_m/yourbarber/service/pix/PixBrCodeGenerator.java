package com.two_m.yourbarber.service.pix;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.text.Normalizer;
import java.util.regex.Pattern;

/**
 * Builds a static Pix "BR Code" payload (EMV(R) QRCPS-MPM), following the Central Bank of
 * Brazil's "Manual do BR Code" v2.0.1. The field layout, lengths and the CRC-16/CCITT-FFFF
 * checksum implemented here were validated against the manual's own worked example.
 */
public final class PixBrCodeGenerator {

    private static final String GUI = "BR.GOV.BCB.PIX";
    private static final String PAYLOAD_FORMAT_INDICATOR = "01";
    private static final String SINGLE_USE_INDICATOR = "12";
    private static final String MERCHANT_CATEGORY_CODE = "0000";
    private static final String TRANSACTION_CURRENCY_BRL = "986";
    private static final String COUNTRY_CODE = "BR";
    private static final String NO_REFERENCE_LABEL = "***";
    private static final int MERCHANT_NAME_MAX_LENGTH = 25;
    private static final int MERCHANT_CITY_MAX_LENGTH = 15;
    private static final int REFERENCE_LABEL_MAX_LENGTH = 25;
    private static final int MAX_FIELD_LENGTH = 99;
    private static final Pattern NON_ASCII_PRINTABLE = Pattern.compile("[^\\x20-\\x7E]");
    private static final Pattern NON_ALPHANUMERIC = Pattern.compile("[^A-Za-z0-9]");

    private PixBrCodeGenerator() {}

    public static String generate(
            String pixKey,
            String merchantName,
            String merchantCity,
            BigDecimal amount,
            String txId) {
        if (pixKey == null || pixKey.isBlank()) {
            throw new IllegalArgumentException("pixKey must not be blank");
        }

        StringBuilder payload = new StringBuilder();
        payload.append(field("00", PAYLOAD_FORMAT_INDICATOR));
        payload.append(field("01", SINGLE_USE_INDICATOR));
        payload.append(field("26", field("00", GUI) + field("01", pixKey.trim())));
        payload.append(field("52", MERCHANT_CATEGORY_CODE));
        payload.append(field("53", TRANSACTION_CURRENCY_BRL));
        if (amount != null) {
            payload.append(
                    field("54", amount.setScale(2, RoundingMode.HALF_UP).toPlainString()));
        }
        payload.append(field("58", COUNTRY_CODE));
        payload.append(field("59", sanitizeRequired(merchantName, MERCHANT_NAME_MAX_LENGTH)));
        payload.append(field("60", sanitizeRequired(merchantCity, MERCHANT_CITY_MAX_LENGTH)));
        payload.append(field("62", field("05", referenceLabel(txId))));
        payload.append("6304");

        return payload + crc16Hex(payload.toString());
    }

    private static String referenceLabel(String txId) {
        if (txId == null || txId.isBlank()) {
            return NO_REFERENCE_LABEL;
        }
        String cleaned = NON_ALPHANUMERIC.matcher(stripAccents(txId)).replaceAll("");
        return cleaned.isBlank()
                ? NO_REFERENCE_LABEL
                : truncate(cleaned, REFERENCE_LABEL_MAX_LENGTH);
    }

    private static String sanitizeRequired(String value, int maxLength) {
        if (value == null || value.isBlank()) {
            throw new IllegalArgumentException("Merchant name/city must not be blank");
        }
        String asciiOnly = NON_ASCII_PRINTABLE.matcher(stripAccents(value)).replaceAll("");
        String trimmed = asciiOnly.trim();
        if (trimmed.isBlank()) {
            throw new IllegalArgumentException(
                    "Merchant name/city must contain printable ASCII characters");
        }
        return truncate(trimmed, maxLength);
    }

    private static String stripAccents(String value) {
        return Normalizer.normalize(value, Normalizer.Form.NFD).replaceAll("\\p{M}", "");
    }

    private static String truncate(String value, int maxLength) {
        return value.length() <= maxLength ? value : value.substring(0, maxLength);
    }

    private static String field(String id, String value) {
        int length = value.getBytes(StandardCharsets.UTF_8).length;
        if (length > MAX_FIELD_LENGTH) {
            throw new IllegalStateException("Field " + id + " exceeds the 99-byte EMV limit");
        }
        return id + String.format("%02d", length) + value;
    }

    static String crc16Hex(String payload) {
        int polynomial = 0x1021;
        int result = 0xFFFF;
        for (byte b : payload.getBytes(StandardCharsets.UTF_8)) {
            result ^= (b & 0xFF) << 8;
            for (int i = 0; i < 8; i++) {
                result = ((result & 0x8000) != 0) ? (result << 1) ^ polynomial : result << 1;
                result &= 0xFFFF;
            }
        }
        return String.format("%04X", result);
    }
}
