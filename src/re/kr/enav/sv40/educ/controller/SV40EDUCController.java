/* -------------------------------------------------------- */
/** Controller of EDUC
File name : SV40EDUCController.java 
Author : Sang Whan Oh(sang@kjeng.kr)
Creation Date : 2017-04-04
Version : v0.1
Rev. history :
Modifier : 
*/
/* -------------------------------------------------------- */
package re.kr.enav.sv40.educ.controller;

import java.io.File;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;

import javax.swing.JOptionPane;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import re.kr.enav.sv40.educ.mc.SV40EDUCMCClient;
import re.kr.enav.sv40.educ.util.LocalNetworkState;
import re.kr.enav.sv40.educ.util.SV40EDUCDownloader;
import re.kr.enav.sv40.educ.util.SV40EDUCHTTPDownloader;
import re.kr.enav.sv40.educ.util.SV40EDUErrCode;
import re.kr.enav.sv40.educ.util.SV40EDUErrMessage;
import re.kr.enav.sv40.educ.util.SV40EDUUtil;
import re.kr.enav.sv40.educ.util.SV40EncReq;
import re.kr.enav.sv40.educ.util.SV40EncZoneReq;
import re.kr.enav.sv40.educ.util.SV40S101UpdateStatusReport;
import re.kr.enav.sv40.educ.util.TaskManager;

/**
 * @brief View UI와 전자해도 다운로드 서비스를 제어관리하는 컨트롤러 클래스
 * @details  MC 송수신, HTTP 송수신 및 설정 파일, 업데이트 레포트 관리기능 제공.
 * @author Sang Whan Oh
 * @date 2017.04.03
 * @version 0.0.1
 *
 */
public class SV40EDUCController {
	private TaskManager m_taskDownload;
	private TaskManager m_taskMC;
	private boolean m_downComplete;
	
	private Date m_dtReq;
	private Date m_dtReqZone;
	
	public interface CallbackView {
		void updateProgress(int nPercent, String label);
		void addLog(StackTraceElement el,String message);
		void debugLog(StackTraceElement el, String message);
		void failDownload(JsonObject jsonFile);
	}

	public SV40EDUCMCClient m_mcClient;
	private CallbackView m_callbackView; 
	public final static String s_pathOfConfig = "Res\\config.json";
	public final static String s_defaultPathOfConfig = "Res\\default.json";
	public final static String s_pathOfReport = "Res\\report.json";
	public final static String s_pathOfZone = "Res\\zone.json";
	public final static String s_log4Config = "Res\\log4j.properties";
	public final static String s_nameOfCatalog = "S101ed1.CAT";
	
	public JsonObject m_jsonConfig;	/**< store configuration */	
	public JsonObject m_jsonZone;
	public LocalNetworkState m_local;
	
	public void setCallbackProgress(CallbackView callback) {
		this.m_callbackView = callback;
	}
	
	public SV40EDUCController() {
		m_taskDownload = new TaskManager(SV40EDUCHTTPDownloader.MAX_COUNT_TRYDOWNLOAD, SV40EDUCHTTPDownloader.TIME_WAIT_TRYDOWNLOAD);
		m_taskMC = new TaskManager(SV40EDUUtil.RETRY_MAXCOUNT, SV40EDUUtil.RETRY_TIMEWAIT);
		
		m_callbackView = null;
		init();
	}
	/**
	 * @brief initialize controller
	 * @details load config, prepare mc and http
	 */		
	public void init() {
		m_jsonConfig = loadConfig();
		if (m_jsonConfig == null) {
			String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_001, "config");
			JOptionPane.showMessageDialog(null, msg, "Warning", JOptionPane.INFORMATION_MESSAGE);
			//JOptionPane.showMessageDialog(null, "Failed load configuration", "Warning", JOptionPane.INFORMATION_MESSAGE);
			System.exit(0);
			return;
		}
		
