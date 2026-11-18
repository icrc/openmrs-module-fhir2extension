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
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.Arrays;

import org.hl7.fhir.r4.model.Parameters;
import org.junit.Test;
import org.openmrs.module.cohort.CohortAttributeType;
import org.openmrs.module.cohort.api.CohortService;

public class GroupAttributeTypesFhirResourceProviderTest {
	
	@Test
	public void attributeTypes_shouldListTheTypesThatAreNotRetiredByName() {
		CohortService service = mock(CohortService.class);
		CohortAttributeType retired = type("Old", "u-old", null);
		retired.setRetired(true);
		when(service.findAllCohortAttributeTypes()).thenReturn(
		    Arrays.asList(type("Funding", "u-funding", "Who pays"), retired, type("Cadence", "u-cadence", null)));
		GroupAttributeTypesFhirResourceProvider provider = new GroupAttributeTypesFhirResourceProvider();
		provider.setCohortService(service);
		
		Parameters answer = provider.attributeTypes();
		
		assertThat(answer.getParameter(), hasSize(2));
		assertThat(answer.getParameter().get(0).getPart().get(0).getValue().primitiveValue(), equalTo("Cadence"));
		assertThat(answer.getParameter().get(0).getPart(), hasSize(2));
		assertThat(answer.getParameter().get(1).getPart().get(1).getValue().primitiveValue(), equalTo("u-funding"));
		assertThat(answer.getParameter().get(1).getPart().get(2).getValue().primitiveValue(), equalTo("Who pays"));
	}
	
	private static CohortAttributeType type(String name, String uuid, String description) {
		CohortAttributeType type = new CohortAttributeType();
		type.setName(name);
		type.setUuid(uuid);
		type.setDescription(description);
		return type;
	}
}
