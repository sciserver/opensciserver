package sciserver.logging;

import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;



public class LoggingTime {
	String time;
	
	public LoggingTime(){
		ZonedDateTime now = ZonedDateTime.now();
		DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss.SSSZZZZZ");
		this.time = now.format(formatter);
	}
	
	public static String now(){
		LoggingTime lt = new LoggingTime();
		return lt.time;
	}

}

