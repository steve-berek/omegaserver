package com.steveberek.cordova.plugin;

import android.os.AsyncTask;
import android.text.Html;
import android.util.Log;

import org.apache.cordova.CallbackContext;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.PluginResult;
import org.json.JSONArray;
import org.json.JSONObject;

import java.io.BufferedWriter;
import java.io.DataInputStream;
import java.io.DataOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.lang.reflect.Method;
import java.net.InetAddress;
import java.net.InetSocketAddress;
import java.net.ServerSocket;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Locale;
import java.util.Map;
import java.util.TimeZone;
import java.util.regex.Pattern;

public class OmegaServer extends CordovaPlugin {


    private String TAG = "OMEGA_HTTP_SERVER";
    private CallbackContext globalCallbackContext;
    private String DEFAULT_DOMAIN = "localhost";
    private String rootPath = "";
    private int port = 80;

    @Override
    public boolean execute(String action, JSONArray args,
                           final CallbackContext callbackContext) {
        globalCallbackContext = callbackContext;
        // Verify that the user sent a 'show' action
        if (!action.equals("startServer") && !action.equals("stopServer")) {
            callbackContext.error("\"" + action + "\" is not a recognized action.");
            return false;
        }
        if (action.equals("startServer")) {
            try {
                JSONObject options = args.getJSONObject(0);
                String root = options.getString("root");
                String port = options.getString("port");
                Log.d(TAG, "ROOTPATH_VALUE -> " + root);
                Log.d(TAG, "PORT_VALUE -> " + port);
                if (!root.equals("") && !port.equals("")) {
                    this.rootPath = root;
                    this.port = Integer.valueOf(port);
                    cordova.getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            new TinyServerTask().executeOnExecutor(AsyncTask.SERIAL_EXECUTOR);
                            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                            globalCallbackContext.sendPluginResult(pluginResult);
                        }
                    });
                } else {
                    callbackContext.error("ROOT PATH OR PORT IS EMPTY");
                    return false;
                }

            } catch (Exception e) {
                callbackContext.error("Error encountered: " + e.getMessage());
                return false;
            }
        }
        if (action.equals("stopServer")) {
            try{
                    cordova.getActivity().runOnUiThread(new Runnable() {
                        public void run() {
                            stopServer();   
                            PluginResult pluginResult = new PluginResult(PluginResult.Status.OK);
                            globalCallbackContext.sendPluginResult(pluginResult);
                            
                        }
                    });


            } catch (Exception e) {
                callbackContext.error("Error encountered: " + e.getMessage());
                return false;
            }
        }
        return true;
    }

    public void startServer() {
        TinyWebServer.startServer( this.DEFAULT_DOMAIN , this.port, this.rootPath);
    }

    public void stopServer() {
        TinyWebServer.stopServer();
    }
    public class TinyServerTask extends AsyncTask<String, Void, Void> {
        protected Void doInBackground(String... urls) {
            Log.d(TAG, "ASYNC_TASK_RUNNING...");
            startServer();
            return null;
        }
    }

    public static class TinyWebServer extends Thread {

        /**
         * @param args the command line arguments
         */
        private static ServerSocket serverSocket;
        private final Map<String, String> lowerCaseHeader = new HashMap<>();

        private String TAG = "OMEGA_HTTP_SERVER";
        public static String CONTENT_TYPE = "text/html";
        private String CONTENT_DATE = "";
        private String CONN_TYPE = "";
        private String Content_Encoding = "";
        private String content_length = "";
        private String STATUS = "200";
        private boolean keepAlive = true;
        private String SERVER_NAME = "Firefly http server v0.1";
        private static final String MULTIPART_FORM_DATA_HEADER = "multipart/form-data";
        private static final String ASCII_ENCODING = "US-ASCII";
        private String REQUEST_TYPE = "GET";
        private String HTTP_VER = "HTTP/1.1";

        //all status
        public static String PAGE_NOT_FOUND = "404";
        public static String OKAY = "200";
        public static String CREATED = "201";
        public static String ACCEPTED = "202";
        public static String NO_CONTENT = "204";
        public static String PARTIAL_NO_CONTENT = "206";
        public static String MULTI_STATUS = "207";
        public static String MOVED_PERMANENTLY = "301";
        public static String SEE_OTHER = "303";
        public static String NOT_MODIFIED = "304";
        public static String TEMP_REDIRECT = "307";
        public static String BAD_REQUEST = "400";
        public static String UNAUTHORIZED_REQUEST = "401";
        public static String FORBIDDEN = "403";
        public static String NOT_FOUND = "404";
        public static String METHOD_NOT_ALLOWED = "405";
        public static String NOT_ACCEPTABLE = "406";
        public static String REQUEST_TIMEOUT = "408";
        public static String CONFLICT = "409";
        public static String GONE = "410";
        public static String LENGTH_REQUIRED = "411";
        public static String PRECONDITION_FAILED = "412";

        public static String PAYLOAD_TOO_LARGE = "413";
        public static String UNSUPPORTED_MEDIA_TYPE = "415";
        public static String RANGE_NOT_SATISFIABLE = "416";
        public static String EXPECTATION_FAILED = "417";
        public static String TOO_MANY_REQUESTS = "429";

        public static String INTERNAL_ERROR = "500";
        public static String NOT_IMPLEMENTED = "501";
        public static String SERVICE_UNAVAILABLE = "503";
        public static String UNSUPPORTED_HTTP_VERSION = "505";

        public static final String CONTENT_DISPOSITION_REGEX = "([ |\t]*Content-Disposition[ |\t]*:)(.*)";

        public static final Pattern CONTENT_DISPOSITION_PATTERN = Pattern.compile(CONTENT_DISPOSITION_REGEX, Pattern.CASE_INSENSITIVE);

        public static final String CONTENT_TYPE_REGEX = "([ |\t]*content-type[ |\t]*:)(.*)";

        public static final Pattern CONTENT_TYPE_PATTERN = Pattern.compile(CONTENT_TYPE_REGEX, Pattern.CASE_INSENSITIVE);

        public static final String CONTENT_DISPOSITION_ATTRIBUTE_REGEX = "[ |\t]*([a-zA-Z]*)[ |\t]*=[ |\t]*['|\"]([^\"^']*)['|\"]";

        public static final Pattern CONTENT_DISPOSITION_ATTRIBUTE_PATTERN = Pattern.compile(CONTENT_DISPOSITION_ATTRIBUTE_REGEX);

        public static final String CONTENT_LENGTH_REGEX = "Content-Length:";
        public static final Pattern CONTENT_LENGTH_PATTERN = Pattern.compile(CONTENT_LENGTH_REGEX, Pattern.CASE_INSENSITIVE);

        public static final String USER_AGENT = "User-Agent:";
        public static final Pattern USER_AGENT_PATTERN = Pattern.compile(USER_AGENT, Pattern.CASE_INSENSITIVE);

        public static final String HOST_REGEX = "Host:";
        public static final Pattern CLIENT_HOST_PATTERN = Pattern.compile(HOST_REGEX, Pattern.CASE_INSENSITIVE);

        public static final String CONNECTION_TYPE_REGEX = "Connection:";
        public static final Pattern CONNECTION_TYPE_PATTERN = Pattern.compile(CONNECTION_TYPE_REGEX, Pattern.CASE_INSENSITIVE);

        public static final String ACCEPT_ENCODING_REGEX = "Accept-Encoding:";
        public static final Pattern ACCEPT_ENCODING_PATTERN = Pattern.compile(ACCEPT_ENCODING_REGEX, Pattern.CASE_INSENSITIVE);

        private static final String CONTENT_REGEX = "[ |\t]*([^/^ ^;^,]+/[^ ^;^,]+)";

        private static final Pattern MIME_PATTERN = Pattern.compile(CONTENT_REGEX, Pattern.CASE_INSENSITIVE);

        private static final String CHARSET_REGEX = "[ |\t]*(charset)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";

        private static final Pattern CHARSET_PATTERN = Pattern.compile(CHARSET_REGEX, Pattern.CASE_INSENSITIVE);

        private static final String BOUNDARY_REGEX = "[ |\t]*(boundary)[ |\t]*=[ |\t]*['|\"]?([^\"^'^;^,]*)['|\"]?";

        private static final Pattern BOUNDARY_PATTERN = Pattern.compile(BOUNDARY_REGEX, Pattern.CASE_INSENSITIVE);

        private String ip;
        private int port;

        public static String WEB_DIR_PATH="/";
        public static String SERVER_IP="localhost";
        public static int SERVER_PORT=9000;
        public static boolean isStart=true;
        public static String INDEX_FILE_NAME="index.html";
        private Thread clientThread;
        private Socket clientSocket;


        public TinyWebServer(final String ip, final int port) throws IOException {
        this.ip = ip;
        this.port = port;
        initSocket();
        }

        public void initSocket() throws IOException {
            if( serverSocket != null ) {
                serverSocket.close();
                serverSocket = null ;
            }

            InetAddress addr = InetAddress.getByName(ip);
            // serverSocket = new ServerSocket(port, 100, addr);
            serverSocket = new ServerSocket();
            serverSocket.setReuseAddress(true);
            serverSocket.bind(new InetSocketAddress(addr, port));
            Log.d(TAG, "RUNNING_PROCESS_TOP_SUPER");
            // serverSocket.setSoTimeout(30000);
        }
        public void restartSocket()  throws IOException {
            try {
                if ( serverSocket.isClosed() ) {
                    InetAddress addr = InetAddress.getByName(ip);
                    serverSocket.bind(new InetSocketAddress(addr, port));
                }
                clientSocket = serverSocket.accept();
                clientSocket.setKeepAlive(true);
                // clientSocket.setSoTimeout(30000);
                clientThread = new EchoThread(clientSocket);
                clientThread.start();
            } catch (Exception ex) {
                Log.d(TAG, "RUNNING_PROCESS_RESTART_SOCKET");
                ex.printStackTrace();
            }

        }
        @Override
        public void run() {
            Log.d(TAG, "RUNNING_PROCESS_TOP_FIRST");
            while (isStart) {
                    try {
                        Log.d(TAG, "RUNNING_PROCESS_TOP");
                        //wait for new connection on port 5000
                        restartSocket();

                    } catch (SocketTimeoutException s) {
                        s.printStackTrace();
                        Log.d(TAG, "RUNNING_PROCESS_TOP_TIMEOUT");
                    } catch (IOException e) {
                        if(null!= serverSocket&& !serverSocket.isClosed()) {
                            try {
                                Log.i(TAG,"Closing Server connection");
                                serverSocket.close();
                            } catch (IOException e2) {
                                e2.printStackTrace();
                            }
                        }
                        e.printStackTrace();
                        Log.d(TAG, "RUNNING_PROCESS_TOP_IOEXCEPTION");

                    }
            }//endof Never Ending while loop
            Log.d(TAG, "RUNNING_PROCESS_LOOP_END");
        }

        public class EchoThread extends Thread {

            protected Socket socket;
            protected boolean nb_open;
            private DataInputStream in ;
            private DataOutputStream out;

            public EchoThread(Socket clientSocket) {
                this.socket = clientSocket;
                this.nb_open = true;
            }

            @Override
            public void run() {

                try {


                    Log.d(TAG, "RUNNING_PROCESS");
                    if (socket.isConnected()) {
                        in = new DataInputStream(socket.getInputStream());
                        out = new DataOutputStream(socket.getOutputStream());
                    } else {
                      //  restartSocket();
                        return;
                    }

                    if ( socket.getInputStream() != null && socket.getOutputStream() != null ) {
                        byte[] data = new byte[1500];
                        //socket.setSoTimeout(60 * 1000 * 5);

                        while ( socket.isConnected() && in.read(data) != -1) {
                            String recData = new String(data).trim();
                            //System.out.println("received data: \n" + recData);
                            //System.out.println("------------------------------");
                            String[] header = recData.split("\\r?\\n");

                            String contentLen = "0";
                            String contentType = "text/html";
                            String connectionType = "keep-alive";
                            String hostname = "";
                            String userAgent = "";
                            String encoding = "";

                            String[] h1 = header[0].split(" ");
                            if (h1.length == 3) {
                                setRequestType(h1[0]);
                                setHttpVer(h1[2]);
                            }

                            for (int h = 0; h < header.length; h++) {
                                String value = header[h].trim();

                                //System.out.println(header[h]+" -> "+CONTENT_LENGTH_PATTERN.matcher(header[h]).find());
                                if (CONTENT_LENGTH_PATTERN.matcher(value).find()) {
                                    contentLen = value.split(":")[1].trim();
                                } else if (CONTENT_TYPE_PATTERN.matcher(value).find()) {
                                    contentType = value.split(":")[1].trim();
                                } else if (CONNECTION_TYPE_PATTERN.matcher(value).find()) {
                                    connectionType = value.split(":")[1].trim();
                                } else if (CLIENT_HOST_PATTERN.matcher(value).find()) {
                                    hostname = value.split(":")[1].trim();
                                } else if (USER_AGENT_PATTERN.matcher(value).find()) {
                                    for (String ua : value.split(":")) {
                                        if (!ua.equalsIgnoreCase("User-Agent:")) {
                                            userAgent += ua.trim();
                                        }
                                    }
                                } else if (ACCEPT_ENCODING_PATTERN.matcher(value).find()) {
                                    encoding = value.split(":")[1].trim();
                                }

                            }

                            if (!REQUEST_TYPE.equals("")) {
                                String postData = "";
                                if (REQUEST_TYPE.equalsIgnoreCase("POST") && !contentLen.equals("0")) {
                                    postData = header[header.length - 1];
                                    if (postData.length() > 0 && contentLen.length() > 0) {
                                        int len = Integer.valueOf(contentLen);
                                        postData = postData.substring(0, len);
                                        // System.out.println("Post data -> " + contentLen + " ->" + postData);
                                    }
                                }

                                // System.out.println("contentLen ->" + contentLen + "\ncontentType ->" + contentType + "\nhostname ->" + hostname + "\nconnectionType-> " + connectionType + "\nhostname ->" + hostname + "\nuserAgent -> " + userAgent);
                                final String requestLocation = h1[1];
                                if (requestLocation != null) {
                                    processLocation(out, requestLocation, postData);
                                }
                                //System.out.println("requestLocation "+requestLocation);
                            }
                        Log.d(TAG, "RUNNING_PROCESS_SLEEP_BEFORE ->"+ CONTENT_TYPE);
                        Thread.sleep(400);
                        Log.d(TAG, "RUNNING_PROCESS_SLEEP_AFTER -> "+ CONTENT_TYPE);
                        }
                        Log.d(TAG, "RUNNING_PROCESS_WHILE_END");
                    }


                } catch (Exception er) {
                    Log.d(TAG, "RUNNING_PROCESS_BOTTOM_EXCEPTION");
                    er.printStackTrace();
                    try {
                        in.close();
                        out.close();
                        clientSocket.close();
                        serverSocket.close();
                        initSocket();
                        restartSocket();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                }

            }

        }

        private static String cleanTextContent(String text)
        {
            // strips off all non-ASCII characters
            text = text.replaceAll("[^\\x00-\\x7F]", "");

            // erases all the ASCII control characters
           // text = text.replaceAll("[\\p{Cntrl}&&[^\r\n\t]]", "");

            // removes non-printable characters from Unicode
           // text = text.replaceAll("\\p{C}", "");

            return text.trim();
        }

        public void processLocation(DataOutputStream out, String location, String postData) {

            String data = "";
            switch (location) {
                case "/":
                    //root location, server index file
                    CONTENT_TYPE = "text/html";
                    data=readFile(WEB_DIR_PATH+"/"+INDEX_FILE_NAME);
                    // data = cleanTextContent(data);
                    constructHeader(out, data.length() + "", data);
                    break;
                default:

                    System.out.println("url location -> " + location);
                    URL geturl = getDecodedUrl("http://localhost" + location);
                    String[] dirPath = geturl.getPath().split("/");
                    String fullFilePath=geturl.getPath();
                    if (dirPath.length > 1) {
                        String fileName = dirPath[dirPath.length - 1];
                        HashMap qparms = (HashMap) splitQuery(geturl.getQuery());
                        if(REQUEST_TYPE.equals("POST")){
                            if (qparms==null){ qparms=new HashMap<String,String>();}
                            qparms.put("_POST", postData);
                        }
                        //System.out.println("File name " + fileName);
                        //System.out.println("url parms " + qparms);
                        CONTENT_TYPE = getContentType(fileName);
                        Log.d(TAG, "CONTENT_TYPE_FILE -> " + CONTENT_TYPE);
                        if(!CONTENT_TYPE.equals("text/plain")){
                            // System.out.println("Full file path - >"+fullFilePath +" "+CONTENT_TYPE);

                            if(CONTENT_TYPE.equals("image/jpeg") || CONTENT_TYPE.equals("image/png") || CONTENT_TYPE.equals("video/mp4")){
                                byte[] bytdata=readImageFiles(WEB_DIR_PATH+fullFilePath,CONTENT_TYPE);
                                Log.d(TAG, "CONTENT_TYPE_FILE_READING_MEDIA... -> " + data);
                                //System.out.println(bytdata.length);
                                if(bytdata!=null){
                                    constructHeaderImage(out, bytdata.length+"", bytdata);
                                }else{
                                    pageNotFound();
                                }
                            }else if (CONTENT_TYPE.equals("text/html")){
                                data=readFile(WEB_DIR_PATH+fullFilePath);
                                // data = cleanTextContent(data);
                                // data = Html.fromHtml(data).toString();

                                Log.d(TAG, "CONTENT_TYPE_FILE_READING_HMTL... -> " + data);
                                if(!data.equals("")){
                                    constructHeader(out, String.valueOf(data.length()), data);
                                }else{
                                    pageNotFound();
                                }
                            } else  {
                                data=readFile(WEB_DIR_PATH+fullFilePath);
                                data = cleanTextContent(data);
                                Log.d(TAG, "CONTENT_TYPE_FILE_READING_DIFF... -> " + data);
                                if(!data.equals("")){
                                    constructHeader(out, String.valueOf(data.length()), data);
                                }else{
                                    pageNotFound();
                                }
                            }
                        }else{
                            data = getResultByName(fileName, qparms);
                            constructHeader(out, data.length() + "", data);
                        }


                    }
            }

        }

        public URL getDecodedUrl(String parms) {
            try {
                //String decodedurl =URLDecoder.decode(parms,"UTF-8");
                URL aURL = new URL(parms);
                return aURL;
            } catch (Exception er) {
            }
            return null;
        }

        public static HashMap<String, String> splitQuery(String parms) {
            try {
                final HashMap<String, String> query_pairs = new HashMap<>();
                final String[] pairs = parms.split("&");
                for (String pair : pairs) {
                    final int idx = pair.indexOf("=");
                    final String key = idx > 0 ? URLDecoder.decode(pair.substring(0, idx), "UTF-8") : pair;
                    if (!query_pairs.containsKey(key)) {
                        query_pairs.put(key, "");
                    }
                    final String value = idx > 0 && pair.length() > idx + 1 ? URLDecoder.decode(pair.substring(idx + 1), "UTF-8") : null;
                    query_pairs.put(key, value);
                }
                return query_pairs;
            } catch (Exception er) {
            }
            return null;
        }

        public String getResultByName(String name, HashMap qparms) {
            try {
                String ClassName = "appapis.queryfiles.AppApis";
                Class<?> rClass = Class.forName(ClassName); // convert string classname to class
                Object obj = rClass.newInstance();          // invoke empty constructor
                Method getNameMethod = obj.getClass().getMethod(name, HashMap.class);
                STATUS = TinyWebServer.OKAY;
                return getNameMethod.invoke(obj, qparms).toString();
            } catch (Exception er) {
                // er.printStackTrace();
                return pageNotFound();
            }
        }

        public void setRequestType(String type) {
            // System.out.println("REQUEST TYPE " + type);
            this.REQUEST_TYPE = type;
        }

        public void setHttpVer(String httpver) {
            // System.out.println("REQUEST ver " + httpver);
            this.HTTP_VER = httpver;
        }

        public String getRequestType() {
            return this.REQUEST_TYPE;
        }

        public String getHttpVer() {
            return this.HTTP_VER;
        }

        public String pageNotFound() {
            STATUS = NOT_FOUND;
            CONTENT_TYPE = "text/html";
            //customize your page here
            return "<!DOCTYPE html>"
                    + "<html><head><title>Page not found | Firefly web server</title>"
                    + "</head><body><h3>Requested page not found</h3></body></html>";
        }

        //hashtable initilization for content types
        static Hashtable<String, String> mContentTypes = new Hashtable();

        {
            mContentTypes.put("js", "application/javascript");
            mContentTypes.put("php", "text/html");
            mContentTypes.put("java", "text/html");
            mContentTypes.put("json", "application/json");
            mContentTypes.put("png", "image/png");
            mContentTypes.put("jpg", "image/jpeg");
            mContentTypes.put("html", "text/html");
            mContentTypes.put("css", "text/css");
            mContentTypes.put("mp4", "video/mp4");
            mContentTypes.put("mov", "video/quicktime");
            mContentTypes.put("wmv", "video/x-ms-wmv");

        }

        //get request content type
        public static String getContentType(String path) {
            String type = tryGetContentType(path);
            if (type != null) {
                return type;
            }
            return "text/plain";
        }

        //get request content type from path
        public static String tryGetContentType(String path) {
            int index = path.lastIndexOf(".");
            if (index != -1) {
                String e = path.substring(index + 1);
                String ct = mContentTypes.get(e);
                // System.out.println("content type: " + ct);
                if (ct != null) {
                    return ct;
                }
            }
            return null;
        }

        private void constructHeader(DataOutputStream output, String size, String data) {
            SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
            gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
            PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)), false);
            pw.append("HTTP/1.1 ").append(STATUS).append(" \r\n");
            if (this.CONTENT_TYPE != null) {
                printHeader(pw, "Content-Type", this.CONTENT_TYPE);
            }
            printHeader(pw, "Date", gmtFrmt.format(new Date()));
            printHeader(pw, "X-Content-Type-Options", "nosniff");
            printHeader(pw, "Connection", (this.keepAlive ? "keep-alive" : "close"));
            printHeader(pw, "Content-Length", size);
            printHeader(pw, "Server", SERVER_NAME);
            pw.append("\r\n");
            pw.append(data);
            pw.flush();
            //pw.close();
        }

        private void constructHeaderImage(DataOutputStream output, String size, byte[] data) {
            try{

                SimpleDateFormat gmtFrmt = new SimpleDateFormat("E, d MMM yyyy HH:mm:ss 'GMT'", Locale.US);
                gmtFrmt.setTimeZone(TimeZone.getTimeZone("GMT"));
                PrintWriter pw = new PrintWriter(new BufferedWriter(new OutputStreamWriter(output)), false);
                pw.append("HTTP/1.1 ").append(STATUS).append(" \r\n");
                if (this.CONTENT_TYPE != null) {
                    printHeader(pw, "Content-Type", this.CONTENT_TYPE);
                }
                printHeader(pw, "Date", gmtFrmt.format(new Date()));
                printHeader(pw, "Connection", (this.keepAlive ? "keep-alive" : "close"));
                printHeader(pw, "Content-Length", size);
                printHeader(pw, "Server", SERVER_NAME);
                pw.append("\r\n");
                pw.flush();
                output.write(data);
                output.flush();
                //System.out.println("data sent success");

                //pw.close();
            }catch(Exception er){er.printStackTrace();}

        }


        @SuppressWarnings("static-method")
        protected void printHeader(PrintWriter pw, String key, String value) {
            pw.append(key).append(": ").append(value).append("\r\n");
        }

        public byte[] readImageFiles(String fileName,String filetype){
            try{
                File ifile=new File(fileName);
                if(ifile.exists()){
                    if(filetype.equalsIgnoreCase("image/png") || filetype.equalsIgnoreCase("image/jpeg") || filetype.equalsIgnoreCase("image/gif") || filetype.equalsIgnoreCase("image/jpg")){
                        FileInputStream fis = new FileInputStream(fileName);
                        byte[] buffer = new byte[fis.available()];
                        while (fis.read(buffer) != -1) {}
                        fis.close();
                        return buffer;
                    }
                }else{

                }
            }catch(Exception er){}
            return null;
        }
        public String readFile(String fileName){
            String content="";
            try{
                File ifile=new File(fileName);
                if(ifile.exists()){
                    FileInputStream fis = new FileInputStream(fileName);
                    byte[] buffer = new byte[10];
                    StringBuilder sb = new StringBuilder();
                    while (fis.read(buffer) != -1) {
                        sb.append(new String(buffer));
                        buffer = new byte[10];
                    }
                    fis.close();
                    content = sb.toString();
                }else{
                    pageNotFound();
                    return content;
                }
            }catch(Exception er){
                pageNotFound();
                return "";
            }
            return content;
        }


        public static void init(String ip,int port,String public_dir){

            SERVER_IP=ip;
            SERVER_PORT=port;
            WEB_DIR_PATH=public_dir;
            scanFileDirectory();

        }

        public static void startServer(String ip,int port,String public_dir){
            try {

                isStart=true;
                init(ip,port,public_dir);
                Thread t = new TinyWebServer(SERVER_IP, SERVER_PORT);
                t.start();
                Log.d("TINY_SERVER_TEST", "Server Started !");

            } catch (Exception ex) {
                // Log.d("TINY_SERVER_TEST_ERROR", ex.getMessage());
                ex.printStackTrace();
            }
        }

        public static void stopServer(){
            if(isStart){
                try{
                    isStart=false;
                    serverSocket.close();
                    System.out.println("Server stopped running !");
                }catch(IOException er){
                    er.printStackTrace();
                }
            }
        }


        //scan for index file
        public static void scanFileDirectory(){
            boolean isIndexFound=false;
            try{
                File file=new File(WEB_DIR_PATH);
                if(file.isDirectory()){
                    File[] allFiles=file.listFiles();
                    for (File allFile : allFiles) {
                        //System.out.println(allFile.getName().split("\\.")[0]);
                        if(allFile.getName().split("\\.")[0].equalsIgnoreCase("index")){
                            TinyWebServer.INDEX_FILE_NAME=allFile.getName();
                            isIndexFound=true;
                        }
                    }
                }

            }catch(Exception er){}

            if(!isIndexFound){
                System.out.println("Index file not found !");
            }
        }
    }
}