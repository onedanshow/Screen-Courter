package com.reelfx.view;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BorderFactory;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JProgressBar;
import javax.swing.JSeparator;

import com.reelfx.controller.ApplicationController;
import com.reelfx.view.util.MessageNotification;
import com.reelfx.view.util.ViewListener;
import com.reelfx.view.util.ViewNotifications;

public class PostOptions extends JPanel implements ViewListener {
	
	private static final long serialVersionUID = 4036818007133606840L;
	
	public JButton previewBtn, saveBtn, insightBtn, deleteBtn;
	private final ApplicationController controller;
	private JLabel message;
	private ViewNotifications currentState;
	private JPanel self;
	private JProgressBar progressBar;
	
	public PostOptions(final ApplicationController controller) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setBackground(new Color(230,230,230));
		setBorder(BorderFactory.createLineBorder(new Color(102, 102, 102)));
		self = this;
		this.controller = controller;
        
        /*
        message = new JTextArea();
        message.setFont(new java.awt.Font("Arial", 0, 13));
        message.setMinimumSize(new Dimension(200,20));
        message.setOpaque(false);
        message.setLineWrap(true);
        message.setBorder(javax.swing.BorderFactory.createLineBorder(messageColor, 5));
        //message.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        message.setText("You have a review for \"Shot 2000-0300\" from 04/03/2010");
        message.setAlignmentX(0.5F);
        /* */
		
        message = new JLabel();
        message.setFont(new java.awt.Font("Arial", 0, 11));
        message.setOpaque(false);
        message.setMaximumSize(new Dimension(180,1000));
        message.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        message.setAlignmentX(0.5F);
        
        progressBar = new JProgressBar();
        progressBar.setIndeterminate(true);
        
        JPanel messageBoard = new JPanel();
        messageBoard.setLayout(new BorderLayout());
        messageBoard.add(message,BorderLayout.CENTER);
        messageBoard.add(progressBar,BorderLayout.SOUTH);
        add(messageBoard);
        
        add(new JSeparator(JSeparator.VERTICAL));
        
        previewBtn = new JButton("Preview It");
        previewBtn.setFont(new java.awt.Font("Arial", 0, 11));
        previewBtn.setAlignmentX(0.5F);
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                controller.previewRecording();
            }
        });
        add(previewBtn);

        saveBtn = new JButton("Save to My Computer");
        saveBtn.setFont(new java.awt.Font("Arial", 0, 11));
        saveBtn.setAlignmentX(0.5F);
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                controller.askForAndSaveRecording();
            }
        });
        add(saveBtn);

        insightBtn = new JButton("Post to Insight");
        insightBtn.setFont(new java.awt.Font("Arial", 0, 11));
        insightBtn.setAlignmentX(0.5F);
        insightBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                controller.postRecording();
            }
        });
        add(insightBtn);
        
        deleteBtn = new JButton("I'm Done With It");
        deleteBtn.setFont(new java.awt.Font("Arial", 0, 11));
        deleteBtn.setAlignmentX(0.5F);
        deleteBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	int n = JOptionPane.showConfirmDialog(self,
            		    "Are you sure that you are done with this screen recording?",
            		    "Are you sure?",
            		    JOptionPane.YES_NO_OPTION);
            	if (n == JOptionPane.YES_OPTION) {
                    controller.deleteRecording();
                }
            }
        });
        add(deleteBtn);
	}

	@Override
	public void receiveViewNotification(ViewNotifications notification, Object body) {
		switch(notification) {
		
		case POST_OPTIONS:
    		previewBtn.setEnabled(true);
    		saveBtn.setEnabled(true);
    		insightBtn.setEnabled(true);
    		deleteBtn.setEnabled(true);
    		progressBar.setVisible(false);
    		if(body instanceof MessageNotification) {
    			message.setText("<html><body><table cellpadding='5' width='100%'><tr><td align='center'>"+((MessageNotification)body).getMessageText()+"</td></tr></table></body></html>");
    		} else {
    			message.setText("<html><body><table cellpadding='5' width='100%'><tr><td align='center'>ReelFX Screen Recorder</td></tr></table></body></html>");
    		}
    		break;
    		
		case POST_OPTIONS_NO_UPLOADING:
			previewBtn.setEnabled(true);
    		saveBtn.setEnabled(true);
    		insightBtn.setEnabled(false);
    		deleteBtn.setEnabled(true);
    		progressBar.setVisible(false);
    		if(body instanceof MessageNotification) {
    			message.setText("<html><body><table cellpadding='5' width='100%'><tr><td align='center'>"+((MessageNotification)body).getMessageText()+"</td></tr></table></body></html>");
    		} else {
    			message.setText("<html><body><table cellpadding='5' width='100%'><tr><td align='center'>ReelFX Screen Recorder</td></tr></table></body></html>");
    		}
			break;
		
		case SHOW_ALL:
		case FATAL:
		case THINKING:
		case READY:
			progressBar.setVisible(false);
    		previewBtn.setEnabled(false);
    		saveBtn.setEnabled(false);
    		insightBtn.setEnabled(false);
    		deleteBtn.setEnabled(false);
    		if(body instanceof MessageNotification) {
    			message.setText("<html><body><table cellpadding='5' width='100%'><tr><td align='center'>"+((MessageNotification)body).getMessageText()+"</td></tr></table></body></html>");
    		} else {
    			message.setText("<html><body><table cellpadding='5' width='100%'><tr><td align='center'>ReelFX Screen Recorder</td></tr></table></body></html>");
    		}
    		break;
		}
		
		// any secondary changes
		switch(notification) {
		case THINKING:
			progressBar.setVisible(true);
		}
		
		currentState = notification; // needs to be at end
	}
}
