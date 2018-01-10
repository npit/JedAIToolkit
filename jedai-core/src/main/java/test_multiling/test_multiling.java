package test_multiling;

import BlockBuilding.StandardBlocking;
import BlockProcessing.ComparisonRefinement.ComparisonPropagation;
import DataModel.*;
import DataReader.EntityReader.EntityDBReader;
import EntityClustering.AbstractEntityClustering;
import EntityClustering.CenterClustering;
import EntityClustering.ConnectedComponentsClustering;
import EntityMatching.AbstractEntityMatching;
import EntityMatching.GroupLinkage;
import EntityMatching.ProfileMatcher;
import Utilities.Enumerations.RepresentationModel;
import Utilities.Enumerations.SimilarityMetric;
import com.google.gson.Gson;
import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.stream.JsonReader;

import java.io.*;
import java.util.*;

public class test_multiling {
    public static String getEntityValue(EntityProfile p, String name){
        for (Attribute at : p.getAttributes()){
            if (at.getName().equals(name)) return at.getValue();
        }
        return null;
    }
    private static String get_topic_name(String topic_id, List<EntityProfile> topics){
        for(EntityProfile top : topics) {
            if (top.getEntityUrl().equals(topic_id)) {
                for (Attribute at : top.getAttributes()) {
                    if (at.getName().equals("topic_name")) {
                        return at.getValue();
                    }
                }
            }
        }
       return "";
    }

    private static void read_mss(List<EntityProfile> topics, List<EntityProfile> refs, List<EntityProfile> sums){
        System.out.println("Reading MSS data (reference and participant summaries) from a mysql db");
        // read topics for the current lang
        EntityDBReader eReader_top = new EntityDBReader("mysql://localhost:3306/multiling2017_mss");
        eReader_top.setPassword("password");
        eReader_top.setUser("root");
        eReader_top.setTable("topic");
        List<EntityProfile> rres = eReader_top.getEntityProfiles();
        if (rres == null) return;
        topics.addAll(rres);


        // read reference summaries
        List<EntityProfile> refs_raw;
        EntityDBReader eReader_ref = new EntityDBReader("mysql://localhost:3306/multiling2017_mss");
        eReader_ref.setPassword("password");
        eReader_ref.setUser("root");
        eReader_ref.setTable("ref_summaries");
        refs_raw = eReader_ref.getEntityProfiles();
        for(EntityProfile r : refs_raw){
            // since raw refs are assigned topic id
            EntityProfile prof = new EntityProfile("ref_" + refs.size());
            String topic_id = r.getEntityUrl();
            String summ = getEntityValue(r, "ref_summary");
            String topic_name = get_topic_name(topic_id, topics);
            prof.addAttribute("topic_id", topic_id);
            prof.addAttribute("topic_name", topic_name);
            prof.addAttribute("summary", summ);
            prof.addAttribute("topic_name", get_topic_name(topic_id, topics));
            refs.add(prof);
        }

        // read submitted summaries
        EntityDBReader eReader_sums = new EntityDBReader("mysql://localhost:3306/multiling2017_mss");
        eReader_sums.setPassword("password");
        eReader_sums.setUser("root");
        eReader_sums.setTable("p_summary ");
        sums.addAll(eReader_sums.getEntityProfiles());
        for(EntityProfile s : sums){
            // get corr. topic
            String topic_id = getEntityValue(s, "topic_id");
            String topic_name = get_topic_name(topic_id, topics);
            s.addAttribute("topic_name", topic_name);
            s.addAttribute("topic_id", topic_id);
        }
    }

