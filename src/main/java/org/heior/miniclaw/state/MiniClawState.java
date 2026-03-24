package org.heior.miniclaw.state;

public class MiniClawState {

    private String lastHeartbeat;
    private String lastDistill;
    private boolean needsDistill;
    private long dailyLogBytes;

    public MiniClawState(String lastHeartbeat, String lastDistill, boolean needsDistill, long dailyLogBytes) {
        this.lastHeartbeat = lastHeartbeat;
        this.lastDistill = lastDistill;
        this.needsDistill = needsDistill;
        this.dailyLogBytes = dailyLogBytes;
    }
    public MiniClawState() {
        this.lastHeartbeat = null;
        this.lastDistill = null;
        this.needsDistill = false;
        this.dailyLogBytes = 0L;
    }

    public long getDailyLogBytes() {
        return dailyLogBytes;
    }

    public String getLastDistill() {
        return lastDistill;
    }
    public String getLastHeartbeat() {
        return lastHeartbeat;
    }

    public boolean isNeedsDistill() {
        return needsDistill;
    }

    public void setLastHeartbeat(String lastHeartbeat) {
        this.lastHeartbeat = lastHeartbeat;
    }

    public void setLastDistill(String lastDistill) {
        this.lastDistill = lastDistill;
    }

    public void setNeedsDistill(boolean needsDistill) {
        this.needsDistill = needsDistill;
    }

    public void setDailyLogBytes(long dailyLogBytes) {
        this.dailyLogBytes = dailyLogBytes;
    }

}
