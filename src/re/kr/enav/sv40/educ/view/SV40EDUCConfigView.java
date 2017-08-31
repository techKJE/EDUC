/* -------------------------------------------------------- */
/** Config View of EDUC
File name : SV40EDUCConfigView.java 
Author : Sang Whan Oh(sang@kjeng.kr)
Creation Date : 2017-04-04
Version : v0.1
Rev. history :
Modifier : 
*/
/* -------------------------------------------------------- */

package re.kr.enav.sv40.educ.view;

import java.awt.Container;
import java.awt.GridBagConstraints;
import java.awt.GridBagLayout;
import java.awt.Insets;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.util.HashMap;

import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JComponent;
import javax.swing.JDialog;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTabbedPane;
import javax.swing.JTextField;

import com.google.gson.JsonObject;

import re.kr.enav.sv40.educ.controller.SV40EDUCController;
import re.kr.enav.sv40.educ.util.SV40EDUErrCode;
import re.kr.enav.sv40.educ.util.SV40EDUErrMessage;
import re.kr.enav.sv40.educ.util.SV40EDUUtil;
/**
 * @brief Configuration view class for EDUC
 * @details save properties
 * @author Sang Whan Oh
 * @date 2017.04.04
 * @version 0.0.1
 *
 */
public class SV40EDUCConfigView extends JDialog implements ActionListener  {
	private SV40EDUCController m_controller;
	private String m_fields[] = {
			"enc.zone", "enc.license", "enc.path", "enc.schedule",
			"ecs.position.lat", "ecs.position.lon", "ecs.ship.name", "ecs.maker", "ecs.model", "ecs.serial",
			"cloud.srcMRN", "cloud.destMRN"
		}; 	/**< name of ui component field */
	
