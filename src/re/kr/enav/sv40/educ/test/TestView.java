/* -------------------------------------------------------- */
/** View Test
File name : TestView.java
Author : Sang Whan Oh(sang@kjeng.kr)
Creation Date : 2017-04-17
Version : v0.1
Rev. history :
Modifier : 
*/
/* -------------------------------------------------------- */
package re.kr.enav.sv40.educ.test;

import static org.junit.Assert.*;

import javax.swing.JTextField;

import org.junit.Test;

import com.google.gson.JsonObject;

import re.kr.enav.sv40.educ.controller.SV40EDUCController;
import re.kr.enav.sv40.educ.view.SV40EDUCConfigView;
import re.kr.enav.sv40.educ.view.SV40EDUCMainView;
/**
 * @brief EDUC View Unit Test 클래스
 * @details 설정값 저장, 불러오기, 기본값 설정 기능 시험
 * @author Sang Whan Oh
 * @date 2017.04.03
 * @version 0.0.1
 *
 */
public class TestView {

	@Test
	public void loadDefaultConfig() {		
		SV40EDUCController controller = new SV40EDUCController();
		JsonObject json = controller.loadDefaultConfig();
		assertNotEquals(null, json);
	}
	@Test
	public void loadAndSaveConfig() {		
		SV40EDUCController controller = new SV40EDUCController();
		JsonObject json = controller.loadDefaultConfig();
		controller.saveConfig(json);
		JsonObject json2 = controller.loadConfig();
		
		assertTrue(json.equals(json2));
	}
	@Test
	public void configInputChange() {		
		SV40EDUCController controller = new SV40EDUCController();
		SV40EDUCMainView mainView = new SV40EDUCMainView(controller);
		// show modal less
		SV40EDUCConfigView configView = mainView.showConfig(false);	
		
		// change value
		JTextField txtField = (JTextField)configView.m_mapComponent.get("enc.zone");
		txtField.setText("5678");
		// apply change
		configView.applyConfigChange();
		
		JsonObject json = new SV40EDUCController().loadConfig();
		
		assertEquals("5678", json.get("enc").getAsJsonObject().get("zone").getAsString());
	}	
}
