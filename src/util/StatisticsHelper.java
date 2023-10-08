package util;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;

import org.apache.commons.io.FileUtils;
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
import org.eclipse.jdt.core.dom.SuperMethodInvocation;
import org.eclipse.jdt.core.dom.VariableDeclarationFragment;

import visitors.FieldAccessVisitor;
import visitors.FieldDeclarationVisitor;
import visitors.MethodDeclarationVisitor;
import visitors.MethodInvocationVisitor;
import visitors.PackageDeclarationVisitor;
import visitors.VariableDeclarationFragmentVisitor;

public final class StatisticsHelper {
	
	public static String projectPath;
	public static String projectSourcePath;
	
	public static void showStatistics(String projectName) throws IOException {
		if (projectName.equals("")) { // default project to parse if no input is provided
			projectPath = System.getProperty("user.dir");			
		}
		else {
			projectPath =  System.getProperty("user.dir") + "/projectsToParse/" + projectName;			
		}
		// read java files
		projectSourcePath = projectPath + "/src";
		final File folder = new File(projectSourcePath);
		ArrayList<File> javaFiles = listJavaFilesForFolder(folder);
		System.out.println("Project " + projectName);
		System.out.println("Part 1 : Analyzing the project");
		int nbLines = 0;
		int allLinesInMethods = 0;
		int nbFields = 0;
		int valMethods1 = 3;
		int valMethods2 = 6;
		List<PackageDeclaration> packages = new ArrayList<>();
		List<Integer> nbMethods = new ArrayList<>();
		Map<String,Integer> classesWithMoreThanXMethods1 = new HashMap<>();
		Map<String,Integer> classesWithMoreThanXMethods2 = new HashMap<>();
		Map<String,Integer> mapClassesFields = new HashMap<>();
		Map<String,Integer> mapClassesMethods = new HashMap<>(); 
		Map<String,Integer> mapMethodsWithLinesOfCode = new HashMap<>();
		Map<String,Object> mapMethodWithMostParameters = new HashMap<>();
		// default values for the map
		mapMethodWithMostParameters.put("name", "none");
		mapMethodWithMostParameters.put("number", 0);
		for (File fileEntry : javaFiles) {
			String content = FileUtils.readFileToString(fileEntry);
			CompilationUnit parse = parse(content.toCharArray());
			packages.addAll(getFilePackages(parse));
			// add number of lines
			nbLines += content.split("\n").length;
			// add number of methods
			nbMethods.add(getNbMethodsInClass(parse));
			// add sum of lines for all the methods in the class
			allLinesInMethods += getNbOfLinesForAllMethods(parse);
			// add the number of fields in the class
			nbFields += getNbFieldsForClass(parse);
			// add class name and methods count entry to the map
			mapClassesMethods.put(fileEntry.getName(), getNbMethodsInClass(parse));
			// add class name and field count entry to the map
			mapClassesFields.put(fileEntry.getName(), getNbFieldsForClass(parse));
			// see if class has more than X methods
			if (classHasMoreThanXMethods(parse, valMethods1)) {
				// then add to the map
				classesWithMoreThanXMethods1.put(fileEntry.getName(), getNbMethodsInClass(parse));
			}
			if (classHasMoreThanXMethods(parse, valMethods2)) {
				// then add to the map
				classesWithMoreThanXMethods2.put(fileEntry.getName(), getNbMethodsInClass(parse));
			}
			// add submap of method / number of lines to global map 
			mapMethodsWithLinesOfCode.putAll(getAllMethodsWithNbLinesOfCodeFromClass(parse));
			// add submap of method / number of parameters to global map 
			mapMethodWithMostParameters = maxNumberOfParametersForMethod(parse, mapMethodWithMostParameters);
		}

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
		
		List<Entry<String, Integer>> topXPercentOfClassesMethods = getPercentageOfMap(sortByDescMap(mapClassesMethods), 10);
		List<Entry<String, Integer>> topXPercentOfClassesFields = getPercentageOfMap(sortByDescMap(mapClassesFields), 10);
		List<Entry<String, Integer>> methodsThatHaveTheMostLines = getPercentageOfMap(sortByDescMap(mapMethodsWithLinesOfCode), 10);
		classesWithMoreThanXMethods1 = sortByDescMap(classesWithMoreThanXMethods1);
		classesWithMoreThanXMethods2 = sortByDescMap(classesWithMoreThanXMethods2);
		List<String> bothCategories = new ArrayList<String>();
		for (Entry<String, Integer> class1 : topXPercentOfClassesMethods) {
			for (Entry<String, Integer> class2 : topXPercentOfClassesFields) {
				if (class1.getKey().equals(class2.getKey())) {
					bothCategories.add(class1.getKey());
				}
			}
		}
		System.out.println("1. Total number of classes : " + javaFiles.size());
		System.out.println("2. Total number of lines : " + nbLines);
		System.out.println("3. Total number of methods : " + nbMethodsSum);
		System.out.println("4. Total number of packages : " + packagesClean.size());
		System.out.println(String.format("5. Average number of methods per class : %.1f", avgNbMethods));
		System.out.println(String.format("6. Average number of lines per method : %.1f", avgNbLinesPerMethod));
		System.out.println(String.format("7. Average number of attributes per class : %.1f", avgNbFields));
		System.out.println("8. Top 10% of classes that have most methods : " + formatListEntry(topXPercentOfClassesMethods));
		System.out.println("9. Top 10% of classes that have most attributes : " + formatListEntry(topXPercentOfClassesFields));
		System.out.println("10. Classes that are in both categories 8. and 9. : " + bothCategories);
		System.out.println(String.format("11. Classes that have more than %s methods : %s", valMethods1,formatMap(classesWithMoreThanXMethods1)));
		System.out.println(String.format("    Classes that have more than %s methods : %s", valMethods2,formatMap(classesWithMoreThanXMethods2)));
		System.out.println("12. 10% of the methods that have the most lines of code : " + formatListEntry(methodsThatHaveTheMostLines));
		System.out.println("13. Max number of parameters in a method : " + mapMethodWithMostParameters.get("number") + " (" + mapMethodWithMostParameters.get("name") + ")");
	}
	
