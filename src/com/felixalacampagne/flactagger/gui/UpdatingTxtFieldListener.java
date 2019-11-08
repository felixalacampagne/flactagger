package com.felixalacampagne.flactagger.gui;


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
		  //setExtUpd();
		  updater.run();
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
