import javax.net.ssl.SSLServerSocket;
import javax.net.ssl.SSLServerSocketFactory;
import javax.net.ssl.SSLSocket;
import java.io.*;
import java.net.Socket;
import java.security.NoSuchAlgorithmException;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

/***************************************************************************************************
 * This Class is responsible for accepting SSL connections and broadcasting messages. Server core  *
 * accepts incoming traffic and creates a thread for each one. The username and socket of a        *
 * authenticated Client will be kept in a ConcurrentHashMap.                                       *
 *                                                                                                 *
 * Usage:                                                                                          *
 * - The server folder should contain the server key store                                         *
 * - The UserAndPass file can be created manually or will be created when the server runs          *
 * - Clients then follow the server messages and respond with proper answers                       *
 * - After registration or authentication, clients are able to join the server and start group     *
 *   conversations.                                                                                *
 * - Clients can leave the chat server by simply sending "BYE" message to the server.              *
 *                                                                                                 *
 ***************************************************************************************************
 *                                      SERVER CORE CLASS                                          *
 *                                                                                                 *
 ***************************************************************************************************
 */
public class serverCore extends Thread {
    private static ConcurrentMap<String,Socket> clientList = new ConcurrentHashMap<String, Socket>();
    private Socket socket = null;
    private BufferedReader buff_In = null;
    private DataOutputStream buff_Out = null;
    private serverAuth auth_User;

    /*================================== Constructor ====================================*/
    public serverCore (SSLSocket socket,serverAuth serverAuth) {
        this.socket = socket;
        this.auth_User = serverAuth;
    }
    /*================================== Remove Client ==================================*/
    public synchronized void removeClient (String client,ConcurrentMap<String,Socket> clientList) {
        if (clientList.containsKey(client))
            clientList.remove(client);
    }
    /*================================== Send a Message =================================*/
    public synchronized void sendMessage(String message,Socket socket) throws IOException {
        DataOutputStream dataOutputStream = new DataOutputStream(new BufferedOutputStream(socket.getOutputStream()));
        dataOutputStream.writeBytes("\r\n"+message+"\r\n");
        dataOutputStream.flush();
    }
    /*===============================  Send a Message to All  ============================*/
    public synchronized void broadcastMessage(String message, ConcurrentMap<String,Socket> clientList) throws IOException {
        for (String client:clientList.keySet())
            sendMessage(message,clientList.get(client));

    }
    /*=====================================  run  ========================================*/
    public void run() {
        try {
            buff_In = new BufferedReader(new InputStreamReader(this.socket.getInputStream()));
            buff_Out = new DataOutputStream(new BufferedOutputStream(this.socket.getOutputStream()));
            int counter = 3,counter1 = 3;
            String tempUser = null,currentUser = null,tempAnswer,tempPass;
            String message;
            do {
                buff_Out.writeBytes("Do you have an account?(y/n): ");
                buff_Out.flush();
                tempAnswer = buff_In.readLine();
                if (tempAnswer.equalsIgnoreCase("y")) {
                    sendMessage("Username: ",this.socket);
                    tempUser = buff_In.readLine();
                    sendMessage("Password: ",this.socket);
                    tempPass = buff_In.readLine();
                    if (this.auth_User.isAuthenticated(tempUser,tempPass) && !tempUser.isEmpty() && !clientList.containsKey(tempUser.toLowerCase())) {
                        clientList.put(tempUser,this.socket);
                        currentUser = tempUser;
                        break;
                        }
                    else
                        sendMessage("Wrong Username/Password!",this.socket);
                }
                else if (tempAnswer.equalsIgnoreCase("n")) {
                    do {

                        sendMessage("Please choose a username contains only number and character: ", this.socket);
                        tempUser = buff_In.readLine();
                        if (this.auth_User.containsNumCharOnly(tempUser)) {
                            sendMessage("Please choose a password: ", this.socket);
                            tempPass = buff_In.readLine();
                            if (this.auth_User.meetPassCondition(tempPass)){
                                if (this.auth_User.createUser(tempUser, tempPass)) {
                                    clientList.put(tempUser.toLowerCase(),this.socket);
                                    currentUser = tempUser;
                                    break;
                                } else {
                                    sendMessage("Username is already taken!", this.socket);
                                    }
                            } else {
                                sendMessage("Password should be larger than 8 characters and contain:",this.socket);
                                sendMessage("At least one lowercase,\n" +
                                            "At least one uppercase,\n" +
                                            "At least one digit,\n" +
                                            "At least one special character!\n" +
                                            "Try again!",this.socket);
                                counter1--;
                            }
                        }
                    } while (counter1 != 0);
                break;
                }
                else if (tempAnswer.isEmpty())
                        counter--;

            } while (counter != 0);
            if (counter != 0) {
                sendMessage("You are logged in! Type BYE to exit. ",this.socket);
                sendMessage("Enjoy using our service! ",this.socket);
                broadcastMessage("SERVER: " + currentUser + " has joined the room!",clientList);
                while ((message = buff_In.readLine()) != null) {
                        if (!message.equals("BYE"))
                            broadcastMessage(currentUser + ": " + message, clientList);
                        else if (message.equals("BYE")) {
                            removeClient(currentUser, clientList);
                            broadcastMessage("SERVER: " + tempUser + " left!", clientList);
                            break;
                        }
                    }

                }
            removeClient(currentUser,clientList); //remove the client if the client's connection is closed without BYE
            this.buff_In.close();
            this.buff_Out.close();
            this.socket.close();

        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        }
    }

    /*=====================================  Main  =======================================*/
    public static void main (String[] args){


        try {
            System.setProperty("javax.net.ssl.keyStore","chatterServerKeyStore");
            System.setProperty("javax.net.ssl.keyStorePassword","1234567890");
            SSLServerSocketFactory sslServerSocketfactory = (SSLServerSocketFactory)SSLServerSocketFactory.getDefault();
            SSLServerSocket sslServerSocket = (SSLServerSocket)sslServerSocketfactory.createServerSocket(3000);
            System.out.println("Server started on port \"" + sslServerSocket.getLocalPort() + "\" ...");
            File file = new File ("UserAndPass.txt");
            if (!file.createNewFile())
                file = new File ("UserAndPass.txt");
            serverAuth serverAuth = new serverAuth(file.getName());
            while (true) {
                SSLSocket sslSocket = (SSLSocket)sslServerSocket.accept();
                Thread thread = new serverCore(sslSocket,serverAuth);
                thread.start();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }

    }
}
