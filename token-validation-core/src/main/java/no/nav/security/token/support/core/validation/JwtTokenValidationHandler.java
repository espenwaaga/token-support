package no.nav.security.token.support.core.validation;

import java.util.AbstractMap;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import no.nav.security.token.support.core.configuration.IssuerConfiguration;
import no.nav.security.token.support.core.configuration.MultiIssuerConfiguration;
import no.nav.security.token.support.core.context.TokenValidationContext;
import no.nav.security.token.support.core.exceptions.IssuerConfigurationException;
import no.nav.security.token.support.core.exceptions.JwtTokenValidatorException;
import no.nav.security.token.support.core.http.HttpRequest;
import no.nav.security.token.support.core.jwt.JwtToken;

public class JwtTokenValidationHandler {

    private static final Logger LOG = LoggerFactory.getLogger(JwtTokenValidationHandler.class);
    private final MultiIssuerConfiguration config;

    public JwtTokenValidationHandler(MultiIssuerConfiguration config) {
        this.config = config;
    }

    public TokenValidationContext getValidatedTokens(HttpRequest request) {

        var tokensOnRequest = JwtTokenRetriever.retrieveUnvalidatedTokens(config, request);

        Map<String, JwtToken> validatedTokens = tokensOnRequest.stream()
                .map(this::validate)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .collect(Collectors.toConcurrentMap(
                        Map.Entry::getKey,
                        Map.Entry::getValue));

        LOG.debug("found {} tokens on request, number of validated tokens is {}", tokensOnRequest.size(), validatedTokens.size());
        if (validatedTokens.isEmpty() && !tokensOnRequest.isEmpty()) {
            LOG.debug("Found {} unvalidated token(s) with issuer(s) {} on request, is this a configuration error?", tokensOnRequest.size(),
                    tokensOnRequest.stream().map(JwtToken::getIssuer).collect(Collectors.toList()));
        }
        return new TokenValidationContext(validatedTokens);
    }

    private Optional<Map.Entry<String, JwtToken>> validate(JwtToken jwtToken) {
        try {
            LOG.debug("check if token with issuer={} is present in config", jwtToken.getIssuer());
            if (config.getIssuer(jwtToken.getIssuer()).isPresent()) {
                var issuerShortName = issuerConfiguration(jwtToken.getIssuer()).getName();
                LOG.debug("found token from trusted issuer={} with shortName={} in request", jwtToken.getIssuer(), issuerShortName);

                long start = System.currentTimeMillis();
                tokenValidator(jwtToken).assertValidToken(jwtToken.getTokenAsString());
                long end = System.currentTimeMillis();

                LOG.debug("validated token from issuer[{}] in {} ms", jwtToken.getIssuer(), (end - start));
                return Optional.of(entry(issuerShortName, jwtToken));
            }
            LOG.debug("token is from an unknown issuer={}, skipping validation.", jwtToken.getIssuer());
            return Optional.empty();

        } catch (JwtTokenValidatorException e) {
            LOG.info(
                    "found invalid token for issuer [{}, expires at {}], exception message:{} ",
                    jwtToken.getIssuer(),
                    e.getExpiryDate(),
                    e.getMessage());
            return Optional.empty();
        }
    }

    private JwtTokenValidator tokenValidator(JwtToken jwtToken) {
        return issuerConfiguration(jwtToken.getIssuer()).getTokenValidator();
    }

    private IssuerConfiguration issuerConfiguration(String issuer) {
        return config.getIssuer(issuer)
                .orElseThrow(() -> new IssuerConfigurationException(String.format("could not find IssuerConfiguration for issuer=%s", issuer)));
    }

    private static <T, U> Map.Entry<T, U> entry(T key, U value) {
        return new AbstractMap.SimpleImmutableEntry<>(key, value);
    }
}
