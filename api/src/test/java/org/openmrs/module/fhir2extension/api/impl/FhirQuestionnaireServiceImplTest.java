/*
 * This Source Code Form is subject to the terms of the Mozilla Public License,
 * v. 2.0. If a copy of the MPL was not distributed with this file, You can
 * obtain one at http://mozilla.org/MPL/2.0/. OpenMRS is also distributed under
 * the terms of the Healthcare Disclaimer located at http://openmrs.org/license.
 *
 * Copyright (C) OpenMRS Inc. OpenMRS is a registered trademark and the OpenMRS
 * graphic logo is a trademark of OpenMRS Inc.
 */
package org.openmrs.module.fhir2extension.api.impl;

import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.not;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

import java.util.*;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2.api.FhirGlobalPropertyService;
import org.openmrs.module.fhir2.api.search.SearchQueryBundleProvider;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2.api.search.param.SearchParameterMap;

//@RunWith(MockitoJUnitRunner.class)
public class FhirQuestionnaireServiceImplTest {
	
	//		private static final String FORM_UUID = "3434gh32-34h3j4-34jk34-3422h";
	//
	//		@Mock
	//		private FhirGlobalPropertyService globalPropertyService;
	//
	//		@Mock
	//		private SearchQueryInclude<org.hl7.fhir.r4.model.Questionnaire> searchQueryInclude;
	//
	//		private FhirQuestionnaireServiceImpl questionnaireService;
	//
	//		private org.hl7.fhir.r4.model.Questionnaire fhirQuestionnaire;
	//
	//		@Before
	//		public void setUp() {
	//			questionnaireService = new FhirQuestionnaireServiceImpl();
	//
	//			questionnaireService.setSearchQueryInclude(searchQueryInclude);
	//
	//			fhirQuestionnaire = new org.hl7.fhir.r4.model.Questionnaire();
	//			fhirQuestionnaire.setId(FORM_UUID);
	//			fhirQuestionnaire.setName(org.openmrs.module.fhir2extension.FhirConstants.FHIR_QUESTIONNAIRE_TYPE);
	//		}
	//
	//		@Test
	//		public void getQuestionnaireByUuid_shouldRetrieveQuestionnaireByUuid() {
	//			when(dao.get(FORM_UUID)).thenReturn(formResource);
	//			when(questionnaireTranslator.toFhirResource(formResource)).thenReturn(fhirQuestionnaire);
	//
	//			org.hl7.fhir.r4.model.Questionnaire result = questionnaireService.get(FORM_UUID);
	//
	//			assertThat(result, notNullValue());
	//			assertThat(result.getId(), notNullValue());
	//			assertThat(result.getId(), equalTo(FORM_UUID));
	//		}
	//
	//		@Test
	//	    public void getQuestionnaireEverything_shouldReturnAllInformationAboutAllQuestionnaires() {
	//	        SearchParameterMap theParams = new SearchParameterMap().addParameter(FhirConstants.EVERYTHING_SEARCH_HANDLER, "");
	//
	//	        when(dao.getSearchResults(any())).thenReturn(Collections.singletonList(formResource));
	//	        when(searchQuery.getQueryResults(any(), any(), any(), any())).thenReturn(new SearchQueryBundleProvider<>(theParams,
	//	                dao, questionnaireTranslator, globalPropertyService, searchQueryInclude));
	//
	//	        when(questionnaireTranslator.toFhirResource(formResource)).thenReturn(fhirQuestionnaire);
	//
	//	        IBundleProvider results = questionnaireService.getQuestionnaireEverything();
	//	        List<IBaseResource> resultList = get(results);
	//
	//	        assertThat(results, notNullValue());
	//	        assertThat(resultList, not(empty()));
	//	        assertThat(resultList.size(), greaterThanOrEqualTo(1));
	//	    }
	//
	//		private List<IBaseResource> get(IBundleProvider results) {
	//			return results.getResources(0, 10);
	//		}
}
