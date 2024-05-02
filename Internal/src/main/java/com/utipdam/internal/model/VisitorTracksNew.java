package com.utipdam.internal.model;

import lombok.Data;

@Data
public class VisitorTracksNew {
    //dataset_id,location_id,anonymized_unique_id,start_time,end_time,distance
    private int datasetId;
    private int locationId;
    private long anonymizedUniqueId;
    private String startTime;
    private String endTime;
    private String distance;

    public VisitorTracksNew(){

    }

    public VisitorTracksNew(int datasetId, int locationId, long anonymizedUniqueId,
                            String startTime, String endTime, String distance){

        this.datasetId = datasetId;
        this.locationId = locationId;
        this.anonymizedUniqueId = anonymizedUniqueId;
        this.startTime = startTime;
        this.endTime = endTime;
        this.distance = distance;
    }

}
