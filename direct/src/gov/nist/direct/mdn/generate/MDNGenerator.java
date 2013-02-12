package gov.nist.direct.mdn.generate;

import javax.mail.MessagingException;
import javax.mail.internet.MimeBodyPart;

import com.google.gwt.dev.jjs.ast.JField.Disposition;
import org.apache.mailet.base.mail.MimeMultipartReport;

/**
 * Class <code>MDNFactory</code> creates MimeMultipartReports containing
 * Message Delivery Notifications as specified by RFC 3798.
 * This class is based on code from the open source Apache James server.
 * https://www.java2s.com/Open-Source/Java/Web-Mail/james-2.3.1/org/apache/james/util/mail/mdn
 *
 *
 * @author dazais
 *
 */
public class MDNGenerator {
	
	  
    /**
     * Answers with a MimeMultipartReport containing a
     * Message Delivery Notification as specified by RFC 2298.
     * 
     * @param humanText
     * @param reporting_UA_name
     * @param reporting_UA_product
     * @param original_recipient (optional)
     * @param final_recipient
     * @param original_message_id
     * @param disposition
     * @return MimeMultipartReport
     * @throws MessagingException
     */
    static public MimeMultipartReport create(String humanText,
            String reporting_UA_name,
            String reporting_UA_product,
            String original_recipient,
            String final_recipient,
            String original_message_id,
            Disposition disposition) throws MessagingException
    {
        // Create the message parts. According to RFC 2298, there are two
        // compulsory parts and one optional part...
        MimeMultipartReport multiPart = new MimeMultipartReport();
        multiPart.setReportType("disposition-notification");
        
        // Part 1: The 'human-readable' part
        MimeBodyPart humanPart = new MimeBodyPart();
        humanPart.setText(humanText);
        multiPart.addBodyPart(humanPart);

        // Part 2: MDN Report Part
        // 1) reporting-ua-field
        StringBuffer mdnReport = new StringBuffer(128);
        mdnReport.append("Reporting-UA: ");
        mdnReport.append((reporting_UA_name == null ? "" : reporting_UA_name));
        mdnReport.append("; ");
        mdnReport.append((reporting_UA_product == null ? "" : reporting_UA_product));
        mdnReport.append("\r\n");
        // 2) original-recipient-field - optional
        if (null != original_recipient)
        {
            mdnReport.append("Original-Recipient: ");
            mdnReport.append("rfc822; ");
            mdnReport.append(original_recipient);
            mdnReport.append("\r\n");
        }
        // 3) final-recipient-field
        mdnReport.append("Final-Recepient: ");
        mdnReport.append("rfc822; ");
        mdnReport.append((final_recipient == null ? "" : final_recipient));
        mdnReport.append("\r\n");
        // 4) original-message-id-field
        mdnReport.append("Original-Message-ID: ");
        mdnReport.append((original_message_id == null ? "" : original_message_id));
        mdnReport.append("\r\n");
        // 5) disposition-field
        mdnReport.append(disposition.toString());
        mdnReport.append("\r\n");
        MimeBodyPart mdnPart = new MimeBodyPart();
        mdnPart.setContent(mdnReport.toString(), "message/disposition-notification");
        multiPart.addBodyPart(mdnPart);

        // Part 3: The optional third part, the original message is omitted.
        // We don't want to propogate over-sized, virus infected or
        // other undesirable mail!
        // There is the option of adding a Text/RFC822-Headers part, which
        // includes only the RFC 822 headers of the failed message. This is
        // described in RFC 1892. It would be a useful addition!        
        return multiPart;
    }
	

}