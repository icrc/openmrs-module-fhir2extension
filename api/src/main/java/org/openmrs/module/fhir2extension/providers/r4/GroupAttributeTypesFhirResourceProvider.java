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

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;

import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.server.IResourceProvider;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Parameters;
import org.hl7.fhir.r4.model.StringType;
import org.openmrs.module.cohort.CohortAttributeType;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * {@code GET Group/$attribute-types}: the attribute types a cohort can have (those that are not
 * retired), so that a client can offer them. One {@code attributeType} parameter per type, with the
 * parts {@code name} (what {@code .../ext/group/attribute} names as its {@code type}), {@code uuid}
 * and, when it has one, {@code description}.
 */
@Component("groupAttributeTypesFhirR4ResourceProvider")
@R4Provider
public class GroupAttributeTypesFhirResourceProvider implements IResourceProvider {
	
	@Autowired
	private CohortService cohortService;
	
	void setCohortService(CohortService cohortService) {
		this.cohortService = cohortService;
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Group.class;
	}
	
	@Operation(name = "$attribute-types", type = Group.class, idempotent = true)
	public Parameters attributeTypes() {
		List<CohortAttributeType> types = new ArrayList<>();
		for (CohortAttributeType type : cohortService.findAllCohortAttributeTypes()) {
			if (!Boolean.TRUE.equals(type.getRetired())) {
				types.add(type);
			}
		}
		types.sort(Comparator.comparing(CohortAttributeType::getName, Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

		Parameters parameters = new Parameters();
		for (CohortAttributeType type : types) {
			Parameters.ParametersParameterComponent entry = parameters.addParameter().setName("attributeType");
			entry.addPart().setName("name").setValue(new StringType(type.getName()));
			entry.addPart().setName("uuid").setValue(new StringType(type.getUuid()));
			if (type.getDescription() != null && !type.getDescription().isEmpty()) {
				entry.addPart().setName("description").setValue(new StringType(type.getDescription()));
			}
		}
		return parameters;
	}
}
