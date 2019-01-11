package org.apache.ibatis.parsing;

import java.io.InputStream;
import java.io.Reader;
import java.io.StringReader;
import java.util.ArrayList;
import java.util.List;
import java.util.Properties;

import javax.xml.namespace.QName;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathFactory;

import org.apache.ibatis.builder.BuilderException;
import org.w3c.dom.Document;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.EntityResolver;
import org.xml.sax.ErrorHandler;
import org.xml.sax.InputSource;
import org.xml.sax.SAXException;
import org.xml.sax.SAXParseException;

/**
 * 对原始的XPath进行进一步封装处理,用于解析给定的xml的节点信息
 */
public class XPathParser {

	private final Document document;
	private XPath xpath;
	private EntityResolver entityResolver;
	
	private boolean validation;
	private Properties variables;
	
	/*
	 * 通过函数重载的方式进行提供多种方式进行构建封装的解析器对象
	 */
	public XPathParser(String xml) {
		commonConstructor(false, null, null);
		this.document = createDocument(new InputSource(new StringReader(xml)));
	}

	public XPathParser(Reader reader) {
		commonConstructor(false, null, null);
		this.document = createDocument(new InputSource(reader));
	}

	public XPathParser(InputStream inputStream) {
		commonConstructor(false, null, null);
		this.document = createDocument(new InputSource(inputStream));
	}

	public XPathParser(Document document) {
		commonConstructor(false, null, null);
		this.document = document;
	}

	public XPathParser(String xml, boolean validation) {
		commonConstructor(validation, null, null);
		this.document = createDocument(new InputSource(new StringReader(xml)));
	}

	public XPathParser(Reader reader, boolean validation) {
		commonConstructor(validation, null, null);
		this.document = createDocument(new InputSource(reader));
	}

	public XPathParser(InputStream inputStream, boolean validation) {
		commonConstructor(validation, null, null);
		this.document = createDocument(new InputSource(inputStream));
	}

	public XPathParser(Document document, boolean validation) {
		commonConstructor(validation, null, null);
		this.document = document;
	}

	public XPathParser(String xml, boolean validation, Properties variables) {
		commonConstructor(validation, variables, null);
		this.document = createDocument(new InputSource(new StringReader(xml)));
	}

	public XPathParser(Reader reader, boolean validation, Properties variables) {
		commonConstructor(validation, variables, null);
		this.document = createDocument(new InputSource(reader));
	}

	public XPathParser(InputStream inputStream, boolean validation, Properties variables) {
		commonConstructor(validation, variables, null);
		this.document = createDocument(new InputSource(inputStream));
	}

	public XPathParser(Document document, boolean validation, Properties variables) {
		commonConstructor(validation, variables, null);
		this.document = document;
	}

	public XPathParser(String xml, boolean validation, Properties variables, EntityResolver entityResolver) {
		commonConstructor(validation, variables, entityResolver);
		this.document = createDocument(new InputSource(new StringReader(xml)));
	}

	public XPathParser(Reader reader, boolean validation, Properties variables, EntityResolver entityResolver) {
		commonConstructor(validation, variables, entityResolver);
		this.document = createDocument(new InputSource(reader));
	}

	public XPathParser(InputStream inputStream, boolean validation, Properties variables, EntityResolver entityResolver) {
		commonConstructor(validation, variables, entityResolver);
		this.document = createDocument(new InputSource(inputStream));
	}

	public XPathParser(Document document, boolean validation, Properties variables, EntityResolver entityResolver) {
		commonConstructor(validation, variables, entityResolver);
		this.document = document;
	}
	
	/*
	 * 构建对象时进行初始化相关参数
	 */
	private void commonConstructor(boolean validation, Properties variables, EntityResolver entityResolver) {
		this.validation = validation;
		this.entityResolver = entityResolver;
		this.variables = variables;
		//获取对应的XPathFactory工厂对象
		XPathFactory factory = XPathFactory.newInstance();
		//根据对应的XPathFactory工厂获取对应的XPath解析器对象
		this.xpath = factory.newXPath();
	}

