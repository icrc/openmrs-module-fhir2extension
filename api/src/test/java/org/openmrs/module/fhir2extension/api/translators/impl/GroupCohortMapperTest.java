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

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Date;
import java.util.List;

import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.junit.Test;
import org.openmrs.Location;
import org.openmrs.Person;
import org.openmrs.PersonName;
import org.openmrs.User;
import org.openmrs.module.cohort.CohortAttribute;
import org.openmrs.module.cohort.CohortAttributeType;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortType;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.fhir2extension.FhirConstants;

public class GroupCohortMapperTest {
	
	private final GroupCohortMapper mapper = new GroupCohortMapper();
	
	@Test
	public void withDetails_shouldTellWhereTheCohortIsItsTypeAndWhoChangedItWhen() {
		CohortM cohort = new CohortM();
		Location location = new Location();
		location.setUuid("loc-1");
		location.setName("Clinic A");
		cohort.setLocation(location);
		CohortType type = new CohortType();
		type.setUuid("type-1");
		type.setName("Diabetes");
		cohort.setCohortType(type);
		Date created = new Date(1700000000000L);
		Date changed = new Date(1700003600000L);
		cohort.setDateCreated(created);
		cohort.setDateChanged(changed);
		User editor = new User();
		editor.setUuid("user-9");
		Person person = new Person();
		person.addName(new PersonName("Ann", null, "Smith"));
		editor.setPerson(person);
		cohort.setChangedBy(editor);
		
		Group group = mapper.withDetails(new Group(), cohort);
		
		Reference locationValue = (Reference) group.getExtensionByUrl(FhirConstants.GROUP_LOCATION_EXTENSION_URL).getValue();
		assertThat(locationValue.getReference(), equalTo("Location/loc-1"));
		assertThat(locationValue.getDisplay(), equalTo("Clinic A"));
		Reference typeValue = (Reference) group.getExtensionByUrl(FhirConstants.GROUP_COHORT_TYPE_EXTENSION_URL).getValue();
		assertThat(typeValue.getReference(), equalTo("CohortType/type-1"));
		assertThat(typeValue.getDisplay(), equalTo("Diabetes"));
		assertThat(
		    ((DateTimeType) group.getExtensionByUrl(FhirConstants.GROUP_DATE_CREATED_EXTENSION_URL).getValue()).getValue(),
		    equalTo(created));
		assertThat(
		    ((DateTimeType) group.getExtensionByUrl(FhirConstants.GROUP_DATE_CHANGED_EXTENSION_URL).getValue()).getValue(),
		    equalTo(changed));
		Reference changedBy = (Reference) group.getExtensionByUrl(FhirConstants.GROUP_CHANGED_BY_EXTENSION_URL).getValue();
		assertThat(changedBy.getDisplay(), equalTo("Ann Smith"));
		assertThat(changedBy.getIdentifier().getValue(), equalTo("user-9"));
	}
	
	@Test
	public void withDetails_shouldSkipWhatTheCohortDoesNotHaveAndKeepWhatTheGroupSays() {
		Group group = new Group();
		group.addExtension().setUrl(FhirConstants.GROUP_LOCATION_EXTENSION_URL).setValue(new StringType("loc-old"));
		
		mapper.withDetails(group, new CohortM());
		
		assertThat(group.getExtensionByUrl(FhirConstants.GROUP_COHORT_TYPE_EXTENSION_URL), nullValue());
		assertThat(group.getExtensionByUrl(FhirConstants.GROUP_DATE_CHANGED_EXTENSION_URL), nullValue());
		assertThat(group.getExtensionByUrl(FhirConstants.GROUP_CHANGED_BY_EXTENSION_URL), nullValue());
		assertThat(group.getExtensionByUrl(FhirConstants.GROUP_LOCATION_EXTENSION_URL).getValue().primitiveValue(),
		    equalTo("loc-old"));
	}
	
