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

import com.google.gson.JsonObject;

public class SV40EncZoneReq {
	public static String getEncZoneReq(JsonObject config)
	{
		String xml="";
		DocumentBuilderFactory dbf = DocumentBuilderFactory.newInstance();
		
		try
		{
			Document doc = dbf.newDocumentBuilder().newDocument();
			Node root = getRoot(doc);
			doc.appendChild(root);
			
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
			request.appendChild(getTextContent(doc, "categoryOfService", "Zone Information"));
			
			//
			String license = SV40EDUUtil.queryJsonValueToString(config, "enc.license");
			request.appendChild(getDevicePropertyForENC(doc, license));
			
			//
			Transformer xfer = TransformerFactory.newInstance().newTransformer();
			xfer.setOutputProperty(OutputKeys.INDENT, "yes");
			xfer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2");
			DOMSource source = new DOMSource(doc);
			StringWriter writer = new StringWriter();
			StreamResult result = new StreamResult(writer);
			
			xfer.transform(source, result);
			xml = writer.getBuffer().toString();
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
		
		return xml;
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
	private static Node getDevicePropertyForENC(Document doc, String license)
	{
		Element el = doc.createElement("devicePropertyForENC");
		
		String names[] = {"deviceLicense"};
		for (int i=0; i<names.length; i++)
		{
			Element child = doc.createElement(names[i]);
			if (names[i].equals("deviceLicense"))
				child.setTextContent(license);
			
			el.appendChild(child);
		}
		
		return el;
	}
}
