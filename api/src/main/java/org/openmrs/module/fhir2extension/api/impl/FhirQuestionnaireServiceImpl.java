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

import javax.annotation.Nonnull;

import ca.uhn.fhir.rest.api.PatchTypeEnum;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.api.server.RequestDetails;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import org.hl7.fhir.r4.model.Questionnaire;
import org.openmrs.module.fhir2.api.search.SearchQueryInclude;
import org.openmrs.module.fhir2extension.api.FhirQuestionnaireService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.openmrs.api.AdministrationService;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

@Component
@Transactional
@Getter(AccessLevel.PROTECTED)
@Setter(AccessLevel.PACKAGE)
public class FhirQuestionnaireServiceImpl implements FhirQuestionnaireService {
	
	@Autowired
	private SearchQueryInclude<Questionnaire> searchQueryInclude;
	
	@Autowired
	@Qualifier("adminService")
	private AdministrationService administrationService;
	
	private String questionnairesFolder = administrationService.getGlobalProperty("fhir2extension.questionnaires.folder");
	
	@Override
	public Questionnaire get(@Nonnull String uuid) {
		return QuestionnaireFileUtils.getQuestionnaire(questionnairesFolder, uuid);
	}
	
	@Override
	public List<Questionnaire> get(@Nonnull Collection<String> collection) {
		return collection.stream().map(uuid -> QuestionnaireFileUtils.getQuestionnaire(questionnairesFolder, uuid)).collect(Collectors.toList());
	}
	
	@Override
	public IBundleProvider getQuestionnaireEverything() {
		return new SimpleBundleProvider(QuestionnaireFileUtils.getAllQuestionnaires(questionnairesFolder));
	}
	
	@Override
	public Questionnaire create(@Nonnull Questionnaire questionnaire) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Questionnaire update(@Nonnull String s, @Nonnull Questionnaire questionnaire) {
		throw new UnsupportedOperationException();
	}
	
	@Override
	public Questionnaire patch(@Nonnull String s, @Nonnull PatchTypeEnum patchTypeEnum, @Nonnull String s1,
	        RequestDetails requestDetails) {
		
		throw new UnsupportedOperationException();
	}
	
	@Override
	public void delete(@Nonnull String s) {
		throw new UnsupportedOperationException();
	}
	
}
