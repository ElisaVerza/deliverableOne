package elisaverza;

import java.io.BufferedReader;
//import java.io.File;
import java.io.FileReader;
//import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

import org.json.JSONArray;

public class DataRetrieve 
{
    //private static final String CSV_FILENAME = "data.csv";
    private static final String PRJ_NAME = "Bookkeeper";
    private static final String USERNAME = "ElisaVerza";
    private static final String AUTH_CODE = "/home/ella/vsWorkspace/auth_code.txt";

    public static URLConnection auth(URL url) throws IOException{
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

        return uc;
    }

    public static JSONArray download_data() throws IOException{
        JSONArray jPage = new JSONArray();
        URL url = new URL("https://api.github.com/repos/apache/"+PRJ_NAME+"/commits?page=1&per_page=100");
        URLConnection uc = auth(url);
        InputStreamReader inputStreamReader = new InputStreamReader(uc.getInputStream());

        try(BufferedReader rd = new BufferedReader(inputStreamReader);){
            StringBuilder sb = new StringBuilder();
            int cp;
            while ((cp = rd.read()) != -1) {
               sb.append((char) cp);
            }
            jPage = new JSONArray(sb.toString());
            
        } finally {
           inputStreamReader.close();
        }
        return jPage;
    }

    public static void main( String[] args ) throws IOException{
        /* TODO: Creazione colonne csv finale, funziona
        File csvFile = new File(CSV_FILENAME);
        FileWriter csvWriter = new FileWriter(csvFile);
        csvWriter.append("name, class name, version, bugginess\n");
        csvWriter.close();*/
        System.out.println(download_data().toString());
    }
}
