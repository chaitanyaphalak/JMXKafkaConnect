import org.json.JSONObject;
import sun.tools.jstat.Jstat;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.Properties;
import java.util.Set;

import javax.management.MBeanAttributeInfo;
import javax.management.MBeanInfo;
import javax.management.MBeanServerConnection;
import javax.management.ObjectName;
import javax.management.remote.JMXConnector;
import javax.management.remote.JMXConnectorFactory;
import javax.management.remote.JMXServiceURL;

import java.net.InetAddress;

class KafkaConnectNode {
  // sleep, localhost, port, filename
  private JMXServiceURL url;
  private JMXConnector jmxc;
  private MBeanServerConnection conn;

  private FileWriter fw;
  private BufferedWriter bw;
  private PrintWriter printWriterOut;

  private String iPAddr;
  private String hostName;

  private String iPAddrRemote;

  private HashMap<Integer, HashMap<String, String>> clientConsumerMetrics = new HashMap<Integer,HashMap<String, String>>();
  private HashMap<Integer, HashMap<String, String>> clientConsumerFetchMetrics = new HashMap<Integer,HashMap<String, String>>();
  private HashMap<Integer, HashMap<String, String>> taskConnectMetrics = new HashMap<Integer,HashMap<String, String>>();
  private HashMap<Integer, HashMap<String, String>> taskConnectErrorMetrics = new HashMap<Integer,HashMap<String, String>>();

  KafkaConnectNode(String host, Integer port, String filename){

    try{
      url = new JMXServiceURL("service:jmx:rmi:///jndi/rmi://" + host + ":" + port  + "/jmxrmi");
      jmxc = JMXConnectorFactory.connect(url, null);
      conn = jmxc.getMBeanServerConnection();
      InetAddress inetAddress = InetAddress.getLocalHost();
      iPAddrRemote = host;
    } catch (Exception ex){
      System.out.println(ex.toString());
    }

    try {
      fw = new FileWriter(filename + "-" + iPAddrRemote + ".txt", true);
      bw = new BufferedWriter(fw);
      printWriterOut = new PrintWriter(bw);
    } catch (IOException e) {
      System.out.println(e.toString());
    }
  }

  public String getCurrentTimeStamp() {
    return new SimpleDateFormat("yyyy-MM-dd-HH:mm:ss.SSS").format(new Date());
  }

  public void update(String query, HashMap<Integer, HashMap<String, String>> hmToUpdate) throws Exception
  {
    System.out.println("update called with query " + query);
    ObjectName name = new ObjectName(query);
    Set<ObjectName> queryName = conn.queryNames(name, null);
    Integer i = 0;
    for (ObjectName objectName : queryName) {
      MBeanInfo beanInfo = conn.getMBeanInfo(objectName);
      MBeanAttributeInfo[] attrInfo = beanInfo.getAttributes();
      HashMap<String, String> metrics = null;
      if(hmToUpdate.containsKey(i)) {
        metrics = hmToUpdate.get(i);
      } else {
        metrics = new HashMap<String, String>();
      }
      for (MBeanAttributeInfo attr : attrInfo) {
        metrics.put(attr.getName(), conn.getAttribute(objectName, attr.getName()).toString());
      }
      hmToUpdate.put(i, metrics);
      // Get all attributes and its values
      i++;
    }

    for (HashMap.Entry<Integer, HashMap<String, String>> entry : hmToUpdate.entrySet()) {
      JSONObject jsonObject = new JSONObject(entry.getValue());
      jsonObject.put("time", getCurrentTimeStamp());
      jsonObject.put("connect_host", iPAddrRemote);
      jsonObject.put("metric_type", query.substring(query.indexOf("=") + 1, query.indexOf(",") - 1));
      jsonObject.put("metric_group", entry.getKey());
      printWriterOut.println(jsonObject.toString());
      printWriterOut.flush();
    }
  }

  public void cleanup() throws IOException{
    if(printWriterOut != null)
      printWriterOut.close();
    if(bw != null)
      bw.close();
    if(fw != null)
      fw.close();
    if(jmxc != null)
      jmxc.close();
  }

  public void getmetrics() throws Exception {
    update("kafka.consumer:type=consumer-coordinator-metrics,client-id=*", clientConsumerMetrics);
    update("kafka.consumer:type=consumer-fetch-manager-metrics,client-id=*", clientConsumerFetchMetrics);
    update("kafka.connect:type=connector-task-metrics,connector=kafka-connect-splunk,task=*", taskConnectMetrics);
    update("kafka.connect:type=task-error-metrics,connector=kafka-connect-splunk,task=*", taskConnectErrorMetrics);
  }

}

public class JMXKafkaConnect {

  static ArrayList<KafkaConnectNode> nodesList;

  public static void createThread(Integer sleep, KafkaConnectNode node) {
    Runnable r = new Runnable() {
      public void run() {
        try {
          while (true) {
            node.getmetrics();
            Thread.sleep(sleep);
          }
        } catch (Exception ex){
        }
      }
    };

    new Thread(r).start();
  }

  public static void main(String [] args)
  {
    System.out.println("Starting JMXKafkaConnect");

    String filenamePrefix = "JMXKafkaConnect";

    Properties prop = new Properties();
    String propFileName = "config.properties";

    try{
      FileInputStream in = new FileInputStream(propFileName);
      prop.load(in);
    } catch (FileNotFoundException ex) {
      System.out.println(ex.toString());
    } catch (IOException e) {
      System.out.println(e.toString());
    }
    prop.list(System.out);

    Integer servers = Integer.parseInt(prop.getProperty("servers", "1"));
    Integer customSleepTime = Integer.parseInt(prop.getProperty("sleep", "60")) * 1000;

    final Integer numberOfServers = servers;

    nodesList = new ArrayList<KafkaConnectNode>();
    for(Integer i = 0; i < numberOfServers; i++){
      String host = prop.getProperty("server" + i + "_host");
      String port = prop.getProperty("server" + i + "_port");
      KafkaConnectNode node = new KafkaConnectNode(host, Integer.parseInt(port), filenamePrefix);
      nodesList.add(node);
      createThread(customSleepTime, node);
    }

    Runtime.getRuntime().addShutdownHook(new Thread() {
      public void run() {
        System.out.println("Shutdown Hook is running....");
        try {
          for(Integer i = 0; i < numberOfServers; i++){
            nodesList.get(i).cleanup();
          }
        } catch (IOException ex) {
          System.out.println(ex.toString());
        }
      }
    });

    while(true) {

    }
  }
}
