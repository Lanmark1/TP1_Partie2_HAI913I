package main;

import visitors.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.apache.commons.io.FileUtils;
import org.eclipse.core.internal.utils.FileUtil;
import org.eclipse.core.resources.IProject;
import org.eclipse.core.resources.IWorkspaceRoot;
import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.core.runtime.CoreException;
import org.eclipse.jdt.core.ICompilationUnit;
import org.eclipse.jdt.core.JavaCore;
import org.eclipse.jdt.core.dom.AST;
import org.eclipse.jdt.core.dom.ASTParser;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class Parser {
	public static String projectPath;
	public static String projectSourcePath;
//	public static final String projectPath = "/home/e20190004783/Bureau/TP2_HAI913I/org.anonbnr.design_patterns-main/";
//	public static final String projectSourcePath = projectPath + "/src";
//	public static final String jrePath = "C:\\Program Files\\Java\\jre1.8.0_51\\lib\\rt.jar";

	public static void main(String[] args) throws IOException {

		// read java files
		projectPath =  System.getProperty("user.dir") + "/../org.anonbnr.design_patterns-main/";
		projectSourcePath = projectPath + "/src";
		System.out.println(projectPath);
		final File folder = new File(projectSourcePath);
		ArrayList<File> javaFiles = listJavaFilesForFolder(folder);
//		int nbMethods = 0;
		int nbLines = 0;
		List<PackageDeclaration> packages = new ArrayList<>();
		List<Integer> nbMethods = new ArrayList<>();
		//
		for (File fileEntry : javaFiles) {
			String content = FileUtils.readFileToString(fileEntry);
//			 System.out.println(fileEntry);
//			 System.out.println(content.split("\n").length);
			CompilationUnit parse = parse(content.toCharArray());
			// print methods info
//			printMethodInfo(parse);
			packages.addAll(getFilePackages(parse));
			
			// add number of lines
			nbLines += content.split("\n").length;
			// add number of methods
			nbMethods.add(getNbMethodsInFile(parse));
//
//			// print variables info
//			printVariableInfo(parse);

			// print package info
//			printPackageInfo(parse);
			
			//print method invocations
//			printMethodInvocationInfo(parse);

		}
//		packages = new ArrayList<>(new HashSet<>(packages));
		List<String> packagesClean = new ArrayList<>();
		for (PackageDeclaration p : packages) {
			if (!packagesClean.contains(p.toString())) {
				packagesClean.add(p.toString());
			}
		}
		
		int sum = 0;
		for (int i = 0; i < nbMethods.size(); i++) {
			sum += nbMethods.get(i);
			
		}
		System.out.println("nbClasses : " + javaFiles.size() + ", nbLines : " + nbLines + ", nbMethods : " + sum + ", nbPackages : " + packagesClean.size());
//		System.out.println(packagesClean);
	}

	// read all java files from specific folder
	public static ArrayList<File> listJavaFilesForFolder(final File folder) {
		ArrayList<File> javaFiles = new ArrayList<File>();
		for (File fileEntry : folder.listFiles()) {
			if (fileEntry.isDirectory()) {
				javaFiles.addAll(listJavaFilesForFolder(fileEntry));
			} else if (fileEntry.getName().contains(".java")) {
				// System.out.println(fileEntry.getName());
				javaFiles.add(fileEntry);
			}
		}

		return javaFiles;
	}

	// create AST
	private static CompilationUnit parse(char[] classSource) {
		ASTParser parser = ASTParser.newParser(AST.JLS4); // java +1.6
		parser.setResolveBindings(true);
		parser.setKind(ASTParser.K_COMPILATION_UNIT);
 
		parser.setBindingsRecovery(true);
 
		Map options = JavaCore.getOptions();
		parser.setCompilerOptions(options);
 
		parser.setUnitName("");
 
		String[] sources = { projectSourcePath }; 
//		String[] classpath = {jrePath};
 
		parser.setEnvironment(null, sources, new String[] { "UTF-8"}, true);
		parser.setSource(classSource);
		
		return (CompilationUnit) parser.createAST(null); // create and parse
	}

	// navigate method information
	public static void printMethodInfo(CompilationUnit parse) {
		MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
		parse.accept(visitor);
		System.out.println("Number of methods : " + visitor.getMethods().size());
		for (MethodDeclaration method : visitor.getMethods()) {
			System.out.println("Method name: " + method.getName()
					+ " Return type: " + method.getReturnType2());
		}
	}
	
	public static List<PackageDeclaration> getFilePackages(CompilationUnit parse) {
		PackageDeclarationVisitor visitor = new PackageDeclarationVisitor();
		parse.accept(visitor);
		return visitor.getPackages();
	}
	
	public static void printPackageInfo(CompilationUnit parse) {
		PackageDeclarationVisitor visitor = new PackageDeclarationVisitor();
		parse.accept(visitor);
		System.out.println("Packages : " + visitor.getPackages());
	}
	
	
	// get method number
		public static int getNbMethodsInFile(CompilationUnit parse) {
			MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
			parse.accept(visitor);
			return visitor.getMethods().size();
		}

	// navigate variables inside method
	public static void printVariableInfo(CompilationUnit parse) {

		MethodDeclarationVisitor visitor1 = new MethodDeclarationVisitor();
		parse.accept(visitor1);
		
		for (MethodDeclaration method : visitor1.getMethods()) {

			VariableDeclarationFragmentVisitor visitor2 = new VariableDeclarationFragmentVisitor();
			method.accept(visitor2);
			int nbVariables = visitor2.getVariables().size();
			if (nbVariables != 0)
				System.out.println("Number of variables for " + method.getName() + " : " + nbVariables);
			for (VariableDeclarationFragment variableDeclarationFragment : visitor2
					.getVariables()) {
				System.out.println("variable name: "
						+ variableDeclarationFragment.getName()
						+ " variable Initializer: "
						+ variableDeclarationFragment.getInitializer());
			}

		}
	}
	
	// navigate method invocations inside method
		public static void printMethodInvocationInfo(CompilationUnit parse) {

			MethodDeclarationVisitor visitor1 = new MethodDeclarationVisitor();
			parse.accept(visitor1);
			for (MethodDeclaration method : visitor1.getMethods()) {

				MethodInvocationVisitor visitor2 = new MethodInvocationVisitor();
				method.accept(visitor2);

				for (MethodInvocation methodInvocation : visitor2.getMethods()) {
					System.out.println("method " + method.getName() + " invoc method "
							+ methodInvocation.getName());
				}

			}
		}

}
