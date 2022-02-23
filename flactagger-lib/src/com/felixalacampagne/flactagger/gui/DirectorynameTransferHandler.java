package com.felixalacampagne.flactagger.gui;

import java.awt.Component;
import java.awt.datatransfer.Clipboard;
import java.awt.datatransfer.DataFlavor;
import java.awt.datatransfer.StringSelection;
import java.awt.datatransfer.Transferable;
import java.awt.datatransfer.UnsupportedFlavorException;
import java.awt.dnd.DropTargetDragEvent;
import java.awt.dnd.DropTargetDropEvent;
import java.awt.dnd.DropTargetEvent;
import java.awt.dnd.DropTargetListener;
import java.awt.event.InputEvent;
import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.TooManyListenersException;

import javax.swing.JComponent;
import javax.swing.TransferHandler;
import javax.swing.text.JTextComponent;

public class DirectorynameTransferHandler extends TransferHandler implements DropTargetListener 
{
	public static void addToComponent(JComponent component)
	{
		// This looks weird, which is why I've hidden it here...
		new DirectorynameTransferHandler(component);
	}
	
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
	
	public DirectorynameTransferHandler(JComponent component)
	{
		defaultHandler = component.getTransferHandler();
		component.setTransferHandler(this);
		try
		{
			component.getDropTarget().addDropTargetListener(this);
		}
		catch (TooManyListenersException e)
		{
			e.printStackTrace();
		}
	}
	
	protected File getDraggedFile(Transferable t)
	{
		
	//Transferable t = support.getTransferable();

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
      	// It means there is a bug in Java which stops the dragged data from being examined during canImport.
      	// Only workaround is to not check whether a directory is being dragged.
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
		if(state != DragState.Accept)
		{
			//System.out.println("canImport: state=" + state);
			return false;
		}
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
   	// Yet More Java Stupidity: paste is done via the same API as drag and drop and the base TransferHandler does not perform pasting
   	// so when the transferhandler of a jtextfield is set to DirectorynameTransferHandler it provide the desired drop behaviour but
   	// it breaks the default paste functionality! There is no way to extend the existing transfer handling and the default jtextfield
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
   	
   	
   	File draggedFile = getDraggedFile(support.getTransferable());
      if((draggedFile != null) && draggedFile.isDirectory())
      {
      	// Can't see a way to put the data without knowing what the destination component is. 
      	// Now that we have the original handler it can be used to handle the text as if it were a paste.
      	// Unfortunately the original handler inserts into the text which is not the behaviour I require for the drag feature, 
      	// and there isn't a way to change this behaviour, AFAIK.
   		// I'll keep it for non-textfields to make the class a bit more generic in case I ever use if for something else!!
      	Component c = support.getComponent();
      	if(c instanceof JTextComponent)
      	{
            JTextComponent txtfld = ((JTextComponent)c);
            String droptxt = draggedFile.getAbsolutePath();
      	   // Undoing a dropped directory name results in an empty field,
      	   // undoing a second time restores the value that was dropped on.
      	   // Don't know how to avoid the blank field... below are some attempts
      	   // 1. Select the text and replace it with the dropped value
      	   //txtfld.selectAll();
      	   //txtfld.replaceSelection(droptxt);
      	   // same undo behaviour...
      	   
      	   // So, keep it simple, until something that works pops up!!
      	   txtfld.setText(droptxt);
      	   
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

   // The madness continues: adding the transfer handler to a textfield breaks
   // copy to clipboard! Apparently because TransferHandler is also invoked for copying
   public void exportToClipboard(JComponent comp, Clipboard clip, int action) 
   {
   	if(defaultHandler != null)
   	{
   		defaultHandler.exportToClipboard(comp, clip, action);
   	}
   	else
   	{
   		super.exportToClipboard(comp, clip, action);
   	}
   }

   // I guess this should also be passed on to the original handler
   public void exportAsDrag(JComponent comp, InputEvent e, int action)  
   {
   	if(defaultHandler != null)
   	{
   		defaultHandler.exportAsDrag(comp, e, action);
   	}
   	else
   	{
   		super.exportAsDrag(comp, e, action);
   	}
   }

   
   
   public enum DragState {

      Waiting,
      Accept,
      Reject
  }
  private DragState state = DragState.Waiting;
 
  @Override
  public void dragEnter(DropTargetDragEvent dtde) {
     Transferable t = dtde.getTransferable();
     File f = getDraggedFile(t);
     if(f.isDirectory())
     {
   	  //dtde.acceptDrag(DnDConstants.ACTION_COPY);
   	  state = DragState.Accept;
   	  //System.out.println("dragEnter:Accept");
     }
     else
     {
   	  state = DragState.Reject;
   	  //System.out.println("dragEnter:Reject");
     }
     dtde.rejectDrag();
  }

  @Override
  public void dragOver(DropTargetDragEvent dtde) {
  }

  @Override
  public void dropActionChanged(DropTargetDragEvent dtde) {
	  
  }

  @Override
  public void dragExit(DropTargetEvent dte) {
	  state = DragState.Waiting;
	  //System.out.println("dragExit:Waiting");
  }

  @Override
  public void drop(DropTargetDropEvent dtde) {
   	
  }


}
