/**
 * Mars Simulation Project
 * MultiplayerServerClient.java
 * @version 3.08 2015-04-06
 * @author Manny Kung
 */

package org.mars_sim.networking;

import java.awt.BorderLayout;
import java.awt.Container;
import java.awt.FlowLayout;
import java.awt.Frame;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.PrintWriter;
import java.net.InetAddress;
import java.net.Socket;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.StringTokenizer;
import java.util.logging.Logger;

import javax.swing.BoxLayout;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;

import org.mars_sim.msp.javafx.MainMenu;

import javafx.application.Platform;
import javafx.scene.control.Alert;
import javafx.scene.control.ChoiceDialog;
import javafx.stage.Modality;
import javafx.scene.control.Alert.AlertType;

/**
 * The MultiplayerClient class allows the computer to take on the client role.
 */
public class MultiplayerClient extends JFrame implements ActionListener {

	/** default logger. */
	private static Logger logger = Logger.getLogger(MultiplayerClient.class.getName());

	private static final int RECORDS = 0;
	private static final int NEW_ID = 1;

	private static final int PORT = 9090;

	//private static final String LOCALHOST = "localhost";

	private int clientID;

	private String addressStr;

	private List<String> addresses = new ArrayList<>();

	private Socket sock;
	private BufferedReader in;     // i/o for the client
	private PrintWriter out;

	private JTextArea jtaMesgs;
	private JTextField jtfName, jtfLat, jtfLong;
	private JButton jbGetRecords;
	private JButton jbSendNew;
	Container c;

	private MainMenu mainMenu;
	private ModeTask modeTask;
	private MultiplayerTray multiplayerTray;

	private Alert alert;

	public MultiplayerClient(MainMenu mainMenu) throws IOException {
	    super( "Settlement Registry Client" );

		this.mainMenu = mainMenu;

		InetAddress ip = InetAddress.getLocalHost();
		addressStr = ip.getHostAddress();
		logger.info("Running the client at " + addressStr + ". Waiting to connect to a host...");
		modeTask = new ModeTask(addressStr);

	    createPanel();
	    addWindowListener( new WindowAdapter() {
	       public void windowClosing(WindowEvent e) {
	    	   closeLink();
	    }});

	    //setState(Frame.ICONIFIED);
	    setSize(400,400);
	    setVisible(true);
	}

	public Container getContainer() {
		return c;
	}

	public String getAddressStr() {
		return addressStr;
	}

	public MainMenu getMainMenu() {
		return mainMenu;
	}

	public ModeTask getModeTask() {
		return modeTask;
	}

	/*
	 * Creates the task that will prompt users to create a dialog box for choosing the host server's IP address
	 */
	class ModeTask implements Runnable {

		private String addressStr;

		private ModeTask(String addressStr) {
			this.addressStr = addressStr;
		}

		@Override
		public void run() {
				Platform.runLater(() -> {
					createClient(addressStr);
		        });
		}
	}

	/*
	 * Creates a dialog box for choosing the host server's IP address
	 */
	public void createClient(String addressStr) {

		addresses.add("127.0.0.1");

		ChoiceDialog<String> dialog = new ChoiceDialog<>("127.0.0.1", addresses);
		dialog.initModality(Modality.APPLICATION_MODAL);
		dialog.initOwner(mainMenu.getStage());
		dialog.setTitle("Mars Simulation Project");
		dialog.setHeaderText("Multiplayer Client");
		//dialog.setContentText("Your IP: " + addressStr + ".\nChoose host : ");
		dialog.setContentText("Choose your host : ");
		Optional<String> result = dialog.showAndWait();
		result.ifPresent(address -> {
			logger.info("Connecting to the host at " + address);

		   try {
			    makeContact(address);
			    createAlert(address);
				multiplayerTray = new MultiplayerTray(this);

			} catch (Exception e) {
				e.printStackTrace();
			}
	   });
	}

	/*
	 * Creates an alert to inform the user that the client can make a connection with the server
	 */
	public void createAlert(String address) {
	    alert = new Alert(AlertType.INFORMATION);
		alert.setTitle("Mars Simulation Project");
		alert.initOwner(mainMenu.getStage());
		alert.setHeaderText("Multiplayer Client");
		alert.setContentText("Connection verified with " + address);
		alert.show();
	}

	/*
	 * Creates a panel to download settlement registry and upload new settlement info
	 */
	private void createPanel() {
		    c = getContentPane();
		    c.setLayout( new BorderLayout() );

		    jtaMesgs = new JTextArea(7, 7);
		    jtaMesgs.setEditable(false);
		    JScrollPane jsp = new JScrollPane( jtaMesgs);
		    c.add( jsp, "Center");

		    JLabel jlName = new JLabel("Name: ");
		    jtfName = new JTextField(10);

		    JLabel jlLat = new JLabel("Lat: ");
		    jtfLat = new JTextField(5);

		    JLabel jlLong = new JLabel("Long: ");
		    jtfLong = new JTextField(5);

		    jbSendNew = new JButton("Send New");
		    jbSendNew.addActionListener(this);

		    jbGetRecords = new JButton("Get Records");
		    jbGetRecords.addActionListener(this);

		    JPanel p1 = new JPanel( new FlowLayout() );
		    p1.add(jlName); p1.add(jtfName);
		    p1.add(jlLat); p1.add(jtfLat);
		    p1.add(jlLong); p1.add(jtfLong);

		    JPanel p2 = new JPanel( new FlowLayout() );
		    p2.add(jbSendNew);
		    p2.add(jbGetRecords);


		    JPanel p = new JPanel();
		    p.setLayout( new BoxLayout(p, BoxLayout.Y_AXIS));
		    p.add(p1); p.add(p2);

		    c.add(p, "South");

		  }

