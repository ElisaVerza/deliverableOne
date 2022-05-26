package elisaverza;

import java.io.IOException;
import java.text.ParseException;
import java.util.Arrays;
import java.util.List;

import com.opencsv.exceptions.CsvValidationException;

public class Methrics {

    private static final String CSV_JIRA = "02-ticketdata.csv";
    private static final String CSV_VERSIONS = "03-versionsdata.csv";
    private static final String CSV_METHRICS = "04-data.csv";


    public static void locTouched() throws CsvValidationException, IOException, ParseException{
        List<List<String>> versions = DataRetrieve.csvToList(CSV_VERSIONS);
        List<List<String>> files = DataRetrieve.csvToList(CSV_METHRICS);
        Integer i = 1;
    
        while(i<versions.size()){
            String[] sha = getCommitByVersion(versions.get(i).get(0));
            System.out.println("VERSIONE: "+versions.get(i).get(0)+" "+sha.length);
            i++;
        }


    }

    public static String[] getCommitByVersion(String versions) throws CsvValidationException, IOException, ParseException{
        Integer j = 1;
        Integer k = 0;
        List<List<String>> commits = DataRetrieve.csvToList(CSV_JIRA);
        String[] sha = new String[0];
        for(j = 1; j<commits.size(); j++){
            if(versions.equals(DataRetrieve.getVersionByDate(commits.get(j).get(0)))){
                String[] newArray = new String[sha.length + 1];
                System.arraycopy(sha, 0, newArray, 0, sha.length);
                sha = newArray;
                sha[k] = commits.get(j).get(1);
                k++;
            }
        } 
        return sha; 
    }
    
    public static void main(String[] args) throws IOException, ParseException, CsvValidationException{
        locTouched();
    }

}