    private static void read_mms(List<EntityProfile> topics, List<EntityProfile> refs, List<EntityProfile> sums, Properties props, ArrayList<String> langs){

        System.out.println("Reading MMS data (reference summaries and source texts) from a mysql db and a local folder");
        HashMap<String,String> langnames = new HashMap<>();
        langnames.put("en","english");
        langnames.put("zh","chinese");
        langnames.put("ar","arabic");
        langnames.put("cs","czech");
        langnames.put("fr","french");
        langnames.put("el","greek");
        langnames.put("he","hebrew");
        langnames.put("hi","hindi");
        langnames.put("ro","romanian");
        langnames.put("es","spanish");

        // read topics
        EntityDBReader eReader_top = new EntityDBReader("mysql://localhost:3306/multiling_mms");
        eReader_top.setPassword("password");
        eReader_top.setUser("root");
        eReader_top.setTable("topic");
        topics.addAll(eReader_top.getEntityProfiles());


        // read reference summaries
        List<EntityProfile> refs_raw;
        EntityDBReader eReader_ref = new EntityDBReader("mysql://localhost:3306/multiling_mms");
        eReader_ref.setPassword("password");
        eReader_ref.setUser("root");
        eReader_ref.setTable("ref_summaries");
        refs_raw = eReader_ref.getEntityProfiles();

        for(EntityProfile r : refs_raw){
            // since raw refs are assigned topic id
            EntityProfile prof = new EntityProfile("ref_" + refs.size());

            String summ = getEntityValue(r, "ref_summary");
            String topic_id = r.getEntityUrl();
            String topic_name =  get_topic_name(topic_id, topics);
            prof.addAttribute("topic_id", topic_id);
            prof.addAttribute("summary", summ);
            prof.addAttribute("topic_name", topic_name);

            prof.addAttribute("topic_id", r.getEntityUrl());
            refs.add(prof);
        }

        String mms_sources_dir = props.getProperty("mms_sources_folder");
        // read text sources from which reference summaries were created
        if (langs.isEmpty()) {
            // use all available languages for sum
            File sf = new File(mms_sources_dir);
            for (File f : sf.listFiles()) {
                if (f.isFile()) continue;
                langs.add(f.getName());
            }
        }

        // read sources per lang
        for (String lang : langs) {
            // get topics for lang
            ArrayList<EntityProfile> topics_for_lang = new ArrayList<>();
            for (EntityProfile p : topics) {
                if (getEntityValue(p, "lang_code").equals(lang)) topics_for_lang.add(p);
            }
            // sort per name
            Collections.sort(topics_for_lang, new Comparator<EntityProfile>() {
                @Override
                public int compare(EntityProfile entityProfile, EntityProfile t1) {
                    String tname = getEntityValue(entityProfile, "topic_name");
                    String tname2 = getEntityValue(t1, "topic_name");
                    return tname.compareTo(tname2);
                }
            });
            File langFolder = new File(mms_sources_dir + langnames.get(lang));
            ArrayList<File> files = new ArrayList<>();
            files.addAll(Arrays.asList(langFolder.listFiles()));
            Collections.sort(files);
            int topic_index = 0;
            int topic_interval = files.size() / topics_for_lang.size();
            int file_counter = 0;
            System.out.println(String.format("Read %d mms source texts", files.size()));
            for (final File fileEntry : files) {
                //System.out.println("Reading source file " + fileEntry.getName());
                try {
                    FileReader fr = new FileReader(fileEntry);
                    BufferedReader br = new BufferedReader(fr);
                    String line;
                    String text = "";
                    while ((line = br.readLine()) != null) {
                        line = line.trim();
                        if (line.isEmpty()) continue;
                        text += line + " ";

                    }
                    if (file_counter++ >= topic_interval) {
                        topic_index++;
                        file_counter = 1;
                    }

                    String topic_name = getEntityValue(topics_for_lang.get(topic_index), "topic_name");
                    String topic_id = topics_for_lang.get(topic_index).getEntityUrl();
                    String source_id = fileEntry.getName().split("\\.")[0];
                    System.out.println(String.format("Assigned sum %s to topic %s : %s", source_id, topic_id, topic_name));
                    EntityProfile ep = new EntityProfile(source_id);
                    ep.addAttribute("summary", text);
                    ep.addAttribute("topic_name", topic_name);
                    ep.addAttribute("topic_id", topic_id);
                    ep.addAttribute("lang", lang);
                    sums.add(ep);
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    return;
                } catch (IOException e) {
                    e.printStackTrace();
                    return;
                } catch (Exception e) {
                    e.printStackTrace();

                }
            }
        }
    }

