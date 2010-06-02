package com.reelfx.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.MouseInfo;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.ComponentEvent;
import java.awt.event.ComponentListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.io.File;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.ImageIcon;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.Timer;
import javax.swing.border.Border;

import com.reelfx.Applet;
import com.reelfx.controller.ApplicationController;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.view.util.MessageNotification;
import com.reelfx.view.util.MoveableWindow;
import com.reelfx.view.util.ViewNotifications;

@SuppressWarnings("serial")
public class RecordControls extends MoveableWindow implements ActionListener {

	public final static String NAME = "RecordControls";

	public JButton recordBtn, closeBtn;
	public AudioSelector audioSelect;
	public JPanel titlePanel, recordingOptionsPanel, statusPanel;

	private JLabel title, status, message;
	// private JTextArea message;
	private Timer timer;
	private ApplicationController controller;
	private JFileChooser fileSelect = new JFileChooser();
	private Color backgroundColor = new Color(34, 34, 34); // new Color(230,230,230);
	private Color statusColor = new Color(255, 102, 102);
	private Color messageColor = new Color(255, 255, 153);
	private int timerCount = 0;
	private JComboBox viewportSelect;
	private ViewNotifications currentState; 

	/**
	 * The small recording GUI that provides recording, microphone, and status
	 * controls. Might be visible, or its logic might just drive an external GUI
	 * (i.e. Flash).
	 * 
	 * @param controller
	 */
	public RecordControls(ApplicationController controller) {
		super();
		setName(NAME);
		
		this.controller = controller;

		// ------- setup -------

		/*
		 * for JFrame setTitle("Review for "+Applet.SCREEN_CAPTURE_NAME);
		 * setResizable(false); setDefaultCloseOperation(HIDE_ON_CLOSE);
		 */

		setBackground(backgroundColor);
		// setPreferredSize(dim); // full screen
		// setPreferredSize(new Dimension(500, 50)); // will auto fit to the size needed, but if you want to specify a size
		// setLayout(new BorderLayout(0, 3));
		setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		setAlwaysOnTop(true);
		//addComponentListener(this);

		/*
		 * if(AWTUtilities.isTranslucencySupported(AWTUtilities.Translucency.
		 * PERPIXEL_TRANSPARENT)) {
		 * System.out.println("Transparency supported!"); }
		 */

		// ------- setup title bar -------

		title = new JLabel();
		title.setFont(new java.awt.Font("Arial", 1, 11));
		title.setForeground(Color.WHITE);
		title.setText("   Review for " + Applet.SCREEN_CAPTURE_NAME + "   ");
		title.setHorizontalAlignment(SwingConstants.CENTER);

		titlePanel = new JPanel();
		titlePanel.setBackground(backgroundColor);
		titlePanel.setLayout(new BorderLayout());
		titlePanel.add(title, BorderLayout.CENTER);

		closeBtn = new JButton("", new ImageIcon(this.getClass().getClassLoader().getResource("com/reelfx/view/images/close.png")));
		closeBtn.setBorderPainted(false);
		closeBtn.setContentAreaFilled(false);
		closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hideInterface();
			}
		});
		// closeBtn.setFont(new Font("Arial", 0, 11));
		titlePanel.add(closeBtn, BorderLayout.EAST);

		add(titlePanel); // ,BorderLayout.CENTER);

		// ------- setup recording options -------

		recordingOptionsPanel = new JPanel();
		recordingOptionsPanel.setBackground(backgroundColor);
		// recordingOptionsPanel.setMaximumSize(new Dimension(180,1000));
		recordingOptionsPanel.setOpaque(true);

		recordBtn = new JButton("Record");
		recordBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				if (recordBtn.getText().equals("Record")) {
					prepareForRecording();
				} else if (recordBtn.getText().equals("Stop")) {
					stopRecording();
				}
			}
		});
		recordBtn.setFont(new Font("Arial", 0, 11));
		recordingOptionsPanel.add(recordBtn);

		status = new JLabel();
		// status.setBackground(statusColor);
		// status.setPreferredSize(new Dimension(50, 40));
		status.setFont(new java.awt.Font("Arial", 1, 11));
		status.setForeground(Color.WHITE);
		status.setHorizontalAlignment(SwingConstants.CENTER);
		recordingOptionsPanel.add(status);

		audioSelect = new AudioSelector();
		recordingOptionsPanel.add(audioSelect);
		
		String[] resolutions = {"Custom","320x240","640x480","800x600","1024x768","1280x720","Fullscreen"};
		viewportSelect = new JComboBox(resolutions);
		viewportSelect.setSelectedIndex(3); // select the 800x600
		viewportSelect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Applet.sendViewNotification(ViewNotifications.SET_CAPTURE_VIEWPORT_RESOLUTION, ((JComboBox)e.getSource()).getSelectedItem());
			}
		});
		if(!Applet.IS_MAC || Applet.DEV_MODE) // TODO temporary
			recordingOptionsPanel.add(viewportSelect);

		add(recordingOptionsPanel); // ,BorderLayout.NORTH);

		/*
		 * statusPanel = new JPanel(); statusPanel.setOpaque(false);
		 * statusPanel.add(status); add(statusPanel); //,BorderLayout.CENTER);
		 */

		System.out.println("RecordControls initialized...");
		
		pack();
		receiveViewNotification(ViewNotifications.CAPTURE_VIEWPORT_CHANGE);
	}
