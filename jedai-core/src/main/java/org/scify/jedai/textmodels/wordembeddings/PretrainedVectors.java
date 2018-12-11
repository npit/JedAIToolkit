package org.scify.jedai.textmodels.wordembeddings;

import com.esotericsoftware.minlog.Log;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import org.scify.jedai.textmodels.ITextModel;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

import java.io.*;
import java.util.*;

/*
  Class to load and handle pretrained word vectors.
  Embeddings file should be place in the file:
  <sources_dir>/JedAIToolkit/jedai-core/src/main/resources/embeddings/weights.txt

  Expected file format is:
  <dimension>,<separator>
  word1<separator>value1<separator>value2....
  .....

  e.g.
  4 ,
  town,2.1,4.0,6.22,8.9
  car,8.0,7.11,6.41,4.44
  .....

  Examples of word embeddings that can be used (conversion to the above format may be required):
  Word2Vec (Mikolov, 2013): https://code.google.com/archive/p/word2vec/
  Glove (Pennington, 2014): https://nlp.stanford.edu/projects/glove/ 
  FastText (Joulin, 2017): https://fasttext.cc/
  Global context embeddings (Huang, 2012) https://www.socher.org/index.php/Main/ImprovingWordRepresentationsViaGlobalContextAndMultipleWordPrototypes
  e.t.c.

 */


public class PretrainedVectors extends VectorSpaceModel{

    char dataSeparator;
    static Double[] unkownWordVector;
    static boolean weightsLoaded = false;
    static Map<String, Double[]> wordMap;
    int numWords;

    /**
     * Constructor
     */
    public PretrainedVectors(int dId, int n, RepresentationModel md, SimilarityMetric sMetric, String iName) {
        super(dId, n, md, sMetric, iName);
        Log.set(Log.LEVEL_INFO);
        numWords = 0;
        loadWeights();
        aggregateVector = getZeroVector();
    }

    /**
     * Load pretrained embedding weights.
     */
   private void loadWeights(){
       if (weightsLoaded) return;
       Log.info("Loading weights.");
       ClassLoader classLoader = getClass().getClassLoader();
       //String fileName = classLoader.getResource("embeddings/weights-full.txt").getFile();
       String fileName = classLoader.getResource("embeddings/weights.txt").getFile();
       wordMap = new HashMap<>();

       try {
           BufferedReader br = new BufferedReader(new FileReader(fileName));
           // first read parsing metadata, split by commas
           String [] header = br.readLine().split(",");
           try {
               dimension = Integer.parseInt(header[0]);
               dataSeparator = header[1].charAt(0);
           }catch (NumberFormatException ex){
               Log.error("Pretrained header malformed -- expected:<dimension>");
               System.exit(-1);
           }
           Log.info(String.format("Read dimension: [%d], delimiter: [%c]", dimension, dataSeparator));

           CSVReader reader = new CSVReader(new FileReader(fileName), dataSeparator, CSVParser.DEFAULT_QUOTE_CHARACTER, 1);
           List<String[]> vectors = reader.readAll();
           for (int s=1; s<vectors.size(); ++s){
               String [] components = vectors.get(s);
               if (components.length != dimension + 1)
                   throw new IOException(String.format("Mismatch in embedding vector #%d length : %d.",
                           s, vectors.size(), components.length));
               Double [] value = new Double[dimension];
               for (int i=1; i<=dimension; ++i){
                   value[i-1] = Double.parseDouble(components[i]);
               }
               wordMap.put(components[0], value);
           }
       } catch (FileNotFoundException e) {
           e.printStackTrace();
           Log.error("No resource file found:" + fileName);
           System.exit(-1);
       } catch (IOException e) {
           e.printStackTrace();
           Log.error("IO exception when reading:" + fileName);
           System.exit(-1);
       }

       unkownWordVector = getZeroVector();
       weightsLoaded = true;
   }

    /**
     * Zero vector fetcher
     */
   private Double[] getZeroVector(){
        Double [] vector = new Double[dimension];
        for(int i=0;i<dimension;++i) vector[i] = 0.0d;
        return vector;
   }

    /**
     * Normalizes to the number of words (as part of the arithmetic mean aggregation)
     */
    @Override
    public void finalizeModel() {
        if (numWords > 0) {
            // produce average
            for (int i = 0; i < dimension; ++i)
                aggregateVector[i] /= numWords;
        }
    }

    @Override
    public Set<String> getSignatures() {
        System.out.println("Getting signatures");
        return null;
    }

    /**
     * How to handle a missing token
     */
    void handleUnknown(String token){
        // addWordVector(getZeroVector());
    }

    /**
     * Add a word vector to the entity collection
     * Already implemented here as arithmetic mean, hence adding.
     *
     * @param vector : the word vector
     */
    void addWordVector(Double[] vector){
       for(int i=0;i<dimension;++i)
           aggregateVector[i] += vector[i];
       numWords++;
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
            if (this.wordMap.containsKey(token)){
                addWordVector(wordMap.get(token));
                localUpdates ++;
            }
            else
                handleUnknown(token);
        }
        Log.debug("Used " + localUpdates + " element(s) from [text]: [" + text + "]");
    }
}