	public HashMap<String,JComponent> m_mapComponent;
	/**
	 * @brief create configuration view
	 * @details  create configuration view
	 * @param parent parent container panel
	 * @param controller EDUC controller reference
	 */	
	public SV40EDUCConfigView(JFrame parent, SV40EDUCController controller, boolean isModal) {
		super(parent, "Configuration-EDUC", isModal);
		m_mapComponent = new HashMap<String, JComponent>(); 
		m_controller = controller;
		
		createFrame();
		setUIFromConfig(m_controller.getConfig());
		
		setVisible(true);
	}
	/**
	 * @brief set tab field from config
	 * @details  read config and set each field value
	 * @param jsonConfig configuration information
	 */
	@SuppressWarnings("unchecked")
	private void setUIFromConfig(JsonObject jsonConfig) {
		JTextField txtField;
		JComboBox<String> comboField;
		for (int i=0;i<m_fields.length;i++) {
			String field = m_fields[i];
			String value = SV40EDUUtil.queryJsonValueToString(jsonConfig, field);
			
			if (field.equals("enc.schedule")) {
				comboField = (JComboBox<String>)m_mapComponent.get(field);
				comboField.setSelectedItem(value);
			} else {
				txtField = (JTextField)m_mapComponent.get(field);
				if(txtField != null)
					txtField.setText(value);
				else {
					m_controller.addLog("Invalid Configuration Field:"+field);
				}
			}
		}
	}
	@SuppressWarnings("unchecked")
	private JsonObject getConfigFromUI() {
		JsonObject json = m_controller.loadDefaultConfig();
		
		JTextField txtField;
		JComboBox<String> comboField;
		for (int i=0;i<m_fields.length;i++) {
			String field = m_fields[i];
			String value = "";
			if (field.equals("enc.schedule")) {
				comboField = (JComboBox<String>)m_mapComponent.get(field);
				value = (String)comboField.getSelectedItem();
			} else {
				txtField = (JTextField)m_mapComponent.get(field);
				value = txtField.getText().trim();
				if (value.isEmpty()) {
					if (field.equals("enc.zone") || field.equals("enc.license")) {
						String msg = SV40EDUErrMessage.get(SV40EDUErrCode.ERR_004, field.substring(4));
						JOptionPane.showMessageDialog(null, msg, "Warning", JOptionPane.INFORMATION_MESSAGE);
						return null;
					}
				}				
			}
			SV40EDUUtil.setJsonPropertyByQuery(json, field,  value);
		}
			
		return json;
	}
	/**
	 * @brief create frame
	 * @details create frame, icon, size
	 */
	private void createFrame() {
		setSize(500, 400);
		setLocationRelativeTo(null);
		
		Container contentPane = getContentPane();
		contentPane.setLayout(new GridBagLayout());
		
		ImageIcon img = new ImageIcon("Res\\config-icon.png");
		setIconImage(img.getImage());		
		
		createTab();
		createToolbar();
		
		setResizable(false);
	}
	/**
	 * @brief create Tab
	 * @details create Tab
	 */
	private void createTab() {
		Container container = getContentPane();
		JTabbedPane pane = new JTabbedPane ();
		container.add(pane);
		
		GridBagConstraints c = new GridBagConstraints();
		c.fill = GridBagConstraints.BOTH;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx =1.0;
		c.weighty =1.0;
		c.insets = new Insets(5,5,5,5);  //top padding
		
		add(pane, c);
		
		createENCTab(pane);
		createECSTab(pane);
		createMCTab(pane);
	}
	/**
	 * @brief create toolbar
	 * @details create toolbar at the bottom
	 */
	private void createToolbar() {
		Container container = getContentPane();
		
		JPanel toolbar = new JPanel();
		
		GridBagConstraints c = new GridBagConstraints();
		c.gridx = 0;
		c.gridy = 1;
		c.insets = new Insets(0,5,5,5);  //top padding
		container.add(toolbar, c);
		
		JButton btn = new JButton("Save");
		btn.addActionListener(this);
		toolbar.add(btn);
		
		c.gridx = 1;
		c.gridy = 1;
		c.insets = new Insets(0,5,5,5);  //top padding
		
		btn = new JButton("Reset");
		btn.addActionListener(this);
		toolbar.add(btn);		
	}		
	/**
	 * @brief create ENC Tab
	 * @details create ENC Tab
	 */
	private void createENCTab(JTabbedPane container) {
		ImageIcon img = new ImageIcon("Res\\enc-icon.png");

		JPanel panel = new JPanel();
		container.addTab("ENC", img, panel, "Set download and update configuration");
		panel.setLayout(new GridBagLayout());
		// Zone
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.PAGE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.2;
		c.insets = new Insets(5,5,5,5);  //top padding
		panel.add(new JLabel("Zone*"), c);
		
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0.8;
		JTextField txtField = new JTextField();
		panel.add(txtField, c);
		m_mapComponent.put("enc.zone" , txtField);
		
		// license
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.2;
		panel.add(new JLabel("license*"), c);
		
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 0.8;
		txtField = new JTextField();
		panel.add(txtField, c);
		m_mapComponent.put("enc.license" , txtField);
		
		// ENC Path
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0.2;
		panel.add(new JLabel("ENC"), c);
		
		c.gridx = 1;
		c.gridy = 2;
		c.weightx = 0.8;
		txtField = new JTextField();
		panel.add(txtField, c);		
		m_mapComponent.put("enc.path" , txtField);
		
		// ENC Schedule
		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 0.2;
		c.weighty = 1;
		panel.add(new JLabel("Schedule"), c);
		
		c.gridx = 1;
		c.gridy = 3;
		c.weightx = 0.8;
		String[] schedule = {"day", "hour", "minute", "none"};
		JComboBox<String> combo = new JComboBox<String>(schedule);
		panel.add(combo, c);		
		m_mapComponent.put("enc.schedule" , combo);
				
	}
	/**
	 * @brief create ECS Tab
	 * @details create ECS Tab
	 */
	private void createECSTab(JTabbedPane container) {
		ImageIcon img = new ImageIcon("Res\\ecs-icon.png");

		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		container.addTab("ECS", img, panel, "Configuration for ECS");
		
		// Position
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.PAGE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.2;
		c.insets = new Insets(5,5,5,5);  //top padding
		panel.add(new JLabel("Position Lat/Lon"), c);
		
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0.9;
		JTextField txtField = new JTextField();
		panel.add(txtField, c);	
		m_mapComponent.put("ecs.position.lat" , txtField);
		
		c.gridx = 2;
		c.gridy = 0;
		c.weightx = 0.9;
		txtField = new JTextField();
		panel.add(txtField, c);		
		m_mapComponent.put("ecs.position.lon" , txtField);
		
		// Ship
		c.gridx = 0;
		c.gridy = 1;
		c.weightx = 0.2;
		panel.add(new JLabel("Ship Name"), c);
		
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 0.9;
		txtField = new JTextField();
		panel.add(txtField, c);	
		m_mapComponent.put("ecs.ship.name" , txtField);
		
		// ECS
		c.gridx = 0;
		c.gridy = 2;
		c.weightx = 0.2;
		panel.add(new JLabel("ECS Maker/Model"), c);
		
		c.gridx = 1;
		c.gridy = 2;
		c.weightx = 0.9;
		c.insets = new Insets(5,5,5,5);  //top padding
		txtField = new JTextField();
		panel.add(txtField, c);
		m_mapComponent.put("ecs.maker" , txtField);
		
		c.gridx = 2;
		c.gridy = 2;
		c.weightx = 0.9;
		txtField = new JTextField();
		panel.add(txtField, c);	
		m_mapComponent.put("ecs.model" , txtField);
		
		// ECS Serial
		c.gridx = 0;
		c.gridy = 3;
		c.weightx = 0.2;
		c.weighty = 1.0;
		panel.add(new JLabel("ECS Serial No."), c);
		
		c.gridx = 1;
		c.gridy = 3;
		c.weightx = 0.9;
		c.insets = new Insets(5,5,5,5);  //top padding
		txtField = new JTextField();
		panel.add(txtField, c);
		m_mapComponent.put("ecs.serial" , txtField);
	}
	/**
	 * @brief create Marine Cloud Tab
	 * @details create Marine Cloud Tab
	 */
	private void createMCTab(JTabbedPane container) {
		ImageIcon img = new ImageIcon("Res\\mc-icon.png");

		JPanel panel = new JPanel();
		panel.setLayout(new GridBagLayout());
		container.addTab("MC", img, panel, "Configuration for Marine Cloud");	
		
		GridBagConstraints c = new GridBagConstraints();
		c.anchor = GridBagConstraints.PAGE_START;
		c.fill = GridBagConstraints.HORIZONTAL;
		c.gridx = 0;
		c.gridy = 0;
		c.weightx = 0.1;
		c.insets = new Insets(5,5,5,5);  //top padding
		panel.add(new JLabel("Source MRN(ECS)"), c);
		
		c.gridx = 1;
		c.gridy = 0;
		c.weightx = 0.8;
		JTextField txtField = new JTextField();
		panel.add(txtField, c);			
		m_mapComponent.put("cloud.srcMRN" , txtField);
		
		c.gridx = 0;
		c.gridy = 1;
		c.weighty = 1;
		c.weightx = 0.1;
		c.insets = new Insets(5,5,5,5);  //top padding
		panel.add(new JLabel("Dest. MRN(EDUS)"), c);
		
		c.gridx = 1;
		c.gridy = 1;
		c.weightx = 0.8;
		txtField = new JTextField();
		panel.add(txtField, c);			
		m_mapComponent.put("cloud.destMRN" , txtField);
	}
	
	/**
	 * @brief button event handler
	 * @details menu event handler
	 * @param
	 */	
	public void actionPerformed(ActionEvent e) {
		String command = e.getActionCommand();
		if (command.equals("Reset")) {
			loadDefaultConfig();
		} else if (command.equals("Save")) {
			applyConfigChange();
		}
	}	
	/**
	 * @brief reset configuration
	 * @details reset configuration and load default
	 */	
	public void loadDefaultConfig() {
		JsonObject defaultConfig = m_controller.loadDefaultConfig();
		setUIFromConfig(defaultConfig);
	}	
	/**
	 * @brief apply configuration change
	 * @details save configuration and apply change
	 */		
	public void applyConfigChange() {
		JsonObject config = getConfigFromUI();
		if (config == null)
			return;
		
		m_controller.applyConfigChange(config);
		this.dispose();
	}
}
