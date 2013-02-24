package com.digipepper.test.ica.tool;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;

import net.sf.json.JSONArray;
import net.sf.json.JSONObject;
import net.sf.json.JSONSerializer;

import org.apache.commons.httpclient.Credentials;
import org.apache.commons.httpclient.HttpClient;
import org.apache.commons.httpclient.HttpMethod;
import org.apache.commons.httpclient.UsernamePasswordCredentials;
import org.apache.commons.httpclient.auth.AuthScope;
import org.apache.commons.httpclient.cookie.CookiePolicy;
import org.apache.commons.httpclient.methods.PostMethod;
import org.apache.commons.httpclient.methods.RequestEntity;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.StringPart;
import org.apache.commons.io.FileUtils;
import org.apache.commons.io.IOUtils;
import org.apache.commons.io.LineIterator;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;


/*
 * Manual
 * http://www-01.ibm.com/support/docview.wss?uid=swg27023678
 */

public class SetFacetsViaRest {

	static DBCursor cursor = null;

	@Option(name="-hostname", metaVar="hostname", usage="Fully qualified host name of the server.",required=true)
	private static String hostname;
	@Option(name="-port", metaVar="port", usage="Port number for Admin REST API running on the server. The default value is 8390 (Embedded Server) or 80 (WebSphere Application Server).",required=true)
	private static int port;
	@Option(name="-username", metaVar="user", usage="Administrative user name.",required=true)
	private static String user;
	@Option(name="-password", metaVar="password", usage="Administrative user password.",required=true)
	private static String password;
	@Option(name="-collectionId", metaVar="cid", usage="ID of the collection to add new document to.",required=true)
	private static String cid;
	
	public final static void main(String[] args) throws Exception {

		SetFacetsViaRest shell = new SetFacetsViaRest();
		CmdLineParser parser = new CmdLineParser(shell);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("Usage");
			System.out
					.println(" CheckViaRest -hostname <hostname> -port <port> -username <username> -password <password> -collectionId <collectionId>");
			System.out.println();
			parser.printUsage(System.out);
			return;
		}

		int portNum = port;

		// Create Commons HTTP Client Object to access REST API
		final HttpClient httpClient = new HttpClient();
		// Set Basic authentication credentials
		final Credentials credentials = new UsernamePasswordCredentials(user,
				password);
		final AuthScope authscope = new AuthScope(hostname, portNum);
		httpClient.getState().setCredentials(authscope, credentials);
		httpClient.getParams().setAuthenticationPreemptive(true);

		//final HttpMethod method = buildListMethod(hostname, portNum, user, password, cid);
		//HttpMethod method = buildFieldListMethod(hostname, portNum, user, password, cid);
		HttpMethod method = null;
		// Access REST API with commons HTTP Client
		//method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);

		// Filename
		//List<String> objList = getListFromFile("jalanReviewFields.txt");
		List<String> objList = getListFromFile("datasift.txt");

		// set loop
		String strTmp = "";
		Iterator<String> objIte = objList.iterator();
		while (objIte.hasNext()) {
			strTmp = new String(objIte.next().getBytes("UTF-8"));
			strTmp = strTmp.substring(3, strTmp.length()-1); // I don't know why.
			System.out.println(">"+strTmp+"<");
			try {
				method = buildAdminMethod(hostname, port, user, password, cid, strTmp);
			} catch (FileNotFoundException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
//			// Delete
			strTmp = getResultJson(httpClient, method);
			System.out.println(strTmp);
		}

		method.releaseConnection();

	}
	
	private static List<String> getListFromFile(String strFilename){

		List<String> listTmp = new ArrayList<String>();
        // This will reference one line at a time
        String line = null;

        try {
        	LineIterator it = FileUtils.lineIterator(new File(strFilename), "UTF-8");
        	 try {
        		   while (it.hasNext()) {
        		     line = it.nextLine();
                	 if(line.indexOf("#") != 0){
                 		line = new String(line.getBytes(),"UTF-8");
                 		line = line.substring(0, line.indexOf(":"));
                 		System.out.println(line);
                     	listTmp.add(line);
                 	}
             		//System.out.println(line);
        		   }
        		 } finally {
        		   LineIterator.closeQuietly(it);
        		 }
            System.out.println("end");
        }
        catch(FileNotFoundException ex) {
            System.out.println(
                "Unable to open file '" + 
                		strFilename + "'");				
        }
        catch(IOException ex) {
            System.out.println(
                "Error reading file '" 
                + strFilename + "'");					
        }
        return listTmp;
	}
           
     private static HttpMethod buildAdminMethod(final String hostname, final int port, final String user, final String password, final String cname, String strField) throws FileNotFoundException {
         final StringBuffer url = new StringBuffer("http://");
         //url.append(hostname).append(":").append(port).append("/api/v10/admin/field?method=add");
         //url.append(hostname).append(":").append(port).append("/api/v10/admin/facet?method=add");
         url.append(hostname).append(":").append(port).append("/api/v10/admin/facetTree?method=editFacet");
         url.append("&api_username=").append(user);
         url.append("&api_password=").append(password);
         final PostMethod method = new PostMethod(url.toString());
         
         // Post parameters
         final List<Part> parts = new LinkedList<Part>();
         parts.add(new StringPart("collectionId", cname));
         parts.add(new StringPart("name", strField)); // facet & facetTree 
         parts.add(new StringPart("fields", "["+strField+"]")); // facetTree
         parts.add(new StringPart("path", strField.replaceAll("\\.", "-"))); // facetTree
         //parts.add(new StringPart("sources", "[\""+strField+"\"]")); //  facet
         parts.add(new StringPart("output", "json"));

         final RequestEntity request = new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), method.getParams());
         method.setRequestEntity(request);

         return method;
      }

	private static String getResultJson(HttpClient httpClient, HttpMethod method) {
		// Access REST API with commons HTTP Client
		method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
	
		try {
			// Access to the API
			System.out.println("accessing to " + method.getURI());
			httpClient.executeMethod(method);
			
			//System.out.println("header: "+method.getResponseHeaders().toString());
			final InputStream input = method.getResponseBodyAsStream();
			final BufferedReader br = new BufferedReader(new InputStreamReader(
					input, "UTF-8"));
			return IOUtils.toString(br);
		} catch (Exception e) {
			return null;
		}
	}

}
