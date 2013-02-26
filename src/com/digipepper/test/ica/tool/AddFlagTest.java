package com.digipepper.test.ica.tool;

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
import org.apache.commons.io.IOUtils;

import org.kohsuke.args4j.CmdLineException;
import org.kohsuke.args4j.CmdLineParser;
import org.kohsuke.args4j.Option;


/*
 * Manual
 * http://www-01.ibm.com/support/docview.wss?uid=swg27023678
 */

public class AddFlagTest {

	@Option(name="-hostname", metaVar="hostname", usage="Fully qualified host name of the server.",required=true)
	private static String hostname;
	@Option(name="-port", metaVar="port", usage="Port number for Admin REST API running on the server. The default value is 8390 (Embedded Server) or 80 (WebSphere Application Server).",required=true)
	private static int port;
	@Option(name="-username", metaVar="user", usage="Administrative user name.",required=true)
	private static String user;
	@Option(name="-password", metaVar="password", usage="Administrative user password.",required=true)
	private static String password;
	@Option(name="-colId", metaVar="cid", usage="ID of the collection to add new document to.",required=true)
	private static String cid;
	@Option(name="-flagName", metaVar="fname", usage="Name of the flag to use.",required=true)
	private static String fname;
	@Option(name="-flagDesc", metaVar="fdesc", usage="Description of the flag to use.",required=true)
	private static String fdesc;
	@Option(name="-flagColor", metaVar="fcolor", usage="Color of the flag to use.",required=false)
	private static String fcolor;
	
	public final static void main(String[] args) throws Exception {

		AddFlagTest shell = new AddFlagTest();
		CmdLineParser parser = new CmdLineParser(shell);
		try {
			parser.parseArgument(args);
		} catch (CmdLineException e) {
			System.out.println("Usage");
			System.out
					.println("SetFlagViaRest -hostname <hostname> -port <port> -username <username> -password <password> -collectionId <collectionId>");
			System.out.println();
			parser.printUsage(System.out);
			return;
		}

		if (fcolor == null) {
			fcolor = ColorUtils.randomColorHex();
			System.out.println("Flag color was automatically setted: "+fcolor);
		}
		
		// Create Commons HTTP Client Object to access REST API
		final HttpClient httpClient = new HttpClient();
		// Set Basic authentication credentials
		final Credentials credentials = new UsernamePasswordCredentials(user, password);
		final AuthScope authscope = new AuthScope(hostname, port);
		httpClient.getState().setCredentials(authscope, credentials);
		httpClient.getParams().setAuthenticationPreemptive(true);

		HttpMethod	method = null;
		// Do
		try {
			method = buildSetFlagMethod(hostname, port, user, password, cid, fname, fdesc, fcolor);
		} catch (FileNotFoundException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		// Print result
		String jsonTxt = getResultJson(httpClient, method);
		System.out.println(jsonTxt);

		method.releaseConnection();

	}
	
     private static HttpMethod buildSetFlagMethod(final String hostname, final int port, final String user, final String password, final String cname, String fname, String fdesc, String fcolor) throws FileNotFoundException {
         final StringBuffer url = new StringBuffer("http://");
         final String strAction = "set";

         url.append(hostname).append(":").append(port).append("/api/v10/admin/collection?method=addFlagDefinition");
         url.append("&action="+strAction);         
         url.append("&api_username=").append(user);
         url.append("&api_password=").append(password);
         
         final PostMethod method = new PostMethod(url.toString());
         
         // Post parameters
         final List<Part> parts = new LinkedList<Part>();
         parts.add(new StringPart("collectionId", cname));
         parts.add(new StringPart("flagName", fname)); 
         parts.add(new StringPart("description", fdesc)); 
         parts.add(new StringPart("color", fcolor)); 
         parts.add(new StringPart("output", "application/json"));

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
