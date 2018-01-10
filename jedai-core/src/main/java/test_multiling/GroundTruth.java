package test_multiling;

import DataModel.EntityProfile;

import java.util.*;

public class GroundTruth {
    List<EntityProfile> topics;
    List<EntityProfile> refs;
    List<EntityProfile> sums;
    HashMap<List<EntityProfile>, List<EntityProfile>> clusters;

    List<String> sumtype;
    List<EntityProfile> aggregate;

    void make_aggregate(){
        List<EntityProfile> llist = new ArrayList<>();
        sumtype = new ArrayList<>();
        for(EntityProfile p : refs) { sumtype.add("ref"); llist.add(p); }
        for(EntityProfile p : sums) { sumtype.add("sum"); llist.add(p); }
        aggregate = llist;
    }
    List<EntityProfile> get_aggregate(){
        if (this.aggregate.isEmpty()) return this.aggregate;
        make_aggregate();
        return aggregate;
    }
    EntityProfile handle_entityprofile(EntityProfile prof){
        if(this.erData) return prof;
        EntityProfile pp = new EntityProfile(prof.getEntityUrl());
        String summ = test_multiling.getEntityValue(prof, "summary");
        pp.addAttribute("summary", summ);
        return pp;
    }

    public List<EntityProfile> get_sums() {
        // keep only summary texts
        List<EntityProfile> s = new ArrayList<>();
        for(EntityProfile p : sums) s.add(handle_entityprofile(p));
        return s;
    }
    public List<EntityProfile> get_refs() {
        // keep only summary texts
        List<EntityProfile> s = new ArrayList<>();
        for(EntityProfile p : refs) s.add(handle_entityprofile(p));
        return s;
    }

    boolean erData;
    boolean do_dirty;
    HashMap<String, Pair<ArrayList<String>, ArrayList<String>>> gt ;

    public GroundTruth(List<EntityProfile> top, boolean erData, boolean do_dirty){
        this.topics = top;
        this.refs = new ArrayList<>();
        this.sums = new ArrayList<>();
        this.clusters = new HashMap<>();
        this.gt = new HashMap<>();
        this.erData = erData;
        this.aggregate = new ArrayList<>();
        this.do_dirty = do_dirty;
    }
    public String getName(EntityProfile top){
        return test_multiling.getEntityValue(top, "topic_name");
    }

    public void add_ref(EntityProfile ref){
        String topic_id = test_multiling.getEntityValue(ref, "topic_id");
        if(!topic_valid(topic_id)) return;
        refs.add(ref);
    }
    boolean topic_valid(String topic_id){
        for(EntityProfile p : topics){
            if(p.getEntityUrl().equals(topic_id)) return true;
        }
        return false;
    }
    public void add_sum(EntityProfile sum){
        String topic_id = test_multiling.getEntityValue(sum, "topic_id");
        if(!topic_valid(topic_id)) {
            return;
        }
        sums.add(sum);
        if(! gt.keySet().contains(topic_id)){
            init_topic_gt(topic_id);
        }
        gt.get(topic_id).f2.add(sum.getEntityUrl());
    }
    public void init_topic_gt(String topic){
        Pair<ArrayList<String>, ArrayList<String>> h = new Pair<ArrayList<String>, ArrayList<String>>(new ArrayList<>(), new ArrayList<>());
        gt.put(topic, h);
    }

    public void add_cluster(List<Integer> ids1, List<Integer> ids2){
        if (ids2.isEmpty())
            clusters.put(refids_to_profiles(ids1), new ArrayList<>());
        else
            clusters.put(refids_to_profiles(ids1), sumids_to_profiles(ids2));
    }

    void print_refs(){
        for(EntityProfile p : refs) print_ent(p);
    }
    void print_sums(){
        for(EntityProfile p : sums) print_ent(p);
    }
    void print_ent(EntityProfile p){
        System.out.print("ent-url: " + p.getEntityUrl() +
                " topic-name: " + test_multiling.getEntityValue(p,"topic_name") +
                " topic-id: " + test_multiling.getEntityValue(p,"topic_id"));
        if(!erData) System.out.println(" {" + test_multiling.getEntityValue(p, "summary").substring(0,100).replaceAll("\\n"," ") + " ...}");
        else System.out.println();

    }
    void print_aggregate(){
        for(EntityProfile p : aggregate) print_ent(p);
    }

