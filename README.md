
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