	/*
	 * Closes sockets to terminate contact with the server
	 */
	  private void closeLink() {
	    try {
	      out.println("bye");    // tell server that client is disconnecting
	      //out.close();		// not working for threaded server
	      sock.close();
	    }
	    catch(Exception e)
	    { e.printStackTrace();}

	    System.exit( 0 );
	  }

	/*
	 * Opens sockets to initiate contact with the server
	 */
	private void makeContact(String address) {
	    try {
	      sock = new Socket(address, PORT);
	      in  = new BufferedReader(
			  		new InputStreamReader( sock.getInputStream() ) );
	      out = new PrintWriter( sock.getOutputStream(), true );  // autoflush
	    }
	    catch(Exception e)
	    { e.printStackTrace();}
	  }

	/*
	 * Responds to pressing either "Send New" or "Get Records" buttons
	 */
	public void actionPerformed(ActionEvent e) {
	     if (e.getSource() == jbGetRecords)
	       sendGetRecords();
	     else if (e.getSource() == jbSendNew)
	       sendNewEntry();
	   }

	   /* Sends out "get" command, read and display responses from server
	    * Note: normal response should be "RECORDS n1 & lat1 & long1 .... nN & latN & longN"
	    */
	   private void sendGetRecords()  {
	     try {
	       out.println("get");
	       String line = in.readLine();
		    logger.info("Received : " + line);
	       if ((line.length() >= 8) &&     // "RECORDS "
	           (line.substring(0, 7).equals("RECORDS")))
	         showRegistryContent(RECORDS, line.substring(7).trim() );
	       else    // should not happen but just in case
	         jtaMesgs.append( line + "\n");
	     }
	     catch(Exception ex)
	     {
	       jtaMesgs.append("Problem obtaining records\n");
		   ex.printStackTrace();
	     }
	   }

	/*
	 * 	Parses and displays the registry entries in a formatted view
	 */
	private void showRegistryContent(int type, String line) {
		if (type == 1) {
			clientID = Integer.parseInt(line);
		    try {
		    	jtaMesgs.append("Received :\n");
		    	jtaMesgs.append("Client ID Assigned: " + clientID + "\n\n");;
		    }
		    catch(Exception e) {
		      jtaMesgs.append("Problem parsing client id\n");
		      logger.info("Parsing error with client id:\n" + e);
		    }
		}
		else if (type == 0) {
		    StringTokenizer st = new StringTokenizer(line, "&");
		    String name;
		    int i;
		    double lat, lo;
		    i = 1;
		    try {
		    	jtaMesgs.append("Received :\n");
			    while (st.hasMoreTokens()) {
			        name = st.nextToken().trim();
			        lat = Double.parseDouble( st.nextToken().trim() );
			        lo = Double.parseDouble( st.nextToken().trim() );
			        jtaMesgs.append("(" + i + "). " + name + " at ( " + lat + ", " + lo + " )\n");
			        i++;
			    }
			    jtaMesgs.append("\n");
		    }
		    catch(Exception e) {
		      jtaMesgs.append("Problem parsing records\n");
		      logger.info("Parsing error with records:\n" + e);
		    }
		}
	}


	/* Checks if the user types in a correct settlement name and coordinates
	 * If true, send "new name & lat & long &" to server
	 */
	private void sendNewEntry() {
	    String name = jtfName.getText().trim();
	    String lat = jtfLat.getText().trim();
	    String lo = jtfLong.getText().trim();

	    if ((name.equals("")) && (lat.equals("")) && (lat.equals("")))
	      JOptionPane.showMessageDialog( null,
	           "No name/coordinates entered", "Send Error",
				JOptionPane.ERROR_MESSAGE);
	    else if (name.equals(""))
	      JOptionPane.showMessageDialog( null,
	              "No name/coordinates entered", "Send Error",
				JOptionPane.ERROR_MESSAGE);
	    else if (lat.equals(""))
	      JOptionPane.showMessageDialog( null,
	              "No name/coordinates entered", "Send Error",
				JOptionPane.ERROR_MESSAGE);
	    else {
	      out.println("new " + name + " & " + lat + " & " + lo + " &");
	      jtaMesgs.append("Sent :\n" + name + " & " + lat + " & " + lo + " &" + "\n\n");
	    }
	  }

	/*
	 * Sends register command to host server to request a new client id
	 */
	public void sendGetNewID()  {
	     try {
	       out.println("register");
	       String line = in.readLine();
	       logger.info("Received : " + line);
	       if ((line.length() >= 7) &&     // "CLIENT ID "
	           (line.substring(0, 6).equals("NEW_ID")))
	         showRegistryContent(NEW_ID, line.substring(6).trim() );
	       else    // should not happen but just in case
	         jtaMesgs.append( line + "\n");
	     }
	     catch(Exception ex)
	     {
	       jtaMesgs.append("Problem obtaining a new client id\n");
		   ex.printStackTrace();
	     }
	}

	public int getClientID() {
		return clientID;
	}

	public void destroy() {
		sock= null;
		in= null;
		out= null;
		jtaMesgs= null;
		jtfName= null;
		jtfLat= null;
		jtfLong= null;
		jbGetRecords= null;
		jbSendNew= null;
		mainMenu= null;
		modeTask= null;
		multiplayerTray= null;
		alert= null;
	}
}