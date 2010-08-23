package com.reelfx.model.util;

import java.util.EventListener;

/**
 * There are two notification setups here. One for all views, called from anywhere, and one for 
 * models (i.e. actual processes) that only controllers are meant to listen to.
 * 
 * Files: ViewListener and ProcessListener
 * 
 * @author Daniel Dixon (http://www.danieldixon.com)
 *
 */
public interface ProcessListener extends EventListener {
	public void processUpdate(int event,Object body);
}