/*
	public void changeState(int state) {
		changeState(state, null);
	}

	public void changeState(int state, String statusText) {
		switch (state) {

		case READY:
			recordBtn.setEnabled(true);
			recordBtn.setText("Record");
			audioSelect.setEnabled(true);
			audioSelect.setVisible(true);
			if (statusText != null) {
				status.setText(statusText);
				// statusPanel.setVisible(true);
			} else {
				status.setText("");
				// statusPanel.setVisible(false);
			}
			break;

		case PRE_RECORDING:
			recordBtn.setEnabled(false);
			audioSelect.setEnabled(false);
			audioSelect.setVisible(false);
			status.setEnabled(true);
			status.setVisible(true);
			if (statusText != null) {
				status.setText(statusText);
				// statusPanel.setVisible(true);
			} else {
				status.setText("");
				// statusPanel.setVisible(false);
			}
			break;

		case RECORDING:
			recordBtn.setEnabled(true);
			recordBtn.setText("Stop");
			audioSelect.setEnabled(false);
			audioSelect.setVisible(false);
			if (statusText != null) {
				status.setText(statusText);
				// statusPanel.setVisible(true);
			} else {
				status.setText("");
				// statusPanel.setVisible(false);
			}
			break;

		}
		currentState = state; // needs to be at end
		if (isVisible())
			pack();

		Applet.handleRecordingUpdate(state, statusText);
	}
*/	
	@Override
	public void receiveViewNotification(ViewNotifications notification, Object body) {
		switch(notification) {
		
		case READY:
		case READY_WITH_OPTIONS:
		case READY_WITH_OPTIONS_NO_UPLOADING:
			recordBtn.setEnabled(true);
			recordBtn.setText("Record");
			audioSelect.setEnabled(true);
			audioSelect.setVisible(true);
			viewportSelect.setEnabled(true);
			viewportSelect.setVisible(true);
			currentState = notification;
			if (body instanceof MessageNotification) {
				status.setText(((MessageNotification) body).getStatusText());
			} else {
				status.setText("");
			}
			Applet.handleRecordingUpdate(currentState, status.getText());
			break;
		
		case PRE_RECORDING:
			recordBtn.setEnabled(false);
			audioSelect.setEnabled(false);
			audioSelect.setVisible(false);
			viewportSelect.setEnabled(false);
			viewportSelect.setVisible(false);
			status.setEnabled(true);
			status.setVisible(true);
			currentState = notification;
			if (body instanceof MessageNotification) {
				status.setText(((MessageNotification) body).getStatusText());
			} else {
				status.setText("");
			}
			Applet.handleRecordingUpdate(currentState, status.getText());
			break;
		
		case RECORDING:
			recordBtn.setEnabled(true);
			recordBtn.setText("Stop");
			audioSelect.setEnabled(false);
			audioSelect.setVisible(false);
			viewportSelect.setEnabled(false);
			viewportSelect.setVisible(false);
			currentState = notification;
			if (body instanceof MessageNotification) {
				status.setText(((MessageNotification) body).getStatusText());
			} else {
				status.setText("");
			}
			Applet.handleRecordingUpdate(currentState, status.getText());
			break;
		
		case CAPTURE_VIEWPORT_CHANGE:
			Point pt = Applet.CAPTURE_VIEWPORT.getBottomLeftPoint();
			pt.translate(0, 10);
			if(pt.y + getHeight() > screen.getHeight() || (Applet.IS_MAC && !Applet.DEV_MODE)) { // TODO temporary (second condition)
				pt = new Point((int) (screen.getWidth() - 10 - getWidth()), 10 + (Applet.IS_MAC ? 20 : 0));
			}
			setLocation(pt);
			break;
		
		case SHOW_ALL:
		case SHOW_RECORD_CONTROLS:
			setVisible(true);
			break;
			
		case HIDE_ALL:
		case HIDE_RECORD_CONTROLS:
			setVisible(false);
			break;
			
		case FATAL:
		case THINKING:
			recordBtn.setEnabled(false);
			audioSelect.setEnabled(false);
			audioSelect.setVisible(true);
			viewportSelect.setEnabled(false);
			viewportSelect.setVisible(true);
			currentState = notification;
			if (body instanceof MessageNotification) {
				status.setText(((MessageNotification) body).getStatusText());
			} else {
				status.setText("");
			}
			Applet.handleRecordingUpdate(currentState, status.getText());
			break;	
			
		case MOUSE_DRAG_CROP_HANDLE:
			viewportSelect.setSelectedIndex(0);
			break;
		}
		
		if (isVisible()) pack();		
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if((!Applet.IS_MAC || Applet.DEV_MODE) && // temporary 
				(currentState == ViewNotifications.PRE_RECORDING || currentState == ViewNotifications.RECORDING)) return;
		super.mousePressed(e);
		Applet.sendViewNotification(ViewNotifications.MOUSE_PRESS_RECORD_CONTROLS, e);
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if((!Applet.IS_MAC || Applet.DEV_MODE) && // temporary 
				(currentState == ViewNotifications.PRE_RECORDING || currentState == ViewNotifications.RECORDING)) return;
		super.mouseDragged(e);
		Applet.sendViewNotification(ViewNotifications.MOUSE_DRAG_RECORD_CONTROLS, e);
	}

	public void prepareForRecording() {
		// do I need permission to delete a file first?
		if (ScreenRecorder.OUTPUT_FILE.exists() && !deleteRecording())
			return;

		Applet.sendViewNotification(ViewNotifications.PRE_RECORDING, new MessageNotification("Ready"));
		//changeState(PRE_RECORDING, "Ready");
		controller.prepareForRecording();

		if (timer == null) {
			timer = new Timer(1000, this);
			timer.start(); // calls actionPerformed
		} else
			timer.restart(); // calls actionPerformed

		/*
		 * countdown.setVisible(true); countdown.pack();
		 */
	}
	
	@Override
	public void actionPerformed(ActionEvent e) { // this method is for the timer
		if (status.getText().equals("Ready")) {
			Applet.sendViewNotification(ViewNotifications.PRE_RECORDING, new MessageNotification("Set"));
			//changeState(PRE_RECORDING, "Set");
		} else if (status.getText().equals("Set")) {
			Applet.sendViewNotification(ViewNotifications.RECORDING, new MessageNotification("Go!"));
			//changeState(RECORDING, "Go!");
			startRecording();
		} else {
			status.setText((timerCount / 60 < 10 ? "0" : "") + timerCount
							/ 60 + ":" + (timerCount % 60 < 10 ? "0" : "")
							+ timerCount % 60);
			Applet.handleRecordingUpdate(currentState, status.getText());
			timerCount++;
		}
	}
	
	private void startRecording() {
		controller.startRecording(audioSelect.getSelectedAudioRecorder());
	}

	public void stopRecording() {
		timer.stop();
		timerCount = 0;
		controller.stopRecording();
	}

	public void previewRecording() {
		controller.previewRecording();
	}

	public void saveRecording() {
		int returnVal = fileSelect.showSaveDialog(this);
		if (returnVal == JFileChooser.APPROVE_OPTION) {
			File file = fileSelect.getSelectedFile();
			controller.saveRecording(file);
		}
	}

	public void postRecording() {
		controller.postRecording();
	}

	/**
	 * @return boolean that says whether to continue with whatever action called
	 *         this or not
	 */
	public boolean deleteRecording() {
		Applet.sendViewNotification(ViewNotifications.DISABLE_ALL);
		int n = JOptionPane.showConfirmDialog(null,
				"Are you sure that you are done with this screen recording?",
				"Are you sure?", JOptionPane.YES_NO_OPTION);
		Applet.sendViewNotification(ViewNotifications.SHOW_ALL);
		if (n == JOptionPane.YES_OPTION) {
			controller.deleteRecording();
			return true;
		} else {
			return false;
		}
	}

	public void hideInterface() {
		setVisible(false);
		Applet.sendViewNotification(ViewNotifications.HIDE_ALL);
		Applet.handleRecordingUIHide();
	}

	@Override
	public void dispose() {
		super.dispose();
		audioSelect.destroy();
	}
}
