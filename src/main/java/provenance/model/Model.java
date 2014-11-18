package provenance.model;

public interface Model {
	public String getMethodType();
	public String getTimestamp();
	public int getObjHashCode();
	public String getObjClass();
}
