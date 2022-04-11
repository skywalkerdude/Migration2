package songbase.models;

import com.google.auto.value.AutoValue;
import com.google.gson.annotations.SerializedName;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

/**
 * {
 * "id": 1,
 * "song_id": 303,
 * "book_id": 1,
 * "index": "1"
 * }
 */
@AutoValue
@GenerateTypeAdapter
public abstract class Reference {

    public abstract int id();
    @SerializedName("song_id") public abstract int songId();
    @SerializedName("book_id") public abstract int bookId();
    public abstract int index();

    public static Reference create(int id, int songId, int bookId, int index) {
        return new AutoValue_Reference(id, songId, bookId, index);
    }
}
