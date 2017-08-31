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

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.net.ConnectException;
import java.net.BindException;
import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.google.gson.JsonSyntaxException;
import kr.ac.kaist.mms_client.MMSClientHandler;
import re.kr.enav.sv40.educ.controller.SV40EDUCController;
import re.kr.enav.sv40.educ.util.SV40EDUErrCode;
import re.kr.enav.sv40.educ.util.SV40EDUErrMessage;
import re.kr.enav.sv40.educ.util.SV40EDUUtil;
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
	private MMSClientHandler m_handler;
	private String m_strSrcMRN;
	private String m_strDestMRN;
	private int m_retryCnt;
	
	private String m_strMessageToSend="";
	
	private TaskManager m_taskManager =null;		/**< reference task manager */
	
	public SV40EDUCMCClient(SV40EDUCController controller) {
		m_controller = controller;
		m_config = m_controller.getConfig();
		m_strSrcMRN = SV40EDUUtil.queryJsonValueToString(m_config, "cloud.srcMRN");
		m_strDestMRN = SV40EDUUtil.queryJsonValueToString(m_config, "cloud.destMRN");
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
			m_handler.setMsgHeader(headerfield);
			
			m_retryCnt=0;
			
			Timer timer = new Timer();
			TimerTask task = new TimerTask() {
				@Override
				public void run() {
					if (m_retryCnt >= SV40EDUUtil.RETRY_MAXCOUNT)
						timer.cancel();
					else
					{
						try {
							m_handler.sendPostMsg(m_strDestMRN, "/edus", message);
							timer.cancel();
						} catch (ConnectException ce) {
							String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_003, "MMSServer");
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
			m_taskManager.failed();	// delegate to the TaskManager
		}
	}	

	public void run() {
		try {
			m_handler = new MMSClientHandler(m_strSrcMRN);
			
			m_handler.setSender(new MMSClientHandler.ResponseCallback (){
				//Response Callback from the request message
				@Override
				public void callbackMethod(Map<String, List<String>> headerField, String message) {
					Iterator<String> iter = headerField.keySet().iterator();
					while (iter.hasNext()){
						String key = iter.next();
						System.out.println(key+":"+headerField.get(key).toString());
					}					
					if (!message.isEmpty()) {
						JsonParser parser = new JsonParser();
						try {
							JsonObject jsonResponse = (JsonObject)parser.parse(message);
							m_controller.processMCMessageReceive(jsonResponse);
						} catch (JsonSyntaxException e) {
							// failed to parse
							m_controller.addLog(message);
						}
				}
				}
			});
			
			if (!m_strMessageToSend.isEmpty()) {
				sendMessage(m_strMessageToSend);
			}
		} catch (BindException be) {
			//mrn이 변경되어 client를 재시작할때 포트 사용중 오류 발생 
			m_controller.addLog("Already Bind Address");			
		} catch (Exception e) {
			
			m_taskManager.failed();	// delegate to the TaskManager
		}
	}
}
