package main.audit;

import com.google.common.collect.ImmutableSet;
import models.ConvertedHymn;
import models.HymnType;
import models.HymnalDbKey;
import models.Reference;

import java.util.*;
import java.util.stream.Collectors;

public class RelevantAuditor {

    public static final ImmutableSet<ImmutableSet<HymnalDbKey>> HYMNAL_DB_RELEVANT_EXCEPTIONS =
            ImmutableSet.<ImmutableSet<HymnalDbKey>>builder()
                        .add(ImmutableSet.of(
                                // h/528, ns/306, and h/8444 are basically different versions of the same song.
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "528", null),
                                new HymnalDbKey(HymnType.NEW_SONG, "306", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "8444", null)))
                        .add(ImmutableSet.of(
                                // Both h/79 and h/8079 have the same chorus
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "79", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "8079", null)))
                        .add(ImmutableSet.of(
                                // Both ns/19 and ns/474 are two English translations of the same song
                                new HymnalDbKey(HymnType.NEW_SONG, "19", null),
                                new HymnalDbKey(HymnType.NEW_SONG, "474", null)))
                        .add(ImmutableSet.of(
                                // Both h/267 and h/1360 have the same chorus
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "267", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "1360", null)))
                        .add(ImmutableSet.of(
                                // h/720, h/8526, nt/720, and nt/720b have all different tunes of the same song
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "720", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "8526", null),
                                new HymnalDbKey(HymnType.NEW_TUNE, "720", null),
                                new HymnalDbKey(HymnType.NEW_TUNE, "720b", null)))
                        .add(ImmutableSet.of(
                                // h/666 is a brother Lee rewrite of h/8661
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "666", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "8661", null)))
                        .add(ImmutableSet.of(
                                // Both h/445 is h/1359 but without the chorus
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "445", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "1359", null)))
                        .add(ImmutableSet.of(
                                // Both h/1353 are h/8476 are alternate versions of each other (probably different
                                // translations of the same Chinese song)
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "1353", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "8476", null)))
                        .add(ImmutableSet.of(
                                // h/921 is the original and h/1358 is an adapted version
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "921", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "1358", null)))
                        .add(ImmutableSet.of(
                                // h/18 is the original and ns/7 is an adapted version
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "18", null),
                                new HymnalDbKey(HymnType.NEW_SONG, "7", null)))
                        .add(ImmutableSet.of(
                                // c/21 is a shortened version of h/70
                                new HymnalDbKey(HymnType.CHILDREN_SONG, "21", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "70", null)))
                        .add(ImmutableSet.of(
                                // c/162 is a shortened version of h/993
                                new HymnalDbKey(HymnType.CHILDREN_SONG, "162", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "993", null)))
                        .add(ImmutableSet.of(
                                // ns/179 is the adapted version of h/1248
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "1248", null),
                                new HymnalDbKey(HymnType.NEW_SONG, "179", null)))
                        .add(ImmutableSet.of(
                                // Both ns/154 and h/8330 are valid translations of the Chinese song ch/330.
                                new HymnalDbKey(HymnType.NEW_SONG, "154", null),
                                new HymnalDbKey(HymnType.CLASSIC_HYMN, "8330", null)))
                        .build();

    static void auditRelevantSets(Map<HymnalDbKey, ConvertedHymn> songs) {
        Set<Set<HymnalDbKey>> relevantSets = new HashSet<>();
        songs.forEach((hymnalDbKey, hymn) -> {
            Set<HymnalDbKey> relevantKeys =
                    hymn.relevantReferences()
                        .stream()
                        .map(reference -> reference.key)
                        .collect(Collectors.toSet());

            // No relevant keys, so this is just a long song and shouldn't be part of any relevant "set".
            if (relevantKeys.isEmpty()) {
                return;
            }

            Set<Set<HymnalDbKey>> containingSets =
                    relevantSets.stream().filter(existingSet -> {
                        for (HymnalDbKey relevantKey : relevantKeys) {
                            if (existingSet.contains(relevantKey)) {
                                return true;
                            }
                        }
                        return false;
                    }).collect(Collectors.toSet());
            if (containingSets.isEmpty()) {
                Set<HymnalDbKey> relevantSet = new HashSet<>(relevantKeys);
                relevantSet.add(hymnalDbKey);
                relevantSets.add(relevantSet);
            } else if (containingSets.size() == 1) {
                containingSets.stream().findFirst().get().addAll(relevantKeys);
            } else {
                // Each relevant reference should be in its unique set. If there are multiple matching sets, then
                // something is wrong.
                throw new IllegalArgumentException(relevantKeys + " was not in a unique set.");
            }
        });
        relevantSets.forEach(RelevantAuditor::auditRelevantSet);
    }

    /**
     * Audit set of {@link Reference}s to see if there are conflicting types.
     */
    public static void auditRelevantSet(Set<HymnalDbKey> setToAudit) {
        if (setToAudit.size() == 1) {
            throw new IllegalArgumentException("Relevant set with only 1 key is a dangling reference, which needs fixing: " + setToAudit);
        }

        // Extract the hymn types for audit.
        List<HymnType> hymnTypes = setToAudit.stream().map(relevant -> relevant.hymnType).collect(Collectors.toList());

        // Verify that the same hymn type doesn't appear more than the allowed number of times the relevant list.
        for (HymnType hymnType : HymnType.values()) {
            int timesAllowed = 1;

            // For each song like h/810, ns/698b, nt/394b, de/786b increment the allowance of that type of hymn,
            // since those are valid alternates.
            if ((hymnType == HymnType.CLASSIC_HYMN || hymnType == HymnType.NEW_TUNE || hymnType == HymnType.NEW_SONG || hymnType == HymnType.GERMAN)) {
                for (HymnalDbKey key : setToAudit) {
                    if (key.hymnType == hymnType && key.hymnNumber.matches("(\\D+\\d+\\D*)|(\\D*\\d+\\D+)")) {
                        timesAllowed++;
                    }
                }
            }

            // If the current set includes an exception group, then remove that exception group from the list and audit
            // again.
            for (Set<HymnalDbKey> exception : HYMNAL_DB_RELEVANT_EXCEPTIONS) {
                if (setToAudit.containsAll(exception)) {
                    if (!setToAudit.removeAll(exception)) {
                        throw new IllegalArgumentException(exception + " was unable to be removed from " + setToAudit);
                    }
                    auditRelevantSet(setToAudit);
                    return;
                }
            }

            // Verify that incompatible hymn types don't appear together the relevants list.
            if ((hymnTypes.contains(HymnType.CLASSIC_HYMN) && hymnTypes.contains(HymnType.NEW_SONG))
                || (hymnTypes.contains(HymnType.CLASSIC_HYMN) && hymnTypes.contains(HymnType.CHILDREN_SONG))
                || hymnTypes.contains(HymnType.CHILDREN_SONG) && hymnTypes.contains(HymnType.NEW_SONG)
                || hymnTypes.contains(HymnType.CHINESE) && hymnTypes.contains(HymnType.CHINESE_SUPPLEMENT)) {
                throw new IllegalArgumentException(String.format("%s has incompatible relevant types", setToAudit));
            }

            if (Collections.frequency(hymnTypes, hymnType) > timesAllowed) {
                throw new IllegalArgumentException(
                        String.format("%s has too many instances of %s", setToAudit, hymnType));
            }
        }
    }
}
