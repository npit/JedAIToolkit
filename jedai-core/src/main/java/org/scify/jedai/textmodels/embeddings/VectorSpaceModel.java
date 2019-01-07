package org.scify.jedai.textmodels.embeddings;

import org.scify.jedai.textmodels.AbstractModel;
import org.scify.jedai.textmodels.ITextModel;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;
import com.esotericsoftware.minlog.Log;

public abstract class VectorSpaceModel extends AbstractModel {
    Double[] aggregateVector;
    static int dimension;
    public VectorSpaceModel(int dId, int n, RepresentationModel md, SimilarityMetric sMetric, String iName) {
        super(dId, n, md, sMetric, iName);
    }
    Double[] getVector(){
        return aggregateVector;
    }
    int getDimension(){
        return dimension;
    }

    @Override
    public double getSimilarity(ITextModel oModel) {
        switch (simMetric) {
            case COSINE_SIMILARITY:
                return getCosineSimilarity((VectorSpaceModel) oModel);
            default:
                Log.error("The given similarity metric is incompatible with the bag representation model!");
                System.exit(-1);
                return -1;
        }
    }

    /**
     * Cosine similarity for two arithmetic vectors
     * @param oModel the other VS model
     * @return the cosine similarity value
     */
    public double getCosineSimilarity(VectorSpaceModel oModel){
        // get vectors
        Double[] v1 = getVector();
        Double[] v2 = oModel.getVector();
        double norm1 = 0.0d;
        double norm2 = 0.0d;
        double dot=0.0d;
        for (int i=0;i<getDimension();++i){
            dot += v1[i] * v2[i];
            norm1 += Math.pow(v1[i], 2);
            norm2 += Math.pow(v2[i], 2);
        }
        return dot / (Math.sqrt(norm1) * Math.sqrt(norm2));

    }

}
