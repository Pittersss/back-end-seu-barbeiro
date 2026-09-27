package com.two_m.yourbarber.service.push;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigInteger;
import java.nio.file.Files;
import java.nio.file.Path;
import java.security.Security;
import java.util.Base64;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.bouncycastle.jce.ECNamedCurveTable;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.jce.spec.ECNamedCurveParameterSpec;
import org.junit.jupiter.api.Test;

/**
 * A VAPID public/private pair that don't belong together makes every browser push service answer
 * 401/403, and nothing ever reaches the phone. Guards the defaults shipped in
 * application.properties (production overrides them by env var, but they must still be a pair).
 */
class VapidKeyPairTest {

    private static String defaultOf(String properties, String key) {
        Matcher m =
                Pattern.compile("^" + Pattern.quote(key) + "=\\$\\{[A-Z_]+:([^}]*)}", Pattern.MULTILINE)
                        .matcher(properties);
        assertThat(m.find()).as("default for " + key).isTrue();
        return m.group(1);
    }

    @Test
    void defaultVapidKeysFormAValidPair() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        String props = Files.readString(Path.of("src/main/resources/application.properties"));
        byte[] publicKey =
                Base64.getUrlDecoder().decode(defaultOf(props, "webpush.vapid.public-key"));
        byte[] privateKey =
                Base64.getUrlDecoder().decode(defaultOf(props, "webpush.vapid.private-key"));

        ECNamedCurveParameterSpec spec = ECNamedCurveTable.getParameterSpec("prime256v1");
        byte[] derived = spec.getG().multiply(new BigInteger(1, privateKey)).getEncoded(false);

        assertThat(publicKey).hasSize(65);
        assertThat(derived).isEqualTo(publicKey);
    }
}
