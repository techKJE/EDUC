/* -------------------------------------------------------- */
/** Maritime Cloud Server for EDUC
File name : SV40EDUCMCServer.java 
Author : Sang Whan Oh(sang@kjeng.kr)
Creation Date : 2017-04-17
Version : v0.1
Rev. history :
Modifier : 
*/
/* -------------------------------------------------------- */
package re.kr.enav.sv40.educ.mc;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import kr.ac.kaist.mms_client.MMSClientHandler;
import kr.ac.kaist.mms_client.MMSConfiguration;
import re.kr.enav.sv40.educ.util.SV40EDUUtil;
import re.kr.enav.sv40.educ.util.SV40EncUpdate;
import re.kr.enav.sv40.educ.util.SV40EncZoneRes;
import re.kr.enav.sv40.educ.util.SV40S101UpdateStatusReport;
/**
 * @brief EDUC 시험을 위한 EDUS 목업용 서버 클래스
 * @details eNav 운영체제의 EDUS를 대신하여 MC 테스트용 서버 클래스
 * @author Sang Whan Oh
 * @date 2017.04.03
 * @version 0.0.1
 *
 */
public class SV40EDUCMCServer {
	/**
	 * @brief launch MC Demo Server
	 * @details  response for request to update
	 * @param args, first is mrn and second is port
	 */	
	public static int m_retryCnt;
	
	private static final String MMS_KEY = "-mms";
	private static final String MRN_KEY = "-mrn";
	private static final String PORT_KEY = "-port";
	
