package src.sockets;

import src.swf.*;
import java.io.*;
import java.net.*;
import com.google.gson.*;

public class Client {
    private Socket clientSocket;
    private ObjectOutputStream oos;
    private ObjectInputStream ois;
 
    public boolean startConnection(String ip, int port) {
        try {
            System.out.println("Starting Connection...");
            this.clientSocket = new Socket(ip, port);
            this.oos = new ObjectOutputStream(this.clientSocket.getOutputStream());
            this.ois = new ObjectInputStream(this.clientSocket.getInputStream());
            return true;
        } catch (Exception e) {
            return false;
        }
    }
 
    public void sendMessage(String msg) {
        try {
            System.out.println("Sending Message \"" + msg + "\"");
            this.oos.writeObject(msg);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public String getLogFile() {
        String message = "";
        try {
            sendMessage("getfile");
            message = (String) this.ois.readObject();
        } catch ( IOException ioe ) {
            ioe.printStackTrace();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return message;
    }

    public String getProcessedLogFile() {
        String message = "";
        try {
            sendMessage("getprocessedfile");
            message = (String) this.ois.readObject();
        } catch ( IOException ioe ) {
            ioe.printStackTrace();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return message;
    }

    public String getFileCount() {
        String message = "";
        try {
            sendMessage("getfilecount");
            message = (String) this.ois.readObject();
        } catch ( IOException ioe ) {
            ioe.printStackTrace();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        return message;
    }

    public SWFFile getFile() {
        SWFJson swfJson = null;
        try {
            //write to socket using ObjectOutputStream
            sendMessage("getgame");
            String message = (String) this.ois.readObject();
            if(message != "complete") {
                swfJson = new Gson().fromJson(message, SWFJson.class);
            }
        } catch ( IOException ioe ) {
            ioe.printStackTrace();
        } catch ( Exception e ) {
            e.printStackTrace();
        }

        //if there are no more files, return null.
        if(swfJson == null) return null;

        return new SWFFile(swfJson.path, swfJson.number);
    }
 
    public SWFFile parseFileJSON(String json) {
        sendMessage("GetFile");
        return new SWFFile("", 0);
    }

    public void stopConnection() {
        try {
            this.ois.close();
            this.oos.close();
            this.clientSocket.close();
        } catch (Exception e) {
            //do nothing for now.
        }
    }
}