
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

// This is the thread for reading messages from the server
public class ServerReadThread extends Thread 
{
	InputStream is;
	OutputStream os;
	QuizClient qc;
	
	public ServerReadThread( QuizClient cc, InputStream is, OutputStream os )
	{
		this.is = is;
		this.os = os;
		this.qc = cc;
	}
	
	private String[] serverRead()
	{
		String request[] = null;
		int n;
		String s;
		
		byte buf[] = new byte[QuizData.BUFSZ];
		
		try
		{
			n = is.read( buf );
			s = new String( buf, 0, n );
			System.out.println( "The server sent: " + s );
			// Split the command from the args
			request = s.split( "|" );
			if (request[0].equals("QUES"))
			{
				int len, got;
				if ((len = Integer.parseInt(request[1])) != (got = request[2].length()) )
				{
					// Not checking if we read too much, only too little
					n = is.read( buf, 0, len-got );
					s = new String( buf, 0, n );
					String ques = request[2] + s;
					System.out.println( "The question is: " + ques );
					// put it on the screen
				}
			}
		}
		catch (IOException e) {
			System.out.println( "Error in ServerReadThread.serverRead()" );
			e.printStackTrace();
		}
		return request;
		
	}
	
	 // Remove the CRLF
		//	 s = s.substring(0, s.length()-QuizData.CRLF.length());
	//request = s.split( "\\|" );
	//for ( int j=0; j < request.length; j++) {
		//System.out.println( "HI " + request[j] );
	
	// These variables and tokenizer is for special purpose
	byte[] cbuf = new byte[QuizData.BUFSZ];
	byte[] buf = new byte[QuizData.BUFSZ];
	int pos;
	int total;
	boolean done;
	
	void setDone( boolean b )
	{
		done = b;
	}
	
	boolean isDone()
	{
		return done;
	}
	
	String readATokenDelim()
	{
		int i, j;
		
		System.out.println( "pos is " + pos );
		for ( i = pos, j = 0; i < total && buf[i] != '|' && buf[i] != '\r' && buf[i] != '\n'; i++, j++ )
		{
			cbuf[j] = buf[i];
		}
		String s = new String( cbuf, 0, j );
		if ( buf[i] == '\r' || buf[i] == '\n' )
		{
			pos = i+2;	// skip CRLF
			setDone( true );
		}
		else
			pos = i+1;  // skip the | delimiter
		System.out.println( "pos is " + pos );
		return s;
	}
	String readATokenDelimResult()
	{
		int i, j;
		
		System.out.println( "pos is " + pos );
		for ( i = pos, j = 0; i < total && buf[i] != '\r' && buf[i] != '\n'; i++, j++ )
		{
			if (buf[i] != '|')
				cbuf[j] = buf[i];
			else 
				cbuf[j] = ' ';
		}
		String s = new String( cbuf, 0, j );
		if ( buf[i] == '\r' || buf[i] == '\n' )
		{
			pos = i+2;	// skip CRLF
			setDone( true );
		}
		else
			pos = i+1;  // skip the | delimiter
		System.out.println( "pos is " + pos );
		return s;
	}
	String readATokenSize( int x )
	{
		int i, j;
		
		System.out.println( "pos is " + pos );
		for ( i = pos, j = 0; j < x; i++, j++ )
		{
			cbuf[j] = buf[i];
		}
		pos = i;
		setDone(true);
		String s = new String( cbuf, 0, j );
		System.out.println( "pos is " + pos );
		System.out.println( s );
		return s;
	}
	
	@Override
	public void run() 
	{
		//  Must allow for multiple commands in the same buffer
		QuizData qd = QuizData.getQuizData();
		
		System.out.println("ServerReadThread is running");
		
		for (;;)
		{
			try {
				setDone(false);
				pos = 0;
				total = is.read( buf );
				System.out.println( "\nMessage from someone " + total );
				// Multiple message may be read together
				while ( total > 0 && pos < total )
				{
					String command = readATokenDelim();
					System.out.println( "The command is " + command );
				
					if ( command.equals("SENDQUIZ") )
					{
						System.out.println( "The command is SENDQUIZ" );
						qc.getGroupGUI().readyForQuizFile();
					}
					else if ( command.equals("OK" ))
					{
						switch (qd.getState())
						{
							case JOINING:
								qd.setState(QState.JOINED);
								qc.setVisualState(QState.JOINED);
								qc.showQuestion( "JOIN request was accepted." );
								break;
							case LEAVING:
								qd.setState(QState.INIT);
								qc.setVisualState(QState.INIT);
								qc.showQuestion( "LEAVE request was accepted." );
								break;
							case DESTROYING:
								qd.setState(QState.INIT);
								qc.setVisualState(QState.INIT);
								qc.showQuestion( "CANCEL group request was accepted." );
								break;
							case SENDING:
								qd.setState(QState.ADMIN);
								qc.setVisualState(QState.ADMIN);
								qc.showQuestion( "CREATE group request was accepted." );
								break;
						default:
							System.out.println( "Unexpected state: " + qd.getState() );
						}
					}
					else if ( command.equals("WAIT" ))
					{
						qc.showQuestion( "Please wait." );
					}
					else if ( command.equals("ENDGROUP"))
					{
						qc.showQuestion( "Group ended" );
						qd.setState( QState.INIT );
						qc.setVisualState(QState.INIT);
					}
					else if (command.equals("OPENGROUPS"))
					{
						// Parse the list of groups
						System.out.println( "The groups message came." );
						qc.getGroups().removeAllElements();
						while ( !isDone() )
						{
							String topic = readATokenDelim();
							System.out.println( topic );
							String name = readATokenDelim();
							System.out.println( name );
							String gsize = readATokenDelim();
							System.out.println( gsize );
							String curr = readATokenDelim();
							System.out.println( curr );
							Group g = new Group( topic, name, 
									(new Integer(gsize)).intValue(), (new Integer(curr)).intValue() );
							qc.getGroups().addElement(g);
						}
						qc.forceUpdate();  // does this work??
					}
					else if ( command.equals( "QUES" ))
					{
						// Read out the command
						String sizestr = readATokenDelim();
						
						int qlen = (new Integer(sizestr)).intValue();
						System.out.println( "the size of the question is " + qlen );
						String ques = readATokenSize( qlen );
						qc.showQuestion( ques );
						qc.makeAnswerable(true);
						qd.setState( QState.QUIZ );
						qc.setVisualState(QState.QUIZ);
					}
					else if ( command.equals("BAD") )
					{
						String error = readATokenDelim();
						qd.setState(QState.INIT);
						qc.setVisualState(QState.INIT);
						// set the buttons and show a message
						qc.showQuestion( "Request was rejected: " + error );
					}
					else if ( command.equals("WIN") )
					{
						String winner = readATokenDelim();
						qc.showQuestion( "The winner was: " + winner );
					}
					else if ( command.equals("RESULT") )
					{
						String result = readATokenDelimResult();
						qc.showQuestion( "The results were: " + result );
					}
					else
					{
						System.out.println( "The actual command is " + command + " so I will exit" );
						System.exit(-1);
					}
				}
			} 
			catch (IOException ioe) {
				// TODO Auto-generated catch block
				ioe.printStackTrace();
			}
		}
	}
	
}

