/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2extension.providers.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.when;

import java.util.*;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.*;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2extension.api.FhirQuestionnaireService;
import org.openmrs.module.fhir2extension.providers.BaseFhirProvenanceResourceTest;

@RunWith(MockitoJUnitRunner.class)
public class QuestionnaireFhirResourceProviderTest extends BaseFhirProvenanceResourceTest<Questionnaire> {
	
	private static final String QUESTIONNAIRE_UUID = "017312a1-cf56-43ab-ae87-44070b801d1c";
	
	private static final String WRONG_QUESTIONNAIRE_UUID = "017312a1-cf56-43ab-ae87-44070b801d1c";
	
	private static final String FORM_NAME = "Form name";
	
	@Mock
	private FhirQuestionnaireService questionnaireService;
	
	private QuestionnaireFhirResourceProvider resourceProvider;
	
	private Questionnaire questionnaire;
	
	@Before
	public void setup() {
		resourceProvider = new QuestionnaireFhirResourceProvider();
		resourceProvider.setFhirQuestionnaireService(questionnaireService);
	}
	
	@Before
	public void initPatient() {
		questionnaire = new Questionnaire();
		questionnaire.setId(QUESTIONNAIRE_UUID);
		setProvenanceResources(questionnaire);
	}
	
	@Test
	public void getResourceType_shouldReturnResourceType() {
		assertThat(resourceProvider.getResourceType(), equalTo(Questionnaire.class));
		assertThat(resourceProvider.getResourceType().getName(), equalTo(Questionnaire.class.getName()));
	}
	
	private List<IBaseResource> getResources(IBundleProvider result) {
		return result.getResources(0, 10);
	}
	
	private List<IBaseResource> getAllResources(IBundleProvider result) {
		return result.getAllResources();
	}
}
