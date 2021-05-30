package classes;

import java.util.ArrayList;

public class Job {
    // JOBN submitTime jobID estRuntime core memory disk
    
    private String type;
    private int submitTime;
    private int jobID;
    private int estRunTime;
    private int core;
    private int memory;
    private int disk;
    private String serverType;
    private int serverID;
            
    public Job(String state) {
        String[] temp = state.split(" ");
        
        this.type = temp[0];
        if (this.type.equals("JOBN")) {
            this.submitTime = Integer.parseInt(temp[1]);
            this.jobID = Integer.parseInt(temp[2]);
            this.estRunTime = Integer.parseInt(temp[3]);
            this.core = Integer.parseInt(temp[4]);
            this.memory = Integer.parseInt(temp[5]);
            this.disk = Integer.parseInt(temp[6]);
        }

        if (this.type.equals("JCPL")) {
            this.jobID = Integer.parseInt(temp[2]);
            this.serverType = temp[3];
            this.serverID = Integer.parseInt(temp[4]);
        }
    }

    public int getJobID() {
        return this.jobID;
    }

    public String getType() {
        return this.type;
    }

    public int getCore() {
        return this.core;
    }

    public int getMemory() {
        return this.memory;
    }

    public int getDisk() {
        return this.disk;
    }

    public String getServerType() {
        return this.serverType;
    }

    public int getServerID() {
        return this.serverID;
    }

    public boolean equals(Object obj) {
        if (obj == this) {
            return true;
        }
        if (!(obj instanceof Job)) {
            return false;
        }
        Job compare = (Job) obj;
        return this.jobID == compare.getJobID();
    }
}