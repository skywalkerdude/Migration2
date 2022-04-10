package main.audit;

import models.ConvertedHymn;
import models.HymnalDbKey;

import java.util.Map;

import static main.audit.LanguageAuditor.auditLanguageSets;
import static main.audit.RelevantAuditor.auditRelevantSets;

public class Auditor {

    public static void audit(Map<HymnalDbKey, ConvertedHymn> songs) {
        auditSelfReferences(songs);
        auditLanguageSets(songs);
        auditRelevantSets(songs);
    }

    /**
     * Ensure that no song references itself in either the language or relevant list.
     */
    private static void auditSelfReferences(Map<HymnalDbKey, ConvertedHymn> songs) {
        songs.forEach((key, hymn) -> {
            hymn.languageReferences().stream().map(reference -> reference.key).forEach(referenceKey -> {
                if (key.equals(referenceKey)) {
                    throw new IllegalStateException(key + " has a language self-reference");
                }
            });

            hymn.relevantReferences().stream().map(reference -> reference.key).forEach(referenceKey -> {
                if (key.equals(referenceKey)) {
                    throw new IllegalStateException(key + " has a relevant self-reference");
                }
            });
        });
    }


}
