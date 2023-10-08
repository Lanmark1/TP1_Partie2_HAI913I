package main;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;

import util.StatisticsHelper;

public class Parser {

	public static void main(String[] args) throws IOException {
		System.out.println("    HAI913I - TP1");
		System.out.println("Please enter a project folder name to be analyzed "
				+ "(must be present in the projectsToParse folder) \nOR just press enter to analyze this project");
		// Partie 1
		BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
		String s = br.readLine();
		StatisticsHelper.showStatistics(s);			
		// Partie 2
		System.out.println();
		StatisticsHelper.showCallGraph(s);
	}
}
