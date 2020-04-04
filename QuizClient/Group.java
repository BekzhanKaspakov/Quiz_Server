
public class Group {
	public String name;
	public String topic;
	public int max;
	public int current;
	
	public Group( String topic, String name, int max, int current )
	{
		this.name = name;
		this.topic = topic;
		this.max = max;
		this.current = current;
	}

	@Override
	public String toString() {
		String s = name + " - " + topic + "  " + current + "  " + max;
		return s;
	}
	
	

}
