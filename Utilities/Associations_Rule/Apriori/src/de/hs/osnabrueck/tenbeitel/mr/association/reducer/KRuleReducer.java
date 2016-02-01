package de.hs.osnabrueck.tenbeitel.mr.association.reducer;

import java.io.IOException;
import java.net.URI;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.DoubleWritable;
import org.apache.hadoop.io.IntWritable;
import org.apache.hadoop.io.SequenceFile;
import org.apache.hadoop.io.SequenceFile.Reader.Option;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.apache.mahout.common.StringTuple;

import de.hs.osnabrueck.tenbeitel.mr.association.utils.AprioriUtils;

public class KRuleReducer extends Reducer<StringTuple, IntWritable, Text, DoubleWritable> {
	private static Double minConf = 0.65;
	private static HashMap<StringTuple, IntWritable> itemSetMap = new HashMap<StringTuple, IntWritable>();

	private static Text associationRule = new Text();
	private static DoubleWritable confidenceWritable;

	@Override
	protected void reduce(StringTuple key, Iterable<IntWritable> values, Context context)
			throws IOException, InterruptedException {
		List<String> itemSetZ = key.getEntries();
		Integer supportOfItemSet = values.iterator().next().get();

		Set<List<String>> thenParts = new HashSet<List<String>>();
		for (String item : itemSetZ) {
			List<String> zWithOutItem = new ArrayList<String>(itemSetZ);
			zWithOutItem.remove(item);
			Double confidence = minConf * itemSetMap.get(zWithOutItem).get();
			if (supportOfItemSet >= confidence) {
				List<String> temp = new ArrayList<String>();
				temp.add(item);
				thenParts.add(temp);
				associationRule
						.set(new StringTuple(zWithOutItem).toString() + " ==> " + new StringTuple(item).toString());
				confidenceWritable = new DoubleWritable(confidence);
				context.write(associationRule, confidenceWritable);
			}
		}

		for (int i = 2; i < itemSetZ.size() - 1; i++) {
			thenParts = AprioriUtils.generateCandidates(thenParts, i);
			if (thenParts.isEmpty()) {
				break;
			}
			for (List<String> tuple : thenParts) {
				List<String> zWithOutItem = new ArrayList<String>(itemSetZ);
				zWithOutItem.removeAll(tuple);
				Double confidence = minConf * itemSetMap.get(zWithOutItem).get();
				if (supportOfItemSet >= confidence) {
					associationRule.set(
							new StringTuple(zWithOutItem).toString() + " ==> " + new StringTuple(tuple).toString());
					confidenceWritable = new DoubleWritable(confidence);
					context.write(associationRule, confidenceWritable);
				} else {
					thenParts.remove(tuple);
				}
			}
		}

	}

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		super.setup(context);

		minConf = context.getConfiguration().getDouble("apriori.min_confidence", minConf);
		readFrequentItemSets(context.getCacheFiles(), context.getConfiguration());
	}

	private void readFrequentItemSets(URI[] filePaths, Configuration conf) throws IOException {

		for (int i = 0; i < filePaths.length; i++) {

			Option fileOption = SequenceFile.Reader.file(new Path(filePaths[i]));
			try (SequenceFile.Reader frequentItemsReader = new SequenceFile.Reader(conf, fileOption)) {

				StringTuple key = new StringTuple();
				IntWritable value = new IntWritable();

				while (frequentItemsReader.next(key, value)) {
					itemSetMap.put(key, value);
				}
			}

		}
	}

}
