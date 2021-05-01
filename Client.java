import java.io.*;
import java.net.*;
import java.util.ArrayList;
import java.util.stream.Collectors;
import classes.Server;
import classes.Job;
import classes.Tuple;

public class Client {
    public static final int JOB_RUNNING = 2;
    public static String algorithm = "allToLargest";

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
            if (args.length > 0) {
                algorithm = args[0];
            }

            ArrayList<Job> jobs = new ArrayList<Job>();

            send("HELO", dout);
        
            receive("OK", din);

            send("AUTH corymacdonald", dout);
            
            receive("OK", din);

            send("REDY", dout);

            String job = receive(din); // JOBN ...
            jobs.add(new Job(job));

            send("GETS All", dout);

            receive(din); // DATA nRecs recLen

            send("OK", dout);

            String serversString = receive(din);

            send("OK", dout);

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
                    Tuple selected = selectServer(current, servers, dout, din);

                    send("SCHD " + current.getJobID() + " " + selected.getX() + " " + selected.getY(), dout);

                    receive(din);

                    send("REDY", dout);
                } else if (current.getType().equals("JCPL")) {
                    send("REDY", dout);
                }

                current = new Job(receive(din));
            }

            send("QUIT", dout);

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void send(String toSend, DataOutputStream dout) throws IOException {
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

    public static Tuple selectServer(Job current, ArrayList<Server> servers, DataOutputStream dout, DataInputStream din) throws IOException {
        if (algorithm.equals("allToLargest")) {
            return allToLargest(current, servers);
        } else {
            return turnaroundTime(current, servers, dout, din);
        }
    }

    public static Tuple allToLargest(Job current, ArrayList<Server> servers) throws IOException {
        String sType = "lol";
        int sID = 0;

        final int coreCount = current.getCore();
        ArrayList<Server> compatibleServers = (ArrayList<Server>)servers.stream().filter(
            (server) -> (server.getNumberOfCores() >= coreCount)
        ).collect(Collectors.toList());

        sType = compatibleServers.get(0).getServerType();
        sID = compatibleServers.get(0).getServerID();

        return new Tuple(sType, sID);
    }

    public static Tuple turnaroundTime(Job current, ArrayList<Server> servers, DataOutputStream dout, DataInputStream din) throws IOException {
        String sType = "lol";
        int sID = 0;

        // REPLACE WITH GETS CAPABLE
        final int coreCount = current.getCore();
        ArrayList<Server> compatibleServers = (ArrayList<Server>)servers.stream().filter(
            (server) -> (server.getNumberOfCores() >= coreCount)
        ).collect(Collectors.toList());

        int minJobs = 1;

        for (int i = 0; i < compatibleServers.size(); i++) {
            send("CNTJ " + compatibleServers.get(i).getServerType() + " " + compatibleServers.get(i).getServerID() + " " + JOB_RUNNING, dout);

            int cntj = Integer.parseInt(receive(din));

            if (cntj < minJobs) {
                sType = compatibleServers.get(i).getServerType();
                sID = compatibleServers.get(i).getServerID();
                break;
            } else {
                minJobs = cntj;
            }
        }

        if (sType.equals("lol")) {
            sType = compatibleServers.get(0).getServerType();
            sID = compatibleServers.get(0).getServerID();
        }

        return new Tuple(sType, sID);
    }
}
