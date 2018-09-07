package closeness.centrality.entity;

public class VertexCentrality implements Comparable<VertexCentrality> {

	private Integer vertex;
	private Double centrality;
	private String id;
	
	public VertexCentrality(Integer vertex, Double centrality) {
		this.vertex = vertex;
		this.centrality = centrality;
	}
	
	public Integer getVertex() {
		return vertex;
	}
	public Double getCentrality() {
		return centrality;
	}
	
	
	public void setId(String id) {
		this.id = id;
	}

	public String getId() {
		return id;
	}

	@Override
	public int compareTo(VertexCentrality other) {
		return this.centrality.compareTo(other.centrality);
	}
	
	
}
