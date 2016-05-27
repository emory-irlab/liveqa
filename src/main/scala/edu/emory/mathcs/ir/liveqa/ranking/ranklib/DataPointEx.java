package edu.emory.mathcs.ir.liveqa.ranking.ranklib;

import ciir.umass.edu.learning.DataPoint;

/**
 * This is a workaround the issue with accessing protected static from Scala.
 */
public abstract class DataPointEx extends DataPoint {
    public void setFeatureCount(int val) {
        DataPoint.featureCount = val;
    }
}
