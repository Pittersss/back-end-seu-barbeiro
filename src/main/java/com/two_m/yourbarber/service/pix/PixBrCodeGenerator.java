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

    /**
     * "11" marks a static QR (payload is fully self-contained, reusable). "12" would mean
     * dynamic, which per the BR Code manual requires a URL in a 26.25 subfield the payer's app
     * fetches for the real payment details — this generator never emits that URL, so declaring
     * "12" made wallets treat the code as a malformed dynamic charge and reject it outright.
     */
    private static final String STATIC_INDICATOR = "11";
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
        payload.append(field("01", STATIC_INDICATOR));
        payload.append(field("26", field("00", GUI) + field("01", normalizeKey(pixKey))));
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

    /**
     * Wallets only accept a key in its canonical form, but barbers type it however they like:
     * "(11) 98888-7777" or "+55 11 98888-7777" for phones, "123.456.789-00" / "12.345.678/0001-90"
     * for CPF/CNPJ. Formatted phones become "+55DDDNUMBER", formatted CPF/CNPJ digits only; emails
     * and random (UUID) keys pass through. Bare digit strings are classified by length and CPF
     * check digits (see {@link #normalizeBareDigits}).
     */
    static String normalizeKey(String rawKey) {
        String key = rawKey.trim();
        if (key.contains("@")) {
            return key.toLowerCase();
        }
        if (key.matches("(?i)[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}")) {
            return key.toLowerCase();
        }
        String digits = key.replaceAll("\\D", "");
        if (key.startsWith("+")) {
            return "+" + digits;
        }
        if (key.contains("(") || key.contains(" ")) {
            return digits.length() <= 11 ? "+55" + digits : "+" + digits;
        }
        if (key.matches("[\\d.\\-/]+")) {
            boolean punctuated = !key.matches("\\d+");
            if (punctuated) {
                return digits;
            }
            return normalizeBareDigits(digits);
        }
        return key;
    }

    /**
     * Bare digits are a phone, CPF or CNPJ. 14 digits is a CNPJ; 11 digits is a CPF only when its
     * check digits are valid, otherwise a Brazilian mobile (DDD + 9 + number) that lacks "+55";
     * 10 digits is a landline; 12-13 digits starting with 55 already carry the country code.
     */
    private static String normalizeBareDigits(String digits) {
        int n = digits.length();
        if (n == 11) {
            return isValidCpf(digits) ? digits : "+55" + digits;
        }
        if (n == 10) {
            return "+55" + digits;
        }
        if ((n == 12 || n == 13) && digits.startsWith("55")) {
            return "+" + digits;
        }
        return digits;
    }

    private static boolean isValidCpf(String d) {
        if (d.chars().distinct().count() == 1) {
            return false;
        }
        for (int len = 9; len <= 10; len++) {
            int sum = 0;
            for (int i = 0; i < len; i++) {
                sum += (d.charAt(i) - '0') * (len + 1 - i);
            }
            int check = (sum * 10) % 11 % 10;
            if (check != d.charAt(len) - '0') {
                return false;
            }
        }
        return true;
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
