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
package org.openmrs.module.fhir2extension;

public final class FhirConstants {
	
	private FhirConstants() {
	}
	
	public static final String QUESTIONNAIRE = "Questionnaire";
	
	public static final String FHIR_QUESTIONNAIRE_TYPE = "FHIR Questionnaire";
	
	public static final String GROUP_COHORT_TYPE_EXTENSION_URL = "http://fhir.openmrs.org/ext/group/cohort-type";

	public static final String GROUP_LOCATION_EXTENSION_URL = "http://fhir.openmrs.org/ext/group/location";

	public static final String GROUP_DATE_CREATED_EXTENSION_URL = "http://fhir.openmrs.org/ext/group/date-created";

	public static final String GROUP_CHANGED_BY_EXTENSION_URL = "http://fhir.openmrs.org/ext/group/changed-by";

	public static final String GROUP_DATE_CHANGED_EXTENSION_URL = "http://fhir.openmrs.org/ext/group/date-changed";

	/** A custom attribute of the cohort, with a sub-extension {@code type} and one {@code value}. */
	public static final String GROUP_ATTRIBUTE_EXTENSION_URL = "http://fhir.openmrs.org/ext/group/attribute";

	public static final String FORM_SYSTEM_URI = "http://fhir.openmrs.org/core/StructureDefinition/omrs-form";
	
}
