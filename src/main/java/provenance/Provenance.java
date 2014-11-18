package provenance;

import japa.parser.JavaParser;
import japa.parser.ParseException;
import japa.parser.ast.CompilationUnit;
import japa.parser.ast.Node;
import japa.parser.ast.body.BodyDeclaration;
import japa.parser.ast.body.ClassOrInterfaceDeclaration;
import japa.parser.ast.body.ConstructorDeclaration;
import japa.parser.ast.body.FieldDeclaration;
import japa.parser.ast.body.MethodDeclaration;
import japa.parser.ast.body.Parameter;
import japa.parser.ast.body.VariableDeclarator;
import japa.parser.ast.body.VariableDeclaratorId;
import japa.parser.ast.expr.ClassExpr;
import japa.parser.ast.expr.Expression;
import japa.parser.ast.expr.FieldAccessExpr;
import japa.parser.ast.expr.IntegerLiteralExpr;
import japa.parser.ast.expr.MethodCallExpr;
import japa.parser.ast.expr.NameExpr;
import japa.parser.ast.expr.NullLiteralExpr;
import japa.parser.ast.expr.ObjectCreationExpr;
import japa.parser.ast.expr.StringLiteralExpr;
import japa.parser.ast.expr.ThisExpr;
import japa.parser.ast.expr.VariableDeclarationExpr;
import japa.parser.ast.stmt.BlockStmt;
import japa.parser.ast.stmt.ExplicitConstructorInvocationStmt;
import japa.parser.ast.stmt.ExpressionStmt;
import japa.parser.ast.stmt.ReturnStmt;
import japa.parser.ast.type.ClassOrInterfaceType;
import japa.parser.ast.type.ReferenceType;
import japa.parser.ast.type.VoidType;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Timestamp;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map.Entry;
import java.util.Properties;

import org.apache.commons.io.FileUtils;
import org.apache.commons.io.filefilter.SuffixFileFilter;
import org.apache.commons.io.filefilter.TrueFileFilter;

public class Provenance {
	
	private ArrayList<String> classNames;
	private ArrayList<String> doNotLog = new ArrayList<String>(Arrays.asList("hashCode","toString"));
	
	public Provenance() {
		String[] args = {"-jar", "/Users/zacharysylvia/.m2/repository/net/interactions/platform/ianalyst/ianalyst-desktop/5.2.0-SNAPSHOT/ianalyst-desktop-5.2.0-SNAPSHOT.jar",
						 "-o", "/Users/zacharysylvia/Downloads/ianalyst-desktop-jar/decompiled-ianalyst-desktop/"};
//		DecompilerDriver.main(args);
	}
	
	private void log(String log) {
		System.out.println(new Timestamp(new Date().getTime()) + " " + log);
	}
	
