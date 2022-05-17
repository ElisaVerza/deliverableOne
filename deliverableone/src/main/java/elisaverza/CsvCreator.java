package elisaverza;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class CsvCreator {
    public static final String PRJ_NAME = "SYNCOPE";
    private static final String CSV_COMMIT = "commitdata.csv";
    private static final String CSV_METHRICS = "data.csv";
    private static final boolean DOWNLOAD_FILES = true;


    public static String[] projectVersions() throws JSONException, IOException, ParseException, InterruptedException{
        String url = "https://issues.apache.org/jira/rest/api/2/project/"+PRJ_NAME+"/";
        Integer i;
        Integer k;
        Integer len;
        JSONObject jsonObj = DataRetrieve.readJsonObjFromUrl(url, false);
        JSONArray json = new JSONArray(jsonObj.getJSONArray("versions"));
        len = json.length();
        String[] versions = new String[len/2+1];
        String[] dateStr = new String[len/2+1];
        String[] commitSha = new String[len/2+1];

        try(FileWriter csvWriter = new FileWriter(CSV_METHRICS, DOWNLOAD_FILES)){
            for(i = 0; i<len; i++){
                System.out.println(i);
                if(json.getJSONObject(i).getBoolean("released")){
                    versions[i] = json.getJSONObject(i).getString("name");
                    if(DOWNLOAD_FILES){
                        dateStr[i] = json.getJSONObject(i).getString("releaseDate");
                        commitSha[i] = dateSearch(dateStr[i]);
                        String filesUrl = "https://api.github.com/repos/apache/"+PRJ_NAME+"/git/trees/"+commitSha[i]+"?recursive=1";
                        JSONObject filesJsonObj = DataRetrieve.readJsonObjFromUrl(filesUrl, true);
                        JSONArray jsonFiles = new JSONArray(filesJsonObj.getJSONArray("tree"));

                        for(k = 0; k<jsonFiles.length(); k++){
                            if(jsonFiles.getJSONObject(k).getString("path").contains(".java")){
                                csvWriter.append(versions[i]+","+jsonFiles.getJSONObject(k).getString("path")+","+"No\n");
                            }
                        }
                    }
                }
                if(i == len/2){
                    return versions;
                }
                Thread.sleep(1000);
            }
        }
        return versions;
    }

    public static void bugginess() throws IOException, JSONException, ParseException, InterruptedException, CsvException{
        Integer i;
        Integer k;
        String[] versions = projectVersions();
        for(i = 0; i<versions.length; i++){
            Integer j = 0;
            try(BufferedReader br = new BufferedReader(new FileReader("ticketdata.csv"))){
                String line = br.readLine();
                while ( (line = br.readLine()) != null ) {
                    String[] values = line.split(",");
                    if(values.length > 3 && versions[i]!=null) {
                        String sha = values[1];
                        String classesUrl = "https://api.github.com/repos/apache/"+PRJ_NAME+"/commits/"+sha;
                        JSONObject classesJsonObj = DataRetrieve.readJsonObjFromUrl(classesUrl, true);
                        JSONArray jsonClasses = new JSONArray(classesJsonObj.getJSONArray("files"));
                        for(k=0; k<jsonClasses.length();k++){
                            System.out.println(versions[i]+" "+k);
                            if(jsonClasses.getJSONObject(k).getString("filename").contains(".java")){
                                updateDataCSV(CSV_METHRICS, "Yes", j, 2);
                            }
                        }
                    }
                    j++;
                }
            }
        }
    }

        /**
     * Update CSV by row and column
     * 
     * @param fileToUpdate CSV file path to update e.g. D:\\chetan\\test.csv
     * @param replace Replacement for your cell value
     * @param row Row for which need to update 
     * @param col Column for which you need to update
     * @throws IOException
         * @throws CsvException
     */
    public static void updateDataCSV(String fileToUpdate, String replace, int row, int col) throws IOException, CsvException {
        List<String[]> csvBody = new ArrayList<>();
        File inputFile = new File(fileToUpdate);

        // Read existing file 
        try(CSVReader reader = new CSVReader(new FileReader(inputFile))){
            csvBody = reader.readAll();
            // get CSV row column  and replace with by using row and column
            csvBody.get(row)[col] = replace;
        }
            // Write to CSV file which is open
        try(CSVWriter writer = new CSVWriter(new FileWriter(inputFile))){
            writer.writeAll(csvBody);
            writer.flush();
        }
    }

    public static String dateSearch(String dateStr) throws ParseException, IOException{
        Date releaseDate = new SimpleDateFormat("yyyy-MM-dd").parse(dateStr);
        File file = new File(CSV_COMMIT);
        Integer i;
        String lastCommitSha = " ";
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            line = br.readLine();
            while ( (line = br.readLine()) != null ) {
                String[] values = line.split(",");
                Date commitDate = Date.from(Instant.parse(values[0]));
                i = releaseDate.compareTo(commitDate);
                if(i>0){
                    lastCommitSha = values[1];
                    break;
                }
            }          
        }
        return lastCommitSha;
    }


    public static void main(String[] args) throws IOException, InterruptedException, JSONException, ParseException, CsvException{
        DataRetrieve.fileHandler();
        bugginess();
    }
}
