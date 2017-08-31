/* -------------------------------------------------------- */
/** Common Utility for S101 Update Status Report
File name : SV40EDUUtil.java 
Author : Sang Whan Oh(sang@kjeng.kr)
Creation Date : 2017-04-05
Version : v0.1
Rev. history :
Modifier : 
*/
/* -------------------------------------------------------- */
package re.kr.enav.sv40.educ.util;
import java.io.FileReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

/**
 * @brief EDUC Update Report를 관리 클래스
 * @details S-101 카탈로그 XML파일을 읽어 JSON 양식의 업데이트 레포트를 관리하는 클래스
 * @author Sang Whan Oh
 * @date 2017.04.03
 * @version 0.0.1
 *
 */
public class SV40S101UpdateStatusReport {
	/**
	 * @brief class for compare function
	 * @details compare first element of JsonArray as string
	 */			
	class ENCJsonComparator implements Comparator<JsonArray> {
		@Override
		public int compare(JsonArray o1, JsonArray o2) {
		    String s1 = o1.get(0).getAsString();
		    String s2 = o2.get(0).getAsString();
		    return s1.compareTo(s2);
		}
	}
	/**
	 * @brief class for name space query of xpath
	 * @details add name space for S100
	 */			
	public class SimpleNamespaceContext implements NamespaceContext {

	    private final Map<String, String> PREF_MAP = new HashMap<String, String>();

	    public SimpleNamespaceContext(final Map<String, String> prefMap) {
	        PREF_MAP.putAll(prefMap);       
	    }

	    public String getNamespaceURI(String prefix) {
	        return PREF_MAP.get(prefix);
	    }

	    public String getPrefix(String uri) {
	        throw new UnsupportedOperationException();
	    }

	    public Iterator<String> getPrefixes(String uri) {
	        throw new UnsupportedOperationException();
	    }

	}
	
	private String m_pathOfReport;		/**< path of report */
	private JsonObject m_jsonReport;	/**< store of report */
	public SV40S101UpdateStatusReport(String path) {
		m_pathOfReport = path;
		try {
		m_jsonReport = SV40EDUUtil.readJson(m_pathOfReport);
		} catch (Exception e) {
			m_jsonReport = createReport();
		}
	}
	
