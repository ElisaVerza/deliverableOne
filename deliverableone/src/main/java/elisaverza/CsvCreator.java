package elisaverza;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.Date;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class CsvCreator {
    public static final String PRJ_NAME = "SYNCOPE";
    private static final String CSV_COMMIT = "commitdata.csv";

    public static void projectVersions() throws JSONException, IOException, ParseException{
        String url = "https://issues.apache.org/jira/rest/api/2/project/"+PRJ_NAME+"/";
        Integer i;
        Integer len;
        JSONObject jsonObj = DataRetrieve.readJsonObjFromUrl(url, false);
        JSONArray json = new JSONArray(jsonObj.getJSONArray("versions"));
        len = json.length();
        String[] versions = new String[len];
        String[] dateStr = new String[len];
        for(i = 0; i<len; i++){
            if(json.getJSONObject(i).getBoolean("released")){
                versions[i] = json.getJSONObject(i).getString("name");
                dateStr[i] = json.getJSONObject(i).getString("releaseDate");
                dateSearch(dateStr[i]);
            }
        }
    }

    public static String dateSearch(String dateStr) throws ParseException, IOException{
        Date releaseDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
        File file = new File(CSV_COMMIT);
        Integer i;
        Date lastCommit = new Date();
        String lastCommitSha = " ";
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            line = br.readLine();
            while ( (line = br.readLine()) != null ) {
                String[] values = line.split(",");
                Date commitDate = Date.from(Instant.parse(values[0]));
                i = releaseDate.compareTo(commitDate);
                if(i>0){
                    lastCommit = commitDate;
                    lastCommitSha = values[1];
                    break;
                }
            }
            System.out.println(lastCommit.toString()+" "+releaseDate.toString()+" "+lastCommitSha);
          
        }
        return lastCommitSha;
    }


    public static void main(String[] args) throws IOException, InterruptedException, JSONException, ParseException{
        DataRetrieve.fileHandler();
        projectVersions();
    }
}
