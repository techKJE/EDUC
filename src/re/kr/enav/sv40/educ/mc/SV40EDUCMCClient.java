/* -------------------------------------------------------- */
/** Maritime Cloud Client for EDUC
File name : SV40EDUCMCClient.java 
Author : Sang Whan Oh(sang@kjeng.kr)
Creation Date : 2017-04-17
Version : v0.1
Rev. history :
Modifier : 
*/
/* -------------------------------------------------------- */
package re.kr.enav.sv40.educ.mc;

import java.io.IOException;
import java.net.ConnectException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import kr.ac.kaist.mms_client.MMSClientHandler;
import net.etri.pkilib.client.ClientPKILibrary;
import net.etri.pkilib.tool.ByteConverter;
import re.kr.enav.sv40.educ.controller.SV40EDUCController;
import re.kr.enav.sv40.educ.util.SV40EDUErrCode;
import re.kr.enav.sv40.educ.util.SV40EDUErrMessage;
import re.kr.enav.sv40.educ.util.SV40EDUUtil;
import re.kr.enav.sv40.educ.util.SV40EncUpdate;
import re.kr.enav.sv40.educ.util.SV40EncZoneRes;
import re.kr.enav.sv40.educ.util.TaskManager;
/**
 * @brief EDUC MC클라이언트 클래스
 * @details  MC를 사용하여 전자해도 업데이트 요청 및 업로드 알림 기능
 * @author Sang Whan Oh
 * @date 2017.04.03
 * @version 0.0.1
 *
 */
public class SV40EDUCMCClient extends Thread{
	private SV40EDUCController m_controller;
	private JsonObject m_config;
	private MMSClientHandler m_handlerMCSender;
	private MMSClientHandler m_handlerMCReceiver;
	private String m_strSrcMRN;
	private String m_strDestServiceMRN;
	private String m_strDestMccMRN;
	private int m_retryCnt;
	
	private boolean m_bDebugHeader = true;
	
	private String m_strMessageToSend="";
	
	private TaskManager m_taskManager =null;		/**< reference task manager */
	
	// MIR Cert
	private String m_hexSignedData;
	public final static String privateKeyPath = "Res\\PrivateKey.pem";
	public final static String certPath = "Res\\Certificate.pem";
	
	public SV40EDUCMCClient(SV40EDUCController controller) {
		m_controller = controller;
		m_config = m_controller.getConfig();
		m_strSrcMRN = SV40EDUUtil.queryJsonValueToString(m_config, "cloud.srcMRN");
		m_strDestServiceMRN = SV40EDUUtil.queryJsonValueToString(m_config, "cloud.destServiceMRN");
		m_strDestMccMRN = SV40EDUUtil.queryJsonValueToString(m_config, "cloud.destMccMRN");
		
		// MIR Cert
		ClientPKILibrary clientPKILib = ClientPKILibrary.getInstance();
		ByteConverter byteConverter = ByteConverter.getInstance();

		byte[] content = {0x00, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07, 0x08};
		
		byte[] signedData = clientPKILib.generateSignedData(content, privateKeyPath, certPath);
		m_hexSignedData = byteConverter.byteArrToHexString(signedData);
	}
	
