package org.scify.jedai.textmodels.embeddings;

import com.esotericsoftware.minlog.Log;
import com.opencsv.CSVParser;
import com.opencsv.CSVReader;
import org.scify.jedai.utilities.enumerations.RepresentationModel;
import org.scify.jedai.utilities.enumerations.SimilarityMetric;

import java.io.*;
import java.util.*;

/*
  Class to load and handle pretrained vectors.
  Embeddings file should be place in the file:
  <sources_dir>/JedAIToolkit/jedai-core/src/main/resources/embeddings/weights.txt

  Expected file format is:
  <dimension>,<separator>
  element1<separator>value1<separator>value2....
  .....

  e.g.
  4,,
  town,2.1,4.0,6.22,8.9
  car,8.0,7.11,6.41,4.44
  .....

  Examples of element embeddings that can be used (conversion to the above format may be required):
  Word2Vec (Mikolov, 2013): https://code.google.com/archive/p/word2vec/
  Glove (Pennington, 2014): https://nlp.stanford.edu/projects/glove/ 
  FastText (Joulin, 2017): https://fasttext.cc/
  Global context embeddings (Huang, 2012) https://www.socher.org/index.php/Main/ImprovingWordRepresentationsViaGlobalContextAndMultipleWordPrototypes
  e.t.c.

 */


public abstract class PretrainedVectors extends VectorSpaceModel{

    char dataSeparator;
    static Double[] unkownVector;
    static boolean weightsLoaded = false;
    static Map<String, Double[]> elementMap;
    int numElements;

    /**
     * Constructor
     */
    public PretrainedVectors(int dId, int n, RepresentationModel md, SimilarityMetric sMetric, String iName) {
        super(dId, n, md, sMetric, iName);
        numElements = 0;
        loadWeights();
        aggregateVector = getZeroVector();
        //Log.set(Log.LEVEL_DEBUG);
    }

    /**
     * Load pretrained embedding weights.
     */
   private void loadWeights(){
       if (weightsLoaded) return;
       ClassLoader classLoader = getClass().getClassLoader();
       //String fileName = classLoader.getResource("embeddings/weights-full.txt").getFile();
       String fileName = classLoader.getResource("embeddings/weights.txt").getFile();
       Log.info("Loading weights from " + fileName);
       elementMap = new HashMap<>();

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
           Log.info(String.format("Reading embedding mapping file. {%s}", Calendar.getInstance().getTime().toString()));
           CSVReader reader = new CSVReader(new FileReader(fileName), dataSeparator, CSVParser.NULL_CHARACTER, 1);
           // List<String[]> vectors = reader.readAll();
           Log.info(String.format("Done reading embedding mapping file. {%s}",  Calendar.getInstance().getTime().toString()));

           String[] components;
           int counter=0;
           while((components = reader.readNext()) != null){
               Log.debug(String.format("Read csv entry # %d: %s",  counter, Arrays.toString(components)));
               counter++;
               if (components.length != dimension + 1)
                   throw new IOException(String.format("Mismatch in embedding vector #%d length : %d.",
                           counter, components.length));
               Double [] value = new Double[dimension];
               for (int i=1; i<=dimension; ++i){
                   value[i-1] = Double.parseDouble(components[i]);
               }
               elementMap.put(components[0], value);
           }
           Log.info(String.format("Done processing %d-line embedding mapping. {%s}",  counter, Calendar.getInstance().getTime().toString()));
       } catch (FileNotFoundException e) {
           e.printStackTrace();
           Log.error("No resource file found:" + fileName);
           System.exit(-1);
       } catch (IOException e) {
           e.printStackTrace();
           Log.error("IO exception when reading:" + fileName);
           System.exit(-1);
       }

       unkownVector = getZeroVector();
       weightsLoaded = true;
   }

    /**
     * Zero vector fetcher
     */
   protected Double[] getZeroVector(){
        Double [] vector = new Double[dimension];
        for(int i=0;i<dimension;++i) vector[i] = 0.0d;
        return vector;
   }

    /**
     * Normalizes to the number of words in the text collection (as part of the arithmetic mean aggregation)
     */
    @Override
    public void finalizeModel() {
        if (numElements > 0) {
            // produce average
            for (int i = 0; i < dimension; ++i)
                aggregateVector[i] /= numElements;
        }
        Log.debug(String.format("Finalizing embedding with vectors of %d words.", numElements));
    }


    /**
     * Add an element vector to the entity collection
     * Already implemented here as arithmetic mean, hence adding.
     *
     * @param vector : the element vector
     */

    void addVector(Double[] vector){
        for(int i=0;i<dimension;++i)
            aggregateVector[i] += vector[i];
        numElements++;
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

}
