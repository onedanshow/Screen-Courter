package com.reelfx.view.util;

import java.util.EventListener;

/**
 * There are two notification setups here. One for all views, called by anyone, and one for 
 * models (i.e. actual processes) that only controllers are meant to listen to.
 * 
 * Files: ViewListener and ProcessListener
 * 
 * @author daniel
 *
 */
public interface ViewListener extends EventListener {
	public void receiveViewNotification(ViewNotifications notification,Object body);
}
