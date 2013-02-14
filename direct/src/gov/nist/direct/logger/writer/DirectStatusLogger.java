package gov.nist.direct.logger.writer;

import gov.nist.direct.logger.LogPathsSingleton;
import gov.nist.direct.utils.Utils;

import java.io.File;
import java.io.IOException;

/**
 * Logs status of message validation to MDN and Direct log file structure
 * @author dazais
 *
 */
public class DirectStatusLogger {
	
	public DirectStatusLogger(){
		
	}

	// Logging a message status
	public void logMessageStatus(String s, LogPathsSingleton ls, String transactionType, String messageType, String username, String messageId) throws IOException {
		String statusLogPath = ls.getMessageStatusLogPath(transactionType, messageType, username, messageId);
		Utils.writeToFile(s, statusLogPath);
		}
	


}
