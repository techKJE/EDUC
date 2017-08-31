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
import javax.swing.JOptionPane;
import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import kr.ac.kaist.mms_client.MMSClientHandler;
import net.lingala.zip4j.core.ZipFile;
import net.lingala.zip4j.model.UnzipParameters;
import net.lingala.zip4j.progress.ProgressMonitor;
import re.kr.enav.sv40.educ.mc.SV40EDUCMCClient;
import re.kr.enav.sv40.educ.util.LocalNetworkState;
import re.kr.enav.sv40.educ.util.SV40EDUCHTTPDownloader;
import re.kr.enav.sv40.educ.util.SV40EDUErrCode;
import re.kr.enav.sv40.educ.util.SV40EDUUtil;
import re.kr.enav.sv40.educ.util.SV40S101UpdateStatusReport;
import re.kr.enav.sv40.educ.util.TaskManager;
import re.kr.enav.sv40.educ.util.SV40EDUErrMessage;

/**
 * @brief View UI�� �����ص� �ٿ�ε� ���񽺸� ��������ϴ� ��Ʈ�ѷ� Ŭ����
 * @details  MC �ۼ���, HTTP �ۼ��� �� ���� ����, ������Ʈ ����Ʈ ������� ����.
 * @author Sang Whan Oh
 * @date 2017.04.03
 * @version 0.0.1
 *
 */
public class SV40EDUCController {
	private TaskManager m_taskDownload;
	private TaskManager m_taskMC;
	
	public interface CallbackView {
		void updateProgress(int nPercent, String label);
		void addLog(String message);
		void failDownload(JsonObject jsonFile);
	}

	public SV40EDUCMCClient m_mcClient;
	private CallbackView m_callbackView; 
	public final static String s_pathOfConfig = "Res\\config.json";
	public final static String s_defaultPathOfConfig = "Res\\default.json";
	public final static String s_pathOfReport = "Res\\report.json";
	public final static String s_nameOfCatalog = "S101ed1.CAT";
	
	public JsonObject m_jsonConfig;	/**< store configuration */		
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
		
