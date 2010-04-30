package com.reelfx.view;

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
import javax.swing.JSeparator;

import com.reelfx.controller.ApplicationController;

public class OptionsInterface extends JPanel {
	
	private static final long serialVersionUID = 4036818007133606840L;
	
    public final static int OPTIONS = 600;
    public final static int OPTIONS_NO_UPLOAD = 601; 
    public final static int THINKING = 602;
    public final static int DISABLED = 603;
    public final static int FATAL = 604;
	
	public JButton previewBtn, saveBtn, insightBtn, deleteBtn;
	private final ApplicationController controller;
	private JLabel message;
	private int currentState = OPTIONS;
	private JPanel self;
	
	public OptionsInterface(final ApplicationController controller) {
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
        
        add(message);
        
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
        
        deleteBtn = new JButton("Delete It");
        deleteBtn.setFont(new java.awt.Font("Arial", 0, 11));
        deleteBtn.setAlignmentX(0.5F);
        deleteBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	int n = JOptionPane.showConfirmDialog(self,
            		    "This will delete your screen recording. Are you sure?",
            		    "Are you sure?",
            		    JOptionPane.YES_NO_OPTION);
            	if (n == JOptionPane.YES_OPTION) {
                    controller.deleteRecording();
                }
            }
        });
        add(deleteBtn);
	}
	
    public void changeState(int state) {
    	changeState(state, null);
    }
    
    public void changeState(int state, String messageText) {
    	
		if(messageText == null) {
			message.setText("<html><body><table cellpadding='5' width='100%'><tr><td align='center'>ReelFX Screen Recorder</td></tr></table></body></html>");
		} else {
			message.setText("<html><body><table cellpadding='5' width='100%'><tr><td align='center'>"+messageText+"</td></tr></table></body></html>");
		}
		
    	switch(state) {	    		
		case OPTIONS:
    		previewBtn.setEnabled(true);
    		saveBtn.setEnabled(true);
    		insightBtn.setEnabled(true);
    		deleteBtn.setEnabled(true);
    		break;
		case OPTIONS_NO_UPLOAD:
			previewBtn.setEnabled(true);
    		saveBtn.setEnabled(true);
    		insightBtn.setEnabled(false);
    		deleteBtn.setEnabled(true);
			break;
		case FATAL:
		case DISABLED:
		case THINKING:
    		previewBtn.setEnabled(false);
    		saveBtn.setEnabled(false);
    		insightBtn.setEnabled(false);
    		deleteBtn.setEnabled(false);
    		break;
    	}
    	
    	currentState = state; // needs to be at end
    }
}
