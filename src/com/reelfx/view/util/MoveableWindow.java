package com.reelfx.view.util;

import java.awt.Dimension;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Rectangle;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;

import javax.swing.JWindow;

import com.reelfx.Applet;

/**
 * Abstract class that is the base for all the always-on-top applet windows.
 * 
 * @author Daniel Dixon (http://www.danieldixon.com)
 *
 */
@SuppressWarnings("serial")
public abstract class MoveableWindow extends JWindow implements MouseListener,
		MouseMotionListener, WindowListener, ViewListener {
	
	protected Point mouseOffset = null;
	public static Rectangle captureViewport;

	public MoveableWindow() {
		super();
		init();
	}

	public MoveableWindow(Frame owner) {
		super(owner);
		init();
	}

	public MoveableWindow(GraphicsConfiguration gc) {
		super(gc);
		init();
	}

	public MoveableWindow(Window owner, GraphicsConfiguration gc) {
		super(owner, gc);
		init();
	}

	public MoveableWindow(Window owner) {
		super(owner);
		init();
	}
	
	protected void init() { 
		addWindowListener(this);
		addMouseListener(this);
		addMouseMotionListener(this);
		
		if(captureViewport == null)
			captureViewport = new Rectangle(new Point(Applet.SCREEN.width/2-400,Applet.SCREEN.height/2-300),new Dimension(800,600));
	}
	
	public void receiveViewNotification(ViewNotifications notification) {
		receiveViewNotification(notification, null);
	}

	@Override
	public void mouseClicked(MouseEvent e) {
		

	}

	@Override
	public void mouseEntered(MouseEvent e) {
		

	}

	@Override
	public void mouseExited(MouseEvent e) {
		

	}

	@Override
	public void mousePressed(MouseEvent e) {
		//mouseOffset = new Point(e.getX(), e.getY());
		mouseOffset = new Point(e.getLocationOnScreen().x-this.getX(),e.getLocationOnScreen().y-this.getY());
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mouseOffset = null;
		//if(e.getSource() == this)
		//	Applet.sendViewNotification(ViewNotifications.MOUSE_RELEASE, e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		// responsibility of capture viewport now, unless on mac
		
		if(!Applet.IS_MAC || Applet.DEV_MODE) return; // TODO temporary
		
		Point p = e.getLocationOnScreen();
		p.x -= mouseOffset.x;
		p.y -= mouseOffset.y;
		p.x = Math.min(Math.max(p.x, 0), Applet.SCREEN.width - this.getWidth());
		p.y = Math.min(Math.max(p.y, 0), Applet.SCREEN.height - this.getHeight());
		setLocation(p);
		//*/
	}

	@Override
	public void mouseMoved(MouseEvent e) {
		

	}

	@Override
	public void windowActivated(WindowEvent e) {
		// gains focus or returned from minimize

	}

	@Override
	public void windowClosed(WindowEvent e) {
		// not called with close operation set to HIDE_FRAME

	}

	@Override
	public void windowClosing(WindowEvent e) {
		

	}

	@Override
	public void windowDeactivated(WindowEvent e) {
		// lose focus or minimized

	}

	@Override
	public void windowDeiconified(WindowEvent e) {
		

	}

	@Override
	public void windowIconified(WindowEvent e) {
		

	}

	@Override
	public void windowOpened(WindowEvent e) {
		

	}

}
