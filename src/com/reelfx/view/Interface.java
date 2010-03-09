package com.reelfx.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

import javax.sound.sampled.AudioSystem;
import javax.sound.sampled.Mixer;
import javax.swing.JButton;
import javax.swing.JComboBox;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JLabel;

import org.apache.commons.net.telnet.TelnetClient;

import com.reelfx.Applet;
import com.reelfx.controller.ApplicationController;
import com.reelfx.model.AudioRecorder;
import com.reelfx.model.PostProcessor;
import com.reelfx.model.PreviewPlayer;
import com.reelfx.model.ScreenRecorder;
import com.reelfx.model.util.ProcessListener;

public class Interface extends JWindow implements MouseListener, MouseMotionListener {

    private static final long serialVersionUID = 4803377343174867777L;
    
    public JButton recordBtn, previewBtn, saveBtn, closeBtn;
    public AudioSelectBox audioSelect;
    public JLabel status;
    private ApplicationController controller;
    
    public Interface(ApplicationController controller) {
        super();
        
        this.controller = controller;
        
        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension dim = tk.getScreenSize();

        setBackground(Color.white);
        //setPreferredSize(dim); // full screen
        //setPreferredSize(new Dimension(500, 50)); // will auto fit to the size needed, but if you want to specify a size
        setLocation(dim.width/2, dim.height/2);
        setLayout(new BorderLayout());
        setAlwaysOnTop(true);
        addMouseListener(this);
        addMouseMotionListener(this);

        /*if (AWTUtilities.isTranslucencySupported(AWTUtilities.Translucency.PERPIXEL_TRANSPARENT)) {
            System.out.println("Transparency supported!");
        }*/
        
        JPanel options = new JPanel();

        recordBtn = new JButton("Record");
        recordBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if(recordBtn.getText().equals("Record")) {
            		prepareForRecording();
            		recordBtn.setText("Stop");
            	}
            	else if(recordBtn.getText().equals("Stop")) {
            		stopRecording();
            		recordBtn.setText("Record");
            	}
            }
        });
        options.add(recordBtn);
        
        audioSelect = new AudioSelectBox();
        audioSelect.addActionListener(new ActionListener() {
			public void actionPerformed(ActionEvent e) {
				System.out.println(e.toString());
			}
		});
        options.add(audioSelect);

        previewBtn = new JButton("Preview");
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                previewRecording();
            }
        });
        if( !new File(ScreenRecorder.OUTPUT_FILE).exists() ) {
        	previewBtn.setEnabled(false);
        }
        options.add(previewBtn);

        saveBtn = new JButton("Save");
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveRecording();
            }
        });
        if( !new File(ScreenRecorder.OUTPUT_FILE).exists() ) {
        	saveBtn.setEnabled(false);
        }
        options.add(saveBtn);

        closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeInterface();
            }
        });
        options.add(closeBtn);
        status = new JLabel();
        status.setText("Loading...");
        
        add(options,BorderLayout.CENTER);
        add(status,BorderLayout.SOUTH);
        
        System.out.println("Interface initialized...");
    }
    
    public void prepareForRecording() {    	
    	status.setText("Ready...");
    	recordBtn.setEnabled(false);
        previewBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        closeBtn.setEnabled(false);
        
        controller.prepareForRecording();
    	
    	ActionListener taskPerformer = new ActionListener() {
            public void actionPerformed(ActionEvent evt) {
                startRecording();
            }
        };
        // TODO make the "ready, set, go!" pretty
        Timer waitForIt = new Timer(2000, taskPerformer);
        waitForIt.setRepeats(false);
        waitForIt.start();
    }

    private void startRecording() {
    	controller.startRecording(audioSelect.getSelectedMixer());
    	
    	recordBtn.setEnabled(true);
        status.setText("Go!");
    }

    public void stopRecording() {
    	controller.stopRecording();
        
        recordBtn.setEnabled(true);
        previewBtn.setEnabled(true);
        saveBtn.setEnabled(true);
        closeBtn.setEnabled(true);

        status.setText("Recording stopped.");
    }

    public void previewRecording() {
        controller.previewRecording();
    }

    public void saveRecording() {
    	controller.saveRecording();
    	disable();
        status.setText("Encoding to H.264...");
    }
	
	public void disable() {
		recordBtn.setEnabled(false);
        previewBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        closeBtn.setEnabled(false);
	}

    public void closeInterface() { 
        setVisible(false);
    }

    private Point mouseOffset = null;
    
	public void mouseClicked(MouseEvent e) {}

	public void mouseEntered(MouseEvent e) {}

	public void mouseExited(MouseEvent e) {}

	public void mousePressed(MouseEvent e) {
		mouseOffset = new Point(e.getX(),e.getY());
	}

	public void mouseReleased(MouseEvent e) {
		mouseOffset = null;
	}

	public void mouseDragged(MouseEvent e) {
		Point p = e.getLocationOnScreen();
		p.x -= mouseOffset.x;
		p.y -= mouseOffset.y;
		setLocation(p);
	}

	public void mouseMoved(MouseEvent e) {}
}
