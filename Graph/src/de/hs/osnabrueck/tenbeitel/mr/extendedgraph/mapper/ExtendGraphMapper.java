package de.hs.osnabrueck.tenbeitel.mr.extendedgraph.mapper;

import java.io.File;
import java.io.IOException;
import java.net.URI;

import org.apache.commons.io.FileUtils;
import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Mapper;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import de.hs.osnabrueck.tenbeitel.mr.graph.utils.GraphUtils;

public class ExtendGraphMapper extends Mapper<Text, Text, Text, Text> {

	private static DefaultDirectedGraph<String, DefaultEdge> graph;

	@Override
	protected void setup(Context context) throws IOException, InterruptedException {
		// TODO Auto-generated method stub
		super.setup(context);
		ExtendGraphMapper.graph = new DefaultDirectedGraph<String, DefaultEdge>(DefaultEdge.class);
		readGraphFromCachedFile(context);
	}

	@Override
	protected void map(Text key, Text value, Context context) throws IOException, InterruptedException {
		String[] keyParts = key.toString().split("_");
		String similarId = value.toString();
		if (keyParts[1].equalsIgnoreCase("pre")) {
			graph.addVertex(keyParts[0]);
			graph.addVertex(similarId);
			graph.addEdge(similarId, keyParts[0]);
		}
		if (keyParts[1].equalsIgnoreCase("post")) {
			graph.addVertex(similarId);
			graph.addVertex(keyParts[0]);
			graph.addEdge(keyParts[0], similarId);
		}
	}

	@Override
	protected void cleanup(Mapper<Text, Text, Text, Text>.Context context) throws IOException, InterruptedException {
		super.cleanup(context);
		String graphString = GraphUtils.getStringRepresantationFromGraph(graph);
		context.write(new Text("graph"), new Text(graphString));
	}

	private void readGraphFromCachedFile(Context context) throws IOException {
		URI[] cachedFiles = context.getCacheFiles();
		if (cachedFiles != null && cachedFiles.length > 0) {
			File graphFile = new File(cachedFiles[0]);
			String graphString = FileUtils.readFileToString(graphFile);
			graph = GraphUtils.getGraphFromString(graphString);
		}
	}

}