    private static EntityProfile jsonobj_to_entityprofile(String id, JsonArray ar){
        EntityProfile ep = new EntityProfile(id);
        for(JsonElement el : ar){
            JsonObject ob = (JsonObject) el;
            for(Map.Entry<String, JsonElement> entry : ob.entrySet()){
                ep.addAttribute(entry.getKey(), new Gson().toJson(entry.getValue()));
            }
        }
        return ep;
    }

    private static void read_entity_res(List<EntityProfile> topics, ArrayList<String> langs, List<EntityProfile> refs, List<EntityProfile> sums, Properties props){
        ArrayList<String> topicids = new ArrayList<>();
        String mode = props.getProperty("datamode");

        if (mode.equals("mss")) {
            try {
                File infold = new File(props.getProperty("entities_mss"));
                File[] infiles = infold.listFiles();
                for (File f : infiles) {
                    // read metadata from the filename
                    String filename = f.getName();
                    String[] parts = filename.split("_");
                    String lang = parts[0];
                    String topic_id = parts[1];
                    String topic_name = parts[2];
                    String category = parts[3];
                    String id = parts[4];
                    if (id.endsWith(".json.entities")) id = id.substring(0, id.length() - ".json.entities".length());
                    if (!topicids.contains(topic_id)) {
                        EntityProfile ep = new EntityProfile(topic_id);
                        ep.addAttribute("topic_name", topic_name);
                        ep.addAttribute("lang_code", lang);
                        topicids.add(topic_id);
                        topics.add(ep);
                    }

                    FileReader fr = new FileReader(f);
                    //BufferedReader br = new BufferedReader(fr);
                    //String content = "";
                    //String line;
                    //while((line = br.readLine()) != null) content += line;
                    JsonReader rd = new Gson().newJsonReader(fr);
                    JsonArray ar = new Gson().fromJson(rd, JsonArray.class);
                    EntityProfile ep = jsonobj_to_entityprofile(id, ar);
                    ep.addAttribute("topic_id", topic_id);

                    // add the read entity to the appropriate collection
                    if (category.equals("refs")) {
                        refs.add(ep);
                    } else if (category.equals("sums")) {
                        sums.add(ep);
                    } else {
                        System.err.println("Unknown file category" + category);
                        return;
                    }
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
        }
        else{
            // read MMS entities
            String refs_dir = props.getProperty("entities_mms");
            String sources_dir = props.getProperty("entities_mms_sources");
            try {
                File infold = new File(refs_dir);
                File[] infiles = infold.listFiles();
                System.out.println(infiles.length + " files in " + refs_dir);
                for (File f : infiles) {
                    // read metadata from the filename
                    String filename = f.getName();
                    String[] parts = filename.split("_");
                    String lang = parts[0];
                    String topic_id = parts[1];
                    String topic_name = parts[2];
                    String category = parts[3];
                    String id = parts[4];
                    if (id.endsWith(".json.entities")) id = id.substring(0, id.length() - ".json.entities".length());
                    if (!topicids.contains(topic_id)) {
                        EntityProfile ep = new EntityProfile(topic_id);
                        ep.addAttribute("topic_name", topic_name);
                        ep.addAttribute("lang_code", lang);
                        topicids.add(topic_id);
                        topics.add(ep);
                    }

                    FileReader fr = new FileReader(f);
                    //BufferedReader br = new BufferedReader(fr);
                    //String content = "";
                    //String line;
                    //while((line = br.readLine()) != null) content += line;
                    JsonReader rd = new Gson().newJsonReader(fr);
                    JsonArray ar = new Gson().fromJson(rd, JsonArray.class);
                    EntityProfile ep = jsonobj_to_entityprofile(id, ar);
                    ep.addAttribute("topic_id", topic_id);

                    // add the read entity to the appropriate collection
                    if (!category.equals("refs")) {
                        System.err.println("mms ref entity has invalid category" + category);
                    }
                    refs.add(ep);
                }

                System.out.println("Read " + refs.size() + " refs.");
                // read source entities
                infold = new File(sources_dir);
                int filecounter = 0;
                int topic_counter = 0;
                int topic_interval = 10;
                ArrayList<String> filenames = new ArrayList<>();
                filenames.addAll(Arrays.asList(infold.list()));
                System.out.println(filenames.size() + " files in " + sources_dir);
                Collections.sort(filenames);
                Collections.sort(topics, new Comparator<EntityProfile>() {
                    @Override
                    public int compare(EntityProfile entityProfile, EntityProfile t1) {
                        String tname = getEntityValue(entityProfile, "topic_name");
                        String tname2 = getEntityValue(t1, "topic_name");
                        return tname.compareTo(tname2);
                    }
                });
                for (String name : filenames) {
                    // read metadata from the filename
                    String[] parts = name.split("\\.");
                    String id = parts[0];
                    String lang = parts[1];
                    // match the id to the ref id
                    if (filecounter++ >= topic_interval){ ++ topic_counter; filecounter = 1;}
                    String topic_id = topics.get(topic_counter).getEntityUrl();

                    if (!topicids.contains(topic_id)) {
                        EntityProfile ep = new EntityProfile(topic_id);
                        String topic_name = get_topic_name(topic_id, topics);
                        ep.addAttribute("topic_name", topic_name);
                        ep.addAttribute("lang_code", lang);
                        topicids.add(topic_id);
                        topics.add(ep);
                    }

                    FileReader fr = new FileReader(new File(sources_dir + "/" + name));
                    JsonReader rd = new Gson().newJsonReader(fr);
                    JsonArray ar = new Gson().fromJson(rd, JsonArray.class);
                    EntityProfile ep = jsonobj_to_entityprofile(id, ar);
                    String topic_name = id;
                    System.out.println(topic_id + " - " + topic_name);
                    ep.addAttribute("topic_id", topic_id);
                    ep.addAttribute("topic_name",topic_name);
                    sums.add(ep);
                }
            } catch (Exception ex) {
                ex.printStackTrace();
                return;
            }
            System.out.println("Read " + sums.size() + " sums.");

        }
    }

    public static void main(String[] args){
        double clustering_threshold = 0.1;
        String datamode;
        boolean doDirty;
        boolean doEntityRes;
        int max_topics;

        Properties props = new Properties();
        try {
            props.load(new FileReader("config.txt"));
            clustering_threshold = Double.parseDouble(props.getProperty("clustering_threshold"));
            datamode =  props.getProperty("datamode","mss");
            doDirty = Boolean.parseBoolean(props.getProperty("dirty"));
            doEntityRes = Boolean.parseBoolean(props.getProperty("do_entity_res"));
            max_topics = Integer.parseInt(props.getProperty("max_topics","-1"));
        } catch (IOException e) {
            e.printStackTrace();
            return;
        }

        // print run info
        String msg = "Running ER on " + datamode;
        if(doDirty)
            msg += (" dirty ");
        else
            msg += (" clean ");
        if(doEntityRes)
            msg += (" entityRes ");
        System.out.println(msg);


        // restrict languages
        ArrayList<String> langs = new ArrayList<>();
        langs.add("en");

        // topic_id, topic_name --> <ref1, ref2,...><sum1,sum2,...>
        List<EntityProfile> sums_raw =new ArrayList<>();
        List<EntityProfile> refs_raw =new ArrayList<>();
        List<EntityProfile> topics =new ArrayList<>();
        GroundTruth ground_truth = null;
        boolean do_mms = datamode.equals("mms");

        if(! doEntityRes) {
            // run without entity recognition
            if (do_mms) {
                read_mms(topics, refs_raw, sums_raw, props, langs);
            } else {
                // for mss, read participant and reference summaries
                read_mss(topics, refs_raw, sums_raw);
            }
        }
        else {
            // for entity recognition run, read the recognized entities
            String source_folder = props.getProperty("entity_source_folder");
            read_entity_res(topics, langs, refs_raw, sums_raw, props);
        }

        if(refs_raw.isEmpty()) return;
        List<EntityProfile> topics_for_lang=new ArrayList<>();

        System.out.println(String.format("Read %d reference and %d non-reference instances.", refs_raw.size(), sums_raw.size()));


        // filter data
        // ----------------
        if(! langs.isEmpty()){
            System.out.println("Limiting to languages:" + langs);
            for(EntityProfile top : topics) {
                if (langs.contains(getEntityValue(top, "lang_code"))) topics_for_lang.add(top);
            }
        }
        if(max_topics > 0){
            System.out.println("Limiting to " + max_topics + " topics.");
            while(topics_for_lang.size() > max_topics) topics_for_lang.remove(0);
        }
        ground_truth = new GroundTruth(topics_for_lang, doEntityRes, doDirty);

        // restrict to topics
        for(EntityProfile p : refs_raw){
            ground_truth.add_ref(p);
        }
        for(EntityProfile p : sums_raw){
            ground_truth.add_sum(p);
        }

        //ground_truth.print_refs();
        //ground_truth.print_sums();
        // end of filter data

        // extract similarities
        StandardBlocking bl = new StandardBlocking();
        AbstractEntityMatching pm = null;
        SimilarityPairs sp;
        if (doDirty){
            ground_truth.make_aggregate();
            List<EntityProfile> aggregate = ground_truth.get_aggregate();
            System.out.println("Clustering a total of " + aggregate.size() + " aggregate summaries");
            List<AbstractBlock> blocks =  bl.getBlocks(aggregate);
            refine_blocks(blocks);
            pm = getMatcher(props);
            sp = pm.executeComparisons(blocks, aggregate);
            ground_truth.print_aggregate();
        }
        else{
            System.out.println("Clustering " + ground_truth.get_refs().size() + " refs and " + ground_truth.get_sums().size() + " non-ref sums.");
            List<AbstractBlock> blocks =  bl.getBlocks(ground_truth.get_refs(), ground_truth.get_sums());
            refine_blocks(blocks);
            pm = getMatcher(props);
            sp = pm.executeComparisons(blocks, ground_truth.get_refs(), ground_truth.get_sums());
            ground_truth.print_refs();
            ground_truth.print_sums();
        }

        double maxSim = -1;
        double minSim = 2;
        for(double sim : sp.getSimilarities()){
            if (sim < minSim) minSim = sim;
            if (sim > maxSim) maxSim = sim;
        }
        System.out.println("max sim:" + maxSim + " - min sim:" + minSim);

        // clustering
        AbstractEntityClustering cl;
        cl = getClusterer(props);
        cl.setSimilarityThreshold(clustering_threshold);
        System.out.println("Clustering with a threshold of " + clustering_threshold);
        List<EquivalenceCluster> clusters_raw = cl.getDuplicates(sp);
        if(clusters_raw.isEmpty()){
            System.out.println("No clusters produced!");
            return;
        }
        System.out.println("Raw size: " + clusters_raw.size() + " clusters");

        int count=1;
        int n_clust = 0;
        List<EquivalenceCluster> clusters= new ArrayList<>();
        if(!doDirty) {
            System.out.println("Processing clusters on non-dirty ER");
            for (EquivalenceCluster c : clusters_raw) {
                System.out.println(count++ + "/" + clusters_raw.size() +
                        " | D1 size:" + c.getEntityIdsD1().size() +
                        " , D2 size:" + c.getEntityIdsD2().size());
                if (c.getEntityIdsD1().isEmpty() || c.getEntityIdsD2().isEmpty()) continue;
                clusters.add(c);
            }
            System.out.println(String.format("Ignoring %d unit clusters: ",  (clusters_raw.size() - clusters.size())));
            System.out.println("Non-unit clusters: " + clusters.size());
            count=1;

            for (EquivalenceCluster c : clusters){
                System.out.println(count++ + "/" + clusters.size() + ":" + c.toString());
                System.out.println("\tE1 - sizes " + c.getEntityIdsD1());
                System.out.println("\tE2 - sizes " + c.getEntityIdsD2());
                ground_truth.add_cluster(c.getEntityIdsD1(), c.getEntityIdsD2());
                n_clust ++;
            }
        }
        else {
            System.out.println("Processing clusters on dirty ER");
            int cluster_count = 0;
            for(EquivalenceCluster c : clusters_raw) {
                cluster_count++;
                if (c.getEntityIdsD1().size() == ground_truth.get_aggregate().size()) {
                    System.out.println("Discarding degenerate all inclusive cluster #" + cluster_count + " / " + clusters_raw.size());
                    continue;
                }
                if (c.getEntityIdsD1().size() == 1) {
                    System.out.println("Discarding degenerate unit cluster #" +cluster_count + " / " + clusters_raw.size());
                    continue;
                }
                ground_truth.add_cluster(c.getEntityIdsD1(), c.getEntityIdsD2());
                n_clust ++;
            }
        }
        ArrayList<Double> res = ground_truth.evaluate(doDirty);
        if (max_topics > 0){
            System.err.println("============\nMax topics is set to " + max_topics);
        }

        // machine readable output
        System.out.println();
        System.out.println("clust. threshold:" + clustering_threshold + " num. clusters " + n_clust);
        if(doDirty) System.out.println("thresh | prec rec | ref prec/rec | sum prec/rec");
        else System.out.println("thresh | prec rec ");
        System.out.println("====================");
        System.out.print("RES: ");
        System.out.print(clustering_threshold + " | ");
        if(n_clust == 0) { System.out.println(); return; }
        count =0;
        for(double r : res) {
            if (count % 2 == 0 && count > 0) System.out.print("| ");
            ++count;
            System.out.print(r + " ");
        }

        System.out.println();
    }
    static void test(){
        List<EntityProfile> aggr = new ArrayList<>();
        EntityProfile p = new EntityProfile("id_" + aggr.size());
        p.addAttribute("sum", "Two years after the seizure of Royal Navy personnel by Iran, two");
        aggr.add(p);
        p = new EntityProfile("id_" + aggr.size());
        p.addAttribute("sum","In March 2007 a British frigate with 15 Navy personnel, includin");
        aggr.add(p);
        p = new EntityProfile("id_" + aggr.size());
        p.addAttribute("sum","Fifteen British Royal Navy personnel was been captured by Irania");
        aggr.add(p);


        for(EntityProfile e : aggr) {
            System.out.println(e);
        }
        StandardBlocking bl = new StandardBlocking();
        List<AbstractBlock> blocks =  bl.getBlocks(aggr);

        ProfileMatcher pm = new ProfileMatcher(RepresentationModel.CHARACTER_FOURGRAM_GRAPHS, SimilarityMetric.GRAPH_VALUE_SIMILARITY);
        SimilarityPairs sp = pm.executeComparisons(blocks, aggr);
        System.out.println(sp.getSimilarities().length + " sim. pairs");
        System.out.print("\nIDs1:");for(int id1 : sp.getEntityIds1()) System.out.print(id1 + " ");
        System.out.print("\nIDs2:");for(int id2 : sp.getEntityIds1()) System.out.print(id2 + " "); System.out.println();

        ConnectedComponentsClustering cl = new ConnectedComponentsClustering();
        cl.setSimilarityThreshold(0.1);
        List<EquivalenceCluster> clusters = cl.getDuplicates(sp);
        System.out.println("Done");
        aggr.get(3);
    }

    static AbstractEntityClustering getClusterer(Properties props){
        String clusterer = props.getProperty("clusterer");
        System.out.println("Using clusterer:" + clusterer);
        double thresh = Double.parseDouble(props.getProperty("clustering_threshold"));
        if(clusterer.equals("center")) return new CenterClustering(thresh);
        if(clusterer.equals("connected")) return new ConnectedComponentsClustering(thresh);
        return null;
    }
    static AbstractEntityMatching getMatcher(Properties props){
        String matcher = props.getProperty("matcher");
        System.out.println("Using entity matcher:" + matcher);
        if(matcher.equals("profilematcher"))
            return new ProfileMatcher(RepresentationModel.CHARACTER_FOURGRAM_GRAPHS, SimilarityMetric.GRAPH_VALUE_SIMILARITY);
        else if(matcher.equals("grouplinkage"))
            return new GroupLinkage();
        return null;
    }
    static void refine_blocks(List<AbstractBlock> blocks){
        ComparisonPropagation cp = new ComparisonPropagation();
        int num_b = blocks.size();
        blocks.addAll(cp.refineBlocks(blocks));
        for(int i=0;i<num_b;++i)blocks.remove(0);
    }
}
