/*
 * Copyright 2018-2018 adorsys GmbH & Co KG
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package de.adorsys.psd2.consent.service;

import de.adorsys.psd2.consent.api.ActionStatus;
import de.adorsys.psd2.consent.api.ais.*;
import de.adorsys.psd2.consent.api.service.AisConsentService;
import de.adorsys.psd2.consent.service.security.SecurityDataService;
import de.adorsys.psd2.xs2a.core.consent.ConsentStatus;
import de.adorsys.psd2.xs2a.core.psu.PsuIdData;
import de.adorsys.psd2.xs2a.core.sca.ScaStatus;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.junit.Assert.*;
import static org.mockito.Matchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class AisConsentServiceInternalEncryptedTest {
    private static final String ENCRYPTED_CONSENT_ID = "encrypted consent id";
    private static final String UNDECRYPTABLE_CONSENT_ID = "undecryptable consent id";
    private static final String DECRYPTED_CONSENT_ID = "255574b2-f115-4f3c-8d77-c1897749c060";
    private static final ConsentStatus CONSENT_STATUS = ConsentStatus.RECEIVED;
    private static final String AUTHORISATION_ID = "b3ecf205-da94-4e83-837b-5cd93ab88120";
    private static final ScaStatus SCA_STATUS = ScaStatus.RECEIVED;

    @InjectMocks
    private AisConsentServiceInternalEncrypted aisConsentServiceInternalEncrypted;
    @Mock
    private AisConsentService aisConsentService;
    @Mock
    private SecurityDataService securityDataService;

    @Before
    public void setUp() {
        when(securityDataService.encryptId(DECRYPTED_CONSENT_ID))
            .thenReturn(Optional.of(ENCRYPTED_CONSENT_ID));
        when(securityDataService.decryptId(ENCRYPTED_CONSENT_ID))
            .thenReturn(Optional.of(DECRYPTED_CONSENT_ID));
        when(securityDataService.decryptId(UNDECRYPTABLE_CONSENT_ID))
            .thenReturn(Optional.empty());

        when(aisConsentService.createConsent(any()))
            .thenReturn(Optional.of(DECRYPTED_CONSENT_ID));
        when(aisConsentService.getConsentStatusById(DECRYPTED_CONSENT_ID))
            .thenReturn(Optional.of(CONSENT_STATUS));
        when(aisConsentService.updateConsentStatusById(DECRYPTED_CONSENT_ID, CONSENT_STATUS))
            .thenReturn(true);
        when(aisConsentService.updateAccountAccess(eq(DECRYPTED_CONSENT_ID), any()))
            .thenReturn(Optional.of(DECRYPTED_CONSENT_ID));
        when(aisConsentService.getAisAccountConsentById(DECRYPTED_CONSENT_ID))
            .thenReturn(Optional.of(buildAisAccountConsent()));
        when(aisConsentService.createAuthorization(DECRYPTED_CONSENT_ID, buildAisConsentAuthorisationRequest()))
            .thenReturn(Optional.of(AUTHORISATION_ID));
        when(aisConsentService.getAccountConsentAuthorizationById(AUTHORISATION_ID, DECRYPTED_CONSENT_ID))
            .thenReturn(Optional.of(buildAisConsentAuthorizationResponse()));
        when(aisConsentService.updateConsentAuthorization(AUTHORISATION_ID, buildAisConsentAuthorisationRequest()))
            .thenReturn(true);
        when(aisConsentService.getPsuDataByConsentId(DECRYPTED_CONSENT_ID))
            .thenReturn(Optional.of(buildPsuIdData()));
        when(aisConsentService.getAuthorisationsByConsentId(DECRYPTED_CONSENT_ID))
            .thenReturn(Optional.of(buildAuthorisations()));

        when(aisConsentService.getAuthorisationScaStatus(DECRYPTED_CONSENT_ID, AUTHORISATION_ID))
            .thenReturn(Optional.of(SCA_STATUS));
    }

    @Test
    public void createConsent_success() {
        // Given
        CreateAisConsentRequest request = buildCreateAisConsentRequest();

        // When
        Optional<String> actual = aisConsentServiceInternalEncrypted.createConsent(request);

        // Then
        assertTrue(actual.isPresent());
        assertEquals(ENCRYPTED_CONSENT_ID, actual.get());
        verify(aisConsentService, times(1)).createConsent(request);
    }

    @Test
    public void createConsent_failure_internalServiceFailed() {
        when(aisConsentService.createConsent(any())).thenReturn(Optional.empty());

        // Given
        CreateAisConsentRequest request = buildCreateAisConsentRequest();

        // When
        Optional<String> actual = aisConsentServiceInternalEncrypted.createConsent(request);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, times(1)).createConsent(request);
    }

    @Test
    public void createConsent_failure_encryptionFailed() {
        when(aisConsentService.createConsent(any())).thenReturn(Optional.empty());

        // Given
        CreateAisConsentRequest request = buildCreateAisConsentRequest();

        // When
        Optional<String> actual = aisConsentServiceInternalEncrypted.createConsent(request);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, times(1)).createConsent(request);
    }

    @Test
    public void getConsentStatusById_success() {
        // When
        Optional<ConsentStatus> actual = aisConsentServiceInternalEncrypted.getConsentStatusById(ENCRYPTED_CONSENT_ID);

        // Then
        assertTrue(actual.isPresent());

        assertEquals(CONSENT_STATUS, actual.get());
        verify(aisConsentService, times(1)).getConsentStatusById(DECRYPTED_CONSENT_ID);
    }

    @Test
    public void getConsentStatusById_internalServiceFailed() {
        when(aisConsentService.getConsentStatusById(any())).thenReturn(Optional.empty());

        // When
        Optional<ConsentStatus> actual = aisConsentServiceInternalEncrypted.getConsentStatusById(ENCRYPTED_CONSENT_ID);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, times(1)).getConsentStatusById(DECRYPTED_CONSENT_ID);
    }

    @Test
    public void getConsentStatusById_decryptionFailed() {
        // When
        Optional<ConsentStatus> actual = aisConsentServiceInternalEncrypted.getConsentStatusById(UNDECRYPTABLE_CONSENT_ID);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, never()).getConsentStatusById(any());
    }

    @Test
    public void updateConsentStatusById_success() {
        // When
        boolean actual = aisConsentServiceInternalEncrypted.updateConsentStatusById(ENCRYPTED_CONSENT_ID, CONSENT_STATUS);

        // Then
        assertTrue(actual);
        verify(aisConsentService, times(1)).updateConsentStatusById(DECRYPTED_CONSENT_ID, CONSENT_STATUS);
    }

    @Test
    public void updateConsentStatusById_internalServiceFailed() {
        when(aisConsentService.updateConsentStatusById(any(), any())).thenReturn(false);

        // When
        boolean actual = aisConsentServiceInternalEncrypted.updateConsentStatusById(ENCRYPTED_CONSENT_ID, CONSENT_STATUS);

        // Then
        assertFalse(actual);
        verify(aisConsentService, times(1)).updateConsentStatusById(DECRYPTED_CONSENT_ID, CONSENT_STATUS);
    }

    @Test
    public void updateConsentStatusById_decryptionFailed() {
        // When
        boolean actual = aisConsentServiceInternalEncrypted.updateConsentStatusById(UNDECRYPTABLE_CONSENT_ID, CONSENT_STATUS);

        // Then
        assertFalse(actual);
        verify(aisConsentService, never()).updateConsentStatusById(any(), any());
    }

    @Test
    public void getAisAccountConsentById_success() {
        // Given
        AisAccountConsent expected = buildAisAccountConsent();

        // When
        Optional<AisAccountConsent> actual = aisConsentServiceInternalEncrypted.getAisAccountConsentById(ENCRYPTED_CONSENT_ID);

        // Then
        assertTrue(actual.isPresent());

        assertEquals(expected, actual.get());
        verify(aisConsentService, times(1)).getAisAccountConsentById(DECRYPTED_CONSENT_ID);
    }

    @Test
    public void getAisAccountConsentById_internalServiceFailed() {
        when(aisConsentService.getAisAccountConsentById(any())).thenReturn(Optional.empty());

        // When
        Optional<AisAccountConsent> actual = aisConsentServiceInternalEncrypted.getAisAccountConsentById(ENCRYPTED_CONSENT_ID);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, times(1)).getAisAccountConsentById(DECRYPTED_CONSENT_ID);
    }

    @Test
    public void getAisAccountConsentById_decryptionFailed() {
        // When
        Optional<AisAccountConsent> actual = aisConsentServiceInternalEncrypted.getAisAccountConsentById(UNDECRYPTABLE_CONSENT_ID);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, never()).getAisAccountConsentById(any());
    }

    @Test
    public void checkConsentAndSaveActionLog_success() {
        // Given
        AisConsentActionRequest request = buildAisActionRequest(ENCRYPTED_CONSENT_ID);

        // When
        aisConsentServiceInternalEncrypted.checkConsentAndSaveActionLog(request);

        // Then
        AisConsentActionRequest decryptedRequest = buildAisActionRequest(DECRYPTED_CONSENT_ID);
        verify(aisConsentService, times(1)).checkConsentAndSaveActionLog(decryptedRequest);
    }

    @Test
    public void checkConsentAndSaveActionLog_decryptionFailed() {
        // Given
        AisConsentActionRequest request = buildAisActionRequest(UNDECRYPTABLE_CONSENT_ID);

        // When
        aisConsentServiceInternalEncrypted.checkConsentAndSaveActionLog(request);

        // Then
        verify(aisConsentService, never()).checkConsentAndSaveActionLog(any());
    }

    @Test
    public void updateAccountAccess_success() {
        // Given
        AisAccountAccessInfo accountAccessInfo = buildAisAccountAccessInfo();

        // When
        Optional<String> actual = aisConsentServiceInternalEncrypted.updateAccountAccess(ENCRYPTED_CONSENT_ID, accountAccessInfo);

        // Then
        assertTrue(actual.isPresent());

        assertEquals(ENCRYPTED_CONSENT_ID, actual.get());
        verify(aisConsentService, times(1)).updateAccountAccess(DECRYPTED_CONSENT_ID, accountAccessInfo);
    }

    @Test
    public void updateAccountAccess_internalServiceFailed() {
        when(aisConsentService.updateAccountAccess(any(), any())).thenReturn(Optional.empty());

        // Given
        AisAccountAccessInfo accountAccessInfo = buildAisAccountAccessInfo();

        // When
        Optional<String> actual = aisConsentServiceInternalEncrypted.updateAccountAccess(ENCRYPTED_CONSENT_ID, accountAccessInfo);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, times(1)).updateAccountAccess(DECRYPTED_CONSENT_ID, accountAccessInfo);
    }

    @Test
    public void updateAccountAccess_decryptionFailed() {
        // Given
        AisAccountAccessInfo accountAccessInfo = buildAisAccountAccessInfo();

        // When
        Optional<String> actual = aisConsentServiceInternalEncrypted.updateAccountAccess(UNDECRYPTABLE_CONSENT_ID, accountAccessInfo);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, never()).updateAccountAccess(any(), any());
    }

    @Test
    public void createAuthorization_success() {
        // Given
        AisConsentAuthorizationRequest request = buildAisConsentAuthorisationRequest();

        // When
        Optional<String> actual = aisConsentServiceInternalEncrypted.createAuthorization(ENCRYPTED_CONSENT_ID, request);

        // Then
        assertTrue(actual.isPresent());

        assertEquals(AUTHORISATION_ID, actual.get());
        verify(aisConsentService, times(1)).createAuthorization(DECRYPTED_CONSENT_ID, request);
    }

    @Test
    public void createAuthorization_internalServiceFailed() {
        when(aisConsentService.createAuthorization(any(), any())).thenReturn(Optional.empty());

        // Given
        AisConsentAuthorizationRequest request = buildAisConsentAuthorisationRequest();

        // When
        Optional<String> actual = aisConsentServiceInternalEncrypted.createAuthorization(ENCRYPTED_CONSENT_ID, request);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, times(1)).createAuthorization(DECRYPTED_CONSENT_ID, request);
    }

    @Test
    public void createAuthorization_decryptionFailed() {
        // Given
        AisConsentAuthorizationRequest request = buildAisConsentAuthorisationRequest();

        // When
        Optional<String> actual = aisConsentServiceInternalEncrypted.createAuthorization(UNDECRYPTABLE_CONSENT_ID, request);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, never()).createAuthorization(any(), any());
    }

    @Test
    public void getAccountConsentAuthorizationById_success() {
        // Given
        AisConsentAuthorizationResponse expected = buildAisConsentAuthorizationResponse();

        // When
        Optional<AisConsentAuthorizationResponse> actual = aisConsentServiceInternalEncrypted.getAccountConsentAuthorizationById(AUTHORISATION_ID, ENCRYPTED_CONSENT_ID);

        // Then
        assertTrue(actual.isPresent());

        assertEquals(expected, actual.get());
        verify(aisConsentService, times(1)).getAccountConsentAuthorizationById(AUTHORISATION_ID, DECRYPTED_CONSENT_ID);
    }

    @Test
    public void getAccountConsentAuthorizationById_internalServiceFailed() {
        when(aisConsentService.getAccountConsentAuthorizationById(any(), any())).thenReturn(Optional.empty());

        // When
        Optional<AisConsentAuthorizationResponse> actual = aisConsentServiceInternalEncrypted.getAccountConsentAuthorizationById(AUTHORISATION_ID, ENCRYPTED_CONSENT_ID);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, times(1)).getAccountConsentAuthorizationById(AUTHORISATION_ID, DECRYPTED_CONSENT_ID);
    }

    @Test
    public void getAccountConsentAuthorizationById_decryptionFailed() {
        // When
        Optional<AisConsentAuthorizationResponse> actual = aisConsentServiceInternalEncrypted.getAccountConsentAuthorizationById(AUTHORISATION_ID, UNDECRYPTABLE_CONSENT_ID);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, never()).getAccountConsentAuthorizationById(any(), any());
    }

    @Test
    public void updateConsentAuthorization_success() {
        // Given
        AisConsentAuthorizationRequest request = buildAisConsentAuthorisationRequest();

        // When
        boolean actual = aisConsentServiceInternalEncrypted.updateConsentAuthorization(AUTHORISATION_ID, request);

        // Then
        assertTrue(actual);
        verify(aisConsentService, times(1)).updateConsentAuthorization(AUTHORISATION_ID, request);
    }

    @Test
    public void updateConsentAuthorization_internalServiceFailed() {
        when(aisConsentService.updateConsentAuthorization(any(), any())).thenReturn(false);

        // Given
        AisConsentAuthorizationRequest request = buildAisConsentAuthorisationRequest();

        // When
        boolean actual = aisConsentServiceInternalEncrypted.updateConsentAuthorization(AUTHORISATION_ID, request);

        // Then
        assertFalse(actual);
        verify(aisConsentService, times(1)).updateConsentAuthorization(AUTHORISATION_ID, request);
    }

    @Test
    public void getPsuDataByConsentId_success() {
        // Given
        PsuIdData expected = buildPsuIdData();

        // When
        Optional<PsuIdData> actual = aisConsentServiceInternalEncrypted.getPsuDataByConsentId(ENCRYPTED_CONSENT_ID);

        // Then
        assertTrue(actual.isPresent());

        assertEquals(expected, actual.get());
        verify(aisConsentService, times(1)).getPsuDataByConsentId(DECRYPTED_CONSENT_ID);
    }

    @Test
    public void getPsuDataByConsentId_internalServiceFailed() {
        when(aisConsentService.getPsuDataByConsentId(any())).thenReturn(Optional.empty());

        // When
        Optional<PsuIdData> actual = aisConsentServiceInternalEncrypted.getPsuDataByConsentId(ENCRYPTED_CONSENT_ID);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, times(1)).getPsuDataByConsentId(DECRYPTED_CONSENT_ID);
    }

    @Test
    public void getPsuDataByConsentId_decryptionFailed() {
        // When
        Optional<PsuIdData> actual = aisConsentServiceInternalEncrypted.getPsuDataByConsentId(UNDECRYPTABLE_CONSENT_ID);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, never()).getPsuDataByConsentId(any());
    }

    @Test
    public void getAuthorisationsByConsentId_success() {
        // Given
        List<String> expected = buildAuthorisations();

        // When
        Optional<List<String>> actual = aisConsentServiceInternalEncrypted.getAuthorisationsByConsentId(ENCRYPTED_CONSENT_ID);

        // Then
        assertTrue(actual.isPresent());

        assertEquals(expected, actual.get());
        verify(aisConsentService, times(1)).getAuthorisationsByConsentId(DECRYPTED_CONSENT_ID);
    }

    @Test
    public void getAuthorisationsByConsentId_internalServiceFailed() {
        when(aisConsentService.getAuthorisationsByConsentId(any())).thenReturn(Optional.empty());

        // When
        Optional<List<String>> actual = aisConsentServiceInternalEncrypted.getAuthorisationsByConsentId(ENCRYPTED_CONSENT_ID);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, times(1)).getAuthorisationsByConsentId(DECRYPTED_CONSENT_ID);
    }

    @Test
    public void getAuthorisationsByConsentId_decryptionFailed() {
        // When
        Optional<List<String>> actual = aisConsentServiceInternalEncrypted.getAuthorisationsByConsentId(UNDECRYPTABLE_CONSENT_ID);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, never()).getAuthorisationsByConsentId(any());
    }

    @Test
    public void getAuthorisationScaStatus_success() {
        // When
        Optional<ScaStatus> actual = aisConsentServiceInternalEncrypted.getAuthorisationScaStatus(ENCRYPTED_CONSENT_ID, AUTHORISATION_ID);

        // Then
        assertTrue(actual.isPresent());
        assertEquals(SCA_STATUS, actual.get());
        verify(aisConsentService, times(1)).getAuthorisationScaStatus(DECRYPTED_CONSENT_ID, AUTHORISATION_ID);
    }

    @Test
    public void getAuthorisationScaStatus_internalServiceFailed() {
        when(aisConsentService.getAuthorisationScaStatus(anyString(), anyString())).thenReturn(Optional.empty());

        // When
        Optional<ScaStatus> actual = aisConsentServiceInternalEncrypted.getAuthorisationScaStatus(ENCRYPTED_CONSENT_ID, AUTHORISATION_ID);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, times(1)).getAuthorisationScaStatus(DECRYPTED_CONSENT_ID, AUTHORISATION_ID);
    }

    @Test
    public void getAuthorisationScaStatus_decryptionFailed() {
        // When
        Optional<ScaStatus> actual = aisConsentServiceInternalEncrypted.getAuthorisationScaStatus(UNDECRYPTABLE_CONSENT_ID, AUTHORISATION_ID);

        // Then
        assertFalse(actual.isPresent());
        verify(aisConsentService, never()).getAuthorisationsByConsentId(any());
    }

    private CreateAisConsentRequest buildCreateAisConsentRequest() {
        return new CreateAisConsentRequest();
    }

    private AisAccountConsent buildAisAccountConsent() {
        return new AisAccountConsent();
    }

    private AisConsentActionRequest buildAisActionRequest(String consentId) {
        return new AisConsentActionRequest("tpp id", consentId, ActionStatus.SUCCESS);
    }

    private AisAccountAccessInfo buildAisAccountAccessInfo() {
        return new AisAccountAccessInfo();
    }

    private AisConsentAuthorizationRequest buildAisConsentAuthorisationRequest() {
        return new AisConsentAuthorizationRequest();
    }

    private AisConsentAuthorizationResponse buildAisConsentAuthorizationResponse() {
        return new AisConsentAuthorizationResponse();
    }

    private PsuIdData buildPsuIdData() {
        return new PsuIdData(null, null, null, null);
    }

    private List<String> buildAuthorisations() {
        return Collections.singletonList(AUTHORISATION_ID);
    }
}
