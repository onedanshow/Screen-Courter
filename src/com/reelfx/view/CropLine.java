package com.reelfx.view;

import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.MouseEvent;

import javax.swing.JPanel;
import javax.swing.border.LineBorder;

import com.reelfx.Applet;
import com.reelfx.view.util.MoveableWindow;
import com.reelfx.view.util.ViewNotifications;

@SuppressWarnings("serial")
public class CropLine extends MoveableWindow {
	
	public final static String TOP = "TOP";
	public final static String RIGHT= "RIGHT";
	public final static String BOTTOM = "BOTTOM";
	public final static String LEFT = "LEFT";
	
	public final static int THICKNESS = 3;
	
	public CropLine(String name) {
		super();
		setName(name);
		//setPreferredSize(new Dimension(12, 12));
		setAlwaysOnTop(true);
		JPanel border = new JPanel();
		border.setBackground(Color.BLACK);
		//border.setBorder(new LineBorder(color));
		add(border);
		pack();
		receiveViewNotification(ViewNotifications.CAPTURE_VIEWPORT_CHANGE);
	}

	@Override
	public void receiveViewNotification(ViewNotifications notification,Object body) {
		switch(notification) {
		case CAPTURE_VIEWPORT_CHANGE:
			Point pt = determineFirstViewportPoint();
			if(isVertical()) {
				pt.translate(-1, 0);
			} else {
				pt.translate(0, -1);
			}
			setLocation(pt);
			int width = isVertical() ? THICKNESS : (int)pt.distance(determineSecondViewportPoint());
			int height = isHorizontal() ? THICKNESS : (int)pt.distance(determineSecondViewportPoint());
			setSize(width,height);
			break;
			
		case SHOW_ALL:
		case SHOW_CROP_HANDLES:
			setVisible(true);
			break;
			
		case PRE_RECORDING:
		case RECORDING:
		case HIDE_ALL:
		case HIDE_CROP_HANDLES:
			setVisible(false);
			break;
		}
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		super.mousePressed(e);
		Applet.sendViewNotification(ViewNotifications.MOUSE_PRESS_CROP_LINE, e);
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		super.mouseDragged(e);
		Applet.sendViewNotification(ViewNotifications.MOUSE_DRAG_CROP_LINE, e);
	}

	private boolean isVertical() {
		return getName().equals(LEFT) || getName().equals(RIGHT);
	}

	private boolean isHorizontal() {
		return getName().equals(TOP) || getName().equals(BOTTOM);
	}
	
	private Point determineFirstViewportPoint() {
		if(getName().equals(RIGHT)) {
			return Applet.CAPTURE_VIEWPORT.getTopRightPoint();
		} else if(getName().equals(BOTTOM)) {
			return Applet.CAPTURE_VIEWPORT.getBottomLeftPoint();
		} else {
			return Applet.CAPTURE_VIEWPORT.getTopLeftPoint();
		}
	}

	private Point determineSecondViewportPoint() {
		if(getName().equals(TOP)) {
			return Applet.CAPTURE_VIEWPORT.getTopRightPoint();
		} else if(getName().equals(LEFT)) {
			return Applet.CAPTURE_VIEWPORT.getBottomLeftPoint();
		} else {
			return Applet.CAPTURE_VIEWPORT.getBottomRightPoint();
		}
	}
}
