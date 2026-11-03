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

import javax.annotation.Nonnull;
import java.util.Date;
import java.util.Map;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.ResourceNotFoundException;
import ca.uhn.fhir.rest.server.exceptions.ResourceVersionConflictException;
import org.hl7.fhir.r4.model.Group;
import org.openmrs.api.context.Context;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortMember;
import org.openmrs.module.cohort.api.CohortMemberService;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.fhir2.api.impl.FhirGroupServiceImpl;
import org.openmrs.module.fhir2extension.api.translators.impl.GroupCohortMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

/**
 * Creates, updates and deletes Groups on the cohort module's entities. FHIR2 saves a Group as a
 * core cohort, which the cohort module refuses while it is active, so no Group could be written.
 * <p>
 * An update is a FHIR PUT: the members of the Group are the members of the cohort afterwards. The
 * memberships already there are kept (nothing is recreated), the missing ones are added and those
 * that are no longer listed are voided. Reads are FHIR2's own, with the members replaced by the
 * active memberships of the cohort.
 */
@Primary
@Component
public class FhirGroupServiceExtensionImpl extends FhirGroupServiceImpl {

	private static final String REMOVED_REASON = "Removed through FHIR";

	@Autowired
	private CohortService cohortService;

	@Autowired
	private CohortMemberService cohortMemberService;

	@Autowired
	private GroupCohortMapper mapper;

	void setCohortService(CohortService cohortService) {
		this.cohortService = cohortService;
	}

	void setCohortMemberService(CohortMemberService cohortMemberService) {
		this.cohortMemberService = cohortMemberService;
	}

	void setMapper(GroupCohortMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public Group get(@Nonnull String uuid) {
		Group group = super.get(uuid);
		CohortM cohort = cohortService.getCohortMByUuid(uuid);
		return cohort == null ? group : mapper.withActiveMembers(group, cohort);
	}

	@Override
	@Transactional
	public Group create(@Nonnull Group newResource) {
		if (newResource == null) {
			throw new InvalidRequestException("A resource of type Group must be supplied");
		}

		CohortM cohort = new CohortM();
		if (newResource.getIdElement().hasIdPart()) {
			String uuid = newResource.getIdElement().getIdPart();
			if (cohortService.getCohortMByUuid(uuid) != null) {
				throw new ResourceVersionConflictException("A Group with ID " + uuid + " exists already");
			}
			cohort.setUuid(uuid);
		}
		mapper.toCohort(cohort, newResource);
		Date now = new Date();
		cohort.setStartDate(now);
		cohort.setGroupCohort(false);

		CohortM saved = cohortService.saveCohortM(cohort);
		syncMembers(saved, newResource, now);
		flush();
		return get(saved.getUuid());
	}

	@Override
	@Transactional
	public Group update(@Nonnull String uuid, @Nonnull Group updatedResource) {
		if (uuid == null) {
			throw new InvalidRequestException("Uuid cannot be null.");
		}
		if (updatedResource == null) {
			throw new InvalidRequestException("Resource cannot be null.");
		}
		if (updatedResource.getId() == null) {
			throw new InvalidRequestException("Group resource is missing id.");
		}
		if (!updatedResource.getIdElement().getIdPart().equals(uuid)) {
			throw new InvalidRequestException("Group id does not match resource id.");
		}

		CohortM cohort = cohortService.getCohortMByUuid(uuid);
		if (cohort == null) {
			throw resourceNotFound(uuid);
		}

		boolean inactive = updatedResource.hasActive() && !updatedResource.getActive();
		mapper.toCohort(cohort, updatedResource);
		if (inactive) {
			// A Group sent as inactive is deleted, which FHIR reads back as gone.
			cohortService.voidCohortM(cohort, "Deactivated through FHIR");
			return updatedResource;
		}
		CohortM saved = cohortService.saveCohortM(cohort);
		
		syncMembers(saved, updatedResource, new Date());
		flush();
		return get(uuid);
	}

	@Override
	@Transactional
	public void delete(@Nonnull String uuid) {
		if (uuid == null) {
			throw new InvalidRequestException("Uuid cannot be null.");
		}
		CohortM cohort = cohortService.getCohortMByUuid(uuid);
		if (cohort == null) {
			throw resourceNotFound(uuid);
		}
		cohortService.voidCohortM(cohort, "Deleted through FHIR");
	}

	/** Makes the active memberships of {@code cohort} those the Group lists. */
	private void syncMembers(CohortM cohort, Group group, Date now) {
		Map<String, Group.GroupMemberComponent> wanted = mapper.membersByPatient(group);

		for (CohortMember membership : cohort.getCohortMembers()) {
			if (!GroupCohortMapper.isActive(membership, now) || membership.getPatient() == null) {
				continue;
			}
			Group.GroupMemberComponent member = wanted.remove(membership.getPatient().getUuid());
			if (member == null) {
				cohortMemberService.voidCohortMember(membership, REMOVED_REASON);
			} else {
				Date end = mapper.endOf(member, now);
				if (end != null && !end.equals(membership.getEndDate())) {
					membership.setEndDate(end);
					cohortMemberService.saveCohortMember(membership);
				}
			}
		}

		for (Map.Entry<String, Group.GroupMemberComponent> entry : wanted.entrySet()) {
			CohortMember membership = new CohortMember(mapper.patientOf(entry.getKey()));
			membership.setCohort(cohort);
			membership.setStartDate(mapper.startOf(entry.getValue(), now));
			membership.setEndDate(mapper.endOf(entry.getValue(), now));
			cohort.getCohortMembers().add(cohortMemberService.saveCohortMember(membership));
		}
	}

	/** Writes what was saved, so that FHIR2's read finds it. */
	void flush() {
		Context.flushSession();
	}
}
