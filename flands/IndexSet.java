package flands;

/**
 * Set of matches to some filter, possibly with reasons for exclusion if a
 * match has failed. Used with ItemFilterNode, to handle item caches that can
 * only take certain item categories.
 * @author Jonathan Mann
 */
public class IndexSet {
	private boolean[] indices;
	private String[] reasons;
	
	public IndexSet() {
		this(14);
	}
	public IndexSet(int capacity) {
		indices = new boolean[capacity];
	}
	
	public IndexSet(int indices[]) {
		this(Math.max(14, indices[indices.length-1]+1));
		add(indices);
	}
	
	public String getExcludedReason(int index) {
		if (reasons == null || reasons.length <= index)
			return null;
		return reasons[index];
	}
	public void setExcludedReason(int index, String reason) {
		if (reasons == null)
			reasons = new String[index+1];
		else if (reasons.length <= index) {
			String[] temp = reasons;
			reasons = new String[Math.min(reasons.length * 2, index+1)];
			System.arraycopy(temp, 0, reasons, 0, temp.length);
		}
		
		if (reasons[index] == null)
			// Don't replace first reason for exclusion
			reasons[index] = reason;
	}
	
	public boolean contains(int index) {
		return (indices.length <= index || indices[index]);
	}
	public void add(int index) {
		if (indices.length <= index) {
			boolean[] temp = indices;
			indices = new boolean[Math.min(indices.length * 2, index+1)];
			System.arraycopy(temp, 0, indices, 0, temp.length);
		}
		
		indices[index] = true;
	}
	public void remove(int index) { remove(index, null); }
	public void remove(int index, String reason) {
		if (indices.length > index)
			indices[index] = false;
		if (reason != null)
			setExcludedReason(index, reason);
	}
	
	public void add(int[] indices) {
		for (int i = indices.length - 1; i >= 0; i--)
			add(indices[i]);
	}
	/**
	 * Add all indices from 0 to <code>size-1</code>.
	 */
	public void addAll(int size) {
		for (int i = size-1; i >= 0; i--)
			add(i);
	}
	
	public void remove(int[] indices) { remove(indices, null); }
	public void remove(int[] indices, String reason) {
		for (int i = indices.length - 1; i >= 0; i--)
			remove(indices[i], reason);
	}

	public int size() {
		int size = 0;
		for (int i = 0; i < indices.length; i++)
			if (indices[i])
				size++;
		return size;
	}
	public int[] getSelected() {
		int[] result = new int[size()];
		int j = 0;
		for (int i = 0; i < indices.length; i++)
			if (indices[i])
				result[j++] = i;
		return result;
	}
}
