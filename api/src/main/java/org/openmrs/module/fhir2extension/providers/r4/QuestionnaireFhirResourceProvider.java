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
package org.openmrs.module.fhir2extension.providers.r4;

import static lombok.AccessLevel.PACKAGE;

import ca.uhn.fhir.model.valueset.BundleTypeEnum;
import ca.uhn.fhir.rest.annotation.*;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import lombok.Setter;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2extension.api.FhirQuestionnaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component("questionnaireFhirR4ResourceProvider")
@R4Provider
@Setter(PACKAGE)
public class QuestionnaireFhirResourceProvider implements IResourceProvider {
	
	@Autowired
	private FhirQuestionnaireService fhirQuestionnaireService;
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Questionnaire.class;
	}
	
	@Read
	@SuppressWarnings("unused")
	public Questionnaire get(@IdParam IdType id) {
		Questionnaire questionnaire = fhirQuestionnaireService.get(id.getIdPart());
		if (questionnaire == null) {
			throw new ResourceNotFoundException("Could not find Questionnaire with Id " + id.getIdPart());
		}
		return questionnaire;
	}
	
	/**
	 * The $everything operation fetches all the information related to all the questionnaires
	 * 
	 * @return a bundle of resources which reference to or are referenced from the questionnaires
	 */
	@Operation(name = "everything", idempotent = true, type = Questionnaire.class, bundleType = BundleTypeEnum.SEARCHSET)
	public IBundleProvider getQuestionnaireEverything() {
		return fhirQuestionnaireService.getQuestionnaireEverything();
	}
	
}
