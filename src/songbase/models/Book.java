package songbase.models;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

/**
 * {
 * "id": 1,
 * "name": "Blue Songbook",
 * "lang": "english",
 * "slug": "blue_songbook"
 * }
 */
@AutoValue
@GenerateTypeAdapter
public abstract class Book {

    public abstract int id();
    public abstract String name();
    public abstract String lang();
    public abstract String slug();

    public static Book create(int id, String name, String lang, String slug) {
        return new AutoValue_Book(id, name, lang, slug);
    }
}
