package kz.asfendiyarov.heartsimulator;

import org.json.*;
import java.util.*;

public final class DeviceState {
    public String firmware="";
    public String playbackState="STOPPED";
    public int track=0;
    public String trackName="";
    public boolean paused=false;
    public int volume=15;
    public int eq=0;
    public String eqName="Нормальный";
    public boolean gainEnabled=false;
    public int gain=15;
    public boolean dirty=false;
    public boolean profileSaved=false;
    public int savedProfiles=0;
    public boolean backupAvailable=false;
    public String checksum="";
    public long uptimeMs=0;
    public boolean diagActive=false;
    public boolean controlVerifyActive=false;
    public boolean controlVerificationComplete=false;
    public int controlRespondedCount=0;
    public int controlVerifiedCount=0;
    public String controlVerificationText="";
    public int feedbackCount=0;
    public int sdCount=0;
    public int trackConfirmedCount=0;
    public String lastWarning="";
    public String lastEvent="";
    public long controlSequence=0;
    public String lastControlAction="";
    public final List<ModuleState> modules=new ArrayList<>();

    public static DeviceState from(JSONObject o) throws JSONException {
        DeviceState s=new DeviceState();
        s.firmware=o.optString("firmware","");
        s.playbackState=o.optString("playbackState", o.optBoolean("paused",false)?"PAUSED":(o.optInt("track",0)>0?"PLAYING":"STOPPED"));
        s.track=o.optInt("track",0);
        s.trackName=o.optString("trackName","");
        s.paused=o.optBoolean("paused",false);
        s.volume=o.optInt("volume",15);
        s.eq=o.optInt("eq",0);
        s.eqName=o.optString("eqName","Нормальный");
        s.gainEnabled=o.optBoolean("gainEnabled",false);
        s.gain=o.optInt("gain",15);
        s.dirty=o.optBoolean("dirty",false);
        s.profileSaved=o.optBoolean("profileSaved",false);
        s.savedProfiles=o.optInt("savedProfiles",0);
        s.backupAvailable=o.optBoolean("backupAvailable",false);
        s.checksum=o.optString("checksum","");
        s.uptimeMs=o.optLong("uptimeMs",0);
        s.diagActive=o.optBoolean("diagActive",false);
        s.controlVerifyActive=o.optBoolean("controlVerifyActive",false);
        s.controlVerificationComplete=o.optBoolean("controlVerificationComplete",false);
        s.controlRespondedCount=o.optInt("controlRespondedCount",0);
        s.controlVerifiedCount=o.optInt("controlVerifiedCount",0);
        s.controlVerificationText=o.optString("controlVerificationText","");
        s.feedbackCount=o.optInt("feedbackCount",0);
        s.sdCount=o.optInt("sdCount",0);
        s.trackConfirmedCount=o.optInt("trackConfirmedCount",0);
        s.lastWarning=o.optString("lastWarning","");
        s.lastEvent=o.optString("lastEvent","");
        s.controlSequence=o.optLong("controlSequence",0);
        s.lastControlAction=o.optString("lastControlAction","");

        JSONArray a=o.optJSONArray("modules");
        if(a!=null) {
            for(int i=0;i<a.length();i++) s.modules.add(ModuleState.from(a.getJSONObject(i)));
        }
        return s;
    }

    public boolean isPlaying(){ return "PLAYING".equals(playbackState); }
    public boolean isPaused(){ return "PAUSED".equals(playbackState) || "PAUSING".equals(playbackState); }
    public boolean isStopped(){ return "STOPPED".equals(playbackState); }
    public boolean isTransient(){
        return "STARTING".equals(playbackState) || "PAUSING".equals(playbackState) || "STOPPING".equals(playbackState);
    }
}

final class ModuleState {
    int n;
    boolean checked;
    boolean feedbackOk;
    boolean sdOk;
    boolean trackQueryOk;
    boolean trackMatch;
    boolean idleHigh;
    boolean stateQueryOk;
    boolean stateMatch;
    int files;
    int reportedTrack;
    int playerStateRaw;
    String playerState="";
    String message="";

    static ModuleState from(JSONObject o){
        ModuleState m=new ModuleState();
        m.n=o.optInt("n",0);
        m.checked=o.optBoolean("checked",false);
        m.feedbackOk=o.optBoolean("feedbackOk",false);
        m.sdOk=o.optBoolean("sdOk",false);
        m.trackQueryOk=o.optBoolean("trackQueryOk",false);
        m.trackMatch=o.optBoolean("trackMatch",false);
        m.idleHigh=o.optBoolean("idleHigh",false);
        m.stateQueryOk=o.optBoolean("stateQueryOk",false);
        m.stateMatch=o.optBoolean("stateMatch",false);
        m.files=o.optInt("files",-1);
        m.reportedTrack=o.optInt("reportedTrack",-1);
        m.playerStateRaw=o.optInt("playerStateRaw",-1);
        m.playerState=o.optString("playerState","UNKNOWN");
        m.message=o.optString("message","");
        return m;
    }
}
