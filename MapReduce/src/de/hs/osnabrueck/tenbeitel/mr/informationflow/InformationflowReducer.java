package de.hs.osnabrueck.tenbeitel.mr.informationflow;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.ObjectInputStream;

import org.apache.hadoop.io.Text;
import org.apache.hadoop.mapreduce.Reducer;
import org.jgrapht.Graphs;
import org.jgrapht.graph.DefaultDirectedGraph;
import org.jgrapht.graph.DefaultEdge;

import com.google.gson.Gson;

public class InformationflowReducer extends Reducer<Text, Text, Text, Text> {
	private static final Gson GSON = new Gson();

	private static DefaultDirectedGraph<String, DefaultEdge> graph = new CustomDefaultDirectedGraph<String, DefaultEdge>(
			DefaultEdge.class);

	@Override
	protected void reduce(Text key, Iterable<Text> values, Context context) throws IOException, InterruptedException {
		for (Text graphString : values) {

			try {
				byte b[] = graphString.getBytes();
				ByteArrayInputStream bi = new ByteArrayInputStream(b);
				ObjectInputStream si = new ObjectInputStream(bi);
				Graphs.addGraph(InformationflowReducer.graph,
						(DefaultDirectedGraph<String, DefaultEdge>) si.readObject());
			} catch (Exception e) {
				System.out.println(e);
			}

		}
	}

	@Override
	protected void cleanup(Context context) throws IOException, InterruptedException {
		super.cleanup(context);

		String graphJson = GSON.toJson(graph);
		context.write(context.getCurrentKey(), new Text(graphJson));
	}

}
