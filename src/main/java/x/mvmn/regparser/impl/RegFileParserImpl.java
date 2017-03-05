package x.mvmn.regparser.impl;

import java.awt.BorderLayout;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;
import java.util.TreeMap;
import java.util.TreeSet;

import javax.swing.JFrame;
import javax.swing.JScrollPane;
import javax.swing.JTree;
import javax.swing.tree.DefaultMutableTreeNode;
import javax.swing.tree.DefaultTreeModel;

import x.mvmn.regparser.RegParser;
import x.mvmn.regparser.RegistryEntry;
import x.mvmn.util.lang.ArraysHelper;
import x.mvmn.util.lang.Tuple;

public class RegFileParserImpl implements RegParser, AutoCloseable {

	protected final BufferedReader reader;
	protected String nextLine;
	protected String currentPath = "";

	public RegFileParserImpl(File regFile, Charset charset) throws IOException {
		this.reader = new BufferedReader(new InputStreamReader(new FileInputStream(regFile), charset));
		this.nextLine = reader.readLine();
		while (this.nextLine != null && !this.nextLine.startsWith("[")) {
			this.nextLine = reader.readLine();
		}
	}

	@Override
	public boolean hasNext() {
		return nextLine != null;
	}

	@Override
	public void remove() {
		throw new UnsupportedOperationException();
	}

	@Override
	public RegistryEntry next() {
		try {
			RegistryEntry result = parseRegEntry();
			// this.nextLine = reader.readLine();
			return result;
		} catch (IOException e) {
			throw new RuntimeException(e);
		}
	}

	private RegistryEntry parseRegEntry() throws IOException {
		RegistryEntry result = null;
		boolean done = false;
		while (!done) {
			String line = getLine();
			if (line == null) {
				done = true;
			} else {
				if (line.startsWith("[")) {
					currentPath = line.substring(1, line.lastIndexOf("]"));
				} else if (line.trim().length() > 0) {
					while (line.endsWith("\\")) {
						line = line.substring(0, line.length() - 1);
						line = line + getLine().trim();
					}
					String key;
					String value;
					int indexOfSeparator;
					if (line.startsWith("\"")) {
						Tuple<String, Boolean, String, Void, Void> processed = readEscapedString(line.substring(1));
						key = processed.getA();
						String reminder = processed.getC();
						if (reminder == null || !reminder.startsWith("=")) {
							throw new RuntimeException("Bad line at " + currentPath + ": " + line);
						}
						value = reminder.substring(1);
					} else {
						indexOfSeparator = line.indexOf("=");
						key = line.substring(0, indexOfSeparator);
						value = line.substring(indexOfSeparator + 1);
					}
					if (value.startsWith("\"")) {
						line = value.substring(1);
						value = "";
						Tuple<String, Boolean, String, Void, Void> valueProcessed;
						do {
							valueProcessed = readEscapedString(line);
							value += valueProcessed.getA();
							if (!valueProcessed.getB()) {
								line = getLine();
							}
						} while (!valueProcessed.getB() && line != null);
						if (!valueProcessed.getB()) {
							throw new RuntimeException("Unfinished string at " + currentPath + "/" + key);
						}
					}
					result = new RegistryEntryImpl(currentPath, currentPath.split("\\\\"), key, value);
					done = true;
				} else {
					done = true;
				}
			}
		}
		return result;
	}

	protected Tuple<String, Boolean, String, Void, Void> readEscapedString(String escapedString) {
		String input = escapedString;
		StringBuilder result = new StringBuilder();
		boolean escaping = false;
		int endIndex = -1;
		for (int i = 0; i < input.length(); i++) {
			char c = input.charAt(i);
			if (!escaping && c == '\\') {
				escaping = true;
			} else {
				if (c == '"' && !escaping) {
					endIndex = i;
					break;
				} else {
					result.append(c);
				}
				escaping = false;
			}
		}

		return new Tuple<String, Boolean, String, Void, Void>(result.toString(), endIndex > -1, endIndex > -1 ? escapedString.substring(endIndex + 1) : null,
				null, null);
	}

	protected String getLine() throws IOException {
		String currentLine = nextLine;
		do {
			nextLine = reader.readLine();
		} while (nextLine != null && nextLine.trim().length() < 1);
		return currentLine;
	}

	public void close() throws IOException {
		reader.close();
	}

	public static void main(String args[]) throws Exception {
		Map<String, RegistryEntry> oldReg = new TreeMap<>();
		{
			RegFileParserImpl parser = new RegFileParserImpl(new File(args[0]), StandardCharsets.UTF_16LE);
			while (parser.hasNext()) {
				RegistryEntry entry = parser.next();
				if (entry != null) {
					String key = entry.getPath() + " :: " + entry.getKey();
					oldReg.put(key, entry);
				} else {
					System.out.println("Null entry");
				}
			}
			parser.close();
		}
		Map<String, RegistryEntry> newReg = new TreeMap<>();
		{
			RegFileParserImpl parser = new RegFileParserImpl(new File(args[1]), StandardCharsets.UTF_16LE);
			while (parser.hasNext()) {
				RegistryEntry entry = parser.next();
				if (entry != null) {
					String key = entry.getPath() + " :: " + entry.getKey();
					RegistryEntry oldEntry = oldReg.get(key);
					if (oldEntry != null && oldEntry.getValueStr().equals(entry.getValueStr())) {
						oldReg.remove(key);
					} else {
						newReg.put(key, entry);
					}
				} else {
					System.out.println("Null entry");
				}
			}
			parser.close();
		}
		TreeSet<String> keys = new TreeSet<>();
		keys.addAll(oldReg.keySet());
		keys.addAll(newReg.keySet());
		DefaultMutableTreeNode rootNode = new DefaultMutableTreeNode("/");
		Map<String, DefaultMutableTreeNode> nodesMap = new HashMap<>();
		for (String key : keys) {
			RegistryEntry oldEntry = oldReg.get(key);
			RegistryEntry newEntry = newReg.get(key);
			RegistryEntry refEntry = oldEntry != null ? oldEntry : newEntry;

			String[] pathElements = refEntry.getPathElements();
			DefaultMutableTreeNode parentNode = rootNode;
			for (int i = 0; i < pathElements.length; i++) {
				String path = ArraysHelper.join(pathElements, 0, i, "/");
				if (nodesMap.get(path) == null) {
					DefaultMutableTreeNode node = new DefaultMutableTreeNode(pathElements[i]);
					parentNode.add(node);
					nodesMap.put(path, node);
					parentNode = node;
				} else {
					parentNode = nodesMap.get(path);
				}
			}
			DefaultMutableTreeNode valsNode = new DefaultMutableTreeNode(refEntry.getKey());
			parentNode.add(valsNode);
			if (oldEntry != null) {
				valsNode.add(new DefaultMutableTreeNode("Old: " + oldEntry.getValueStr(), false));
			}
			if (newEntry != null) {
				valsNode.add(new DefaultMutableTreeNode("New: " + newEntry.getValueStr(), false));
			}
		}
		DefaultTreeModel model = new DefaultTreeModel(rootNode);
		JTree jtree = new JTree(model);
		JFrame frame = new JFrame();
		frame.getContentPane().setLayout(new BorderLayout());
		frame.getContentPane().add(new JScrollPane(jtree), BorderLayout.CENTER);
		frame.pack();
		frame.setVisible(true);
	}
}
