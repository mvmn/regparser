package x.mvmn.regparser;

public interface RegistryEntry {

	public String getPath();

	public String[] getPathElements();

	public String getKey();

	public String getValueStr();
}
