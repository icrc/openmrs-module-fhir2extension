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

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Encounter;
import org.openmrs.Form;
import org.openmrs.module.fhir2.api.translators.impl.EncounterTranslatorImpl;
import org.openmrs.module.fhir2extension.FhirConstants;
import org.openmrs.module.fhir2extension.api.translators.FormTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

import javax.annotation.Nonnull;

import static org.apache.commons.lang3.Validate.notNull;

@Primary
@Component
public class EncounterTranslatorExtensionImpl extends EncounterTranslatorImpl {

    @Autowired
    private FormTranslator<Form> formTranslator;

    @Override
    public Encounter toFhirResource(@Nonnull org.openmrs.Encounter openmrsEncounter) {
        notNull(openmrsEncounter, "The Openmrs Encounter object should not be null");

        Encounter encounter = super.toFhirResource(openmrsEncounter);

        return encounter;
    }

    @Override
    public org.openmrs.Encounter toOpenmrsType(@Nonnull Encounter fhirEncounter) {
        notNull(fhirEncounter, "The Encounter object should not be null");
        return this.toOpenmrsType(new org.openmrs.Encounter(), fhirEncounter);
    }

    @Override
    public org.openmrs.Encounter toOpenmrsType(@Nonnull org.openmrs.Encounter existingEncounter,
                                               @Nonnull Encounter encounter) {
        super.toOpenmrsType(existingEncounter, encounter);
        notNull(existingEncounter, "The existing Openmrs Encounter object should not be null");
        notNull(encounter, "The Encounter object should not be null");

        CodeableConcept formType = encounter.getType().stream()
                .filter(codeableConcept -> codeableConcept.getCoding().stream()
                        .anyMatch(coding -> FhirConstants.FORM_SYSTEM_URI.equals(coding.getSystem())))
                .findFirst()
                .orElse(null);

        Form form = formTranslator.toOpenmrsType(formType);

        if (formType != null) {
            notNull(form, "The Form object should not be null");
        }

        existingEncounter.setForm(form);

        return existingEncounter;
    }
}
