package org.scify.jedai.textmodels.embeddings;

import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import com.esotericsoftware.minlog.Log;

public class PretrainedWordVectors extends PretrainedVectors{
    /**
     * Constructor
     *
     * @param dId
     * @param n
     * @param md
     * @param sMetric
     * @param iName
     */
    public PretrainedWordVectors(int dId, int n, RepresentationModel md, SimilarityMetric sMetric, String iName) {
        super(dId, n, md, sMetric, iName);
    }


    /**
     * Updates the model with the input text. Tokenizes, maps each token to the aggregate vector.
     * @param text
     */
    @Override
    public void updateModel(String text) {
        int localUpdates=0;
        final String[] tokens = text.toLowerCase().split("[\\W_]");
        for (String token : tokens){
            if (this.elementMap.containsKey(token)){
                addVector(elementMap.get(token));
                localUpdates ++;
            }
            else
                handleUnknown(token);
        }
        Log.debug("Used " + localUpdates + " element(s) from [text]: [" + text + "]");
    }


}
