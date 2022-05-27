package elisaverza;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import com.opencsv.exceptions.CsvValidationException;

public class Methrics {

    private static final String CSV_METHRICS = "04-data.csv";
    private static final String CSV_CACHE = "05-commitcache.csv";


    public static void locTouched() throws CsvValidationException, IOException, ParseException{
        List<List<String>> commit = DataRetrieve.csvToList(CSV_CACHE);
        List<List<String>> methrics = DataRetrieve.csvToList(CSV_METHRICS);
    }

    public static void main(String[] args) throws IOException, ParseException, CsvValidationException{
        locTouched();
    }

}
