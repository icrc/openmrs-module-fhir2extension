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

import static org.openmrs.module.fhir2.FhirConstants.OPENMRS_FHIR_EXT_GROUP_DESCRIPTION;
import static org.openmrs.module.fhir2extension.FhirConstants.GROUP_ATTRIBUTE_EXTENSION_URL;
import static org.openmrs.module.fhir2extension.FhirConstants.GROUP_CHANGED_BY_EXTENSION_URL;
import static org.openmrs.module.fhir2extension.FhirConstants.GROUP_COHORT_TYPE_EXTENSION_URL;
import static org.openmrs.module.fhir2extension.FhirConstants.GROUP_DATE_CHANGED_EXTENSION_URL;
import static org.openmrs.module.fhir2extension.FhirConstants.GROUP_DATE_CREATED_EXTENSION_URL;
import static org.openmrs.module.fhir2extension.FhirConstants.GROUP_LOCATION_EXTENSION_URL;

import java.util.ArrayList;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import ca.uhn.fhir.rest.server.exceptions.InvalidRequestException;
import ca.uhn.fhir.rest.server.exceptions.UnprocessableEntityException;
import org.hl7.fhir.r4.model.DateTimeType;
import org.hl7.fhir.r4.model.Extension;
import org.hl7.fhir.r4.model.Group;
import org.hl7.fhir.r4.model.Identifier;
import org.hl7.fhir.r4.model.Reference;
import org.hl7.fhir.r4.model.StringType;
import org.hl7.fhir.r4.model.Type;
import org.openmrs.Location;
import org.openmrs.Patient;
import org.openmrs.User;
import org.openmrs.api.LocationService;
import org.openmrs.api.PatientService;
import org.openmrs.module.cohort.CohortAttribute;
import org.openmrs.module.cohort.CohortAttributeType;
import org.openmrs.module.cohort.CohortM;
import org.openmrs.module.cohort.CohortMember;
import org.openmrs.module.cohort.CohortType;
import org.openmrs.module.cohort.api.CohortService;
import org.openmrs.module.cohort.api.CohortTypeService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

/**
 * Moves what a FHIR Group says into the cohort module's entities ({@link CohortM} and its
 * {@link CohortMember}s), and the active members of a cohort back into a Group.
 * <p>
 * FHIR2 maps a Group to the core cohort, which the cohort module refuses to save while it is active
 * ("A standard cohort should not be created while the cohort module is active"). The extension
 * works on the module's own entities instead.
 */
@Component
public class GroupCohortMapper {
	
	@Autowired
	private PatientService patientService;
	
	@Autowired
	private LocationService locationService;
	
	@Autowired
	private CohortTypeService cohortTypeService;
	
	@Autowired
	private CohortService cohortService;
	
	public void setPatientService(PatientService patientService) {
		this.patientService = patientService;
	}
	
	public void setLocationService(LocationService locationService) {
		this.locationService = locationService;
	}
	
	public void setCohortTypeService(CohortTypeService cohortTypeService) {
		this.cohortTypeService = cohortTypeService;
	}
	
	public void setCohortService(CohortService cohortService) {
		this.cohortService = cohortService;
	}
	
	/**
	 * Copies what the Group says about the cohort itself (not its members) into {@code cohort}.
	 * Whatever the Group does not mention is left as it is.
	 */
	public CohortM toCohort(CohortM cohort, Group group) {
		if (group.hasName()) {
			cohort.setName(group.getName());
		}
		
		Extension description = group.getExtensionByUrl(OPENMRS_FHIR_EXT_GROUP_DESCRIPTION);
		if (description != null && description.hasValue()) {
			cohort.setDescription(description.getValue().primitiveValue());
		}
		
		String cohortTypeUuid = uuidOf(group.getExtensionByUrl(GROUP_COHORT_TYPE_EXTENSION_URL));
		if (cohortTypeUuid != null) {
			CohortType cohortType = cohortTypeService.getCohortTypeByUuid(cohortTypeUuid);
			if (cohortType == null) {
				throw new UnprocessableEntityException("Unknown cohort type " + cohortTypeUuid);
			}
			cohort.setCohortType(cohortType);
		}
		
		String locationUuid = uuidOf(group.getExtensionByUrl(GROUP_LOCATION_EXTENSION_URL));
		if (locationUuid != null) {
			Location location = locationService.getLocationByUuid(locationUuid);
			if (location == null) {
				throw new UnprocessableEntityException("Unknown location " + locationUuid);
			}
			cohort.setLocation(location);
		}
		
		if (group.hasActive()) {
			cohort.setVoided(!group.getActive());
			if (group.getActive()) {
				cohort.setVoidedBy(null);
				cohort.setDateVoided(null);
				cohort.setVoidReason(null);
			}
		}
		return cohort;
	}
	
