package provenance.model;

import java.util.List;

public class Constructor implements Model {
	private final String methodType = "Constructor";
	private String timestamp;
	private int objHashCode;
	private String objClass;
	private List<? extends Object> params;
	
	public Constructor() {}
	
	public Constructor(String timestamp, int objHashCode, String objClass, List<? extends Object> params) {
		this.timestamp = timestamp;
		this.objHashCode = objHashCode;
		this.objClass = objClass;
		this.params = params;
	}
	
	/* Getters */
	
	public String getMethodType() {
		return methodType;
	}
	
	public String getTimestamp() {
		return timestamp;
	}
	
	public int getObjHashCode() {
		return objHashCode;
	}
	
	public String getObjClass() {
		return objClass;
	}
	
	public List<? extends Object> getParams() {
		return params;
	}
	
	/* Setters */

	public void setTimestamp(String timestamp) {
		this.timestamp = timestamp;
	}

	public void setObjHashCode(int objHashCode) {
		this.objHashCode = objHashCode;
	}

	public void setObjClass(String objClass) {
		this.objClass = objClass;
	}

	public void setParams(List<? extends Object> params) {
		this.params = params;
	}
	
}
