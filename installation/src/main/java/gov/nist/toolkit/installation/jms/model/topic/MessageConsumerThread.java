package gov.nist.toolkit.installation.jms.model.topic;

import net.timewalker.ffmq3.FFMQConstants;

import javax.jms.Connection;
import javax.jms.ConnectionFactory;
import javax.jms.DeliveryMode;
import javax.jms.Destination;
import javax.jms.MapMessage;
import javax.jms.Message;
import javax.jms.MessageConsumer;
import javax.jms.MessageProducer;
import javax.jms.Session;
import javax.naming.Context;
import javax.naming.InitialContext;
import java.util.Hashtable;
import java.util.logging.Logger;


class MessageConsumerThread implements Runnable 
{

	private static Logger log = Logger.getLogger(MessageConsumerThread.class.getName());

	private String destinationName = "valTopic";
  
    /*
     * (non-Javadoc)
     * @see java.lang.Runnable#run()
     */
    public void run()
    {
	        MessageConsumer consumer = null;
        //javax.jms.QueueSession session = null;
        Session session = null;
        //javax.jms.QueueConnection connection = null;
		Connection connection = null;


		try {
			Hashtable<String,String> env = new Hashtable<String, String>();
            env.put(Context.INITIAL_CONTEXT_FACTORY, FFMQConstants.JNDI_CONTEXT_FACTORY);
            env.put(Context.PROVIDER_URL, "tcp://localhost:10002");
            Context context = new InitialContext(env);

            // Lookup a connection factory in the context
            //javax.jms.QueueConnectionFactory factory = (QueueConnectionFactory) context.lookup(FFMQConstants.JNDI_QUEUE_CONNECTION_FACTORY_NAME);
			ConnectionFactory factory = (ConnectionFactory) context.lookup(FFMQConstants.JNDI_CONNECTION_FACTORY_NAME);

			connection = factory.createConnection();

			session = connection.createSession(true, Session.SESSION_TRANSACTED);

            connection.start();

            Destination source = session.createTopic(destinationName);

            // Create a MessageConsumer from the Session to the Topic or Queue
            consumer = session.createConsumer(source);



            // Wait for a message
            Message message = consumer.receive(1000*60);

            if (message instanceof MapMessage) {

				String data = (String)((MapMessage)message).getObject("OMElement");

				if (data!=null) {
					log.info("**** success! ****");
					log.info(data);
				}

				Destination replyToDestination = message.getJMSReplyTo();

				log.info("sending a response to: " + replyToDestination);
	            MessageProducer producer = session.createProducer(replyToDestination);

            	MapMessage mapMsg = session.createMapMessage();
            	mapMsg.setJMSDeliveryMode(DeliveryMode.NON_PERSISTENT);
            	mapMsg.setObject("status", "success!");
				

				producer.send(mapMsg);
				session.commit();


			}
			else {
				log.warning(" receive failed - not a MapMessage, or wait timed out");
			}


        } catch (Exception ex) { 
            log.warning(ex.toString());
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




		MessageConsumerThread client = new MessageConsumerThread();
		client.run();





		} catch (Throwable t) 
		{
			t.printStackTrace();
		}

    }
}

