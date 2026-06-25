package com.notifyengine.filter;

import com.notifyengine.config.TenantContext;
import com.notifyengine.domain.ApiKey;
import com.notifyengine.domain.Tenant;
import com.notifyengine.repository.ApiKeyRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.OffsetDateTime;
import java.util.HexFormat;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ApiKeyAuthFilterTest {

    @Mock
    private ApiKeyRepository apiKeyRepository;

    @Mock
    private FilterChain filterChain;

    private ApiKeyAuthFilter filter;

    private static final String RAW_KEY = "test-raw-api-key-abc123";
    private static final String KEY_HASH = sha256Hex(RAW_KEY);

    @BeforeEach
    void setUp() {
        filter = new ApiKeyAuthFilter(apiKeyRepository);
    }

    @AfterEach
    void clearContext() {
        TenantContext.clear();
    }

    // Scenario 1: valid active non-expiring key
    @Test
    void validActiveNonExpiringKey_setsContextCallsChainThenClears() throws Exception {
        when(apiKeyRepository.findByKeyHash(KEY_HASH)).thenReturn(Optional.of(apiKey(KEY_HASH, "tenant_acme", true, null)));

        String[] tenantDuringChain = {null};
        doAnswer(inv -> { tenantDuringChain[0] = TenantContext.getTenant(); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(requestWith(RAW_KEY), new MockHttpServletResponse(), filterChain);

        assertThat(tenantDuringChain[0]).isEqualTo("tenant_acme");
        verify(filterChain, times(1)).doFilter(any(), any());
        assertThat(TenantContext.getTenant()).isNull();
    }

    // Scenario 2: valid key with future expiry
    @Test
    void validKeyWithFutureExpiry_setsContextAndCallsChain() throws Exception {
        OffsetDateTime future = OffsetDateTime.now().plusDays(1);
        when(apiKeyRepository.findByKeyHash(KEY_HASH)).thenReturn(Optional.of(apiKey(KEY_HASH, "tenant_acme", true, future)));

        String[] tenantDuringChain = {null};
        doAnswer(inv -> { tenantDuringChain[0] = TenantContext.getTenant(); return null; })
                .when(filterChain).doFilter(any(), any());

        filter.doFilterInternal(requestWith(RAW_KEY), new MockHttpServletResponse(), filterChain);

        assertThat(tenantDuringChain[0]).isEqualTo("tenant_acme");
        verify(filterChain, times(1)).doFilter(any(), any());
    }

    // Scenario 3: missing header
    @Test
    void missingHeader_returns401WithMissingApiKeyBody() throws Exception {
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(new MockHttpServletRequest(), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).isEqualTo("Missing API key");
        verify(filterChain, never()).doFilter(any(), any());
    }

    // Scenario 4: blank header
    @Test
    void blankHeader_returns401WithMissingApiKeyBody() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", "   ");
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(request, response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).isEqualTo("Missing API key");
        verify(filterChain, never()).doFilter(any(), any());
    }

    // Scenario 5: unknown key hash
    @Test
    void unknownHash_returns401WithInvalidApiKeyBody() throws Exception {
        when(apiKeyRepository.findByKeyHash(any())).thenReturn(Optional.empty());
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(requestWith(RAW_KEY), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).isEqualTo("Invalid API key");
        verify(filterChain, never()).doFilter(any(), any());
    }

    // Scenario 6: inactive key
    @Test
    void inactiveKey_returns401WithInvalidApiKeyBody() throws Exception {
        when(apiKeyRepository.findByKeyHash(KEY_HASH)).thenReturn(Optional.of(apiKey(KEY_HASH, "tenant_acme", false, null)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(requestWith(RAW_KEY), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).isEqualTo("Invalid API key");
        verify(filterChain, never()).doFilter(any(), any());
    }

    // Scenario 7: expired key
    @Test
    void expiredKey_returns401WithInvalidApiKeyBody() throws Exception {
        OffsetDateTime past = OffsetDateTime.now().minusDays(1);
        when(apiKeyRepository.findByKeyHash(KEY_HASH)).thenReturn(Optional.of(apiKey(KEY_HASH, "tenant_acme", true, past)));
        MockHttpServletResponse response = new MockHttpServletResponse();

        filter.doFilterInternal(requestWith(RAW_KEY), response, filterChain);

        assertThat(response.getStatus()).isEqualTo(401);
        assertThat(response.getContentAsString()).isEqualTo("Invalid API key");
        verify(filterChain, never()).doFilter(any(), any());
    }

    // Scenario 8: inactive and expired return identical response bodies — no reason leaked
    @Test
    void inactiveAndExpiredKeys_returnExactSameBody() throws Exception {
        String rawKey2 = "another-test-key-xyz";
        String hash2 = sha256Hex(rawKey2);

        when(apiKeyRepository.findByKeyHash(KEY_HASH)).thenReturn(Optional.of(apiKey(KEY_HASH, "tenant_acme", false, null)));
        MockHttpServletResponse inactiveResponse = new MockHttpServletResponse();
        filter.doFilterInternal(requestWith(RAW_KEY), inactiveResponse, filterChain);

        when(apiKeyRepository.findByKeyHash(hash2)).thenReturn(Optional.of(apiKey(hash2, "tenant_acme", true, OffsetDateTime.now().minusDays(1))));
        MockHttpServletResponse expiredResponse = new MockHttpServletResponse();
        filter.doFilterInternal(requestWith(rawKey2), expiredResponse, filterChain);

        assertThat(inactiveResponse.getStatus()).isEqualTo(401);
        assertThat(expiredResponse.getStatus()).isEqualTo(401);
        assertThat(inactiveResponse.getContentAsString())
                .isEqualTo(expiredResponse.getContentAsString());
    }

    // Scenario 9: TenantContext cleared even when downstream throws
    @Test
    void downstreamThrows_contextIsClearedAfterException() throws Exception {
        when(apiKeyRepository.findByKeyHash(KEY_HASH)).thenReturn(Optional.of(apiKey(KEY_HASH, "tenant_acme", true, null)));
        doAnswer(inv -> {
            // proves context WAS set before the exception — null afterward is a real state change
            assertThat(TenantContext.getTenant()).isEqualTo("tenant_acme");
            throw new ServletException("downstream explodes");
        }).when(filterChain).doFilter(any(), any());

        assertThrows(ServletException.class,
                () -> filter.doFilterInternal(requestWith(RAW_KEY), new MockHttpServletResponse(), filterChain));

        assertThat(TenantContext.getTenant()).isNull();
    }

    // Scenario 10: two sequential requests get their own tenant, no cross-request leak
    @Test
    void twoSequentialRequests_eachReceiveCorrectTenantWithNoLeak() throws Exception {
        String rawKey1 = "key-for-tenant-a";
        String rawKey2 = "key-for-tenant-b";
        String hash1 = sha256Hex(rawKey1);
        String hash2 = sha256Hex(rawKey2);

        when(apiKeyRepository.findByKeyHash(hash1)).thenReturn(Optional.of(apiKey(hash1, "tenant_a", true, null)));
        when(apiKeyRepository.findByKeyHash(hash2)).thenReturn(Optional.of(apiKey(hash2, "tenant_b", true, null)));

        String[] tenantA = {null};
        doAnswer(inv -> { tenantA[0] = TenantContext.getTenant(); return null; })
                .when(filterChain).doFilter(any(), any());
        filter.doFilterInternal(requestWith(rawKey1), new MockHttpServletResponse(), filterChain);
        assertThat(tenantA[0]).isEqualTo("tenant_a");

        String[] tenantB = {null};
        doAnswer(inv -> { tenantB[0] = TenantContext.getTenant(); return null; })
                .when(filterChain).doFilter(any(), any());
        filter.doFilterInternal(requestWith(rawKey2), new MockHttpServletResponse(), filterChain);
        assertThat(tenantB[0]).isEqualTo("tenant_b");
        assertThat(tenantB[0]).isNotEqualTo(tenantA[0]);
    }

    // Scenario 11: actuator paths bypass filter entirely
    @Test
    void actuatorPath_shouldNotFilter_andNoDatabaseLookup() throws Exception {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/actuator/health");

        assertThat(filter.shouldNotFilter(request)).isTrue();
        verify(apiKeyRepository, never()).findByKeyHash(any());
    }

    // Scenario 12: swagger-ui paths bypass filter entirely
    @Test
    void swaggerUiPath_shouldNotFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/swagger-ui/index.html");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    // Scenario 13: openapi doc paths bypass filter entirely
    @Test
    void openApiDocsPath_shouldNotFilter() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRequestURI("/v3/api-docs/openapi.json");

        assertThat(filter.shouldNotFilter(request)).isTrue();
    }

    // --- Helpers ---

    private MockHttpServletRequest requestWith(String rawKey) {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.addHeader("X-API-Key", rawKey);
        return request;
    }

    private ApiKey apiKey(String keyHash, String schemaName, boolean active, OffsetDateTime expiresAt) {
        Tenant tenant = new Tenant(UUID.randomUUID(), schemaName, schemaName, "ACTIVE", OffsetDateTime.now());
        return new ApiKey(UUID.randomUUID(), tenant, keyHash, "pfx", active, OffsetDateTime.now(), expiresAt);
    }

    private static String sha256Hex(String input) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(input.getBytes(StandardCharsets.UTF_8));
            return HexFormat.of().formatHex(hash);
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException(e);
        }
    }
}
