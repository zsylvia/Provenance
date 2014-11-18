package provenance.model;

import java.util.List;

public class Method implements Model {
	private final String methodType = "Method";
	private String timestamp;
	private int objHashCode;
	private String objClass;
	private String methodName;
	private List<? extends Object> params;
	
	public Method() {}

	public Method(String timestamp, int objHashCode, String objClass, String methodName, List<? extends Object> params) {
		this.timestamp = timestamp;
		this.objHashCode = objHashCode;
		this.objClass = objClass;
		this.methodName = methodName;
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
	
	public String getMethodName() {
		return methodName;
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

	public void setMethodName(String methodName) {
		this.methodName = methodName;
	}

	public void setParams(List<? extends Object> params) {
		this.params = params;
	}

}
