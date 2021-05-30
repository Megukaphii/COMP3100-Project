package classes;

import java.util.ArrayList;

public class Server {
    // serverType serverID state curStartTime core mem disk #wJobs #rJobs [#failures totalFailtime mttf mttr madf lastStartTime]
    public static final String STATE_ACTIVE = "active";
    public static final String STATE_INACTIVE = "inactive";
    public static final String STATE_IDLE = "idle";
    public static final String STATE_UNAVAILABLE = "unavailable";

    private String serverType;
    private int serverID;
    private String state;
    private int curStartTime;
    private int cores;
    private int memory;
    private int disk;
    private int wJobs;
    private int rJobs;
    private ArrayList<Job> waitingJobs;
    private ArrayList<Job> runningJobs;

    public Server(String serverState) {
        String[] temp = serverState.split(" ");

        this.serverType = temp[0];
        this.serverID = Integer.parseInt(temp[1]);
        this.state = temp[2];
        this.curStartTime = Integer.parseInt(temp[3]);
        this.cores = Integer.parseInt(temp[4]);
        this.memory = Integer.parseInt(temp[5]);
        this.disk = Integer.parseInt(temp[6]);
        this.wJobs = Integer.parseInt(temp[7]);
        this.rJobs = Integer.parseInt(temp[8]);

        Job standInJob = new Job("JOBN 0 0 400 1 4 500");

        this.waitingJobs = new ArrayList<>();
        // for (int i = 0; i < this.wJobs; i++) {
        //     this.waitingJobs.add(standInJob);
        // }
        this.runningJobs = new ArrayList<>();
        // for (int i = 0; i < this.rJobs; i++) {
        //     this.runningJobs.add(standInJob);
        // }
    }

    public String getServerType() {
        return this.serverType;
    }

    public int getServerID() {
        return this.serverID;
    }

    public String getState() {
        return this.state;
    }

    public int getNumberOfCores() {
        return this.cores;
    }

    public int getMemory() {
        return this.memory;
    }

    public int getDisk() {
        return this.disk;
    }

    public void AddJob(Job newJob) {
        if (getAvailableCores() > newJob.getCore()) {
            runningJobs.add(newJob);
        } else {
            waitingJobs.add(newJob);
        }
    }

    public void RemoveJob(int jobID) {
        for (int i = 0; i < runningJobs.size(); i++) {
            if (runningJobs.get(i).getJobID() == jobID) {
                runningJobs.remove(i);
            }
        }
        updateRunningJobs();
    }

    public void updateRunningJobs() {
        int availableCores = getAvailableCores();
        boolean[] removeJobs = new boolean[this.waitingJobs.size()];

        for (int i = 0; i < this.waitingJobs.size(); i++) {
            Job checkingJob = this.waitingJobs.get(i);
            if (checkingJob.getCore() < availableCores) {
                removeJobs[i] = true;
                this.runningJobs.add(checkingJob);
                availableCores -= checkingJob.getCore();
            } else {
                // Possibly break if capable job isn't found?
                removeJobs[i] = false;
            }
        }

        for (int i = this.waitingJobs.size() - 1; i > 0; i--) {
            if (removeJobs[i]) {
                this.waitingJobs.remove(i);
            }
        }
    }

    public int getWaitingJobCount() {
        return this.waitingJobs.size();
    }

    public int getRunningJobCount() {
        return this.runningJobs.size();
    }

    public int getAvailableCores() {
        int coresReq = 0;
        for (int i = 0; i < this.runningJobs.size(); i++) {
            coresReq += this.runningJobs.get(i).getCore();
        }

        return coresReq;
    }

    public boolean isCompatibleWithJob(Job a) {
        return a.getCore() <= this.getNumberOfCores() && a.getDisk() <= this.getDisk() && a.getMemory() <= this.getMemory();
    }

    public void printServer() {
        System.out.println(this.serverType + " " + this.serverID + " " + this.state + " " + this.cores);
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Job)) {
            return false;
        }
        Server compare = (Server) obj;
        return this.serverType == compare.getServerType() && this.serverID == compare.getServerID();
    }
}