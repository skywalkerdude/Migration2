package songbase.models;

import com.google.auto.value.AutoValue;
import com.ryanharter.auto.value.gson.GenerateTypeAdapter;

/**
 * {
 * "id": 965,
 * "title": "Glory, glory, glory, praise and adoration",
 * "lang": "english",
 * "lyrics": "1\n[G]Glory, glory, glo[D7]ry, [G]praise and adorati[D7]on!\n[C]Hear the anthems swelling [C]out thro' all [D]ete[A]rn[D7]ity!\n[G]Father, Son, and [D7]Spirit-[G]God in revelation-\n[G]Prostrate each [C]soul before the [G]De[D]it[G]y! \n\n2\nFather, source of glory, naming every fam'ly;\nAnd the Son upholding all by His almighty power; \nHoly Spirit, filling the vast scene of glory-\nO glorious Fulness, let our hearts adore!\n\n3\nGod supreme, we worship now in holy splendour, \nHead of the vast scene of bliss, before Thy face we fall!\nMajesty and greatness, glory, praise and power\nTo Thee belong, eternal Source of all!\n"
 * }
 */
@AutoValue
@GenerateTypeAdapter
public abstract class Song {

    public abstract int id();
    public abstract String title();
    public abstract String lang();
    public abstract String lyrics();

    public static Song create(int id, String title, String lang, String lyrics) {
        return new AutoValue_Song(id, title, lang, lyrics);
    }
}
