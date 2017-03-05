package x.mvmn.util.lang;

public class ArraysHelper {

	public static String join(String[] values, int start, int end, String separator) {
		StringBuilder result = new StringBuilder();

		for (int i = start; i <= end; i++) {
			if (i > start) {
				result.append(separator);
			}
			result.append(values[i]);
		}

		return result.toString();
	}
}
