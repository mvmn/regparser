package x.mvmn.regparser.impl;

import java.util.Arrays;

import x.mvmn.regparser.RegistryEntry;

public class RegistryEntryImpl implements RegistryEntry {

	protected final String path;

	protected final String[] pathElements;

	protected final String key;

	protected final String valueStr;

	public RegistryEntryImpl(String path, String[] pathElements, String key, String valueStr) {
		super();
		this.path = path;
		this.pathElements = pathElements;
		this.key = key;
		this.valueStr = valueStr;
	}

	public String getPath() {
		return path;
	}

	public String[] getPathElements() {
		return pathElements;
	}

	public String getKey() {
		return key;
	}

	public String getValueStr() {
		return valueStr;
	}

	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append("RegistryEntryImpl [path=").append(path).append(", pathElements=").append(Arrays.toString(pathElements)).append(", key=").append(key)
				.append(", valueStr=").append(valueStr).append("]");
		return builder.toString();
	}
}
