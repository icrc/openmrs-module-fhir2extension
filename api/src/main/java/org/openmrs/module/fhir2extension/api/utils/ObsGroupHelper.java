package org.openmrs.module.fhir2extension.api.utils;

/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */

import lombok.Setter;
import org.openmrs.Obs;
import org.openmrs.api.ObsService;
import org.openmrs.module.fhir2.api.dao.FhirObservationDao;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
@Setter
public class ObsGroupHelper {

    @Autowired
    private ObsService obsService;

    @Autowired
    private FhirObservationDao observationDao;

    public void voidAndAddToGroupNewObservation(Obs obsGroup, Obs oldObservation) {
        Obs newObservation = Obs.newInstance(oldObservation);

        //void oldObservation
        obsService.voidObs(oldObservation, "voided while translating Observation to add to Obs Group");

        //Add to group new observation
        newObservation.setObsGroup(obsGroup);
        obsGroup.addGroupMember(newObservation);
    }

    public void setObsGroupDefaultValue(Obs obs) {
        if (obs.getConcept() != null && obs.getConcept().getDatatype() != null) {
            String hl7Abbreviation = obs.getConcept().getDatatype().getHl7Abbreviation();
            switch (hl7Abbreviation) {
                case "CWE":
                case "C":
                case "ZZ":
                    obs.setValueCoded(org.openmrs.api.context.Context.getConceptService().getConceptByUuid(
                            "1065AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA"));
                    break;
                case "ST":
                case "TEXT":
                    obs.setValueText("");
                    break;
                case "NM":
                case "NUMERIC":
                    obs.setValueNumeric(0d);
                    break;
                default:
                    break;
            }
        }

    }

}