    public ArrayList<Double> evaluate_dirty() {
        double total_precision = 0;
        double total_recall = 0;
        int cluster_count = 0;
        System.out.println("\n\n==================================\nEvaluating dirty clustering.");
        for(List<EntityProfile> refsums : clusters.keySet()) {

            int num_truePos = 0;
            int num_falsePos = 0;

            String false_members="";
            ArrayList<Pair<String,Integer>> clustertopics = topics_in_entlist(refsums);


            // measure total cluster prec/rec/F-Measure and per group (i.e. ref and sums)
            // wrt to the maj. voted topic in the cluster
            String cluster_reftopic = clustertopics.get(0).f1;
            int cluster_reftopic_num = clustertopics.get(0).f2;

            num_truePos += cluster_reftopic_num;
            for(int i=1;i<clustertopics.size();++i){
                Pair<String,Integer> ppair = clustertopics.get(i);
                String topic = ppair.f1;
                int ntopic = ppair.f2;
                if(topic.equals(cluster_reftopic)){
                    num_truePos += ntopic;
                }
                else{
                    num_falsePos += ntopic;
                    false_members += topic + " ";
                }
            }

            // get total positives for the cluster to compute recall
            ArrayList<EntityProfile> actual_topic_members = get_total_topic_members(cluster_reftopic, aggregate);

            double precision = (1.0 * num_truePos) / (num_truePos + num_falsePos);
            double recall = (1.0 * num_truePos) / actual_topic_members.size();
            total_precision += precision;
            total_recall += recall;

            System.out.print("\nCluster " + ++cluster_count + "/" +clusters.size() + " topic: [" + cluster_reftopic + "] from " + cluster_reftopic_num + " members. Hist. of all are:");
            for(Pair<String, Integer> p : clustertopics) System.out.print("("+p.f1 + ", " + p.f2+") ");
            System.out.print("\n\tcluster members ent.urls/topics: { ");
            for(EntityProfile r : refsums){ System.out.print(r.short_str() + " ");} System.out.println("}");
            System.out.print("\ttrue members of topic " + cluster_reftopic + " [num=" + actual_topic_members.size() + "] are :{");
            for(EntityProfile r : actual_topic_members){ System.out.print(r.short_str() + " ");} System.out.println("}");

            System.out.println(String.format("\tPrecision: %f", precision));

            System.out.println(String.format("\tRecall: %f ",recall));
            if(! false_members.isEmpty())
                System.out.println("\tfalse members {" + false_members + "}");
        }

        if(!clusters.isEmpty()) {
            total_precision /= clusters.keySet().size();
            total_recall /= clusters.keySet().size();
        }
        else
            System.out.println("No clusters!");

        System.out.println(String.format("\nTotal Precision: %f", total_precision));
        System.out.println(String.format("Total Recall: %f", total_recall));
        ArrayList<Double> res = new ArrayList<>();
        res.add(total_precision); res.add(total_recall);
        return res;
    }

