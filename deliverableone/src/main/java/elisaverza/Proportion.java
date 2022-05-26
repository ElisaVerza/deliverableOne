package elisaverza;
import java.io.IOException;
import java.util.List;


import com.opencsv.exceptions.CsvValidationException;


public class Proportion {
    private Proportion() {
        throw new IllegalStateException("Proportion class");
    }
    private static final String CSV_JIRA = "02-ticketdata.csv";
    private static final String CSV_VERSIONS = "03-versionsdata.csv";

    public static Integer indexCalc(List<List<String>> versions, String verToFind){
        Integer i;
        Integer index = 0;
        for(i=0; i<versions.size(); i++){
            if(verToFind.contains(versions.get(i).get(0))){
                index = i;
            }
        }
        return index;
    }

    public static Float pCalc() throws IOException, CsvValidationException{
        Integer j = 0;
        Integer i;
        Integer injIndex = 0;
        Integer openIndex = 0;
        Integer fixIndex = 0;
        Float[] propArray = new Float[0];
        String injected = " ";

        List<List<String>> csv = DataRetrieve.csvToList(CSV_JIRA);

        for(i = 1; i<csv.size(); i++) {
            String[] affected = csv.get(i).get(3).split(" ");
            if(affected.length != 0){
                injected = affected[0];
            }
            List<List<String>> versions = DataRetrieve.csvToList(CSV_VERSIONS);

            injIndex = indexCalc(versions, injected);
            fixIndex = indexCalc(versions,  csv.get(i).get(4));
            openIndex = indexCalc(versions,  csv.get(i).get(5));

            Integer num = fixIndex-injIndex;
            Integer denum = fixIndex-openIndex;
            if(denum == 0){
                denum = 1;
            }
            Float[] newArray = new Float[propArray.length + 1];
            System.arraycopy(propArray, 0, newArray, 0, propArray.length);
            propArray = newArray;
            propArray[j] = Float.valueOf(num)/Float.valueOf(denum);
            j++;
        }
        return proportionAvg(propArray);
    }

    public static Float proportionAvg(Float[] propArray){
        Integer i = 0;
        Float sum = 0f;
        while (i < propArray.length) {
            sum += propArray[i];
            i++;
        }
        return sum/propArray.length;

        
    }

    public static String ivCalc(String fixed, String ov, Float p) throws CsvValidationException, IOException{
        List<List<String>> csv = DataRetrieve.csvToList(CSV_VERSIONS);
        String[] ovArray = {ov};
        String[] fvArray = {fixed};
        Integer ovIndex = (Integer) DataRetrieve.minVersion(ovArray).getValue(0);
        Integer fvIndex = (Integer) DataRetrieve.minVersion(fvArray).getValue(0);
        Integer ivIndex = Math.round(fvIndex-(fvIndex-ovIndex)*p);

        if(ivIndex>csv.size()){
            ivIndex = csv.size()-1;
        }
        return csv.get(ivIndex).get(0)+"mod";
    }
    
}
