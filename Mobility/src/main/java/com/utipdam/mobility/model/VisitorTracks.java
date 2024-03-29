package com.utipdam.mobility.model;

import lombok.Data;

@Data
public class VisitorTracks {
    private int siteId;
    private int regionId;
    private long visitorId;
    private int populationType;
    private int globalId;
    private String firstTimeSeen;
    private String lastTimeSeen;

    public VisitorTracks(){

    }

    public VisitorTracks(int siteId, int regionId, long visitorId,
                         int populationType, int globalId, String firstTimeSeen, String lastTimeSeen){

        this.siteId = siteId;
        this.regionId = regionId;
        this.visitorId = visitorId;
        this.populationType = populationType;
        this.globalId = globalId;
        this.firstTimeSeen = firstTimeSeen;
        this.lastTimeSeen = lastTimeSeen;
    }

}
