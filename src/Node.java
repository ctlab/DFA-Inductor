import java.util.HashMap;
import java.util.Map;

public class Node {

	enum Status {
		ACCEPTABLE, COMMON, REJECTABLE
	}

	private int number;
	private Map<String, Node> children;
	private Map<String, Node> parents;
	private Status status;

	public Node(int number) {
		this.number = number;
		this.children = new HashMap<>();
		this.parents = new HashMap<>();
		this.status = Status.COMMON;
	}

	public Node(int number, Status status) {
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
		this.status = Status.COMMON;
	}

	public Node(int number, String label, Node father, Status status) {
		this.number = number;
		this.children = new HashMap<>();
		this.parents = new HashMap<>();
		this.parents.put(label, father);
		this.status = status;
	}

	public int getNumber() {
		return number;
	}

	public Status getStatus() {
		return status;
	}

	public boolean isAcceptable() {
		return status == Status.ACCEPTABLE;
	}

	public boolean isRejectable() {
		return status == Status.REJECTABLE;
	}

	public void setStatus(Status status) {
		this.status = status;
	}

	public Map<String, Node> getParents() {
		return parents;
	}

	public Node getParent(String s) {
		return parents.containsKey(s) ? parents.get(s) : null;
	}

	public Map<String, Node> getChildren() {
		return children;
	}
	
	public Node getChild(String s) {
		return children.containsKey(s) ? children.get(s) : null;
	}

	public void addChild(String s, Node child) {
		children.put(s, child);
	}

	public void addParent(String s, Node child) {
		parents.put(s, child);
	}
}
