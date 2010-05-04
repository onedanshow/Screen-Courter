package com.reelfx.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Font;
import java.awt.GraphicsDevice;
import java.awt.GraphicsEnvironment;
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
import java.awt.event.WindowFocusListener;
import java.awt.event.WindowListener;
import java.awt.event.WindowStateListener;
import java.io.File;
import java.sql.Time;
import java.util.Calendar;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JSeparator;
import javax.swing.JTextArea;
import javax.swing.JWindow;
import javax.swing.SwingConstants;
import javax.swing.Timer;

import com.reelfx.Applet;
import com.reelfx.controller.ApplicationController;
import com.reelfx.model.ScreenRecorder;

public class RecordInterface extends JFrame implements MouseListener, MouseMotionListener, WindowListener, ActionListener, ComponentListener {

    private static final long serialVersionUID = 4803377343174867777L;
    
    public final static int READY = 500;
    public final static int PRE_RECORDING = 505;
    public final static int RECORDING = 502;
    public final static int THINKING = 503;
    public final static int FATAL = 504;
    
    public JButton recordBtn;
    public AudioSelectBox audioSelect;
    public JPanel recordingOptionsPanel, statusPanel;
    
    private JLabel status, message;
    //private JTextArea message;
    private Timer timer;
    private ApplicationController controller;
    private JFileChooser fileSelect = new JFileChooser();
    private Color backgroundColor = new Color(230,230,230);    
    private Color statusColor = new Color(255, 102, 102);
    private Color messageColor = new Color(255, 255, 153);
    private int currentState = READY, timerCount = 0;
    private Dimension screen;
    private CountDown countdown = new CountDown();
    
