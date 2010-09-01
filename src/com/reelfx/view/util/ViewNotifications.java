package com.reelfx.view.util;

/**
 * List of notifications that can be sent to the view
 * 
 * @author Daniel Dixon (http://www.danieldixon.com)
 * 
 * Helpful notes on enums: http://java.sun.com/j2se/1.5.0/docs/guide/language/enums.html
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
public enum ViewNotifications {
	MOUSE_PRESS_INFO_BOX,
	MOUSE_RELEASE_INFO_BOX,
	MOUSE_DRAG_INFO_BOX,
	
	MOUSE_PRESS_RECORD_CONTROLS,
	MOUSE_RELEASE_RECORD_CONTROLS,
	MOUSE_DRAG_RECORD_CONTROLS,
	
	MOUSE_PRESS_CROP_HANDLE,
	MOUSE_DRAG_CROP_HANDLE,
	MOUSE_RELEASE_CROP_HANDLE,
	
	MOUSE_PRESS_CROP_LINE,
	MOUSE_DRAG_CROP_LINE,
	
	CAPTURE_VIEWPORT_CHANGE,
	SET_CAPTURE_VIEWPORT,
	
	HIDE_CROP_HANDLES,
	SHOW_CROP_HANDLES,
	
	HIDE_INFO_BOX,
	SHOW_INFO_BOX,
	
	HIDE_CROP_LINES,
	SHOW_CROP_LINES,
	
	HIDE_RECORD_CONTROLS,
	SHOW_RECORD_CONTROLS,
	
	SHOW_ALL,
	DISABLE_ALL,
	HIDE_ALL,
	
	READY,
	POST_OPTIONS,
	POST_OPTIONS_NO_UPLOADING,
	PRE_RECORDING,
	RECORDING,
	THINKING,
	THINKING_PROGRESS,
	FATAL
}
