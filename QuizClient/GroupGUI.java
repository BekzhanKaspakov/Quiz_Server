import java.awt.BorderLayout;
import java.awt.Dialog;
import java.awt.FlowLayout;
import java.awt.GridLayout;
import java.io.File;

import javax.swing.JButton;
import javax.swing.JDialog;
import javax.swing.JFileChooser;
import javax.swing.JFrame;
import javax.swing.JLabel;
import javax.swing.JPanel;
import javax.swing.JTextField;


public class GroupGUI 
{	
	JButton okButton, cancelButton, quizFileButton, checkButton;
	JTextField topic, groupSize, groupName;
	JDialog newGroupDialog;
	JFileChooser jfc;
	ClientSendHandler sh;
	JFrame jf;
	
	public GroupGUI( JFrame jf, ClientSendHandler sh )
	{
		this.jf = jf;
		this.sh = sh;
		createNewGroupDialog();
	}	

	public void createNewGroupDialog()
	{
		newGroupDialog = new JDialog( jf, "Create a Group" );
		JPanel buttonPanel = new JPanel( new FlowLayout() );
		okButton = new JButton(QuizData.OK);
		okButton.setActionCommand(QuizData.OK);
		okButton.addActionListener(sh);
		cancelButton = new JButton(QuizData.CANCEL);
		cancelButton.setActionCommand(QuizData.CANCEL);
		cancelButton.addActionListener(sh);
		buttonPanel.add( okButton );
		buttonPanel.add( cancelButton );
		JPanel groupInfo = new JPanel( new GridLayout(4,2));
		JLabel lt = new JLabel( "Quiz Topic" );
		groupInfo.add(lt);
		topic = new JTextField(20);
		groupInfo.add(topic);
		JLabel ln = new JLabel( "Group Name:" );
		groupInfo.add(ln);
		groupName = new JTextField(20);
		groupInfo.add(groupName);
		JLabel ls = new JLabel( "Group Size" );
		groupInfo.add(ls);
		groupSize = new JTextField();
		groupInfo.add(groupSize);
		checkButton = new JButton("Check Group Name");
		checkButton.setActionCommand(QuizData.CHECK);
		checkButton.addActionListener(sh);
		groupInfo.add(checkButton);
		quizFileButton = new JButton("Upload Quiz File");
		quizFileButton.setActionCommand(QuizData.QUIZ);
		quizFileButton.addActionListener(sh);
		groupInfo.add(quizFileButton);
		newGroupDialog.getContentPane().add(groupInfo, BorderLayout.CENTER);
		newGroupDialog.getContentPane().add(buttonPanel, BorderLayout.SOUTH);
		newGroupDialog.setSize( 350, 180 );
		
		jfc = new JFileChooser();

	}
	
	public void openNewGroupDialog()
	{
		checkButton.setEnabled(true);
		cancelButton.setEnabled(true);
		quizFileButton.setEnabled(false);
		okButton.setEnabled(false);
		newGroupDialog.setVisible(true);
	}
	
	public void closeNewGroupDialog()
	{
		newGroupDialog.setVisible(false);
	}
	
	public String getGroupSpecs()
	{
		// Can't check again until you get a message back
		checkButton.setEnabled(false);
		QuizData.getQuizData().setState( QState.CHECKING );
		// No error checking ... sorry!
		return topic.getText() + "|" + groupName.getText() + "|" + groupSize.getText();
	
	}
	
	public void readyForQuizFile()
	{
			System.out.println( "Enabling the file button" );
			quizFileButton.setEnabled(true);
	}
	
	public void confirmGroup()
	{
			System.out.println( "Enabling the file button" );
			quizFileButton.setEnabled(false);
			okButton.setEnabled(true);
	}
	
	public File openFileChooser()
	{
		// Adapted from the Java docs sample
		File file = null;

		int returnVal = jfc.showOpenDialog( newGroupDialog );
 
		if (returnVal == JFileChooser.APPROVE_OPTION) 
		{
			file = jfc.getSelectedFile();
			System.out.println( "File selected: " + file );
			if ( !(file.isFile() && file.canRead()) )
					file = null;
		} 
		else 
		{
			System.out.println("Open command cancelled by user.\n" );
		}
		return file;
	}
}