	private void writePropertiesFile(String projectName, Properties prop) {
		String propFileName = projectName + ".properties";
		OutputStream output = null;
		try {
			log(String.format("Storing properties in %s", propFileName));
			output = new FileOutputStream(propFileName);
			prop.store(output, null);
		} catch (IOException io) {
			io.printStackTrace();
		} finally {
			if (output != null) {
				try {
					output.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
	}
	
	private Properties getPropertiesFile(String projectName) {
		String propFileName = projectName + ".properties";
		Properties prop = null;
		InputStream in = null;
		try {
			File f = new File(propFileName);
			log(String.format("Checking if %s exists: %s", propFileName, f.exists()));
			if(f.exists()) {
				log(String.format("Loading properties from %s", propFileName));
				in = new FileInputStream(f);
				prop = new Properties();
				prop.load(in);
			} 
		} catch (IOException e) {
			e.printStackTrace();
		} finally {
			if (in != null) {
				try {
					in.close();
				} catch (IOException e) {
					e.printStackTrace();
				}
			}
		}
		return prop;
	}
	
	public void runInjection(String path) {
		long start = System.currentTimeMillis();
		try {
			File projectDirectory = new File(path);
			if(projectDirectory.exists()) {
				Properties prop = getPropertiesFile(path.substring(path.lastIndexOf("/") + 1));
				if(prop == null) {
					prop = new Properties();
				}
				int totalInjections = 0;
				Collection<File> col = FileUtils.listFiles(new File(path), new SuffixFileFilter("java"), TrueFileFilter.TRUE);
				classNames = new ArrayList<String>();
				for(File f : col) {
					log("Adding class " + f.getName().replace(".java", ""));
					classNames.add(f.getName().replace(".java", ""));
				}
				for (File f : col) {
					int injectionsInCurrentFile = 0;
					boolean writeToFile = false;
					if(!f.getName().equals("MenuManager.java")) {
//						continue;
					}
					if(prop.getProperty(f.getName()) != null && prop.getProperty(f.getName()).equals(new Timestamp(f.lastModified()).toString())) {
						log(String.format("File %s has not been modified since injection", f.getPath()));
						continue;
					}
//					JCodeModel codeModel = new JCodeModel();
					CompilationUnit cu = JavaParser.parse(f);
					
					for(Node n : cu.getChildrenNodes()) {
						if(n instanceof ClassOrInterfaceDeclaration && !((ClassOrInterfaceDeclaration)n).isInterface()) {
							writeToFile = true;
							ArrayList<ArrayList<String>> globalVariables = getGlobalVariables((ClassOrInterfaceDeclaration)n);
							
							for(Node n2 : ((ClassOrInterfaceDeclaration)n).getChildrenNodes()) {
								if(n2 instanceof ConstructorDeclaration) {
									ArrayList<ArrayList<String>> localVariables = getLocalVariables(n2);
									
									ConstructorDeclaration constructorDeclaration = (ConstructorDeclaration)n2;
									String log = "Constructor: " + constructorDeclaration.getName();
									
									List<Expression> paramNames = new ArrayList<Expression>();
									if(constructorDeclaration.getBlock() != null) {
										if(constructorDeclaration.getBlock().getStmts() != null && !constructorDeclaration.getBlock().getStmts().isEmpty()) {
											int positionToAddLog = 0;
											if(constructorDeclaration.getBlock().getStmts().get(0) instanceof ExplicitConstructorInvocationStmt) {
												positionToAddLog = 1;
											}
											if(constructorDeclaration.getParameters() != null && !constructorDeclaration.getParameters().isEmpty()) {
												String params = "[";
												List<Parameter> parameters = constructorDeclaration.getParameters();
												for(int i = 0; i < parameters.size(); i++) {
													Parameter p = parameters.get(i);
													//Getting many errors from logging the parameter objects
//													paramNames.add(new NameExpr(p.getId().getName()));
													params += "\"+" + p.getId() + "+\"";
													if(i != parameters.size()-1) {
														params += ", ";
													}
												}
												params += "]";
												log += "(" + parameters.toString().substring(1, parameters.toString().length()-1).toString().replace("\"", "\\\"") + "): input: " + params;
											} else {
												log += "()";
											}
											
											List<Expression> logObjArgs = new ArrayList<Expression>();
											List<Expression> date = new ArrayList<Expression>();
											date.add(new MethodCallExpr(new ObjectCreationExpr(null, new ClassOrInterfaceType("java.util.Date"), null),"getTime"));
											logObjArgs.add(new MethodCallExpr(new ObjectCreationExpr(null, new ClassOrInterfaceType("java.sql.Timestamp"), date), "toString"));
											if(((((ConstructorDeclaration)n2).getModifiers()-8)&((((ConstructorDeclaration)n2).getModifiers()-8)-1)) != 0) {
												//If this is true, that means this method is not static. If it was, we couldn't use hashCode
												//System.identityHashCode() gets the default hashCode() regardless if the hashCode method was overwritten
												LinkedList<Expression> thisArg = new LinkedList<Expression>();
												thisArg.add(new ThisExpr());
												logObjArgs.add(new MethodCallExpr(new NameExpr("System"), "identityHashCode", thisArg));
											}
											logObjArgs.add(new StringLiteralExpr(((ClassOrInterfaceDeclaration)n).getName()));
											logObjArgs.add(new MethodCallExpr(new NameExpr("java.util.Arrays"), "asList", paramNames));
											ObjectCreationExpr cre = new ObjectCreationExpr(null, new ClassOrInterfaceType(new ClassOrInterfaceType(new ClassOrInterfaceType("provenance"),"model"),"Constructor"), logObjArgs);
											List<Expression> args = new ArrayList<Expression>();
											args.add(cre);
											ExpressionStmt logExpr = new ExpressionStmt(new MethodCallExpr(new FieldAccessExpr(new NameExpr("provenance.log"), "ProvenanceLogger"), "log", args));
											constructorDeclaration.getBlock().getStmts().add(positionToAddLog, logExpr);
											injectionsInCurrentFile++;
//											for(Statement statement : constructorDeclaration.getBlock().getStmts()) {
//												
//											}
										}
									}
									for(Node child : ((ConstructorDeclaration)n2).getChildrenNodes()) {
										if(child instanceof BlockStmt) {
											ArrayList<ArrayList<String>> allVariables = new ArrayList<ArrayList<String>>();
											allVariables.addAll(globalVariables);
											allVariables.addAll(localVariables);
											HashMap<Node, HashMap<Integer, ExpressionStmt>> map = traverseNode(child, (ClassOrInterfaceDeclaration)n, allVariables);
											for(Entry<Node, HashMap<Integer, ExpressionStmt>> entry : map.entrySet()) {
												injectionsInCurrentFile += entry.getValue().size();
											}
										}
									}
								} else if(n2 instanceof MethodDeclaration && !doNotLog.contains(((MethodDeclaration)n2).getName())) {
									if(!(((MethodDeclaration)n2).getType() instanceof VoidType) && ((MethodDeclaration)n2).getBody() != null 
											&& ((MethodDeclaration)n2).getBody().getStmts().size() == 1 
											&& ((MethodDeclaration)n2).getBody().getStmts().get(0) instanceof ReturnStmt
											&& ((ReturnStmt)((MethodDeclaration)n2).getBody().getStmts().get(0)).getExpr() instanceof NameExpr) {
										boolean skip = false;
										for(ArrayList<String> classNamePair : globalVariables) {
											if(classNamePair.get(1).equals((((NameExpr)((ReturnStmt)((MethodDeclaration)n2).getBody().getStmts().get(0)).getExpr()).getName()))) {
												//Skip over logging for methods that are just 'getter' methods for global variables
												skip = true;
												break;
											}
										}
										if(skip) {
											continue;
										}
									}
									ArrayList<ArrayList<String>> localVariables = getLocalVariables(n2);
									
									MethodDeclaration methodDeclaration = (MethodDeclaration)n2;
									String log = "Method: " + methodDeclaration.getName();

									if(methodDeclaration.getBody() != null) {
										if(methodDeclaration.getBody().getStmts() != null && !methodDeclaration.getBody().getStmts().isEmpty()) {
											int positionToAddLog = 0;
											if(methodDeclaration.getBody().getStmts().get(0) instanceof ExplicitConstructorInvocationStmt) {
												positionToAddLog = 1;
											}
											if(methodDeclaration.getParameters() != null && !methodDeclaration.getParameters().isEmpty()) {
												String params = "[";
												List<Parameter> parameters = methodDeclaration.getParameters();
												for(int i = 0; i < parameters.size(); i++) {
													Parameter p = parameters.get(i);
													params += "\"+" + p.getId() + "+\"";
													if(i != parameters.size()-1) {
														params += ", ";
													}
												}
												params += "]";
												log += "(" + parameters.toString().substring(1, parameters.toString().length()-1).toString().replace("\"", "\\\"") + "): input: " + params;
											} else {
												log += "()";
											}
											
											List<Expression> logObjArgs = new ArrayList<Expression>();
											List<Expression> date = new ArrayList<Expression>();
											date.add(new MethodCallExpr(new ObjectCreationExpr(null, new ClassOrInterfaceType("java.util.Date"), null),"getTime"));
											logObjArgs.add(new MethodCallExpr(new ObjectCreationExpr(null, new ClassOrInterfaceType("java.sql.Timestamp"), date), "toString"));
											if(((((MethodDeclaration)n2).getModifiers()-8)&((((MethodDeclaration)n2).getModifiers()-8)-1)) != 0) {
												//If this is true, that means this method is not static. If it was, we couldn't use hashCode
												//System.identityHashCode() gets the default hashCode() regardless if the hashCode method was overwritten
												LinkedList<Expression> thisArg = new LinkedList<Expression>();
												thisArg.add(new ThisExpr());
												logObjArgs.add(new MethodCallExpr(new NameExpr("System"), "identityHashCode", thisArg));
											} else {
												logObjArgs.add(new IntegerLiteralExpr("-1"));
											}
											logObjArgs.add(new StringLiteralExpr(((ClassOrInterfaceDeclaration)n).getName()));
											logObjArgs.add(new StringLiteralExpr(methodDeclaration.getName()));
											logObjArgs.add(new MethodCallExpr(new NameExpr("java.util.Arrays"), "asList", null));
											ObjectCreationExpr cre = new ObjectCreationExpr(null, new ClassOrInterfaceType(new ClassOrInterfaceType(new ClassOrInterfaceType("provenance"),"model"),"Method"), logObjArgs);
											List<Expression> args = new ArrayList<Expression>();
											args.add(cre);
											ExpressionStmt logExpr = new ExpressionStmt(new MethodCallExpr(new FieldAccessExpr(new NameExpr("provenance.log"), "ProvenanceLogger"), "log", args));
											
											
											methodDeclaration.getBody().getStmts().add(positionToAddLog, logExpr);
											injectionsInCurrentFile++;
//											for(Statement statement : constructorDeclaration.getBlock().getStmts()) {
//												
//											}
										}
									}
									for(Node child : ((MethodDeclaration)n2).getChildrenNodes()) {
										if(child instanceof BlockStmt) {
											ArrayList<ArrayList<String>> allVariables = new ArrayList<ArrayList<String>>();
											allVariables.addAll(globalVariables);
											allVariables.addAll(localVariables);
											HashMap<Node, HashMap<Integer, ExpressionStmt>> map = traverseNode(child, (ClassOrInterfaceDeclaration)n, allVariables);
											for(Entry<Node, HashMap<Integer, ExpressionStmt>> entry : map.entrySet()) {
												injectionsInCurrentFile += entry.getValue().size();
											}
										}
									}
								}
							}
						}
					}

					totalInjections += injectionsInCurrentFile;
					if(writeToFile) {
						FileUtils.write(f, cu.toString());
					}
					log(String.format("Injected %d provenance capturing logs into %s", injectionsInCurrentFile, f.getPath()));
					prop.remove(f.getName());
					prop.setProperty(f.getName(), new Timestamp(f.lastModified()).toString());
				}
				writePropertiesFile(path.substring(path.lastIndexOf("/") + 1), prop);
				log(String.format("Total number of injected provenance capturing logs over all files: %d", totalInjections));
			} else {
				log(String.format("Project directory %s does not exists.", path));
			}
		} catch (IOException e) {
			e.printStackTrace();
		} catch (ParseException e) {
			e.printStackTrace();
		} finally {
			long end = System.currentTimeMillis();
			log("Injection took " + (end-start)/1000 + " seconds to complete");
		}
	}
	
	private ArrayList<ArrayList<String>> getGlobalVariables(ClassOrInterfaceDeclaration mainClass) {
		ArrayList<ArrayList<String>> variables = new ArrayList<ArrayList<String>>();
		for(BodyDeclaration member : mainClass.getMembers()) {
			if(member instanceof FieldDeclaration) {
				ArrayList<String> pair = new ArrayList<String>();
				//Class name is index 0
				pair.add(((FieldDeclaration)member).getType().toString());
				//Variable name is index 1
				pair.add(((FieldDeclaration)member).getVariables().get(0).getId().getName());
				variables.add(pair);
			}
		}
		return variables;
	}
	
	private ArrayList<ArrayList<String>> getLocalVariables(Node node) {
		ArrayList<ArrayList<String>> variables = new ArrayList<ArrayList<String>>();
		if(node.getChildrenNodes() != null && !node.getChildrenNodes().isEmpty()) {
			for(Node child : node.getChildrenNodes()) {
				ArrayList<String> nodeParams = new ArrayList<String>(2);
				if(child instanceof VariableDeclarator) {
					String className = "";
					if(node instanceof FieldDeclaration) {
						className = ((FieldDeclaration)node).getType().toString();
					} else if (node instanceof VariableDeclarationExpr) {
						className = ((VariableDeclarationExpr)node).getType().toString();
					}
					if(!className.equals("")) {
						//Class name is index 0
						nodeParams.add(className);
						//Variable name is index 1
						nodeParams.add(((VariableDeclarator)child).getId().getName());
					}
				}
				variables.add(nodeParams);
				//Get parameters of all children
				variables.addAll(getLocalVariables(child));
			}
		}
		return variables;
	}
	
	private HashMap<Node, HashMap<Integer, ExpressionStmt>> traverseNode(Node node, ClassOrInterfaceDeclaration classDeclaration, ArrayList<ArrayList<String>> variables) {
		HashMap<Node, HashMap<Integer, ExpressionStmt>> map = new HashMap<Node, HashMap<Integer, ExpressionStmt>>();
		if(node.getChildrenNodes() != null && !node.getChildrenNodes().isEmpty()) {
			HashMap<Integer, ExpressionStmt> logMap = new HashMap<Integer, ExpressionStmt>();
			for(int i = 0; i < node.getChildrenNodes().size(); i++) {
				Node child = node.getChildrenNodes().get(i);
				if(child instanceof MethodCallExpr) {
					if(((MethodCallExpr)child).getScope() != null && !((MethodCallExpr)child).getScope().toString().equals("super") && (child.getParentNode() instanceof ExpressionStmt) 
							&& !((MethodCallExpr)child).getName().equals("ProvenanceLogger") && child.getParentNode().getParentNode() instanceof BlockStmt 
							&& validObjectToLog(((MethodCallExpr)child).getScope().toString(),variables)) {
						List<Expression> logObjArgs = new ArrayList<Expression>();
						List<Expression> date = new ArrayList<Expression>();
						date.add(new MethodCallExpr(new ObjectCreationExpr(null, new ClassOrInterfaceType("java.util.Date"), null),"getTime"));
						logObjArgs.add(new MethodCallExpr(new ObjectCreationExpr(null, new ClassOrInterfaceType("java.sql.Timestamp"), date), "toString"));
						if(!methodIsStatic(child)) {
							LinkedList<Expression> thisArg = new LinkedList<Expression>();
							thisArg.add(new ThisExpr());
							logObjArgs.add(new MethodCallExpr(new NameExpr("System"), "identityHashCode", thisArg));
						} else {
							logObjArgs.add(new IntegerLiteralExpr("-1"));
						}
						logObjArgs.add(new StringLiteralExpr(classDeclaration.getName()));
						List<Expression> calledObj = new ArrayList<Expression>();
						calledObj.add(new NameExpr(((MethodCallExpr)child).getScope().toString()));
						logObjArgs.add(new MethodCallExpr(new NameExpr("System"), "identityHashCode", calledObj));
						logObjArgs.add(new StringLiteralExpr("Unknown"));
						logObjArgs.add(new StringLiteralExpr(((MethodCallExpr)child).getName()));
						logObjArgs.add(new MethodCallExpr(new NameExpr("java.util.Arrays"), "asList", null));
						ObjectCreationExpr cre = new ObjectCreationExpr(null, new ClassOrInterfaceType(new ClassOrInterfaceType(new ClassOrInterfaceType("provenance"),"model"),"Calling"), logObjArgs);
						List<Expression> args = new ArrayList<Expression>();
						args.add(cre);
						ExpressionStmt logExpr = new ExpressionStmt(new MethodCallExpr(new FieldAccessExpr(new NameExpr("provenance.log"), "ProvenanceLogger"), "log", args));
						int index = ((BlockStmt)child.getParentNode().getParentNode()).getStmts().indexOf(child.getParentNode());
						logMap.put(index, logExpr);
					}
				}
				HashMap<Node, HashMap<Integer, ExpressionStmt>> children = traverseNode(child, classDeclaration, variables);
				if(children != null && !children.isEmpty()) {
					for (Entry<Node, HashMap<Integer, ExpressionStmt>> entry : children.entrySet()) {
						if(map.get(entry.getKey()) != null) {
							HashMap<Integer, ExpressionStmt> combine = new HashMap<Integer, ExpressionStmt>();
							combine.putAll(map.get(entry.getKey()));
							combine.putAll(entry.getValue());
							map.put(entry.getKey(), combine);
						} else {
							map.putAll(children);
						}
					}
				}
				for (Entry<Node, HashMap<Integer, ExpressionStmt>> entry : map.entrySet()) {
					for (Entry<Integer, ExpressionStmt> methodCalls : entry.getValue().entrySet()) {
						if(entry.getKey() instanceof BlockStmt) {
							((BlockStmt)entry.getKey()).getStmts().add(methodCalls.getKey(), methodCalls.getValue());
						}
					}
				}
				map.clear();
			}
			if(!logMap.isEmpty()) {
				map.put(node.getParentNode(), logMap);
			}
		}
		return map;
	}
	
	private boolean validObjectToLog(String variableName, ArrayList<ArrayList<String>> variables) {
		for(ArrayList<String> classNamePair : variables) {
			if(!classNamePair.isEmpty() && classNamePair.get(1).equals(variableName)) {
				if(classNames.contains(classNamePair.get(0))) {
					return true;
				} else {
					return false;
				}
			}
		}
		return false;
	}
	
	private boolean methodIsStatic(Node node) {
		while(node.getParentNode() != null) {
			Node parentNode = node.getParentNode();
			if(parentNode instanceof ConstructorDeclaration) {
				if(((((ConstructorDeclaration)parentNode).getModifiers()-8)&((((ConstructorDeclaration)parentNode).getModifiers()-8)-1)) == 0) {
					return true;
				} else {
					return false;
				}
			} else if(parentNode instanceof MethodDeclaration) {
				if(((((MethodDeclaration)parentNode).getModifiers()-8)&((((MethodDeclaration)parentNode).getModifiers()-8)-1)) == 0) {
					return true;
				} else {
					return false;
				}
			} else {
				node = parentNode;
			}
		}
		return false;
	}

	public static void main(String[] args) {
		new Provenance().runInjection("/Users/zacharysylvia/Documents/workspace/ianalyst-desktop");
	}

}
