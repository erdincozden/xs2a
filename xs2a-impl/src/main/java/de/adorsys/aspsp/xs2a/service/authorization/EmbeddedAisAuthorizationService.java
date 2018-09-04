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

package de.adorsys.aspsp.xs2a.service.authorization;

import de.adorsys.aspsp.xs2a.domain.consent.ConsentAuthorizationResponseLinkType;
import de.adorsys.aspsp.xs2a.domain.consent.CreateConsentAuthorizationResponse;
import de.adorsys.aspsp.xs2a.domain.consent.UpdateConsentPsuDataReq;
import de.adorsys.aspsp.xs2a.domain.consent.UpdateConsentPsuDataResponse;
import de.adorsys.aspsp.xs2a.service.consent.ais.AisConsentService;
import de.adorsys.aspsp.xs2a.spi.domain.SpiResponse;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiAccountConsentAuthorization;
import de.adorsys.aspsp.xs2a.spi.domain.account.SpiScaMethod;
import de.adorsys.aspsp.xs2a.spi.domain.consent.SpiScaStatus;
import de.adorsys.aspsp.xs2a.spi.service.AccountSpi;
import de.adorsys.psd2.model.ScaStatus;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

import static de.adorsys.aspsp.xs2a.domain.consent.ConsentAuthorizationResponseLinkType.*;

@Service
public class EmbeddedAisAuthorizationService implements AisAuthorizationService {
    @Autowired
    private AccountSpi accountSpi;
    @Autowired
    private AisConsentService aisConsentService;

    @Override
    public Optional<CreateConsentAuthorizationResponse> createConsentAuthorization(String psuId, String consentId) {
        return StringUtils.isBlank(psuId)
                   ? createConsentAuthorizationAndGetResponse(ScaStatus.RECEIVED, START_AUTHORISATION_WITH_PSU_IDENTIFICATION, consentId)
                   : createConsentAuthorizationAndGetResponse(ScaStatus.PSUAUTHENTICATED, START_AUTHORISATION_WITH_PSU_AUTHENTICATION, consentId);
    }

    private Optional<CreateConsentAuthorizationResponse> createConsentAuthorizationAndGetResponse(ScaStatus scaStatus, ConsentAuthorizationResponseLinkType linkType, String consentId) {
        return aisConsentService.createConsentAuthorization(consentId, SpiScaStatus.valueOf(scaStatus.toString()))
                   .map(authId -> {
                       CreateConsentAuthorizationResponse resp = new CreateConsentAuthorizationResponse();
                       resp.setConsentId(consentId);
                       resp.setAuthorizationId(authId);
                       resp.setScaStatus(scaStatus);
                       resp.setResponseLinkType(linkType);

                       return resp;
                   });
    }

    @Override
    public UpdateConsentPsuDataResponse updateConsentPsuData(UpdateConsentPsuDataReq updatePsuData, SpiAccountConsentAuthorization consentAuthorization) {
        UpdateConsentPsuDataResponse response = new UpdateConsentPsuDataResponse();

        if (checkPsuIdentification(updatePsuData, response)) {
            return response;
        }

        if (checkPsuAuthentication(updatePsuData, response, consentAuthorization)) {
            return response;
        }

        if (checkScaMethod(updatePsuData, response, consentAuthorization)) {
            return response;
        }

        if (checkScaAuthenticationData(updatePsuData, response)) {
            return response;
        }

        return response;

    }

    private boolean checkPsuIdentification(UpdateConsentPsuDataReq updatePsuData, UpdateConsentPsuDataResponse response) {
        if (updatePsuData.isUpdatePsuIdentification()) {
            response.setPsuId(updatePsuData.getPsuId());
            response.setScaStatus(ScaStatus.PSUIDENTIFIED);
            response.setResponseLinkType(START_AUTHORISATION_WITH_PSU_AUTHENTICATION);
            return true;
        }
        return false;
    }

    private boolean checkPsuAuthentication(UpdateConsentPsuDataReq updatePsuData, UpdateConsentPsuDataResponse response, SpiAccountConsentAuthorization spiAuthorization) {
        if (spiAuthorization.getPassword() == null && updatePsuData.getPassword() != null) {
            response.setPassword(updatePsuData.getPassword());

            SpiResponse<List<SpiScaMethod>> spiResponse = accountSpi.readAvailableScaMethods(spiAuthorization.getPsuId(), spiAuthorization.getPassword());
            if (spiResponse.getPayload().size() > 1) {
                response.setScaStatus(ScaStatus.PSUAUTHENTICATED);
                response.setResponseLinkType(START_AUTHORISATION_WITH_AUTHENTICATION_METHOD_SELECTION);
                return true;
            } else {
                response.setAuthenticationMethodId(spiResponse.getPayload().get(0).getId());
                response.setScaStatus(ScaStatus.SCAMETHODSELECTED);
                response.setResponseLinkType(START_AUTHORISATION_WITH_TRANSACTION_AUTHORISATION);
                return true;
            }
        }
        return false;
    }

    private boolean checkScaMethod(UpdateConsentPsuDataReq updatePsuData, UpdateConsentPsuDataResponse response, SpiAccountConsentAuthorization spiAuthorization) {
        if (spiAuthorization.getAuthenticationMethodId() == null && updatePsuData.getAuthenticationMethodId() != null) {
            response.setAuthenticationMethodId(updatePsuData.getAuthenticationMethodId());
            response.setScaStatus(ScaStatus.SCAMETHODSELECTED);
            response.setResponseLinkType(START_AUTHORISATION_WITH_TRANSACTION_AUTHORISATION);
            return true;
        }
        return false;
    }

    private boolean checkScaAuthenticationData(UpdateConsentPsuDataReq updatePsuData, UpdateConsentPsuDataResponse response) {
        if (updatePsuData.getScaAuthenticationData() != null) {
            response.setScaAuthenticationData(updatePsuData.getScaAuthenticationData());
            response.setScaStatus(ScaStatus.STARTED);
            response.setResponseLinkType(START_AUTHORISATION_WITH_AUTHENTICATION_METHOD_SELECTION);
            return true;
        }
        return false;
    }


}
