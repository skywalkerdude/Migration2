package songbase;

import infra.ContentValues;
import infra.DatabaseClient;
import infra.SQLiteDatabase;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.runners.MockitoJUnitRunner;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Scanner;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
// Note: need to add "--add-opens java.base/java.lang=ALL-UNNAMED" VM params to run
// https://cnpubf.com/java-lang-reflect-inaccessibleobjectexception-unable-to-make-protected-final-java-lang-class/
public class SongbaseDbHandlerTest {

    @Mock DatabaseClient client;
    @Mock SQLiteDatabase database;

    private SongbaseDbHandler target;

    @Before
    public void setUp() throws FileNotFoundException, SQLException {
        File myObj = new File("raw/test_app_data.json");
        Scanner reader = new Scanner(myObj);
        StringBuilder data = new StringBuilder();
        while (reader.hasNextLine()) {
            data.append(reader.nextLine());
        }
        reader.close();

        when(client.getDb()).thenReturn(database);
        target = SongbaseDbHandler.create(client, data.toString());
    }

    @Test
    public void write() {
        target.write();
        verify(database).insert("songs",
                                new ContentValues().put("book_id", 1)
                                                   .put("book_index", 1)
                                                   .put("title", "Song'' 1")
                                                   .put("language", "english")
                                                   .put("lyrics", "  Song Number one ''chords. fin.")
                                                   .put("chords", "#(Capo 3)\n\n  [D]Song [D7]Number on[G]e ''chords. [D]fin."));
        verify(database).insert("songs",
                                new ContentValues().put("book_id", 2)
                                                   .put("book_index", 213)
                                                   .put("title", "Song 2")
                                                   .put("language", "english")
                                                   .put("lyrics", "Song Number two chords. fin.")
                                                   .put("chords", "# [capo 3]\n\n\n\n[D]Song [D7]Number tw[G]o chords. []fin."));
        verify(database).insert("songs",
                                new ContentValues().put("book_id", 99)
                                                   .put("book_index", 1)
                                                   .put("title", "Dangling song")
                                                   .put("language", "english")
                                                   .put("lyrics", "\n\n\nDangling Song\n. fin.")
                                                   .put("chords", "\n\n\n[D]Dangling Song\n. fin[A]."));
    }
}
