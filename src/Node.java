import java.util.HashMap;
import java.util.Map;

public class Node {

	static final int ACCEPTABLE = 1;
	static final int COMMON = 0;
	static final int REJECTABLE = -1;

	private int number;
	private Map<String, Node> children;
	private Map<String, Node> parents;
	private int status;

	public Node(int number) {
		this.number = number;
		this.children = new HashMap<>();
		this.parents = new HashMap<>();
		this.status = COMMON;
	}

	public Node(int number, int status) {
		this.number = number;
		this.children = new HashMap<>();
		this.parents = new HashMap<>();
		this.status = status;
	}

	public Node(int number, String label, Node father) {
		this.number = number;
		this.children = new HashMap<>();
		this.parents = new HashMap<>();
		this.parents.put(label, father);
		this.status = COMMON;
	}

	public Node(int number, String label, Node father, int status) {
		this.number = number;
		this.children = new HashMap<>();
		this.parents = new HashMap<>();
		this.parents.put(label, father);
		this.status = status;
	}

	public int getNumber() {
		return number;
	}

	public int getStatus() {
		return status;
	}

	public boolean isAcceptable() {
		return status == ACCEPTABLE;
	}

	public boolean isRejectable() {
		return status == REJECTABLE;
	}

	public void setStatus(int status) {
		this.status = status;
	}

	public Map<String, Node> getParents() {
		return parents;
	}

	public Node getParent(String s) {
		if (parents.containsKey(s)) {
			return parents.get(s);
		} else {
			return null;
		}
	}

	public Map<String, Node> getChildren() {
		return children;
	}
	
	public Node getChild(String s) {
		return children.get(s);
	}

	public void addChild(String s, Node child) {
		children.put(s, child);
	}

	public void addParent(String s, Node child) {
		parents.put(s, child);
	}
}