		m_jsonZone = loadZone();
		if (m_jsonZone == null) {
			String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_008, "zone");
			JOptionPane.showMessageDialog(null, msg, "Warning", JOptionPane.INFORMATION_MESSAGE);
		}
		
		startMCClient();
		startLocalNetwork();
	}
	
	public JsonObject getConfig() {
		return m_jsonConfig;
	}
	
	public JsonObject loadZone() {
		JsonObject json = null;
		try {
			json = readConfig(s_pathOfZone);
		} catch (Exception e) {
		}
		
		return json;
	}
	
	/**
	 * @brief read configuration
	 * @details read json file and return configuration json object
	 */		
	public JsonObject loadConfig() {
		JsonObject json = null;
		try {
			json = readConfig(s_pathOfConfig);
		} catch (Exception e) {
			// if fail to load config, load default
			json = loadDefaultConfig();
		}
		
		return json;
	}
	
	/**
	 * @brief read configuration
	 * @details read json file and return configuration json object
	 * param path path of configuration json file
	 */		
	private JsonObject readConfig(String path) throws Exception {
		return SV40EDUUtil.readJson(path);
	}	
	
	/**
	 * @brief write configuration
	 * @details write json
	 */		
	public boolean saveConfig(JsonObject json) {
		return SV40EDUUtil.writeJson(json, s_pathOfConfig);
	}
	
	/**
	 * @brief apply configuration change
	 * @details save changed configuration and apply
	 */		
	public void applyConfigChange(JsonObject jsonChanged) {
		//"cloud.srcMRN", "cloud.destMRN"
		String newSrcMRN = SV40EDUUtil.queryJsonValueToString(jsonChanged, "cloud.srcMRN");
		String newDestServiceMRN = SV40EDUUtil.queryJsonValueToString(jsonChanged, "cloud.destServiceMRN");
		String newDestMccMRN = SV40EDUUtil.queryJsonValueToString(jsonChanged, "cloud.destMccMRN");
		
		String srcMRN = SV40EDUUtil.queryJsonValueToString(m_jsonConfig, "cloud.srcMRN");
		String destServiceMRN = SV40EDUUtil.queryJsonValueToString(m_jsonConfig, "cloud.destServiceMRN");
		String destMccMRN = SV40EDUUtil.queryJsonValueToString(m_jsonConfig, "cloud.destMccMRN");
		
		m_jsonConfig = jsonChanged;
		saveConfig(m_jsonConfig);
		
		// mrn이 변경되었을때 mcclient 재시작
		if (!srcMRN.equals(newSrcMRN) 
				|| !destServiceMRN.equals(newDestServiceMRN)
				|| !destMccMRN.equals(newDestMccMRN)) {
			stopMCClient();
			startMCClient();
		}
	}
	
	public JsonObject loadDefaultConfig() {
		JsonObject json = null;
		try {
			json = readConfig(s_defaultPathOfConfig);
		} catch (Exception e) {
			e.printStackTrace();
		}
		return json;
	}
	
	/**
	 * @brief request latest base enc (EN) download
	 * @details request latest base enc (EN) download
	 */		
	public boolean requestDownloadEN() {
		boolean bRet = true;
		StackTraceElement el = Thread.currentThread().getStackTrace()[1];
		
		// 내부네트웍 확인
		if (SV40EDUUtil.LOCALNETWORK_STATE == false) {
			String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_002, "localhost");
			addLog(el, msg);
			return false;
		}
		
		JsonObject jsonConfig = getConfig();
		SV40S101UpdateStatusReport report = new SV40S101UpdateStatusReport(s_pathOfReport);

		boolean useS10x=true;
		
		// S-10x 전송
		if (useS10x == true) 
		{
			String gml = SV40EncReq.getEncReq("EN", jsonConfig, report);
			addLog(el, "Requested update check for - EN");
			debugLog(el, gml);
			m_dtReq = new Date();
			m_mcClient.sendMessage(gml);
		} 
		else 
		{
			JsonObject jsonRequest = new JsonObject();
			jsonRequest.addProperty("license", SV40EDUUtil.queryJsonValueToString(jsonConfig, "enc.license"));
			jsonRequest.addProperty("maker", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.maker"));
			jsonRequest.addProperty("model", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.model"));
			jsonRequest.addProperty("serial", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.serial"));
			jsonRequest.addProperty("latitude", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.position.lat"));
			jsonRequest.addProperty("longitude", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.position.lon"));
			
			jsonRequest.addProperty("category", "EN");
			
			JsonArray zones = new JsonArray();
			
			String localZone = SV40EDUUtil.queryJsonValueToString(jsonConfig, "enc.zone");
			String zonever = SV40EDUUtil.getZoneVer(m_jsonZone, localZone);
			
			// basezones + updatezones
			JsonObject jsonLocalZone = report.getBaseZone(localZone);
			if (jsonLocalZone == null) {
				JsonObject zone = new JsonObject();
				zone.addProperty("zone", localZone);
				zone.addProperty("zonever", zonever);
				zone.addProperty("version", "");
				zone.addProperty("releaseDate", "");
				zones.add(zone);
			} else {
				zones.add(jsonLocalZone);
			}
			jsonRequest.add("basezones", zones);
			
			JsonArray uzones = new JsonArray();
			jsonLocalZone = report.getUpdateZone(localZone);
			if (jsonLocalZone == null) {
				JsonObject zone = new JsonObject();
				zone.addProperty("zone", localZone);
				zone.addProperty("zonever", zonever);
				zone.addProperty("version", "");
				zone.addProperty("releaseDate", "");
				uzones.add(zone);
			} else {
				uzones.add(jsonLocalZone);
			}
			jsonRequest.add("updatezones", uzones);
			
			JsonObject jsonRequestFull = new JsonObject();
		
			JsonArray testArrayJson = new JsonArray();
			JsonObject MSG = new JsonObject();
			MSG.addProperty("message", jsonRequest.toString());
			testArrayJson.add(MSG);
			jsonRequestFull.add("EncReq", testArrayJson);
			
			addLog(el, "Requested update check for - EN");
			
			m_mcClient.sendMessage(jsonRequestFull.toString());
		}
		
		return bRet;
	}
	/**
	 * @brief request latest ER download
	 * @details request latest ER download
	 */		
	public boolean requestDownloadER() {
		boolean bRet = true;
		StackTraceElement el = Thread.currentThread().getStackTrace()[1];
		
		// 내부네트웍 확인
		if (SV40EDUUtil.LOCALNETWORK_STATE == false) {
			String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_002, "localhost");
			addLog(el, msg);
			return false;
		}
		
		JsonObject jsonConfig = getConfig();
		SV40S101UpdateStatusReport report = new SV40S101UpdateStatusReport(s_pathOfReport);
		JsonObject jsonReport = report.toJson();

		boolean useS10x=true;
		
		// S-10x 전송
		if (useS10x == true) 
		{
			String gml = SV40EncReq.getEncReq("ER", jsonConfig, report);
			addLog(el, "Requested update check for - ER");
			debugLog(el, gml);
			m_dtReq = new Date();
			m_mcClient.sendMessage(gml);
		} 
		else 
		{
			JsonObject jsonRequest = new JsonObject();
			jsonRequest.addProperty("license", SV40EDUUtil.queryJsonValueToString(jsonConfig, "enc.license"));
			jsonRequest.addProperty("maker", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.maker"));
			jsonRequest.addProperty("model", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.model"));
			jsonRequest.addProperty("serial", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.serial"));
			jsonRequest.addProperty("latitude", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.position.lat"));
			jsonRequest.addProperty("longitude", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.position.lon"));
			
			jsonRequest.addProperty("category", "ER");
			
			/* remove cells property for the limit of size */
	//		jsonReport.add("cells", null);
	//		jsonReport.add("encs", null);
	//		jsonRequest.add("report", jsonReport);
			
			JsonArray zones = new JsonArray();
			
			String localZone = SV40EDUUtil.queryJsonValueToString(jsonConfig, "enc.zone");
			String zonever = SV40EDUUtil.getZoneVer(m_jsonZone, localZone);
			
			// base가 없는 경우
			JsonObject jsonLocalZone = report.getBaseZone(localZone);
			if (jsonLocalZone == null) {
				String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_008, "EN");
				addLog(el, msg);
				return false;
			} else {
				zones.add(jsonLocalZone);
			}
			jsonRequest.add("basezones", zones);
			
			JsonArray uzones = new JsonArray();
			jsonLocalZone = report.getUpdateZone(localZone);
			if (jsonLocalZone == null) {
				JsonObject zone = new JsonObject();
				zone.addProperty("zone", localZone);
				zone.addProperty("zonever", zonever);
				zone.addProperty("version", "");
				zone.addProperty("releaseDate", "");
				uzones.add(zone);
			} else {
				uzones.add(jsonLocalZone);
			}
			jsonRequest.add("updatezones", uzones);
			
	//		JsonArray jsonUserZones = jsonReport.get("zones").getAsJsonArray();
	//		jsonRequest.add("zones", jsonUserZones);
			
			JsonObject jsonRequestFull = new JsonObject();
			
			JsonArray testArrayJson = new JsonArray();
			JsonObject MSG = new JsonObject();
			MSG.addProperty("message", jsonRequest.toString());
			testArrayJson.add(MSG);
			jsonRequestFull.add("EncReq", testArrayJson);
			
			addLog(el, "Requested update check for - ER");
			m_mcClient.sendMessage(jsonRequestFull.toString());
		}
		
		return bRet;
	}
	
	/**
	 * @brief request latest ER download
	 * @details request latest ER download
	 */		
	public boolean requestDownloadZone() {
		boolean bRet = true;
		StackTraceElement el = Thread.currentThread().getStackTrace()[1];
		
		// 내부네트웍 확인
		if (SV40EDUUtil.LOCALNETWORK_STATE == false) {
			String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_002, "localhost");
			addLog(el, msg);
			return false;
		}
		
		JsonObject jsonConfig = getConfig();
		
		boolean useS10x=true;
		
		// S-10X 전송
		if (useS10x == true) 
		{
			String gml = SV40EncZoneReq.getEncZoneReq(jsonConfig);
			addLog(el, "Requested zone info.");
			debugLog(el, gml);
			m_dtReqZone = new Date();
			m_mcClient.sendMessage(gml);
		} 
		else 
		{
			JsonObject jsonRequest = new JsonObject();
			jsonRequest.addProperty("license", SV40EDUUtil.queryJsonValueToString(jsonConfig, "enc.license"));
			
			JsonObject jsonRequestFull = new JsonObject();
			
			JsonArray testArrayJson = new JsonArray();
			JsonObject MSG = new JsonObject();
			MSG.addProperty("message", jsonRequest.toString());
			testArrayJson.add(MSG);
			jsonRequestFull.add("EncZoneReq", testArrayJson);
			
			addLog(el, "Requested zone info.");
			m_mcClient.sendMessage(jsonRequestFull.toString());
		}
		return bRet;
	}
	
	/**
	 * @brief request latest ER download
	 * @details request latest ER download
	 * @param received total bytes of received
	 * @param total file size to receive
	 */		
	public void updateDownloadProgress(long received, long fileSize) {
		int nPercent = (int) (100*(double)received/(double)fileSize);
		String label = String.format("downloading %d%%",  nPercent);
		updateProgress(nPercent, label);
	}
	/**
	 * @brief update progress
	 * @details update progress
	 * @param nPercent state of progress
	 * @param label label to show in progress
	 */		
	public void updateProgress(int nPercent, String label) {
		if (m_callbackView != null) {
			m_callbackView.updateProgress(nPercent, label);
		}
	}
	
	/**
	 * @brief set download complete flag
	 * @details set download complete flag
	 * @param download complete flag
	 */
	public void setDownComplete(boolean downComplete) {
		m_downComplete = downComplete; 
	}
	
	/**
	 * @brief get download complete flag
	 * @details get download complete flag
	 * @param download complete flag
	 */
	public boolean getDownComplete() {
		return m_downComplete; 
	}
	
	/**
	 * @brief add log
	 * @details add log
	 * @param message message to log
	 */		
	public void addLog(StackTraceElement el, String message) {
		if (m_callbackView != null) {
			m_callbackView.addLog(el, message);
		}		
	}
	
	public void debugLog(StackTraceElement el, String message) {
		if (m_callbackView != null) {
			m_callbackView.debugLog(el, message);
		}		
	}
	
	/**
	 * @brief add log
	 * @details add log
	 * @param message message to log
	 */		
	public void failedDownload() {
		StackTraceElement el = Thread.currentThread().getStackTrace()[1];
		addLog(el, "Failed download");
	}
	/**
	 * @brief add log
	 * @details add log
	 * @param message message to log
	 */		
	public void cancelDownload() {
		StackTraceElement el = Thread.currentThread().getStackTrace()[1];
		addLog(el, "Cancel download");
	}
	
	
	public ArrayList<String> searchZones(String destDir, String fileCategory) throws Exception {
		ArrayList<String> zones = new ArrayList<String>();
		boolean bEN = fileCategory.equals("EN");
		String[] names = {
			"S-101",
			"S-63-101",
			"S-57",
			"S-63",
		};

		for (String name:names) {
			String path = String.format("%s/%s%s/M01X01/%s/ENC_ROOT/KR", destDir, name, (bEN)? "Base":"Update", (bEN)? "B1":"U1");
			File dir = new File(path);
			if (!dir.exists())
				continue;

			File[] files = dir.listFiles();
			for (File file:files) {
				System.out.println("file: " + file.getName());
				if (file.isDirectory())
					zones.add(file.getName());
			}
			break;
		}
		
		return zones;
	}
	
	/**
	 * @brief add log
	 * @details add log
	 * @param message message to log
	 */		
	public void completeDownload(String workdir, String zipFilePath, JsonObject jsonFile) {
		StackTraceElement el = Thread.currentThread().getStackTrace()[1];
		
		addLog(el, "completed download");
		// unzip file
		try {
			String fileName = SV40EDUUtil.queryJsonValueToString(jsonFile, "fileName");
			String fileZone = SV40EDUUtil.queryJsonValueToString(jsonFile, "zone");
			addLog(el, "starting unzip - " + fileName);			
			String destDir = unzip(workdir, zipFilePath, jsonFile);
			addLog(el, "completed unzip");
			
			// A1 상세 존정보 추출
			ArrayList<String> zones = null;
			if (fileZone.equals("A1")) {
				String fileCategory = SV40EDUUtil.queryJsonValueToString(jsonFile, "fileCategory");
				zones = searchZones(destDir, fileCategory);
				addLog(el, "detail zones " + zones.toString());
			}  
			addLog(el, "Ready to use ENC");
			
			// S-101
			SV40S101UpdateStatusReport report = new SV40S101UpdateStatusReport(s_pathOfReport);
//			String pathOfCatalog = destDir+"\\"+s_nameOfCatalog;

			// 압축해제후 1초 지연처리(catalog 접근 오류)
//			Thread.sleep(1000);
			
//			// S-101 카탈로그가 있으면 기록
//			File f = new File(pathOfCatalog);
//			if (f.isFile()) {
//				report.updateReportFromCatalog(pathOfCatalog);
//			}
			
			report.updateReportFromENCUpdateFile(jsonFile, zones);
			report.save();
			updateProgress(0, "Ready to use");
			
			//m_downComplete = true;			
		} catch (Exception e) {
			int pos = zipFilePath.lastIndexOf('\\');
			String file = zipFilePath.substring(pos+1);
			String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_005, file);
			addLog(el, msg);
			//addLog("Failed to unzip download");
			e.printStackTrace();
		}
		
		// delete zipFile
		File toDelete = new File(zipFilePath);
		toDelete.delete();
	}
	/**
	 * @brief add log
	 * @details add log
	 * @param message message to log
	 */		
	public String unzip(String workdir, String zipFilePath,JsonObject jsonFile) throws Exception {
		StackTraceElement el = Thread.currentThread().getStackTrace()[1];
		
		JsonObject jsonConfig = getConfig();
		String fileName = SV40EDUUtil.queryJsonValueToString(jsonFile, "fileName");
		String destDirectory = String.format("%s/%s/%s", SV40EDUUtil.queryJsonValueToString(jsonConfig, "enc.path"), 
				workdir, fileName.substring(0, fileName.lastIndexOf(".")));
		
		UnzipParameters param = new UnzipParameters();
		param.setIgnoreAllFileAttributes(true);
		
		ZipFile zipFile = new ZipFile(zipFilePath);
		zipFile.setRunInThread(true);
		zipFile.extractAll(destDirectory, param);
		ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
		
		while (progressMonitor.getState() == ProgressMonitor.STATE_BUSY) {
			int nPercent = progressMonitor.getPercentDone();
			updateProgress(nPercent, String.format("unziping %d%%", nPercent));
			Thread.sleep(5);
		}
		
		if (progressMonitor.getResult() == ProgressMonitor.RESULT_ERROR) {
			if (progressMonitor.getException() != null)
				debugLog(el, progressMonitor.getException().getMessage());
		}
		
//		boolean isCompleted = false;
//		int nPrevPercent = -1;
//		while (!isCompleted && progressMonitor.getState() == ProgressMonitor.STATE_BUSY) {
//			int nPercent = progressMonitor.getPercentDone();
//			if (nPrevPercent != nPercent) {			
//				updateProgress(nPercent, String.format("unziping %d%%", nPercent));
//				nPrevPercent = nPercent;
//			}
//			if (nPercent == 100) {
//				isCompleted = true;
//			} else {
//				// give time to update progress bar
//				try {
//					Thread.sleep(50);
//				} catch(Exception e) {
//					
//				}
//				switch (progressMonitor.getCurrentOperation()) {
//				case ProgressMonitor.OPERATION_NONE:
////						System.out.println("no operation being performed");
//					break;
//				case ProgressMonitor.OPERATION_ADD:
////						System.out.println("Add operation");
//					break;
//				case ProgressMonitor.OPERATION_EXTRACT:
////						System.out.println("Extract operation");
//					break;
//				case ProgressMonitor.OPERATION_REMOVE:
////						System.out.println("Remove operation");
//					break;
//				case ProgressMonitor.OPERATION_CALC_CRC:
////						System.out.println("Calculating CRC");
//					break;
//				case ProgressMonitor.OPERATION_MERGE:
////						System.out.println("Merge operation");
//					break;
//				default:
//					//isCompleted = true;
////						System.out.println("invalid operation");
//					break;
//				}
//			}
//		}
		
		String extractDirectory = destDirectory+"\\ENC_ROOT";  
		File file = new File(extractDirectory);
		return (file.isDirectory())? extractDirectory:destDirectory;  
	}	
	
	/**
	 * @brief start download
	 * @details add log
	 * @param message message to log
	 */			
	public void startDownload(String workdir, JsonObject jsonMessage) {
		//m_downComplete = false;
		setDownComplete(false);
		
		SV40EDUCController controller = this;
		
		m_taskDownload.setTask(new TaskManager.Task() {
			@Override
			public void work() {		
				new SV40EDUCDownloader(controller, workdir, jsonMessage, m_taskDownload);
			}
		
		});
		m_taskDownload.start();
	}
	/**
	 * @brief start maritime cloud client
	 * @details start maritime cloud client
	 */			
	public void startMCClient() {
		SV40EDUCController controller = this;
		m_taskMC.setTask(new TaskManager.Task() {
			@Override
			public void work() {		
				controller.m_mcClient = new SV40EDUCMCClient(controller);
				controller.m_mcClient.setTaskManager(m_taskMC);
				controller.m_mcClient.start();
			}
		
		});
		m_taskMC.start();
	}
	/**
	 * @brief stop maritime cloud client
	 * @details stop maritime cloud client
	 */			
	public void stopMCClient() {
		if (m_taskMC != null)
			m_taskMC.stop();
		
		if (m_mcClient != null && !m_mcClient.isInterrupted()) {
			m_mcClient.interrupt();
			m_mcClient = null;
		}
	}

	/**
	 * @brief local network state
	 * @details local network state
	 */			
	public void startLocalNetwork() {
		m_local = new LocalNetworkState(this, 1000);
		m_local.start();
	}
	
	/**
	 * @brief call back Maritime Cloud Message
	 * @details call back Maritime Cloud Message
	 */			
	public void processMCMessageReceive(JsonObject jsonResponse) {
		StackTraceElement el = Thread.currentThread().getStackTrace()[1];
		
		Date dtCur = new Date();
		long sec = dtCur.getTime() - m_dtReq.getTime();
		String detail = (new SimpleDateFormat("mm:ss.SSS")).format(new Date(sec));
		addLog(el, "Received update (" + detail + ")");
		
		String result = jsonResponse.get("result").getAsString();
		String message = jsonResponse.get("message").getAsString();
		
		if (!result.equals("ok")) {
			addLog(el, "Update Error: " + message);
			return;
		}
		
		// get to update 
		JsonArray jsonPackages = jsonResponse.get("packages").getAsJsonArray();
		int nSizePackages = jsonPackages.size();
		if (nSizePackages == 0) {
			addLog(el, "ENC is already updated.");
			return;
		}
		
		Object[] options = { "Yes", "No" };
	    int response = JOptionPane.showOptionDialog(null,
	            "There is a new package. Would you like to download it?", "Notification",
	            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
	            options, options[1]);
	    
	    // 다운로드 여부 선택
	    if (response != JOptionPane.OK_OPTION) {
	    	addLog(el, "ENC download cancel");
	    } else {
			// start to download
	    	Date now = new Date();
	    	SimpleDateFormat ftDate = new SimpleDateFormat("yyyyMMddHHmmssSSS");
	    	String workdir = String.format("%s_%d", ftDate.format(now), nSizePackages);
			for (int i=0; i<nSizePackages; i++) {
				JsonObject jsonPackage = (JsonObject)jsonPackages.get(i);
				startDownload(workdir, jsonPackage);
				
				// download complete wait
				try {
					//while (m_downComplete == false) {
					while (getDownComplete() == false) {
						Thread.sleep(1000);
					}
				} catch (InterruptedException ie) {				
				}			
			}
	    }
	}
	
	public void processEncZoneReceive(JsonObject jsonResponse) {
		StackTraceElement el = Thread.currentThread().getStackTrace()[1];
		
		Date dtCur = new Date();
		long sec = dtCur.getTime() - m_dtReqZone.getTime();
		String detail = (new SimpleDateFormat("mm:ss.SSS")).format(new Date(sec));
		addLog(el, "Received zone (" + detail + ")");
		debugLog(el, jsonResponse.toString());
		
		String result = jsonResponse.get("result").getAsString();
		String message = jsonResponse.get("message").getAsString();
		
		if (!result.equals("ok")) {
			addLog(el, "Zone Error " + message);
			return;
		}
		
		// get to zone 
		JsonArray jsonZones = jsonResponse.get("zones").getAsJsonArray();
		int nSizeZones = jsonZones.size();
		if (nSizeZones == 0) {
			addLog(el, "ZONE not found");
			return;
		}

		jsonResponse.remove("result");
		jsonResponse.remove("message");
		
		SV40EDUUtil.writeJson(jsonResponse, s_pathOfZone);
		addLog(el, "Received zone done");
		
		// load zone
		m_jsonZone = loadZone();
		if (m_jsonZone == null) {
			String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_008, "ZONE");
			JOptionPane.showMessageDialog(null, msg, "Warning", JOptionPane.INFORMATION_MESSAGE);
		} else {
			// 기존의 report.json을 리네임
			File file = new File(s_pathOfReport);
			if (file.exists()) {
				String tail = (new SimpleDateFormat("yyMMddHHmmss")).format(new Date());
				File nfile = new File(s_pathOfReport + "_" + tail);
				file.renameTo(nfile);
				addLog(el, "rename ("+s_pathOfReport + ") -> (" + s_pathOfReport + "_" + tail + ")");
			}
		}		
	}
}
