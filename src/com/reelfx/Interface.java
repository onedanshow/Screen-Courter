package com.reelfx;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.File;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URL;

import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JPanel;
import javax.swing.JWindow;
import javax.swing.SwingUtilities;
import javax.swing.Timer;
import javax.swing.JLabel;

import org.apache.commons.net.telnet.TelnetClient;

import com.reelfx.util.ProcessListener;

public class Interface extends JWindow implements ProcessListener {

    private static final long serialVersionUID = 4803377343174867777L;
    TelnetClient telnet = new TelnetClient();
    AudioRecorder audio;
    ScreenRecorder screen;
    PostProcessor postProcess;
    JButton recordBtn, stopBtn, previewBtn, saveBtn, closeBtn;
    JLabel status;
    
    public Interface() {
        super();

        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension dim = tk.getScreenSize();

        setBackground(Color.white);
        //setPreferredSize(dim); // full screen
        setPreferredSize(new Dimension(500, 50));
        setLocation(100, 100);
        setLayout(new BorderLayout());
        setAlwaysOnTop(true);

        /*if (AWTUtilities.isTranslucencySupported(AWTUtilities.Translucency.PERPIXEL_TRANSPARENT)) {
            System.out.println("Transparency supported!");
        }*/
        
        JPanel buttons = new JPanel();

        recordBtn = new JButton("Record");
        recordBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                prepRecording();
            }
        });
        buttons.add(recordBtn);

        stopBtn = new JButton("Stop");
        stopBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopRecording();
            }
        });
        stopBtn.setEnabled(false);
        buttons.add(stopBtn);

        previewBtn = new JButton("Preview");
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                previewRecording();
            }
        });
        if( !new File(ScreenRecorder.OUTPUT_FILE).exists() ) {
        	previewBtn.setEnabled(false);
        }
        buttons.add(previewBtn);

        saveBtn = new JButton("Save");
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveRecording();
            }
        });
        if( !new File(ScreenRecorder.OUTPUT_FILE).exists() ) {
        	saveBtn.setEnabled(false);
        }
        buttons.add(saveBtn);

        closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeInterface();
            }
        });
        buttons.add(closeBtn);
        status = new JLabel();
        
        add(buttons,BorderLayout.CENTER);
        add(status,BorderLayout.SOUTH);
        
        if(RfxApplet.IS_MAC && !RfxApplet.BIN_FOLDER.exists()){
			status.setText("Performing one-time install...");
			disable();
        }
        
    	postProcess = new PostProcessor();
    	postProcess.addProcessListener(this);
        
        System.out.println("Interface initialized...");
    }
    
    /**
     * Installs VLC, ffmpeg and ffplay if needed.
     */
    public void setupExtensions() { 
    	
    	try {
        	/* might revisit copying the jar locally later
    		if(!VLC_EXEC.exists() && RfxApplet.DEV_MODE) {
    			RfxApplet.copyFolderFromRemoteJar(new URL("jar", "", "/Users/daniel/Documents/Java/java-review-tool/lib"+File.separator+"bin-mac.jar" + "!/"), "bin-mac");
    			Runtime.getRuntime().exec("chmod 755 "+VLC_EXEC.getAbsolutePath()).waitFor();
    			if(!VLC_EXEC.exists()) throw new IOException("Did not copy VLC to its execution directory!");
    		} else */
			if(RfxApplet.IS_MAC && !RfxApplet.BIN_FOLDER.exists()){
				RfxApplet.copyFolderFromRemoteJar(new URL(RfxApplet.CODE_BASE+"/bin-mac.jar"), "bin-mac");
				Runtime.getRuntime().exec("chmod 755 "+RfxApplet.BIN_FOLDER+File.separator+"VLC").waitFor();
				Runtime.getRuntime().exec("chmod 755 "+RfxApplet.BIN_FOLDER+File.separator+"ffmpeg").waitFor();
				Runtime.getRuntime().exec("chmod 755 "+RfxApplet.BIN_FOLDER+File.separator+"ffplay").waitFor();
				if(!RfxApplet.BIN_FOLDER.exists()) throw new IOException("Did not copy VLC to its execution directory!");
				status.setText("");
				recordBtn.setEnabled(true);
		        closeBtn.setEnabled(true);
			}
        } catch (MalformedURLException e1) {
			status.setText("Error downloading native extensions");
			e1.printStackTrace();
		} catch (InterruptedException e) {
			status.setText("Error setting up native extentions");
			e.printStackTrace();
		} catch (IOException e) {
			status.setText("Error downloading native extentions");
			e.printStackTrace();
		}
    }
    
    public void prepRecording() {
    	// start up VLC
    	screen = new ScreenRecorder();
    	screen.start();
    	// TODO check that it starts up correctly
    	status.setText("Ready...");
    	
    	recordBtn.setEnabled(false);
        stopBtn.setEnabled(false);
        previewBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        closeBtn.setEnabled(false);
    	
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
    	
    	stopBtn.setEnabled(true);
    	
    	if(RfxApplet.IS_MAC) {
	        ActionListener taskPerformer = new ActionListener() {
	            public void actionPerformed(ActionEvent evt) {
	                try {
	                    if (!telnet.isConnected()) {
	                        telnet.connect("localhost", 4444);
	                    }
	                    BufferedWriter bw = new BufferedWriter(
	                            new OutputStreamWriter(telnet.getOutputStream()));
	                    bw.write("add screen:// \n");
	                    bw.flush();
	                } catch (SocketException e) {
	                    e.printStackTrace();
	                } catch (IOException e) {
	                    e.printStackTrace();
	                }
	            }
	        };
	        // HACK: audio takes a second to get going, delay the video a second (maybe a mac only thing)
	        Timer delayVideo = new Timer(500, taskPerformer);
	        delayVideo.setRepeats(false);
	        delayVideo.start();
	
	        audio = new AudioRecorder();
	        audio.startRecording();
    	}
    	else if(RfxApplet.IS_LINUX) {
    		// already started...
    	}
        
        status.setText("Go!");
    }

    public void stopRecording() {
    	if(RfxApplet.IS_MAC) {
	        try {
	            if (!telnet.isConnected()) {
	                telnet.connect("localhost", 4444);
	            }
	            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(telnet.getOutputStream()));
	            bw.write("stop \n");
	            bw.flush();
	        } catch (SocketException e) {
	            e.printStackTrace();
	        } catch (IOException e) {
	            e.printStackTrace();
	        }
	        if(audio != null)
	            audio.stopRecording();
    	}
    	else if(RfxApplet.IS_LINUX) {
    		screen.stopRecording();
    	}
        
        recordBtn.setEnabled(true);
        stopBtn.setEnabled(false);
        previewBtn.setEnabled(true);
        saveBtn.setEnabled(true);
        closeBtn.setEnabled(true);

        status.setText("Recording stopped.");
    }

    public void previewRecording() {
        PreviewPlayer preview = new PreviewPlayer();
        preview.start();
    }

    public void saveRecording() {
    	disable();
        postProcess.start();
        status.setText("Encoding to H.264...");
    }
    
	public void processUpdate(int event) {
		switch(event) {
			case PostProcessor.POST_PROCESS_COMPLETE:
				recordBtn.setEnabled(true);
				closeBtn.setEnabled(true);
				status.setText("Done");
			break;
		}
	}
	
	public void disable() {
		recordBtn.setEnabled(false);
        stopBtn.setEnabled(false);
        previewBtn.setEnabled(false);
        saveBtn.setEnabled(false);
        closeBtn.setEnabled(false);
	}

    public void closeInterface() {
        try {
        	System.out.println("Closing interface...");
            if (telnet.isConnected()) {
            	BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(telnet.getOutputStream()));
                bw.write("quit \n");
                bw.flush();
            }
			screen = null;
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (Throwable e) {
			e.printStackTrace();
		} 
        setVisible(false);
    }
}
