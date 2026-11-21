package dev.redlab.gamespace.bean;

import java.util.ArrayList;
import java.util.Arrays;

/* loaded from: classes.dex */
public class OMTInfo {
    private String en_note;
    private String[] games;
    public int hasVote;
    private int hot_level;
    private int id;
    private String minimum_app_version;
    private String note;
    private ArrayList<String> omtString;
    private String[] scenario;
    private String tr_note;

    public OMTInfo(int i, String str, String str2, String str3, String str4, String[] strArr, String[] strArr2, int i2, int i3, ArrayList<String> arrayList) {
        this.id = i;
        this.note = str;
        this.en_note = str2;
        this.tr_note = str3;
        this.minimum_app_version = str4;
        this.games = strArr;
        this.scenario = strArr2;
        this.hot_level = i2;
        this.hasVote = i3;
        this.omtString = arrayList;
    }

    public int getId() {
        return this.id;
    }

    public void setId(int i) {
        this.id = i;
    }

    public String getNote() {
        return this.note;
    }

    public void setNote(String str) {
        this.note = str;
    }

    public String getEn_note() {
        return this.en_note;
    }

    public void setEn_note(String str) {
        this.en_note = str;
    }

    public String getTr_note() {
        return this.tr_note;
    }

    public void setTr_note(String str) {
        this.tr_note = str;
    }

    public String getMinimum_app_version() {
        return this.minimum_app_version;
    }

    public void setMinimum_app_version(String str) {
        this.minimum_app_version = str;
    }

    public String[] getGames() {
        return this.games;
    }

    public void setGames(String[] strArr) {
        this.games = strArr;
    }

    public String[] getScenario() {
        return this.scenario;
    }

    public void setScenario(String[] strArr) {
        this.scenario = strArr;
    }

    public int getHot_level() {
        return this.hot_level;
    }

    public void setHot_level(int i) {
        this.hot_level = i;
    }

    public int getHasVote() {
        return this.hasVote;
    }

    public void setHasVote(int i) {
        this.hasVote = i;
    }

    public ArrayList<String> getOmtString() {
        return this.omtString;
    }

    public void setOmtString(ArrayList<String> arrayList) {
        this.omtString = arrayList;
    }

    public String toString() {
        return "OMTInfo{id=" + this.id + ", note='" + this.note + "', en_note='" + this.en_note + "', tr_note='" + this.tr_note + "', minimum_app_version='" + this.minimum_app_version + "', games=" + Arrays.toString(this.games) + ", scenario=" + Arrays.toString(this.scenario) + ", hot_level=" + this.hot_level + ", hasVote=" + this.hasVote + ", omtString=" + this.omtString + '}';
    }
}
