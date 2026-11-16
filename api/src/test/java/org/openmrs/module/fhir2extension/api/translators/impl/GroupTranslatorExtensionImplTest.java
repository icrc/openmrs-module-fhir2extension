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
package org.openmrs.module.fhir2extension.api.translators.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Date;

import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Cohort;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortMember;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.fhir2.api.translators.GroupMemberTranslator;
import org.openmrs.module.fhir2.api.translators.PractitionerReferenceTranslator;
import org.openmrs.module.fhir2.model.GroupMember;
import org.openmrs.module.fhir2extension.FhirConstants;

@RunWith(MockitoJUnitRunner.Silent.class)
public class GroupTranslatorExtensionImplTest {
	
	private static final String UUID = "11111111-1111-1111-1111-111111111111";
	
	@Mock
	private CohortService cohortService;
	
	@SuppressWarnings("unchecked")
	private final PractitionerReferenceTranslator<User> practitioners = mock(PractitionerReferenceTranslator.class);
	
	private final GroupMemberTranslator memberTranslator = mock(GroupMemberTranslator.class);
	
	private GroupTranslatorExtensionImpl translator;
	
	@Before
	public void setUp() {
		translator = new GroupTranslatorExtensionImpl() {
			
			{
				setPractitionerReferenceTranslator(practitioners);
				setGroupMemberTranslator(memberTranslator);
			}
		};
		translator.setCohortService(cohortService);
		translator.setMapper(new GroupCohortMapper());
		when(practitioners.toFhirResource(org.mockito.ArgumentMatchers.any())).thenReturn(new Reference("Practitioner/p"));
		GroupMember voidedMember = new GroupMember(new Reference("Patient/voided"));
		when(memberTranslator.toFhirResource(1)).thenReturn(voidedMember);
	}
	
	@Test
	public void toFhirResource_shouldListOnlyTheActiveMembersAndAddTheDetails() {
		Cohort core = core();
		core.addMember(1);
		CohortM own = new CohortM();
		own.setUuid(UUID);
		own.setDateCreated(new Date(1700000000000L));
		CohortMember active = new CohortMember(patient("aaaaaaaa-0000-0000-0000-000000000000"));
		active.setStartDate(new Date(System.currentTimeMillis() - 86400000L));
		active.setCohort(own);
		own.getCohortMembers().add(active);
		CohortMember voided = new CohortMember(patient("bbbbbbbb-0000-0000-0000-000000000000"));
		voided.setVoided(true);
		voided.setCohort(own);
		own.getCohortMembers().add(voided);
		when(cohortService.getCohortMByUuid(UUID)).thenReturn(own);
		
		Group group = translator.toFhirResource(core);
		
		assertThat(group.getMember(), hasSize(1));
		assertThat(group.getMember().get(0).getEntity().getReference(),
		    equalTo("Patient/aaaaaaaa-0000-0000-0000-000000000000"));
		assertThat(group.getQuantity(), equalTo(1));
		assertThat(group.getExtensionByUrl(FhirConstants.GROUP_DATE_CREATED_EXTENSION_URL).hasValue(), equalTo(true));
		assertThat(group.getManagingEntity().getReference(), equalTo("Practitioner/p"));
	}
	
	@Test
	public void toFhirResource_shouldAnswerWhatFhir2AnswersForACohortTheModuleDoesNotKnow() {
		Cohort core = core();
		when(cohortService.getCohortMByUuid(UUID)).thenReturn(null);
		
		Group group = translator.toFhirResource(core);
		
		assertThat(group.getIdElement().getIdPart(), equalTo(UUID));
		assertThat(group.getExtensionByUrl(FhirConstants.GROUP_DATE_CREATED_EXTENSION_URL), nullValue());
	}
	
	private static Cohort core() {
		Cohort core = new Cohort();
		core.setUuid(UUID);
		core.setName("A list");
		core.setVoided(false);
		core.setDateCreated(new Date());
		core.setCreator(new User());
		return core;
	}
	
	private static Patient patient(String uuid) {
		Patient patient = new Patient();
		patient.setUuid(uuid);
		return patient;
	}
}
