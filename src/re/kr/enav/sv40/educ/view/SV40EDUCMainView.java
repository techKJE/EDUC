/* -------------------------------------------------------- */
/** Main View of EDUC
File name : SV40EDUCMainView.java 
Author : Sang Whan Oh(sang@kjeng.kr)
Creation Date : 2017-04-03
Version : v0.1
Rev. history :
Modifier : 
*/
/* -------------------------------------------------------- */

package re.kr.enav.sv40.educ.view;
import java.awt.Container;
import java.awt.Dimension;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.KeyEvent;
import java.text.SimpleDateFormat;
import java.util.Date;
import javax.swing.DefaultListModel;
import javax.swing.ImageIcon;
import javax.swing.JFrame;
import javax.swing.JList;
import javax.swing.JMenu;
import javax.swing.JMenuBar;
import javax.swing.JMenuItem;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JScrollPane;
import javax.swing.SwingUtilities;
import com.google.gson.JsonObject;

import kr.ac.kaist.mms_client.MMSConfiguration;
import re.kr.enav.sv40.educ.controller.SV40EDUCController;

import org.apache.log4j.*;

/**
 * @brief EDUC 의 뷰를 위한 클래스
 * @details View 컴포넡트를 초기화
 * @author Sang Whan Oh
 * @date 2017.04.03
 * @version 0.0.1
 *
 */
public class SV40EDUCMainView extends JFrame implements ActionListener {
	private static final String MMS_KEY = "-mms";
	
	private static String s_MENU_DNEN = "Download EN";
	private static String s_MENU_DNER = "Download ER";
	private static String s_MENU_ZONE = "Download ZONE";
	
	private static String s_MENU_CLEARLOG = "Clear Log";
	private static String s_MENU_CONFIG = "Config";

	private static final Logger m_logger = Logger.getLogger(SV40EDUCMainView.class);
	
	SimpleDateFormat m_ftDate = new SimpleDateFormat ("yyyy.MM.dd hh:mm:ss");	/**< date format for logging message */
	
	private SV40EDUCController m_controller;
	private DefaultListModel<String> m_listModel;
	JProgressBar m_progressBar;	/**< progress bar for download */
	/**
	 * @brief create main view
	 * @details  create main view
	 * @param controller EDUC controller reference
	 */	
	public SV40EDUCMainView(SV40EDUCController controller) {
		super("ENC Download Update Client-EDUC");
		m_controller = controller;
		SV40EDUCMainView view  = this;
		
		m_controller.setCallbackProgress(
			new SV40EDUCController.CallbackView() {
				@Override
				public void updateProgress(int nPercent, String label) {
					view.updateProgress(nPercent, label);
				}
				@Override
				public void addLog(StackTraceElement el, String message) {
					view.addLog(el, message);
				}
				@Override
				public void debugLog(StackTraceElement el, String message) {
					view.debugLog(el, message);
				}
				@Override 
				public void failDownload(JsonObject jsonFile) {
					StackTraceElement el = Thread.currentThread().getStackTrace()[1];
					view.addLog(el, "Failed to download");
				}				
			}
		);
		
		createFrame();
		createMenu();
		createLogView();
		createStatusBar();
		
		setVisible(true);
		
		StackTraceElement el = Thread.currentThread().getStackTrace()[1];
		addLog(el, "Application is launched successfuly");
	}
	
