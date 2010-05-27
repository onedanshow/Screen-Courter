package com.reelfx.view;

import java.awt.Component;
import java.awt.Dimension;
import java.awt.Event;
import java.awt.Point;
import java.awt.Toolkit;
import java.awt.Window;
import java.awt.event.MouseEvent;
import java.awt.event.MouseListener;
import java.util.EventObject;

import javax.swing.JWindow;

import com.reelfx.view.util.MoveableWindow;

@SuppressWarnings("serial")
public class CenterInterface extends MoveableWindow {

	public final static String NAME = "CenterWindow";

	public CenterInterface() {
		super();
	}

	public CenterInterface(Window owner) {
		super(owner);
	}

	@Override
	protected void init() {
		super.init();
		setPreferredSize(new Dimension(300, 300));
		setAlwaysOnTop(true);
		setName(NAME);
	}

	@Override
	public void receiveViewNotification(int notification, Object body) {
		MouseEvent me;
		switch(notification) {
		
		case MoveableWindow.MOUSE_PRESS:
			me = (MouseEvent) body;
			if(me.getSource() == this) break;
			super.mousePressed(me);				
			break;
		
		case MoveableWindow.MOUSE_DRAG:
			me = (MouseEvent) body;
			if(me.getSource() == this) break;
			super.mouseDragged(me);				
			break;
		}
	}
}
