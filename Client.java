import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.stream.Collectors;
import classes.Server;
import classes.Job;
import classes.Tuple;

public class Client {
    public static void printErrorMessage(String expected, String actual) {
        System.out.println("Expected: " + expected + " | Actual: " + actual);
    }

    public static void printErrorMessage(int expected, int actual) {
        System.out.println("Expected: " + expected + " | Actual: " + actual);
    }


    public static void main(String[] args) throws Exception {
        // Using Java SE 7's Automatic Resource Management to call close() for us, since these objects implement the AutoCloseable interface.
        // This works even if any of the code throws an exception.
        try (
            Socket s = new Socket("localhost", 50000);
            DataInputStream din = new DataInputStream(s.getInputStream());
            DataOutputStream dout = new DataOutputStream(s.getOutputStream());
        ) {
            ArrayList<Job> jobs = new ArrayList<Job>();

            send(dout, "HELO");
        
            receive("OK", din);

            send(dout, "AUTH shadoweagle7");
            
            receive("OK", din);

            send(dout, "REDY");

            String job = receive(din); // JOBN ...
            jobs.add(new Job(job));

            send(dout, "GETS All");

            receive(din); // DATA nRecs recLen

            send(dout, "OK");

            String serversString = receive(din);

            send(dout, "OK");

            receive(".", din);

            // The magic infinite debug loop
            // int i = 0; while (i < Integer.MAX_VALUE) { i++; }

            // Build servers array
            ArrayList<Server> servers = new ArrayList<Server>();
            String serverStateAll = new String(serversString);
            String[] serverStates = serverStateAll.split("\n");

            for (String serverState : serverStates) {
                servers.add(new Server(serverState));
            }

            // Filter servers
            servers.removeIf((server) -> {
                return !(server.getState().equals(Server.STATE_IDLE) || server.getState().equals(Server.STATE_INACTIVE));
            });

            // Sort servers
            servers.sort((Server l, Server r) -> {
                if (l.getNumberOfCores() > r.getNumberOfCores()) {
                    return -1;
                } else if (l.getNumberOfCores() < r.getNumberOfCores()) {
                    return 1;
                }

                return 0;
            });

            Job current = jobs.get(0);
            
            while (!current.getType().equals("NONE")) {
                if (current.getType().equals("JOBN")) {
                    Tuple selected = selectServer(current, servers);

                    send(dout, "SCHD " + current.getJobID() + " " + selected.getX() + " " + selected.getY());

                    receive(din);

                    send(dout, "REDY");
                } else if (current.getType().equals("JCPL")) {
                    send(dout, "REDY");
                }

                current = new Job(receive(din));
            }

            send(dout, "QUIT");

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void send(DataOutputStream dout, String toSend) throws IOException {
        System.out.println("Sending " + toSend);
        dout.write(toSend.getBytes());
        dout.flush();
    }

    public static String receive(DataInputStream din) throws IOException {
        int size = 4096;
        byte[] data = new byte[size];
        if (din.read(data) >= size) {
            printErrorMessage(size, data.length);
            //System.out.println("Server: " + new String(data, 0, expectedSize));
            //throw new IllegalArgumentException("Server gave unexpected response");
        }

        String receivedString = new String(data).trim();

        System.out.println("Server: " + receivedString);

        return receivedString;
    }

    public static String receive(String expectedString, DataInputStream din) throws IOException, IllegalArgumentException {
        int expectedSize = expectedString.length();
        byte[] data = new byte[expectedSize];

        if (din.read(data) >= expectedSize) {
            //System.out.println("Server: " + new String(data, 0, expectedSize));
            //throw new IllegalArgumentException("Server gave unexpected response");
        }

        String receivedString = new String(data, 0, expectedSize).trim();
        System.out.println("Server: " + receivedString);
        
        if (!receivedString.equals(expectedString)) {
            printErrorMessage(expectedString, receivedString);

            throw new IllegalArgumentException("Server sent unknown message");
        }

        return receivedString;
    }

    public static Tuple selectServer(Job current, ArrayList<Server> servers) throws IOException {
        String sType = "lol";
        int sID = 0;

        final int coreCount = current.getCore();
        ArrayList<Server> compatibleServers = (ArrayList<Server>)servers.stream().filter(
            (server) -> (server.getNumberOfCores() >= coreCount)
        ).collect(Collectors.toList());

        // int minJobs = 1;

        // for (int i = 0; i < compatibleServers.size(); i++) {
        //     send(dout, "CNTJ " + compatibleServers.get(i).getServerType() + " " + compatibleServers.get(i).getServerID() + " " + 2 /* the running job state */ );

        //     byte[] received = new byte[4];
        //     int cntj = Integer.parseInt(receive(received, 4, din));

        //     if (cntj < minJobs) {
        //         sType = compatibleServers.get(i).getServerType();
        //         sID = compatibleServers.get(i).getServerID();
        //         break;
        //     } else {
        //         minJobs = cntj;
        //     }
        // }

        // if (sType.equals("lol")) {
        //     sType = compatibleServers.get(0).getServerType();
        //     sID = compatibleServers.get(0).getServerID();
        // }

        sType = compatibleServers.get(0).getServerType();
        sID = compatibleServers.get(0).getServerID();

        return new Tuple(sType, sID);
    }
}
