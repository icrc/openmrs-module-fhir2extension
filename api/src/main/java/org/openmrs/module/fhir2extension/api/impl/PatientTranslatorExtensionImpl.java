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

import static org.apache.commons.lang3.Validate.notNull;
import javax.annotation.Nonnull;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import java.util.Objects;
import org.hl7.fhir.r4.model.Patient;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.api.LocationService;
import org.openmrs.api.context.Context;
import org.openmrs.module.fhir2.api.translators.PatientTranslator;
import org.openmrs.module.fhir2.api.translators.impl.PatientTranslatorImpl;

@Primary
@Component
public class PatientTranslatorExtensionImpl extends PatientTranslatorImpl implements PatientTranslator {
	
	@Autowired
	LocationService locationService;
	
	@Override
	public org.openmrs.Patient toOpenmrsType(@Nonnull Patient fhirPatient) {
		notNull(fhirPatient, "The Patient object should not be null");
		
		if (Context.getUserContext().getLocation() == null) {
			String l = extractLocationUuidFromPatient(fhirPatient);
			
			Integer locationId = getLocationIdFromUuid(l);
			if (locationId != null) {
				Context.getUserContext().setLocationId(locationId);
			}
		}
		
		return toOpenmrsType(new org.openmrs.Patient(), fhirPatient);
	}
	
	private String extractLocationUuidFromPatient(Patient patient) {
        if (patient.hasIdentifier()) {
            return patient.getIdentifier().stream().flatMap(identifier -> identifier.getExtension().stream())
                    .filter(
                            extension -> "http://fhir.openmrs.org/ext/patient/identifier#location".equals(extension.getUrl()))
                    .map(extension -> {
                        Reference locationReference = (Reference) extension.getValue();
                        if (locationReference != null && locationReference.getReference() != null) {
                            String reference = locationReference.getReference();
                            return reference.substring(reference.lastIndexOf('/') + 1);
                        }
                        return null;
                    }).filter(Objects::nonNull).findFirst().orElse(null);
        }
        return null;
    }
	
	private Integer getLocationIdFromUuid(String uuid) {
		org.openmrs.Location location = locationService.getLocationByUuid(uuid);
		
		if (location != null) {
			return location.getId();
		}
		return null;
	}
}
