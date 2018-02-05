/* -------------------------------------------------------- */
/** TaskManager of EDUC
File name : TaskManager.java 
Author : Sang Whan Oh(sang@kjeng.kr)
Creation Date : 2017-08-01
Version : v0.1
Rev. history :
Modifier : 
*/
/* -------------------------------------------------------- */
package re.kr.enav.sv40.educ.util;
/**
 * @brief 비동기 통신 작업 반복 수행 관리자
 * @details  Http Download, MC 송수신 재실행 관리자
 * @author Sang Whan Oh
 * @date 2017.08.01
 * @version 0.0.1
 */
public class TaskManager {
	private Task m_task; 					/**< Task reference */
	private long m_maxRetryCount = 3;		/**< maximum retry count when task is failed */
	private long m_nRetryCount = 0;			/**< count of retry to task work */
	private long m_nRetryDuration = 30;		/**< interval duration of retry, unit ms */
	public Thread m_threadTask = null;				/**< retry thread controller */

	/**
	 * @brief Task Manager 생성자
	 * @param nMaxRetryCount 반복 실행 횟수
	 * @param nRetryDuration 반복 실행 주기
	 */		
	public TaskManager(long nMaxRetryCount, long nRetryDuration) {
		this.m_nRetryCount = nMaxRetryCount;
		this.m_nRetryDuration = nRetryDuration;
	}
	/**
	 * @brief Task Manager 생성자
	 * @param nMaxRetryCount 반복 실행 횟수
	 * @param nRetryDuration 반복 실행 주기
	 */		
	public interface Task {
		void work();
	}	
	/**
	 * @brief Task Setter
	 * @param task 반복 실행할 작업
	 */		
	public void setTask(Task task) {
		this.m_task = task;
	}
	/**
	 * @brief Call back when task is completed
	 */		
	public void completed() {
		System.out.println("Completed");
	}
	
	/**
	 * @brief retry availabitity
	 */	
	public boolean retryable() {
		return (m_nRetryCount+1 < m_maxRetryCount)? true:false;
	}
	
	/**
	 * @brief Call back when task is failed, retry
	 */		
	public void failed() {
		if (m_nRetryCount <= m_maxRetryCount) {
			m_nRetryCount++;
			if (m_threadTask != null) {
				m_threadTask.interrupt();
				try {
					m_threadTask.join();
				} catch(Exception e) {
					e.printStackTrace();
				}
			}
			m_threadTask = new Thread(
				new Runnable() {
					public void run() {
						try {
							Thread.sleep(m_nRetryDuration);
							m_task.work();
						} catch (InterruptedException e) {
						}
					}
				}
			);
			m_threadTask.start();
		}
	}
	/**
	 * @brief Task 작업 시작
	 */		
	public void start() {
		m_nRetryCount = 0;
		m_task.work();
	}
	
	public void stop() {
		if (m_threadTask != null) {
			m_threadTask.interrupt();
			try {
				m_threadTask.join();
			} catch(Exception e) {
				e.printStackTrace();
			}
		}
	}
}
