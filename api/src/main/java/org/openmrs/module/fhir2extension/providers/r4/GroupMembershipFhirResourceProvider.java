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
import java.util.List;

import ca.uhn.fhir.rest.annotation.IdParam;
import ca.uhn.fhir.rest.annotation.Operation;
import ca.uhn.fhir.rest.annotation.OperationParam;
import ca.uhn.fhir.rest.server.IResourceProvider;
import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import org.hl7.fhir.instance.model.api.IBaseResource;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.IdType;
import org.hl7.fhir.r4.model.Reference;
import org.openmrs.module.fhir2.api.annotations.R4Provider;
import org.openmrs.module.fhir2extension.api.FhirGroupMembershipService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * The operations that change the members of a Group by what changes:
 * {@code POST Group/[id]/$add-members} and {@code POST Group/[id]/$remove-members}, each with one
 * {@code patient} parameter (a reference to a Patient) per patient. They answer with the Group as
 * it is afterwards.
 * <p>
 * FHIR2's own provider of Group is left alone: a server merges the methods of every provider of a
 * resource type.
 */
@Component("groupMembershipFhirR4ResourceProvider")
@R4Provider
public class GroupMembershipFhirResourceProvider implements IResourceProvider {
	
	@Autowired
	private FhirGroupMembershipService membershipService;
	
	void setMembershipService(FhirGroupMembershipService membershipService) {
		this.membershipService = membershipService;
	}
	
	@Override
	public Class<? extends IBaseResource> getResourceType() {
		return Group.class;
	}
	
	@Operation(name = "$add-members", type = Group.class, idempotent = false)
	public Group addMembers(@IdParam IdType id,
	        @OperationParam(name = "patient", min = 1, max = OperationParam.MAX_UNLIMITED) List<Reference> patients) {
		return membershipService.addMembers(id.getIdPart(), patientUuids(patients));
	}
	
	@Operation(name = "$remove-members", type = Group.class, idempotent = false)
	public Group removeMembers(@IdParam IdType id,
	        @OperationParam(name = "patient", min = 1, max = OperationParam.MAX_UNLIMITED) List<Reference> patients) {
		return membershipService.removeMembers(id.getIdPart(), patientUuids(patients));
	}
	
	static List<String> patientUuids(List<Reference> patients) {
		List<String> uuids = new ArrayList<>();
		if (patients != null) {
			for (Reference patient : patients) {
				if (patient == null || !patient.hasReference()
						|| !"Patient".equals(patient.getReferenceElement().getResourceType())
						|| !patient.getReferenceElement().hasIdPart()) {
					throw new InvalidRequestException(
							"Every patient parameter must be a reference to a Patient (Patient/<uuid>)");
				}
				uuids.add(patient.getReferenceElement().getIdPart());
			}
		}
		if (uuids.isEmpty()) {
			throw new InvalidRequestException("At least one patient parameter is needed");
		}
		return uuids;
	}
}
