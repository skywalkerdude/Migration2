package main.audit;

import com.google.common.collect.ImmutableSet;
import models.ConvertedHymn;
import models.HymnType;
import models.HymnalDbKey;

import java.util.*;
import java.util.stream.Collectors;

public class LanguageAuditor {

    public static final ImmutableSet<ImmutableSet<HymnalDbKey>> HYMNAL_DB_LANGUAGES_EXCEPTIONS =
            ImmutableSet.<ImmutableSet<HymnalDbKey>>builder()
                        .add(ImmutableSet.of(
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "1353", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "8476", null),
                                new HymnalDbKey(HymnType.FRENCH, "129", null),
                                new HymnalDbKey(HymnType.TAGALOG, "1353", null),
                                new HymnalDbKey(HymnType.CHINESE, "476", null),
                                new HymnalDbKey(HymnType.CHINESE, "476", "?gb=1")))
                        .add(ImmutableSet.of(
                                // Both h/8330 and ns/154 are valid translations of the Chinese song ch/330.
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "8330", null),
                                new HymnalDbKey(HymnType.NEW_SONG, "154", null)))
                        .add(ImmutableSet.of(
                                // Both ns/19 and ns/474 are valid translations of the Chinese song ts/428.
                                new HymnalDbKey(HymnType.NEW_SONG, "19", null),
                                new HymnalDbKey(HymnType.NEW_SONG, "474", null)))
                        .add(ImmutableSet.of(
                                // h/505 seems to have two linked Chinese songs, that from my investigation via Google
                                // Translate, both are valid translations of that song.
                                new HymnalDbKey(HymnType.CHINESE, "383", null),
                                new HymnalDbKey(HymnType.CHINESE, "383", "?gb=1"),
                                new HymnalDbKey(HymnType.CHINESE_SUPPLEMENT, "27", null),
                                new HymnalDbKey(HymnType.CHINESE_SUPPLEMENT, "27", "?gb=1")))
                        .add(ImmutableSet.of(
                                // h/893 seems to have two linked Chinese songs, that from my investigation via Google
                                // Translate, both are valid translations of that song.
                                new HymnalDbKey(HymnType.CHINESE, "641", null),
                                new HymnalDbKey(HymnType.CHINESE, "641", "?gb=1"),
                                new HymnalDbKey(HymnType.CHINESE_SUPPLEMENT, "917", null),
                                new HymnalDbKey(HymnType.CHINESE_SUPPLEMENT, "917", "?gb=1")))
                        .add(ImmutableSet.of(
                                // h/1353 and h/8476 are essentially two slightly different versions of the same song.
                                // So both should link to the same set of translations, since the lyrics are very
                                // similar.
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "1353", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "8476", null),
                                new HymnalDbKey(HymnType.TAGALOG, "1353", null),
                                new HymnalDbKey(HymnType.CHINESE, "476", null),
                                new HymnalDbKey(HymnType.CHINESE, "476", "?gb=1")))
                        .add(ImmutableSet.of(
                                // T437 is from H4A, and seems like also a valid translation of h/437 as well as ht/c333
                                new HymnalDbKey(HymnType.TAGALOG, "c333", null),
                                new HymnalDbKey(HymnType.TAGALOG, "437", null)))
                        .build();

    static void auditLanguageSets(Map<HymnalDbKey, ConvertedHymn> songs) {
        Set<Set<HymnalDbKey>> languageSets = new HashSet<>();
        songs.forEach((hymnalDbKey, hymn) -> {
            Set<HymnalDbKey> languageKeys =
                    hymn.languageReferences()
                        .stream()
                        .map(reference -> reference.key)
                        .collect(Collectors.toSet());

            // No language keys, so this is just a long song and shouldn't be part of any language "set".
            if (languageKeys.isEmpty()) {
                return;
            }

            Set<Set<HymnalDbKey>> containingSets =
                    languageSets.stream().filter(existingSet -> {
                        for (HymnalDbKey languageKey : languageKeys) {
                            if (existingSet.contains(languageKey)) {
                                return true;
                            }
                        }
                        return false;
                    }).collect(Collectors.toSet());
            if (containingSets.isEmpty()) {
                Set<HymnalDbKey> languageSet = new HashSet<>(languageKeys);
                languageSet.add(hymnalDbKey);
                languageSets.add(languageSet);
            } else if (containingSets.size() == 1) {
                containingSets.stream().findFirst().get().addAll(languageKeys);
            } else {
                // Each language reference should be in its unique set. If there are multiple matching sets, then
                // something is wrong.
                throw new IllegalArgumentException(languageKeys + " was not in a unique set.");
            }
        });
        languageSets.forEach(LanguageAuditor::auditLanguageSet);
    }

    /**
     * Audit set of {@link HymnalDbKey}s to see if there are conflicting types.
     */
    private static void auditLanguageSet(Set<HymnalDbKey> setToAudit) {
        if (setToAudit.size() == 1) {
            throw new IllegalArgumentException("Language set with only 1 key is a dangling reference, which needs fixing: " + setToAudit);
        }

        // Extract the hymn types for audit.
        List<HymnType> hymnTypes = setToAudit.stream().map(language -> language.hymnType).collect(Collectors.toList());

        // Verify that the same hymn type doesn't appear more than the allowed number of times the languages list.
        for (HymnType hymnType : HymnType.values()) {
            int timesAllowed = 1;
            if (hymnType == HymnType.CHINESE || hymnType == HymnType.CHINESE_SUPPLEMENT) {
                timesAllowed = 2;
            }
            // For each song like h225b or ns92f, increment the allowance of that type of hymn, since those are valid
            // alternates.
            if ((hymnType == HymnType.CLASSIC_HYMN || hymnType == HymnType.NEW_SONG || hymnType == HymnType.HOWARD_HIGASHI)) {
                for (HymnalDbKey key : setToAudit) {
                    if (key.hymnType == hymnType && key.hymnNumber.matches("(\\D+\\d+\\D*)|(\\D*\\d+\\D+)")) {
                        timesAllowed++;
                    }
                }
            }

            // If the current set includes an exception group, then remove that exception group from the list and audit
            // again.
            for (Set<HymnalDbKey> exception : HYMNAL_DB_LANGUAGES_EXCEPTIONS) {
                if (setToAudit.containsAll(exception)) {
                    if (!setToAudit.removeAll(exception)) {
                        throw new IllegalArgumentException(exception + " was unable to be removed from " + setToAudit);
                    }
                    auditLanguageSet(setToAudit);
                    return;
                }
            }

            if (Collections.frequency(hymnTypes, hymnType) > timesAllowed) {
                throw new IllegalArgumentException(
                        String.format("%s has too many instances of %s", setToAudit, hymnType));
            }
        }

        // Verify that incompatible hymn types don't appear together the languages list.
        if ((hymnTypes.contains(HymnType.CLASSIC_HYMN) && hymnTypes.contains(HymnType.NEW_SONG))
            || (hymnTypes.contains(HymnType.CLASSIC_HYMN) && hymnTypes.contains(HymnType.CHILDREN_SONG))
            || hymnTypes.contains(HymnType.CHILDREN_SONG) && hymnTypes.contains(HymnType.NEW_SONG)
            || hymnTypes.contains(HymnType.CHINESE) && hymnTypes.contains(HymnType.CHINESE_SUPPLEMENT)) {
            throw new IllegalArgumentException(
                    String.format("%s has incompatible languages types", setToAudit));
        }
    }
}
