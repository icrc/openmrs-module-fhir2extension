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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import org.hl7.fhir.r4.model.Group;
import org.openmrs.Patient;
import org.openmrs.api.context.Context;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortMember;
import org.openmrs.module.cohort.api.CohortMemberService;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.fhir2.api.FhirGroupService;
import org.openmrs.module.fhir2extension.api.FhirGroupMembershipService;
import org.openmrs.module.fhir2extension.api.translators.impl.GroupCohortMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
public class FhirGroupMembershipServiceImpl implements FhirGroupMembershipService {
	
	private static final String REMOVED_REASON = "Removed through FHIR";
	
	@Autowired
	private CohortService cohortService;
	
	@Autowired
	private CohortMemberService cohortMemberService;
	
	@Autowired
	private FhirGroupService groupService;
	
	@Autowired
	private GroupCohortMapper mapper;
	
	void setCohortService(CohortService cohortService) {
		this.cohortService = cohortService;
	}
	
	void setCohortMemberService(CohortMemberService cohortMemberService) {
		this.cohortMemberService = cohortMemberService;
	}
	
	void setGroupService(FhirGroupService groupService) {
		this.groupService = groupService;
	}
	
	void setMapper(GroupCohortMapper mapper) {
		this.mapper = mapper;
	}
	
	@Override
	@Transactional
	public Group addMembers(String groupUuid, Collection<String> patientUuids) {
		CohortM cohort = cohortOf(groupUuid);
		Set<String> wanted = distinct(patientUuids);

		// Every patient is looked up before anything is saved, so that one unknown patient adds nobody.
		Date now = new Date();
		Set<String> members = activePatients(cohort, now);
		List<Patient> toAdd = new ArrayList<>();
		for (String uuid : wanted) {
			Patient patient = mapper.patientOf(uuid);
			if (!members.contains(uuid)) {
				toAdd.add(patient);
			}
		}
		for (Patient patient : toAdd) {
			CohortMember membership = new CohortMember(patient);
			membership.setCohort(cohort);
			membership.setStartDate(now);
			cohort.getCohortMembers().add(cohortMemberService.saveCohortMember(membership));
		}
		flush();
		return groupService.get(groupUuid);
	}
	
	@Override
	@Transactional
	public Group removeMembers(String groupUuid, Collection<String> patientUuids) {
		CohortM cohort = cohortOf(groupUuid);
		Set<String> unwanted = distinct(patientUuids);

		Date now = new Date();
		for (CohortMember membership : new ArrayList<>(cohort.getCohortMembers())) {
			if (GroupCohortMapper.isActive(membership, now) && membership.getPatient() != null
					&& unwanted.contains(membership.getPatient().getUuid())) {
				cohortMemberService.voidCohortMember(membership, REMOVED_REASON);
			}
		}
		flush();
		return groupService.get(groupUuid);
	}
	
	private CohortM cohortOf(String groupUuid) {
		CohortM cohort = groupUuid == null ? null : cohortService.getCohortMByUuid(groupUuid);
		if (cohort == null) {
			throw new ResourceNotFoundException("Could not find Group with Id " + groupUuid);
		}
		return cohort;
	}
	
	private static Set<String> distinct(Collection<String> patientUuids) {
		if (patientUuids == null || patientUuids.isEmpty()) {
			throw new InvalidRequestException("At least one patient is needed");
		}
		return new LinkedHashSet<>(patientUuids);
	}
	
	private static Set<String> activePatients(CohortM cohort, Date now) {
		Set<String> uuids = new LinkedHashSet<>();
		for (CohortMember membership : cohort.getCohortMembers()) {
			if (GroupCohortMapper.isActive(membership, now) && membership.getPatient() != null) {
				uuids.add(membership.getPatient().getUuid());
			}
		}
		return uuids;
	}
	
	/** Writes what was saved, so that FHIR2's read finds it. */
	void flush() {
		Context.flushSession();
	}
}
