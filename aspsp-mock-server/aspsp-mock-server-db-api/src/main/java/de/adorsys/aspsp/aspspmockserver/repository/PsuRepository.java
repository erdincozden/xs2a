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

package de.adorsys.aspsp.aspspmockserver.repository;

import de.adorsys.aspsp.aspspmockserver.domain.spi.psu.PsuPO;

import java.util.List;
import java.util.Optional;

public interface PsuRepository {

    Optional<PsuPO> findOne(String psuId);

    List<PsuPO> findAll();

    Optional<PsuPO> findPsuByAccountDetailsList_Iban(String iban);

    Optional<PsuPO> findByPsuId(String psuId);

    Optional<PsuPO> findPsuByAccountDetailsList_Id(String accountId);

    PsuPO save(PsuPO psu);

    boolean exists(String aspspPsuId);

    void delete(String aspspPsuId);

    void deleteAll();
}
