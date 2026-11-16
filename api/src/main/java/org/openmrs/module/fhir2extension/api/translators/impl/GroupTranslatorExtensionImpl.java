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
package org.openmrs.module.fhir2extension.api.translators.impl;

import org.hl7.fhir.r4.model.Group;
import org.openmrs.Cohort;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.fhir2.api.translators.impl.GroupTranslatorImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Primary;
import org.springframework.stereotype.Component;

/**
 * The Group FHIR2 builds from a core cohort, completed from the cohort module's own entity: the members
 * (and the quantity) are the active ones only, and the cohort's location, type, dates, last editor and
 * attributes are added (see {@link GroupCohortMapper#withDetails}).
 * <p>
 * It sits in the translator, not in the service, so that a search gives the same answer as a read: a
 * list of cohorts that counted voided members would not agree with the cohort once opened.
 */
@Primary
@Component
public class GroupTranslatorExtensionImpl extends GroupTranslatorImpl {

	@Autowired
	private CohortService cohortService;

	@Autowired
	private GroupCohortMapper mapper;

	void setCohortService(CohortService cohortService) {
		this.cohortService = cohortService;
	}

	void setMapper(GroupCohortMapper mapper) {
		this.mapper = mapper;
	}

	@Override
	public Group toFhirResource(Cohort cohort) {
		Group group = super.toFhirResource(cohort);
		CohortM own = cohort.getUuid() == null ? null : cohortService.getCohortMByUuid(cohort.getUuid());
		if (own == null) {
			return group;
		}
		return mapper.withDetails(mapper.withActiveMembers(group, own), own);
	}
}
