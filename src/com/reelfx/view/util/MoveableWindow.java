package com.reelfx.view.util;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Frame;
import java.awt.GraphicsConfiguration;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.awt.event.MouseMotionListener;
import java.awt.event.WindowEvent;
import java.awt.event.WindowListener;
import java.util.EventObject;

import javax.swing.JWindow;

import com.reelfx.Applet;


@SuppressWarnings("serial")
public abstract class MoveableWindow extends JWindow implements MouseListener,
		MouseMotionListener, WindowListener, ViewListener {
	
	public final static int MOUSE_PRESS = 300;
	public final static int MOUSE_RELEASE = 301;
	public final static int MOUSE_DRAG = 302;
	
	protected Point mouseOffset = null;
	protected Dimension screen;

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
		screen = Toolkit.getDefaultToolkit().getScreenSize();
		// From Docs: Gets the size of the screen. On systems with multiple
		// displays, the primary display is used. Multi-screen aware display
		// dimensions are available from GraphicsConfiguration and GraphicsDevice
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
		if(e.getSource() == this)
			Applet.sendViewNotification(MOUSE_PRESS, e);
	}

	@Override
	public void mouseReleased(MouseEvent e) {
		mouseOffset = null;
		if(e.getSource() == this)
			Applet.sendViewNotification(MOUSE_RELEASE, e);
	}

	@Override
	public void mouseDragged(MouseEvent e) {
		Point p = e.getLocationOnScreen();
		p.x -= mouseOffset.x;
		p.y -= mouseOffset.y;
		p.x = Math.min(Math.max(p.x, 0), screen.width - this.getWidth());
		p.y = Math.min(Math.max(p.y, 0), screen.height - this.getHeight());
		setLocation(p);
		if(e.getSource() == this)
			Applet.sendViewNotification(MOUSE_DRAG, e);
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
