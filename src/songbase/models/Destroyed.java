package songbase.models;

import com.google.auto.value.AutoValue;
import com.google.gson.TypeAdapter;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

import java.util.List;

/**
 * {
 * "songs": [],
 * "references": [],
 * "books": []
 * }
 */
@AutoValue
@GenerateTypeAdapter
public abstract class Destroyed {

    public abstract List<Integer> songs();
    public abstract List<Integer> references();
    public abstract List<Integer> books();

    public static Destroyed create(List<Integer> songs, List<Integer> references, List<Integer> books) {
        return new AutoValue_Destroyed(songs, references, books);
    }
}
