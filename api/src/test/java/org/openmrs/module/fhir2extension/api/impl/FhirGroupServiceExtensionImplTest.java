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
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortMember;
import org.openmrs.module.cohort.CohortType;
import org.openmrs.module.cohort.api.CohortMemberService;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.cohort.api.CohortTypeService;
import org.openmrs.module.fhir2.FhirConstants;
import org.openmrs.module.fhir2extension.api.translators.impl.GroupCohortMapper;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FhirGroupServiceExtensionImplTest {
	
	private static final String GROUP_UUID = "11111111-1111-1111-1111-111111111111";
	
	private static final String TYPE_UUID = "22222222-2222-2222-2222-222222222222";
	
	private static final String LOCATION_UUID = "33333333-3333-3333-3333-333333333333";
	
	private static final String PATIENT_A = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
	
	private static final String PATIENT_B = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
	
	@Mock
	private CohortService cohortService;
	
	@Mock
	private CohortMemberService cohortMemberService;
	
	@Mock
	private CohortTypeService cohortTypeService;
	
	@Mock
	private PatientService patientService;
	
	@Mock
	private LocationService locationService;
	
	private FhirGroupServiceExtensionImpl service;
	
	private CohortType cohortType;
	
	@Before
	public void setUp() {
		GroupCohortMapper mapper = new GroupCohortMapper();
		mapper.setCohortTypeService(cohortTypeService);
		mapper.setLocationService(locationService);
		mapper.setPatientService(patientService);

		service = spy(new FhirGroupServiceExtensionImpl());
		service.setCohortService(cohortService);
		service.setCohortMemberService(cohortMemberService);
		service.setMapper(mapper);
		doNothing().when(service).flush();

		cohortType = new CohortType();
		cohortType.setUuid(TYPE_UUID);
		when(cohortTypeService.getCohortTypeByUuid(TYPE_UUID)).thenReturn(cohortType);

		Location location = new Location();
		location.setUuid(LOCATION_UUID);
		when(locationService.getLocationByUuid(LOCATION_UUID)).thenReturn(location);

		when(patientService.getPatientByUuid(PATIENT_A)).thenReturn(patient(PATIENT_A));
		when(patientService.getPatientByUuid(PATIENT_B)).thenReturn(patient(PATIENT_B));

		when(cohortService.saveCohortM(any(CohortM.class))).thenAnswer(invocation -> invocation.getArgument(0));
		when(cohortMemberService.saveCohortMember(any(CohortMember.class)))
				.thenAnswer(invocation -> invocation.getArgument(0));
		doReturn(new Group()).when(service).get(anyString());
	}
	
	@Test
	public void create_shouldSaveTheCohortWithItsTypeLocationAndMembers() {
		Group group = group("Group A", PATIENT_A, PATIENT_B);
		group.setId(GROUP_UUID);
		
		service.create(group);
		
		ArgumentCaptor<CohortM> saved = ArgumentCaptor.forClass(CohortM.class);
		verify(cohortService).saveCohortM(saved.capture());
		assertThat(saved.getValue().getUuid(), equalTo(GROUP_UUID));
		assertThat(saved.getValue().getName(), equalTo("Group A"));
		assertThat(saved.getValue().getCohortType(), equalTo(cohortType));
		assertThat(saved.getValue().getLocation().getUuid(), equalTo(LOCATION_UUID));
		assertThat(saved.getValue().getStartDate(), notNullValue());
		assertThat(saved.getValue().getGroupCohort(), equalTo(false));
		assertThat(saved.getValue().getCohortMembers(), hasSize(2));
		verify(cohortMemberService, org.mockito.Mockito.times(2)).saveCohortMember(any(CohortMember.class));
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void create_shouldRefuseAnUnknownCohortType() {
		Group group = group("Group A");
		group.getExtensionByUrl(org.openmrs.module.fhir2extension.FhirConstants.GROUP_COHORT_TYPE_EXTENSION_URL).setValue(
		    new StringType("00000000-0000-0000-0000-000000000000"));
		
		service.create(group);
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void create_shouldRefuseAnUnknownPatient() {
		service.create(group("Group A", "99999999-9999-9999-9999-999999999999"));
	}
	
	@Test(expected = ResourceVersionConflictException.class)
	public void create_shouldRefuseAnIdThatExists() {
		when(cohortService.getCohortMByUuid(GROUP_UUID)).thenReturn(new CohortM());
		Group group = group("Group A");
		group.setId(GROUP_UUID);
		
		service.create(group);
	}
	
	@Test
	public void update_shouldKeepTheMembersListedAddNewOnesAndVoidTheOthers() {
		CohortM cohort = cohort("Group A", membership(PATIENT_A), membership(PATIENT_B));
		when(cohortService.getCohortMByUuid(GROUP_UUID)).thenReturn(cohort);
		
		Group group = group("Group A", PATIENT_A, "cccccccc-cccc-cccc-cccc-cccccccccccc");
		when(patientService.getPatientByUuid("cccccccc-cccc-cccc-cccc-cccccccccccc")).thenReturn(
		    patient("cccccccc-cccc-cccc-cccc-cccccccccccc"));
		group.setId(GROUP_UUID);
		
		service.update(GROUP_UUID, group);
		
		ArgumentCaptor<CohortMember> voided = ArgumentCaptor.forClass(CohortMember.class);
		verify(cohortMemberService).voidCohortMember(voided.capture(), anyString());
		assertThat(voided.getValue().getPatient().getUuid(), equalTo(PATIENT_B));
		
		ArgumentCaptor<CohortMember> added = ArgumentCaptor.forClass(CohortMember.class);
		verify(cohortMemberService).saveCohortMember(added.capture());
		assertThat(added.getValue().getPatient().getUuid(), equalTo("cccccccc-cccc-cccc-cccc-cccccccccccc"));
		assertThat(added.getValue().getCohort(), equalTo(cohort));
		assertThat(added.getValue().getStartDate(), notNullValue());
	}
	
	@Test
	public void update_shouldKeepTheMembersWhenTheGroupListsNone() {
		CohortMember kept = membership(PATIENT_A);
		CohortM cohort = cohort("Group A", kept);
		when(cohortService.getCohortMByUuid(GROUP_UUID)).thenReturn(cohort);
		Group group = group("A new name");
		group.setId(GROUP_UUID);
		
		service.update(GROUP_UUID, group);
		
		assertThat(cohort.getName(), equalTo("A new name"));
		verify(cohortMemberService, never()).voidCohortMember(any(CohortMember.class), anyString());
		verify(cohortMemberService, never()).saveCohortMember(any(CohortMember.class));
	}
	
	@Test
	public void update_shouldNotTouchAMembershipThatIsStillListed() {
		CohortM cohort = cohort("Group A", membership(PATIENT_A));
		when(cohortService.getCohortMByUuid(GROUP_UUID)).thenReturn(cohort);
		Group group = group("Group A", PATIENT_A);
		group.setId(GROUP_UUID);
		
		service.update(GROUP_UUID, group);
		
		verify(cohortMemberService, never()).saveCohortMember(any(CohortMember.class));
		verify(cohortMemberService, never()).voidCohortMember(any(CohortMember.class), anyString());
	}
	
	@Test
	public void update_shouldVoidTheCohortWhenTheGroupIsInactive() {
		CohortM cohort = cohort("Group A");
		when(cohortService.getCohortMByUuid(GROUP_UUID)).thenReturn(cohort);
		Group group = group("Group A");
		group.setId(GROUP_UUID);
		group.setActive(false);
		
		service.update(GROUP_UUID, group);
		
		verify(cohortService).voidCohortM(eq(cohort), anyString());
		verify(cohortService, never()).saveCohortM(any(CohortM.class));
	}
	
	@Test
	public void update_shouldCreateTheCohortWithTheIdOfTheClientWhenItDoesNotExist() {
		Group group = group("Group A", PATIENT_A);
		group.setId(GROUP_UUID);
		
		service.update(GROUP_UUID, group);
		
		ArgumentCaptor<CohortM> saved = ArgumentCaptor.forClass(CohortM.class);
		verify(cohortService).saveCohortM(saved.capture());
		assertThat(saved.getValue().getUuid(), equalTo(GROUP_UUID));
		assertThat(saved.getValue().getName(), equalTo("Group A"));
		assertThat(saved.getValue().getCohortMembers(), hasSize(1));
	}
	
	@Test
	public void update_shouldNotCreateTheSameCohortTwice() {
		when(cohortService.getCohortMByUuid(GROUP_UUID)).thenReturn(cohort("Group A"));
		Group group = group("Group A");
		group.setId(GROUP_UUID);
		
		service.update(GROUP_UUID, group);
		
		verify(cohortService, org.mockito.Mockito.times(1)).saveCohortM(any(CohortM.class));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void update_shouldFailWhenTheIdsDoNotMatch() {
		Group group = group("Group A");
		group.setId("44444444-4444-4444-4444-444444444444");
		
		service.update(GROUP_UUID, group);
	}
	
	@Test
	public void delete_shouldVoidTheCohort() {
		CohortM cohort = cohort("Group A");
		when(cohortService.getCohortMByUuid(GROUP_UUID)).thenReturn(cohort);
		
		service.delete(GROUP_UUID);
		
		verify(cohortService).voidCohortM(eq(cohort), anyString());
	}
	
	@Test(expected = ResourceNotFoundException.class)
	public void delete_shouldFailForAnUnknownGroup() {
		service.delete(GROUP_UUID);
	}
	
	@Test
	public void withActiveMembers_shouldNotListVoidedOrEndedMemberships() {
		CohortMember active = membership(PATIENT_A);
		CohortMember voided = membership(PATIENT_B);
		voided.setVoided(true);
		CohortMember ended = membership("cccccccc-cccc-cccc-cccc-cccccccccccc");
		ended.setEndDate(new Date(System.currentTimeMillis() - 86400000L));
		CohortM cohort = cohort("Group A", active, voided, ended);
		
		Group group = group("Group A", PATIENT_A, PATIENT_B);
		new GroupCohortMapper().withActiveMembers(group, cohort);
		
		assertThat(group.getQuantity(), equalTo(1));
		assertThat(group.getMember(), hasSize(1));
		assertThat(group.getMember().get(0).getEntity().getReference(), equalTo("Patient/" + PATIENT_A));
	}
	
	@Test
	public void withActiveMembers_shouldAddAnActiveMembershipTheGroupLacks() {
		CohortM cohort = cohort("Group A", membership(PATIENT_A));
		Group group = group("Group A");
		
		new GroupCohortMapper().withActiveMembers(group, cohort);
		
		assertThat(group.getQuantity(), equalTo(1));
		assertThat(group.getMember().get(0).getEntity().getReference(), equalTo("Patient/" + PATIENT_A));
	}
	
	private static Patient patient(String uuid) {
		Patient patient = new Patient();
		patient.setUuid(uuid);
		return patient;
	}
	
	private static CohortMember membership(String patientUuid) {
		CohortMember membership = new CohortMember(patient(patientUuid));
		membership.setStartDate(new Date(System.currentTimeMillis() - 86400000L));
		return membership;
	}
	
	private static CohortM cohort(String name, CohortMember... memberships) {
		CohortM cohort = new CohortM();
		cohort.setUuid(GROUP_UUID);
		cohort.setName(name);
		for (CohortMember membership : memberships) {
			membership.setCohort(cohort);
			cohort.getCohortMembers().add(membership);
		}
		return cohort;
	}
	
	private static Group group(String name, String... patientUuids) {
		Group group = new Group();
		group.setName(name);
		group.addExtension().setUrl(org.openmrs.module.fhir2extension.FhirConstants.GROUP_COHORT_TYPE_EXTENSION_URL)
		        .setValue(new StringType(TYPE_UUID));
		group.addExtension().setUrl(org.openmrs.module.fhir2extension.FhirConstants.GROUP_LOCATION_EXTENSION_URL)
		        .setValue(new Reference("Location/" + LOCATION_UUID));
		group.addExtension().setUrl(FhirConstants.OPENMRS_FHIR_EXT_GROUP_DESCRIPTION).setValue(new StringType("A list"));
		for (String uuid : patientUuids) {
			group.addMember().setEntity(new Reference("Patient/" + uuid));
		}
		return group;
	}
}
