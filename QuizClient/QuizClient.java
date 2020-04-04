
import java.awt.BorderLayout;
import java.awt.Component;
import java.awt.FlowLayout;
import java.awt.Font;
import java.awt.event.WindowAdapter;
import java.awt.event.WindowEvent;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.UnknownHostException;
import java.nio.file.Files;
import java.nio.file.Paths;

import javax.swing.DefaultListModel;
import javax.swing.JButton;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JList;
import javax.swing.JPanel;
import javax.swing.JScrollPane;
import javax.swing.JTextArea;
import javax.swing.JTextField;
import javax.swing.ListCellRenderer;
import javax.swing.event.ListSelectionEvent;
import javax.swing.event.ListSelectionListener;


public class QuizClient implements ListSelectionListener
{
	String	host;
	int		port = 0;
	InputStream istream;
	OutputStream ostream;
	
	JTextField answer, nameField;
	JLabel	groupLabel;
	JButton answerButton, joinButton, leaveButton, destroyButton, createButton, refreshButton;
	JFrame jf;
	GroupGUI ggui;
	
	//QuizData	cd;
	ClientSendHandler sh;
	JList<Group> grouplist;
	DefaultListModel<Group> groups;
	
	JTextArea questionArea;
	
	public static void main(String[] args) 
	{
		new QuizClient( args );
	}
	
	public QuizClient( String[] args )
	{
		 InetAddress address;
		 Socket sock;
		 
		// process the command-line args
        switch( args.length )
        {
             case 0:
                 System.out.println( "Usage: client host port" );
                 System.exit(-1);
                 break;    
             case 2:
            	 host = args[0];
                 port = (new Integer(args[1])).intValue();
                 break;              
             default:
                 System.out.println( "Usage: server port" );
                 System.exit(-1);
                 break;
        }
        
		try {
			address = InetAddress.getByName( host );
			sock = new Socket( address, port );
			ostream = sock.getOutputStream();
			istream = sock.getInputStream();
		} 
		catch (UnknownHostException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
		}
		
		sh = new ClientSendHandler( this, istream, ostream );
		//cd = QuizData.getQuizData();
		
		javax.swing.SwingUtilities.invokeLater(new Runnable() {
			public void run() {
				startGUI();
			}
		});
		
	}
	
	public void startGUI()
	{
		// Make the GUI, open it, and start the networking
		jf = new JFrame();
		jf.addWindowListener( new QuizWindowAdapter() );
		
		// Group area
		groupLabel = new JLabel( "GROUPS" );
		groups = new DefaultListModel<Group>();
		grouplist = new JList<Group>(groups);
		grouplist.setSize( 100, 100 );
		grouplist.addListSelectionListener(this);
		
		questionArea = new JTextArea();
		questionArea.setFont( new Font( "SansSerif", Font.BOLD, 18 ));
		questionArea.setLineWrap(true);
		questionArea.setWrapStyleWord(true);
		questionArea.setEditable(false);
		JScrollPane jsp = new JScrollPane( questionArea );
		
		jf.setSize( 800, 500 );
		
		// Group Controls
		// Name
		JLabel cname = new JLabel( "Name: " );
		nameField = new JTextField( 20 );
		nameField.setText( "Player" );
		JPanel jpname = new JPanel( new FlowLayout() );
		jpname.add(cname);
		jpname.add(nameField);
		// Join-leave
		joinButton = new JButton( QuizData.JOIN );
		joinButton.setActionCommand(QuizData.JOIN);
		joinButton.addActionListener(sh);
		leaveButton = new JButton( QuizData.LEAVE );
		leaveButton.setActionCommand(QuizData.LEAVE);
		leaveButton.addActionListener(sh);
		JPanel groupButtons = new JPanel( new FlowLayout() );
		groupButtons.add( joinButton );
		groupButtons.add( leaveButton );
		// Create- destroy
		createButton = new JButton( QuizData.CREATE );
		createButton.setActionCommand(QuizData.CREATE);
		createButton.addActionListener(sh);
		destroyButton = new JButton( QuizData.CANCEL );
		destroyButton.setActionCommand(QuizData.CANCEL);
		destroyButton.addActionListener(sh);
		JPanel addGroup = new JPanel( new FlowLayout() );
		addGroup.add( createButton );
		addGroup.add( destroyButton );
		
		JPanel groupPanel = new JPanel( new BorderLayout() );
		groupPanel.add( jpname, BorderLayout.NORTH );
		groupPanel.add( groupButtons, BorderLayout.CENTER );
		groupPanel.add( addGroup, BorderLayout.SOUTH );
		
		JPanel ur = new JPanel( new BorderLayout() );
		ur.add( groupLabel, BorderLayout.NORTH );
		ur.add( grouplist, BorderLayout.CENTER );
		ur.add( groupPanel, BorderLayout.SOUTH );
		
		JPanel upper = new JPanel( new BorderLayout() );
		upper.add( jsp, BorderLayout.CENTER );
		upper.add( ur, BorderLayout.EAST );
		
		JPanel lower = new JPanel( new FlowLayout() );
		answer = new JTextField( 40 );
		answer.addActionListener(sh);
		answer.setActionCommand( QuizData.SEND );
		answerButton = new JButton( QuizData.SEND );
		answerButton.setActionCommand(QuizData.SEND);
		answerButton.addActionListener(sh);
		
		JButton exitButton = new JButton( QuizData.EXIT );
		exitButton.setActionCommand(QuizData.EXIT);
		exitButton.addActionListener(sh);
		refreshButton = new JButton( "Refresh Groups" );
		refreshButton.setActionCommand(QuizData.OG);
		refreshButton.addActionListener(sh);
		lower.add( answer );
		lower.add( answerButton );
		lower.add( refreshButton );
		lower.add( exitButton );
		
		jf.getContentPane().add( upper, BorderLayout.CENTER );
		jf.getContentPane().add( lower, BorderLayout.SOUTH );
		
		ggui = new GroupGUI( jf, sh );
		// initial button settings
		leaveButton.setEnabled(false);
		answerButton.setEnabled(false);
		destroyButton.setEnabled(false);
		joinButton.setEnabled(false);
		
		jf.setVisible( true );
		
		// Begin the initial networking - in a different thread
		sh.start();
		
	}