	public SV40EDUCMCClient(SV40EDUCController controller, String toSendMessage) {
		this(controller);
		m_strMessageToSend = toSendMessage;
	}
	/**
	 * @brief setter for TaskManager
	 * @param taskManager task work manager
	 */		
	public void setTaskManager(TaskManager taskManager) {
		this.m_taskManager = taskManager;
		taskManager.m_threadTask = this;
	}
	/**
	 * @brief send message to MC
	 * @param message content
	 */
	public void sendMessage(String message) {
		try {
			Map<String, List<String>> headerfield = new HashMap<String, List<String>>();
			List<String> valueList = new ArrayList<String>();
			valueList.add("1234567890");
			headerfield.put("AccessToken",valueList);
			
			// MIR cert
			List<String> certList = new ArrayList<String>();
			certList.add(m_hexSignedData);
			headerfield.put("HexSignedData",certList);
			
			m_handlerMCSender.setMsgHeader(headerfield);
			
			m_retryCnt=0;
			
			Timer timer = new Timer();
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					if (m_retryCnt >= SV40EDUUtil.RETRY_MAXCOUNT) {
						timer.cancel();
						String msg = "잠시 후 다시 시도해 주시기 바랍니다.";
						m_controller.addLog(msg);
					}
					else
					{
						try {
							m_handlerMCSender.sendPostMsg(m_strDestServiceMRN, message);
							timer.cancel();
						} catch (ConnectException ce) {
							String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_003, "MMSServer");
							m_controller.addLog(msg);
						} catch (IOException ie) {
							String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_003, m_strDestServiceMRN);
							m_controller.addLog(msg);
						} catch (Exception e) {
							e.printStackTrace();
						}
						m_retryCnt++;
					}
				}
			};

			timer.schedule(task, 0, SV40EDUUtil.RETRY_TIMEWAIT);
			
		} catch (Exception e) {
			e.printStackTrace();
			m_taskManager.failed();	// delegate to the TaskManager
		}
		
	}	
	
	/**
	 * @brief initialize MC Sender 
	 */	
	private void initSender() throws Exception {
		m_handlerMCSender = new MMSClientHandler(m_strSrcMRN);
		m_handlerMCSender.setSender(new MMSClientHandler.ResponseCallback (){
			@Override
			public void callbackMethod(Map<String, List<String>> headerField, String message) {
				printHeader("MC Sender", headerField);
				if (message!= null && !message.isEmpty()) {
					m_controller.addLog("Sender Result Message:"+message);
				}
			}
		});		
	}
	
	private void printHeader(String title, Map<String, List<String>> headerField) {
		if (m_bDebugHeader == false) return;
		
		Iterator<String> iter = headerField.keySet().iterator();
		System.out.println("======="+title+"=======");
		while (iter.hasNext()){
			String key = iter.next();
			System.out.println(key+":"+headerField.get(key).toString());
		}						
	}
	
	/**
	 * @brief initialize MC Receiver, this polling message from MCC 
	 */	
	private void initReceiver() throws Exception {
		int pollInterval = 1000;
		m_handlerMCReceiver = new MMSClientHandler(m_strSrcMRN);
		
		m_handlerMCReceiver.startPolling(m_strDestMccMRN, m_strDestServiceMRN, pollInterval, new MMSClientHandler.PollingResponseCallback() {
			@Override
			public void callbackMethod(Map<String, List<String>> headerField, List<String> messages) {
				printHeader("MC Receiver",headerField);
			
				Iterator<String> iter = messages.iterator();
				while (iter.hasNext()){
					String message = iter.next();
					JsonParser parser = new JsonParser();
					try {

						// S-10x 수신
						String categoryOfService = SV40EDUUtil.getCategoryOfService(message);
						if (categoryOfService.equals("Zone Information"))
						{
							JsonObject jsonRes = SV40EncZoneRes.getEncZoneRes(message);
							m_controller.processEncZoneReceive(jsonRes);
						}
						else if (categoryOfService.equals("ENC"))
						{
							JsonObject jsonRes = SV40EncUpdate.getEncUpdate(message);
							m_controller.processMCMessageReceive(jsonRes);
						}
						
//						/* response from MCC
//						 * {
//						 *		"EncUpdate": [{          
//						 *			"message": "{......}"
//						 *		}]
//						 * }
//						 * {
//						 * 		"EncZoneRes":[{
//						 * 			"message": "{......}"
//						 * 		}]
//						 * }
//						 */
//						JsonObject jsonResponse = (JsonObject)parser.parse(message);
//						
//						JsonArray jsonTopic;
//						JsonElement elZone = jsonResponse.get("EncZoneRes");
//						if (elZone != null) 
//							jsonTopic = jsonResponse.get("EncZoneRes").getAsJsonArray();
//						else 
//							jsonTopic = jsonResponse.get("EncUpdate").getAsJsonArray();
//						
//						JsonObject jsonMessage = jsonTopic.get(0).getAsJsonObject();
//						JsonObject jsonMessage2 = (JsonObject)parser.parse(jsonMessage.get("message").getAsString());
//						
//						if (elZone != null)
//							m_controller.processEncZoneReceive(jsonMessage2);
//						else
//							m_controller.processMCMessageReceive(jsonMessage2);
					} catch (Exception e) {
						m_controller.addLog("Receiver failed to parse message from EDUS:"+message);
					}
				}										
			}
		});
		
	}
	
	public void run() {
		try {
			initSender();
			initReceiver();
			
			if (!m_strMessageToSend.isEmpty()) {
				sendMessage(m_strMessageToSend);
			}
		} catch (Exception e) {
			
			m_taskManager.failed();	// delegate to the TaskManager
		}
	}
}
