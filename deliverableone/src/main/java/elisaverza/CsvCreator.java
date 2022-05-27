package elisaverza;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Date;
import java.util.List;
import java.util.logging.Logger;

import com.opencsv.CSVReader;
import com.opencsv.CSVWriter;
import com.opencsv.exceptions.CsvException;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;


public class CsvCreator {
    private static final Logger LOGGER = Logger.getLogger(CsvCreator.class.getName());
    public static final String PRJ_NAME = "SYNCOPE";
    private static final String CSV_COMMIT = "01-commitdata.csv";
    private static final String CSV_JIRA = "02-ticketdata.csv";
    private static final String CSV_VERSIONS = "03-versionsdata.csv";
    private static final String CSV_METHRICS = "04-data.csv";
    private static final String CSV_CACHE = "05-commitcache.csv";
    private static final boolean DOWNLOAD_DATA = true;


    public static void downloadFiles() throws IOException, ParseException{
        LOGGER.warning("Download file per release in corso...");

        Integer k;
        Integer i = 0;

        try(BufferedReader brVd = new BufferedReader(new FileReader(CSV_VERSIONS))){
            String lineVd = brVd.readLine();
            String [] commitSha = new String[0];
            try(FileWriter csvWriter = new FileWriter(CSV_METHRICS)){
                csvWriter.append("versione,file,LOC Touched, metrica2, metrica3, metrica4, metrica5, metrica6, metrica7, metrica8, metrica9, bugginess\n");

                while((lineVd = brVd.readLine()) != null ) {
                    String[] valuesVd = lineVd.split(",");
                    String[] newArray = new String[commitSha.length + 1];
                    System.arraycopy(commitSha, 0, newArray, 0, commitSha.length);
                    commitSha = newArray;

                    commitSha[i] = dateSearch(valuesVd[1]);
                    LOGGER.warning(valuesVd[0]);

                    if(!commitSha[i].equals(" ")){
                        String filesUrl = "https://api.github.com/repos/apache/"+PRJ_NAME+"/git/trees/"+commitSha[i]+"?recursive=1";
                        JSONObject filesJsonObj = DataRetrieve.readJsonObjFromUrl(filesUrl, true);
                        JSONArray jsonFiles = new JSONArray(filesJsonObj.getJSONArray("tree"));

                        for(k = 0; k<jsonFiles.length(); k++){
                            if(jsonFiles.getJSONObject(k).getString("path").contains(".java")){
                                csvWriter.append(valuesVd[0]+","+jsonFiles.getJSONObject(k).getString("path")+","+0+","+0+","+0+","+0+","+0+","+0+","+0+","+0+","+0+","+"No\n");
                            }
                        }
                    }
                }
                i++;
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
    public static void fileTouched(String line, Integer rowIndex, String version, boolean overwrite) throws IOException, CsvException{
        LOGGER.warning("Ricerca file toccati dai commit in corso...");
        LOGGER.warning(version);        
        Integer i = 0;
        Integer k;
        String[] values = line.split(",");

        if(values.length > 3 && values[3].contains(version)) {
            String sha = values[1];
            String classesUrl = "https://api.github.com/repos/apache/"+PRJ_NAME+"/commits/"+sha.replace("\"", "");
            JSONObject classesJsonObj = DataRetrieve.readJsonObjFromUrl(classesUrl, true);
            JSONArray jsonClasses = new JSONArray(classesJsonObj.getJSONArray("files"));
            String[] files = new String[0];
            Integer[] added = new Integer[0];
            Integer[] deleted = new Integer[0];
            try(FileWriter csvWriter = new FileWriter(CSV_CACHE, overwrite)){
                for(k=0; k<jsonClasses.length();k++){
                    if(jsonClasses.getJSONObject(k).getString("filename").contains(".java")){
                        String[] newArrayFile = new String[files.length + 1];
                        System.arraycopy(files, 0, newArrayFile, 0, files.length);
                        files = newArrayFile;
                        files[i] = jsonClasses.getJSONObject(k).getString("filename");

                        Integer[] newArrayAdd = new Integer[added.length + 1];
                        System.arraycopy(added, 0, newArrayAdd, 0, added.length);
                        added = newArrayAdd;    
                        added[i] = jsonClasses.getJSONObject(k).getInt("additions");

                        Integer[] newArrayDel = new Integer[deleted.length + 1];
                        System.arraycopy(deleted, 0, newArrayDel, 0, deleted.length);
                        deleted = newArrayDel;    
                        deleted[i] = jsonClasses.getJSONObject(k).getInt("deletions");

                        updateDataCSV(CSV_METHRICS, "Yes", rowIndex, 11);
                        i++;
                    }
                }
                if(files.length!=0){
                    csvWriter.append(version+","+sha+","+Arrays.toString(files).replace(",", " ")+","+Arrays.toString(added).replace(",", " ")+","+Arrays.toString(deleted).replace(",", " ")+"\n");
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
     * @throws InterruptedException
     */
    public static void bugginess() throws IOException, JSONException, ParseException, CsvException, InterruptedException{
        downloadFiles();
        boolean overwrite = false;

        try(BufferedReader brVd = new BufferedReader(new FileReader(CSV_VERSIONS));){
            String lineVd = brVd.readLine();
            while( (lineVd = brVd.readLine()) != null ){
                String[] valuesVd = lineVd.split(",");
                String version = valuesVd[0];
                Integer j = 1;
                try(BufferedReader brTd = new BufferedReader(new FileReader(CSV_JIRA))){
                    String lineTd = brTd.readLine();
                    while ( (lineTd = brTd.readLine()) != null ) {
                        if(version!=null){

                            if(!overwrite){
                                fileTouched(lineTd, j, version, false);
                            }
                            else{
                                fileTouched(lineTd, j, version, true);
                            }
                            overwrite = true;
                        }
                        j++;
                    }
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
                Date commitDate = new SimpleDateFormat("yyyy-MM-dd").parse(values[0].substring(0, 10));
                i = releaseDate.compareTo(commitDate);
                if(i>=0){
                    lastCommitSha = values[1];
                    break;
                }
            }          
        }
        return lastCommitSha;
    }


    public static void main(String[] args) throws IOException, InterruptedException, JSONException, ParseException, CsvException{
        
        DataRetrieve.fileHandler();
        if(DOWNLOAD_DATA){
            bugginess();
        }
    }
}
