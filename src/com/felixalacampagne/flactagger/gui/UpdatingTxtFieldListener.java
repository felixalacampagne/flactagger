package com.felixalacampagne.flactagger.gui;


import javax.swing.SwingUtilities;
import javax.swing.event.DocumentEvent;
import javax.swing.event.DocumentListener;

public class UpdatingTxtFieldListener implements DocumentListener {

Runnable updater = null;

	// Bizarrely 'Runnable' is now the name of the 'functional interface' of a 'function' which
	// takes 'void' and returns 'void'.
	// Note that all the method needs to do is to have a signature like 'void func(void)' and it can
	// be passed as an argument into the constructor, eg. new UpdatingTxtFieldListener(this::func)
	public UpdatingTxtFieldListener(Runnable updateFunc) 
	{
		updater = updateFunc;
	}

	  protected void changed(DocumentEvent e)
	  {
		  // Keep getting white screen at startup and think it is due to trying to update components
		  // while they are still initialiseing. This is attempt to avoid the white screen and might
		  // even be a good idea since the updater could cause more events which might in turn cause
		  // a conflict. Luckily the updater method is considered to be a Runnable which is just the
		  // ticket for invokeLater! Unfortunately this is really difficult to debug since it only
		  // seems to occur on the first invocation of a "session" of invocations.
		  SwingUtilities.invokeLater(updater);
		  
	  }
		@Override
		public void changedUpdate(DocumentEvent e) {
			changed(e);
			
		}
	
		@Override
		public void insertUpdate(DocumentEvent e) {
			changed(e);
		}
	
		@Override
		public void removeUpdate(DocumentEvent e) {
			changed(e);
		}

}
