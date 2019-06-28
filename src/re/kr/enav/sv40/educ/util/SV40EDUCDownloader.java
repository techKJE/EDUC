package re.kr.enav.sv40.educ.util;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.gson.JsonObject;

import re.kr.enav.sv40.educ.controller.SV40EDUCController;

public class SV40EDUCDownloader {
	public SV40EDUCDownloader(SV40EDUCController controller, String workdir, JsonObject jsonFile,TaskManager taskManager) {
		StackTraceElement el = Thread.currentThread().getStackTrace()[1];
		URL url = null;
		String strUrl = jsonFile.get("url").getAsString();
		try {
			url = new URL(strUrl);
		} catch (MalformedURLException e) {
			controller.addLog(el, "Invalid download URL:"+strUrl);
		}
		if (url.getProtocol().toUpperCase().equals("FTP")) {
			SV40EDUCFTPDownloader dn = new SV40EDUCFTPDownloader(controller, workdir, jsonFile);
			dn.setTaskManager(taskManager);
			dn.start();					
		} else if (url.getProtocol().toUpperCase().equals("HTTP")) {
			SV40EDUCHTTPDownloader dn = new SV40EDUCHTTPDownloader(controller, workdir, jsonFile);
			dn.setTaskManager(taskManager);
			dn.start();			
		} else {
			controller.addLog(el, "Invalid download URL:"+strUrl);
		}
	}
}
