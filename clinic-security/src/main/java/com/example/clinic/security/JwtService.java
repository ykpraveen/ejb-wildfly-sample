package com.example.clinic.security;

import jakarta.ejb.Stateless;
import jakarta.json.bind.Jsonb;
import jakarta.json.bind.JsonbBuilder;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.NotAuthorizedException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Set;

@Stateless
public class JwtService {
    private static final Base64.Encoder URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder URL_DECODER = Base64.getUrlDecoder();
    private static final String HMAC_ALG = "HmacSHA256";
    private static final String SECRET_ENV = "CLINIC_JWT_SECRET";
    private static final String DEFAULT_SECRET = "dev-only-change-this-secret-for-clinic";

    public String issueToken(String subject, Long clinicId, Set<String> roles) {
        Instant now = Instant.now();
        Instant expiresAt = now.plusSeconds(3600);

        Map<String, Object> header = Map.of("alg", "HS256", "typ", "JWT");
        Map<String, Object> payload = new LinkedHashMap<>();
        payload.put("sub", subject);
        payload.put("clinicId", clinicId);
        payload.put("roles", roles);
        payload.put("iat", now.getEpochSecond());
        payload.put("exp", expiresAt.getEpochSecond());

        String encodedHeader = encodeJson(header);
        String encodedPayload = encodeJson(payload);
        String signature = sign(encodedHeader + "." + encodedPayload);
        return encodedHeader + "." + encodedPayload + "." + signature;
    }

    public JwtPrincipal verify(String token) {
        String[] parts = token.split("\\.");
        if (parts.length != 3) {
            throw new NotAuthorizedException("Invalid token format");
        }

        String signedContent = parts[0] + "." + parts[1];
        String expectedSignature = sign(signedContent);
        if (!MessageDigest.isEqual(expectedSignature.getBytes(StandardCharsets.UTF_8), parts[2].getBytes(StandardCharsets.UTF_8))) {
            throw new NotAuthorizedException("Invalid token signature");
        }

        Map<String, Object> claims = decodeJson(parts[1]);
        long exp = toLong(claims.get("exp"));
        if (Instant.now().getEpochSecond() >= exp) {
            throw new NotAuthorizedException("Token expired");
        }

        Object rolesObj = claims.get("roles");
        Set<String> roles;
        if (rolesObj instanceof java.util.Collection<?> collection) {
            roles = collection.stream().map(String::valueOf).collect(java.util.stream.Collectors.toSet());
        } else {
            throw new BadRequestException("Token roles claim is invalid");
        }

        String username = String.valueOf(claims.get("sub"));
        Long clinicId = toLong(claims.get("clinicId"));
        return new JwtPrincipal(username, clinicId, roles);
    }

    private String encodeJson(Map<String, ?> input) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            return URL_ENCODER.encodeToString(jsonb.toJson(input).getBytes(StandardCharsets.UTF_8));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to encode JWT part", ex);
        }
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> decodeJson(String encoded) {
        try (Jsonb jsonb = JsonbBuilder.create()) {
            byte[] decoded = URL_DECODER.decode(encoded);
            return jsonb.fromJson(new String(decoded, StandardCharsets.UTF_8), Map.class);
        } catch (Exception ex) {
            throw new NotAuthorizedException("Invalid token payload");
        }
    }

    private String sign(String content) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALG);
            SecretKeySpec secretKey = new SecretKeySpec(resolveSecret().getBytes(StandardCharsets.UTF_8), HMAC_ALG);
            mac.init(secretKey);
            return URL_ENCODER.encodeToString(mac.doFinal(content.getBytes(StandardCharsets.UTF_8)));
        } catch (Exception ex) {
            throw new IllegalStateException("Failed to sign JWT", ex);
        }
    }

    private String resolveSecret() {
        String secret = System.getenv(SECRET_ENV);
        if (secret == null || secret.isBlank()) {
            return DEFAULT_SECRET;
        }
        return secret;
    }

    private Long toLong(Object value) {
        if (value instanceof Number n) {
            return n.longValue();
        }
        return Long.parseLong(String.valueOf(value));
    }
}
