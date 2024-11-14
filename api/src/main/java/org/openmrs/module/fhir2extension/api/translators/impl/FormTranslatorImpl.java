package org.openmrs.module.fhir2extension.api.translators.impl;/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
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