	public static void main(String args[]) throws Exception{
		MMSConfiguration.MMS_URL="www.mms-kaist.com:8088";
		String myMRN = "urn:mrn:smart-navi:service:sv40";
		int port = 8088;
		
		for(int i=0; i<args.length; i+=2)
	    {
	        String key = args[i];
	        String value = args[i+1];

	        switch (key)
	        {
	            case MMS_KEY : MMSConfiguration.MMS_URL = value;
	            break;
	            case MRN_KEY : myMRN = value;
	            break;
	            case PORT_KEY : port = Integer.parseInt(value); 
	            break;
	        }
	    }
		
		MMSClientHandler server = new MMSClientHandler(myMRN);
		MMSClientHandler sender = new MMSClientHandler(myMRN);	//MCC 목업으로 직접 회신 하지 못하기에 필요함
		sender.setSender(new MMSClientHandler.ResponseCallback() {
			//Response Callback from the request message
			@Override
			public void callbackMethod(Map<String, List<String>> headerField, String message) {
				// TODO Auto-generated method stub
				System.out.println(message);
			}
		});
		
		//server.setServerPort(port, "/edus", new MMSClientHandler.RequestCallback() {
		server.setServerPort(port, new MMSClientHandler.RequestCallback() {
			@Override
			public int setResponseCode() {
				return 200;
			}
			
			@Override
			public String respondToClient(Map<String,List<String>> headerField, String message) {
				String response = "error";
				try {
					List<String> values = (List<String>)headerField.get("srcMRN");
					String destMRN = values.get(0);
					String responseMessage = SV40EDUCMCServer.getResponse(message);
					sender.sendPostMsg(destMRN, responseMessage);
										
					response = "OK";
					
				} catch (Exception e) {
					e.printStackTrace();
				}
					
				return response;
			}

			@Override
			public Map<String, List<String>> setResponseHeader() {
				return null;
			}
		});
		
		System.out.println("Now starting EDUS MMS Mockup server:"+myMRN);
	}
	/**
	 * @brief demonstrate file download 
	 * @details mock up file download with requested information
	 * @param message to process download result
	 */		
	public static String getResponse(String message) {
		String response="";
		boolean useS10x=true;
		
		if (useS10x == true)
		{
			String categoryOfService = SV40EDUUtil.getCategoryOfService(message);
			if (categoryOfService.equals("Zone Information"))
			{
				try {
					response = new String(Files.readAllBytes(Paths.get("Res"+File.separator+"response_zone.gml")));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
			else if (categoryOfService.equals("ENC"))
			{
				String categoryOfENC = SV40EDUUtil.getCategoryOfENC(message);
				try {
					if (categoryOfENC.equals("EN"))
						response = new String(Files.readAllBytes(Paths.get("Res"+File.separator+"download_en_sample.gml")));
					else
						response = new String(Files.readAllBytes(Paths.get("Res"+File.separator+"download_er_sample.gml")));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}

			return response;
		}

		JsonParser parser = new JsonParser();
		//JsonObject jsonRequest = (JsonObject)parser.parse(message);
		JsonObject jsonObj = (JsonObject)parser.parse(message);
	
		JsonElement elZone = jsonObj.get("EncZoneReq");
		if (elZone != null) {
			try {
				response = new String(Files.readAllBytes(Paths.get("Res"+File.separator+"response_zone.json")));
			} catch (IOException e) {
				e.printStackTrace();
			}
			
			return response;
		}
		
		JsonArray jsonReq = jsonObj.get("EncReq").getAsJsonArray();
		JsonObject jsonMsg = jsonReq.get(0).getAsJsonObject();
		JsonObject jsonRequest = (JsonObject)parser.parse(jsonMsg.get("message").getAsString());
		
		String license = SV40EDUUtil.queryJsonValueToString(jsonRequest, "license");
		String category = SV40EDUUtil.queryJsonValueToString(jsonRequest, "category");
		String latitude = SV40EDUUtil.queryJsonValueToString(jsonRequest, "latitude");
		String longitude = SV40EDUUtil.queryJsonValueToString(jsonRequest, "longitude");

		// zone 정보가 배열로 입력되지만, 목업이기에 값 유효성만 확인 하고.. 준비된 것을 돌려준다.
		String zones[] = SV40EDUUtil.queryJsonValueToString(jsonRequest, "basezone").split(",");
		
		// 입력 데이타  validation check
		if (license.isEmpty() || latitude.isEmpty() || longitude.isEmpty() || zones.length == 0) {
			try {
				if (license.isEmpty()) {
					response = new String(Files.readAllBytes(Paths.get("Res"+File.separator+"download_license_error.json")));
				} else if (latitude.isEmpty() || longitude.isEmpty()) {
					response = new String(Files.readAllBytes(Paths.get("Res"+File.separator+"download_location_error.json")));
				} else if (zones.length == 0) {
					response = new String(Files.readAllBytes(Paths.get("Res"+File.separator+"download_zone_error.json")));
				}
			} catch (IOException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			
			return response;
		}
		
		try {
			if (category.equals("EN")) {
				//response = new String(Files.readAllBytes(Paths.get("Res"+File.separator+"download_en_ftp_sample.json")));
				//String returnFile = "Res"+File.separator+"download_en_ftp_sample.json";
				String returnFile = "Res"+File.separator+"download_en_sample.json";
				JsonObject jsonLocal = null;
				try {
					jsonLocal = (JsonObject)parser.parse(new String(Files.readAllBytes(Paths.get(returnFile))));
				} catch (Exception e1) {
					e1.printStackTrace();
					return new String(Files.readAllBytes(Paths.get("Res"+File.separator+"download_parser_error.json")));
				}
				
				JsonArray jsonUserZones = jsonRequest.get("basezones").getAsJsonArray();
				
				JsonArray jsonTopic = jsonLocal.get("EncUpdate").getAsJsonArray();
				JsonObject jsonMessage = jsonTopic.get(0).getAsJsonObject();
				JsonObject jsonMessage2 = (JsonObject)parser.parse(jsonMessage.get("message").getAsString());
				JsonArray jsonLocalZones = jsonMessage2.get("packages").getAsJsonArray();
				
				//JsonArray jsonLocalZones = jsonLocal.get("packages").getAsJsonArray();
				
				// it is demo, only compare first zone
				String localVersion = jsonLocalZones.get(0).getAsJsonObject().get("version").getAsString();
				String userVersion = jsonUserZones.get(0).getAsJsonObject().get("version").getAsString();
				
				String localDate = jsonLocalZones.get(0).getAsJsonObject().get("releaseDate").getAsString();
				String userDate = jsonUserZones.get(0).getAsJsonObject().get("releaseDate").getAsString();
				
				// 요청 버전이 현재버전보다 높으면 전달 패키지가 없다 
				if (localVersion.compareTo(userVersion) < 1  && localDate.compareTo(userDate) < 1) {	// if first of report cell udtn is 1, asume it is already updated.
					returnFile = "Res"+File.separator+"download_en_empty_sample.json";
				}
				
				try {
					response = new String(Files.readAllBytes(Paths.get(returnFile)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			} 
			else if (category.equals("ER")) {
				//String returnFile = "Res"+File.separator+"download_er_ftp_sample.json";
				String returnFile = "Res"+File.separator+"download_er_sample.json";
				JsonObject jsonLocal = null;
				try {
					jsonLocal = (JsonObject)parser.parse(new String(Files.readAllBytes(Paths.get(returnFile))));
				} catch (Exception e1) {
					e1.printStackTrace();
					return new String(Files.readAllBytes(Paths.get("Res"+File.separator+"download_parser_error.json")));
				}
				
				JsonArray jsonUserZones = jsonRequest.get("updatezones").getAsJsonArray();
				
				JsonArray jsonTopic = jsonLocal.get("EncUpdate").getAsJsonArray();
				JsonObject jsonMessage = jsonTopic.get(0).getAsJsonObject();
				JsonObject jsonMessage2 = (JsonObject)parser.parse(jsonMessage.get("message").getAsString());
				JsonArray jsonLocalZones = jsonMessage2.get("packages").getAsJsonArray();
				
				//JsonArray jsonLocalZones = jsonLocal.get("packages").getAsJsonArray();
				
				// it is demo, only compare first zone
				String localVersion = jsonLocalZones.get(0).getAsJsonObject().get("version").getAsString();
				String userVersion = jsonUserZones.get(0).getAsJsonObject().get("version").getAsString();
				
				String localDate = jsonLocalZones.get(0).getAsJsonObject().get("releaseDate").getAsString();
				String userDate = jsonUserZones.get(0).getAsJsonObject().get("releaseDate").getAsString();
				
				if (localVersion.compareTo(userVersion) < 1 && localDate.compareTo(userDate) < 1) {	// if first of report cell udtn is 1, asume it is already updated.
					returnFile = "Res"+File.separator+"download_er_empty_sample.json";
				}
				
				try {
					response = new String(Files.readAllBytes(Paths.get(returnFile)));
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		} catch (IOException ee) {
			ee.printStackTrace();
		}
		
		return response;
	}
	
}