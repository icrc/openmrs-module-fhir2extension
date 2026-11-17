/*
 * with Copyright 2024 ICRC
 *
 */
package org.openmrs.module.fhir2extension.api.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Group;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.openmrs.Location;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortType;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.cohort.api.CohortTypeService;
import org.openmrs.module.fhir2.api.FhirGroupService;

@RunWith(MockitoJUnitRunner.Silent.class)
public class FhirGroupSearchServiceImplTest {
	
	private static final String TYPE_UUID = "cccccccc-cccc-cccc-cccc-cccccccccccc";
	
	private static final String NORTH = "aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa";
	
	private static final String SOUTH = "bbbbbbbb-bbbb-bbbb-bbbb-bbbbbbbbbbbb";
	
	@Mock
	private CohortService cohortService;
	
	@Mock
	private CohortTypeService cohortTypeService;
	
	@Mock
	private FhirGroupService groupService;
	
	private FhirGroupSearchServiceImpl service;
	
	private CohortM atNorth;
	
	private CohortM atSouth;
	
	private CohortM nowhere;
	
	@Before
	public void setUp() {
		service = new FhirGroupSearchServiceImpl();
		service.setCohortService(cohortService);
		service.setCohortTypeService(cohortTypeService);
		service.setGroupService(groupService);
		
		atNorth = cohort("north-list", NORTH);
		atSouth = cohort("south-list", SOUTH);
		nowhere = cohort("no-location-list", null);
		for (CohortM cohort : Arrays.asList(atNorth, atSouth, nowhere)) {
			Group group = new Group();
			group.setId(cohort.getUuid());
			when(groupService.get(cohort.getUuid())).thenReturn(group);
		}
	}
	
	private CohortM cohort(String uuid, String locationUuid) {
		CohortM cohort = new CohortM();
		cohort.setUuid(uuid);
		if (locationUuid != null) {
			Location location = new Location();
			location.setUuid(locationUuid);
			cohort.setLocation(location);
		}
		return cohort;
	}
	
	private List<String> ids(IBundleProvider results) {
		return results.getResources(0, results.size()).stream().map(IBaseResource::getIdElement)
		        .map(id -> id.getIdPart()).collect(Collectors.toList());
	}
	
	@Test
	public void search_shouldAnswerEveryCohortWhenNothingIsAsked() {
		when(cohortService.findMatchingCohortMs(null, null, null, false)).thenReturn(
		    Arrays.asList(atNorth, atSouth, nowhere));
		
		assertThat(ids(service.search(null, null, null)), contains("north-list", "south-list", "no-location-list"));
	}
	
	@Test
	public void search_shouldPassTheNameAndTheTypeToTheCohortModule() {
		CohortType type = new CohortType();
		when(cohortTypeService.getCohortTypeByUuid(TYPE_UUID)).thenReturn(type);
		when(cohortService.findMatchingCohortMs("north", null, type, false)).thenReturn(Arrays.asList(atNorth));
		
		assertThat(ids(service.search("north", TYPE_UUID, null)), contains("north-list"));
	}
	
	@Test
	public void search_shouldKeepOnlyTheCohortsOfTheLocation() {
		when(cohortService.findMatchingCohortMs(null, null, null, false)).thenReturn(
		    Arrays.asList(atNorth, atSouth, nowhere));
		
		assertThat(ids(service.search(null, null, SOUTH)), contains("south-list"));
	}
	
	@Test
	public void search_shouldAnswerNothingForATypeThatDoesNotExist() {
		IBundleProvider results = service.search(null, TYPE_UUID, null);
		
		assertThat(results.size(), equalTo(0));
		assertThat(results.getResources(0, 0), empty());
		verify(cohortService, never()).findMatchingCohortMs(any(), any(), any(), eq(false));
	}
	
	@Test
	public void search_shouldNeverAskForVoidedCohorts() {
		service.search("x", null, null);
		
		verify(cohortService).findMatchingCohortMs(eq("x"), eq(null), eq(null), eq(false));
		verify(cohortService, never()).findMatchingCohortMs(any(), any(), any(), eq(true));
	}
}
