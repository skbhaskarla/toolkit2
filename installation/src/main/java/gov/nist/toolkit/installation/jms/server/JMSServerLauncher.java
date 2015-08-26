package gov.nist.toolkit.installation.jms.server;

import net.timewalker.ffmq3.FFMQServerLauncher;

import java.io.File;

/**
 * This class is only used for demonstration purposes.
 * Created by skb1 on 8/26/15.
 */
public class JMSServerLauncher {

    public static void main(String[] args)
    {

        System.setProperty("FFMQ_HOME", new File(JMSServerLauncher.class.getResource("/ffmq/conf/ffmq-server.properties").getFile()).getParentFile().getParent());


        FFMQServerLauncher.main(args);

    }
}
