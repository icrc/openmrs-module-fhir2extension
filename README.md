
openmrs-module-fhir2Extension
==========================


Description
-----------
This module's objective is to provide an extension to the `openmrs-module-fhir2`.

## Questionnaire Support

This module extends the capabilities of the [OpenMRS FHIR2](https://github.com/openmrs/openmrs-module-fhir2) module by introducing support for the `Questionnaire` resource. It enables fetching questionnaires from the OpenMRS backend and exposing them through the FHIR API, facilitating interoperability with external FHIR-compliant systems.

**Key Features**

Questionnaire Resource Support:
- Fetches questionnaires from the OpenMRS backend.
- Exposes questionnaires through the FHIR API in compliance with the   
  FHIR specification.

**Interoperability:**

Enables seamless integration with external systems that require FHIR-compatible questionnaire data.

**Configuration:**

The folder containing JSON schemas for questionnaires is configured via the global property `fhir2extension.questionnaires.folder`. For example:

    <globalProperty>
	    <property>fhir2extension.questionnaires.folder</property>
	    <value>/opt/openmrs-init-data/configuration/questionnaires/</value>
	</globalProperty>

Ensure this property points to the directory where your questionnaire JSON schemas are stored. The module will use this location to fetch and expose the questionnaires.

**Usage:**

Once installed, the module provides endpoints under the FHIR API to interact with Questionnaire resources. These endpoints can be used to retrieve and manage questionnaires as per the FHIR standards.

**Example endpoint:**
In module FHIR Questionnaires correspond to an OpenMRS Form Resource with the json for the FHIR Questionnaire.

    GET /ws/fhir2/R4/Questionnaire

## Custom Patient Translator Extension: Injecting Location in OpenMRS

The `PatientTranslatorExtensionImpl` overrides the default `PatientTranslator` in OpenMRS to ensure that a location is injected into the user's context when transforming a `FHIR Patient` object into an `OpenMRS Patient`.

In OpenMRS, a location is required to create a patient. This implementation extracts the location UUID from the `FHIR Patient` object (via a custom extension) and sets the user's context location accordingly. If the location is not already set in the context, the system resolves the location from the provided UUID and assigns it.

This customization simplifies workflows by automatically ensuring the required location is available during patient creation.

Building from Source
--------------------
You will need to have Java 1.8+ and Maven 2.x+ installed.  Use the command 'mvn package' to
compile and package the module.  The .omod file will be in the omod/target folder.

Alternatively you can add the snippet provided in the [Creating Modules](https://wiki.openmrs.org/x/cAEr) page to your
omod/pom.xml and use the mvn command:

    mvn package -P deploy-web -D deploy.path="../../openmrs-1.8.x/webapp/src/main/webapp"

It will allow you to deploy any changes to your web
resources such as jsp or js files without re-installing the module. The deploy path says
where OpenMRS is deployed.

Installation
------------
1. Build the module to produce the .omod file.
2. Use the OpenMRS Administration > Manage Modules screen to upload and install the .omod file.
3. Set the global property fhir2extension.questionnaires.folder to the directory path where the files are located.

If uploads are not allowed from the web (changable via a runtime property), you can drop the omod
into the ~/.OpenMRS/modules folder.  (Where ~/.OpenMRS is assumed to be the Application
Data Directory that the running openmrs is currently using.)  After putting the file in there
simply restart OpenMRS/tomcat and the module will be loaded and started.

## Group (patient list) support

With the [Cohort Module](https://github.com/openmrs/openmrs-module-cohort) installed, FHIR2 cannot save a `Group`: it maps it to the core cohort, which the Cohort Module refuses ("A standard cohort should not be created while the cohort module is active"). This module saves Groups as the Cohort Module's own `CohortM` and `CohortMember`, so a Group can be created, edited, deleted and have its members changed through the FHIR API. It needs the Cohort Module (`org.openmrs.module.cohort`, built against 3.7.3) and its privileges (`Edit Cohorts` to write).

### Requests

| Request | Effect |
|---|---|
| `PUT Group/{id}` for an id the server does not have | **Creates** the cohort with that id (FHIR's update as create). This is how a client that chose the id itself (a device that was offline) creates one, and sending it again is harmless: it is then an update. The name must be unique (the Cohort Module checks it: **422**). |
| `POST Group` | Creates the cohort; **the server gives the id**: HAPI replaces the id of a created resource, so a client that needs to know the id beforehand uses `PUT`. |
| `PUT Group/{id}` for an existing id | Updates the attributes (name, description, type, location, `active`). **A Group without `member` leaves the members as they are.** With `member`, it **sets the members to those listed**, as FHIR says: listed patients that are missing are added, patients no longer listed are voided. `active: false` voids the cohort. |
| `DELETE Group/{id}` | Voids the cohort. |
| `POST Group/{id}/$add-members` | Adds the patients (see below). |
| `POST Group/{id}/$remove-members` | Ends the membership of the patients (see below). |
| `PATCH Group/{id}` | Not supported (FHIR2's Group provider has no patch). |

`$add-members` and `$remove-members` take a `Parameters` resource with one `patient` parameter per patient, each a `valueReference` to `Patient/<uuid>`:

```json
{ "resourceType": "Parameters",
  "parameter": [ { "name": "patient", "valueReference": { "reference": "Patient/<uuid>" } } ] }
```

They answer with the Group as it is afterwards. They change what is asked and nothing else, so two clients that edit the same cohort do not undo each other, and a request sent twice does no harm: a patient who already is a member is not added again, a patient who is not a member is ignored when removed. One unknown patient adds nobody (**422**); an unknown Group is **404**; a parameter that is not a `Patient` reference, or no parameter at all, is **400**. A membership that was removed is voided, not deleted, so the history stays; adding the patient again starts a new membership.

Use these two for members and `PUT` for the attributes: `PUT` with a member list replaces it, which is wrong for a client that was offline while someone else edited the cohort.

### What a Group carries

| Group | Cohort |
|---|---|
| `name` | name |
| extension `http://fhir.openmrs.org/ext/group/description` (string) | description |
| extension `http://fhir.openmrs.org/ext/group/cohort-type` (string or reference, the cohort type uuid) | cohort type; unknown uuid: 422 |
| extension `http://fhir.openmrs.org/ext/group/location` (string or reference, the location uuid) | location; unknown uuid: 422 |
| `active` | `false` voids the cohort |
| `member[].entity` (`Patient/<uuid>`) | membership of that patient; unknown patient: 422 |
| `member[].period.start` / `.end`, `member[].inactive` | start and end date of the membership (`inactive` without an end ends it now) |

### What a read adds

Reads (one Group and searches alike) are FHIR2's own, completed from the cohort:

| Group | From |
|---|---|
| `member`, `quantity` | the memberships that are active (not voided, started and not ended); FHIR2 alone lists voided ones too |
| `managingEntity` | the creator (FHIR2) |
| `meta.lastUpdated` | the last change (FHIR2) |
| extension `.../ext/group/location` | `valueReference` `Location/<uuid>` with the name as `display` |
| extension `.../ext/group/cohort-type` | `valueReference` `CohortType/<uuid>` with the name as `display` |
| extension `.../ext/group/date-created`, `.../ext/group/date-changed` | `valueDateTime` |
| extension `.../ext/group/changed-by` | `valueReference` with the person's name as `display` and the user uuid as `identifier.value` |
| extension `.../ext/group/attribute` (one per active attribute) | sub-extensions `type` (the attribute type's name) and `value`, both strings |

These extensions are read only: they are ignored when a Group is sent. A client that wants a light list can ask for `Group?_elements=id,name,quantity,extension,managingEntity,meta`.

### Deployment notes

* `FhirGroupServiceExtensionImpl` and `GroupTranslatorExtensionImpl` are `@Primary` beans that replace FHIR2's Group service and translator. Another module that does the same (the `icrc` module might) would make the server fail to start with two primary beans: remove one of them.
* `GroupMembershipFhirResourceProvider` is a second provider of the type Group, for the two operations. HAPI merges the methods of every provider of a resource type (checked in `RestfulServer.findResourceMethods` of HAPI 5.7), but this has not been run on a server: if the server refuses to start with a message about a provider of Group, the two methods belong in a subclass of FHIR2's `GroupFhirResourceProvider`.
* Never delete a cohort member with `?purge=true` through the Cohort Module's REST: it tries to delete the patient row.
