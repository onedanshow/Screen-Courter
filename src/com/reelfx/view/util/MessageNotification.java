package com.reelfx.view.util;

/**
 * This is a view notification that's meant to carrying messages for the parts of the screen that need such things.
 * 
 * @author Daniel Dixon (http://www.danieldixon.com)
 *
 */
public class MessageNotification {
	public String statusText = "";
	public String messageText = "";
	
	public MessageNotification(String statusText) {
		super();
		this.statusText = statusText;
		this.messageText = "";
	}
	public MessageNotification(String statusText, String messageText) {
		super();
		this.statusText = statusText;
		this.messageText = messageText;
	}
	public String getStatusText() {
		return statusText;
	}
	public void setStatusText(String statusText) {
		this.statusText = statusText;
	}
	public String getMessageText() {
		return messageText;
	}
	public void setMessageText(String messageText) {
		this.messageText = messageText;
	}	
}
