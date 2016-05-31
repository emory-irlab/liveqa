package edu.emory.mathcs.ir.liveqa.ranking.ranklib;

import ciir.umass.edu.learning.DataPoint;
import ciir.umass.edu.learning.tree.LambdaMART;

/**
 * This is a workaround the issue with accessing protected static from Scala.
 */
public abstract class DataPointEx extends DataPoint {
    static {
        LambdaMART.nRoundToStopEarly = 1000;
        LambdaMART.nTrees = 1000;
        LambdaMART.learningRate = 0.05f;
    }
    public void setFeatureCount(int val) {
        DataPoint.featureCount = val;
    }
}
