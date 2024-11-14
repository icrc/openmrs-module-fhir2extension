package org.openmrs.module.fhir2extension.api.translators;/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

import org.hl7.fhir.r4.model.CodeableConcept;
import org.openmrs.module.fhir2.api.translators.OpenmrsFhirTranslator;

public interface FormTranslator<T> extends OpenmrsFhirTranslator<T, CodeableConcept> {

    /**
     * @param form the OpenMRS form to translate
     * @return a list consisting of an encoded version of the OpenMRS form
     */
    @Override
    CodeableConcept toFhirResource(T form);

    /**
     * @param form a list consisting of an encoded version of the OpenMRS form
     * @return the OpenMRS encounter type or visit type
     */
    @Override
    T toOpenmrsType(CodeableConcept form);
}
