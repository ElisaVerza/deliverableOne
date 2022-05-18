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


    /**
     * 
     * @return
     */
    public static String[] projectVersions() throws JSONException, IOException, ParseException, InterruptedException{
        String url = "https://issues.apache.org/jira/rest/api/2/project/"+PRJ_NAME+"/";
        Integer i;
        Integer len;
        JSONObject jsonObj = DataRetrieve.readJsonObjFromUrl(url, false);
        JSONArray json = new JSONArray(jsonObj.getJSONArray("versions"));
        len = json.length();
        String[] versions = new String[len/2+1];

            for(i = 0; i<len; i++){
                if(json.getJSONObject(i).getBoolean("released")){
                    versions[i] = json.getJSONObject(i).getString("name");
                    if(DOWNLOAD_FILES){
                        downloadFiles(i, json, versions);
                    }
                }
                if(i == len/2){
                    return versions;
                }
                Thread.sleep(1000);
            }
        
        return versions;
    }

    public static void downloadFiles(int i, JSONArray json, String[] versions) throws IOException, ParseException{
        Integer k;
        Integer len = json.length();
        String[] dateStr = new String[len/2+1];
        String[] commitSha = new String[len/2+1];
        try(FileWriter csvWriter = new FileWriter(CSV_METHRICS)){
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

    /**
     * Metodo che individua tutti i file buggy di un determinato commit se la versione indicata
     * dal parametro versions è presente nella lista delle affected versions. Prende in input una
     * riga del csv contenente le informazioni dei commit e dei ticket (ticketdata) e se nella colonna
     * affected versions è presente la versione di interesse tramite lo sha del commit ottiene tutti i
     * file toccati da quest'ultimo, li cerca nel csv data che contiene tutte le classi presenti nelle 
     * singole release e, tramite il metodo updateDataCSV imposta la colonna buggy a Yes.
     * 
     * @param line riga del file ticketdata in cui cercare se presente version nella colonna AV
     * @param rowIndex indice di riga da modificare per impostare buggy a yes
     * @param version la versione di interesse da cercare nella colonna AV del csv ticketdata
     * @return void
     */
    public static void fileTouched(String line, Integer rowIndex, String version) throws IOException, CsvException{
        Integer k;

        String[] values = line.split(",");
        if(values.length > 3 && values[3].contains(version)) {
            String sha = values[1];
            String classesUrl = "https://api.github.com/repos/apache/"+PRJ_NAME+"/commits/"+sha;
            JSONObject classesJsonObj = DataRetrieve.readJsonObjFromUrl(classesUrl, true);
            JSONArray jsonClasses = new JSONArray(classesJsonObj.getJSONArray("files"));
            for(k=0; k<jsonClasses.length();k++){
                if(jsonClasses.getJSONObject(k).getString("filename").contains(".java")){
                    updateDataCSV(CSV_METHRICS, "Yes", rowIndex, 2);
                }
            }
        }

    }

    /**
     * Metodo che ottiene una lista delle versioni del progetto tramite il metodo projectVersions()
     * per ogni versione legge il file ticketdata contenente tutti i commit con le informazioni dei
     * ticket. Per ogni riga chiama il metodo fileTouched() che controlla se la versione corrente
     * rientra nella lista delle affected versions (se presenti), prende lo sha del commit e segna
     * come buggy tutti i file toccati dal commit risolutivo del ticket.
     * 
     * @return void
     */
    public static void bugginess() throws IOException, JSONException, ParseException, InterruptedException, CsvException{
        Integer i;
        String[] versions = projectVersions();
        for(i = 0; i<versions.length; i++){
            Integer j = 0;
            try(BufferedReader br = new BufferedReader(new FileReader("ticketdata.csv"))){
                String line = br.readLine();
                while ( (line = br.readLine()) != null ) {
                    if(versions[i]!=null){
                        fileTouched(line, j, versions[i]);
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
