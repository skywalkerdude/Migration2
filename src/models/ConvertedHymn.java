package models;

import com.google.auto.value.AutoValue;
import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableSet;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import groovyjarjarantlr4.v4.runtime.misc.Nullable;
import org.springframework.lang.NonNull;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@AutoValue
public abstract class ConvertedHymn {

    public abstract String title();
    public abstract String lyricsJson();
    @Nullable public abstract String category();
    @Nullable public abstract String subCategory();
    @Nullable public abstract String author();
    @Nullable public abstract String composer();
    @Nullable public abstract String key();
    @Nullable public abstract String time();
    @Nullable public abstract String meter();
    @Nullable public abstract String scriptures();
    @Nullable public abstract String hymnCode();
    @Nullable public abstract String musicJson();
    @Nullable public abstract String svgJson();
    @Nullable public abstract String pdfJson();
    @Nullable public abstract String languagesJson();
    @Nullable public abstract String relevantJson();

    @NonNull
    public ImmutableSet<Reference> languageReferences() {
        Languages languages = new Gson().fromJson(languagesJson(), Languages.class);
        if (languages == null) {
            return ImmutableSet.of();
        }
        return ImmutableSet.copyOf(languages
                .getData().stream()
                .map(datum ->
                             Reference.create(datum.getValue(), HymnalDbKey.extractFromPath(datum.getPath())))
                .collect(Collectors.toSet()));
    }

    @NonNull
    public ImmutableSet<Reference> relevantReferences() {
        Relevant relevant = new Gson().fromJson(relevantJson(), Relevant.class);
        if (relevant == null) {
            return ImmutableSet.of();
        }
        return ImmutableSet.copyOf(relevant.getData().stream()
                                           .map(datum ->
                                                        Reference.create(datum.getValue(), HymnalDbKey.extractFromPath(datum.getPath())))
                                           .collect(Collectors.toSet()));
    }

    @NonNull
    public ImmutableList<Verse> lyrics() {
        Type listOfVerses = new TypeToken<ArrayList<Verse>>() {}.getType();
        List<Verse> lyrics = new Gson().fromJson(lyricsJson(), listOfVerses);
        if (lyrics == null) {
            throw new IllegalArgumentException("lyricsJson failed to parse: " + lyricsJson());
        }
        return ImmutableList.copyOf(lyrics);
    }

    public abstract Builder toBuilder();

    public static Builder builder() {
        return new AutoValue_ConvertedHymn.Builder();
    }

    @AutoValue.Builder
    public abstract static class Builder {
        public abstract Builder title(String title);
        public abstract Builder lyricsJson(String lyricsJson);

        public Builder lyrics(ImmutableList<Verse> verses) {
            String lyricsJson = new Gson().toJson(verses);
            return lyricsJson(lyricsJson);
        }

        public abstract Builder category(String category);
        public abstract Builder subCategory(String subCategory);
        public abstract Builder author(String author);
        public abstract Builder composer(String composer);
        public abstract Builder key(String key);
        public abstract Builder time(String time);
        public abstract Builder meter(String meter);
        public abstract Builder scriptures(String scriptures);
        public abstract Builder hymnCode(String hymnCode);
        public abstract Builder musicJson(String musicJson);
        public abstract Builder svgJson(String svgJson);
        public abstract Builder pdfJson(String pdfJson);
        public abstract Builder languagesJson(String languagesJson);

        public Builder languageReferences(ImmutableSet<Reference> languageReferences) {
            if (languageReferences.isEmpty()) {
                return languagesJson(null);
            }

            Languages languages = new Languages();
            languages.setName("Languages");
            List<Datum> data = languageReferences.stream().map(reference -> {
                HymnalDbKey key = reference.key;
                String path = "/en/hymn/" + key.hymnType.hymnalDb + "/" + key.hymnNumber + key.queryParams;
                Datum datum = new Datum();
                datum.setValue(reference.text);
                datum.setPath(path);
                return datum;
            }).collect(Collectors.toList());
            languages.setData(data);
            String languagesJson = new Gson().toJson(languages);
            return languagesJson(languagesJson);
        }

        public abstract Builder relevantJson(String relevantJson);
        public abstract ConvertedHymn build();
    }
}
