package de.hs.osnabrueck.tenbeitel.mr.twittergraph.reducer;

import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

import org.apache.commons.io.IOUtils;
import org.apache.hadoop.fs.FSDataInputStream;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.apache.hadoop.io.NullWritable;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import de.hs.osnabrueck.tenbeitel.mr.graph.utils.GraphUtils;
import de.hs.osnabrueck.tenbeitel.mr.twittergraph.io.TwitterWritable;
import de.hs.osnabrueck.tenbeitel.twitter.graph.constants.TwitterWritableConstants;
import de.hs.osnabrueck.tenbeitel.twitter.graph.model.TwitterVertex;

public class TwitterClusterJoinReducer extends Reducer<Text, TwitterWritable, NullWritable, Text> {

	DefaultDirectedGraph<String, DefaultEdge> idGraph;
	DefaultDirectedGraph<TwitterVertex, DefaultEdge> twitterGraph;

	Map<String, TwitterVertex> vertexMap = new HashMap<String, TwitterVertex>();

	@Override
	protected void setup(Reducer<Text, TwitterWritable, NullWritable, Text>.Context context)
			throws IOException, InterruptedException {
		super.setup(context);
		this.idGraph = readGraphFromCachedFile(context);
		System.out.println(idGraph.vertexSet());
		twitterGraph = new DefaultDirectedGraph<TwitterVertex, DefaultEdge>(DefaultEdge.class);
	}

	@Override
	protected void reduce(Text key, Iterable<TwitterWritable> values, Context context)
			throws IOException, InterruptedException {
		System.out.println(key + " - " + values.toString());
		if (idGraph.containsVertex(key.toString())) {
			Iterator<TwitterWritable> it = values.iterator();
			TwitterWritable first = new TwitterWritable(it.next());
			System.out.println(first);
			while (it.hasNext()) {
				TwitterWritable second = new TwitterWritable(it.next());
				System.out.println(second);
				System.out.println();
				if (first.getTwitterId().toString().equalsIgnoreCase(second.getTwitterId().toString())) {
					TwitterVertex mergeResult = mergeTwitterWritablesToVertex(first, second);

					System.out.println("Added vertex:" + mergeResult);
					vertexMap.put(mergeResult.getTwitterId(), mergeResult);
				}
			}
		}
	}

	@Override
	protected void cleanup(Reducer<Text, TwitterWritable, NullWritable, Text>.Context context)
			throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		super.cleanup(context);
		Set<DefaultEdge> edgeSet = idGraph.edgeSet();
		Set<String> vertexSet = idGraph.vertexSet();
		for (String id : vertexSet) {
			if (!vertexMap.containsKey(id)) {
				vertexMap.put(id, TwitterVertex.createEmptyVertex(id));
			}
		}
		for (DefaultEdge edge : edgeSet) {
			String sourceId = this.idGraph.getEdgeSource(edge);
			String targetId = this.idGraph.getEdgeTarget(edge);

			TwitterVertex tempSource = vertexMap.get(sourceId);
			TwitterVertex tempTarget = vertexMap.get(targetId);

			// TwitterVertex tempSource =
			// TwitterVertex.createEmptyVertex(sourceId);

			// TwitterVertex tempTarget =
			// TwitterVertex.createEmptyVertex(targetId);

			if (!this.twitterGraph.containsVertex(tempSource)) {
				this.twitterGraph.addVertex(tempSource);
			}
			if (!this.twitterGraph.containsVertex(tempTarget)) {
				this.twitterGraph.addVertex(tempTarget);
			}
			this.twitterGraph.addEdge(tempSource, tempTarget);
		}

		context.write(NullWritable.get(), new Text(GraphUtils.getStringRepresantationFromTwitterGraph(twitterGraph)));

	}

	private DefaultDirectedGraph<String, DefaultEdge> readGraphFromCachedFile(Context context) throws IOException {
		FileSystem fs = FileSystem.get(context.getConfiguration());

		URI[] cachedFiles = context.getCacheFiles();
		if (cachedFiles != null && cachedFiles.length > 0) {
			FSDataInputStream inputStream = fs.open(new Path(cachedFiles[0]));

			StringWriter writer = new StringWriter();
			IOUtils.copy(inputStream, writer, Charset.forName("UTF-8"));
			String graphString = writer.toString();
			// String graphString = FileUtils.readFileToString(graphFile);

			return idGraph = GraphUtils.getGraphFromString(graphString);
		} else {
			return null;
		}
	}

	private TwitterVertex mergeTwitterWritablesToVertex(TwitterWritable first, TwitterWritable second) {
		TwitterVertex vertex = new TwitterVertex();
		if (first.getClusterId().toString().equalsIgnoreCase(TwitterWritableConstants.NOT_AVAILABLE)) {
			vertex.setClusterId(second.getClusterId().toString());
			vertex.setTwitterId(first.getTwitterId().toString());
			vertex.setCreatedDate(first.getCreatedDate().toString());
			vertex.setTweetMessage(first.getTweetMessage().toString());
			vertex.setUserId(first.getUserId().toString());
			vertex.setUserScreenName(first.getUserScreenName().toString());
		}
		if (second.getClusterId().toString().equalsIgnoreCase(TwitterWritableConstants.NOT_AVAILABLE)) {
			vertex.setClusterId(first.getClusterId().toString());
			vertex.setTwitterId(second.getTwitterId().toString());
			vertex.setCreatedDate(second.getCreatedDate().toString());
			vertex.setTweetMessage(second.getTweetMessage().toString());
			vertex.setUserId(second.getUserId().toString());
			vertex.setUserScreenName(second.getUserScreenName().toString());
		}

		return vertex;
	}
}
