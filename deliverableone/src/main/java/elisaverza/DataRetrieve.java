package elisaverza;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.URL;
import java.net.URLConnection;
import java.util.Base64;

import org.json.JSONArray;
import org.json.JSONObject;

public class DataRetrieve 
{
    private static final String CSV_COMMIT = "commitdata.csv";
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

    public static String parse_id(String toParse){
        String parsed = "";
        Integer i = toParse.indexOf("#", 0);
	    while(i>=0){
	        if(Character.isDigit(toParse.charAt(i+1))){
	            while(Character.isDigit(toParse.charAt(i+1))){
		            parsed = parsed+toParse.charAt(i+1);
		            i++;
	            }
	            return parsed;
	        }
	        else{
	            i = toParse.indexOf("#", i+1);}
	    }
        return parsed;
    }

    public static void commit_data(FileWriter commitWriter) throws IOException{
        Integer i = 1; 
        Integer l = 0;
        Integer k;
        String date;
        String jiraId;
        JSONArray jPage;
        String temp;
        do{
            jPage = new JSONArray();
            URL url = new URL("https://api.github.com/repos/apache/"+PRJ_NAME+"/commits?page="+i.toString()+"&per_page=100");
            URLConnection uc = auth(url);
            InputStreamReader inputStreamReader = new InputStreamReader(uc.getInputStream());

            try(BufferedReader rd = new BufferedReader(inputStreamReader);){
                StringBuilder sb = new StringBuilder();
                int cp;
                while ((cp = rd.read()) != -1) {
                sb.append((char) cp);
                }
                jPage = new JSONArray(sb.toString());
                l = jPage.length();

                for(k=0; k<l; k++){
                    temp = jPage.getJSONObject(k).getJSONObject("commit").getString("message");
                    date = jPage.getJSONObject(k).getJSONObject("commit").getJSONObject("committer").getString("date");
                    jiraId = parse_id(jPage.getJSONObject(k).getJSONObject("commit").getString("message"));
                    temp = temp.replaceAll(",", " ");
                    temp = temp.replaceAll("\n", " ");
                    temp = temp.replaceAll("\r", " ");
                    temp = temp.replaceAll("\t", " ");
                    commitWriter.append(date + "," + jiraId +",");
                    commitWriter.append(temp);
                    commitWriter.append("\n");
                }

                
            } finally {
            inputStreamReader.close();
            }
            i++;
        } while(l != 0);
    }

    public static void main( String[] args ) throws IOException{

        File commitFile = new File(CSV_COMMIT);
        FileWriter commitWriter = new FileWriter(commitFile);
        commitWriter.append("date,jira_id,comment\n");
        commit_data(commitWriter);
        commitWriter.close();

    }
}
