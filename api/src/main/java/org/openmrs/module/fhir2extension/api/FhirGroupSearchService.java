/*
 * with Copyright 2024 ICRC
 *
 */
package org.openmrs.module.fhir2extension.api;

import ca.uhn.fhir.rest.api.server.IBundleProvider;

/**
 * Searches the Groups that are cohorts of the cohort module by what FHIR2 cannot search them by:
 * their name, their type and their location.
 */
public interface FhirGroupSearchService {
	
	/**
	 * @param name a part of the name of the cohort, or null for any
	 * @param cohortTypeUuid the uuid of the cohort type, or null for any; an unknown type matches
	 *            no Group
	 * @param locationUuid the uuid of the location of the cohort, or null for any
	 * @return the matching Groups, as a read of each would give them
	 */
	IBundleProvider search(String name, String cohortTypeUuid, String locationUuid);
}
