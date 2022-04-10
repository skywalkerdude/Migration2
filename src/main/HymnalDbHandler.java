package main;

import infra.ContentValues;
import infra.DatabaseClient;
import models.ConvertedHymn;
import models.HymnType;
import models.HymnalDbKey;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Populates, audits, and makes a dense graph of all the songs in the hymnal db
 */
public class HymnalDbHandler {

    public final DatabaseClient client;
    public final Map<HymnalDbKey, ConvertedHymn> allHymns;

    public static HymnalDbHandler create(DatabaseClient client) throws SQLException {
        return new HymnalDbHandler(client);
    }

    public HymnalDbHandler(DatabaseClient client) throws SQLException {
        this.client = client;
        this.allHymns = populateHymns();
    }

    private Map<HymnalDbKey, ConvertedHymn> populateHymns() throws SQLException {
        Map<HymnalDbKey, ConvertedHymn> allHymns = new LinkedHashMap<>();
        ResultSet resultSet = client.getDb().rawQuery("SELECT * FROM song_data");
        if (resultSet == null) {
            throw new IllegalArgumentException("hymnalDb query returned null");
        }
        while (resultSet.next()) {
            HymnType hymnType = HymnType.fromHymnalDb(resultSet.getString(2));
            String hymnNumber = resultSet.getString(3);
            String queryParams = resultSet.getString(4);
            allHymns.put(new HymnalDbKey(hymnType, hymnNumber, queryParams),
                         ConvertedHymn.builder()
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
                                      .build());
        }
        return allHymns;
    }

    public void clearDatabase() {
        client.getDb().delete("song_data", null);
    }

    public void writeToDatabase(Map<HymnalDbKey, ConvertedHymn> songs) {
        songs.forEach((key, hymn) -> client.getDb().insert("song_data", createContentValue(key, hymn)));
    }

    private ContentValues createContentValue(HymnalDbKey key, ConvertedHymn hymn) {
        ContentValues contentValues = new ContentValues();
        contentValues.put("HYMN_TYPE", key.hymnType.hymnalDb);
        contentValues.put("HYMN_NUMBER", key.hymnNumber);
        contentValues.put("QUERY_PARAMS", key.queryParams);
        contentValues.put("SONG_TITLE", TextUtils.escapeSingleQuotes(hymn.title()));
        contentValues.put("SONG_LYRICS", TextUtils.escapeSingleQuotes(hymn.lyricsJson()));
        contentValues.put("SONG_META_DATA_CATEGORY", TextUtils.escapeSingleQuotes(hymn.category()));
        contentValues.put("SONG_META_DATA_SUBCATEGORY", TextUtils.escapeSingleQuotes(hymn.subCategory()));
        contentValues.put("SONG_META_DATA_AUTHOR", TextUtils.escapeSingleQuotes(hymn.author()));
        contentValues.put("SONG_META_DATA_COMPOSER", TextUtils.escapeSingleQuotes(hymn.composer()));
        contentValues.put("SONG_META_DATA_KEY", TextUtils.escapeSingleQuotes(hymn.key()));
        contentValues.put("SONG_META_DATA_TIME", TextUtils.escapeSingleQuotes(hymn.time()));
        contentValues.put("SONG_META_DATA_METER", TextUtils.escapeSingleQuotes(hymn.meter()));
        contentValues.put("SONG_META_DATA_SCRIPTURES", TextUtils.escapeSingleQuotes(hymn.scriptures()));
        contentValues.put("SONG_META_DATA_HYMN_CODE", TextUtils.escapeSingleQuotes(hymn.hymnCode()));
        contentValues.put("SONG_META_DATA_MUSIC", TextUtils.escapeSingleQuotes(hymn.musicJson()));
        contentValues.put("SONG_META_DATA_SVG_SHEET_MUSIC", TextUtils.escapeSingleQuotes(hymn.svgJson()));
        contentValues.put("SONG_META_DATA_PDF_SHEET_MUSIC", TextUtils.escapeSingleQuotes(hymn.pdfJson()));
        contentValues.put("SONG_META_DATA_LANGUAGES", TextUtils.escapeSingleQuotes(hymn.languagesJson()));
        contentValues.put("SONG_META_DATA_Relevant", TextUtils.escapeSingleQuotes(hymn.relevantJson()));
        return contentValues;
    }
}
