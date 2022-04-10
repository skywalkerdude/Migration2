package main;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import infra.DatabaseClient;
import models.*;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.*;
import java.util.stream.Collectors;

import static main.Main.LOGGER;

/**
 * Populates, audits, and makes a dense graph of all the songs in the hymnal db.
 */
public class RussianDbHandler {

    public final Map<HymnalDbKey, ConvertedHymn> allRussianHymns;

    public static RussianDbHandler create(DatabaseClient client) throws SQLException {
        return new RussianDbHandler(client);
    }

    public RussianDbHandler(DatabaseClient client) throws SQLException {
        this.allRussianHymns = populateHymns(client);
    }

    private Map<HymnalDbKey, ConvertedHymn> populateHymns(DatabaseClient client) throws SQLException {
        Map<HymnalDbKey, ConvertedHymn> allHymns = new LinkedHashMap<>();
        ResultSet resultSet = client.getDb().rawQuery("SELECT number, number_eng, first_string, html FROM hymns");
        if (resultSet == null) {
            throw new IllegalArgumentException("hymns-russian query returned null");
        }
        while (resultSet.next()) {
            int number = resultSet.getInt(1);
            int englishNumber = resultSet.getInt(2);
            String title = resultSet.getString(3);
            String html = resultSet.getString(4);
            Document doc = Jsoup.parse(html);
            String category = null;
            String subcategory = null;
            String meter = null;
            List<Verse> verses = new ArrayList<>();
            int verseNumber = 1;
            outerLoop:
            for (Element child : doc.body().child(0).children()) {
                String className = child.className();
                String text = child.text();
                switch (className) {
                    case "title" -> {
                        category = text;
                        continue outerLoop;
                    }
                    case "subtitle" -> {
                        subcategory = text;
                        continue outerLoop;
                    }
                    case "meter" -> {
                        meter = text;
                        continue outerLoop;
                    }
                }

                // Verse text
                Elements lyrics = child.getElementsByTag("td");
                for (Element element : lyrics) {
                    String elementText = element.text().replaceAll("[.*]", "").trim();
                    if (TextUtils.isEmpty(elementText)) {
                        continue;
                    }

                    if (TextUtils.isNumeric(elementText)) {
                        int textInt = Integer.parseInt(elementText);
                        if (textInt == verseNumber) {
                            verseNumber++;
                            continue;
                        } else {
                            throw new IllegalStateException("Verse numbers not consecutive for " + number);
                        }
                    }

                    final String verseType;
                    if (TextUtils.isEmpty(element.className())) {
                        verseType = "verse";
                    } else if ("chorus".equals(element.className())) {
                        verseType = "chorus";
                    } else {
                        verseType = "other";
                    }

                    // Extract the verse
                    List<TextNode> textNodes = element.textNodes();
                    Verse verse = new Verse();
                    verse.setVerseType(verseType);
                    verse.setVerseContent(textNodes.stream().map(TextNode::text).collect(Collectors.toList()));
                    verses.add(verse);
                }
            }

            ImmutableSet<Reference> languageReferences
                    = ImmutableSet.of(Reference.create("English",
                                                       new HymnalDbKey(HymnType.CLASSIC_HYMN, String.valueOf(englishNumber), null)));

            allHymns.put(new HymnalDbKey(HymnType.RUSSIAN, String.valueOf(number), null),
                         ConvertedHymn.builder()
                                      .title(title)
                                      .lyrics(ImmutableList.copyOf(verses))
                                      .category(category)
                                      .subCategory(subcategory)
                                      .meter(meter)
                                      .languageReferences(languageReferences)
                                      .build());
        }
        return allHymns;
    }

    public Map<HymnalDbKey, ConvertedHymn> combineRussianHymnsWith(Map<HymnalDbKey, ConvertedHymn> hymns) {
        Map<HymnalDbKey, ConvertedHymn> combinedHymns = new HashMap<>(hymns);
        allRussianHymns.forEach(
                (russianHymnalDbKey, russianHymn) ->
                        russianHymn.languageReferences().forEach(languageReference -> {
                            HymnalDbKey languageKey = languageReference.key;
                            if (combinedHymns.containsKey(languageKey)) {
                                ConvertedHymn languageHymn = hymns.get(languageKey);
                                combineReferences(languageKey, languageHymn, russianHymnalDbKey, russianHymn, combinedHymns);
                            } else {
                                LOGGER.finer(languageKey + " was not found in all hymns. Occurred in " + russianHymnalDbKey);
                            }
                        }));
        return combinedHymns;
    }

    private void combineReferences(HymnalDbKey key, ConvertedHymn hymn,
                                   HymnalDbKey russianKey, ConvertedHymn russianHymn,
                                   Map<HymnalDbKey, ConvertedHymn> combinedHymns) {
        Reference russianReference = Reference.create("Russian", russianKey);
        if (key.equals(russianReference.key) || hymn.languageReferences().contains(russianReference)) {
            return;
        }

        ImmutableSet<Reference> newRussianReferences =
                ImmutableSet.<Reference>builder()
                            .addAll(russianHymn.languageReferences())
                            // Don't add self reference.
                            .addAll(hymn.languageReferences()
                                        .stream()
                                        .filter(reference -> !russianReference.equals(reference))
                                        .collect(Collectors.toList()))
                            .build();
        ConvertedHymn newRussianHymn = russianHymn.toBuilder().languageReferences(newRussianReferences).build();
        combinedHymns.put(russianKey, newRussianHymn);

        ImmutableSet<Reference> newReferences =
                ImmutableSet.<Reference>builder()
                            .addAll(hymn.languageReferences())
                            .add(russianReference)
                            .build();
        ConvertedHymn newHymn = hymn.toBuilder().languageReferences(newReferences).build();
        combinedHymns.put(key, newHymn);

        hymn.languageReferences()
            .forEach(reference ->
                             combineReferences(reference.key, combinedHymns.get(reference.key),
                                               russianKey, newRussianHymn, combinedHymns));
    }
}
