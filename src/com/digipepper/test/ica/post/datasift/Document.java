package com.digipepper.test.ica.post.datasift;

import java.net.URLEncoder;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Locale;
import java.util.Set;

import net.sf.json.JSONArray;
import net.sf.json.JSONException;
import net.sf.json.JSONSerializer;

class Document {
    private final String docId;
    private final String strFilename;
    private final String strContent;
    private String lang;
    private String format;
    private String title;
    private String posted;
    private HashMap<String,String> hashExclude = new HashMap<String,String>();
    private HashMap<String,String> objFields = new HashMap<String,String>();

    public Document(final String docId, final String strContent, HashMap objHash) {

		// main infos
		this.docId = (String) objHash.get(docId);
		this.strContent = (String) objHash.get(strContent);
		this.strFilename = (String) objHash.get("_id");
		
		hashExclude.put(docId, "1");
		hashExclude.put(strContent, "1");
		hashExclude.put("_id", "1"); // mongo's id

		if(this.docId == null || this.strContent == null){
        	   System.out.println("NG: "+this.docId);
        	   System.exit(0);
		}else{
        	  // System.out.println("OK: "+this.docId);
		}

		// posted date
	   this.posted = this.toMilliseconds((String) objHash.get("interaction.interaction.created_at"));
       hashExclude.put("posted","1");
       //hashExclude.put("interaction.twitter.user.description","1");

       // useless fields
       hashExclude.put("interactionId","1");
       hashExclude.put("interaction.links.hops","1");
       //hashExclude.put("interaction.twitter.id","1");
       hashExclude.put("interaction.twitter.text","1");
       hashExclude.put("interaction.twitter.source","1");
       hashExclude.put("interaction.interaction.id","1");
       hashExclude.put("interaction.interaction.link","1");
       hashExclude.put("interaction.interaction.author.id","1");
       hashExclude.put("interaction.interaction.author.avatar","1");
       //hashExclude.put("interaction.twitter.user.statuses_count","1");
       
       this.objFields = objHash;
    }

    public String getDocId() {
		return docId;
	}

	public void setLang(final String lang) {
       this.lang = lang;
    }

    public void setFormat(String format) {
       this.format = format;
    }

    public void setTitle(final String title) {
       this.title = title;
    }

    public String getFilename() {
        return this.strFilename;
     }
    
    public String getContent() {
        return this.strContent;
     }

    public String toMilliseconds(String strDate){
    	Date date = null;
    	strDate = strDate.replaceAll("-", "/");
    	try {
        	// Wed, 05 Dec 2012 01:06:41 +0000
    		// http://www.javaroad.jp/java_date3.htm
			date = (new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss Z", Locale.US)).parse(strDate);
		} catch (ParseException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
    	return String.format("%TQ", date);
    }
	
	public String toJson() {
		final StringBuffer sb = new StringBuffer("{");
		
		sb.append("\"documentId\":\"").append(escape(this.docId)).append("\"");
		sb.append(",\"filename\":\"").append(escape(this.strFilename)).append("\"");
		if(this.posted != null) {
			sb.append(",\"documentDate\":").append(this.posted).append(""); // numeric
		}
		if (this.lang != null) {
			sb.append(",\"language\":\"").append(escape(this.lang)).append("\"");
		}
		if (this.format != null) {
			sb.append(",\"format\":\"").append(escape(this.format)).append("\",");
		}
		
		sb.append(toJson_mkFields());

		sb.append("}");
		System.out.println(sb.toString().substring(0, 130)+"..("+sb.length()+")");
		return sb.toString();
	}

	private StringBuffer toJson_mkFields() {
		final StringBuffer sb = new StringBuffer("");
		sb.append("\"fields\":{");
			String strRes = "";
			Set<String> objKeys = objFields.keySet();
			Iterator<String> objIte = objKeys.iterator();
			while (objIte.hasNext()) {
				strRes = (String) objIte.next();
				if(hashExclude.get(strRes) == null){
					sb.append("\"" + strRes + "\":").append(
							// We have to send Japanese sentences as ¥uNNNN format on json. 
							unicodeEscape("", objFields.get(strRes))
							//unicodeEscape(strRes, objFields.get(strRes))
							).append(",");
				}
			}
			sb.append("\"mongo_id\":\"").append(objFields.get("_id")).append("\"");
		sb.append("}");
		
		return sb;
	}
	
	// http://d.hatena.ne.jp/nishin5/20080916/1221538406
	private static String unicodeEscape(String colName, String value) {
		return unicodeEscape(colName, value, true);
	}
	
	private static String unicodeEscape(String colName, String value, boolean f) {

		if(f){
			if (value == null){
				System.out.println("false, null"); // debug
				return "\"\""; // json doesn't have null.
			} else if(value.startsWith("[")){
				JSONArray aryJson = null;
				JSONArray aryRes = new JSONArray();
				//System.out.println("conv: "+value);
				try{
					aryJson = (JSONArray) JSONSerializer.toJSON( value );
				}catch(JSONException e){
					return unicodeEscape(colName, value,false);
				}
				if(aryJson.size() > 1){
					for(int i = 0; i < aryJson.size(); i++){
						aryRes.add(unicodeEscape(colName, aryJson.getString(i), false));
					}
					return aryRes.toString();
				}else if(aryJson.size() == 1){
					return unicodeEscape(colName, aryJson.getString(0), false);
				}else{
					return ""; // probably error
				}
				
			}
		}
		
		/* ICA can recognize RFC 1123 (Sun, 06 Nov 1994 08:49:37 GMT),
		 * but datasift returns "Wed, 30 May 2012 01:21:22 +0000"
		*/
		value = value.replace("+0000", "GMT");

		// If the sentence does'nt have japanese, return escaped value immediately
		Zenkaku zenkaku = new Zenkaku();
		if(!zenkaku.includeZenkaku(value)){
			if(colName.indexOf("count")>0){
				return escape(value);
			}else{
				return "\""+escape(value)+"\"";
			}
		}

		char[] charValue = value.toCharArray();
	    
	    StringBuilder result = new StringBuilder();
	    for (char ch : charValue){
	        if (ch != '_'
	        		&& !(ch >= '0' && '9' >= ch)
	        		&& !(ch >= 'a' && 'z' >= ch)
	        		&& !(ch >= 'A' && 'Z' >= ch)) {    
	            String unicodeCh = Integer.toHexString((int)ch);
	           
	            result.append("\\u");
	            for (int i = 0; i < 4 - unicodeCh.length(); i++) {
	                result.append("0");
	            }
	            result.append(unicodeCh);

	        } else {
	        	result.append(ch);
	        }
	        
	    }
		if(colName.indexOf("count")>0){
			return result.toString();
		}else{
			return "\""+result.toString()+"\"";
		}
	}

    private static String escape(final String value) {
       String escaped = value.replace("\\", "\\\\"); // escape back-slash first
       escaped = escaped.replace("\"", "\\\""); // escape double quote
       //escaped = escaped.replace("/", "\\/"); // escape slash
       return escaped;
    }
    
    public static boolean isZen(String s){
        
        for(int i=0;i<s.length();i++){
            String s1 = s.substring(i, i+1);
            if(URLEncoder.encode(s1).length() < 4) return false;
        }
        
        return true;
    }
    
 }

