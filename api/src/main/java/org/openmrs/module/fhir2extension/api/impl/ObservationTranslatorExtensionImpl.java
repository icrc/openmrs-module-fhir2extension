/*
 * with Copyright 2024 ICRC
 *
 * BSD 3-Clause License
 *
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions are met:
 *
 * 1. Redistributions of source code must retain the above copyright notice, this
 *    list of conditions and the following disclaimer.
 *
 * 2. Redistributions in binary form must reproduce the above copyright notice,
 *    this list of conditions and the following disclaimer in the documentation
 *    and/or other materials provided with the distribution.
 *
 * 3. Neither the name of the copyright holder nor the names of its
 *    contributors may be used to endorse or promote products derived from
 *    this software without specific prior written permission.
 *
 * THIS SOFTWARE IS PROVIDED BY THE COPYRIGHT HOLDERS AND CONTRIBUTORS "AS IS"
 * AND ANY EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE
 * IMPLIED WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL THE COPYRIGHT HOLDER OR CONTRIBUTORS BE LIABLE
 * FOR ANY DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL
 * DAMAGES (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR
 * SERVICES; LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER
 * CAUSED AND ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY,
 * OR TORT (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE
 * OF THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */
package org.openmrs.module.fhir2extension.api.impl;

import org.hl7.fhir.r4.model.Observation;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir2.api.translators.ObservationReferenceTranslator;
import org.openmrs.module.fhir2.api.translators.ObservationValueTranslator;
import org.openmrs.module.fhir2.api.translators.impl.ObservationTranslatorImpl;
import org.openmrs.module.fhir2extension.api.utils.ObsGroupHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import java.util.function.Supplier;

import static org.apache.commons.lang3.Validate.notNull;

@Primary
@Component
public class ObservationTranslatorExtensionImpl extends ObservationTranslatorImpl {

    @Autowired
    private ObsGroupHelper obsGroupHelper;

    @Autowired
    private ObservationValueTranslator observationValueTranslator;

    @Autowired
    private ObservationReferenceTranslator observationReferenceTranslator;

    @Autowired
    private ObsService obsService;

    @Override
    public Observation toFhirResource(@Nonnull Obs observation) {
        notNull(observation, "The Obs object should not be null");
        Observation fhirObservation = super.toFhirResource(observation);

        if (observation.getObsGroup() != null) {
            Reference parentRef = observationReferenceTranslator.toFhirResource(observation.getObsGroup());
            if (parentRef != null) {
                fhirObservation.addPartOf(parentRef);
            }
        }

        return fhirObservation;
    }

    public Obs toOpenmrsType(@Nonnull Observation fhirObservation) {
        notNull(fhirObservation, "The Observation object should not be null");

        Obs obs = super.toOpenmrsType(new Obs(), fhirObservation);

        if (!fhirObservation.hasValue()) {
            obsGroupHelper.setObsGroupDefaultValue(obs);
        }

        if (fhirObservation.hasPartOf()) {
            for (Reference reference : fhirObservation.getPartOf()) {
                Obs parentObs = observationReferenceTranslator.toOpenmrsType(reference);
                if (parentObs != null) {
                    obs.setObsGroup(parentObs);
                    parentObs.addGroupMember(obs);
                    return obs;
                }
            }
        }

        return obsService.saveObs(obs, "Created observation");
    }

    @Override
    public Obs toOpenmrsType(Obs existingObs, Observation observation, Supplier<Obs> groupedObsFactory) {
        notNull(existingObs, "The existing Obs object should not be null");
        notNull(observation, "The Observation object should not be null");

        for (Reference reference : observation.getHasMember()) {
            Obs childObservation = observationReferenceTranslator.toOpenmrsType(reference);
            if (childObservation.getObsGroup() == null) {
                obsGroupHelper.voidAndAddToGroupNewObservation(existingObs, childObservation);
            } else {
                existingObs.addGroupMember(childObservation);
            }
        }

        return super.toOpenmrsType(existingObs, observation, groupedObsFactory);
    }

}