	public GroupGUI getGroupGUI()
	{
		return ggui;
	}
	
	public void showQuestion( String s )
	{
		System.out.println( "calling to write the message" + s );
		questionArea.append( "\n" );
		questionArea.append( s );
	}
	
	public String getName()
	{
		return nameField.getText();
	}
	
	public String getAnswer()
	{
		return answer.getText();
	}
	
	public void makeAnswerable( boolean b )
	{
		answerButton.setEnabled(b);
		if (b)
			answer.setText( "" );
			//clearAnswer();
	}

	
	public DefaultListModel<Group> getGroups()
	{
		return groups;
		
	}
	
	public void forceUpdate()
	{
		grouplist.ensureIndexIsVisible(groups.size());
	}
	
	public Group getGroup()
	{
		return grouplist.getSelectedValue();
	}
	
	
	public void setVisualState( QState qs )
	{
		refreshButton.setEnabled(true);
		destroyButton.setEnabled(false);
		leaveButton.setEnabled(false);
		joinButton.setEnabled(false);
		createButton.setEnabled(false);
		switch( qs )
		{
			case INIT:
				createButton.setEnabled(true);
				joinButton.setEnabled(true);
				answerButton.setEnabled(false);
				break;
			case CHECKING:
			case CHECKED:
			case SENDING:
				createButton.setEnabled(false);
				break;
			case ADMIN:
				System.out.println( "Entered the ADMIN state." );
				destroyButton.setEnabled(true);
				break;
			case JOINED:
				leaveButton.setEnabled(true);
				break;
			case QUIZ:
				refreshButton.setEnabled(false);
				leaveButton.setEnabled(true);
			default:
				break;
		}
	}
	
	private class QuizWindowAdapter extends WindowAdapter
	{
		// When the X button on the window is clicked 
		public void windowClosing(WindowEvent e) 
		{
			QuizData cd = QuizData.getQuizData();
			cd.setAction( QuizData.EXIT );
			System.out.println( "closing the window" );
		}
	}


	@Override
	public void valueChanged(ListSelectionEvent event) 
	{
		QuizData qd = QuizData.getQuizData();
		if ( grouplist.getSelectedValue() != null && qd.getState() == QState.INIT )
			joinButton.setEnabled(true);
		else
			joinButton.setEnabled(false);
	}
	
}
