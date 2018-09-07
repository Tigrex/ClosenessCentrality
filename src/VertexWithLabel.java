import java.util.ArrayList;
import java.util.List;

public class VertexWithLabel {

	private int vertex;
	private List<DistanceLabel> labels;
	
	public VertexWithLabel(int vertex, List<DistanceLabel> labels) {
		this.vertex = vertex;
		this.labels = labels;
	}
	
	public VertexWithLabel(int vertex, DistanceLabel label) {
		this.vertex = vertex;
		this.labels = new ArrayList<DistanceLabel>();
		this.labels.add(label);
	}
	
	public int getVertex() {
		return this.vertex;
	}

	public List<DistanceLabel> getLabels() {
		return this.labels;
	}
}
