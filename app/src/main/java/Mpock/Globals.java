package Mpock;

/**
 * Created by Mac on 2016/03/17.
 */
public class Globals {


    public static final String appName = "mpocket";
    //MY STUFF
    public static final String MPOCK_PREFS = "mpocketkeypad_Prefs";

    public static final Boolean log = true;
    //MESSAGE DELIVERED TO BROKER BRAODCAST..
    public static final boolean clean = false;

    //public static final String root = "www.xanthansoftware.com";
    public static final String root = "lipamobile.com";
    //public static final String BASE_URL = "http://"+root+":8080";
    public static final String BASE_URL = "http://"+root+":9090";

    //MQ STUFF
    //MESSAGE DELIVERED TO BROKER BRAODCAST..
    public static final String heartbeatString = "mpock_heartbeat";
    public static final int heartBeatMqQos = 0;

    //MQ STUFF
  public static final String mqRoot = "207.182.150.74";
    //public static final String mqRoot = "192.168.10.2";
    public static int MQ_ROOT_PORT = 1883;
   //public static int MQ_ROOT_PORT = 1983;


    public static final String MESS_REC = "new message received";
    public static final String NEW_REQUEST = "new_request";
    public static final int mqQos = 1;
    public static final String DEL_TO_SERV = "DEL_TO_SERV";
    public static final String COMMAND = "command";
    public static final String CHAT_MESSAGE = "chatMessage";
    public static final String NEW_MESSAGE = "new_message";

    //STUDENT CHARGE STUFF
    public static final String NEW_STUDENT_CHARGE = "new_student_charge";
    public static final String STUDENT_CHARGE_REPLY = "student_charge_reply";
    public static final String STUDENT_CHARGE_FINISHED = "student_charge_finished";

    public static String PARTNER_MAIN_DEVICE = "mpocket/C0:BD:D1:0D:50:5D";

    public static final String DEVICE_NAME = "device_name";
    public static final String PARTNER_DEVICE_NAME = "partner_device_name";

    public static final String DISCONNECTED = "disconnected";
    public static final String CONNECTED = "connected";


}
