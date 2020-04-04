
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.util.Enumeration;

/*
 * This class runs all network interaction in a separate thread from the main GUI thread.
 * It writes on the GUI only when needed
 * The GUI and this networking thread synchronize on ChatData (action)
 */
public class ClientSendHandler extends Thread implements ActionListener
{
	InputStream is;
	OutputStream os;
	QuizData quizData;
	QuizClient gui;
	ServerReadThread lt;
	
	public ClientSendHandler( QuizClient cc, InputStream is, OutputStream os )
	{
		this.gui = cc;
		this.is = is;
		this.os = os;
		this.quizData = QuizData.getQuizData();
		lt = new ServerReadThread( gui, is, os );
	}
	
	

	@Override
	public void run()
	{
		String act, text;
		byte[] buf;
		String msg;

		lt.start();
		// Process button actions on this thread
		for (;;)
		{
			// getAction waits for an action
			if ( (act = quizData.getAction()) == null )
			{
				System.out.println( "Action is NULL?" );
				System.exit(0);
			}
			
			System.out.println( act );
			Group group;
			switch ( act )
			{
				case QuizData.JOIN:
					// User selected a group and clicked the join button
					group = gui.getGroup();
					msg = new String( "JOIN|" + group.name + "|" + gui.getName() + QuizData.CRLF );
					try {
						os.write( msg.getBytes() );
						quizData.setState( QState.JOINING );
						gui.setVisualState( QState.JOINING );
					} 
					catch (IOException e) {
						System.out.println( "msg send failed." );
						e.printStackTrace();
					}
					System.out.println( "Sending string to server: " + msg );
					break;
				case QuizData.LEAVE:
					// User clicked the leave button (must be in a group and not taking the quiz - control the button)
					if ( quizData.getState() == QState.QUIZ || quizData.getState()== QState.JOINED )
					{
						msg = new String( "LEAVE" + QuizData.CRLF );
						try {
							os.write( msg.getBytes() );
						} catch (IOException e) {
							System.out.println( "msg send failed." );
							e.printStackTrace();
						}
					
						quizData.setState( QState.LEAVING );
						gui.setVisualState( QState.LEAVING );
					}
					break;
				case QuizData.DESTROY:
					// User clicked the button to cancel the group - not already taking the quiz
					if ( quizData.getState() != QState.QUIZ && quizData.getState() == QState.ADMIN )
					{
						msg = new String( "CANCEL" + QuizData.CRLF );
						try {
							os.write( msg.getBytes() );
						} catch (IOException e) {
							System.out.println( "msg send failed." );
							e.printStackTrace();
						}
						quizData.setState( QState.DESTROYING );
						gui.setVisualState( QState.DESTROYING );
					}
					break;
				case QuizData.SEND:
					// User clicked the button to send the answer to a quiz question
					if ( (text = gui.getAnswer()).length() > 0 )
						msg = new String( "ANS|" + gui.getAnswer() + QuizData.CRLF );
					else
						msg = new String( "ANS|NOANS" + QuizData.CRLF );
					try {
							os.write( msg.getBytes() );
					} 
					catch (IOException e) {
						System.out.println( "msg send failed." );
						e.printStackTrace();
					}
					System.out.println( "Sending string to server: " + msg );
					gui.makeAnswerable(false);
					break;
				case QuizData.QUIZ:
					// User clicked the button to send the quiz question file
					FileInputStream		fis;
					int					n;
					String 				header;
					
					File file = gui.getGroupGUI().openFileChooser();
					if ( file == null )
						break;
					try {
						fis = new FileInputStream( file );
					} catch (FileNotFoundException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
						break;
					}
					long len = file.length();
					header = new String( "QUIZ|" + len + "|" );
					System.out.println( "Msg header: " + header );
					quizData.setState( QState.SENDING );
					gui.setVisualState( QState.SENDING );
					buf = new byte[QuizData.BUFSZ];
					try {
						os.write( header.getBytes() );
						while ( len > 0 )
						{
							n = fis.read( buf, 0, QuizData.BUFSZ );
							if ( n > 0 )
							{
								System.out.println( "write " + n );
								os.write( buf, 0, n );
								os.flush();
							}
							len -= n;
							System.out.println( "len " + len );
						}
					} catch (IOException e) {
						// TODO Auto-generated catch block
						e.printStackTrace();
					}
					//gui.getGroupGUI().readyForQuizFile();
					gui.getGroupGUI().confirmGroup();
					//System.out.println( "Back to the GUI" );
					//gui.clearAnswer();
					break;
				case QuizData.CREATE:
					// Opens the group create dialog
					gui.getGroupGUI().openNewGroupDialog();
					break;
				case QuizData.CHECK:
					// Send the new GROUP message to check if it's ok and we can send the quiz
					msg = new String( "GROUP|" + gui.getGroupGUI().getGroupSpecs() + QuizData.CRLF );	
					try {
						os.write( msg.getBytes() );
						quizData.setState( QState.CHECKING );
						gui.setVisualState( QState.CHECKING );
					} 
					catch (IOException e) {
						System.out.println( "group create msg send failed." );
						e.printStackTrace();
					}
					System.out.println( "Sending string to server: " + msg );
					break;
				case QuizData.CANCEL:
					//  HEY reset the state!!
					if ( quizData.getState() != QState.QUIZ && quizData.getState()== QState.ADMIN )
					{
						System.out.println( "Clicked the CANCEL button 222" );
						msg = new String( "CANCEL" + QuizData.CRLF );
						try {
							os.write( msg.getBytes() );
						} catch (IOException e) {
							System.out.println( "msg send failed." );
							e.printStackTrace();
						}
					
						quizData.setState( QState.LEAVING );
						gui.setVisualState( QState.LEAVING );
					} else{
						gui.getGroupGUI().closeNewGroupDialog();
					}
					System.out.println( "Clicked the CANCEL button" );
					break;
					// fall-through??
					
				case QuizData.OK:
					gui.getGroupGUI().closeNewGroupDialog();
					break;
				case QuizData.OG:
					// Click the button to request the open group list - when is it allowed?
					String req = new String( QuizData.OG + QuizData.CRLF );	
					try {
						os.write( req.getBytes() );
					} 
					catch (IOException e) {
						System.out.println( "open groups request msg send failed." );
						e.printStackTrace();
					}
					System.out.println( "Sending string to server: " + req );
					break;
				case QuizData.EXIT:
				default:
					System.exit(0);
					
			}
			// Now clear the action for the next one
			quizData.setAction(null);
		}

	}
	


	@Override
	public void actionPerformed(ActionEvent ae) {
		
		quizData.setAction( ae.getActionCommand() );
		
	}
		
}