	/**
	 * @brief menu event handler
	 * @details menu event handler
	 * @param
	 */	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals(s_MENU_DNEN)) {
			m_controller.requestDownloadEN();
		} else if (command.equals(s_MENU_DNER)) {
			m_controller.requestDownloadER();
		} else if (command.equals(s_MENU_ZONE)) {
			m_controller.requestDownloadZone();	
		} else if (command.equals(s_MENU_CLEARLOG)) {
			clearLog();
		} else if (command.equals(s_MENU_CONFIG)) {
			showConfig(true);
		}
	}

	/**
	 * @brief create frame
	 * @details create frame, icon, size
	 */
	private void createFrame() {
		setSize(500, 400);
		setLocationRelativeTo(null);
		setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
		
		Container contentPane = this.getContentPane();
		contentPane.setLayout(new GridBagLayout());
		JPanel pane = new JPanel();
		contentPane.add(pane);
		
		ImageIcon img = new ImageIcon("Res\\map-icon.png");
		setIconImage(img.getImage());		
		
		setResizable(false);
	}

	/**
	 * @brief create menu bar
	 * @details create menu bar
	 */
	private void createMenu() {
		JMenuBar menuBar;
		JMenu menu;
		JMenuItem menuItem;
		
		//Create the menu bar.
		menuBar = new JMenuBar();

		//ENC
		menu = new JMenu("ENC");
		menu.setMnemonic(KeyEvent.VK_A);
		menu.getAccessibleContext().setAccessibleDescription(
		        "The only menu in this program that has menu items");
		menuBar.add(menu);

		menuItem = new JMenuItem(s_MENU_DNEN);
		menuItem.getAccessibleContext().setAccessibleDescription(
		        "download EN ENC from eNavi center");
		menuItem.addActionListener(this);
		menu.add(menuItem);

		menuItem = new JMenuItem(s_MENU_DNER);
		menuItem.getAccessibleContext().setAccessibleDescription(
		        "download ER ENC from eNavi center");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		
		menuItem = new JMenuItem(s_MENU_ZONE);
		menuItem.getAccessibleContext().setAccessibleDescription(
		        "download ZONE INFO from eNavi center");
		menuItem.addActionListener(this);
		menu.add(menuItem);
		
		// Tool
		menu = new JMenu("Tool");
		menu.getAccessibleContext().setAccessibleDescription(
		        "The only menu in this program that has menu items");
		menuBar.add(menu);

		menuItem = new JMenuItem(s_MENU_CLEARLOG);
		menuItem.addActionListener(this);
		menuItem.getAccessibleContext().setAccessibleDescription(
		        "Clear all list in the log");
		menu.add(menuItem);

		menuItem = new JMenuItem(s_MENU_CONFIG);
		menuItem.addActionListener(this);
		menuItem.getAccessibleContext().setAccessibleDescription(
		        "Configure the setting");
		menu.add(menuItem);
		
		setJMenuBar(menuBar);		
	}
	/**
	 * @brief create log view
	 * @details create log view
	 * @param
	 */
	private void createLogView() {
		m_listModel = new DefaultListModel<String>();
		JList<String> list = new JList<String>(m_listModel);
		
		JScrollPane areaScrollPane = new JScrollPane(list);

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx =1.0;
		c.weighty =1.0;
		c.insets = new Insets(5,5,0,5);  //top padding
		
		this.add(areaScrollPane, c);
	}
	/**
	 * @brief create download progress bar
	 * @details create download progress bar
	 * @param
	 */
	private void createStatusBar() {
		m_progressBar = new JProgressBar() {
		    @Override
		    public Dimension getPreferredSize() {
		    	
		        return new Dimension(this.getParent().getSize().width, 100);
		    }
		};
		
		m_progressBar = new JProgressBar();
		m_progressBar.setMaximum(100);
		m_progressBar.setMinimum(0);
		m_progressBar.setValue(0);
		m_progressBar.setString("");

		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 1;
		c.insets = new Insets(5,5,5,5);  //top padding

		m_progressBar.setPreferredSize(new Dimension(0,30));
		m_progressBar.setStringPainted(true);
		this.add(m_progressBar,c);
	}
	
	/**
	 * @brief append message to log list
	 * @details insert message to top log list with date
	 * @param message to append log
	 */
	public void addLog(StackTraceElement el, String message) {
		debugLog(el, message);
		
		Date now = new Date( );
		String log = String.format("%s %s", m_ftDate.format(now), message);
		
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				m_listModel.insertElementAt(log, 0);
			}
		});
	}
	
	public void debugLog(StackTraceElement el, String message) {
		String msg = message;
		if (el != null)
			msg = String.format("(%s:%d) %s", el.getFileName(), el.getLineNumber(), message);
		
		m_logger.debug(msg);
	}
	
	/**
	 * @brief clear log
	 * @details clear all log in the box
	 * @param
	 */
	public void clearLog() {
		Object[] options = { "Yes", "No" };
	    int response = JOptionPane.showOptionDialog(this,
	            "Confirm to delete all log in the list?", "Notification",
	            JOptionPane.YES_NO_OPTION, JOptionPane.QUESTION_MESSAGE, null,
	            options, options[1]);
	    
	    if (response == JOptionPane.OK_OPTION){
	    	m_listModel.removeAllElements();
	    }
	}
	/**
	 * @brief update progress status
	 * @details show configuration dialog
	 * @param percent progress 0 - 100
	 * @param message lable of progress bar
	 */
	public void updateProgress(int percent, String message) {
		SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				m_progressBar.setValue(percent);
				m_progressBar.setString(message);
			}
		});
	}	
	/**
	 * @brief show configuration dialog
	 * @details show configuration dialog
	 */
	public SV40EDUCConfigView showConfig(boolean isModal) {
		return new SV40EDUCConfigView(this, m_controller, isModal);
	}	
	
	/**
	 * @brief main view test
	 * @details main view test
	 */
	public static void main(String args[]) {
		//MMSConfiguration.MMS_URL="143.248.55.83:8088";
		MMSConfiguration.MMS_URL="www.mms-kaist.com:8088";
		for(int i=0; i<args.length; i+=2)
	    {
	        String key = args[i];
	        String value = args[i+1];

	        switch (key)
	        {
	            case MMS_KEY : MMSConfiguration.MMS_URL = value;
	            break;
	        }
	    }
		
		PropertyConfigurator.configure(SV40EDUCController.s_log4Config);
		
		new SV40EDUCMainView(new SV40EDUCController());
	}
}
