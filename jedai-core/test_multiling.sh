mvn clean package || exit 1


# loop on the sim. threshold

for thresh in $(seq 0 0.05 1 | sed 's/,/./g'); do
	sed  -i "s/clustering_threshold.*/clustering_threshold = $thresh /g"  config.txt
	echo "Running experiment with sim.thresh at $thresh ..."
	./execute.sh | tail -1  >> res.txt
	tail res.txt
done

python3 parse_res.py res.txt