	public void setVariables(Properties variables) {
		this.variables = variables;
	}

	public String evalString(String expression) {
		return evalString(document, expression);
	}

	public String evalString(Object root, String expression) {
		String result = (String) evaluate(expression, root, XPathConstants.STRING);
		result = PropertyParser.parse(result, variables);
		return result;
	}

	public Boolean evalBoolean(String expression) {
		return evalBoolean(document, expression);
	}

	public Boolean evalBoolean(Object root, String expression) {
		return (Boolean) evaluate(expression, root, XPathConstants.BOOLEAN);
	}

	public Short evalShort(String expression) {
		return evalShort(document, expression);
	}

	public Short evalShort(Object root, String expression) {
		return Short.valueOf(evalString(root, expression));
	}

	public Integer evalInteger(String expression) {
		return evalInteger(document, expression);
	}

	public Integer evalInteger(Object root, String expression) {
		return Integer.valueOf(evalString(root, expression));
	}

	public Long evalLong(String expression) {
		return evalLong(document, expression);
	}

	public Long evalLong(Object root, String expression) {
		return Long.valueOf(evalString(root, expression));
	}

	public Float evalFloat(String expression) {
		return evalFloat(document, expression);
	}

	public Float evalFloat(Object root, String expression) {
		return Float.valueOf(evalString(root, expression));
	}

	public Double evalDouble(String expression) {
		return evalDouble(document, expression);
	}

	public Double evalDouble(Object root, String expression) {
		return (Double) evaluate(expression, root, XPathConstants.NUMBER);
	}

	public List<XNode> evalNodes(String expression) {
		return evalNodes(document, expression);
	}

	public List<XNode> evalNodes(Object root, String expression) {
		List<XNode> xnodes = new ArrayList<XNode>();
		NodeList nodes = (NodeList) evaluate(expression, root, XPathConstants.NODESET);
		for (int i = 0; i < nodes.getLength(); i++) {
			xnodes.add(new XNode(this, nodes.item(i), variables));
		}
		return xnodes;
	}

	/*
	 * 根据给定的表达式获取对应的节点
	 */
	public XNode evalNode(String expression) {
		return evalNode(document, expression);
	}

	/*
	 * 根据提供的根节点和对应的表达式获取对应封装类型的节点
	 */
	public XNode evalNode(Object root, String expression) {
		//首先获取原生节点
		Node node = (Node) evaluate(expression, root, XPathConstants.NODE);
		//检测是否获取对应的节点
		if (node == null) {
			//返回对应的空对象
			return null;
		}
		//对原生节点进行封装获取封装后对应的节点对象
		return new XNode(this, node, variables);
	}

	/*
	 * 根据提供的表达式获取原生对应的节点
	 */
	private Object evaluate(String expression, Object root, QName returnType) {
		try {
			//通过xpath来获取指定表达式对应的原始节点
			return xpath.evaluate(expression, root, returnType);
		} catch (Exception e) {
			throw new BuilderException("Error evaluating XPath.  Cause: " + e, e);
		}
	}

	private Document createDocument(InputSource inputSource) {
		// important: this must only be called AFTER common constructor
		try {
			DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
			factory.setValidating(validation);

			factory.setNamespaceAware(false);
			factory.setIgnoringComments(true);
			factory.setIgnoringElementContentWhitespace(false);
			factory.setCoalescing(false);
			factory.setExpandEntityReferences(true);

			DocumentBuilder builder = factory.newDocumentBuilder();
			builder.setEntityResolver(entityResolver);
			builder.setErrorHandler(new ErrorHandler() {
				@Override
				public void error(SAXParseException exception) throws SAXException {
					throw exception;
				}

				@Override
				public void fatalError(SAXParseException exception) throws SAXException {
					throw exception;
				}

				@Override
				public void warning(SAXParseException exception) throws SAXException {
				}
			});
			return builder.parse(inputSource);
		} catch (Exception e) {
			throw new BuilderException("Error creating document instance.  Cause: " + e, e);
		}
	}
	
}
