import java.util.HashMap;



public class QuizData 
{
	public static final String CRLF = "\r\n";
	public static final String JOIN = "JOIN";
	public static final String LEAVE = "LEAVE";
	public static final String CREATE = "CREATE";
	public static final String DESTROY = "DESTROY";
	public static final String EXIT = "EXIT";
	public static final String SEND = "ANSWER";
	public static final String CHECK = "CHECK";
	public static final String QUIZ = "QUIZ";
	public static final String OK = "OK";
	public static final String CANCEL = "CANCEL";
	public static final String OG = "GETOPENGROUPS";

	public static final int BUFSZ = 2048;
	
	private static QuizData	cd = null;
	
	private String action = null;
	private QState state = QState.INIT;
	
	private HashMap<String, String> groups;
	
	private QuizData()
	{
		groups = new HashMap<String, String>();
	}
	
	static QuizData getQuizData()
	{
		if ( cd == null )
			cd = new QuizData();
		return cd;
	}
	
	public synchronized void add( String name, String data )
	{
		groups.put( name, data );	
		
	}
	
	public synchronized QState getState() {
		return state;
	}

	public synchronized void setState(QState state) {
		this.state = state;
	}

	public synchronized String getAction()
	{
		try {
			if ( action == null )
				wait();
		} 
		catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
		}
		return action;
	}
	
	public synchronized void setAction( String action )
	{
		this.action = action;
		notify();
		
	}


}
