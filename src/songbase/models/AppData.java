package songbase.models;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

import java.util.List;

@AutoValue
@GenerateTypeAdapter
public abstract class AppData {

    public abstract List<Song> songs();
    public abstract List<Book> books();
    public abstract List<Reference> references();
    public abstract Destroyed destroyed();
    public abstract int songCount();

    public static AppData create(List<Song> songs, List<Book> books, List<Reference> references,
                                 Destroyed destroyed, int songCount) {
        return new AutoValue_AppData(songs, books, references, destroyed, songCount);
    }
}
