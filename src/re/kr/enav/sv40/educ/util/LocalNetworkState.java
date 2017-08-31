package re.kr.enav.sv40.educ.util;

import re.kr.enav.sv40.educ.controller.SV40EDUCController;

public class LocalNetworkState extends Thread {
	private SV40EDUCController m_controller;
	private long m_nDuration = 30;
	
	/**
	 * @brief LocalNetworkState ������
	 * @param controller EDUC ��Ʈ�ѷ�
	 * @param nDuration �ݺ� ���� �ֱ�
	 */	
	public LocalNetworkState(SV40EDUCController controller, long duration) {
		this.m_controller = controller;
		this.m_nDuration = duration;
	}
	
	/**
	 * @brief local network state
	 */	
	public void run() {
		try
		{
			while (true) {
				String addr = SV40EDUUtil.getLocalAddr();
				boolean curstate = (addr != null)? SV40EDUUtil.ping(addr,5000):false;
				
				String msg = (curstate == true)? "localhost alive" : SV40EDUErrMessage.get(SV40EDUErrCode.ERR_002, "localhost");
				if (curstate != SV40EDUUtil.LOCALNETWORK_STATE) {
					SV40EDUUtil.LOCALNETWORK_STATE = curstate;
					m_controller.addLog(msg);
				}
				
				Thread.sleep(m_nDuration);
			}
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
