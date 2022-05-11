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
import java.util.Base64;


import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

public class DataRetrieve 
{
    private static final String CSV_COMMIT = "jiradata.csv";
    private static final String PRJ_NAME = "SYNCOPE";
    private static final String USERNAME = "ElisaVerza";
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

    private static String read_all(BufferedReader rd) throws IOException {
        StringBuilder sb = new StringBuilder();
        int cp;
        while ((cp = rd.read()) != -1) {
           sb.append((char) cp);
        }
        return sb.toString();
     }

     public static JSONArray read_json_array_from_url(String url, Boolean git) throws IOException, JSONException {
        InputStreamReader is;
        if(git){
            URL strToUrl = new URL(url);
            is = auth(strToUrl);
        }
        else{
            InputStream iStream = new URL(url).openStream();
            is = new InputStreamReader(iStream);
        }
        try {
           BufferedReader rd = new BufferedReader(is);
           String jsonText = read_all(rd);

           JSONArray json = new JSONArray(jsonText);
           return json;
         } finally {
           is.close();
         }
     }
  
     public static JSONObject read_json_obj_from_url(String url, Boolean git) throws IOException, JSONException {
        InputStreamReader is;
        if(git){
            URL strToUrl = new URL(url);
            is = auth(strToUrl);
        }
        else{
            InputStream iStream = new URL(url).openStream();
            is = new InputStreamReader(iStream);
        }
        try {
           BufferedReader rd = new BufferedReader(is);
           String jsonText = read_all(rd);

           JSONObject json = new JSONObject(jsonText);
           return json;
         } finally {
           is.close();
         }
     }

    public static String parse_id(String toParse) throws SecurityException, IOException{
        String parsed = "";
        Integer i = toParse.indexOf("#", 0);
        Integer j;

        if(toParse.contains("BOOKKEEPER-")){
            j = toParse.indexOf("BOOKKEEPER-");
            while(toParse.length()>j){
                if(Character.isDigit(toParse.charAt(j))){
                    parsed = parsed+toParse.charAt(j);
                    j++;
                }
                else{ break;}
            }
        }
        if(toParse.contains("SYNCOPE-")){
            j = toParse.indexOf("SYNCOPE-");
            while(toParse.length()>j){
                if(Character.isDigit(toParse.charAt(j))){
                    parsed = parsed+toParse.charAt(j);
                    j++;
                }
                else{ break;}
            }
        }
        if(parsed.length()>0){return parsed;}

	    while(i>=0 && toParse.length()>i+1){
	        if(Character.isDigit(toParse.charAt(i+1))){
                System.out.println(toParse.length()+" "+i);
	            while(toParse.length()>i+1 && Character.isDigit(toParse.charAt(i+1))){
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

    public static void jira_data(FileWriter commitWriter) throws JSONException, IOException{
        Integer j = 0, i = 0, total = 1;
      //Get JSON API for closed bugs w/ AV in the project
      do {
         //Only gets a max of 1000 at a time, so must do this multiple times if bugs >1000
         j = i + 1000;
         JSONArray json = new JSONArray();
         String url = "https://issues.apache.org/jira/rest/api/2/search?jql=project=%22"
                + PRJ_NAME + "%22AND%22issueType%22=%22Bug%22AND(%22status%22=%22closed%22OR"
                + "%22status%22=%22resolved%22)AND%22resolution%22=%22fixed%22&fields=key,resolutiondate,versions,created&startAt="
                + i.toString() + "&maxResults=" + j.toString();
         JSONObject jsonObj = read_json_obj_from_url(url, false);
         json = new JSONArray(jsonObj.getJSONArray("issues"));
         total = jsonObj.getInt("total");
         for (; i < total && i < j; i++) {
            //Iterate through each bug
            String key = json.getJSONObject(i%1000).get("key").toString();
            System.out.println(key);
            commitWriter.append(key+"\n");
         }  
      } while (i < total);
    }


    public static void commit_data(FileWriter commitWriter) throws IOException, InterruptedException{
        Integer i = 1; 
        Integer l = 0;
        Integer k;
        String date;
        String jiraId;
        JSONArray jPage;
        String temp;
        do{
            jPage = new JSONArray();

            String url = "https://api.github.com/repos/apache/"+PRJ_NAME+"/commits?page="+i.toString()+"&per_page=100";
            jPage = new JSONArray(read_json_array_from_url(url, true));
            l = jPage.length();
            Thread.sleep(1000);
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

                
            i++;
        } while(l != 0);
    }

    public static void main( String[] args ) throws IOException, InterruptedException{

        /*File commitFile = new File(CSV_COMMIT);
        FileWriter commitWriter = new FileWriter(commitFile);
        commitWriter.append("date,jira_id,comment\n");
        commit_data(commitWriter);
        commitWriter.close();*/
        File commitFile = new File(CSV_COMMIT);
        FileWriter commitWriter = new FileWriter(commitFile);
        jira_data(commitWriter);
        commitWriter.close();

    }
}
