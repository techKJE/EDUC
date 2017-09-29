package re.kr.enav.sv40.educ.util;

import java.net.MalformedURLException;
import java.net.URL;

import com.google.gson.JsonObject;

import re.kr.enav.sv40.educ.controller.SV40EDUCController;

public class SV40EDUCDownloader {
	public SV40EDUCDownloader(SV40EDUCController controller, JsonObject jsonFile,TaskManager taskManager) {
		URL url = null;
		String strUrl = jsonFile.get("url").getAsString();
		try {
			url = new URL(strUrl);
		} catch (MalformedURLException e) {
			controller.addLog("Invalid download URL:"+strUrl);
		}
		if (url.getProtocol().toUpperCase().equals("FTP")) {
			SV40EDUCFTPDownloader dn = new SV40EDUCFTPDownloader(controller,  jsonFile);
			dn.setTaskManager(taskManager);
			dn.start();					
		} else if (url.getProtocol().toUpperCase().equals("HTTP")) {
			SV40EDUCHTTPDownloader dn = new SV40EDUCHTTPDownloader(controller,  jsonFile);
			dn.setTaskManager(taskManager);
			dn.start();			
		} else {
			controller.addLog("Invalid download URL:"+strUrl);
		}
	}
}