	/**
	 * Sets the attributes the Group lists (one extension per attribute, with the {@code type} name and the
	 * {@code value} as text) on the saved {@code cohort}. An attribute the Group does not list is left as it
	 * is; one listed with an empty value is voided. An unknown type: 422.
	 */
	public void applyAttributes(CohortM cohort, Group group) {
		for (Extension extension : group.getExtensionsByUrl(GROUP_ATTRIBUTE_EXTENSION_URL)) {
			String typeName = textOf(extension.getExtensionByUrl("type"));
			if (typeName == null) {
				throw new InvalidRequestException("An attribute of the Group needs a type");
			}
			CohortAttributeType type = cohortService.getCohortAttributeTypeByName(typeName);
			if (type == null) {
				throw new UnprocessableEntityException("Unknown attribute type " + typeName);
			}
			String value = textOf(extension.getExtensionByUrl("value"));
			CohortAttribute current = activeAttributeOf(cohort, type);
			if (value == null || value.isEmpty()) {
				if (current != null) {
					cohortService.voidCohortAttribute(current, "Removed through FHIR");
				}
				continue;
			}
			if (current == null) {
				current = new CohortAttribute();
				current.setAttributeType(type);
				cohort.addAttribute(current);
			}
			current.setValueReferenceInternal(value);
			cohortService.saveCohortAttribute(current);
		}
	}
	
	private static CohortAttribute activeAttributeOf(CohortM cohort, CohortAttributeType type) {
		for (CohortAttribute attribute : cohort.getActiveAttributes()) {
			if (attribute.getAttributeType() != null && type.getName().equals(attribute.getAttributeType().getName())) {
				return attribute;
			}
		}
		return null;
	}
	
	private static String textOf(Extension extension) {
		return extension != null && extension.hasValue() ? extension.getValue().primitiveValue() : null;
	}
	
	/** The members the Group names, by patient uuid, in the order given. */
	public Map<String, Group.GroupMemberComponent> membersByPatient(Group group) {
		Map<String, Group.GroupMemberComponent> members = new LinkedHashMap<>();
		for (Group.GroupMemberComponent member : group.getMember()) {
			String uuid = uuidOf(member.getEntity());
			if (uuid == null) {
				throw new InvalidRequestException("Group.member.entity must be a reference to a Patient");
			}
			members.put(uuid, member);
		}
		return members;
	}
	
	public Patient patientOf(String uuid) {
		Patient patient = patientService.getPatientByUuid(uuid);
		if (patient == null) {
			throw new UnprocessableEntityException("Unknown patient " + uuid);
		}
		return patient;
	}
	
	/** When the membership starts: the period of the member, or {@code fallback}. */
	public Date startOf(Group.GroupMemberComponent member, Date fallback) {
		return member.hasPeriod() && member.getPeriod().hasStart() ? member.getPeriod().getStart() : fallback;
	}
	
	/** When the membership ends: the period of the member, now if it is inactive, else none. */
	public Date endOf(Group.GroupMemberComponent member, Date now) {
		if (member.hasPeriod() && member.getPeriod().hasEnd()) {
			return member.getPeriod().getEnd();
		}
		return member.hasInactive() && member.getInactive() ? now : null;
	}
	
	/**
	 * Makes the members of {@code group} those that are active in {@code cohort}: a voided or an
	 * ended membership is not listed, and the quantity follows. Entries already in the Group for an
	 * active member are kept as they are (display, extensions).
	 */
	public Group withActiveMembers(Group group, CohortM cohort) {
		Date now = new Date();
		Map<String, CohortMember> active = new LinkedHashMap<>();
		for (CohortMember membership : cohort.getCohortMembers()) {
			if (isActive(membership, now) && membership.getPatient() != null) {
				active.put(membership.getPatient().getUuid(), membership);
			}
		}

		List<Group.GroupMemberComponent> members = new ArrayList<>();
		for (Group.GroupMemberComponent member : group.getMember()) {
			String uuid = uuidOf(member.getEntity());
			if (uuid != null && active.remove(uuid) != null) {
				members.add(member);
			}
		}
		for (Map.Entry<String, CohortMember> left : active.entrySet()) {
			Group.GroupMemberComponent member = new Group.GroupMemberComponent();
			member.setEntity(new Reference("Patient/" + left.getKey()).setType("Patient"));
			if (left.getValue().getStartDate() != null) {
				member.getPeriod().setStart(left.getValue().getStartDate());
			}
			if (left.getValue().getEndDate() != null) {
				member.getPeriod().setEnd(left.getValue().getEndDate());
			}
			members.add(member);
		}

		group.setMember(members);
		group.setQuantity(members.size());
		return group;
	}
	
