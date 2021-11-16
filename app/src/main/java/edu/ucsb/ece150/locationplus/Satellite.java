package edu.ucsb.ece150.locationplus;

import android.location.GnssStatus;

/*
 * This class is provided as a way for you to store information about a single satellite. It can
 * be helpful if you would like to maintain the list of satellites using an ArrayList (i.e.
 * ArrayList<Satellite>). As in Homework 3, you can then use an Adapter to update the list easily.
 *
 * You are not required to implement this if you want to handle satellite information in using
 * another method.
 */
public class Satellite {

    // [TODO] Define private member variables
    int mSatNum;
    float mAzimuth;
    float mElevation;
    //float mCarrierFreq;
    float mCND;
    String mConstellation;
    int mSVid;
    boolean inFix;

    String[] constellation_str = {
        "UNKNOWN",
        "GPS",
        "SBAS",
        "GLONASS",
        "QZSS",
        "BEIDOU",
        "GALILEO",
        "IRNSS"
    };

    // [TODO] Write the constructor
    public Satellite(float azimuth, float elevation, float CND, int constellation,
                     int SVid, boolean fix) {
        mAzimuth = azimuth;
        mElevation = elevation;
        //mCarrierFreq = carrierFreq;
        mCND = CND;
        mConstellation = constellation_str[constellation];
        mSVid = SVid;
        inFix = fix;
    }

    // [TODO] Implement the toString() method. When the Adapter tries to assign names to items
    // in the ListView, it calls the toString() method of the objects in the ArrayList
    @Override
    public String toString() {
        return "Satellite: " + mSVid;
    }
}
