package elisaverza;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Base64;
import java.util.Date;
import java.util.List;

import com.opencsv.CSVReader;
import com.opencsv.exceptions.CsvValidationException;

import org.javatuples.Pair;
import org.javatuples.Tuple;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DataRetrieve 
{
    private static final String CSV_COMMIT = "01-commitdata.csv";
    private static final String CSV_JIRA = "02-ticketdata.csv";
    private static final String CSV_VERSIONS = "03-versionsdata.csv";
    private static final String PRJ_NAME = "SYNCOPE";
    private static final String USERNAME = "ElisaVerza";
    private static final boolean DOWNLOAD_COMMIT = false;
    private static final boolean DOWNLOAD_JIRA = true;
    private static final boolean DOWNLOAD_VERSIONS = false;
    private static final String AUTH_CODE = "/home/ella/vsWorkspace/auth_code.txt";
    private static final String DATE_FORMAT = "yyyy-MM-dd";

    public static Date getLastRelease() throws IOException, ParseException{
        Date last = new Date();
        try(BufferedReader br = new BufferedReader(new FileReader(CSV_VERSIONS))){
            String line = br.readLine();
            while ( (line = br.readLine()) != null ) {
                String[] values = line.split(",");
                last = new SimpleDateFormat(DATE_FORMAT).parse(values[1]);

            }
        }
        return last;
    }

    public static String getVersionByDate(String date) throws IOException, ParseException{
        Date openingToDate = new SimpleDateFormat(DATE_FORMAT).parse(date);
        Date versionToDate = new Date();
        String version = " ";
        try(BufferedReader br = new BufferedReader(new FileReader(CSV_VERSIONS))){
            String line = br.readLine();
            while ( (line = br.readLine()) != null ) {
                String[] values = line.split(",");
                versionToDate = new SimpleDateFormat(DATE_FORMAT).parse(values[1]);
                if(versionToDate.compareTo(openingToDate)>=0){
                    version = values[0];
                    break;
                }
            }
        }
        return version;
    }

    /**
     * Metodo che permette di autenticare le richieste fatte a github per evitare 
     * il limit rate sulle chiamate get.
     * 
     * @param url indirizzo della richiesta
     * @return input stream reader
     */

    public static InputStreamReader auth(URL url) throws IOException{
        String token;
        URLConnection uc = url.openConnection();
        uc.setRequestProperty("X-Requested-With", "Curl");

        try(BufferedReader oauthReader = new BufferedReader(new FileReader(AUTH_CODE));){
	 	   token = oauthReader.readLine();
	    }
        String username =  USERNAME;
        String userpass = username + ":" + token;
        byte[] encodedBytes = Base64.getEncoder().encode(userpass.getBytes());
        String basicAuth = "Basic " + new String(encodedBytes);
        uc.setRequestProperty("Authorization", basicAuth);

        return new InputStreamReader(uc.getInputStream());
    }

    /**
     * Metodo che prende in input un buffered reader e ne restituisce il contenuto
     * in formato stringa.
     *  
     * @param rd buffered reader da cui leggere
     * @return contenuto del buffered reader il formato stringa
     */
    private static String readAll(BufferedReader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
           sb.append((char) cp);
        }
        return sb.toString();
     }

     /**
     * Metodo per eseguire una get dall'url specificato dal parametro omonimo da cui 
     * si otterrà un JsonArray. Se la richiesta è inidirizzata a github viene chiamato 
     * il metodo auth che provvederà all'autenticazione della richiesta per evitare la 
     * limitazione delle richieste.
     * 
     * @param url URL da cui effetturare la get
     * @param git booleano per riconoscere se la get è indirizzata a github o no
     * @return jsonText JsonArray ottenuto con la chiamata get
     */
     public static JSONArray readJsonArrayFromUrl(String url, boolean git) throws IOException, JSONException {
        InputStreamReader is;
        if(git){
            URL strToUrl = new URL(url);
            is = auth(strToUrl);
        }
        else{
            InputStream iStream = new URL(url).openStream();
            is = new InputStreamReader(iStream);
        }
        try(BufferedReader rd = new BufferedReader(is)) {
           String jsonText = readAll(rd);

           return new JSONArray(jsonText);
         } finally {
           is.close();
         }
     }

     /**
     * Metodo per eseguire una get dall'url specificato dal parametro omonimo da cui 
     * si otterrà un JsonObject. Se la richiesta è inidirizzata a github viene chiamato 
     * il metodo auth che provvederà all'autenticazione della richiesta per evitare la 
     * limitazione delle richieste.
     * 
     * @param url URL da cui effetturare la get
     * @param git booleano per riconoscere se la get è indirizzata a github o no
     * @return jsonText JsonObject ottenuto con la chiamata get
     */
    public static JSONObject readJsonObjFromUrl(String url, boolean git) throws IOException, JSONException {
        InputStreamReader is;
        if(git){
            URL strToUrl = new URL(url);
            is = auth(strToUrl);
        }
        else{
            InputStream iStream = new URL(url).openStream();
            is = new InputStreamReader(iStream);
        }
        try(BufferedReader rd = new BufferedReader(is)){
           String jsonText = readAll(rd);
           return new JSONObject(jsonText);
         } finally {
           is.close();
         }
     }

     /**
      * Metodo per cercare il jira id nei commenti dei commit.
      *
      * @param toParse stringa del messaggio di commit in cui cercare il jira id
      * @return parseId stringa contenente il jira id
      */
    public static String parseId(String toParse) throws SecurityException{
        StringBuilder parsed = new StringBuilder();        
        Integer j = 0;

        if(toParse.contains(PRJ_NAME)){
            j = toParse.indexOf(PRJ_NAME);
            j=j+8;
            parsed.append("SYNCOPE-");

            while(toParse.length()>j){
                if(toParse.length()>j && Character.isDigit(toParse.charAt(j))){
                    parsed.append(toParse.charAt(j));
                }
                else{break;}
                j++;
            }
        }
        return parsed.toString();
    }
    
    /**
     * Metodo cercare un determinato valore in una colonna del csv. Il csv viene letto riga per riga
     * ed ad ognuna di queste viene fatto il parsing per colonna. Ogni riga viene confrontato il 
     * contenuto della colonna specificata con la stringa che si sta cercando, alla fine del file
     * viene restituito un array con il contenuto di tutte le righe contenenti la stringa cercata.
     * 
     * @param searchColumnIndex indice della colonna del csv (0 bound)
     * @param searchString stringa da cercare
     * @param file nome del file csv in cui cercare
     * @return resultRow array di stringhe contenente le righe che contengono searchString
     */
    public static String[] searchCsvLine(int searchColumnIndex, String searchString, String file) throws IOException {
        String[] resultRow = new String[0];
        Integer i = 0;
        try(BufferedReader br = new BufferedReader(new FileReader(file))){
            String line;
            while ( (line = br.readLine()) != null ) {
                String[] values = line.split(",");

                if(values.length > searchColumnIndex && values[searchColumnIndex].equals(searchString)) {
                    String[] newArray = new String[resultRow.length + 1];
                    System.arraycopy(resultRow, 0, newArray, 0, resultRow.length);

                    resultRow = newArray;
                    resultRow[i] = line;
                    i++;
                }
            }
        }

        return resultRow;
    }

    public static Boolean validTicket(String iVersion, String ovDate) throws IOException, ParseException{
        Boolean valid = false;
        String [] versionDate = searchCsvLine(0, iVersion, CSV_VERSIONS);
        String[] values = versionDate[0].split(",");
        if(values.length != 0){
            Date initialVersion = new SimpleDateFormat(DATE_FORMAT).parse(values[1]);
            Date openingVersion = new SimpleDateFormat(DATE_FORMAT).parse(ovDate.substring(0, 10));
            valid = initialVersion.compareTo(openingVersion)<=0;
        }
        return valid;
    }

    public static Tuple minVersion(String[] versionArray) throws IOException, CsvValidationException{
        List<List<String>> csv = csvToList(CSV_VERSIONS);
        Integer i;
        Integer j;
        Integer lastIndex = csv.size()+1;
        Tuple lastVer = Pair.with(0, " ");
        for(i=0; i<versionArray.length; i++){
            for(j=1; j<csv.size(); j++){
                if(versionArray[i]!=null && versionArray[i].equals(csv.get(j).get(0)) && j<lastIndex){
                    lastIndex = j;
                    lastVer = Pair.with(lastIndex, csv.get(lastIndex).get(0));
                    break;
                }
            }
        }
        return lastVer;
    }
    
    public static List<List<String>> csvToList(String csvFile) throws IOException, CsvValidationException{
        List<List<String>> records = new ArrayList<>();
        try (CSVReader csvReader = new CSVReader(new FileReader(csvFile));) {
            String[] values = null;
            while ((values = csvReader.readNext()) != null) {
                records.add(Arrays.asList(values));
            }
        }  
      return records;
    }

    public static String[] findAffected(JSONArray versionArray) throws IOException{
        Integer j = 0;
        Integer i = 0;
        String[] version = new String[0];

        while(versionArray.length()!=0 && i<versionArray.length()){  
            String currAffVersion = versionArray.getJSONObject(i).getString("name");
            Boolean isReleasedVersion = searchCsvLine(0, currAffVersion, CSV_VERSIONS).length != 0;
            if(Boolean.TRUE.equals(isReleasedVersion)){
                String[] newArray = new String[version.length + 1];
                System.arraycopy(version, 0, newArray, 0, version.length);
                version = newArray;
                version[j] = currAffVersion;
                j++;
            }
            i++;
        }
        return version;
    }

    public static String[] findFixed(JSONArray fixVersionArray) throws IOException{
        Integer i = 0;
        Integer j = 0;
        String[] fixVersion = new String[0];

        while(fixVersionArray.length()!=0 && i<fixVersionArray.length()){    
            String currFixVersion = fixVersionArray.getJSONObject(i).getString("name");
            //Controllo sulle fixed versions per vedere se sono released
            Boolean isReleasedVersion = searchCsvLine(0, currFixVersion, CSV_VERSIONS).length != 0;
            if(Boolean.TRUE.equals(isReleasedVersion)){
                String[] newArray = new String[fixVersion.length + 1];
                System.arraycopy(fixVersion, 0, newArray, 0, fixVersion.length);
                fixVersion = newArray;
                fixVersion[j] = currFixVersion;
                j++;
            }
            i++;
        }

        return fixVersion;
    }


    /**
     * Metodo per ottenere i dati necessari dal jsonArray contenente il informazioni dei ticket 
     * jira ed inserirli, tramite la chiave jira id nel csv con le informazioni dei commit.
     * Vengono ricavati i valori delle Affected Version, Fixed Version, id ticket jira. Il metodo
     * searchCsvLine trova la riga del csv dei commit che risolve il ticket in questione.
     * Quest'ultima insieme ai dati del ticket vengono scritti sul csv specificato dal file writer.
     * 
     * @param i indice dell'arrayJson in cui leggere i dati
     * @param json JsonArray contenente le informazioni di tutti i ticket
     * @param commitWriter File Writer in cui copiare dati del commit associati a dati del ticket
     * @return void
     * @throws ParseException
     * @throws CsvValidationException
     * @throws InterruptedException
     */
    public static void jiraJsonArray(Integer i, JSONArray json, FileWriter jiraWriter) throws IOException, ParseException, CsvValidationException, InterruptedException{
        String jsonKey = "fields";
        Boolean isValid = true;
        JSONArray versionArray = json.getJSONObject(i%1000).getJSONObject(jsonKey).getJSONArray("versions");
        JSONArray fixVersionArray = json.getJSONObject(i%1000).getJSONObject(jsonKey).getJSONArray("fixVersions");
        String ovDate = json.getJSONObject(i%1000).getJSONObject(jsonKey).getString("created");
        Tuple oldestAffected = Pair.with(0, " ");
        Tuple oldestFixed = Pair.with(0, " ");
        Integer k = 0;

        String[] version = findAffected(versionArray);
        String[] fixVersion = findFixed(fixVersionArray);

        if(version.length != 0){
            oldestAffected = minVersion(version);
            //Controllo per verificare che ov<iv
            isValid = validTicket(version[0], ovDate);

        }

        if(fixVersion.length != 0){
            oldestFixed = minVersion(fixVersion);
        }

        //Controllo per vedere se è un post release bug
        Boolean isPostReleaseTicket = oldestAffected.getValue(1).equals(oldestFixed.getValue(1));

        if(Boolean.FALSE.equals(isPostReleaseTicket) && Boolean.TRUE.equals(isValid)){
            String key = json.getJSONObject(i%1000).get("key").toString();
            String[] commit = searchCsvLine(2, key, CSV_COMMIT);
            for(k=0;k<commit.length;k++){
                if(commit[k] != null){
                    commit[k] = commit[k].replace("\n", " ");
                    jiraWriter.append(commit[k]+","+oldestAffected.getValue(1)+","+oldestFixed.getValue(1)+","+ovDate+"\n");
                }
            }
        }
    }

    /** 
    * Metodo per ottenere dal jsonObject dei ticket jira il jsonArray "issue" cotenente i dati utili.
    * Viene fatta la query su tutti i ticket di tipo BUG con stato CLOSED o RESOLVED e resolution FIXED.
    * Nella stringa url viene specificata la query che il metodo readJsonObjectFromUrl esegue.
    * Infine viene chimato il metodo jiraJsonArray per estrarre dal jsonArray i dati.
    *
    * @param jiraWriter FileWriter del csv su cui verranno scritti i dati necessari dei ticket jira
    * @return void
     * @throws ParseException
     * @throws CsvValidationException
     * @throws InterruptedException
    */
    public static void jiraData(FileWriter jiraWriter) throws JSONException, IOException, ParseException, CsvValidationException, InterruptedException{
        Integer j = 0;
        Integer i = 0;
        Integer total = 1;
        do {
            j = i + 1000;
            String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                    + PRJ_NAME + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                    + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,fixVersions,created&startAt="
                    + i.toString() + "&maxResults=" + j.toString();
            JSONObject jsonObj = readJsonObjFromUrl(url, false);
            JSONArray json = new JSONArray(jsonObj.getJSONArray("issues"));
            total = jsonObj.getInt("total");

            for (; i < total && i < j; i++) {
                jiraJsonArray(i, json, jiraWriter);
            }  
      } while (i < total);
    }

    /** 
    * Metodo per ottenere dal jsonArray dei commit, scaricato da github, le informazioni necessarie:
    * data, messaggio, sha del commit. Nella stringa url viene specificata la query che il metodo
    * readJsonArrayFromUrl esegue tramite chiamata get. Dal campo "message", con il metodo parseId,
    * viene ricavato l'id del corrispondente ticket jira di cui il commit è risolutivo.
    * Vengono presi solo i commit della prima metà release.
    *
    * @param commitWriter FileWriter del csv su cui verranno scritti i dati necessari dei commit
    * @return void
     * @throws ParseException
    */
    public static void commitData(FileWriter commitWriter) throws IOException, InterruptedException, ParseException{
        Integer i = 1; 
        Integer l = 0;
        Integer k;
        String date;
        String sha;
        String jiraId;
        Date lastRelease = getLastRelease();
        do{
            String url = "https://api.github.com/repos/apache/"+PRJ_NAME+"/commits?page="+i.toString()+"&per_page=100";
            JSONArray jPage = new JSONArray(readJsonArrayFromUrl(url, true));
            l = jPage.length();
            Thread.sleep(1000);
            for(k=0; k<l; k++){
                jiraId = parseId(jPage.getJSONObject(k).getJSONObject("commit").getString("message"));
                date = jPage.getJSONObject(k).getJSONObject("commit").getJSONObject("committer").getString("date");
                Date commitDate = Date.from(Instant.parse(date));
                if(jiraId.contains(PRJ_NAME) && lastRelease.compareTo(commitDate)>=0){
                    sha = jPage.getJSONObject(k).getString("sha");
                    commitWriter.append(date + "," +sha+","+ jiraId +"\n");
                }
            }                
            i++;
        } while(l != 0);
    }

    /**
     * Metodo che restituisce, in formato Data, la data della release più recente tra quelle prese
     * in considerazione, ovvero la metà
     * 
     * @param versionsWriter File writer del csv contenente tutte le release con nome e data
     */
    public static void versionsData(FileWriter versionsWriter) throws JSONException, IOException{
        String url = "https://issues.apache.org/jira/rest/api/2/project/"+PRJ_NAME+"/";
        Integer len;
        Integer i;
        JSONObject jsonObj = readJsonObjFromUrl(url, false);
        JSONArray json = new JSONArray(jsonObj.getJSONArray("versions"));
        len = json.length()/2;
        for(i = 0; i<len; i++){
            if(json.getJSONObject(i).getBoolean("released")){
                versionsWriter.append(json.getJSONObject(i).getString("name")+","+
                                      json.getJSONObject(i).getString("releaseDate")+"\n");
            }
        }

    }


    /**
    * Metodo che gestisce i due file prodotti dalla classe. Crea i file csv che conterranno
    * i dati dei commit provenienti da github ed i dati dei ticket da jira. Invoca i metodi
    * che popleranno i csv. Ciò avviene solo se le due variabili booleane DOWNLOAD_COMMIT e
    * DOWNLOAD_JIRA sono poste a true. Nel caso siano false non viene effettuato il download
    * dei dati assumendo che i file siano già stati popolati in precedenza.
    *
    * @return void
     * @throws ParseException
     * @throws JSONException
     * @throws CsvValidationException
    */
    public static void fileHandler() throws IOException, InterruptedException, ParseException, CsvValidationException, JSONException{

        if(DOWNLOAD_VERSIONS){
            File versionsFile = new File(CSV_VERSIONS);
                try(FileWriter versionsWriter = new FileWriter(versionsFile)){
                    versionsWriter.append("name, release date\n");
                    versionsData(versionsWriter);    
                }
        }
        if(DOWNLOAD_COMMIT){
            File commitFile = new File(CSV_COMMIT);
            try(FileWriter commitWriter = new FileWriter(commitFile)){
                commitWriter.append("commit date,commit sha,jira_id\n");
                commitData(commitWriter);
            }
        }
        if(DOWNLOAD_JIRA){
        File jiraFile = new File(CSV_JIRA);
            try(FileWriter jiraWriter = new FileWriter(jiraFile)){
                jiraWriter.append("commit date git,commit sha,jira_id,affected versions,fixed version\n");
                jiraData(jiraWriter);    
            }
        }
    }


    public static void main(String[] args) throws IOException, InterruptedException, ParseException, CsvValidationException, JSONException{
        fileHandler();
    }
}
