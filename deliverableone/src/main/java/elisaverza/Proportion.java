package elisaverza;
import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.util.List;


import com.opencsv.exceptions.CsvValidationException;

public class Proportion {
    private static final String CSV_JIRA = "02-ticketdata.csv";
    private static final String CSV_VERSIONS = "03-versionsdata.csv";

    public static Integer pCalc() throws FileNotFoundException, IOException, CsvValidationException{
        Integer p = 0;
        Integer i;
        Integer injIndex = 0;
        Integer openIndex = 0;
        Integer fixIndex = 0;
        String line;
        String injected = " ";
        try(BufferedReader br = new BufferedReader(new FileReader(CSV_JIRA))){
            line = br.readLine();
            while ( (line = br.readLine()) != null ) {
                String[] values = line.split(",");
                String[] affected = values[3].split(" ");
                if(affected.length != 0){
                    injected = affected[0].replace("[", "");
                    injected = injected.replace("]", "");
                }
                List<List<String>> versions = DataRetrieve.csvToList(CSV_VERSIONS);
                for(i=0; i<versions.size(); i++){
                    if(injected.contains(versions.get(i).get(0))){
                        injIndex = i;
                    }
                }
                for(i=0; i<versions.size(); i++){
                    if(values[4].contains(versions.get(i).get(0))){
                        fixIndex = i;
                    }
                }
                for(i=0; i<versions.size(); i++){
                    if(values[6].contains(versions.get(i).get(0))){
                        openIndex = i;
                    }
                }
            }
        }
        return p;
    }

    public static void main(String[] args) throws FileNotFoundException, IOException, CsvValidationException{
        pCalc();
    }

    
}