	/**
	 * @brief parse ENC Catalog file and update report
	 * @details parse ENC Catalog file and update report
	 * @param pathOfCatalog file path of catalog
	 * @throws Exception 
	 */			
	public boolean updateReportFromENCUpdateFile(JsonObject jsonENCUpdateFile) {
		boolean bRet = true;
		
		if (!m_jsonReport.has("zones")) {
			m_jsonReport.add("zones",  new JsonArray());
		}
		JsonArray jsonZones = m_jsonReport.get("zones").getAsJsonArray();
		String fileZone = jsonENCUpdateFile.get("zone").getAsString();
		String fileVersion = jsonENCUpdateFile.get("version").getAsString();
		String fileReleaseDate = jsonENCUpdateFile.get("releaseDate").getAsString();
		
		JsonObject jsonTargetZone = null;
		for (int i=0;i<jsonZones.size(); i++) {
			JsonObject jsonZone = jsonZones.get(i).getAsJsonObject();
			String localZone = jsonZone.get("zone").getAsString();
			
			if (fileZone.equals(localZone)) {
				jsonTargetZone = jsonZone;
				break;
			}
		}
		
		// in case new zone
		if (jsonTargetZone==null) {
			jsonTargetZone = new JsonObject();
			jsonZones.add(jsonTargetZone);
		}
		
		jsonTargetZone.addProperty("zone", fileZone);
		jsonTargetZone.addProperty("version", fileVersion);
		jsonTargetZone.addProperty("releaseDate", fileReleaseDate);
	
		return bRet;
	}
	/**
	 * @brief parse ENC Catalog file and update report
	 * @details parse ENC Catalog file and update report
	 * @param pathOfCatalog file path of catalog
	 * @throws Exception 
	 */			
	public boolean updateReportFromCatalog(String pathOfCatalog) {
		boolean bRet = true;
		JsonObject jsonMeta = null;
		try {
			jsonMeta = parseMetadata(pathOfCatalog);
		} catch (Exception e) {
			e.printStackTrace();
		}
		
		// if empty, create new one
		if (m_jsonReport == null) {	// create new one
			m_jsonReport = createReport();
		}
		// obtain array
		JsonArray encLocal = m_jsonReport.get("encs").getAsJsonArray();
		JsonArray encEDUS = jsonMeta.get("encs").getAsJsonArray();
		// compare stored report and update
		for (int i=0;i<encEDUS.size();i++) {
			JsonArray cellEDUS = (JsonArray)encEDUS.get(i);
			String nameEDUS = cellEDUS.get(0).getAsString();
			JsonArray cellTarget = null;
			for (int j=0; j<encLocal.size();j++) {
				JsonArray cellLocal = (JsonArray)encLocal.get(j);
				String nameLocal= cellLocal.get(0).getAsString();
				if (nameEDUS.equals(nameLocal)) {
					cellTarget = cellLocal;
					break;
				}
			}
			if (cellTarget == null) {
				encLocal.add(cellEDUS); // add new
			} else {
				for (int k=1;k<cellEDUS.size();k++) {
					cellTarget.set(k, cellEDUS.get(k));
				}
			}
		}
		// sort cell
		ArrayList<JsonArray> list = new ArrayList<>();
		for (int i = encLocal.size()-1; i > -1; i--) {
            list.add((JsonArray) encLocal.get(i));
            encLocal.remove(i);
        }
		Collections.sort(list, new ENCJsonComparator());
		encLocal = new JsonArray();
		for (int i = 0; i < list.size(); i++) {
			encLocal.add(list.get(i));
        }
		m_jsonReport.add("encs", encLocal);
		return bRet;
	}
	/**
	 * @brief parse S-101 exchange catalog xml file and build json object
	 * @details parse S-101 exchange catalog xml file and build json object
	 * @return JsonObject of parsed
	 * @throws Exception 
	 */			
	public JsonObject parseMetadata(String pathOfCatalog) throws Exception {
		JsonObject jsonMeta = new JsonObject();
		
		JsonArray array = new JsonArray();
		
		DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
		InputSource is = new InputSource(new FileReader(pathOfCatalog));
		factory.setNamespaceAware(true);
		Document document =  factory.newDocumentBuilder().parse(is);

		XPath xpath = XPathFactory.newInstance().newXPath();
		HashMap<String, String> prefMap = new HashMap<String, String>() {{
			put("S100XC", "http://www.iho.int/s100/xc");
			
			put("gml", "http://www.opengis.net/gml/3.2");
			put("gco", "http://www.isotc211.org/2005/gco");
			put("gmd", "http://www.isotc211.org/2005/gmd");
			put("gts", "http://www.isotc211.org/2005/gts");
			put("gsr", "http://www.isotc211.org/2005/gsr");
			put("gss", "http://www.isotc211.org/2005/gss");
			put("gmx", "http://www.isotc211.org/2005/gmx");
		}};
		SimpleNamespaceContext namespaces = new SimpleNamespaceContext(prefMap);
		xpath.setNamespaceContext(namespaces);

		NodeList metas = (NodeList)xpath.evaluate("//S100XC:S101_DatasetDiscoveryMetadata", document, XPathConstants.NODESET);
		for( int i=0; i<metas.getLength(); i++ ){
			
			NodeList childNodes = metas.item(i).getChildNodes();
			String name = null, edtn=null, udtn=null, isdt=null;
			for (int j=0;j<childNodes.getLength(); j++) {
				Node node = childNodes.item(j);
				switch(node.getNodeName()) {
				case "S100XC:fileName":
					name = node.getTextContent();
					name = name.substring(0, name.indexOf(".")); // except .
					break;
				case "S100XC:editionNumber":
					edtn = node.getTextContent();
					break;
				case "S100XC:updateNumber":
					udtn = node.getTextContent();
					break;
				case "S100XC:issueDate":
					isdt = node.getTextContent();
					break;
				}
			}
			JsonArray subArray = null;
			if (name!=null && edtn!=null && udtn!=null && isdt!=null) {
				subArray = new JsonArray();
				subArray.add(name);
				subArray.add(Integer.parseInt(edtn));
				subArray.add(Integer.parseInt(udtn));
				subArray.add(isdt);
			}
			
			if (subArray!= null) {
				array.add(subArray);				
			}
		}
		
		jsonMeta.add("encs", array);
		
		return jsonMeta;
	}
	/**
	 * @brief create empty report
	 * @details create empty repor
	 * @return JsonObject of report
	 * @throws Exception 
	 */		
	private JsonObject createReport() {
		JsonObject report = new JsonObject();
		report.add("encs", new JsonArray());
		return report;
	}
	/**
	 * @brief save report
	 * @details save json report file
	 * @return boolean of success
	 */	
	public boolean save() {
		return SV40EDUUtil.writeJson(m_jsonReport, m_pathOfReport);
	}
	/**
	 * @brief convert string
	 * @details convert report to string
	 * @return boolean of success
	 */	
	public String toString() {
		return m_jsonReport.toString();
	}
	/**
	 * @brief convert report to JsonObject
	 * @details convert report to JsonObject
	 * @return JsonObject
	 */		
	public JsonObject toJson() {
		return m_jsonReport;
	}
}
