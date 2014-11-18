package provenance.model;

import java.util.List;

public class Calling implements Model {
	private final String methodType = "Calling";
	private String timestamp;
	private int objHashCode;
	private String objClass;
	private int calledObjHashCode;
	private String calledObjClass;
	private String calledObjMethodName;
	private List<? extends Object> params;
	
	public Calling() {}

	public Calling(String timestamp, int objHashCode, String objClass, int calledObjHashCode, String calledObjClass,
			String calledObjMethodName, List<? extends Object> params) {
		this.timestamp = timestamp;
		this.objHashCode = objHashCode;
		this.objClass = objClass;
		this.calledObjHashCode = calledObjHashCode;
		this.calledObjClass = calledObjClass;
		this.calledObjMethodName = calledObjMethodName;
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

	public int getCalledObjHashCode() {
		return calledObjHashCode;
	}

	public String getCalledObjClass() {
		return calledObjClass;
	}
	
	public String getCalledObjMethodName() {
		return calledObjMethodName;
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

	public void setCalledObjHashCode(int calledObjHashCode) {
		this.calledObjHashCode = calledObjHashCode;
	}

	public void setCalledObjClass(String calledObjClass) {
		this.calledObjClass = calledObjClass;
	}

	public void setCalledObjMethodName(String calledObjMethodName) {
		this.calledObjMethodName = calledObjMethodName;
	}

	public void setParams(List<? extends Object> params) {
		this.params = params;
	}
	
}