	/**
	 * Adds what the Group does not say by itself and a client needs to show the cohort: where it
	 * is, of which type, who created and last changed it and when, and the cohort module's own
	 * attributes. {@link #toCohort} ignores these extensions when a Group is written; the attributes
	 * are written by {@link #applyAttributes}.
	 * <p>
	 * The creator is not repeated here: FHIR2 already sets it as {@code Group.managingEntity}.
	 */
	public Group withDetails(Group group, CohortM cohort) {
		if (cohort.getLocation() != null) {
			Reference location = new Reference("Location/" + cohort.getLocation().getUuid());
			location.setDisplay(cohort.getLocation().getName());
			replaceExtension(group, GROUP_LOCATION_EXTENSION_URL, location);
		}
		if (cohort.getCohortType() != null) {
			Reference type = new Reference("CohortType/" + cohort.getCohortType().getUuid());
			type.setDisplay(cohort.getCohortType().getName());
			replaceExtension(group, GROUP_COHORT_TYPE_EXTENSION_URL, type);
		}
		if (cohort.getDateCreated() != null) {
			replaceExtension(group, GROUP_DATE_CREATED_EXTENSION_URL, new DateTimeType(cohort.getDateCreated()));
		}
		if (cohort.getChangedBy() != null) {
			replaceExtension(group, GROUP_CHANGED_BY_EXTENSION_URL, userReference(cohort.getChangedBy()));
		}
		if (cohort.getDateChanged() != null) {
			replaceExtension(group, GROUP_DATE_CHANGED_EXTENSION_URL, new DateTimeType(cohort.getDateChanged()));
		}

		group.getExtension().removeIf(extension -> GROUP_ATTRIBUTE_EXTENSION_URL.equals(extension.getUrl()));
		for (CohortAttribute attribute : cohort.getActiveAttributes()) {
			String value = valueOf(attribute);
			if (attribute.getAttributeType() == null || value == null) {
				continue;
			}
			Extension extension = group.addExtension();
			extension.setUrl(GROUP_ATTRIBUTE_EXTENSION_URL);
			extension.addExtension().setUrl("type").setValue(new StringType(attribute.getAttributeType().getName()));
			extension.addExtension().setUrl("value").setValue(new StringType(value));
		}
		return group;
	}
	
	/** A user as a reference that shows their name; the uuid goes in the identifier. */
	private static Reference userReference(User user) {
		Reference reference = new Reference();
		reference.setDisplay(user.getPerson() != null && user.getPerson().getPersonName() != null ? user.getPerson()
		        .getPersonName().getFullName() : user.getUsername());
		reference.setIdentifier(new Identifier().setValue(user.getUuid()));
		return reference;
	}
	
	/** What an attribute holds, as text; a value that cannot be read is skipped. */
	private static String valueOf(CohortAttribute attribute) {
		try {
			Object value = attribute.getValue();
			return value == null ? null : String.valueOf(value);
		}
		catch (RuntimeException e) {
			return attribute.getValueReference();
		}
	}
	
	private static void replaceExtension(Group group, String url, Type value) {
		group.getExtension().removeIf(extension -> url.equals(extension.getUrl()));
		group.addExtension().setUrl(url).setValue(value);
	}
	
	/** Not voided, started and not yet ended. */
	public static boolean isActive(CohortMember membership, Date now) {
		return !Boolean.TRUE.equals(membership.getVoided())
		        && (membership.getStartDate() == null || !membership.getStartDate().after(now))
		        && (membership.getEndDate() == null || !membership.getEndDate().before(now));
	}
	
	/** The uuid an extension (a string or a reference) points at, or null. */
	private static String uuidOf(Extension extension) {
		return extension != null && extension.hasValue() ? uuidOf(extension.getValue()) : null;
	}
	
	private static String uuidOf(Type value) {
		if (value == null) {
			return null;
		}
		String raw = value instanceof Reference ? ((Reference) value).getReference() : value.primitiveValue();
		if (raw == null || raw.trim().isEmpty()) {
			return null;
		}
		String trimmed = raw.trim();
		return trimmed.substring(trimmed.lastIndexOf('/') + 1);
	}
}
