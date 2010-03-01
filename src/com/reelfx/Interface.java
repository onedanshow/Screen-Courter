package com.reelfx;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.FlowLayout;
import java.awt.Toolkit;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.net.SocketException;

import javax.swing.JButton;
import javax.swing.JWindow;
import javax.swing.Timer;

import org.apache.commons.net.telnet.TelnetClient;

public class Interface extends JWindow {

    private static final long serialVersionUID = 4803377343174867777L;
    TelnetClient telnet = new TelnetClient();
    AudioRecorder audio;

    public Interface() {
        super();

        Toolkit tk = Toolkit.getDefaultToolkit();
        Dimension dim = tk.getScreenSize();

        setBackground(Color.white);
        //setPreferredSize(dim); // full screen
        setPreferredSize(new Dimension(500, 40));
        setLayout(new FlowLayout());
        setAlwaysOnTop(true);

        /*if (AWTUtilities.isTranslucencySupported(AWTUtilities.Translucency.PERPIXEL_TRANSPARENT)) {
            System.out.println("Transparency supported!");
        }*/

        JButton recordBtn = new JButton("Record");
        recordBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                startRecording();
            }
        });
        add(recordBtn);

        JButton stopBtn = new JButton("Stop");
        stopBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                stopRecording();
            }
        });
        add(stopBtn);

        JButton previewBtn = new JButton("Preview");
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                previewRecording();
            }
        });
        add(previewBtn);

        JButton saveBtn = new JButton("Save");
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                saveRecording();
            }
        });
        add(saveBtn);

        JButton closeBtn = new JButton("Close");
        closeBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                closeApplication();
            }
        });
        add(closeBtn);

        System.out.println("Interface initialized...");
    }

    public void startRecording() {
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

    public void stopRecording() {
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

    public void previewRecording() {
        PreviewPlayer preview = new PreviewPlayer();
        preview.start();
    }

    public void saveRecording() {
        new PostProcessor().start();
    }

    public void closeApplication() {
        try {
            if (!telnet.isConnected()) {
                telnet.connect("localhost", 4444);
            }
            BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(telnet.getOutputStream()));
            bw.write("quit \n");
            bw.flush();
        } catch (SocketException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
        setVisible(false);
    }
}
