/*
 * with Copyright 2024 ICRC
 *
 */
package org.openmrs.module.fhir2extension.providers.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ca.uhn.fhir.rest.annotation.Search;
import ca.uhn.fhir.rest.api.server.IBundleProvider;
import ca.uhn.fhir.rest.param.ReferenceParam;
import ca.uhn.fhir.rest.param.StringParam;
import ca.uhn.fhir.rest.param.TokenParam;
import org.hl7.fhir.r4.model.Group;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.fhir2extension.api.FhirGroupSearchService;

public class GroupSearchFhirResourceProviderTest {
	
	private final FhirGroupSearchService service = mock(FhirGroupSearchService.class);
	
	private GroupSearchFhirResourceProvider provider;
	
	@Before
	public void setUp() {
		provider = new GroupSearchFhirResourceProvider();
		provider.setSearchService(service);
	}
	
	@Test
	public void getResourceType_shouldBeGroup() {
		assertThat(provider.getResourceType() == Group.class, equalTo(true));
	}
	
	@Test
	public void search_shouldPassTheValuesOfTheParameters() throws Exception {
		IBundleProvider answer = mock(IBundleProvider.class);
		when(service.search("mothers", "type-uuid", "location-uuid")).thenReturn(answer);
		
		IBundleProvider result = provider.search(new StringParam("mothers"), new TokenParam("type-uuid"),
		    new ReferenceParam("location-uuid"));
		
		assertThat(result == answer, equalTo(true));
		assertThat(
		    GroupSearchFhirResourceProvider.class.getMethod("search", StringParam.class, TokenParam.class,
		        ReferenceParam.class).isAnnotationPresent(Search.class), equalTo(true));
	}
	
	@Test
	public void search_shouldTakeTheIdOfALocationReference() {
		provider.search(null, null, new ReferenceParam("Location/location-uuid"));
		
		verify(service).search(null, null, "location-uuid");
	}
	
	@Test
	public void search_shouldPassNothingWhenNoParameterIsGiven() {
		provider.search(null, null, null);
		
		verify(service).search(null, null, null);
	}
}