    public ArrayList<Double> evaluate(boolean doDirty){
        if(doDirty) return evaluate_dirty();

        //print_refs();
        //print_sums();
        System.out.println("\n\n==================================\nEvaluating clustering.");
        double total_ref_prec = 0;
        double total_ref_rec = 0;
        double total_sum_prec = 0;
        double total_sum_rec = 0;
        int cluster_count = 0;
        for(List<EntityProfile> refsums : clusters.keySet()) {

            int num_ref_truePos = 0;
            int num_ref_falsePos = 0;
            int num_sum_truePos = 0;
            int num_sum_falsePos = 0;

            String false_refs="";
            String false_sums="";
            List<EntityProfile> sumsums = clusters.get(refsums);

            ArrayList<Pair<String,Integer>> reftopics = topics_in_entlist(refsums);
            ArrayList<Pair<String,Integer>> sumtopics = topics_in_entlist(sumsums);
            ArrayList<String> descs = new ArrayList<>();

            // measure total cluster prec/rec/F-Measure and per group (i.e. ref and sums)
            // wrt to the maj. voted topic in the cluster
            String cluster_reftopic = reftopics.get(0).f1;
            int cluster_reftopic_num = reftopics.get(0).f2;
            num_ref_truePos += cluster_reftopic_num;
            for(int i=1;i<reftopics.size();++i){
                Pair<String,Integer> ppair = reftopics.get(i);
                String topic = ppair.f1;
                int ntopic = ppair.f2;
                if(topic.equals(cluster_reftopic)){
                   num_ref_truePos += ntopic;
                }
                else{
                    num_ref_falsePos += ntopic;
                    false_refs += topic + " ";
                }
            }
            for(int i=0;i<sumtopics.size();++i){
                Pair<String,Integer> ppair = sumtopics.get(i);
                String topic = ppair.f1;
                int ntopic = ppair.f2;
                if(topic.equals(cluster_reftopic)){
                    num_sum_truePos += ntopic;
                }
                else{
                    num_sum_falsePos += ntopic;
                    false_sums += topic + " ";
                }
            }
            // get total positives for the cluster to compute recall
            ArrayList<EntityProfile> actual_ref_topic_members = get_total_topic_members(cluster_reftopic, refs);
            ArrayList<EntityProfile> actual_sum_topic_members = get_total_topic_members(cluster_reftopic, sums);
            double ref_precision = (1.0 * num_ref_truePos) / (num_ref_truePos + num_ref_falsePos);
            double ref_recall = (1.0 * num_ref_truePos) / actual_ref_topic_members.size();
            total_ref_prec += ref_precision;
            total_ref_rec += ref_recall;
            double sum_precision = (1.0 * num_sum_truePos) / (num_sum_truePos + num_sum_falsePos);
            double sum_recall = (1.0 * num_sum_truePos) / actual_sum_topic_members.size();
            total_sum_prec += sum_precision;
            total_sum_rec += sum_recall;

            double precision = (ref_precision + sum_precision) /2.0;
            double recall = (ref_recall + sum_recall) /2.0;

            System.out.print("\nCluster " + ++cluster_count + "/" + clusters.size() + " topic: [" + cluster_reftopic
                    + "] from " + cluster_reftopic_num + " members. Hist. of all are:");
            for(Pair<String, Integer> p : reftopics) System.out.print("("+p.f1 + ", " + p.f2+") ");

            System.out.print("\n\trefs: { ");
            for(EntityProfile r : refsums){ System.out.print(r.short_str() + " ");} System.out.println("}");
            System.out.print("\tsums: { ");
            for(EntityProfile s : sumsums){ System.out.print(s.short_str() + " ");} System.out.println("}");

            System.out.println(String.format("\tPrecision (ref/sum/total): %f %f %f", ref_precision, sum_precision, precision));
            System.out.println(String.format("\tRecall (ref/sum/total): %f %f %f", ref_recall,sum_recall, recall));
            if(! false_refs.isEmpty())
                System.out.println("\tfalse refs " + false_refs);
            if(! false_sums.isEmpty())
                System.out.println("\tfalse sums " + false_sums);
        }
        double total_precision = 0 ;
        double total_recall = 0 ;
        if(!clusters.isEmpty()) {
            total_ref_prec /= clusters.keySet().size();
            total_ref_rec /= clusters.keySet().size();
            total_sum_prec /= clusters.keySet().size();
            total_sum_rec /= clusters.keySet().size();
            total_precision = (total_ref_prec + total_sum_prec) / 2.0;
            total_recall = (total_ref_rec + total_sum_rec) / (2.0 * clusters.keySet().size());
        }
        System.out.println();
        System.out.println(String.format("Total Precision (ref/sum/total): %f %f %f", total_ref_prec, total_sum_prec, total_precision));
        System.out.println(String.format("Total Recall (ref/sum/total): %f %f %f", total_ref_rec, total_sum_rec, total_recall));
        ArrayList<Double> res = new ArrayList<>();
        res.add(total_precision); res.add(total_recall);
        res.add(total_ref_prec); res.add(total_sum_prec);
        res.add(total_ref_rec); res.add(total_ref_rec);
        return res;
    }

    public ArrayList<EntityProfile> get_total_topic_members(String topic_id, List<EntityProfile> eplist){
        ArrayList<EntityProfile> res = new ArrayList<>();
        for(EntityProfile r : eplist){
           if(test_multiling.getEntityValue(r, "topic_id").equals(topic_id)) res.add(r);
        }
        return res;
    }

    public List<EntityProfile> sumids_to_profiles(List<Integer> ids){
        List<EntityProfile> plist = new ArrayList<>();
        for(int id : ids){
            plist.add(sums.get(id));
        }
        return plist;
    }
    
    public List<EntityProfile> refids_to_profiles(List<Integer> ids){
        List<EntityProfile> plist = new ArrayList<>();
        for(int id : ids){
            if(do_dirty)
                plist.add(aggregate.get(id));
            else
                plist.add(refs.get(id));
        }
        return plist;
    }

    ArrayList<Pair<String, Integer>> topics_in_entlist(List<EntityProfile> proflist){
        ArrayList<Pair<String, Integer>> llist = new ArrayList<>();
        for(EntityProfile p : proflist){
            String topic_id = test_multiling.getEntityValue(p, "topic_id");
            boolean found = false;
            for(Pair<String,Integer> ppair : llist){
                if (ppair.f1.equals(topic_id)){
                    ppair.f2 ++;
                    found = true;
                    break;
                }
            }
            if (found) continue;
            Pair<String,Integer> top = new Pair<>(topic_id, 1);
            llist.add(top);
        }
        // sort the list
        Collections.sort(llist, new Comparator<Pair<String, Integer>>() {
            @Override
            public int compare(Pair<String, Integer> pair1, Pair<String, Integer> pair2) {
                return pair1.f2.compareTo(pair2.f2);
            }
        });
        Collections.reverse(llist);
        return llist;
    }

}
