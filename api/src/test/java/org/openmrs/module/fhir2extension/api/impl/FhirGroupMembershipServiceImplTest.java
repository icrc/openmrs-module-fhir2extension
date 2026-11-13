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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Date;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.Group;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Patient;
import org.openmrs.api.PatientService;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortMember;
import org.openmrs.module.cohort.api.CohortMemberService;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.fhir2.api.FhirGroupService;
import org.openmrs.module.fhir2extension.api.translators.impl.GroupCohortMapper;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FhirGroupMembershipServiceImplTest {

	private static final String GROUP_UUID = "11111111-1111-1111-1111-111111111111";

	private static final String PATIENT_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";

	private static final String PATIENT_B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";

	@Mock
	private CohortService cohortService;

	@Mock
	private CohortMemberService cohortMemberService;

	@Mock
	private PatientService patientService;

	@Mock
	private FhirGroupService groupService;

	private FhirGroupMembershipServiceImpl service;

	private CohortM cohort;

	@Before
	public void setUp() {
		GroupCohortMapper mapper = new GroupCohortMapper();
		mapper.setPatientService(patientService);
		service = spy(new FhirGroupMembershipServiceImpl());
		service.setCohortService(cohortService);
		service.setCohortMemberService(cohortMemberService);
		service.setGroupService(groupService);
		service.setMapper(mapper);
		doNothing().when(service).flush();

		cohort = new CohortM();
		cohort.setUuid(GROUP_UUID);
		when(cohortService.getCohortMByUuid(GROUP_UUID)).thenReturn(cohort);
		when(patientService.getPatientByUuid(PATIENT_A)).thenReturn(patient(PATIENT_A));
		when(patientService.getPatientByUuid(PATIENT_B)).thenReturn(patient(PATIENT_B));
		when(cohortMemberService.saveCohortMember(any(CohortMember.class))).thenAnswer(i -> i.getArgument(0));
		when(groupService.get(GROUP_UUID)).thenReturn(new Group());
	}

	@Test
	public void addMembers_shouldAddThePatientsThatAreNotMembersYet() {
		member(PATIENT_A, false);

		service.addMembers(GROUP_UUID, Arrays.asList(PATIENT_A, PATIENT_B));

		ArgumentCaptor<CohortMember> saved = ArgumentCaptor.forClass(CohortMember.class);
		verify(cohortMemberService, times(1)).saveCohortMember(saved.capture());
		assertThat(saved.getValue().getPatient().getUuid(), equalTo(PATIENT_B));
		assertThat(saved.getValue().getCohort(), equalTo(cohort));
		assertThat(saved.getValue().getStartDate() != null, equalTo(true));
		assertThat(cohort.getCohortMembers(), hasSize(2));
	}

	@Test
	public void addMembers_shouldDoNothingTheSecondTimeItIsSent() {
		service.addMembers(GROUP_UUID, Arrays.asList(PATIENT_A, PATIENT_A));
		service.addMembers(GROUP_UUID, Arrays.asList(PATIENT_A));

		verify(cohortMemberService, times(1)).saveCohortMember(any(CohortMember.class));
		assertThat(cohort.getCohortMembers(), hasSize(1));
	}

	@Test
	public void addMembers_shouldAddAgainAPatientWhoseMembershipWasVoided() {
		member(PATIENT_A, true);

		service.addMembers(GROUP_UUID, Arrays.asList(PATIENT_A));

		verify(cohortMemberService, times(1)).saveCohortMember(any(CohortMember.class));
	}

	@Test
	public void addMembers_shouldAddNobodyWhenOnePatientIsUnknown() {
		try {
			service.addMembers(GROUP_UUID, Arrays.asList(PATIENT_A, "99999999-9999-9999-9999-999999999999"));
			throw new AssertionError("expected a 422");
		}
		catch (UnprocessableEntityException expected) {
			verify(cohortMemberService, never()).saveCohortMember(any(CohortMember.class));
		}
	}

	@Test(expected = ResourceNotFoundException.class)
	public void addMembers_shouldFailForAnUnknownGroup() {
		service.addMembers("00000000-0000-0000-0000-000000000000", Arrays.asList(PATIENT_A));
	}

	@Test(expected = InvalidRequestException.class)
	public void addMembers_shouldRefuseAnEmptyList() {
		service.addMembers(GROUP_UUID, java.util.Collections.<String> emptyList());
	}

	@Test
	public void removeMembers_shouldVoidTheMembershipOfTheListedPatientsOnly() {
		CohortMember a = member(PATIENT_A, false);
		CohortMember b = member(PATIENT_B, false);

		service.removeMembers(GROUP_UUID, Arrays.asList(PATIENT_A));

		verify(cohortMemberService).voidCohortMember(a, "Removed through FHIR");
		verify(cohortMemberService, never()).voidCohortMember(org.mockito.ArgumentMatchers.eq(b), anyString());
	}

	@Test
	public void removeMembers_shouldIgnoreAPatientWhoIsNotAMember() {
		member(PATIENT_A, true);

		service.removeMembers(GROUP_UUID, Arrays.asList(PATIENT_A, PATIENT_B));

		verify(cohortMemberService, never()).voidCohortMember(any(CohortMember.class), anyString());
	}

	@Test(expected = ResourceNotFoundException.class)
	public void removeMembers_shouldFailForAnUnknownGroup() {
		service.removeMembers("00000000-0000-0000-0000-000000000000", Arrays.asList(PATIENT_A));
	}

	private CohortMember member(String patientUuid, boolean voided) {
		CohortMember membership = new CohortMember(patient(patientUuid));
		membership.setStartDate(new Date(System.currentTimeMillis() - 86400000L));
		membership.setVoided(voided);
		membership.setCohort(cohort);
		cohort.getCohortMembers().add(membership);
		return membership;
	}

	private static Patient patient(String uuid) {
		Patient patient = new Patient();
		patient.setUuid(uuid);
		return patient;
	}
}
