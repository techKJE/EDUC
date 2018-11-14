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

public class SV40EncZoneRes {
	public static JsonObject getEncZoneRes(String gml)
	{
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
			
			String categoryOfENC = getTextContent(doc, "categoryOfENC");
			System.out.println("- categoryOfENC: " + categoryOfENC);
			
			String categoryOfService = getTextContent(doc, "categoryOfService");
			System.out.println("- categoryOfService: " + categoryOfService);
			
			JsonArray jsonZones = new JsonArray();
			
			NodeList nodes = getNodeList(doc, "zoneInformation");
			for (int i=0; i<nodes.getLength(); i++)
			{
				JsonObject jsonZone = new JsonObject();
				
				System.out.println("zoneInformation #" + i);
				Element zinfo = (Element)nodes.item(i);
				
				String zoneName = getTextContent(zinfo, "zoneName");
				System.out.println("- zoneName: " + zoneName);
				
				String zoneVersion = getTextContent(zinfo, "zoneVersion");
				System.out.println("- zoneVersion: " + zoneVersion);
				
				// boundary
				String northLatitude = getTextContent(zinfo, "northLatitude");
				System.out.println("- northLatitude: " + northLatitude);
				
				String southLatitude = getTextContent(zinfo, "southLatitude");
				System.out.println("- southLatitude: " + southLatitude);
				
				String eastLongitude = getTextContent(zinfo, "eastLongitude");
				System.out.println("- eastLongitude: " + eastLongitude);
				
				String westLongitude = getTextContent(zinfo, "westLongitude");
				System.out.println("- westLongitude: " + westLongitude);
				
				jsonZone.addProperty("name", zoneName);
				jsonZone.addProperty("ver", zoneVersion);
				jsonZone.addProperty("slat", southLatitude);
				jsonZone.addProperty("wlon", westLongitude);
				jsonZone.addProperty("nlat", northLatitude);
				jsonZone.addProperty("elon", eastLongitude);
				
				jsonZones.add(jsonZone);
			}
			jsonRes.add("zones", jsonZones);
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
