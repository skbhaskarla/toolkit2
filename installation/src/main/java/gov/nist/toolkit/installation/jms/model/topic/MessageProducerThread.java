package gov.nist.toolkit.installation.jms.model.topic;

import net.timewalker.ffmq3.FFMQConstants;
import org.apache.axiom.om.OMAbstractFactory;
import org.apache.axiom.om.OMElement;
import org.apache.axiom.om.OMNamespace;
import org.apache.axiom.soap.SOAPFactory;

import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.JMSException;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.Session;
import javax.jms.Topic;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;
import java.util.logging.Logger;

/**
 * Created by skb1 on 8/26/15.
 */
class MessageProducerThread implements Runnable
{

	private static Logger log = Logger.getLogger(MessageProducerThread.class.getName());

	private String destinationName = "valTopic";

    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run()
    {

        javax.jms.Connection connection = null;
        Session session = null;

        try {
	        Hashtable<String,String> env = new Hashtable<String, String>();
	        env.put(Context.INITIAL_CONTEXT_FACTORY, FFMQConstants.JNDI_CONTEXT_FACTORY);
	        env.put(Context.PROVIDER_URL, "tcp://localhost:10002"); // FFMQ server
	        Context context = new InitialContext(env);

	        // Lookup a connection factory in the context
	        ConnectionFactory factory = (ConnectionFactory) context.lookup(FFMQConstants.JNDI_CONNECTION_FACTORY_NAME);

	        connection = factory.createConnection();


			session = connection.createSession(true, Session.SESSION_TRANSACTED);

            Topic destination = session.createTopic(destinationName);

		    javax.jms.MessageProducer producer = session.createProducer(destination);

            // sender = session.createPublisher(destination);



            // Now that all setup is complete, start the Connection and send the
            // message.
            connection.start();


			SOAPFactory soapFactory = OMAbstractFactory.getSOAP11Factory();
			  // lets create the namespace object of the Article element
    	    OMNamespace ns = soapFactory.createOMNamespace("http://ihexds.nist.gov/test/1.0/testnamespace", "article");
    	    // now create the Article element with the above namespace
    	    OMElement articleElement = soapFactory.createOMElement("Article", ns);


			// javax.jms.ObjectMessage objMessage = session.createObjectMessage(articleElement);

			MapMessage mapMessage = session.createMapMessage();

			mapMessage.setObject("OMElement", articleElement.toString());


			Destination replyToDestination;
			replyToDestination =  session.createTemporaryQueue();

			mapMessage.setJMSReplyTo(replyToDestination);

            mapMessage.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);

			log.info("sending...");

            producer.send(mapMessage);
			session.commit();

			consumeResponse(connection, session, replyToDestination);




        } catch (Exception ex) {
            log.info("\n *************** Error inserting into the queue \n");
            log.warning(ex.toString());
            ex.printStackTrace();
        }

 }

private void consumeResponse(javax.jms.Connection connection, Session session, Destination replyToDestination) throws JMSException, InterruptedException {

		log.info("Entering consumeResponse" + replyToDestination.toString());

		if (replyToDestination==null) {
			log.warning("replyToDestination is null!");
			return;
		}

        MessageConsumer consumer = null;

	try {

            consumer = session.createConsumer(replyToDestination);

            // Wait for a message
            Message message = consumer.receive(1000*60);

            if (message instanceof MapMessage) {
				  String status = (String)((MapMessage)message).getObject("status");

					log.info("**** got status code <"+ status +">from the response ****");

			}
			else {
				log.info("message is null?" + ((message==null)?"true":"false"));
			}

        } catch (Exception ex) {
            log.warning("**** consumeResponse" + ex.toString());
        } finally {
            if (consumer!=null) {
                try {
                    consumer.close();
                } catch (Exception ex) {}
            }
            if (session!=null) {
                try {
                    session.close();
                } catch (Exception ex) {}
            }
            if (connection!=null) {
                try {
                    connection.close();
                } catch (Exception ex) {}
            }

        }


}

    /**
     * @param args
     */
    public static void main(String[] args)
    {

        try {




            MessageProducerThread producer = new MessageProducerThread();
            producer.run();





        } catch (Throwable t)
        {
            t.printStackTrace();
        }

    }
}
