package com.reelfx.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
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
import javax.swing.border.LineBorder;

import org.apache.log4j.Logger;

import com.reelfx.Applet;
import com.reelfx.controller.AbstractController;
import com.reelfx.model.AttributesManager;
import com.reelfx.model.PreferencesManager;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.view.util.MessageNotification;
import com.reelfx.view.util.MoveableWindow;
import com.reelfx.view.util.ViewNotifications;

/**
 * Interface shown while recording that displays controls and recording time/status.
 * 
 * @author Daniel Dixon (http://www.danieldixon.com)
 * 
 * 	Copyright (C) 2010  ReelFX Creative Studios (http://www.reelfx.com)
 *
 *	This program is free software: you can redistribute it and/or modify
 * 	it under the terms of the GNU General Public License as published by
 * 	the Free Software Foundation, either version 3 of the License, or
 * 	(at your option) any later version.
 * 	
 * 	This program is distributed in the hope that it will be useful,
 * 	but WITHOUT ANY WARRANTY; without even the implied warranty of
 * 	MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * 	GNU General Public License for more details.
 * 	
 * 	You should have received a copy of the GNU General Public License
 * 	along with this program.  If not, see <http://www.gnu.org/licenses/>
 *
 */
@SuppressWarnings("serial")
public class RecordControls extends MoveableWindow implements ActionListener {

	public final static String NAME = "RecordControls";
	private static Logger logger = Logger.getLogger(RecordControls.class);
	
	public JButton recordBtn, positionBtn, closeBtn;
	public AudioSelector audioSelect;
	public JPanel titlePanel, recordingOptionsPanel, statusPanel;

	private JLabel title, status;
	private JPanel borderPanel;
	private Timer timer;
	private AbstractController controller;
	private JFileChooser fileSelect = new JFileChooser();
	private static Color backgroundColor = new Color(200,200,200); // new Color(34, 34, 34); //
	private static Color borderColor = new Color(62,64,65);
	private static Color textColor = Color.BLACK;
	private int timerCount = 0;
	private JComboBox viewportSelect;
	private ViewNotifications currentState;
	private boolean lockedToCorner = false; // is the viewport big enough to lock us to the top corner?
	private boolean forceLockToCorner = PreferencesManager.isForceRecordingToolsToCorner(); // has user selected to force into the top corner?
	private String[] resolutions = {"Custom","320x240","640x480","800x600","1024x768","1280x720","Fullscreen"};
	
