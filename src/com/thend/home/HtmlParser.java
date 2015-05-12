package com.thend.home;
import info.monitorenter.cpdetector.io.ASCIIDetector;
import info.monitorenter.cpdetector.io.CodepageDetectorProxy;
import info.monitorenter.cpdetector.io.JChardetFacade;
import info.monitorenter.cpdetector.io.ParsingDetector;
import info.monitorenter.cpdetector.io.UnicodeDetector;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import org.apache.commons.io.FileUtils;
import org.cyberneko.html.parsers.DOMParser;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.xml.sax.InputSource;

public class HtmlParser {
	
	private static final int TYPE_HTTP = 1;
	
	private static final int TYPE_FILE = 2;
	
	public static final List<String> filterTag = new ArrayList<String>();
	
	public static final String textTag = "#text";
	
	public static final String metaTag = "meta";
	
	public static final String charFilterPtn = "[0-9a-zA-Z/;\\s=]*";
	
	private String encoding = "UTF-8"; //默认UTF-8编码
	
	private AtomicInteger selfNum = new AtomicInteger(1);
	
	public static final StringBuilder sb = new StringBuilder();
	
	static {
		filterTag.add("SCRIPT");
		filterTag.add("STYLE");
		filterTag.add("#comment");
	}
	
	/**
	 * @param args
	 */
	public static void main(String[] args) {
		HtmlParser parser = new HtmlParser();
		if(args.length < 2) {
			System.out.println("Usage:\nstart 1 url_addr\nstart 2 file_addr");
			return;
		}
		int type = Integer.parseInt(args[0]);
		String addr = args[1];
		parser.doParse(type, addr);
	}
	
	private String getEncoding(String filePath) {
	    File file = new File(filePath);
	    try {
		    InputStream in= new FileInputStream(file);
		    byte[] b = new byte[3];
		    in.read(b);
		    in.close();
		    if (b[0] == -17 && b[1] == -69 && b[2] == -65) {
		    	encoding = "UTF-8";
		    } else {
		    	//其他暂时统一认为gbk
		    	encoding = "gbk";
		    }
	    } catch (Exception e) {
	    	e.printStackTrace();
	    }
	    return encoding;
	}
	
	/**
	 * 更好，更准确
	 * @param path
	 * @return
	 */
	private String getFileEncode(String path) {
	    CodepageDetectorProxy detector = CodepageDetectorProxy.getInstance();
	    detector.add(new ParsingDetector(false));
	    detector.add(JChardetFacade.getInstance());
	    detector.add(ASCIIDetector.getInstance());
	    detector.add(UnicodeDetector.getInstance());
	    java.nio.charset.Charset charset = null;
	    File f = new File(path);
	    try {
	    	charset = detector.detectCodepage(f.toURI().toURL());
	    } catch (Exception ex) {
	        ex.printStackTrace();
	    }
	    String charsetName = encoding;
	    if(charset != null && !charset.name().equalsIgnoreCase("void")) {
	    	charsetName = charset.name();
	    }
	    return charsetName;
	}
	
	public void doParse(int type, String addr) {
		try {
			DOMParser parser = new DOMParser();
			String fileName = "";
			if(type == TYPE_HTTP) {
				if(!addr.contains("http://")) {
					addr = "http://" + addr;
				}
				
				URL u = new URL(addr);
				InputSource uInputSource = new InputSource(u.openStream());
				parser.setProperty("http://cyberneko.org/html/properties/default-encoding", encoding);
		        parser.parse(uInputSource);
		        wrap(selfNum, 0, parser.getDocument(), "");
		        int idx = addr.lastIndexOf("/");
		        if(idx != -1) {
		        	fileName = addr.substring(idx) + ".txt";
		        }
			} else if(type == TYPE_FILE) {
				int idx = addr.lastIndexOf("/");
				fileName = "";
				if(idx != -1) {
					fileName = addr.substring(idx + 1) + ".txt";
				} else {
					fileName = addr + ".txt";
				}
				parser.setProperty("http://cyberneko.org/html/properties/default-encoding", "utf-8");
				parser.parse(addr);
				wrap(selfNum, 0, parser.getDocument(), "");
			}
	        FileUtils.writeStringToFile(new File("output/" + fileName), sb.toString().replaceAll("\u00A0", ""), "gbk");
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
	
	private void wrap(AtomicInteger selfNum, int parentNum, Node node, String indent) {
		String nodeName = node.getNodeName();
		if(!filterTag.contains(nodeName)) {
			if(nodeName.equalsIgnoreCase(metaTag)) {
				Element element = (Element) node;
				String metaContent = element.getAttribute("content");
				nodeName = element.getAttribute("name");
				if(metaContent != null && metaContent.length() > 0) {
					metaContent = metaContent.replaceAll("\\s", "");
					String cnContext = metaContent.replaceAll(charFilterPtn, "");
					if(cnContext.length() > 0) {
						String nodeNumPre = "[" + parentNum + ":" + selfNum.get() + "]";
						sb.append(nodeNumPre + indent + nodeName + "==>" + metaContent + "\n");
						selfNum.incrementAndGet();
					}
				}
			} else {
				String nodeContext = node.getTextContent();
				if(nodeContext != null && nodeContext.length() > 0) {
					nodeContext = nodeContext.replaceAll("\\s", "");
					String cnContext = nodeContext.replaceAll(charFilterPtn, "");
					if(nodeContext.length() > 0 && cnContext.length() > 0) {
						String nodeNumPre = "[" + parentNum + ":" + selfNum.get() + "]";
						if(nodeName.equals(textTag)) {
							sb.append(nodeNumPre + indent + nodeName + "==>" + nodeContext + "\n");
						} else {
							sb.append(nodeNumPre + indent + nodeName + "==>\n");
						}
						selfNum.incrementAndGet();
					}
				}
			}
	        Node child = node.getFirstChild();
	        int parent = selfNum.get() - 1;
	        while (child != null) {
	        	wrap(selfNum, parent, child, indent+"\t");
	            child = child.getNextSibling();
	        }
		}
	}
}