	@Test
	public void withDetails_shouldListTheActiveAttributesWithTheirTypeAndValue() {
		CohortM cohort = new CohortM();
		cohort.addAttribute(attribute("Funding", "Ministry"));
		CohortAttribute voided = attribute("Old", "gone");
		voided.setVoided(true);
		cohort.addAttribute(voided);
		
		Group group = mapper.withDetails(new Group(), cohort);
		
		List<Extension> attributes = group.getExtensionsByUrl(FhirConstants.GROUP_ATTRIBUTE_EXTENSION_URL);
		assertThat(attributes, hasSize(1));
		assertThat(attributes.get(0).getExtensionByUrl("type").getValue().primitiveValue(), equalTo("Funding"));
		assertThat(attributes.get(0).getExtensionByUrl("value").getValue().primitiveValue(), equalTo("Ministry"));
	}
	
	@Test
	public void withDetails_shouldNotRepeatTheAttributesWhenCalledTwice() {
		CohortM cohort = new CohortM();
		cohort.addAttribute(attribute("Funding", "Ministry"));
		
		Group group = mapper.withDetails(mapper.withDetails(new Group(), cohort), cohort);
		
		assertThat(group.getExtensionsByUrl(FhirConstants.GROUP_ATTRIBUTE_EXTENSION_URL), hasSize(1));
	}
	
	@Test
	public void applyAttributes_shouldAddANewAttributeAndChangeAnExistingOne() {
		CohortService service = mock(CohortService.class);
		when(service.getCohortAttributeTypeByName("Funding")).thenReturn(attributeType("Funding"));
		when(service.getCohortAttributeTypeByName("Cadence")).thenReturn(attributeType("Cadence"));
		mapper.setCohortService(service);
		CohortM cohort = new CohortM();
		CohortAttribute old = attribute("Funding", "Ministry");
		cohort.addAttribute(old);
		Group group = new Group();
		addAttribute(group, "Funding", "NGO");
		addAttribute(group, "Cadence", "Weekly");
		
		mapper.applyAttributes(cohort, group);
		
		assertThat(old.getValueReference(), equalTo("NGO"));
		assertThat(cohort.getActiveAttributes(), hasSize(2));
		verify(service).saveCohortAttribute(old);
	}
	
	@Test
	public void applyAttributes_shouldVoidAnAttributeSentEmptyAndLeaveTheOthers() {
		CohortService service = mock(CohortService.class);
		when(service.getCohortAttributeTypeByName("Funding")).thenReturn(attributeType("Funding"));
		mapper.setCohortService(service);
		CohortM cohort = new CohortM();
		CohortAttribute funding = attribute("Funding", "Ministry");
		CohortAttribute other = attribute("Cadence", "Weekly");
		cohort.addAttribute(funding);
		cohort.addAttribute(other);
		Group group = new Group();
		addAttribute(group, "Funding", "");
		
		mapper.applyAttributes(cohort, group);
		
		verify(service).voidCohortAttribute(eq(funding), anyString());
		verify(service, never()).voidCohortAttribute(eq(other), anyString());
	}
	
	@Test(expected = UnprocessableEntityException.class)
	public void applyAttributes_shouldRefuseAnUnknownType() {
		mapper.setCohortService(mock(CohortService.class));
		Group group = new Group();
		addAttribute(group, "Nothing", "x");
		
		mapper.applyAttributes(new CohortM(), group);
	}
	
	private static void addAttribute(Group group, String type, String value) {
		Extension extension = group.addExtension();
		extension.setUrl(FhirConstants.GROUP_ATTRIBUTE_EXTENSION_URL);
		extension.addExtension().setUrl("type").setValue(new StringType(type));
		extension.addExtension().setUrl("value").setValue(new StringType(value));
	}
	
	private static CohortAttributeType attributeType(String name) {
		CohortAttributeType type = new CohortAttributeType();
		type.setName(name);
		return type;
	}
	
	private static CohortAttribute attribute(String typeName, String value) {
		CohortAttributeType type = new CohortAttributeType();
		type.setName(typeName);
		CohortAttribute attribute = new CohortAttribute();
		attribute.setAttributeType(type);
		// Reading the value needs the datatype of the type, which only a running server has: the
		// stored text is what is used when it cannot be read.
		attribute.setValueReferenceInternal(value);
		return attribute;
	}
}
