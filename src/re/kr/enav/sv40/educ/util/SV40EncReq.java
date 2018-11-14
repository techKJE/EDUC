package re.kr.enav.sv40.educ.util;

import java.io.StringWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.transform.OutputKeys;
import javax.xml.transform.Transformer;
import javax.xml.transform.TransformerFactory;
import javax.xml.transform.dom.DOMSource;
import javax.xml.transform.stream.StreamResult;

import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

public class SV40EncReq {
	public static String getEncReq(String category, JsonObject config, SV40S101UpdateStatusReport report)
	{
		String gml="";
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try
		{
			Document doc = dbf.newDocumentBuilder().newDocument();
			Node root = getRoot(doc);
			doc.appendChild(root);
			
			JsonObject jsonZone = SV40EDUUtil.readJson("Res\\zone.json");
			
			// imember
			String srcMRN = SV40EDUUtil.queryJsonValueToString(config, "cloud.srcMRN");
			String dstMRN = SV40EDUUtil.queryJsonValueToString(config, "cloud.destServiceMRN");
			String curTime = getTime();
			root.appendChild(getImember(doc, srcMRN, dstMRN, curTime));
			
			//
			Node member = getMember(doc);
			root.appendChild(member);
			
			Node request = getRequest(doc, (Element)member);
			member.appendChild(request);
			
			request.appendChild(getIdentification(doc));
			request.appendChild(getTextContent(doc, "shipMRN", srcMRN));
			
			//
//			String categoryOfENC = category.equals("EN")? "base collection":"update collection";
//			request.appendChild(getTextContent(doc, "categoryOfENC", categoryOfENC));
			
			request.appendChild(getTextContent(doc, "categoryOfENC", category));
			
			//
			request.appendChild(getTextContent(doc, "categoryOfService", "ENC"));
			
			//
			String license = SV40EDUUtil.queryJsonValueToString(config, "enc.license");
			String maker = SV40EDUUtil.queryJsonValueToString(config, "ecs.maker");
			String serial = SV40EDUUtil.queryJsonValueToString(config, "ecs.serial");
			String model = SV40EDUUtil.queryJsonValueToString(config, "ecs.model");
			request.appendChild(getDevicePropertyForENC(doc, license, maker, serial, model));
			
			//
			String latitude = SV40EDUUtil.queryJsonValueToString(config, "ecs.position.lat");
			String longitude = SV40EDUUtil.queryJsonValueToString(config, "ecs.position.lon");	
			request.appendChild(getVesselPosition(doc, latitude, longitude));
			
			String localZone = SV40EDUUtil.queryJsonValueToString(config, "enc.zone");
			String version = "", releaseDate = "", zonever = "", zone="";
			
			// base zones
			JsonObject jsonLocalZone = report.getBaseZone(localZone);
			if (jsonLocalZone == null) {
				request.appendChild(getENCRequest(doc, "base zones", localZone, zonever, version, releaseDate));
			} else {
				String curzonever = SV40EDUUtil.queryJsonValueToString(jsonLocalZone, "zonever");
				version = SV40EDUUtil.queryJsonValueToString(jsonLocalZone, "version");
				releaseDate = SV40EDUUtil.queryJsonValueToString(jsonLocalZone, "releaseDate");
				request.appendChild(getENCRequest(doc, "base zones", localZone, curzonever, version, releaseDate));
			}
			
//			JsonArray jsonZones = report.getBaseZones();
//			if (jsonZones == null) {
//				zonever = SV40EDUUtil.getZoneVer(jsonZone, localZone);
//				request.appendChild(getENCRequest(doc, "base zones", localZone, zonever, version, releaseDate));
//			} else {
//				for (int i=0; i<jsonZones.size(); i++) {
//					JsonObject curZone = jsonZones.get(i).getAsJsonObject();
//					
//					zone = curZone.get("zone").getAsString();
//					zonever = curZone.get("zonever").getAsString();
//					version = curZone.get("version").getAsString();
//					releaseDate = curZone.get("releaseDate").getAsString();
//					request.appendChild(getENCRequest(doc, "base zones", zone, zonever, version, releaseDate));
//				}
//			}

			version = "";
			releaseDate = "";
			
			// update zones
			jsonLocalZone = report.getUpdateZone(localZone);
			if (jsonLocalZone == null) {
				request.appendChild(getENCRequest(doc, "update zones", localZone, zonever, version, releaseDate));
			} else {
				String curzonever = SV40EDUUtil.queryJsonValueToString(jsonLocalZone, "zonever");
				version = SV40EDUUtil.queryJsonValueToString(jsonLocalZone, "version");
				releaseDate = SV40EDUUtil.queryJsonValueToString(jsonLocalZone, "releaseDate");
				request.appendChild(getENCRequest(doc, "update zones", localZone, zonever, version, releaseDate));
			}

//			jsonZones = report.getUpdateZones();
//			if (jsonZones == null) {
//				zonever = SV40EDUUtil.getZoneVer(jsonZone, localZone);
//				request.appendChild(getENCRequest(doc, "update zones", localZone, zonever, version, releaseDate));
//			} else {
//				for (int i=0; i<jsonZones.size(); i++) {
//					JsonObject curZone = jsonZones.get(i).getAsJsonObject();
//					
//					zone = curZone.get("zone").getAsString();
//					zonever = curZone.get("zonever").getAsString();
//					version = curZone.get("version").getAsString();
//					releaseDate = curZone.get("releaseDate").getAsString();
//					request.appendChild(getENCRequest(doc, "update zones", zone, zonever, version, releaseDate));
//				}
//			}
			
			//
			Transformer xfer = TransformerFactory.newInstance().newTransformer();
			xfer.setOutputProperty(OutputKeys.INDENT, "yes");
			xfer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			
			xfer.transform(source, result);
			gml = writer.getBuffer().toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return gml;
	}

	//
	private static String getTime()
	{
		SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss", Locale.KOREA);
		return sdf.format(new Date());
	}
	
	//
	private static Node getRoot(Document doc)
	{
		Element el = doc.createElementNS("http://www.iho.int/ENCPropertyForNonSOLAS/gml/1.0", "ENCPropertyForNonSOLAS:DataSet");
		el.setAttribute("xmlns:xsi", "http://www.w3.org/2001/XMLSchema-instance");
		el.setAttribute("xmlns:gml", "http://www.opengis.net/gml/3.2");
		el.setAttribute("xmlns:S100", "http://www.iho.int/s100gml/1.0");
		el.setAttribute("xmlns:xlink", "http://www.w3.org/1999/xlink");
		el.setAttribute("gml:id", "ENCPropertyForNonSOLAS");
		return el;
	}
	
	//
	private static Node getImember(Document doc, String srcMRN, String dstMRN, String timeOfIssue)
	{
		Element el = doc.createElement("imember");
		el.appendChild(getServiceIdentification(doc, srcMRN, dstMRN, timeOfIssue));
		
		return el;
	}
	
	private static Node getServiceIdentification(Document doc, String srcMRN, String destMRN, String timeOfIssue)
	{
		Element el = doc.createElement("ENCPropertyForNonSOLAS:ServiceIdentification");
		el.setAttribute("gml:id", "IM.0001");
		
		String names[] = {"sourceMRN", "destinationMRN", "timeOfIssue"};
		for (int i=0; i<names.length; i++)
		{
			Element child = doc.createElement(names[i]);
			if (names[i].equals("sourceMRN"))
				child.setTextContent(srcMRN);
			else if (names[i].equals("destinationMRN"))
				child.setTextContent(destMRN);
			else if (names[i].equals("timeOfIssue"))
				child.setTextContent(timeOfIssue);
			
			el.appendChild(child);
		}
		
		return el;
	}
	
	//
	private static Node getMember(Document doc)
	{
		Element el = doc.createElement("member");
		return el;
	}
	
	private static Node getRequest(Document doc, Element parent)
	{
		Element el = doc.createElement("ENCPropertyForNonSOLAS:Request");
		el.setAttribute("gml:id", "M.0001");
		
		parent.appendChild(el);
		return el;
	}
	
	private static Node getIdentification(Document doc)
	{
		Element el = doc.createElement("identification");
		el.setAttribute("gml:id", "a.0001");
		el.setAttribute("xlink:href", "#IM.0001");
		el.setAttribute("xlink:role", "serviceIdentification");
		
		return el;
	}
	
	//
	private static Node getTextContent(Document doc, String name, String value)
	{
		Element el = doc.createElement(name);
		el.setTextContent(value);

		return el;
	}

	//
	private static Node getDevicePropertyForENC(Document doc, String license, String maker, String serial, String model)
	{
		Element el = doc.createElement("devicePropertyForENC");
		
		String names[] = {"deviceLicense", "deviceMaker", "deviceSerialNumber", "deviceModel"};
		for (int i=0; i<names.length; i++)
		{
			Element child = doc.createElement(names[i]);
			if (names[i].equals("deviceLicense"))
				child.setTextContent(license);
			else if (names[i].equals("deviceMaker"))
				child.setTextContent(maker);
			else if (names[i].equals("deviceSerialNumber"))
				child.setTextContent(serial);
			else if (names[i].equals("deviceModel"))
				child.setTextContent(model);
			
			el.appendChild(child);
		}
		
		return el;
	}
	
	private static Node getVesselPosition(Document doc, String latitude, String longitude)
	{
		Element el = doc.createElement("vesselPosition");
		
		String names[] = {"vesselLatitude", "vesselLongitude"};
		for (int i=0; i<names.length; i++)
		{
			Element child = doc.createElement(names[i]);
			if (names[i].equals("vesselLatitude"))
				child.setTextContent(latitude);
			else if (names[i].equals("vesselLongitude"))
				child.setTextContent(longitude);
			
			el.appendChild(child);
		}

		return el;
	}
	
	//
	private static Node getENCRequest(Document doc, String type, String zone, String zonever, String version, String releaseDate)
	{
		Element el = doc.createElement("ENCRequest");
		
		// base zones, update zones
		el.appendChild(getTextContent(doc, "typeOfZoneData", type));
		el.appendChild(getZoneOfENC(doc, zone, zonever));
		el.appendChild(getENCProperty(doc, version, releaseDate));
		
		return el;
	}
	
	private static Node getZoneOfENC(Document doc, String zone, String zonever)
	{
		Element el = doc.createElement("zoneOfENC");
		
		String names[] = {"zoneName", "zoneVersion"};
		for (int i=0; i<names.length; i++)
		{
			Element child = doc.createElement(names[i]);
			if (names[i].equals("zoneName"))
				child.setTextContent(zone);
			else if (names[i].equals("zoneVersion"))
				child.setTextContent(zonever);
			
			el.appendChild(child);
		}
		
		return el;
	}
	
	private static Node getENCProperty(Document doc, String version, String releaseDate)
	{
		Element el = doc.createElement("ENCProperty");
		
		String names[] = {"versionOfENC", "releaseDate"};
		for (int i=0; i<names.length; i++)
		{
			Element child = doc.createElement(names[i]);
			if (names[i].equals("versionOfENC"))
				child.setTextContent(version);
			else if (names[i].equals("releaseDate"))
				child.setTextContent(releaseDate);
			
			el.appendChild(child);
		}
		
		return el;
	}
}
