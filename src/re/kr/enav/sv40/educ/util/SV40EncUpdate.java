package re.kr.enav.sv40.educ.util;

import java.io.StringReader;

import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SV40EncUpdate {
	public static JsonObject getEncUpdate(String gml) {
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		JsonObject jsonRes = new JsonObject();
		
		try
		{
			DocumentBuilder db = dbf.newDocumentBuilder();
			Document doc = db.parse(new InputSource(new StringReader(gml)));
			doc.getDocumentElement().normalize();
			
			//
			String srcMRN = getTextContent(doc, "sourceMRN");
			System.out.println("srcMRN: " + srcMRN);
			
			String dstMRN = getTextContent(doc, "destinationMRN");
			System.out.println("dstMRN: " + dstMRN);
			
			String curTime = getTextContent(doc, "timeOfIssue");
			System.out.println("timeOfIssue: " + curTime);
			
			//
			String shipMRN = getTextContent(doc, "shipMRN");
			System.out.println("shipMRN: " + shipMRN);
			
			String result = getTextContent(doc, "result");
			System.out.println("result: " + result);
			jsonRes.addProperty("result", result);
			
			Element info = getElement(doc, "information");
			String text = getTextContent(info, "text");
			System.out.println("text: " + text);
			jsonRes.addProperty("message", text);
			
			JsonArray jsonPackages = new JsonArray();

			NodeList nodes = getNodeList(doc, "downloadInformation");
			for (int i=0; i<nodes.getLength(); i++)
			{
				JsonObject jsonPackage = new JsonObject();
				
				System.out.println("downloadInformation #" + i);
				Element dinfo = (Element)nodes.item(i);
				String categoryOfENC = getTextContent(dinfo, "categoryOfENC");
				System.out.println("- categoryOfENC: " + categoryOfENC);
				
				String categoryOfService = getTextContent(dinfo, "categoryOfService");
				System.out.println("- categoryOfService: " + categoryOfService);
				
				String hashFunctionValue = getTextContent(dinfo, "hashFunctionValue");
				System.out.println("- hashFunctionValue: " + hashFunctionValue);
				
				// zoneOfENC
				String zoneName = getTextContent(dinfo, "zoneName");
				System.out.println("- zoneName: " + zoneName);
				
				String zoneVersion = getTextContent(dinfo, "zoneVersion");
				System.out.println("- zoneVersion: " + zoneVersion);
				
				// downloadExchangeSet
				String downloadURL = getTextContent(dinfo, "downloadURL");
				System.out.println("- downloadURL: " + downloadURL);
				
				String fileName = getTextContent(dinfo, "fileName");
				System.out.println("- fileName: " + fileName);
				
				String fileSize = getTextContent(dinfo, "fileSize");
				System.out.println("- fileSize: " + fileSize);
				
				// ENCProperty
				String encryption = getTextContent(dinfo, "encryption");
				System.out.println("- encryption: " + encryption);
				
				String typeOfENC = getTextContent(dinfo, "typeOfENC");
				System.out.println("- typeOfENC: " + typeOfENC);
				
				String versionOfENC = getTextContent(dinfo, "versionOfENC");
				System.out.println("- versionOfENC: " + versionOfENC);
				
				String releaseDate = getTextContent(dinfo, "releaseDate");
				System.out.println("- releaseDate: " + releaseDate);
				
				jsonPackage.addProperty("url",downloadURL);
				jsonPackage.addProperty("zone", zoneName);
				jsonPackage.addProperty("zonever", zoneVersion);
				jsonPackage.addProperty("encType", typeOfENC);
				
				// base collection => EN, update collection => ER
				if (categoryOfENC.equals("base collection") || categoryOfENC.equals(""))
					categoryOfENC = "EN";
				else if (categoryOfENC.equals("update collection"))
					categoryOfENC = "ER";
				jsonPackage.addProperty("fileCategory", categoryOfENC);
				jsonPackage.addProperty("fileName", fileName);
				jsonPackage.addProperty("fileSize", Integer.parseInt(fileSize));
				jsonPackage.addProperty("destPath", "");
				jsonPackage.addProperty("version", versionOfENC);
				jsonPackage.addProperty("releaseDate", releaseDate);
				jsonPackage.addProperty("md5", hashFunctionValue);
				
				jsonPackages.add(jsonPackage);
			}
			jsonRes.add("packages", jsonPackages);
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return jsonRes;
	}
	
	//
	private static NodeList getNodeList(Document doc, String tag)
	{
		NodeList nodes = doc.getElementsByTagName(tag);
		return nodes;
	}
	
	//
	private static Element getElement(Document doc, String tag)
	{
		NodeList nodes = doc.getElementsByTagName(tag);
		
		for (int i=0; i<nodes.getLength(); i++)
		{
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE)
				return (Element)node;
		}
		
		return null;
	}
	
	//
	private static Element getChildElement(Element el, String tag)
	{
		NodeList nodes = el.getElementsByTagName(tag);
		
		for (int i=0; i<nodes.getLength(); i++)
		{
			Node node = nodes.item(i);
			if (node.getNodeType() == Node.ELEMENT_NODE)
				return (Element)node;
		}
		
		return null;
	}
	//
	private static String getTextContent(Document doc, String tag)
	{
		NodeList nodes = doc.getElementsByTagName(tag);
		if (nodes.getLength() <= 0)
			return null;
		
		return nodes.item(0).getTextContent();
	}
	
	//
	private static String getTextContent(Element el, String tag)
	{
		NodeList nodes = el.getElementsByTagName(tag);
		if (nodes.getLength() <= 0)
			return null;
		
		return nodes.item(0).getTextContent();
	}	
}
