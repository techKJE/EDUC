/* -------------------------------------------------------- */
/** ENC update unit test
File name : TestUnit.java 
Author : Sang Whan Oh(sang@kjeng.kr)
Creation Date : 2017-04-17
Version : v0.1
Rev. history :
Modifier : 
*/
/* -------------------------------------------------------- */
package re.kr.enav.sv40.educ.test;

import static org.junit.Assert.*;

import org.junit.Test;

import com.google.gson.JsonObject;

import re.kr.enav.sv40.educ.util.SV40S101UpdateStatusReport;
/**
 * @brief EDUC Update ���� ���� Ŭ����
 * @details �ٿ�ε� ���� ENC S-101 īŻ�α� �Ľ� ���� Ŭ����
 * @author Sang Whan Oh
 * @date 2017.04.03
 * @version 0.0.1
 *
 */
public class TestUpdate {

	@Test
	public void parseCatalog() {
		JsonObject json = null;
		SV40S101UpdateStatusReport report= new SV40S101UpdateStatusReport(".");
		try {
			json = report.parseMetadata("Res\\S101ed1.xml");
		} catch (Exception e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		assertNotEquals(json, null);
	}

}
