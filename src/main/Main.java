package main;

import com.google.gson.Gson;
import infra.DatabaseClient;
import main.audit.Auditor;
import models.ConvertedHymn;
import models.HymnalDbKey;
import models.Languages;
import net.sourceforge.pinyin4j.format.exception.BadHanyuPinyinOutputFormatCombination;

import java.io.IOException;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.Map;
import java.util.logging.ConsoleHandler;
import java.util.logging.Level;
import java.util.logging.Logger;

public final class Main {

    /**
     * Perform a dry run without actually writing anything to the database.
     */
    public static final boolean DRY_RUN = true;

    public static final Logger LOGGER = Logger.getAnonymousLogger();

    static {
        ConsoleHandler handler = new ConsoleHandler();
        handler.setLevel(Level.ALL);
        LOGGER.addHandler(handler);
        LOGGER.setLevel(Level.INFO);
    }

    private static final String HYMNAL_DB_NAME = "hymnaldb-v18";
    private static final String RUSSIAN_DB_NAME = "hymns-russian";

    public static void main(String[] args) throws SQLException, BadHanyuPinyinOutputFormatCombination, IOException {
        DatabaseClient hymnalDbClient = new DatabaseClient(HYMNAL_DB_NAME, 18);
        hymnalDbClient.getDb().execSql("PRAGMA user_version = 19");
        HymnalDbHandler hymnalDbHandler = HymnalDbHandler.create(hymnalDbClient);

        DatabaseClient russianDbClient = new DatabaseClient(RUSSIAN_DB_NAME, 23);
        RussianDbHandler russianDbHandler = RussianDbHandler.create(russianDbClient);
        Map<HymnalDbKey, ConvertedHymn> combinedHymns =
                russianDbHandler.combineRussianHymnsWith(hymnalDbHandler.allHymns);

        Auditor.audit(combinedHymns);
        hymnalDbHandler.clearDatabase();
        hymnalDbHandler.writeToDatabase(combinedHymns);

        runTests(hymnalDbClient);
        hymnalDbClient.close();
        russianDbClient.close();
    }

    /**
     * Run through some basic tests to make sure databases have been migrated correctly.
     */
    private static void runTests(DatabaseClient hymnalDbClient) throws SQLException, IOException {
        ResultSet resultSet = hymnalDbClient.getDb().rawQuery(
                "SELECT * FROM song_data WHERE (hymn_type = 'h' AND hymn_number = '43') OR (hymn_type = 'S' AND hymn_number = '28') OR (hymn_type = 'ch' AND hymn_number = '37')");

        if (resultSet == null) {
            throw new IllegalArgumentException("hymns were not found in the database");
        }

        int resultCount = 0;
        while (resultSet.next()) {
            resultCount++;
            ConvertedHymn hymn = ConvertedHymn.builder()
                                              .title(resultSet.getString(5))
                                              .lyricsJson(resultSet.getString(6))
                                              .category(resultSet.getString(7))
                                              .subCategory(resultSet.getString(8))
                                              .author(resultSet.getString(9))
                                              .composer(resultSet.getString(10))
                                              .key(resultSet.getString(11))
                                              .time(resultSet.getString(12))
                                              .meter(resultSet.getString(13))
                                              .scriptures(resultSet.getString(14))
                                              .hymnCode(resultSet.getString(15))
                                              .musicJson(resultSet.getString(16))
                                              .svgJson(resultSet.getString(17))
                                              .pdfJson(resultSet.getString(18))
                                              .languagesJson(resultSet.getString(19))
                                              .relevantJson(resultSet.getString(20))
                                              .build();

            if (!TextUtils.isJsonValid(hymn.lyricsJson())) {
                throw new IllegalArgumentException("invalid json");
            }
            if (!TextUtils.isJsonValid(hymn.musicJson())) {
                throw new IllegalArgumentException("invalid json");
            }
            if (!TextUtils.isJsonValid(hymn.svgJson())) {
                throw new IllegalArgumentException("invalid json");
            }
            if (!TextUtils.isJsonValid(hymn.pdfJson())) {
                throw new IllegalArgumentException("invalid json");
            }
            if (!TextUtils.isJsonValid(hymn.languagesJson())) {
                throw new IllegalArgumentException("invalid json");
            }
            if (!TextUtils.isJsonValid(hymn.relevantJson())) {
                throw new IllegalArgumentException("invalid json");
            }

            Languages languages = new Gson().fromJson(hymn.languagesJson(), Languages.class);
            if (languages.getData().size() != 8) {
                throw new IllegalArgumentException("languages are wrong size");
            }
        }
        if (resultCount != 4) {
            throw new IllegalArgumentException("should have 4 results");
        }
    }
}
