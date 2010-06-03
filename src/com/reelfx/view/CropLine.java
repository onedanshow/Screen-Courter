package com.reelfx.view;

import java.awt.BasicStroke;
import java.awt.Color;
import java.awt.Graphics;
import java.awt.Graphics2D;
import java.awt.Point;
import java.awt.Stroke;
import java.awt.event.MouseEvent;
import java.awt.geom.Line2D;

import javax.swing.JPanel;

import com.reelfx.Applet;
import com.reelfx.view.util.MoveableWindow;
import com.reelfx.view.util.ViewNotifications;

@SuppressWarnings("serial")
public class CropLine extends MoveableWindow {
	
	public final static String TOP = "TOP";
	public final static String RIGHT= "RIGHT";
	public final static String BOTTOM = "BOTTOM";
	public final static String LEFT = "LEFT";
	
	public final static int THICKNESS = 2;
	
	private ViewNotifications currentState;
	
	public CropLine(String name) {
		super();
		setName(name);
		setAlwaysOnTop(true);
		add(new LinePaint());
		pack();
		receiveViewNotification(ViewNotifications.CAPTURE_VIEWPORT_CHANGE);
	}

	@Override
	public void receiveViewNotification(ViewNotifications notification,Object body) {
		switch(notification) {
		case CAPTURE_VIEWPORT_CHANGE:
			Point pt = determineFirstViewportPoint();
			/*if(getName().equals(TOP)) {
				pt.translate(0, -1);
			} else if(getName().equals(RIGHT)) {
				pt.translate(1,0);
			} else if(getName().equals(BOTTOM)) {
				pt.translate(0,1);
				*/
			if(getName().equals(LEFT)) {
				pt.translate(-2,0);
			}
			setLocation(pt);
			int width = isVertical() ? THICKNESS : (int)pt.distance(determineSecondViewportPoint());
			int height = isHorizontal() ? THICKNESS : (int)pt.distance(determineSecondViewportPoint());
			setSize(width,height);
			break;
			
		case SHOW_ALL:
			setAlwaysOnTop(true);
			setVisible(true);
			break;
			
		case DISABLE_ALL:
			setAlwaysOnTop(false);
			break;
			
		case HIDE_ALL:
			setVisible(false);
			break;
		}
		
		currentState = notification;
		//pack(); // don't call
	}
	
	@Override
	public void mousePressed(MouseEvent e) {
		if(currentState == ViewNotifications.DISABLE_ALL) return;
		super.mousePressed(e);
		Applet.sendViewNotification(ViewNotifications.MOUSE_PRESS_CROP_LINE, e);
	}
	
	@Override
	public void mouseDragged(MouseEvent e) {
		if(currentState == ViewNotifications.DISABLE_ALL) return;
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
	
	class LinePaint extends JPanel {
		Stroke drawingStroke = new BasicStroke(3, BasicStroke.CAP_BUTT, BasicStroke.JOIN_BEVEL, 0, new float[]{6}, 0);
		
		public LinePaint() {
			super();
			setBackground(Color.WHITE);
		}

		@Override
		protected void paintComponent(Graphics g) {
			super.paintComponent(g);
			Graphics2D g2d = (Graphics2D) g;
			g2d.setStroke(drawingStroke);
			g2d.setColor(Color.black);
			if(getWidth() > getHeight())
				g2d.draw(new Line2D.Double(getX(),getY(),getWidth(),getY()));
			else
				g2d.draw(new Line2D.Double(getX(),getY(),getX(),getHeight()));
		}
	}
}