    public RecordInterface(ApplicationController controller) {
        super();
        
        this.controller = controller;
        
        Toolkit tk = Toolkit.getDefaultToolkit();
        screen = tk.getScreenSize();
        // From Docs: Gets the size of the screen. On systems with multiple displays, the primary display is used. Multi-screen aware display dimensions are available from GraphicsConfiguration and GraphicsDevice

        // ------- setup the JFrame -------
        
        setTitle("Review for "+Applet.SCREEN_CAPTURE_NAME);
        setResizable(false);
        setDefaultCloseOperation(HIDE_ON_CLOSE);
        setBackground(backgroundColor);
        //setPreferredSize(dim); // full screen
        //setPreferredSize(new Dimension(500, 50)); // will auto fit to the size needed, but if you want to specify a size
        setLocation((int)(screen.width*0.75), screen.height/6);
        //setLayout(new BorderLayout(0, 3));
        setLayout(new BoxLayout(getContentPane(), BoxLayout.PAGE_AXIS));
        setAlwaysOnTop(true);
        addMouseListener(this);
        addMouseMotionListener(this);
        addWindowListener(this);
        addComponentListener(this);

        /*if (AWTUtilities.isTranslucencySupported(AWTUtilities.Translucency.PERPIXEL_TRANSPARENT)) {
            System.out.println("Transparency supported!");
        }*/
        
        // ------- setup recording options -------
        
        recordingOptionsPanel = new JPanel();
        //recordingOptionsPanel.setMaximumSize(new Dimension(180,1000));
        recordingOptionsPanel.setOpaque(false);
        
        recordBtn = new JButton("Record");
        recordBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	if(recordBtn.getText().equals("Record")) {
            		prepareForRecording();
            	}
            	else if(recordBtn.getText().equals("Stop")) {
            		stopRecording();
            	}
            }
        });
        recordBtn.setFont(new Font("Arial", 0, 11));
        recordingOptionsPanel.add(recordBtn);
        
        audioSelect = new AudioSelectBox();
        recordingOptionsPanel.add(audioSelect);
        
        add(recordingOptionsPanel); //,BorderLayout.NORTH);
        
        // ------- setup status bar -------
        
        status = new JLabel();
        //status.setBackground(statusColor);
        //status.setPreferredSize(new Dimension(50, 40));
        status.setFont(new java.awt.Font("Arial", 1, 11));
        //status.setForeground(Color.WHITE);
        status.setOpaque(true);
        status.setHorizontalAlignment(SwingConstants.CENTER);
        
        statusPanel = new JPanel();
        statusPanel.setOpaque(false);
        statusPanel.add(status);
        
        add(statusPanel); //,BorderLayout.CENTER);
        
        System.out.println("RecordInterface initialized...");
    }
    
    public void changeState(int state) {
    	changeState(state, null);
    }
    
    public void changeState(int state, String statusText) {
    	switch(state) {
    	
    	case READY:
    		recordBtn.setEnabled(true);
    		recordBtn.setText("Record");
    		audioSelect.setEnabled(true);
    		if(statusText != null) {
    			status.setText(statusText);
    			statusPanel.setVisible(true);
    		} else {
    			status.setText("");
    			statusPanel.setVisible(false);
    		}
    		break;
    		
    	case PRE_RECORDING:
    		recordBtn.setEnabled(false);
    		audioSelect.setEnabled(false);
    		status.setEnabled(true);
    		status.setVisible(true);
    		if(statusText != null) {
    			status.setText(statusText);
    			statusPanel.setVisible(true);
    		} else {
    			status.setText("");
    			statusPanel.setVisible(false);
    		}
    		break;
    		
    	case RECORDING:
    		recordBtn.setEnabled(true);
    		recordBtn.setText("Stop");
    		audioSelect.setEnabled(false);
    		if(statusText != null) {
    			status.setText(statusText);
    			statusPanel.setVisible(true);
    		} else {
    			status.setText("");
    			statusPanel.setVisible(false);
    		}
    		break;
    		
    	case FATAL:	
    	case THINKING:
    		recordBtn.setEnabled(false);
    		audioSelect.setEnabled(false);
    		if(statusText != null) {
    			status.setText(statusText);
    			statusPanel.setVisible(true);
    		} else {
    			status.setText("");
    			statusPanel.setVisible(false);
    		}
    		break;
    	}
    	currentState = state; // needs to be at end
    	pack();
    }
    
    public void prepareForRecording() {   
    	// do I need permission to delete a file first?
    	if(ScreenRecorder.OUTPUT_FILE.exists() && !deleteRecording()) return;
    	
    	changeState(PRE_RECORDING,"Ready");
        controller.prepareForRecording();
    	
        if(timer == null) {
        	timer = new Timer(1000, this);
        	timer.start(); // calls actionPerformed
        }
        else
        	timer.restart(); // calls actionPerformed
        
        /*
        countdown.setVisible(true);
    	countdown.pack();
        */
    }
    
    @Override
	public void actionPerformed(ActionEvent e) { // for the timer
		if(status.getText().equals("Ready")) {
			changeState(PRE_RECORDING,"Set");
		} else if(status.getText().equals("Set")) {
			changeState(RECORDING,"Go!");
			startRecording();
		} else {
			status.setText((timerCount/60 < 10 ? "0" : "")+timerCount/60+":"+(timerCount%60 < 10 ? "0" : "")+timerCount%60);
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
    	changeState(READY);
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
     * @return boolean that says whether to continue with whatever action called this or not
     */
    public boolean deleteRecording() {
    	int n = JOptionPane.showConfirmDialog(this,
    		    "This will delete your screen recording. Are you sure?",
    		    "Are you sure?",
    		    JOptionPane.YES_NO_OPTION);
    	if (n == JOptionPane.YES_OPTION) {
            controller.deleteRecording();
            return true;
        } else {
        	return false;
        }

    }
    
    @Override
    public void dispose() {
    	super.dispose();
    	audioSelect.destroy();
    }

    // ----- listener methods till EOF ------
    
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
		p.x = Math.min(Math.max(p.x, 0), screen.width-this.getWidth());
		p.y = Math.min(Math.max(p.y, 0), screen.height-this.getHeight());
		setLocation(p);
	}

	public void mouseMoved(MouseEvent e) {}

	@Override
	public void windowActivated(WindowEvent e) { // gains focus or returned from minimize
		//System.out.println("Window activated.");
	}

	@Override
	public void windowClosing(WindowEvent e) {
		//System.out.println("Window closing");
	}

	@Override
	public void windowDeactivated(WindowEvent e) { // lose focus or minimized
		//System.out.println("Window deactivated");
	}

	@Override
	public void windowDeiconified(WindowEvent e) {}

	@Override
	public void windowIconified(WindowEvent e) {}

	@Override
	public void windowOpened(WindowEvent e) {}
	
	@Override
	public void windowClosed(WindowEvent e) {} // not called with close operation set to HIDE_FRAME

	@Override
	public void componentHidden(ComponentEvent e) {}

	@Override
	public void componentMoved(ComponentEvent e) {  // temporary until I return to a JWindow and not a JFrame
		Point p = this.getLocation();
		p.x = Math.min(Math.max(p.x, 0), screen.width-this.getWidth());
		p.y = Math.min(Math.max(p.y, 0), screen.height-this.getHeight());
		setLocation(p);
	}

	@Override
	public void componentResized(ComponentEvent e) {}

	@Override
	public void componentShown(ComponentEvent e) {}
}
