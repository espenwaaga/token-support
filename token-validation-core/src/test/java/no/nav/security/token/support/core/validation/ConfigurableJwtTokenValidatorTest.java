package no.nav.security.token.support.core.validation;

import com.nimbusds.jose.jwk.source.RemoteJWKSet;
import com.nimbusds.jwt.JWT;
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException;
import org.junit.jupiter.api.Test;

import java.net.MalformedURLException;
import java.net.URI;
import java.util.Collections;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertThrows;

 class ConfigurableJwtTokenValidatorTest extends AbstractJwtValidatorTest {

    private static final String ISSUER = "https://issuer";

    @Test
     void assertValidToken() throws JwtTokenValidatorException {
        JwtTokenValidator validator = tokenValidator(ISSUER, List.of("aud", "sub"));
        JWT token = createSignedJWT(ISSUER, null, null);
        validator.assertValidToken(token.serialize());
    }

    @Test
     void testAssertUnexpectedIssuer() throws JwtTokenValidatorException {
        String otherIssuer = "https://differentfromtoken";
        JwtTokenValidator validator = tokenValidator(otherIssuer, Collections.emptyList());
        JWT token = createSignedJWT(ISSUER, null, null);
        assertThrows(JwtTokenValidatorException.class, () -> validator.assertValidToken(token.serialize()));
    }

    private ConfigurableJwtTokenValidator tokenValidator(String issuer, List<String> optionalClaims) {
        try {
            return new ConfigurableJwtTokenValidator(
                issuer,
                optionalClaims,
                new RemoteJWKSet<>(URI.create("https://someurl").toURL(), new MockResourceRetriever())
            );
        } catch (MalformedURLException e) {
            throw new RuntimeException(e);
        }
    }
}
