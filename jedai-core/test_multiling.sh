echo "$(find . -iname '*.jar' | tr '\n' ':')" > cpath 
echo "$(find ~/.m2/ -iname '*.jar' | tr '\n' ':')" >> cpath 

# loop on the sim. threshold

for thresh in $(seq 0 0.05 1 | sed 's/,/./g'); do
	echo "Running experiment with sim.thresh at $thresh"
	echo "clustering_threshold = $thresh" > config.txt
	java -cp "$(cat cpath)" test_multiling.test_multiling | tail -1  >> res.txt
	tail res.txt
done

python3 parse_res.py res.txt
