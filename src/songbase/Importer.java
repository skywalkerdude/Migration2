package songbase;

import infra.DatabaseClient;

import java.io.File;
import java.io.FileNotFoundException;
import java.sql.SQLException;
import java.util.Scanner;

public class Importer {

    private static final String SONGBASE_DB_NAME = "songbase-v3";

    public static void main(String[] args) throws SQLException, FileNotFoundException {
        File myObj = new File("raw/app_data.json");
        Scanner reader = new Scanner(myObj);
        StringBuilder data = new StringBuilder();
        while (reader.hasNextLine()) {
            data.append(reader.nextLine());
        }
        reader.close();

        DatabaseClient songbaseClient = new DatabaseClient(SONGBASE_DB_NAME, 3);
        SongbaseDbHandler handler = SongbaseDbHandler.create(songbaseClient, data.toString());
        handler.write();
        songbaseClient.close();
    }
}
