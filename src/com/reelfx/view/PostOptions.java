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

import org.apache.log4j.Logger;

import com.reelfx.controller.AbstractController;
import com.reelfx.model.PostProcessor;
import com.reelfx.view.util.MessageNotification;
import com.reelfx.view.util.ViewListener;
import com.reelfx.view.util.ViewNotifications;

/**
 * The JPanel displayed in the Java Applet itself so they can be part of the webpage itself.
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
public class PostOptions extends JPanel implements ViewListener {
	
	private static final long serialVersionUID = 4036818007133606840L;
	
	private static Logger logger = Logger.getLogger(PostOptions.class);
	public JButton previewBtn, saveBtn, insightBtn, deleteBtn;
	private final AbstractController controller;
	private JLabel message;
	private ViewNotifications currentState;
	private JPanel self;
	private JProgressBar progressBar;
	
	public PostOptions(final AbstractController controller) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		setBackground(new Color(200,200,200));
		setBorder(BorderFactory.createLineBorder(new Color(102, 102, 102)));
		self = this;
		this.controller = controller;
		
        message = new JLabel();
        message.setFont(new java.awt.Font("Arial", 0, 12));
        message.setMaximumSize(new Dimension(180,1000));
        message.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        message.setAlignmentX(0.5F);
        
        progressBar = new JProgressBar();
        //progressBar.setIndeterminate(true);
        progressBar.setMinimum(0);
        progressBar.setMaximum(100);
        
        JPanel messageBoard = new JPanel();
        messageBoard.setLayout(new BorderLayout());
        messageBoard.setOpaque(false);
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

        insightBtn = new JButton("Upload to Insight");
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
		
		case THINKING_PROGRESS:
			//logger.info("Progress update: "+body.toString());
			progressBar.setIndeterminate(false);
			progressBar.setValue(Integer.parseInt(body.toString()));
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
			progressBar.setIndeterminate(true);
			progressBar.setVisible(true);
		}
		
		currentState = notification; // needs to be at end
	}
}
