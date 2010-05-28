package com.reelfx.model;

import java.awt.Dimension;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Toolkit;
import java.awt.event.MouseEvent;

import com.reelfx.Applet;
import com.reelfx.view.CropHandle;
import com.reelfx.view.util.MoveableWindow;
import com.reelfx.view.util.ViewListener;
import com.reelfx.view.util.ViewNotifications;

@SuppressWarnings("serial")
public class CaptureViewport extends Rectangle implements ViewListener {

	public static Dimension SCREEN;
	
	protected Point mouseOffset = null;
	
	public CaptureViewport() {
		super(new Dimension(800,600));
		SCREEN = Toolkit.getDefaultToolkit().getScreenSize();	
		setLocation(SCREEN.width/2-width/2, SCREEN.height/2-height/2);
	}
	
	@Override
	public void receiveViewNotification(ViewNotifications notification,Object body) {
		MouseEvent me;
		CropHandle handle;
		switch(notification) {
		case MOUSE_DRAG_CROP_HANDLE:
			me = (MouseEvent) body;
			handle = (CropHandle) me.getSource();
			if(handle.getName().equals(CropHandle.TOP_LEFT)) {
				setFrameFromDiagonal(handle.getViewportPoint(),getBottomRightPoint());
			} 
			else if(handle.getName().equals(CropHandle.TOP_MIDDLE)) {
				setFrameFromDiagonal(new Point(getTopLeftPoint().x,handle.getViewportPoint().y),getBottomRightPoint());
			} 
			else if(handle.getName().equals(CropHandle.TOP_RIGHT)) {
				setFrameFromDiagonal(
						new Point(getTopLeftPoint().x,handle.getViewportPoint().y),
						new Point(handle.getViewportPoint().x,getBottomRightPoint().y));
			}
			break;
			
		case MOUSE_PRESS_RECORD_CONTROLS:	
		case MOUSE_PRESS_INFO_BOX:
			me = (MouseEvent) body;
			mouseOffset = new Point(me.getLocationOnScreen().x-(int)getX(), me.getLocationOnScreen().y-(int)getY());
			break;
			
		case MOUSE_DRAG_RECORD_CONTROLS:	
		case MOUSE_DRAG_INFO_BOX:
			me = (MouseEvent) body;
			Point p = me.getLocationOnScreen();
			p.x -= mouseOffset.x;
			p.y -= mouseOffset.y;
			p.x = (int) Math.min(Math.max(p.x, 0), SCREEN.width - getWidth());
			p.y = (int) Math.min(Math.max(p.y, 0), SCREEN.height - getHeight());
			setLocation(p);
			Applet.sendViewNotification(ViewNotifications.CAPTURE_VIEWPORT_CHANGE);
			break;
		}
	}
	
	/**
	 * Base method for all frame alterations, so override it to send view notification.
	 */
	@Override
	public void setFrame(double x, double y, double w, double h) {
		super.setFrame(x, y, w, h);
		Applet.sendViewNotification(ViewNotifications.CAPTURE_VIEWPORT_CHANGE);
	}
	
	public Point getTopLeftPoint() {
		return new Point((int)getMinX(),(int)getMinY());
	}

	public Point getTopMiddlePoint() {
		return new Point((int)getMinX()+(int)Math.floor(getWidth()/2.0),(int)getMinY());
	}	
	
	public Point getTopRightPoint() {
		return new Point((int)getMaxX(),(int)getMinY());
	}
	
	public Point getBottomLeftPoint() {
		return new Point((int)getMinX(),(int)getMaxY());
	}
	
	public Point getBottomRightPoint() {
		return new Point((int)getMaxX(),(int)getMaxY());
	}
}
