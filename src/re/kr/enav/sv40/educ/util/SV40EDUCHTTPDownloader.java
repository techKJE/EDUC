/* -------------------------------------------------------- */
/** HTTP Download of EDUC
File name : SV40EDUCHTTPDownloader.java 
Author : Sang Whan Oh(sang@kjeng.kr)
Creation Date : 2017-04-12
Version : v0.1
Rev. history :
Modifier : 
*/
/* -------------------------------------------------------- */
package re.kr.enav.sv40.educ.util;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.HttpURLConnection;
import java.net.SocketException;
import java.net.URL;
import java.net.URLConnection;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import com.google.gson.JsonObject;

import re.kr.enav.sv40.educ.controller.SV40EDUCController;
/**
 * @brief EDUC HTTP Download 를 실행하는 클래스
 * @details ENC 다운로드 할 대상(Json)을 읽어 HTTP 다운로드를 실행함
 * @author Sang Whan Oh
 * @date 2017.04.03
 * @version 0.0.1
 *
 */
public class SV40EDUCHTTPDownloader extends Thread {
	static final int DOWNLOAD_DONE = 0;
	static final public int TIME_WAIT_TRYDOWNLOAD = 1000;
	static final public int MAX_COUNT_TRYDOWNLOAD = 10;

	protected int m_nReadTimeout = 30000;		/**< milliseconds time outs to download */
	protected int m_nUnitSizeOfSave = 1024;		/**< unit size of download request, file recorded as unit */
	
	protected SV40EDUCController m_controller;
	protected String m_strSrcUrl;
	protected String m_strDestFilePath;
	protected String m_strMD5Hash;
	protected JsonObject m_jsonFile;
	protected JsonObject m_jsonConfig;
	
	private TaskManager m_taskManager =null;		/**< reference task manager */
	
	public SV40EDUCHTTPDownloader(SV40EDUCController controller, JsonObject jsonFile) {
		m_controller = controller;
		m_jsonFile = jsonFile;
		m_jsonConfig = controller.getConfig(); 
		m_strSrcUrl = m_jsonFile.get("url").getAsString();
		if (m_jsonFile.get("md5") == null)
			m_strMD5Hash = "";
		else
			m_strMD5Hash = m_jsonFile.get("md5").getAsString();
		
		String strFileName = m_jsonFile.get("fileName").getAsString();
		m_strDestFilePath = SV40EDUUtil.queryJsonValueToString(m_jsonConfig, "enc.path")+"\\temp\\"+strFileName;
	}
	/**
	 * @brief setter for TaskManager
	 * @param taskManager task work manager
	 */		
	public void setTaskManager(TaskManager taskManager) {
		this.m_taskManager = taskManager;
		taskManager.m_threadTask = this;
	}
	public SV40EDUCHTTPDownloader(SV40EDUCController controller, int nUnitSize, long timeOut) {
		m_controller = controller;
	}
	public void run() {
		long nRet = 0;
		
		try {
			nRet = download(m_strSrcUrl, m_strDestFilePath, m_strMD5Hash);
			if (nRet > 0) {
				m_controller.completeDownload(m_strDestFilePath, m_jsonFile);
				m_controller.setDownComplete(true);
				m_taskManager.completed();
				return;
			}
		} catch (Exception e) {
			if (m_taskManager.retryable() == true)
				m_taskManager.failed();	// delegate to the TaskManager
			else
				m_controller.setDownComplete(true);
		}
	}
	
	/**
	 * @brief download to destination
	 * @details download to destination
	 * @param srcUrl http(s) url of file located
	 * @param destFilePath file destination to save
	 * @return if file not found -1, download completed return size of file, if need more download try return 0.
	 */			
	private long download(String srcUrl, String destFilePath, String strMD5Hash) throws Exception {
		long lRet = 0;
		long fileSize = 0, remains, lengthOfFile = 0;
		int errorCode = 0;
		StackTraceElement el = Thread.currentThread().getStackTrace()[1];
		
		m_controller.addLog(el, "Try download - " + srcUrl);
		
		File file = new File(destFilePath);
		if (file.exists() == false) {
		    try {
		    	Path pathToFile = Paths.get(destFilePath);
		    	Files.createDirectories(pathToFile.getParent());
		    	Files.createFile(pathToFile);
			} catch (IOException e) {
				lRet = -1;
				errorCode = 1;
			}
		}
		
		RandomAccessFile output = null;
		URL url = null;
		URLConnection conn = null;
		InputStream input = null;
		
		if (lRet != -1) {
			try {
				output = new RandomAccessFile(file.getAbsolutePath(), "rw");
				fileSize = output.length(); // 현재 파일 크기
				fileSize = 0; // 이어받기 x
				output.seek(fileSize); 
			} catch (Exception e) {
				lRet = -1;
				errorCode = 2;
			}
		}
		
		if (lRet != -1) {
			try {
				//srcUrl = srcUrl.replaceAll(" ", "%20");
				url = new URL(srcUrl);
				conn = url.openConnection();

//				int code = conn.getResponseCode();
//				String msg = String.format("response code: %d", code);
//				m_controller.addLog(msg);

				conn.setRequestProperty("Accept-Ranges", "bytes");
				conn.setRequestProperty("Range", "bytes=" + String.valueOf(fileSize) + '-');
				conn.connect();
			} catch (SocketException se) {
				// connect fail
				errorCode = 3;
				lRet = -2;
			} catch (Exception e) {
				// file not found or failure
				lRet = -1;
				errorCode = 4;
			}
		}
		
		if (lRet > -1) {
			conn.setConnectTimeout(m_nReadTimeout);
			conn.setReadTimeout(m_nReadTimeout);
			remains = conn.getContentLengthLong();
			
			lengthOfFile = remains + fileSize;
			 
			if (remains <= DOWNLOAD_DONE) {
				// file not found
				lRet = -1;
			} else if (remains == fileSize) {
				lRet = lengthOfFile;
			} else {
				try {
					input = conn.getInputStream();
					lRet = -1;
					byte data[] = new byte[m_nUnitSizeOfSave];
					int count = 0;
					
					if (fileSize < lengthOfFile) {
						m_controller.updateDownloadProgress(0, lengthOfFile);
						while((count = input.read(data)) != -1) {
							output.write(data, 0, count);
							lRet = lRet==-1?lengthOfFile-remains:lRet;
							lRet += count;
							m_controller.updateDownloadProgress(lRet, lengthOfFile);
						}
					}
				} catch (IOException e) {
					// does not complete download
					lRet = 0;
					errorCode = 5;
				}
			}
		}
		try {
			if (output != null) output.close();
		} catch (IOException e) {
		}
		try {
			if (input != null) input.close();
		} catch (IOException e) {
		}
		
		if (errorCode != 0) {
			String msg = "";
			if (errorCode == 3)
				msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_003, "FileServer");
			else 
				msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_006, "File");
			m_controller.addLog(el, msg);			
			throw new Exception();
		} else {
			// 파일 유효성 확인
			if (strMD5Hash != "") {
				String curMD5Hash = SV40EDUUtil.getMD5(destFilePath);
				if (!strMD5Hash.equals(curMD5Hash)) {
					String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_007, "File");
					m_controller.addLog(el, msg);			
					throw new Exception();
				}
			}
		}
		
		return lRet;
	}
}
