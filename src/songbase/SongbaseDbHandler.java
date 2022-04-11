package songbase;

import com.google.common.collect.ImmutableSet;
import com.google.gson.GsonBuilder;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;
import infra.ContentValues;
import infra.DatabaseClient;
import main.TextUtils;
import songbase.models.AppData;
import songbase.models.Reference;
import songbase.models.Song;

import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.toMap;
import static main.Main.LOGGER;

public class SongbaseDbHandler {

    private final DatabaseClient client;
    private final AppData appData;

    public static SongbaseDbHandler create(DatabaseClient client, String appData) throws SQLException {
        return new SongbaseDbHandler(client, appData);
    }

    public SongbaseDbHandler(DatabaseClient client, String appData) {
        this.client = client;
        this.appData = new GsonBuilder()
                .registerTypeAdapterFactory(GenerateTypeAdapter.FACTORY)
                .create().fromJson(appData, AppData.class);
    }

    public void write() {
        clearTables();
        writeBooks();

        // Populate song-reference map.
        Map<Song, ImmutableSet<Reference>> songReferenceMap = new HashMap<>();
        Map<Integer, Song> songsById =
                appData.songs().stream().collect(toMap(Song::id, (song) -> song));
        appData.references()
               .stream()
               .filter(reference -> {
                   // Incorrect reference. Song #954 actually has nothing to do with Hymns #1193 (https://github.com/ReganRyanNZ/songbase/issues/22)
                   if (reference.id() == 816) {
                       return false;
                   }

                   // Random repeated reference in the json (https://github.com/ReganRyanNZ/songbase/issues/21)
                   if (reference.id() == 1616) {
                       return false;
                   }

                   return true;
               }).forEach(reference -> {
                   ImmutableSet.Builder<Reference> references = ImmutableSet.builder();
                   references.add(reference);

                   int songId = reference.songId();
                   Song song = songsById.get(songId);
                   if (songReferenceMap.containsKey(song)) {
                       references.addAll(songReferenceMap.get(song));
                   }
                   songReferenceMap.put(song, references.build());
               });

        // find unreferenced songs and put them in misc
        int miscNumber = 1;
        List<Song> unreferencedSongs =
                appData.songs()
                       .stream()
                       .filter(song -> !songReferenceMap.containsKey(song)).collect(Collectors.toList());
        LOGGER.finer(String.format("%d unreferenced songs", unreferencedSongs.size()));
        for (Song unreferencedSong : unreferencedSongs) {
            songReferenceMap.put(unreferencedSong,
                                 ImmutableSet.of(Reference.create(new Random().nextInt(), unreferencedSong.id(), 99, miscNumber++)));
        }

        songReferenceMap.forEach((song, references) -> {
            final Reference bestReference;
            if (references.size() == 1) {
                bestReference = references.stream().findFirst().get();
            } else if (references.size() == 2 && references.stream().map(Reference::bookId).collect(Collectors.toSet()).equals(ImmutableSet.of(1, 2))) {
                // If a song refers to both english hymnal and blue songbook, then english hymnal takes precedence
                bestReference = references.stream().filter(reference -> reference.bookId() == 2).findFirst().get();
            } else {
                throw new IllegalStateException("multiple references but didn't reference either english hymnal or blue songbook");
            }

            int bookId = bestReference.bookId();
            int bookIndex = bestReference.index();
            String chords =
                    song.lyrics()
                        // If the first line is a blank newline, then remove it.
                        .replaceAll("\\A\\n", "")
                        // Fix the typos where the chord has a leading space, line [ Am].
                        .replaceAll("\\[\\s+", "[");
            String lyricsWithoutChords =
                    chords
                            // Remove the capo line.
                            .replaceAll("#?\\s?\\(?\\[?([Cc])apo \\d\\)?]?[\\n]*", "")
                            // Remove the chords.
                            .replaceAll("\\[.*?]", "");
            LOGGER.fine(String.format("Inserting: book_id=%d, book_index=%d, title=%s, language=%s, chords=%s, lyricsWithoutChords=%s", bookId, bookIndex, song.title(), song.lang(), chords, lyricsWithoutChords));
            client.getDb().insert("songs",
                                  new ContentValues().put("book_id", bookId)
                                                     .put("book_index", bookIndex)
                                                     .put("title", TextUtils.escapeSingleQuotes(song.title()))
                                                     .put("language", TextUtils.escapeSingleQuotes(song.lang()))
                                                     .put("lyrics", TextUtils.escapeSingleQuotes(lyricsWithoutChords))
                                                     .put("chords", TextUtils.escapeSingleQuotes(chords)));
        });
    }

    private void clearTables() {
        client.getDb().delete("books", null);
        client.getDb().delete("destroyed", null);
        client.getDb().delete("songs", null);
    }

    private void writeBooks() {
        client.getDb().execSql("INSERT INTO books VALUES(1, \"Blue Songbook\", \"english\", \"blue_songbook\")");
        client.getDb().execSql("INSERT INTO books VALUES(2, \"Hymnal\", \"english\", \"english_hymnal\")");
        client.getDb().execSql("INSERT INTO books VALUES(3, \"Himnos\", \"español\", \"spanish_hymnal\")");
        client.getDb().execSql("INSERT INTO books VALUES(4, \"Liederbuch\", \"Deutsch\", \"german_hymnal\")");
        client.getDb().execSql("INSERT INTO books VALUES(99, \"Misc\", \"english\", \"songbase_misc\")");
    }
}
