/* -------------------------------------------------------- */
/** Download unit test
File name : TestDownload.java 
Author : Sang Whan Oh(sang@kjeng.kr)
Creation Date : 2017-04-17
Version : v0.1
Rev. history :
Modifier : 
*/
/* -------------------------------------------------------- */
package re.kr.enav.sv40.educ.test;

import static org.junit.Assert.*;

import java.io.File;

import org.junit.Test;

import com.google.gson.JsonArray;
import com.google.gson.JsonObject;

import re.kr.enav.sv40.educ.controller.SV40EDUCController;
import re.kr.enav.sv40.educ.util.SV40EDUCHTTPDownloader;
import re.kr.enav.sv40.educ.util.SV40EDUUtil;
import re.kr.enav.sv40.educ.view.SV40EDUCMainView;
/**
 * @brief EDUC Download unit test 클래스
 * @details 다운로드 시험 클래스
 * @author Sang Whan Oh
 * @date 2017.04.03
 * @version 0.0.1
 *
 */
public class TestDownload {

	@Test
	public void downloadHTTP() throws Exception {		
		String destFile = "c:\\temp\\enc.zip";
		
		// prepare empty file
		File f = new File(destFile);
		if (f.exists()) f.delete();
		
		SV40EDUCController controller = new SV40EDUCController();
		new SV40EDUCMainView(controller);	
		SV40EDUCHTTPDownloader dn = new SV40EDUCHTTPDownloader(controller, 1024, 30000);
		dn.start();
		
		JsonArray jsonFiles= SV40EDUUtil.getJsonArrayFromFile("Res\\download_en_sample.json");
		JsonObject jsonFile = jsonFiles.get(0).getAsJsonObject();
		
		controller.setCallbackProgress(new SV40EDUCController.CallbackView() {
			@Override
			public void updateProgress(int nPercent, String label) {
				if (nPercent == 100) {
					File f = new File(destFile);
					long fileLength = f.length();
					long orgFileLength = jsonFile.get("fileSize").getAsLong();
					assertEquals(orgFileLength, fileLength);
				}
			}
			@Override
			public void addLog(String message) {
			}
			@Override 
			public void failDownload(JsonObject jsonFile) {
				assertTrue(false);
			}
		});
		
		dn.start();
	}
}
