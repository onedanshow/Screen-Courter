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
		case SET_CAPTURE_VIEWPORT_RESOLUTION: // TODO used?
			String resolution = ((String) body).toLowerCase();
			if(resolution.equals("fullscreen")) {
				setFrameFromDiagonal(0,0,SCREEN.getWidth(),SCREEN.getHeight());
			} else if(resolution.contains("x")) {
				String[] dim = resolution.split("x");
				int width = Integer.parseInt(dim[0]);
				int height = Integer.parseInt(dim[1]);
				Point pt = getTopLeftPoint();
				if(pt.x + width > SCREEN.width) {
					pt.x -= Math.max(0,pt.x + width - SCREEN.width);
				}
				if(pt.y + height > SCREEN.height) {
					pt.y -= Math.max(0,pt.y + height - SCREEN.height);
				}
				setFrameFromDiagonal(pt.x,pt.y,pt.x+width,pt.y+height);
			}
			break;
		
		case MOUSE_DRAG_CROP_HANDLE:
			me = (MouseEvent) body;
			handle = (CropHandle) me.getSource();
			if(handle.getName().equals(CropHandle.TOP_LEFT)) {
				setFrameFromDiagonal(cleanPoint(me.getLocationOnScreen()),getBottomRightPoint());
			} 
			else if(handle.getName().equals(CropHandle.TOP_MIDDLE)) {
				setFrameFromDiagonal(cleanPoint((int)getMinX(),me.getLocationOnScreen().y),getBottomRightPoint());
			} 
			else if(handle.getName().equals(CropHandle.TOP_RIGHT)) {
				setFrameFromDiagonal(
						cleanPoint((int)getMinX(),me.getLocationOnScreen().y),
						cleanPoint(me.getLocationOnScreen().x,(int)getMaxY()));
			}
			else if(handle.getName().equals(CropHandle.MIDDLE_RIGHT)) {
				setFrameFromDiagonal(getTopLeftPoint(),
						cleanPoint(me.getLocationOnScreen().x,(int)getMaxY()));
			}
			else if(handle.getName().equals(CropHandle.BOTTOM_RIGHT)) {
				setFrameFromDiagonal(getTopLeftPoint(), cleanPoint(me.getLocationOnScreen()));
			}
			else if(handle.getName().equals(CropHandle.BOTTOM_MIDDLE)) {
				setFrameFromDiagonal(getTopLeftPoint(), 
						cleanPoint((int)getMaxX(),me.getLocationOnScreen().y));
			}
			else if(handle.getName().equals(CropHandle.BOTTOM_LEFT)) {
				setFrameFromDiagonal(
						cleanPoint(me.getLocationOnScreen().x,(int)getMinY()),
						cleanPoint((int)getMaxX(),me.getLocationOnScreen().y));
			}
			else if(handle.getName().equals(CropHandle.MIDDLE_LEFT)) {
				setFrameFromDiagonal(cleanPoint(me.getLocationOnScreen().x,(int)getMinY()), getBottomRightPoint());
			}
			break;
			
		//case MOUSE_PRESS_CROP_LINE:
		case MOUSE_PRESS_RECORD_CONTROLS:	
		case MOUSE_PRESS_INFO_BOX:
			me = (MouseEvent) body;
			mouseOffset = new Point(me.getLocationOnScreen().x-(int)getX(), me.getLocationOnScreen().y-(int)getY());
			break;
			
		//case MOUSE_DRAG_CROP_LINE:
		case MOUSE_DRAG_RECORD_CONTROLS:	
		case MOUSE_DRAG_INFO_BOX:
			if(Applet.IS_MAC && !Applet.DEV_MODE) break; // TODO temporary
			
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
	
	// Windows ffmpeg has problem when clip window size is not factor of 2
	private Point cleanPoint(Point pt) {
		if(Applet.IS_WINDOWS)
			return cleanPoint(pt.x, pt.y);
		else
			return pt;
	}
	
	// Windows ffmpeg has problem when clip window size is not factor of 2
	private Point cleanPoint(int x, int y) {	
		if(Applet.IS_WINDOWS) {
			return new Point(x % 2 == 0 ? x : --x, y % 2 == 0 ? y : --y);
		} else {
			return new Point(x,y);
		}
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

	public Point getMiddleRightPoint() {
		return new Point((int)getMaxX(),(int)getMinY()+(int)Math.floor(getHeight()/2.0));
	}
	
	public Point getBottomRightPoint() {
		return new Point((int)getMaxX(),(int)getMaxY());
	}
	
	public Point getBottomMiddlePoint() {
		return new Point((int)getMinX()+(int)Math.floor(getWidth()/2.0),(int)getMaxY());
	}
	
	public Point getBottomLeftPoint() {
		return new Point((int)getMinX(),(int)getMaxY());
	}
	
	public Point getMiddleLeftPoint() {
		return new Point((int)getMinX(),(int)getMinY()+(int)Math.floor(getHeight()/2.0));
	}
}
