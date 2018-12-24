/* -------------------------------------------------------- */
/** Common Utility for EDUC and EDUS
File name : SV40EDUUtil.java 
Author : Sang Whan Oh(sang@kjeng.kr)
Creation Date : 2017-04-05
Version : v0.1
Rev. history :
Modifier : 
*/
/* -------------------------------------------------------- */
package re.kr.enav.sv40.educ.util;

import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.StringReader;
import java.net.InetAddress;
import java.net.NetworkInterface;
import java.util.Enumeration;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.stream.JsonReader;

/**
 * @brief 유틸리티 클래스
 * @details Json Object를 쉽게 사용하도록 간편 기능 제공 유틸리티
 * @author Sang Whan Oh
 * @date 2017.04.03
 * @version 0.0.1
 *
 */
public class SV40EDUUtil {
	static public final int RETRY_MAXCOUNT = 3;
	static public final int RETRY_TIMEWAIT = 10000;
	static public boolean LOCALNETWORK_STATE = true;
	
	/**
	 * @brief parse query at json and return found json
	 * @details parse query for json, and return string 
	 */			
	static public String queryJsonValueToString(JsonObject json, String query) {
		String ret="";
		
		JsonElement el = queryJsonElement(json, query);
		if (el != null) ret = el.getAsString();
		
		return ret;
	}
	/**
	 * @brief parse query at json and return found json
	 * @details parse query and return json element
	 */			
	static public JsonElement queryJsonElement(JsonObject json, String query) {
		JsonElement ret= null;
		
		String items[] = query.split("\\.");
		JsonObject jsonFound = json;
		for (int i=0;i<items.length-1;i++) {
			jsonFound = jsonFound.getAsJsonObject(items[i]);
			if (jsonFound == null) return ret;
		}
		
		if (jsonFound != null) {
			ret = jsonFound.get(items[items.length-1]);
		}
		
		return ret;
	}	
	/**
	 * @brief set json property by query and set value as string 
	 * @details set json property by query and set value as string 
	 * @param json target to find jsonproperty
	 * @param query query string to find with . separator
	 * @param value value for set
	 */			
	static public boolean setJsonPropertyByQuery(JsonObject json, String query, String value) {
		boolean ret = false;
		
		String items[] = query.split("\\.");
		JsonObject jsonFound = json;
		for (int i=0;i<items.length-1;i++) {
			jsonFound = jsonFound.getAsJsonObject(items[i]);
			if (jsonFound == null) return ret;
		}
		
		if (jsonFound != null) {
			jsonFound.addProperty(items[items.length-1], value);
			ret = true;
		}		
		
		return ret;
	}
	/**
	 * @brief read json file and return JsonObject 
	 * @details read json file and return JsonObject  
	 * @param path path of json file
	 * @return parsed JsonObject
	 */		
	static public JsonObject getJsonObjectFromFile(String path) {
		JsonObject json = null;
		JsonParser parser = new JsonParser();
		FileInputStream in;
		try {
			in = new FileInputStream(path);
			JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
			json = (JsonObject)parser.parse(reader);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return json;
	}
	/**
	 * @brief read json file and return JsonObject 
	 * @details read json file and return JsonObject  
	 * @param path path of json file
	 * @return parsed JsonObject
	 */		
	static public JsonArray getJsonArrayFromFile(String path) {
		JsonArray json = null;
		JsonParser parser = new JsonParser();
		FileInputStream in;
		try {
			in = new FileInputStream(path);
			JsonReader reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
			json = (JsonArray)parser.parse(reader);
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		return json;
	}	
	/**
	 * @brief read json file
	 * @details read json file and return JsonObject
	 * param path path of json file
	 */		
	static public JsonObject readJson(String path) throws Exception {
		JsonObject json = null;

		JsonParser parser = new JsonParser();
		FileInputStream in = null;
		
		JsonReader reader = null;
		in = new FileInputStream(path);
		reader = new JsonReader(new InputStreamReader(in, "UTF-8"));
		json= (JsonObject)parser.parse(reader);
			
		if (in != null) {
			in.close();
		}		
		
		return json;
	}	
	/**
	 * @brief write json to file
	 * @details write json
	 * @param json target to write
	 * @param path file path to write
	 */		
	static public boolean writeJson(JsonObject json, String path) {
		boolean bSuccess = true;

		Gson gson = new GsonBuilder().setPrettyPrinting().create();
		String jsonString = gson.toJson(json);
		
		try {
		 FileWriter writer = new FileWriter(path);
		 writer.write(jsonString);
		 writer.close();
		} catch (IOException e) {
			bSuccess = false;
			e.printStackTrace();
		}
		
		return bSuccess;
	}
	
	/**
	 * @brief get local address
	 * @details get local address
	 */			
	static public String getLocalAddr() {
		try {
			
			for (Enumeration<NetworkInterface> en = NetworkInterface.getNetworkInterfaces(); en.hasMoreElements();) {
				NetworkInterface inf = en.nextElement();
				for (Enumeration<InetAddress> addr = inf.getInetAddresses(); addr.hasMoreElements();) {
					InetAddress iaddr = addr.nextElement();
					// 192.168.x.x : virtualbox ip 처리가 안됨
					if (!iaddr.isLoopbackAddress() && !iaddr.isLinkLocalAddress() && iaddr.isSiteLocalAddress())
						return iaddr.getHostAddress().toString();
				}
			}
		} catch (Exception e) {
		}
		
		return null;
	}
	
	/**
	 * @brief ping check
	 * @details ping check
	 */			
	static public boolean ping(String host, int timeout) {
		try {
			InetAddress target = InetAddress.getByName(host);
			return target.isReachable(timeout);
		} catch (Exception e) {
			return false;
		}
	}
	
	static public String getMD5(String filename) {
		StringBuffer sb = new StringBuffer();
		
		try {
			MessageDigest md = MessageDigest.getInstance("MD5");
		    FileInputStream fis = new FileInputStream(filename);
		 
		    byte[] dataBytes = new byte[1024];
		 
		    int nread = 0;
		    while ((nread = fis.read(dataBytes)) != -1) {
		        md.update(dataBytes, 0, nread);
		    };
		    
		    fis.close();
		    
		    byte[] mdbytes = md.digest();
		    for (int i = 0; i < mdbytes.length; i++) {
		        sb.append(Integer.toString((mdbytes[i] & 0xff) + 0x100, 16).substring(1));
		    }
		    System.out.println("Digest(in hex format):: " + sb.toString());
		} catch (Exception e) {
		}
		
		return sb.toString();
	}
	
	static public String getZoneVer(JsonObject jsonEncZone, String zone) {
		String ver="";
		
		if (!jsonEncZone.has("zones")) {
			return ver;
		}
		
		JsonArray jsonZones = jsonEncZone.get("zones").getAsJsonArray();
		for (int i=0;i<jsonZones.size(); i++) {
			JsonObject jsonZone = jsonZones.get(i).getAsJsonObject();
			String localZone = jsonZone.get("name").getAsString();
			
			if (zone.equals(localZone)) {
				ver = jsonZone.get("ver").getAsString();
				break;
			}
		}
	
		return ver;
	}
	
	//
	static public String getCategoryOfService(String gml) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		String categoryOfService = "";
		
		try
		{
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(new StringReader(gml)));
			doc.getDocumentElement().normalize();
			
			NodeList nodes = doc.getElementsByTagName("categoryOfService");
			if (nodes.getLength() > 0)
				categoryOfService = nodes.item(0).getTextContent();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return categoryOfService;
	}
	
	//
	static public String getCategoryOfENC(String gml) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		String categoryOfENC = "";
		
		try
		{
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(new StringReader(gml)));
			doc.getDocumentElement().normalize();
			
			NodeList nodes = doc.getElementsByTagName("categoryOfENC");
			if (nodes.getLength() > 0)
				categoryOfENC = nodes.item(0).getTextContent();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return categoryOfENC;
	}
}
