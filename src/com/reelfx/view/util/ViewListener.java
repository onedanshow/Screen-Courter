package com.reelfx.view.util;

import java.util.EventListener;

/**
 * There are two notification setups here. One for all views, called by anyone, and one for 
 * models (i.e. actual processes) that only controllers are meant to listen to.
 * 
 * Files: ViewListener and ProcessListener
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
public interface ViewListener extends EventListener {
	public void receiveViewNotification(ViewNotifications notification,Object body);
}