	public static void showCallGraph(String projectName) throws IOException {
		if (projectName.equals("")) { // default project to parse if no input is provided
			projectPath = System.getProperty("user.dir");			
		}
		else {
			projectPath =  System.getProperty("user.dir") + "/projectsToParse/" + projectName;			
		}
		// read java files
		projectSourcePath = projectPath + "/src";
		final File folder = new File(projectSourcePath);
		ArrayList<File> javaFiles = listJavaFilesForFolder(folder);
		List<MethodDeclaration> listMethodDeclarations = new ArrayList<>();
		List<MethodInvocation> listMethodInvocations = new ArrayList<>();
		List<String> methodNames = new ArrayList<>();
		System.out.println("Part 2 : Call graph");
		for (File fileEntry : javaFiles) {
			
			String content = FileUtils.readFileToString(fileEntry);
			System.out.println("in " + fileEntry.getName());
			CompilationUnit parse = parse(content.toCharArray());
			listMethodDeclarations = getMethodDeclarations(parse);
			for (MethodDeclaration method : listMethodDeclarations) {
				System.out.print("  -> " + method.getName());
				methodNames.clear();
				listMethodInvocations = getMethodInvocationsFromDeclarations(method);
				if (listMethodInvocations.size() != 0)
					System.out.println(" calls :");
				else
					System.out.println();
				for(MethodInvocation mi : listMethodInvocations) {
					methodNames.add(mi.getName().toString());
				}
				// remove duplicate calls in the same function
				methodNames = new ArrayList<>(new LinkedHashSet<>(methodNames));
				for(String methodName : methodNames) {
					System.out.println("    -> " + methodName);					
				}
			}
		}
	}
	
