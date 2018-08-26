package com.smallcatutilities.flactagger.gui;

import java.awt.BorderLayout;
import java.awt.Color;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.beans.PropertyChangeEvent;
import java.beans.PropertyChangeListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.List;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JCheckBox;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.JTextPane;
import javax.swing.SwingConstants;
import javax.swing.SwingWorker;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;
import javax.swing.text.AttributeSet;
import javax.swing.text.BadLocationException;
import javax.swing.text.Document;
import javax.swing.text.MutableAttributeSet;
import javax.swing.text.SimpleAttributeSet;
import javax.swing.text.StyleConstants;

import com.smallcatutilities.flactagger.FLACtagger;


public class FLACtaggerGui
{
private enum TaggerAction { EXTRACT, UPDATE };
private static final String PROP_FILE = "flactagger.properties";
private static final String PROP_ROOTDIR = "flactagger.rootdir";
private static final String PROP_TAGFILE = "flactagger.tagfile";
private static final String PROP_CALCMD5 = "flactagger.calcmd5";


private static final String PROP_LOCATION = "flactagger.location";

private static final String MSG_DONE = "Done.";
private static final String MSG_FAILED = "Failed!";
private static final String MSG_EXCEPTION = "Exception!!!!!!";

private JFrame mainframe;
private JTextField txtRootDir;
private JTextField txtFlacTagsFile;
private JCheckBox chkMD5;
private JButton btnExtract;
private JButton btnUpdate;
private JTextPane logdisplay;

private Properties settings = new Properties();

private ActionListener updateAction = new ActionListener(){
	@Override
	public void actionPerformed(ActionEvent e)
	{
		doTask(TaggerAction.UPDATE);
	}
};

private ActionListener extractAction = new ActionListener(){
	@Override
	public void actionPerformed(ActionEvent e)
	{
		doTask(TaggerAction.EXTRACT);
	}
};


WindowAdapter exitEvent = new WindowAdapter() 
{
   @Override
   public void windowClosing(java.awt.event.WindowEvent windowEvent) 
   {
   	try
   	{
   	Point p = mainframe.getLocation();
   	String loc = String.format("%d,%d", (int) p.getX(), (int)p.getY()); 
   	settings.setProperty(PROP_LOCATION, loc);
   	 
   	saveSettings();
   	}
   	catch(Exception ex) {} // We are exiting, ensure nothing gets in the way
   	System.exit(0);
    }
};


public FLACtaggerGui()
{
	try
	{
		UIManager.setLookAndFeel("javax.swing.plaf.nimbus.NimbusLookAndFeel");
	}
	catch (Exception ex)
	{
		// Ignore this - I copied it from somewhere and have no idea if it is valid or not
	}
	init();
	
}

protected void popupBox(String msg, int status)
{
	JOptionPane.showMessageDialog(mainframe, msg, "FLACtagger", status);
}

protected void setEnabled(boolean enable)
{
	// A better way to do this might be to use a glass pane over everything which can
	// be enabled/disabled, but that would prevent a Cancel option from being added,
	// unless it can be added on top of the Glass pane.
	// actual state of the Update and Extract buttons depends on the content of the text fields
	// and is handled by setExtUpd
	if(enable)
	{
		setExtUpd();
	}
	else
	{
		this.btnExtract.setEnabled(enable);
		this.btnUpdate.setEnabled(enable);
	}
	this.txtFlacTagsFile.setEnabled(enable);
	this.txtRootDir.setEnabled(enable);
	
	// Should also disable the directory/file choosers.
}

// NICETOHAVE: a Cancel button to abort the task... don't think it is possible with the existing FLACtagger
// unless there is a way to kill the SwingWorker thread.
protected void doTask(TaggerAction action)
{
final TaggerTask task = new TaggerTask(action, logdisplay, getRootDir(), getFlactagFile(), isCalcMD5Enabled());
	logdisplay.setText("");
	task.addPropertyChangeListener(new PropertyChangeListener()
	{
			public void propertyChange(PropertyChangeEvent evt)
			{
				String prop = evt.getPropertyName();
				Object obj = evt.getNewValue();
				if("state".equals(prop))
				{
					switch((SwingWorker.StateValue)obj)
					{
					case STARTED:
						setEnabled(false);
						break;
					case DONE:
						task.removePropertyChangeListener(this);
						setEnabled(true);
						break;
					case PENDING:
						break;
					default:
						break;
					}
				}
			}			
	});
	task.execute();
}

// NICETOHAVE: This might be better done as a listener on the text boxes rather than relying
// on the setters being called.
protected void setExtUpd()
{
String r= getRootDir();
String t = getFlactagFile();
boolean b = false;
	if((r != null) && (r.length()>0) && (t != null) && (t.length()>0))
	{
		b = true;
	}
	btnExtract.setEnabled(b);
	btnUpdate.setEnabled(b);
}
protected void setRootDir(String root)
{
	txtRootDir.setText(root);
	setExtUpd();
}

protected String getRootDir()
{
String root = txtRootDir.getText();

// remove the annoying quotes added when using a pasted "copy as path".
// regex is a bit of overkill but I copied it from the filechooser
// Could check for a full path, but since it's only me who will use this
// and consequently only a pasted full path will be quoted, I'm not going to....
Matcher mat = Pattern.compile("^\"(\\p{Alpha}:.*)\"$").matcher(root);
if(mat.matches())
{
   root = mat.group(1);
   txtRootDir.setText(root); // Correct the displayed value
}
   return root;
}

protected void setFlactagFile(String tagfile)
{
	txtFlacTagsFile.setText(tagfile);
	setExtUpd();
}

protected void setCalcMD5Enabled(boolean enabled)
{
   chkMD5.setSelected(enabled);
}

protected boolean isCalcMD5Enabled()
{
   return chkMD5.isSelected();
}

protected String getFlactagFile()
{
	return txtFlacTagsFile.getText();
}

private void init()
{
  JPanel pnl;
  BoxLayout bl;

  
  mainframe = new JFrame("FLACtagger");
  mainframe.setDefaultCloseOperation(JFrame.DO_NOTHING_ON_CLOSE);
  mainframe.addWindowListener(exitEvent);
  mainframe.getContentPane().setPreferredSize(new Dimension(450, 400));
  mainframe.getContentPane().setLayout(new BoxLayout(mainframe.getContentPane(), BoxLayout.Y_AXIS));
  
  mainframe.setResizable(false);

  // Root dir - where to find the directories
  //   Label, textbox to display value, button for dir chooser
  txtRootDir = new JTextField();
  JLabel lbl1 = new JLabel("Base directory:");
  lbl1.setLabelFor(txtRootDir); 
  lbl1.setToolTipText("Directory containing the directories to scan for FLAC files. All sub-directories in the base directory will be scanned.");
  JButton btnRootDir = new JButton("...");

  pnl = new JPanel(); 
  bl = new BoxLayout(pnl,BoxLayout.X_AXIS);
  pnl.setLayout(bl);
  pnl.add(lbl1);
  
  pnl.add(txtRootDir);
  pnl.add(btnRootDir);
  mainframe.getContentPane().add(pnl, BorderLayout.CENTER);
  
  btnRootDir.addActionListener(
     new ActionListener()
     {
    	 @Override
    	 public void actionPerformed(ActionEvent e)
    	 {
    	 JFileChooser chooser = new JFileChooser(); 
    	 String sf = getRootDir();
    	 	if((sf == null) || (sf.length() < 2))
    	 		sf = ".";

    	 	chooser.setCurrentDirectory(new File(sf));
    	 	chooser.setDialogTitle("Choose base directory");
    	 	chooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
    	 	chooser.setAcceptAllFileFilterUsed(false);
    	 	
    	 	if (chooser.showOpenDialog(mainframe) == JFileChooser.APPROVE_OPTION) 
    	 	{ 
    	 		File sel = chooser.getSelectedFile();
    	 		
    	 		// Java too stupid to realise when a path is pasted into it, instead it
    	 		// appends the quoted text onto the current directory. Can't find a practical way to
    	 		// intercept the quotes to try to crudely detected a quoted filename and assume its
    	 		// a full path name. It's even worse than that. The quoted text is not even interpretted
    	 		// as the name, it's just blindly added to the current directory and then the path
    	 		// is parsed as if the quotes are not there with the result that the name is the last directory
    	 		// with a quote at the end but not the begining!!
    	 		String name = sel.getAbsolutePath();
    	 		Matcher mat = Pattern.compile("^.*\"(\\p{Alpha}:.*)\"$").matcher(name);
    	 		if(mat.matches())
    	 		{
    	 		   sel = new File(mat.group(1));
    	 		}

    	 		if(!sel.isDirectory())
    	 		{
    	 			sel = sel.getParentFile();
				}
    	 		setRootDir(sel.getAbsolutePath());
			}
    	 	else 
    	 	{
    	 		System.out.println("No Selection ");
    	 	}
    	 }
     });
  
  // XML file - for output/input dpending on mode
  //   Label, textbox to display value, button for file chooser
  txtFlacTagsFile = new JTextField();
  JLabel lbl2 = new JLabel("Flac tags file:");
  lbl2.setLabelFor(txtFlacTagsFile);
  lbl2.setToolTipText("File to receive extracted tags or file containing tags to perform update with.");
  JButton btnTagFile = new JButton("...");
  pnl = new JPanel(); 
  bl = new BoxLayout(pnl,BoxLayout.X_AXIS);
  pnl.setLayout(bl);
  pnl.add(lbl2);

  pnl.add(Box.createRigidArea(new Dimension(8, 0)));
  
  
  pnl.add(txtFlacTagsFile);
  pnl.add(btnTagFile);
  mainframe.getContentPane().add(pnl, BorderLayout.CENTER);

  
   btnTagFile.addActionListener(new ActionListener()
   {
      @Override
	  public void actionPerformed(ActionEvent e)
	  {
      String sf = getFlactagFile();
      JFileChooser chooser = new JFileChooser(); 
         chooser.setDialogTitle("Choose tag file");
      	 chooser.setAcceptAllFileFilterUsed(true);
      	 chooser.setApproveButtonText("Select");
      	 chooser.setFileFilter(new FileNameExtensionFilter("FLACtagger files", "xml", "txt"));
      	 if((sf == null) || (sf.length() < 2))
      	 {
      		chooser.setCurrentDirectory(new File("."));
      	 }
      	 else
      	 {
      		chooser.setSelectedFile(new File(sf));
      	 }
      	 if (chooser.showOpenDialog(mainframe) == JFileChooser.APPROVE_OPTION) 
         { 
      		File sel = chooser.getSelectedFile();
      		setFlactagFile(sel.getAbsolutePath());
      	 }
      	 else 
      	 {
      		System.out.println("No Selection ");
         }
      }
   });
  

   chkMD5 = new JCheckBox("Calculate MD5");
   chkMD5.setHorizontalTextPosition(SwingConstants.LEADING);
   chkMD5.setToolTipText("Calculate the audio only MD5 when extracting tags (slow)");
//   pnl = new JPanel(); 
//   bl = new BoxLayout(pnl,BoxLayout.X_AXIS);
//   pnl.setLayout(bl);   
//   pnl.add(chkMD5);
//   mainframe.getContentPane().add(pnl, BorderLayout.CENTER);
   
   // Extract and update buttons
   pnl = new JPanel(); 
   bl = new BoxLayout(pnl,BoxLayout.X_AXIS);
   pnl.setLayout(bl);
   btnExtract = new JButton("Extract");
   btnExtract.setEnabled(false);
   btnExtract.addActionListener(extractAction);
   btnUpdate = new JButton("Update");
   btnUpdate.setEnabled(false);
   btnUpdate.addActionListener(updateAction);
   pnl.add(chkMD5);
   pnl.add(btnExtract);
   pnl.add(btnUpdate);
   pnl.add(Box.createRigidArea(new Dimension(100, 0)));
   mainframe.getContentPane().add(pnl, BorderLayout.CENTER);

   pnl = new JPanel(); 
   logdisplay = new JTextPane();
   logdisplay.setEditable(false);
   
   // xpnl is required to prevent the JTextPane from wrapping lines instread of allowing
   // the ScrollPane to display horizontal scrollbars (dont' ask me why, I've no idea, I
   // just found it via Google and it appears to be the only way to prevent the line wrapping.
   JPanel xpnl = new JPanel(new BorderLayout());
   xpnl.add(logdisplay, BorderLayout.CENTER);
   JScrollPane	scp = new JScrollPane(xpnl,  JScrollPane.VERTICAL_SCROLLBAR_AS_NEEDED, JScrollPane.HORIZONTAL_SCROLLBAR_AS_NEEDED);
   scp.setMaximumSize(new Dimension(400,300));
   scp.setPreferredSize(scp.getMaximumSize());
   pnl.setLayout(new BorderLayout());
   pnl.add(scp, BorderLayout.CENTER);
   mainframe.getContentPane().add(pnl, BorderLayout.CENTER);
   //CaptureLogTA cl = new CaptureLogTA(logdisplay);
   //Logger.getLogger(FLACtagger.class.getName()).addHandler(cl);	
  
   mainframe.pack();
   mainframe.setVisible(true);
   loadSettings();
   try
   {
	   String lcn[] = settings.getProperty(PROP_LOCATION, "100,50").split(",");
	   mainframe.setLocation(Integer.parseInt(lcn[0]),Integer.parseInt(lcn[1]));
   }
   catch(Exception ex)
   {
	   // ignore this
   }
}

private void saveSettings()
{
	File pfile = new File(System.getProperty("user.home"), PROP_FILE);
	try
	{
		settings.setProperty(PROP_ROOTDIR, getRootDir());
		settings.setProperty(PROP_TAGFILE, getFlactagFile());
		settings.setProperty(PROP_CALCMD5, isCalcMD5Enabled() ? "Y" : "N");
		settings.store(new FileOutputStream(pfile), "FLACtagger Gui settings file");
		
	}
	catch (Exception e)
	{
		e.printStackTrace();
	}
}

private void loadSettings()
{
File pfile = new File(System.getProperty("user.home"), PROP_FILE);
   try
   {
		settings.load(new FileInputStream(pfile));
      setRootDir(settings.getProperty(PROP_ROOTDIR, ""));
      setFlactagFile(settings.getProperty(PROP_TAGFILE, ""));
      setCalcMD5Enabled("Y".equals(settings.getProperty(PROP_CALCMD5, "")));
	}
	catch (Exception e)
	{

		e.printStackTrace();
	}
}


public static void main(String[] args)
{
	Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING);
	new FLACtaggerGui();
}

