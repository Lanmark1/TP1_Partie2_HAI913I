package main;

import visitors.*;
import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashMap;
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
import org.eclipse.jdt.core.dom.Block;
import org.eclipse.jdt.core.dom.CompilationUnit;
import org.eclipse.jdt.core.dom.FieldDeclaration;
import org.eclipse.jdt.core.dom.MethodDeclaration;
import org.eclipse.jdt.core.dom.MethodInvocation;
import org.eclipse.jdt.core.dom.PackageDeclaration;
import org.eclipse.jdt.core.dom.SimpleName;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

public class Parser {
	public static String projectPath;
	public static String projectSourcePath;
//	public static final String projectPath = "/home/e20190004783/Bureau/TP2_HAI913I/org.anonbnr.design_patterns-main/";
//	public static final String projectSourcePath = projectPath + "/src";
//	public static final String jrePath = "C:\\Program Files\\Java\\jre1.8.0_51\\lib\\rt.jar";

	public static void main(String[] args) throws IOException {

		// read java files
		projectPath =  System.getProperty("user.dir") + "/projectsToParse/org.anonbnr.design_patterns-main";
//		String[] spl = System.getProperty("user.dir").split("");
//		String[] newArray = Arrays.copyOf(spl, spl.length-1);
//		StringBuilder stringBuilder = new StringBuilder();
//		for (String str : newArray) {
//		    stringBuilder.append(str);
//		}
//		System.out.println(stringBuilder.toString());
//		System.out.println(Arrays.toString(spl));
		projectSourcePath = projectPath + "/src";
//		System.out.println(projectPath);
//		System.out.println(System.getProperty("os.name"));
		final File folder = new File(projectSourcePath);
		ArrayList<File> javaFiles = listJavaFilesForFolder(folder);
//		int nbMethods = 0;
		int nbLines = 0;
		int allLinesInMethods = 0;
		int nbFields = 0;
		List<PackageDeclaration> packages = new ArrayList<>();
		List<Integer> nbMethods = new ArrayList<>();
		List<String> classesWithMoreThanXMethods = new ArrayList<>();
		Map<String,Integer> mapClassesFields = new HashMap<>();
		Map<String,Integer> mapClassesMethods = new HashMap<>(); 

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
			nbMethods.add(getNbMethodsInClass(parse));
			// add sum of lines for all the methods in the class
			allLinesInMethods += getNbOfLinesForAllMethods(parse);
			// add the number of fields in the class
			nbFields += getNbFieldsForClass(parse);
			// add class name and field count entry to the map
			mapClassesMethods.put(fileEntry.getName(), getNbMethodsInClass(parse));
			// add class name and field count entry to the map
			mapClassesFields.put(fileEntry.getName(), getNbFieldsForClass(parse));
			// see if class has more than X methods
			if (classHasMoreThan(parse, 2)) {
				// then add to the list
				classesWithMoreThanXMethods.add(fileEntry.getName());
			}
//			// print variables info
//			printVariableInfo(parse);

			// print package info
//			printPackageInfo(parse);
			// print field info
//			printFieldInfov2(parse);
			
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
		
		int nbMethodsSum = 0;
		for (int i = 0; i < nbMethods.size(); i++) {
			nbMethodsSum += nbMethods.get(i);
		}
		float avgNbMethods = (float) nbMethodsSum / (float) javaFiles.size();
		float avgNbLinesPerMethod = (float) allLinesInMethods / nbMethodsSum;
		float avgNbFields = (float) nbFields / (float) javaFiles.size();
		
//		Map<String, Integer> sortedMapClassesFields = sortByDescMap(mapClassesFields);
//		Map<String, Integer> sortedMapClassesMethods = sortByDescMap(mapClassesMethods);
		List<String> topXPercentOfClassesMethods = getPercentageOfMap(sortByDescMap(mapClassesMethods), 30);
		List<String> topXPercentOfClassesFields = getPercentageOfMap(sortByDescMap(mapClassesFields), 30);
		
		List<String> bothCategories = new ArrayList<String>();
		for (String class1 : topXPercentOfClassesMethods) {
			for (String class2 : topXPercentOfClassesFields) {
				if (class1.equals(class2)) {
					bothCategories.add(class1);
				}
			}
		}
		System.out.println("1. nbClasses : " + javaFiles.size() + "\n2. nbLines : " + nbLines
				+ "\n3. nbMethods : " + nbMethodsSum + "\n4. nbPackages : " + packagesClean.size()
				+ "\n5. avgNbMethodsPerClass : " + avgNbMethods + "\n6. avgNbLinesPerMethod : " + avgNbLinesPerMethod
				+ "\n7. avgNbFieldsPerClass : " + avgNbFields + "\n8. 10% of classes that have most methods : " + topXPercentOfClassesMethods
				+ "\n9. 10% of classes that have most attributes : " + topXPercentOfClassesFields 
				+ "\n10. Classes that are in both categories 8. and 9. : " + bothCategories
				+ "\n11. Classes that have more than X methods : " + classesWithMoreThanXMethods
				+ "\n12. 10% of the methods that have the most lines of code : " /*+ methodsThatHaveTheMostLines*/);
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

	
	public static List<PackageDeclaration> getFilePackages(CompilationUnit parse) {
		PackageDeclarationVisitor visitor = new PackageDeclarationVisitor();
		parse.accept(visitor);
		return visitor.getPackages();
	}
	
	// get method number
	public static int getNbMethodsInClass(CompilationUnit parse) {
		MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
		parse.accept(visitor);
		return visitor.getMethods().size();
	}
	
	public static int getNbOfLinesForMethod(MethodDeclaration method) {
		Block body = method.getBody();
		if (!(body == null)) {
			List<String> linesInMethod = new ArrayList<String>(Arrays.asList(body.toString().split("\n")));
			linesInMethod.remove("{");
			linesInMethod.remove("}");
			return linesInMethod.size();
		}
		else {
			return 0;
		}
	}
	
	public static int getNbOfLinesForAllMethods(CompilationUnit parse) {
		MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
		parse.accept(visitor);
		int sum = 0;
		for (MethodDeclaration method : visitor.getMethods()) {
			sum += getNbOfLinesForMethod(method);	
			}
		return sum;
	}
	
	// get method number
	public static int getNbFieldsForClass(CompilationUnit parse) {
		FieldDeclarationVisitor visitor = new FieldDeclarationVisitor();
		parse.accept(visitor);
		return visitor.getFields().size();
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

		public static void printPackageInfo(CompilationUnit parse) {
			PackageDeclarationVisitor visitor = new PackageDeclarationVisitor();
			parse.accept(visitor);
			System.out.println("Packages : " + visitor.getPackages());
		}
		
		// navigate field information
		public static void printFieldInfo(CompilationUnit parse) {
			FieldAccessVisitor visitor = new FieldAccessVisitor();
			parse.accept(visitor);
			System.out.println("Number of fields : " + visitor.getFields().size());
			for (SimpleName field: visitor.getFields()) {
				System.out.println("Field name: " + field.getFullyQualifiedName());
				}
			}
		
		public static void printFieldInfov2(CompilationUnit parse) {
			FieldDeclarationVisitor visitor = new FieldDeclarationVisitor();
			parse.accept(visitor);
			System.out.println("Number of fields : " + visitor.getFields().size());
			for (FieldDeclaration field: visitor.getFields()) {
				System.out.println("Field name: " + field.toString());
				}
			}
		
		public static Map<String, Integer> sortByDescMap(Map<String, Integer> map) {
			// Convert the map into a list of entries
	        List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());

	        // Define a custom comparator to sort by integer values
	        Comparator<Map.Entry<String, Integer>> valueComparator = Comparator.comparing(Map.Entry::getValue);

	        // Sort the list of entries using the custom comparator
	        entryList.sort(valueComparator);
	        Collections.reverse(entryList);

	        // Create a new LinkedHashMap to store the sorted entries
	        Map<String, Integer> sortedMap = new LinkedHashMap<>();

	        // Populate the sorted map with the sorted entries
	        for (Map.Entry<String, Integer> entry : entryList) {
	            sortedMap.put(entry.getKey(), entry.getValue());
	        }
	        
	        return sortedMap;
		}
		
		public static List<String> getPercentageOfMap(Map<String, Integer> map, int percentage) {
			if (map.size() == 0) return null;
			List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
			List<String> list = new ArrayList<String>();
			int nbToFetch = (int) (Math.floor((0.01 * percentage) * map.size()));
			// if percentage is too low to fetch one class, fetch only the first one
			if (nbToFetch == 0) {
				list.add(entryList.get(0).getKey());
			}
			else {
				for (int i = 0; i < nbToFetch; i++) {
					list.add(entryList.get(i).getKey());
				}
			}
			return list;
		}
		
		public static boolean classHasMoreThan(CompilationUnit parse, int val) {
			return getNbMethodsInClass(parse) > val;
		}
}
