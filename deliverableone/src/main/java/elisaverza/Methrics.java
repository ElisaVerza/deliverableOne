package elisaverza;

import java.io.IOException;
import java.text.ParseException;
import java.util.List;

import com.opencsv.exceptions.CsvException;

public class Methrics {

    private static final String CSV_METHRICS = "04-data.csv";
    private static final String CSV_CACHE = "05-commitcache.csv";


    public static void locTouched() throws IOException, ParseException, CsvException{
        Integer i;
        Integer j;
        Integer k;
        Integer currLocT = 0;
        List<List<String>> commit = DataRetrieve.csvToList(CSV_CACHE);
        List<List<String>> methrics = DataRetrieve.csvToList(CSV_METHRICS);
        System.out.println(commit.size());
        for(i=0; i<commit.size(); i++){
            System.out.println(i);
            for(j=0; j<methrics.size(); j++){
                if(commit.get(i).get(0).contains(methrics.get(j).get(0))){
                    String filesList = commit.get(i).get(2).replace("[", "");
                    filesList = filesList.replace("]", "");
                    String[] files = filesList.split(" ");

                    String addedList = commit.get(i).get(3).replace("[", "");
                    addedList = addedList.replace("]", "");
                    String[] added = addedList.split(" ");

                    String deletedList = commit.get(i).get(4).replace("[", "");
                    deletedList = deletedList.replace("]", "");
                    String[] deleted = deletedList.split(" ");

                    for(k=0; k<files.length; k++){
                        if(files[k].contains(methrics.get(j).get(1))){
                            currLocT = Integer.valueOf(methrics.get(j).get(2));
                            currLocT = currLocT+Integer.valueOf(added[k])+Integer.valueOf(deleted[k]);
                            CsvCreator.updateDataCSV(CSV_METHRICS, currLocT.toString(), j, 2);

                        }
                    }

                    //
                    //String
                }
            }
        }
    }

    public static void main(String[] args) throws IOException, ParseException, CsvException{
        locTouched();
    }

}
