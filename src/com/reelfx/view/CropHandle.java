package com.reelfx.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Graphics;
import java.awt.Insets;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.border.Border;
import javax.swing.border.LineBorder;

import com.reelfx.Applet;
import com.reelfx.view.util.MoveableWindow;
import com.reelfx.view.util.ViewListener;
import com.reelfx.view.util.ViewNotifications;

/**
 * A little box handle for dragging the crop interface.
 * 
 * @author Daniel Dixon (http://www.danieldixon.com)
 *
 */
@SuppressWarnings("serial")
public class CropHandle extends MoveableWindow implements ViewListener {
	
	public final static String TOP_LEFT = "TOP_LEFT";
	public final static String TOP_MIDDLE = "TOP_MIDDLE";
	public final static String TOP_RIGHT = "TOP_RIGHT";
	public final static String MIDDLE_RIGHT = "MIDDLE_RIGHT";
	public final static String BOTTOM_RIGHT = "BOTTOM_RIGHT";
	public final static String BOTTOM_MIDDLE = "BOTTOM_MIDDLE";
	public final static String BOTTOM_LEFT = "BOTTOM_LEFT";
	public final static String MIDDLE_LEFT = "MIDDLE_LEFT";
	
	private ViewNotifications currentState;
	
	public CropHandle(String name) {
		super();
		setName(name);
		setPreferredSize(new Dimension(12, 12));
		setAlwaysOnTop(true);
		JPanel border = new JPanel();
		border.setBackground(Color.WHITE);
		border.setBorder(new LineBorder(Color.BLACK, 2));
		add(border);
		pack();
		receiveViewNotification(ViewNotifications.CAPTURE_VIEWPORT_CHANGE);
	}

	@Override
	public void receiveViewNotification(ViewNotifications notification,Object body) {
		switch(notification) {
		case CAPTURE_VIEWPORT_CHANGE:
			Point pt = determineViewportPoint();
			pt.translate(-getWidth()/2, -getHeight()/2);
			setLocation(pt);
			pack();
			break;
			
		case READY:
			if(Applet.IS_MAC && !Applet.DEV_MODE) break; // TODO temporary
		case SHOW_ALL:
		case SHOW_CROP_HANDLES:
			setAlwaysOnTop(true);
			setVisible(true);
			pack();
			break;
			
		case DISABLE_ALL:
			setAlwaysOnTop(false);
			pack();
			break;
			
		case POST_OPTIONS:
		case POST_OPTIONS_NO_UPLOADING:	
		case PRE_RECORDING:
		case RECORDING:
		case HIDE_ALL:
		case HIDE_CROP_HANDLES:
			setVisible(false);
			pack();
			break;
		
		case MOUSE_PRESS_CROP_LINE:
			toFront();
			pack();
			break;
		}
		currentState = notification;
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if(currentState == ViewNotifications.DISABLE_ALL) return;
		super.mousePressed(e);
		Applet.sendViewNotification(ViewNotifications.MOUSE_PRESS_CROP_HANDLE, e);
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if(currentState == ViewNotifications.DISABLE_ALL) return;
		super.mouseDragged(e);
		Applet.sendViewNotification(ViewNotifications.MOUSE_DRAG_CROP_HANDLE, e);
	}
	
	@Override
	public void mouseReleased(MouseEvent e) {
		if(currentState == ViewNotifications.DISABLE_ALL) return;
		super.mouseReleased(e);
		Applet.sendViewNotification(ViewNotifications.MOUSE_RELEASE_CROP_HANDLE,e);
	}	
	
	/**
	 * The point actually represented by this handle at the moment.
	 * 
	 * @return
	 */
	public Point getViewportPoint() {
		return new Point(getX()+getWidth()/2,getY()+getHeight()/2);
	}
	
	/**
	 * The point that the capture viewport has for this handle.
	 * 
	 * @return
	 */
	private Point determineViewportPoint() {
		if(getName().equals(TOP_LEFT)) {
			return Applet.CAPTURE_VIEWPORT.getTopLeftPoint();
		} else if(getName().equals(TOP_MIDDLE)) {
			return Applet.CAPTURE_VIEWPORT.getTopMiddlePoint();
		} else if(getName().equals(TOP_RIGHT)) {
			return Applet.CAPTURE_VIEWPORT.getTopRightPoint();
		} else if(getName().equals(MIDDLE_RIGHT)) {
			return Applet.CAPTURE_VIEWPORT.getMiddleRightPoint();			
		} else if(getName().equals(BOTTOM_RIGHT)) {
			return Applet.CAPTURE_VIEWPORT.getBottomRightPoint();
		} else if(getName().equals(BOTTOM_MIDDLE)) {
			return Applet.CAPTURE_VIEWPORT.getBottomMiddlePoint();	
		} else if(getName().equals(BOTTOM_LEFT)) {
			return Applet.CAPTURE_VIEWPORT.getBottomLeftPoint();
		} else {
			return Applet.CAPTURE_VIEWPORT.getMiddleLeftPoint();
		}
	}

}
