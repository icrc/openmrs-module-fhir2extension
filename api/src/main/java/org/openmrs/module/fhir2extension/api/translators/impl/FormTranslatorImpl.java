package org.openmrs.module.fhir2extension.api.translators.impl;/*
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

import org.hl7.fhir.r4.model.CodeableConcept;
import org.hl7.fhir.r4.model.Coding;
import org.openmrs.Form;
import org.openmrs.api.FormService;
import org.openmrs.module.fhir2extension.api.translators.FormTranslator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import static org.openmrs.module.fhir2.api.util.FhirUtils.getMetadataTranslation;

@Component
public class FormTranslatorImpl implements FormTranslator<Form> {

    public static final String FORM_SYSTEM_URI = "http://fhir.openmrs.org/core/StructureDefinition/omrs-form";

    @Autowired
    private FormService formService;

    @Override
    public CodeableConcept toFhirResource(Form form) {
        if (form == null) {
            return null;
        }

        CodeableConcept code = new CodeableConcept();
        code.addCoding().setSystem(FORM_SYSTEM_URI).setCode(form.getUuid()).setDisplay(getMetadataTranslation(form));

        return code;
    }

    @Override
    public Form toOpenmrsType(CodeableConcept form) {
        if (form == null || !form.hasCoding()) {
            return null;
        }
        Coding coding = form.getCoding().stream()
                .filter(Coding::hasSystem)
                .filter(c -> FORM_SYSTEM_URI.equals(c.getSystem()))
                .findFirst()
                .orElse(null);

        if (coding == null) {
            return null;
        }

        return formService.getFormByUuid(coding.getCode());
    }
}
