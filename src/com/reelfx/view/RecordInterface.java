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
import com.reelfx.view.util.MoveableWindow;

public class RecordInterface extends MoveableWindow implements ActionListener {

	public final static String NAME = "RecordInterface";

	public final static int READY = 500;
	public final static int PRE_RECORDING = 501;
	public final static int RECORDING = 502;
	public final static int THINKING = 503;
	public final static int FATAL = 504;

	public JButton recordBtn, closeBtn;
	public AudioSelectBox audioSelect;
	public JPanel titlePanel, recordingOptionsPanel, statusPanel;

	private JLabel title, status, message;
	// private JTextArea message;
	private Timer timer;
	private ApplicationController controller;
	private JFileChooser fileSelect = new JFileChooser();
	private Color backgroundColor = new Color(34, 34, 34); // new Color(230,230,230);
	private Color statusColor = new Color(255, 102, 102);
	private Color messageColor = new Color(255, 255, 153);
	private int currentState = READY, timerCount = 0;
	private CenterInterface countdown;

	/**
	 * The small recording GUI that provides recording, microphone, and status
	 * controls. Might be visible, or its logic might just drive an external GUI
	 * (i.e. Flash).
	 * 
	 * @param controller
	 */
	public RecordInterface(ApplicationController controller) {
		super();
		setName(NAME);
		
		this.controller = controller;
		this.countdown = new CenterInterface(this);

		// ------- setup -------

		/*
		 * for JFrame setTitle("Review for "+Applet.SCREEN_CAPTURE_NAME);
		 * setResizable(false); setDefaultCloseOperation(HIDE_ON_CLOSE);
		 */

		setBackground(backgroundColor);
		// setPreferredSize(dim); // full screen
		// setPreferredSize(new Dimension(500, 50)); // will auto fit to the
		// size needed, but if you want to specify a size
		setLocation((int) (screen.width * 0.75), screen.height / 6);
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
		title.setOpaque(true);
		title.setHorizontalAlignment(SwingConstants.CENTER);

		titlePanel = new JPanel();
		titlePanel.setOpaque(false);
		titlePanel.setLayout(new BorderLayout());
		titlePanel.add(title, BorderLayout.CENTER);

		closeBtn = new JButton("", new ImageIcon(this.getClass()
				.getClassLoader().getResource("com/reelfx/view/images/close.png")));
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
		// recordingOptionsPanel.setMaximumSize(new Dimension(180,1000));
		recordingOptionsPanel.setOpaque(false);

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
		status.setOpaque(true);
		status.setHorizontalAlignment(SwingConstants.CENTER);
		recordingOptionsPanel.add(status);

		audioSelect = new AudioSelectBox();
		recordingOptionsPanel.add(audioSelect);

		add(recordingOptionsPanel); // ,BorderLayout.NORTH);

		/*
		 * statusPanel = new JPanel(); statusPanel.setOpaque(false);
		 * statusPanel.add(status); add(statusPanel); //,BorderLayout.CENTER);
		 */
		
		countdown.setVisible(true); countdown.pack();

		System.out.println("RecordInterface initialized...");
	}

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

		case FATAL:
		case THINKING:
			recordBtn.setEnabled(false);
			audioSelect.setEnabled(false);
			audioSelect.setVisible(true);
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
	
	@Override
	public void receiveViewNotification(int notification, Object body) {

	}

	public void prepareForRecording() {
		// do I need permission to delete a file first?
		if (ScreenRecorder.OUTPUT_FILE.exists() && !deleteRecording())
			return;

		changeState(PRE_RECORDING, "Ready");
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
			changeState(PRE_RECORDING, "Set");
		} else if (status.getText().equals("Set")) {
			changeState(RECORDING, "Go!");
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
		int n = JOptionPane.showConfirmDialog(this,
				"Are you sure that you are done with this screen recording?",
				"Are you sure?", JOptionPane.YES_NO_OPTION);
		if (n == JOptionPane.YES_OPTION) {
			controller.deleteRecording();
			return true;
		} else {
			return false;
		}
	}

	public void hideInterface() {
		setVisible(false);
		Applet.handleRecordingUIHide();
	}

	@Override
	public void dispose() {
		super.dispose();
		audioSelect.destroy();
	}
}
