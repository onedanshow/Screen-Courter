package com.reelfx.view;

import java.awt.Dimension;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JSeparator;

import com.reelfx.controller.ApplicationController;

public class OptionsInterface extends JPanel {
	
	private static final long serialVersionUID = 4036818007133606840L;
	
	public JButton recordBtn, previewBtn, saveBtn, insightBtn, deleteBtn;
	private final ApplicationController controller;
	private JLabel message;
	
	public OptionsInterface(final ApplicationController controller) {
		super();
		setLayout(new BoxLayout(this, BoxLayout.LINE_AXIS));
		
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
        message.setFont(new java.awt.Font("Arial", 0, 13));
        message.setOpaque(false);
        message.setMaximumSize(new Dimension(180,1000));
        message.setHorizontalAlignment(javax.swing.SwingConstants.CENTER);
        message.setAlignmentX(0.5F);
        
        add(new JSeparator(JSeparator.VERTICAL));
        
        add(message);
        
        previewBtn = new JButton("Preview It");
        previewBtn.setFont(new java.awt.Font("Arial", 0, 13));
        previewBtn.setAlignmentX(0.5F);
        previewBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                controller.previewRecording();
            }
        });
        add(previewBtn);

        saveBtn = new JButton("Save to My Computer");
        saveBtn.setFont(new java.awt.Font("Arial", 0, 13));
        saveBtn.setAlignmentX(0.5F);
        saveBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                //controller.saveRecording();
            }
        });
        add(saveBtn);

        insightBtn = new JButton("Post to Insight");
        insightBtn.setFont(new java.awt.Font("Arial", 0, 13));
        insightBtn.setAlignmentX(0.5F);
        insightBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                controller.postRecording();
            }
        });
        add(insightBtn);
        
        deleteBtn = new JButton("Delete It");
        deleteBtn.setFont(new java.awt.Font("Arial", 0, 13));
        deleteBtn.setAlignmentX(0.5F);
        deleteBtn.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
            	controller.deleteRecording();
            }
        });
        add(deleteBtn);
	}
	
}