		startMCClient();
		startLocalNetwork();
	}	
	public JsonObject getConfig() {
		return m_jsonConfig;
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
		String nsrcMRN = SV40EDUUtil.queryJsonValueToString(jsonChanged, "cloud.srcMRN");
		String ndestMRN = SV40EDUUtil.queryJsonValueToString(jsonChanged, "cloud.destMRN");
		String srcMRN = SV40EDUUtil.queryJsonValueToString(m_jsonConfig, "cloud.srcMRN");
		String destMRN = SV40EDUUtil.queryJsonValueToString(m_jsonConfig, "cloud.destMRN");
		
		m_jsonConfig = jsonChanged;
		saveConfig(m_jsonConfig);
		
		// mrn�� ����Ǿ����� mcclient �����
		if (!srcMRN.equals(nsrcMRN) || !destMRN.equals(ndestMRN)) {
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

		// ���γ�Ʈ�� Ȯ��
		if (SV40EDUUtil.LOCALNETWORK_STATE == false) {
			String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_002, "localhost");
			addLog(msg);
			return false;
		}
		
		JsonObject jsonConfig = getConfig();
		
		JsonObject jsonRequest = new JsonObject();
		jsonRequest.addProperty("license", SV40EDUUtil.queryJsonValueToString(jsonConfig, "enc.license"));
		jsonRequest.addProperty("zone", SV40EDUUtil.queryJsonValueToString(jsonConfig, "enc.zone"));
		jsonRequest.addProperty("maker", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.maker"));
		jsonRequest.addProperty("model", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.model"));
		jsonRequest.addProperty("serial", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.serial"));
		jsonRequest.addProperty("latitude", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.position.lat"));
		jsonRequest.addProperty("longitude", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.position.lon"));
		
		jsonRequest.addProperty("category", "EN");
		
		addLog("Requested update check for - EN");
		
		m_mcClient.sendMessage(jsonRequest.toString());
	
		return bRet;
	}
	/**
	 * @brief request latest ER download
	 * @details request latest ER download
	 */		
	public boolean requestDownloadER() {
		boolean bRet = true;

		
		// ���γ�Ʈ�� Ȯ��
		if (SV40EDUUtil.LOCALNETWORK_STATE == false) {
			String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_002, "localhost");
			addLog(msg);
			return false;
		}
		
		JsonObject jsonConfig = getConfig();
		
		JsonObject jsonRequest = new JsonObject();
		jsonRequest.addProperty("license", SV40EDUUtil.queryJsonValueToString(jsonConfig, "enc.license"));
		jsonRequest.addProperty("zone", SV40EDUUtil.queryJsonValueToString(jsonConfig, "enc.zone"));
		jsonRequest.addProperty("maker", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.maker"));
		jsonRequest.addProperty("model", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.model"));
		jsonRequest.addProperty("serial", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.serial"));
		jsonRequest.addProperty("latitude", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.position.lat"));
		jsonRequest.addProperty("longitude", SV40EDUUtil.queryJsonValueToString(jsonConfig, "ecs.position.lon"));
		
		jsonRequest.addProperty("category", "ER");
		
		SV40S101UpdateStatusReport report = new SV40S101UpdateStatusReport(s_pathOfReport);
		JsonObject jsonReport = report.toJson();
		/* remove cells property for the limit of size */
		jsonReport.add("cells", null);
		
		jsonRequest.add("report", jsonReport);
		
		addLog("Requested update check for - ER");
		m_mcClient.sendMessage(jsonRequest.toString());
		
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
	 * @brief add log
	 * @details add log
	 * @param message message to log
	 */		
	public void addLog(String message) {
		if (m_callbackView != null) {
			m_callbackView.addLog(message);
		}		
	}
	/**
	 * @brief add log
	 * @details add log
	 * @param message message to log
	 */		
	public void failedDownload() {
		addLog("Failed download");
	}
	/**
	 * @brief add log
	 * @details add log
	 * @param message message to log
	 */		
	public void cancelDownload() {
		addLog("Cancel download");
	}
	/**
	 * @brief add log
	 * @details add log
	 * @param message message to log
	 */		
	public void completeDownload(String zipFilePath,JsonObject jsonFile) {
		addLog("completed download");
		// unzip file
		try {
			addLog("starting unzip");			
			String destDir = unzip(zipFilePath, jsonFile);
			addLog("Ready to use ENC");
			SV40S101UpdateStatusReport report = new SV40S101UpdateStatusReport(s_pathOfReport);
			String pathOfCatalog = destDir+"\\"+s_nameOfCatalog;
			report.updateReportFromCatalog(pathOfCatalog);
			report.updateReportFromENCUpdateFile(jsonFile);
			report.save();
			updateProgress(0, "Ready to use");
		} catch (Exception e) {
			int pos = zipFilePath.lastIndexOf('\\');
			String file = zipFilePath.substring(pos+1);
			String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_005, file);
			addLog(msg);
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
	public String unzip(String zipFilePath,JsonObject jsonFile) throws Exception {
		JsonObject jsonConfig = getConfig();
		String destDirectory = SV40EDUUtil.queryJsonValueToString(jsonConfig, "enc.path") +"\\"+
				SV40EDUUtil.queryJsonValueToString(jsonFile, "destPath");
		
		UnzipParameters param = new UnzipParameters();
		param.setIgnoreAllFileAttributes(true);
		
		ZipFile zipFile = new ZipFile(zipFilePath);
		zipFile.setRunInThread(true);
		zipFile.extractAll(destDirectory, param);
		ProgressMonitor progressMonitor = zipFile.getProgressMonitor();
		
		boolean isCompleted = false;
		int nPrevPercent = -1;
		while (!isCompleted && progressMonitor.getState() == ProgressMonitor.STATE_BUSY) {
			int nPercent = progressMonitor.getPercentDone();
			if (nPrevPercent != nPercent) {			
				updateProgress(nPercent, String.format("unziping %d%%", nPercent));
				nPrevPercent = nPercent;
			}
			if (nPercent == 100) {
				isCompleted = true;
			} else {
				// give time to update progress bar
				try {
					Thread.sleep(50);
				} catch(Exception e) {
					
				}
				switch (progressMonitor.getCurrentOperation()) {
				case ProgressMonitor.OPERATION_NONE:
//						System.out.println("no operation being performed");
					break;
				case ProgressMonitor.OPERATION_ADD:
//						System.out.println("Add operation");
					break;
				case ProgressMonitor.OPERATION_EXTRACT:
//						System.out.println("Extract operation");
					break;
				case ProgressMonitor.OPERATION_REMOVE:
//						System.out.println("Remove operation");
					break;
				case ProgressMonitor.OPERATION_CALC_CRC:
//						System.out.println("Calculating CRC");
					break;
				case ProgressMonitor.OPERATION_MERGE:
//						System.out.println("Merge operation");
					break;
				default:
					isCompleted = true;
//						System.out.println("invalid operation");
					break;
				}
			}
		}
		
		return destDirectory;
	}	
	
	/**
	 * @brief start download
	 * @details add log
	 * @param message message to log
	 */			
	public void startDownload(JsonObject jsonMessage) {
		SV40EDUCController controller = this;
		
		m_taskDownload.setTask(new TaskManager.Task() {
			@Override
			public void work() {			
				SV40EDUCHTTPDownloader dn = new SV40EDUCHTTPDownloader(controller,  jsonMessage);
				dn.setTaskManager(m_taskDownload);
				dn.start();
			}
		
		});
		m_taskDownload.start();
		
	}
	/**
	 * @brief start maritime cloud client
	 * @details stop maritime cloud client
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
		addLog("Received update items "+ jsonResponse.size());
		String result = jsonResponse.get("result").getAsString();
		String message = jsonResponse.get("message").getAsString();
		
		if (result.equals("error")) {
			addLog("Update Error" + message);
			return;
		}
		
		if (!result.equals("ok")) {
			addLog("Update Error : Unknown");
			return;
		}
		
		// get to update 
		JsonArray jsonPackages = jsonResponse.get("packages").getAsJsonArray();
		int nSizePackages = jsonPackages.size();
		if (nSizePackages == 0) {
			addLog("ENC is already updated.");
			return;
		}
		
		// start to download
		for (int i=0; i<nSizePackages; i++) {
			startDownload((JsonObject)jsonPackages.get(i));
		}
	}		
}
