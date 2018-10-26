package re.kr.enav.sv40.educ.util;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.net.MalformedURLException;
import java.net.URL;
import java.nio.channels.Channels;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.apache.commons.net.ftp.FTPReply;
import org.apache.commons.net.ftp.FTPClient;
import org.apache.commons.net.ftp.FTPConnectionClosedException;
import org.apache.commons.net.ftp.FTPFile;
import org.apache.commons.net.io.CopyStreamException;

import com.google.gson.JsonObject;

import re.kr.enav.sv40.educ.controller.SV40EDUCController;

public class SV40EDUCFTPDownloader extends Thread {
	static final int DOWNLOAD_DONE = 0;
	static final public int TIME_WAIT_TRYDOWNLOAD = 1000;
	static final public int TIME_WAIT_CONNECT = 1000;
	static final public int MAX_COUNT_TRYDOWNLOAD = 10;
	static final public String FTP_USER_ID = "ftpUser"/*"enavi"*/;
	static final public String FTP_USER_PASSWORD = "ftpUser"/*"enavi"*/;
	
//	static final public String FTP_USER_ID = "rivo"/*"enavi"*/;
//	static final public String FTP_USER_PASSWORD = "kjeng"/*"enavi"*/;

	protected int m_nReadTimeout = 30000;		/**< milliseconds time outs to download */
	protected int m_nUnitSizeOfSave = 1024;		/**< unit size of download request, file recorded as unit */
	
	protected SV40EDUCController m_controller;
	protected String m_strSrcUrl;
	protected String m_strDestFilePath;
	protected String m_strMD5Hash;
	protected JsonObject m_jsonFile;
	protected JsonObject m_jsonConfig;
	protected URL m_url = null;
	
	private TaskManager m_taskManager =null;		/**< reference task manager */
	
	public SV40EDUCFTPDownloader(SV40EDUCController controller, JsonObject jsonFile) {
		m_controller = controller;
		m_jsonFile = jsonFile;
		m_jsonConfig = controller.getConfig(); 
		m_strSrcUrl = m_jsonFile.get("url").getAsString();
		if (m_jsonFile.get("md5") == null)
			m_strMD5Hash = "";
		else
			m_strMD5Hash = m_jsonFile.get("md5").getAsString();
		
		try {
			m_url = new URL(m_strSrcUrl);
		} catch (MalformedURLException e) {
			controller.addLog("Invalid download URL:"+m_strSrcUrl);
			return;
		}
		
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
	public void run() {
		long nRet = 0;
		if (m_url == null) return;
		
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
		
		m_controller.addLog("Try download - " + srcUrl);
		
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
		FTPClient ftp = new FTPClient();
		ftp.setConnectTimeout(TIME_WAIT_CONNECT);
		
		String fileName = m_url.getFile();
		if (fileName.startsWith("/"))
			fileName = fileName.substring(1);
		
		if (lRet != -1) {
			try {
				output = new RandomAccessFile(file.getAbsolutePath(), "rw");
				fileSize = output.length();
				output.seek(fileSize);
			} catch (Exception e) {
				lRet = -1;
				errorCode = 2;
			}
		}

		if (lRet > -1) {
			try {
				ftp.connect(m_url.getHost(), m_url.getPort());
//				int conRep = ftp.getReplyCode();
//				if (FTPReply.isPositiveCompletion(conRep)) {
//					lRet = -1;
//				    ftp.disconnect();
//				    return lRet;
//				}
				
				boolean isLogin = ftp.login(FTP_USER_ID, FTP_USER_PASSWORD);
				if (isLogin == false) {
					lRet = -1;
				    ftp.disconnect();
				    String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_003, "FileServer");
				    m_controller.addLog(msg);
					return lRet;
				}
				
				ftp.enterLocalPassiveMode();
				
				FTPFile[] remoteFiles = ftp.listFiles(fileName);
				FTPFile remoteFile = remoteFiles[0];
				remains = remoteFile.getSize();
			
				lengthOfFile = remains + fileSize;
				 
				if (remains <= DOWNLOAD_DONE) {
					// file not found
					lRet = -1;
				} else if (remains == fileSize) {
					lRet = lengthOfFile;
				} else {
					try {
						if (fileSize < lengthOfFile) {
							m_controller.updateDownloadProgress(0, lengthOfFile);
							OutputStream os = Channels.newOutputStream(output.getChannel());
							ftp.setRestartOffset(fileSize);
							boolean bRet = ftp.retrieveFile(fileName, os);
							//boolean bRet = ftp.retrieveFile("enc_en.zip", os);
							if (bRet == true) {
								m_controller.updateDownloadProgress(lengthOfFile, lengthOfFile);
								lRet = lengthOfFile;
							}
						}
					} catch (CopyStreamException  e) {
						// file not found or failure
						lRet = -1;
						errorCode = 3;
					} catch (FTPConnectionClosedException e2) {
						lRet = -1;
						errorCode = 4;
					} catch (IOException e1) {
						// connect fail
						errorCode = 5;
						lRet = -1;					
					}
				}
			} catch (Exception e) {
				// file not found or failure
				lRet = -1;
				errorCode = 3;			
			}
		}
		
		if (output != null) {
			output.close();
		}
		if (ftp != null && ftp.isConnected()) {
			ftp.logout();
			ftp.disconnect();
		}
		
		if (errorCode != 0) {
			String msg = "";
			if (errorCode == 3)
				msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_003, "FileServer");
			else 
				msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_006, "File");
			m_controller.addLog(msg);			
			throw new Exception();
		} else {
			
			// 파일 유효성 확인
			if (strMD5Hash != "") {
				String curMD5Hash = SV40EDUUtil.getMD5(destFilePath);
				if (!strMD5Hash.equals(curMD5Hash)) {
					String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_007, "File");
					m_controller.addLog(msg);			
					throw new Exception();
				}
			}
		}
		
		return lRet;
	}
}
