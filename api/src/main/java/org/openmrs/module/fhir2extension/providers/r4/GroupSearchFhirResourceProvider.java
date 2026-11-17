/*
 * with Copyright 2024 ICRC
 *
 */
package org.openmrs.module.fhir2extension.providers.r4;

import ca.uhn.fhir.rest.annotation.OptionalParam;
import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Group;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2extension.api.FhirGroupSearchService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * {@code GET Group?name=&cohort-type=&location=}: the search of Groups by name (a part of it), by
 * cohort type (the type's uuid) and by location (the location's uuid, or {@code Location/<uuid>}),
 * which FHIR2's own provider of Group does not have. A server merges the methods of every provider
 * of a resource type.
 */
@Component("groupSearchFhirR4ResourceProvider")
@R4Provider
public class GroupSearchFhirResourceProvider implements IResourceProvider {
	
	@Autowired
	private FhirGroupSearchService searchService;
	
	void setSearchService(FhirGroupSearchService searchService) {
		this.searchService = searchService;
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Group.class;
	}
	
	@Search
	public IBundleProvider search(@OptionalParam(name = "name") StringParam name,
	        @OptionalParam(name = "cohort-type") TokenParam cohortType,
	        @OptionalParam(name = "location") ReferenceParam location) {
		return searchService.search(name == null ? null : name.getValue(),
		    cohortType == null ? null : cohortType.getValue(), location == null ? null : location.getIdPart());
	}
}
