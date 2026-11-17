/*
 * with Copyright 2024 ICRC
 *
 */
package org.openmrs.module.fhir2extension.api.impl;

import java.util.List;
import java.util.stream.Collectors;

import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.server.SimpleBundleProvider;
import org.hl7.fhir.r4.model.Group;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortType;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.cohort.api.CohortTypeService;
import org.openmrs.module.fhir2.api.FhirGroupService;
import org.openmrs.module.fhir2extension.api.FhirGroupSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FhirGroupSearchServiceImpl implements FhirGroupSearchService {
	
	@Autowired
	private CohortService cohortService;
	
	@Autowired
	private CohortTypeService cohortTypeService;
	
	@Autowired
	private FhirGroupService groupService;
	
	void setCohortService(CohortService cohortService) {
		this.cohortService = cohortService;
	}
	
	void setCohortTypeService(CohortTypeService cohortTypeService) {
		this.cohortTypeService = cohortTypeService;
	}
	
	void setGroupService(FhirGroupService groupService) {
		this.groupService = groupService;
	}
	
	@Override
	@Transactional(readOnly = true)
	public IBundleProvider search(String name, String cohortTypeUuid, String locationUuid) {
		CohortType type = null;
		if (cohortTypeUuid != null) {
			type = cohortTypeService.getCohortTypeByUuid(cohortTypeUuid);
			if (type == null) {
				return new SimpleBundleProvider();
			}
		}

		List<Group> groups = cohortService.findMatchingCohortMs(name, null, type, false).stream()
		        .filter(cohort -> locationUuid == null || isAt(cohort, locationUuid))
		        .map(cohort -> groupService.get(cohort.getUuid())).collect(Collectors.toList());
		return new SimpleBundleProvider(groups);
	}
	
	private boolean isAt(CohortM cohort, String locationUuid) {
		return cohort.getLocation() != null && locationUuid.equals(cohort.getLocation().getUuid());
	}
}
