package com.smallcatutilities.flactagger.gui;

import java.awt.BorderLayout;
import java.awt.Dimension;
import java.awt.Point;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.geom.Point2D;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Properties;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.swing.Box;
import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.UIManager;
import javax.swing.filechooser.FileNameExtensionFilter;

import com.smallcatutilities.flactagger.FLACtagger;


public class FLACtaggerGui
{
private static final String PROP_FILE = "flactagger.properties";
private static final String PROP_ROOTDIR = "flactagger.rootdir";
private static final String PROP_TAGFILE = "flactagger.tagfile";
private static final String PROP_LOCATION = "flactagger.location";
private JFrame mainframe;
private JTextField txtRootDir;
private JTextField txtFlacTagsFile;
private JButton btnExtract;
private JButton btnUpdate;

private Properties settings = new Properties();

private ActionListener updateAction = new ActionListener(){
	@Override
	public void actionPerformed(ActionEvent e)
	{
		doUpdate();
	}
};

private ActionListener extractAction = new ActionListener(){
	@Override
	public void actionPerformed(ActionEvent e)
	{
		doExtract();
	}
};


WindowAdapter exitEvent = new WindowAdapter() {
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

	protected void popupBox(String title, String msg, int status)
	{
	JTextArea tb = new JTextArea();
	JPanel pnl = new JPanel();
	JLabel lbl = new JLabel();
		
		lbl.setText(title);
		tb.setText(msg);
		pnl.setLayout(new BorderLayout());
		pnl.add(lbl, BorderLayout.NORTH);
		pnl.add(tb, BorderLayout.CENTER);
		
		JOptionPane.showMessageDialog(mainframe, pnl, "FLACtagger", status);
		
	}
	
	protected void doExtract()
	{
	String rd = this.getRootDir();
	FLACtagger taggr = new FLACtagger(rd);
	CaptureLog cl = new CaptureLog();
		Logger.getLogger(FLACtagger.class.getName()).addHandler(cl);
		try
		{
			if(taggr.extract(this.getFlactagFile()) != 0)
			{
				popupBox("Extraction failed!", cl.getLog(), JOptionPane.ERROR_MESSAGE);
			}
			else
			{
				popupBox("Extraction done!", cl.getLog(), JOptionPane.INFORMATION_MESSAGE);
			}
		}
		catch (Exception e)
		{
			popupBox("Extraction failed!", cl.getLog(), JOptionPane.ERROR_MESSAGE);
		}
		finally
		{
			Logger.getGlobal().removeHandler(cl);
		}
	}

	protected void doUpdate()
	{
	String rd = this.getRootDir();
	FLACtagger taggr = new FLACtagger(rd);

	CaptureLog cl = new CaptureLog();
	Logger.getLogger(FLACtagger.class.getName()).addHandler(cl);

		try
		{
			if( taggr.update(this.getFlactagFile()) != 0)
			{
				popupBox("Update failed!", cl.getLog(), JOptionPane.ERROR_MESSAGE);
			}
			else
			{
				popupBox("Update done!", cl.getLog(), JOptionPane.INFORMATION_MESSAGE);
			}
		}
		catch (Exception e)
		{
			e.printStackTrace();
			popupBox("Update failed!", cl.getLog(), JOptionPane.ERROR_MESSAGE);
		}
		finally
		{
			Logger.getGlobal().removeHandler(cl);
		}
	
	}

	// TODO: implement as Listener on the textboxes?
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
		return txtRootDir.getText();
	}
	
	protected void setFlactagFile(String tagfile)
	{
		txtFlacTagsFile.setText(tagfile);
		setExtUpd();
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
      mainframe.getContentPane().setPreferredSize(new Dimension(400, 100));
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
						//
						// disable the "All files" option.
						//
						chooser.setAcceptAllFileFilterUsed(false);
						//    
						if (chooser.showOpenDialog(mainframe) == JFileChooser.APPROVE_OPTION) 
						{ 
							File sel = chooser.getSelectedFile();
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
      		}
      );
      
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

      // TODO Figure out how to align the left edges of the textfields....
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
      pnl.add(btnExtract);
      pnl.add(btnUpdate);
      
      mainframe.getContentPane().add(pnl, BorderLayout.CENTER);

      
      
      //GroupLayout layout = new GroupLayout(mainframe.getContentPane()); 
      //layout.setHorizontalGroup(
      //		layout.createSequentialGroup()
      //		  .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      //	         .addComponent(lbl1)
      //	         .addComponent(lbl2))
      //	     .addGroup(layout.createParallelGroup(GroupLayout.Alignment.LEADING)
      //	         .addComponent(txtRootDir)
      //	         .addComponent(txtFlacTagsFile)
      //	     		.addComponent(pnl))
      //	);      
      //mainframe.getContentPane().setLayout(layout);

      // Display the directory chooser
      
      
      
      
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
		File pfile = new File(System.getProperty("user.home"), this.PROP_FILE);
		try
		{
			settings.setProperty(PROP_ROOTDIR, getRootDir());
			settings.setProperty(PROP_TAGFILE, getFlactagFile());
			settings.store(new FileOutputStream(pfile), "FLACtagger Gui settings file");
		}
		catch (Exception e)
		{
			e.printStackTrace();
		}
	}
	
	private void loadSettings()
	{
	File pfile = new File(System.getProperty("user.home"), this.PROP_FILE);
		try
		{
			settings.load(new FileInputStream(pfile));
	      setRootDir(settings.getProperty(PROP_ROOTDIR, ""));
	      setFlactagFile(settings.getProperty(PROP_TAGFILE, ""));
		}
		catch (Exception e)
		{

			e.printStackTrace();
		}
	}

	public static void main(String[] args)
	{
		Logger.getLogger("org.jaudiotagger").setLevel(Level.WARNING);
		FLACtaggerGui gui = new FLACtaggerGui();
	}

	
}
