package com.felixalacampagne.flactagger.gui;

import java.awt.Component;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.io.File;
import java.io.IOException;
import java.util.List;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;

public class DirectorynameTransferHandler extends TransferHandler
{
	private static final long serialVersionUID = 1L;
	TransferHandler defaultHandler=null;
	
	// This was going to be a 10min implementation because, after all, what are GUIs about? Drag and Drop, right? 
	// So doing drag and drop should be trivial to implement, right?
	// No forking way with Java.
	// I got the drop to work eventually, although could not restrict it to directories, only to discover
	// that pasting no longer works at all. 
	// Tried calling the super implementation, constructing with a property but nothing worked.
	// In the end the only way I could get it to work was to store the original TransferHandler of the
	// the component and invoke it's importData if the action does not appear to be a drop. 
	// What a forking awful way to do it - and no forking mention that this is the way it must be done in
	// the docs and they have the nerve to say this is making it easy for the developer!
	
	public DirectorynameTransferHandler(TransferHandler orighandler)
	{
		defaultHandler = orighandler;
	}
	
	protected File getDraggedFile(TransferHandler.TransferSupport support)
	{
		
	Transferable t = support.getTransferable();

      try 
      {
      	
     	Object o = t.getTransferData(DataFlavor.javaFileListFlavor);
     		if((o != null) &&  (o instanceof List))
     		{
     			// Ugh! Java!! Leaving off the <> from List results in a warning.
     			// Putting <File> results in a warning.
     			// Only way to remove the warnings is to use <?> and then check the
     			// type of the elements one by one (but only interested in the first element)
     			List<?> l =  (List<?>) o;
     		 
	         if(l!= null)
	         {
	         	if( (l.get(0) != null) && (l.get(0) instanceof File) )
	         	{
	         		File f = (File) l.get(0);
	         		return f;
	         	}
	         }
     		}
      } catch (UnsupportedFlavorException e) {
          // Ignore this
      } catch (IOException e) {
     	// Ignore this
      } catch (java.awt.dnd.InvalidDnDOperationException wtfex)
      {
      	System.out.println("getDraggedFile: Don't know what this means or how to stop it: " + wtfex.getMessage());
      }
      

      return null;
	}
	
	public boolean canImport(TransferHandler.TransferSupport support) 
   {
		// More Java madness!! This handler should only accept a dragged Directory.
		// To determine the type being dragged requires a getTransferData(DataFlavor.javaFileListFlavor) call.
		// This is fine while canImport is being called during the drag action but as soon
		// as the drop occurs the getTransferData results in "InvalidDnDOperationException: No drop current"
		// and the importData is not invoked. Google is very quiet on this one - there is one question with
		// exactly the same scenario (checking for pdfs in the TransferHandler) but no answers. The only
		// other references are implementing their own DnD functionality and for them the solution appears
		// to be to do event.acceptDrop(DragNDropConstants.ACTION_COPY_OR_MOVE) before getting the data...
		// but there is no acceptDrop either on the Component or in TransferHandler!!!
		// The only reason I dropping to work at all is because I did have a working "drop" in the Clock app 
		// which does not try to verify the file type and by luck decided to revert to what it does!
      if (!support.isDataFlavorSupported(DataFlavor.javaFileListFlavor)) 
      {
          return false;
      }
      
      // Link seems better than the copy or move options which imply the directory will get relocated
      support.setDropAction(LINK);
      return true;
   }

   public boolean importData(TransferHandler.TransferSupport support) 
   {
   	// Yet More Java Stupidity: paste is done via the same API as drag and drop and the base TransferHandler does not perform any pasting
   	// so when the transferhandler of a jtextfield is set to DirectorynameTransferHandler it provide the desired drop behaviour
   	// it breaks the default paste functionality! There is no way to add to the existing transfer handling and the default jtextfield
   	// transferhandler is an internal class dedicated to the jtextfield. So the only way I could get it to work was to
   	// store the original transferhandler and invoke it if the action is not a drop. Clumsy as hell, but at least it is finally
   	// working.
   	if(!support.isDrop())
   	{
   		if(defaultHandler!=null)
   		{
   			return defaultHandler.importData(support);
   		}
   		return false;
   	}
   	
   	
   	File draggedFile = getDraggedFile(support);
      if((draggedFile != null) && draggedFile.isDirectory())
      {
      	// Can't see a way to put the data without knowing
      	// what the destination component is. 
      	// Now that we have the original handler it can be used to handle the text as if it were a paste.
      	// Unfortunately the original handler inserts into the text
      	// already present in the textfield instead of overwriting. This defeats the reason for the drag feature, 
      	// and there isn't a way to change this behaviour, AFAIK.
   		// I'll keep the it for non-textfields to make the class a bit more generic in case I ever use
   		// if for something else!!
      	Component c = support.getComponent();
      	if(c instanceof JTextComponent)
      	{
      		((JTextComponent)c).setText(draggedFile.getAbsolutePath());
      		return true;
      	}
      	else if( c instanceof JComponent)
      	{
	      	StringSelection sstfr = new StringSelection(draggedFile.getAbsolutePath());
	      	defaultHandler.importData((JComponent)c, sstfr);
      	}
      
      }

      return false;
   }

}