		// read all java files from specific folder
		public static ArrayList<File> listJavaFilesForFolder(final File folder) {
			ArrayList<File> javaFiles = new ArrayList<File>();
			for (File fileEntry : folder.listFiles()) {
				if (fileEntry.isDirectory()) {
					javaFiles.addAll(listJavaFilesForFolder(fileEntry));
				} else if (fileEntry.getName().contains(".java")) {
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
		
		public static int getNbOfLinesForMethod(CompilationUnit parse, MethodDeclaration method) {
	        
	        // Calculate the number of lines within the method
			int methodStartPosition = method.getStartPosition();
	        int methodEndPosition = methodStartPosition + method.getLength();

	        // Calculate the line numbers for the method
	        int startLineNumber = parse.getLineNumber(methodStartPosition);
	        int endLineNumber = parse.getLineNumber(methodEndPosition);
	        return endLineNumber - startLineNumber + 1;
		}
		
		public static int getNbOfLinesForAllMethods(CompilationUnit parse) {
			MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
			parse.accept(visitor);
			int sum = 0;
			for (MethodDeclaration method : visitor.getMethods()) {
				sum += getNbOfLinesForMethod(parse, method);	
				}
			return sum;
		}
		
		// get number of fields in class
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
		
		public static String formatListEntry(List<Map.Entry<String, Integer>> listEntry) {
			if (listEntry.size() == 0) return "[ ]";
			String s = "[";
			for (int i = 0; i < listEntry.size() - 1; i++) {
				s += listEntry.get(i).getKey() + "(" + listEntry.get(i).getValue() + "), ";
			}
			s += listEntry.get(listEntry.size() - 1).getKey() + "(" + listEntry.get(listEntry.size() - 1).getValue() + ")]";
			return s;
		}
		
		public static String formatMap(Map<String, Integer> map) {
			List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
			return formatListEntry(entryList);
		}
		
		public static List<Map.Entry<String, Integer>> getPercentageOfMap(Map<String, Integer> map, int percentage) {
			if (map.size() == 0) return null;
			List<Map.Entry<String, Integer>> entryList = new ArrayList<>(map.entrySet());
			int nbToFetch = (int) (Math.floor((0.01 * percentage) * map.size()));
			// if percentage is too low to fetch one class, fetch only the first one
			if (nbToFetch == 0) {
				nbToFetch++;
			}
			
			return entryList.subList(0, nbToFetch);
		}
			
		public static boolean classHasMoreThanXMethods(CompilationUnit parse, int val) {
			return getNbMethodsInClass(parse) > val;
		}
			
		public static Map<String,Integer> getAllMethodsWithNbLinesOfCodeFromClass(CompilationUnit parse) {
			MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
			parse.accept(visitor);
			Map<String,Integer> map = new HashMap<>();
			for (MethodDeclaration method : visitor.getMethods()) {
				map.put(method.getName().toString(), getNbOfLinesForMethod(parse, method));
			}
			return map;
		}
			
		public static Map<String,Object> maxNumberOfParametersForMethod(CompilationUnit parse, Map<String,Object> oldMap) {
			MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
			parse.accept(visitor);
			Map<String,Object> map = new HashMap<>(oldMap);
			int maxParameters = (int) map.get("number");
			for (MethodDeclaration method : visitor.getMethods()) {
				if (method.parameters().size()> maxParameters) {
					map.put("name", method.getName());
					map.put("number", method.parameters().size());
				}
			}
			return map;
		}
		
		public static Map<String,Integer> showMethodInvocations(CompilationUnit parse) {
			MethodInvocationVisitor visitor = new MethodInvocationVisitor();
			parse.accept(visitor);
			Map<String,Integer> map = new HashMap<>();
			System.out.println("Methods : ");
			for (MethodInvocation method : visitor.getMethods()) {
				System.out.println(method.getName());
			}
			System.out.println("Super Methods : ");
			for (SuperMethodInvocation method : visitor.getSuperMethod()) {
				System.out.println(method.getName());
			}
			return map;
		}
		
		public static List<MethodDeclaration> getMethodDeclarations(CompilationUnit parse) {
			MethodDeclarationVisitor visitor = new MethodDeclarationVisitor();
			parse.accept(visitor);
			return visitor.getMethods();
		}
		
		public static List<MethodInvocation> getMethodInvocationsFromDeclarations(MethodDeclaration methodD) {
			MethodInvocationVisitor visitor = new MethodInvocationVisitor();
			methodD.accept(visitor);
			return visitor.getMethods();
		}
		
		public static void getMethodInvocationsFromDeclarations2(CompilationUnit parse) {

			MethodDeclarationVisitor visitor1 = new MethodDeclarationVisitor();
			parse.accept(visitor1);
			for (MethodDeclaration method : visitor1.getMethods()) {
				System.out.print("  -> " + method.getName());//+ " calls :");
				MethodInvocationVisitor visitor2 = new MethodInvocationVisitor();
				method.accept(visitor2);
				if (visitor2.getMethods().size() != 0)
					System.out.println(" calls :");
				else
					System.out.println();
				for (MethodInvocation methodInvocation : visitor2
						.getMethods()) {
					System.out.println("    -> " + methodInvocation.getName());

				}
			}
		}
}
