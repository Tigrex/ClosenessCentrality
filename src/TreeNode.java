import java.util.ArrayList;
import java.util.List;

public class TreeNode {
	
	public int id;
	
	public int level;
	
	public List<TreeNode> parents = new ArrayList<TreeNode>();
	
	public List<TreeNode> children = new ArrayList<TreeNode>();
	
	public TreeNode(int id) {
		this.id = id;
	}
	
}
