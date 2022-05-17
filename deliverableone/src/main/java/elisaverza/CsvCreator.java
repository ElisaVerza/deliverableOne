package elisaverza;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
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
    private static final String CSV_METHRICS = "data.csv";

    public static void projectVersions() throws JSONException, IOException, ParseException, InterruptedException{
        String url = "https://issues.apache.org/jira/rest/api/2/project/"+PRJ_NAME+"/";
        Integer i;
        Integer k;
        Integer len;
        JSONObject jsonObj = DataRetrieve.readJsonObjFromUrl(url, true);
        JSONArray json = new JSONArray(jsonObj.getJSONArray("versions"));
        len = json.length();
        String[] versions = new String[len];
        String[] dateStr = new String[len];
        String[] commitSha = new String[len];
        try(FileWriter csvWriter = new FileWriter(CSV_METHRICS)){
            for(i = 0; i<len; i++){
                if(json.getJSONObject(i).getBoolean("released")){
                    versions[i] = json.getJSONObject(i).getString("name");
                    dateStr[i] = json.getJSONObject(i).getString("releaseDate");
                    commitSha[i] = dateSearch(dateStr[i]);
                    String filesUrl = "https://api.github.com/repos/apache/"+PRJ_NAME+"/git/trees/"+commitSha[i]+"?recursive=1";
                    JSONObject filesJsonObj = DataRetrieve.readJsonObjFromUrl(filesUrl, true);
                    JSONArray jsonFiles = new JSONArray(filesJsonObj.getJSONArray("tree"));

                    for(k = 0; k<jsonFiles.length(); k++){
                        if(jsonFiles.getJSONObject(k).getString("path").contains(".java")){
                            csvWriter.append(versions[i]+","+jsonFiles.getJSONObject(k).getString("path")+"\n");
                        }
                    }

                }
                Thread.sleep(1000);
            }
        }

    }

    public static void bugginess(){
        
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