class TaggerTask extends SwingWorker<Integer, String> implements CaptureLogPublisher
{
	//
String metadataFile;
String rootDir;
JTextPane display;
TaggerAction taggeraction;
MutableAttributeSet defaultAttr = null;
MutableAttributeSet errorAttr = null;
MutableAttributeSet successAttr = null;
boolean bMD5;
	 public TaggerTask(TaggerAction action, JTextPane logdisplay, String rootdir, String metadatafile, boolean calcMD5) 
	 { 
		 taggeraction = action;
		 display = logdisplay;
		 metadataFile = metadatafile;
		 rootDir = rootdir;
		 bMD5 = calcMD5;
		 
		// defaultAttr doesn't need to be Mutable (at the moment) but doing it like this for consistency.
		 defaultAttr = new SimpleAttributeSet(logdisplay.getCharacterAttributes()); 
		 errorAttr = new SimpleAttributeSet(defaultAttr);
		 StyleConstants.setForeground(errorAttr, Color.RED);
		 
		 successAttr = new SimpleAttributeSet(defaultAttr);
		 StyleConstants.setForeground(successAttr, Color.GREEN);
	 }

	 @Override
	 public Integer doInBackground() 
	 {
			FLACtagger taggr = new FLACtagger(rootDir);
			CaptureLog cl = new CaptureLog(this);
			//Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING);
			//Logger log = Logger.getLogger(FLACtagger.class.getName());
			//Logger log = Logger.getGlobal(); // This stops any logging to the handler, and turns jaudiotagger logging back on!!!!
			Logger log = Logger.getLogger("");
			
			int rc = 0;
			log.addHandler(cl);
			logdisplay.setText("");
			
			try
			{
				taggr.setMd5Enabled(bMD5);
				switch(taggeraction)
				{
				case EXTRACT:
					rc = taggr.extract(metadataFile);
					break;
				case UPDATE:
					rc = taggr.update(metadataFile);
					break;
				}
				if(rc == 0)
					publish(MSG_DONE);
				else
					publish(MSG_FAILED);
				return rc;
			}
			catch (Exception e)
			{
				publish(MSG_EXCEPTION);
				return 2;
			}
			finally
			{
				log.removeHandler(cl);
			}

	 }
	 @Override
	 protected void process(List<String> chunks) 
	 {
		 for (String s: chunks) 
       {
			 //logdisplay.append(s);
		    AttributeSet attr = null;
		    Document doc = logdisplay.getDocument();
		  
		  
		    try
		    {
		   	 if(s.startsWith("INFO:") || s.startsWith("DEBUG:"))
		   	 {
		   		 attr = defaultAttr;
		   	 }
		   	 else if(s.startsWith("WARNING:") || s.startsWith("ERROR:") || s.startsWith("SEVERE:"))
		       {
		      	 // This only prints the first debug line in red. So an excpetion stacktrace
		      	 // is still printed with the default attributes. Maybe should make this a toggle
		      	 // so the attribute stays in effect until a different severity arrives??
		          attr = errorAttr;
		       }
		       else if(MSG_DONE.equals(s))
		       {
		      	 attr = successAttr;
		       }
		       else if(MSG_EXCEPTION.equals(s) || MSG_FAILED.equals(s))
		       {
		      	 attr = errorAttr;
		       }
		       doc.insertString(doc.getLength(), s, attr);
		    }
         catch (BadLocationException e)
         {
            // TODO Auto-generated catch block
            e.printStackTrace();
         }
       }
	 }

	@Override
	public void publishMessage(String logmessage)
	{
		publish(logmessage);
	}
 }




} // End of class

