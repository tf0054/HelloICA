package com.digipepper.test.ica.nlp;

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.LinkedList;
import java.util.List;

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

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.DBCursor;
import com.mongodb.DBObject;
import com.mongodb.Mongo;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;

public class PostFromMongoNLP {

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
	@Option(name="-mongo", metaVar="flgMongo", usage="Get statements from mongodb.",required=false)
	private static boolean flgMongo;
	
    public final static void main(String[] args) throws Exception {
		
		PostFromMongoNLP shell = new PostFromMongoNLP();
        CmdLineParser parser = new CmdLineParser(shell);
        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.out.println("Usage");
            System.out.println(" PostFromMongoICA -hostname <hostname> -port <port> -username <username> -password <password> -collectionId <collectionId>");
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

		if (!flgMongo) {
			final HttpMethod method = buildHttpMethod(hostname, portNum, user, password, cid,
					"今晩はパスタです。");
			printOutput(httpClient, method);
		} else {
			int intCount = 10;

			Mongo mongo = new Mongo("localhost", 27017);
			DB db = mongo.getDB("test");
			DBCollection collection = db.getCollection("tweets");
			BasicDBObject searchQuery = new BasicDBObject();
			//searchQuery.put("content", java.util.regex.Pattern.compile("（|\\("));		
			cursor = collection.find(searchQuery);
			
			System.out.println("The num of Data on mongodb: "+cursor.count());

			String strRes = null;
			DBObject objItem = null;
			while (cursor.hasNext()) {
				if ((intCount-- == 0))
					break;
				objItem = cursor.next();
				strRes = (String) objItem.get("content");
				strRes = strRes.replaceAll("[\\r\\n]", " ");  
				final HttpMethod method = buildHttpMethod(hostname, portNum, user, password, cid, 
						strRes);

				printOutput(httpClient, method);
			}
		}
     }
           private static void printOutput(HttpClient httpClient, HttpMethod method){
	           // Access REST API with commons HTTP Client
	           method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);

	           try {
	              // Access to the API
					System.out.println("accessing to " + method.getURI());
	              httpClient.executeMethod(method);
	              final InputStream input = method.getResponseBodyAsStream();

	              final BufferedReader br = new BufferedReader(new InputStreamReader(input, "UTF-8"));
	
	              // Print response
	              String line = null;
	              while ((line = br.readLine()) != null) {
	            	  System.out.println(line);
	              }
				} catch (Exception e) {
					// TODO Auto-generated catch block
					e.printStackTrace();
	           } finally {
		              method.releaseConnection();
		           }

           }
           
     private static HttpMethod buildHttpMethod(final String hostname, final int port, final String user, final String password, final String cname, final String strContent) throws FileNotFoundException {
        final StringBuffer url = new StringBuffer("http://");
        url.append(hostname).append(":").append(port).append("/api/v10/analysis/text");
        url.append("?api_username=").append(user);
        url.append("&api_password=").append(password);
        final PostMethod method = new PostMethod(url.toString());
        
        // Post parameters
        final List<Part> parts = new LinkedList<Part>();
        parts.add(new StringPart("collection", cname));
        parts.add(new StringPart("output", "application/json"));
        parts.add(new StringPart("language", "ja"));
        parts.add(new StringPart("text", strContent, "UTF-8"));

        final RequestEntity request = new MultipartRequestEntity(parts.toArray(new Part[parts.size()]), method.getParams());
        method.setRequestEntity(request);

        return method;
     }

}
