package com.digipepper.test.ica.post.datasift;

/*
 * non one-jar
 * java -jar HelloICA_fat.jar -hostname ec2-175-41-203-170.ap-northeast-1.compute.amazonaws.com -port 8390 -username esadmin -password esadmin -collectionId nakano2 -mhostname localhost -mcollection cameran -numLoops 10 -numDocs 5
 */

import java.io.BufferedReader;
import java.io.FileNotFoundException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.Iterator;
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
import org.apache.commons.httpclient.methods.multipart.ByteArrayPartSource;
import org.apache.commons.httpclient.methods.multipart.FilePart;
import org.apache.commons.httpclient.methods.multipart.MultipartRequestEntity;
import org.apache.commons.httpclient.methods.multipart.Part;
import org.apache.commons.httpclient.methods.multipart.PartSource;
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

public class PostFromMongo {

	static DBCursor cursor = null;

	@Option(name="-host", metaVar="hostname", usage="Fully qualified host name of the server.",required=true)
	private static String hostname;
	@Option(name="-port", metaVar="port", usage="Port number for Admin REST API running on the server. The default value is 8390 (Embedded Server) or 80 (WebSphere Application Server).",required=true)
	private static int port;
	@Option(name="-username", metaVar="user", usage="Administrative user name.",required=true)
	private static String user;
	@Option(name="-password", metaVar="password", usage="Administrative user password.",required=true)
	private static String password;
	@Option(name="-collection", metaVar="cid", usage="ID of the collection on ICA to add new document to.",required=true)
	private static String icid;
	
	@Option(name="-mhost", metaVar="mhost", usage="Fully qualified host name of mongodb.",required=true)
	private static String mhost;
	@Option(name="-mport", metaVar="mport", usage="Port number for mongodb.",required=true)
	private static int mport;
	@Option(name="-mdb", metaVar="mcid", usage="Db name of mongodb.",required=true)
	private static String mdb;
	@Option(name="-mcollection", metaVar="mcid", usage="Collection name of mongodb.",required=true)
	private static String mcid;
	
	@Option(name="-numDocs", metaVar="numDocs", usage="num of docs for one post.",required=true)
	private static int numDocs;
	@Option(name="-numLoops", metaVar="numLoops", usage="num of loop.",required=false)
	private static int numLoops;
	
    public final static void main(String[] args) throws Exception {

		PostFromMongo shell = new PostFromMongo();
        CmdLineParser parser = new CmdLineParser(shell);

        try {
            parser.parseArgument(args);
        } catch (CmdLineException e) {
            System.out.println("Usage");
            System.out.println(" PostFromMongo -hostname <hostname> -port <port> -username <username> -password <password> -collectionId <collectionId>");
            System.out.println();
            parser.printUsage(System.out);
                        
            return;
        }

		Mongo mongo = new Mongo(mhost, mport);
		DB db = mongo.getDB(mdb);
		DBCollection collection = db.getCollection(mcid);

		BasicDBObject searchQuery = new BasicDBObject();
		//searchQuery.put("content", java.util.regex.Pattern.compile("（|\\("));		
		cursor = collection.find(searchQuery);
		        
		// auto generate the num of loops.
		if(numLoops == 0)
			numLoops = (cursor.count() / numDocs) + 1;

		System.out.println(cursor.count() + " -> "+numDocs+"(docs) * "+numLoops+"(loops)");

       // Create Commons HTTP Client Object to access REST API
       final HttpClient httpClient = new HttpClient();
       // Set Basic authentication credentials
       final Credentials credentials = new UsernamePasswordCredentials(user, password);
       final AuthScope authscope = new AuthScope(hostname, port);
       httpClient.getState().setCredentials(authscope, credentials);
       httpClient.getParams().setAuthenticationPreemptive(true);

       int intCount = numLoops;//20;
       while(intCount-- > 0){
           // Build HTTP method to access the REST API
           final HttpMethod method = buildHttpMethod(hostname, port, user, password, new String[]{mcid,icid});
           method.getParams().setCookiePolicy(CookiePolicy.BROWSER_COMPATIBILITY);
           
           // Access REST API with commons HTTP Client
           try {
              // Access to the API
              System.out.println("accessing to " + method.getURI());
              httpClient.executeMethod(method);

              final BufferedReader br = new BufferedReader(
            		  new InputStreamReader(
            				  method.getResponseBodyAsStream(), "UTF-8"));

              // Print response
              String line = "";
              while ((line = br.readLine()) != null) {
                 System.out.println(line);
              }
           } finally {
              method.releaseConnection();
           }
       }
     }