	/**
	 * The small recording GUI that provides recording, microphone, and status
	 * controls. Might be visible, or its logic might just drive an external GUI
	 * (i.e. Flash).
	 * 
	 * @param controller
	 */
	public RecordControls(AbstractController controller) {
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
		//setPreferredSize(new Dimension(350, 70)); // will auto fit to the size needed, but if you want to specify a size
		// setLayout(new BorderLayout(0, 3));
		// setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
		setAlwaysOnTop(true);
		//addComponentListener(this);

		/*
		 * if(AWTUtilities.isTranslucencySupported(AWTUtilities.Translucency.
		 * PERPIXEL_TRANSPARENT)) {
		 * System.out.println("Transparency supported!"); }
		 */
		
		borderPanel = new JPanel();
		borderPanel.setBackground(backgroundColor);
		borderPanel.setBorder(new LineBorder(borderColor, 3));
		borderPanel.setLayout(new BoxLayout(borderPanel, BoxLayout.PAGE_AXIS));
		add(borderPanel);

		// ------- setup title bar -------

		title = new JLabel();
		title.setFont(new java.awt.Font("Arial", 1, 11));
		title.setForeground(textColor);
		title.setText("   Screen Recording for \"" + Applet.SCREEN_CAPTURE_NAME + "\"   ");
		title.setHorizontalAlignment(SwingConstants.CENTER);

		titlePanel = new JPanel();
		titlePanel.setBackground(backgroundColor);
		titlePanel.setLayout(new BorderLayout());
		titlePanel.add(title, BorderLayout.CENTER);

		positionBtn = new JButton("", new ImageIcon(this.getClass().getClassLoader().getResource("com/reelfx/view/images/position.png")));
		positionBtn.setBorderPainted(false);
		positionBtn.setToolTipText("Toggle snap of recording tools to top right corner");
		positionBtn.setContentAreaFilled(false);
		positionBtn.setPreferredSize(new Dimension(15,15));
		positionBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				forceLockToCorner = !forceLockToCorner;
				PreferencesManager.setForceRecordingToolsToCorner(forceLockToCorner);
				PreferencesManager.write();
				receiveViewNotification(ViewNotifications.CAPTURE_VIEWPORT_CHANGE);
			}
		});
		
		closeBtn = new JButton("", new ImageIcon(this.getClass().getClassLoader().getResource("com/reelfx/view/images/close.png")));
		closeBtn.setBorderPainted(false);
		closeBtn.setToolTipText("Hide the recording tools");
		closeBtn.setContentAreaFilled(false);
		closeBtn.setPreferredSize(new Dimension(15,15));	
		closeBtn.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				hideInterface();
			}
		});
		
		JPanel buttons = new JPanel();
		buttons.setLayout(new FlowLayout(FlowLayout.RIGHT));
		if(!Applet.IS_MAC || Applet.DEV_MODE) {
			buttons.add(positionBtn);
		}
		buttons.add(closeBtn);
		buttons.setBackground(backgroundColor);
		titlePanel.add(buttons, BorderLayout.EAST);

		borderPanel.add(titlePanel); // ,BorderLayout.CENTER);

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
		recordBtn.setFont(new Font("Arial", Font.BOLD, 12));
		recordingOptionsPanel.add(recordBtn);

		status = new JLabel();
		// status.setBackground(statusColor);
		status.setFont(new java.awt.Font("Arial", 1, 12));
		status.setForeground(textColor);
		status.setHorizontalAlignment(SwingConstants.CENTER);
		recordingOptionsPanel.add(status);

		audioSelect = new AudioSelector();
		recordingOptionsPanel.add(audioSelect);
		
		viewportSelect = new JComboBox(resolutions);
		if(PreferencesManager.hasPreferences()) {
			// set the dropdown to the size if we have it
			String size = PreferencesManager.getWidth()+"x"+PreferencesManager.getHeight();
			boolean hasItem = false;
			for(int i = 0; i < viewportSelect.getModel().getSize(); i++) {
				if(size.equals(viewportSelect.getModel().getElementAt(i))) {
					hasItem = true;
					viewportSelect.setSelectedIndex(i);
					break;
				}
			}
			if(!hasItem) {
				viewportSelect.setSelectedIndex(0); // show "Custom"
			}
		} else {
			viewportSelect.setSelectedIndex(3); // select the 800x600
		}
		viewportSelect.addActionListener(new ActionListener() {
			@Override
			public void actionPerformed(ActionEvent e) {
				Applet.sendViewNotification(ViewNotifications.SET_CAPTURE_VIEWPORT, ((JComboBox)e.getSource()).getSelectedItem());
			}
		});
		if(!Applet.IS_MAC || Applet.DEV_MODE) // TODO temporary
			recordingOptionsPanel.add(viewportSelect);

		borderPanel.add(recordingOptionsPanel);

		logger.info("Initialized...");
		
		pack();
		receiveViewNotification(ViewNotifications.CAPTURE_VIEWPORT_CHANGE);
	}
	
	@Override
	public void receiveViewNotification(ViewNotifications notification, Object body) {
		switch(notification) {
		
		case READY:
			recordBtn.setEnabled(true);
			recordBtn.setText("Record");
			audioSelect.setEnabled(true);
			audioSelect.setVisible(true);
			viewportSelect.setEnabled(true);
			viewportSelect.setVisible(true);
			closeBtn.setVisible(true);
			currentState = notification;
			if (body instanceof MessageNotification) {
				status.setText(((MessageNotification) body).getStatusText());
			} else {
				status.setText("");
			}
			Applet.handleRecordingUpdate(currentState, status.getText());
			// no break
		case SHOW_ALL:
		case SHOW_RECORD_CONTROLS:
			setAlwaysOnTop(true);
			setVisible(true);
			borderPanel.setBorder(new LineBorder(borderColor,3));
			break;
		
		case PRE_RECORDING:
			recordBtn.setEnabled(false);
			audioSelect.setEnabled(false);
			audioSelect.setVisible(false);
			viewportSelect.setEnabled(false);
			viewportSelect.setVisible(false);
			status.setEnabled(true);
			status.setVisible(true);
			closeBtn.setVisible(false);
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
			closeBtn.setVisible(false);
			currentState = notification;
			borderPanel.setBorder(new LineBorder(new Color(255,71,71),3)); // recording color
			if (body instanceof MessageNotification) {
				status.setText(((MessageNotification) body).getStatusText());
			} else {
				status.setText("");
			}
			Applet.handleRecordingUpdate(currentState, status.getText());
			break;
			
		case DISABLE_ALL:
			setAlwaysOnTop(false);
			break;
			
		case HIDE_ALL:
		case HIDE_RECORD_CONTROLS:
		case POST_OPTIONS:
		case POST_OPTIONS_NO_UPLOADING:	
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
			borderPanel.setBorder(new LineBorder(borderColor,3));
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
		
		pack();	
		
		// update position after GUI changes
		switch(notification) {
		case FATAL:
		case THINKING:
		case SHOW_ALL:
		case SHOW_RECORD_CONTROLS:
		case READY:
		case PRE_RECORDING:
		case RECORDING:
		case CAPTURE_VIEWPORT_CHANGE:
			Point pt = Applet.CAPTURE_VIEWPORT.getTopRightPoint();
			pt.translate(-getWidth(), -getHeight()-10);
			if(pt.y < 0 || forceLockToCorner || (Applet.IS_MAC && !Applet.DEV_MODE)) { // TODO temporary (second condition)
				pt = Applet.CAPTURE_VIEWPORT.getBottomRightPoint();
				pt.translate(-getWidth(), 10);
				
				if(pt.y + getHeight() > Applet.SCREEN.getHeight() || forceLockToCorner || (Applet.IS_MAC && !Applet.DEV_MODE)) { // TODO temporary (second condition)
					pt = new Point((int) (Applet.SCREEN.getWidth() - 10 - getWidth()), 10 + (Applet.IS_MAC ? 20 : 0));
					lockedToCorner = true;
				}
			} else {
				lockedToCorner = false;
			}
			setLocation(pt);
			break;
		}
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if((!Applet.IS_MAC || Applet.DEV_MODE) && // temporary 
				(currentState == ViewNotifications.PRE_RECORDING || currentState == ViewNotifications.RECORDING)) return;
		if(lockedToCorner && (!Applet.IS_MAC || Applet.DEV_MODE)) return; // temporary
		super.mousePressed(e);
		Applet.sendViewNotification(ViewNotifications.MOUSE_PRESS_RECORD_CONTROLS, e);
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if((!Applet.IS_MAC || Applet.DEV_MODE) && // temporary 
				(currentState == ViewNotifications.PRE_RECORDING || currentState == ViewNotifications.RECORDING)) return;
		if(lockedToCorner && (!Applet.IS_MAC || Applet.DEV_MODE)) return; // temporary
		super.mouseDragged(e);
		Applet.sendViewNotification(ViewNotifications.MOUSE_DRAG_RECORD_CONTROLS, e);
	}

	/**
	 * Called when the "Record" button is pressed, starting the countdown timer.
	 */
	public void prepareForRecording() {
		// do I need permission to delete a file first?
		if (AttributesManager.OUTPUT_FILE.exists() && !deleteRecording())
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
		} else if (status.getText().equals("Set")) {
			Applet.sendViewNotification(ViewNotifications.RECORDING, new MessageNotification("Go!"));
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

	/**
	 * @return boolean that says whether to continue with whatever action called
	 *         this or not
	 */
	public boolean deleteRecording() {
		Applet.sendViewNotification(ViewNotifications.DISABLE_ALL);
		int n = JOptionPane.showConfirmDialog(null,
				"Are you sure that you are done with this screen recording?",
				"Are you sure?", JOptionPane.YES_NO_OPTION);
		if(Applet.IS_MAC && !Applet.DEV_MODE) // temporary
			Applet.sendViewNotification(ViewNotifications.SHOW_RECORD_CONTROLS);
		else
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
