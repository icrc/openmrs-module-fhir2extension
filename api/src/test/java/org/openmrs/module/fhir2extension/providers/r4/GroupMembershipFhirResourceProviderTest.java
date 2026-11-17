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
package org.openmrs.module.fhir2extension.providers.r4;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Arrays;
import java.util.Collections;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.junit.Before;
import org.junit.Test;
import org.openmrs.module.fhir2extension.api.FhirGroupMembershipService;

public class GroupMembershipFhirResourceProviderTest {
	
	private static final String GROUP = "11111111-1111-1111-1111-111111111111";
	
	private final FhirGroupMembershipService service = mock(FhirGroupMembershipService.class);
	
	private GroupMembershipFhirResourceProvider provider;
	
	@Before
	public void setUp() {
		provider = new GroupMembershipFhirResourceProvider();
		provider.setMembershipService(service);
	}
	
	@Test
	public void addMembers_shouldPassThePatientUuidsAndAnswerTheGroup() {
		Group answer = new Group();
		when(service.addMembers(GROUP, Arrays.asList("p1", "p2"))).thenReturn(answer);
		
		Group result = provider.addMembers(new IdType("Group", GROUP),
		    Arrays.asList(new Reference("Patient/p1"), new Reference("Patient/p2")));
		
		assertThat(result == answer, equalTo(true));
	}
	
	@Test
	public void removeMembers_shouldPassThePatientUuids() {
		provider.removeMembers(new IdType("Group", GROUP), Collections.singletonList(new Reference("Patient/p1")));
		
		verify(service).removeMembers(GROUP, Collections.singletonList("p1"));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void addMembers_shouldRefuseAReferenceThatIsNotAPatient() {
		provider.addMembers(new IdType("Group", GROUP), Collections.singletonList(new Reference("Practitioner/p1")));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void addMembers_shouldRefuseAReferenceWithoutAnId() {
		provider.addMembers(new IdType("Group", GROUP), Collections.singletonList(new Reference()));
	}
	
	@Test(expected = InvalidRequestException.class)
	public void removeMembers_shouldRefuseNoPatient() {
		provider.removeMembers(new IdType("Group", GROUP), Collections.<Reference> emptyList());
	}
	
	@Test
	public void theOperationsAreNamedAsTheContractSays() throws Exception {
		assertThat(
		    provider.getClass().getMethod("addMembers", IdType.class, java.util.List.class).getAnnotation(Operation.class)
		            .name(), equalTo("$add-members"));
		assertThat(
		    provider.getClass().getMethod("removeMembers", IdType.class, java.util.List.class)
		            .getAnnotation(Operation.class).name(), equalTo("$remove-members"));
	}
}
