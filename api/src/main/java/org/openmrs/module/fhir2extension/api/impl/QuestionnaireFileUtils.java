package org.openmrs.module.fhir2extension.api.impl;

import ca.uhn.fhir.context.FhirContext;
import ca.uhn.fhir.parser.IParser;
import org.hl7.fhir.r4.model.Questionnaire;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.apache.commons.lang3.Validate.notNull;

public class QuestionnaireFileUtils {
	
	private static final String JSON_EXTENSION = ".json";
	
	public static Questionnaire getQuestionnaire(String questionnairesFolder, String uuid) {
		return getQuestionnaireByFilename(questionnairesFolder, uuid + JSON_EXTENSION);
	}
	
	public static List<Questionnaire> getAllQuestionnaires(String questionnairesFolder) {
        notNull(questionnairesFolder, "The questionnaires folder is not set");
        File folder = new File(questionnairesFolder);
        if (folder.isDirectory()){
            return Arrays.stream(folder.listFiles()).map(file -> getQuestionnaireByFilename(questionnairesFolder, file.getName())).collect(Collectors.toList());
        }
        return new ArrayList<>();
    }
	
	private static Questionnaire getQuestionnaireByFilename(String questionnairesFolder, String fileName) {
        notNull(questionnairesFolder, "The questionnaires folder is not set");
        Questionnaire questionnaire = null;
        String filePath = questionnairesFolder + fileName;
        try (FileReader reader = new FileReader(filePath)) {
            FhirContext ctx = FhirContext.forR4();
            IParser p = ctx.newJsonParser();
            questionnaire = p.parseResource(Questionnaire.class, reader);
        } catch (IOException e) {
            // Ignore exception
        }
        return questionnaire;
    }
}
