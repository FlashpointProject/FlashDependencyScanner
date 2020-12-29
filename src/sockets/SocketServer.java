package src.sockets;

import src.swf.*;
import java.util.*;
import java.io.*;
import java.util.concurrent.atomic.*;
import java.net.ServerSocket;
import java.net.Socket;

public class SocketServer extends Thread
{
    private static final Object lock = new Object();
    static AtomicInteger _offset = new AtomicInteger(0);
    static List<SWFFile> _files;
    private boolean _running = false;
    private ObjectInputStream _ois;
    private ObjectOutputStream _oos;
    private Socket _socket;
    private String _logFile;

    public SocketServer(Socket socket, List<SWFFile> files, String fileName) {
        this._socket = socket;
        this._files = files;
        this._logFile = fileName;
        this._ois = null;
        this._oos = null;
    }

    public void startThread() {
        System.out.println( "Starting Server..." );
        this.start();
    }

    public void stopServer() {
        this._running = false;
        this.interrupt();
    }

    @Override
    public void run() {
        this._running = true;

        System.out.println( "Listening for a connection..." );
        try {
            this._oos = new ObjectOutputStream(this._socket.getOutputStream());
            this._ois = new ObjectInputStream(this._socket.getInputStream());
            while( this._running ) {
                    String message = "";
                    try {
                        message = (String) this._ois.readObject();
                        if(message.equalsIgnoreCase("getgame")) {
                            int c = _offset.getAndIncrement();
                            System.out.println("Message Received " + c + ": " + message);
                            if(c < _files.size()-1) {
                                this._oos.writeObject(_files.get(c).toJSON());
                            } else {
                                System.out.println("No files left to scan.");
                                break;
                            }
                            //this._oos.writeObject("" + counter.getAndIncrement());
                        }
                        if(message.equalsIgnoreCase("getfile")) {
                            this._oos.writeObject(this._logFile);
                        }
                        
                        if(message.equalsIgnoreCase("exit")) this._running = false;
                    } catch (IOException ioe) {
                        //EOF exception
                    }
            }

            System.out.println( "Closing server..." );
            this._ois.close();
            this._oos.close();
            this._socket.close();
        } catch (IOException e) {
            System.out.println( e );
            e.printStackTrace();
        } catch (ClassNotFoundException cnf) {
            System.out.println( cnf );
            cnf.printStackTrace();
        }
    }
}

/*
    public static void main( String[] args ) {
        if( args.length == 0 ) {
            System.out.println( "Usage: SimpleSocketServer <port>" );
            System.exit( 0 );
        }

        int port = Integer.parseInt( args[ 0 ] );
        System.out.println( "Start server on port: " + port );
        SimpleSocketServer server = new SimpleSocketServer( port );
        server.startServer();

        // Automatically shutdown in 1 minute
        try {
            Thread.sleep( 60000 );
        } catch( Exception e ) {
            e.printStackTrace();
        }

        server.stopServer();
    }
*/