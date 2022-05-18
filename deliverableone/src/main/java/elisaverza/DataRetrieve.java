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
import java.util.Arrays;
import java.util.Base64;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DataRetrieve 
{
    private static final String CSV_COMMIT = "commitdata.csv";
    private static final String CSV_JIRA = "ticketdata.csv";
    private static final String PRJ_NAME = "SYNCOPE";
    private static final String USERNAME = "ElisaVerza";
    private static final boolean DOWNLOAD_COMMIT = false;
    private static final boolean DOWNLOAD_JIRA = false;
    private static final String AUTH_CODE = "/home/ella/vsWorkspace/auth_code.txt";

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
        Integer i = 0;

        if(toParse.contains(PRJ_NAME)){
            j = toParse.indexOf(PRJ_NAME);
            i = j+8;
            while(toParse.length()>j && j<=i){
                if(toParse.length()>j+8 && Character.isDigit(toParse.charAt(j+8))){
                    i = j+8;
                }
                parsed.append(toParse.charAt(j));
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
     */
    public static void jiraJsonArray(Integer i, JSONArray json, FileWriter jiraWriter) throws IOException{
        String jsonKey = "fields";

        JSONArray versionArray = json.getJSONObject(i%1000).getJSONObject(jsonKey).getJSONArray("versions");
        JSONArray fixVersionArray = json.getJSONObject(i%1000).getJSONObject(jsonKey).getJSONArray("fixVersions");
        String[] version = new String[versionArray.length()];
        String[] fixVersion = new String[fixVersionArray.length()];
        Integer k = 0;
        while(versionArray.length()!=0 && k<versionArray.length()){    
            version[k] = versionArray.getJSONObject(k).getString("name");
            k++;
        }
        k=0;
        while(fixVersionArray.length()!=0 && k<fixVersionArray.length()){    
            fixVersion[k] = fixVersionArray.getJSONObject(k).getString("name");
            k++;
        }
        String key = json.getJSONObject(i%1000).get("key").toString();
        String[] commit = searchCsvLine(2, key, CSV_COMMIT);
        String versionStr = Arrays.toString(version);
        versionStr = versionStr.replace(",", " ");
        String fixVersionStr = Arrays.toString(fixVersion);
        fixVersionStr = fixVersionStr.replace(",", " ");
        for(k=0;k<commit.length;k++){

            if(commit[k] != null){
                commit[k] = commit[k].replace("\n", " ");
                jiraWriter.append(commit[k]+","+versionStr+","+fixVersionStr+"\n");
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
    */
    public static void jiraData(FileWriter jiraWriter) throws JSONException, IOException{
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
    *
    * @param commitWriter FileWriter del csv su cui verranno scritti i dati necessari dei commit
    * @return void
    */
    public static void commitData(FileWriter commitWriter) throws IOException, InterruptedException{
        Integer i = 1; 
        Integer l = 0;
        Integer k;
        String date;
        String sha;
        String jiraId;
        do{
            String url = "https://api.github.com/repos/apache/"+PRJ_NAME+"/commits?page="+i.toString()+"&per_page=100";
            JSONArray jPage = new JSONArray(readJsonArrayFromUrl(url, true));
            l = jPage.length();
            Thread.sleep(1000);
            for(k=0; k<l; k++){
                date = jPage.getJSONObject(k).getJSONObject("commit").getJSONObject("committer").getString("date");
                jiraId = parseId(jPage.getJSONObject(k).getJSONObject("commit").getString("message"));
                sha = jPage.getJSONObject(k).getString("sha");
                commitWriter.append(date + "," +sha+","+ jiraId +"\n");
            }                
            i++;
        } while(l != 0);
    }

    /**
    * Metodo che gestisce i due file prodotti dalla classe. Crea i file csv che conterranno
    * i dati dei commit provenienti da github ed i dati dei tiket da jira. Invoca i metodi
    * che popleranno i csv. Ciò avviene solo se le due variabili booleane DOWNLOAD_COMMIT e
    * DOWNLOAD_JIRA sono poste a true. Nel caso siano false non viene effettuato il download
    * dei dati assumendo che i file siano già stati popolati in precedenza.
    *
    * @return void
    */
    public static void fileHandler() throws IOException, InterruptedException{

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

    public static void main(String[] args) throws IOException, InterruptedException{
        fileHandler();
    }
}
