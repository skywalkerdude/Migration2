package models;

import main.TextUtils;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class HymnalDbKey {

    private static final Pattern PATH_PATTERN = Pattern.compile("(\\w+)/(c?\\d+[a-z]*)(\\?gb=1)?");

    public final HymnType hymnType;
    public final String hymnNumber;
    public final String queryParams;

    public HymnalDbKey(HymnType hymnType, String hymnNumber, String queryParams) {
        assert hymnType != null;
        assert !TextUtils.isEmpty(hymnNumber);

        this.hymnType = hymnType;
        this.hymnNumber = hymnNumber;
        this.queryParams = queryParams != null ? queryParams : "";
    }

    @Override
    public int hashCode() {
        return hymnType.hashCode() + hymnNumber.hashCode() + queryParams.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj instanceof HymnalDbKey) {
            HymnalDbKey key = (HymnalDbKey) obj;
            return hymnType == key.hymnType && hymnNumber.equals(key.hymnNumber) && queryParams.equals(key.queryParams);
        }
        return false;
    }

    @Override
    public String toString() {
        return hymnType.hymnalDb + "/" + hymnNumber + "/" + queryParams;
    }

    public static HymnalDbKey extractFromPath(String path) {
        HymnType hymnType = extractTypeFromPath(path);
        String hymnNumber = extractNumberFromPath(path);
        String queryParam = extractQueryParamFromPath(path);
        return new HymnalDbKey(hymnType, hymnNumber, queryParam);
    }

    public static HymnType extractTypeFromPath(String path) {
        Matcher matcher = PATH_PATTERN.matcher(path);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unable to extract hymn type from " + path);
        }

        return HymnType.fromHymnalDb(matcher.group(1));
    }

    public static String extractNumberFromPath(String path) {
        Matcher matcher = PATH_PATTERN.matcher(path);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unable to extract hymn number from " + path);
        }

        return matcher.group(2);
    }

    public static String extractQueryParamFromPath(String path) {
        Matcher matcher = PATH_PATTERN.matcher(path);
        if (!matcher.find()) {
            throw new IllegalArgumentException("Unable to extract query param from " + path);
        }

        return matcher.group(3);
    }
}