     private static HttpMethod buildHttpMethod(final String hostname, final int port, final String user, final String password, final String[] cids) throws FileNotFoundException {
        final StringBuffer url = new StringBuffer("http://");
        url.append(hostname).append(":").append(port).append("/api/v10/admin/document?method=addMultiDocs");
        url.append("&api_username=").append(user);
        url.append("&api_password=").append(password);
        final PostMethod method = new PostMethod(url.toString());
        
        final List<Document> docs = getDocuments();
        // Post parameters and the file with Multipart
        final List<Part> parts = new LinkedList<Part>();
        parts.add(new StringPart("collectionId", cids[1]));
        parts.add(new StringPart("documentSource", "datasift/"+cids[0]));
        parts.add(new StringPart("docs", buildDocsJSONString(docs)));
        
        System.out.println("The num of documents: "+docs.size());
        for (Document d : docs) {
        	if(d.getContent() != null){
	            byte[] bytes = d.getContent().getBytes(); // the main content is sent as file
	            PartSource partSource = new ByteArrayPartSource(d.getFilename(), bytes);
	            parts.add(new FilePart("file", partSource));
        	}else{
        		System.out.println("docId: "+d.getDocId());
        	}
        }
        parts.add(new StringPart("output", "json"));
        final RequestEntity request = new MultipartRequestEntity(
        		parts.toArray(new Part[parts.size()]), method.getParams());
        method.setRequestEntity(request);

        return method;
     }

     private static List<Document> getDocuments() {
        final List<Document> docs = new LinkedList<Document>();
		DBObject objItem = null;
        int intCount = numDocs;
        
		while (cursor.hasNext()) {
			if((intCount-- == 0))
				break;
			objItem = cursor.next();
			//
        	HashMap<String,String> objHash = new HashMap<String,String>();

        	toBean(objItem, "", objHash);
        	
			final Document objDocument = new Document(
					"interaction.twitter.id", //docId
					"interaction.interaction.content", //"interaction.twitter.text"
					objHash
			);
			
	        objDocument.setTitle(objItem.get("interaction.twitter.user.screen_name")+" / "+objHash.get("interaction.twitter.user.id"));
	        objDocument.setFormat("text/plain");
	        objDocument.setLang("ja");
	        docs.add(objDocument);
		}
		
        return docs;
     }

     private static String buildDocsJSONString(List<Document> docs) {
        final StringBuffer sb = new StringBuffer("[");
        if(docs.size() < 1){
            System.out.println("Error (no docs)");
            System.exit(1);
        }
        for (Document d : docs) {
           sb.append(d.toJson()).append(",");
        }
        sb.deleteCharAt(sb.length() - 1); // remove the last comma
        sb.append("]");
        //System.out.println(sb.toString()); // debug
        return sb.toString();
     }

     private static void toBean(Object json, String strKey, HashMap<String, String> objReturn) {
		if ((json instanceof BasicDBObject) || (json instanceof BasicDBObject)) {
			Iterator<String> objIte = ((DBObject) json).keySet().iterator();
			String strTmp = "";
			while (objIte.hasNext()) {
				strTmp = objIte.next();
				if (strKey.length() > 0)
					toBean(((DBObject) json).get(strTmp), strKey + "." + strTmp, objReturn);
				else
					toBean(((DBObject) json).get(strTmp), strTmp, objReturn);
			}
		} else {
			objReturn.put(strKey, json.toString());
		}
	}

}
