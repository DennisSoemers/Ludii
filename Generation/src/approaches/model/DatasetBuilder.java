package approaches.model;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import approaches.model.TokenizationParameters.NumericTokenType;
import game.Game;
import main.grammar.Symbol;
import other.GameLoader;

public class DatasetBuilder {

	public static void main(String[] args) {
		String ludiiRooot = "../Common/res/lud/board";
		String datasetRoot = "../../Ludii Evaluation Dataset/Small Board Games";
		int maxSize = 512;
		TokenizationParameters parameters = TokenizationParameters.smallBoardGameParameters();
		
		Tokenizer tokenizer = new Tokenizer(parameters);
		Restorer restorer = new Restorer(parameters);

		List<File> ludiiFiles = new ArrayList<>();
		
		try {
			Files.find(Paths.get(ludiiRooot),
			           Integer.MAX_VALUE,
			           (filePath, fileAttr) -> fileAttr.isRegularFile() && filePath.toString().toLowerCase().endsWith(".lud"))
			        .forEach(path -> ludiiFiles.add(path.toFile()));
		} catch (IOException e) {
			// TODO Auto-generated catch block
			e.printStackTrace();
			return;
		}
		
		int tokenLimit = 512;

		
		for (File file: ludiiFiles) {
			System.out.println(file.getPath());
			
			try {

				Game originalGame = GameLoader.loadGameFromFile(file);
			
				try {
					List<Integer> tokens = tokenizer.tokenizeGame(originalGame);
					
					if (tokens.size() > tokenLimit) {
						System.out.println("Skip");
						continue;
					}
					
					
					
				} catch (RuntimeException e) {
					System.out.println("An exception occured while attempting to tokenize or restore" + file.getPath());
					System.out.println("---------------------------------------- # ----------------------------------------");
	
					e.printStackTrace();
					continue;
				}
			} catch (Exception e) {
				System.out.println("Bad file. Failed to compile " + file.getPath());
				System.out.println("---------------------------------------- # ----------------------------------------");
				continue;
			}			
		}
	}

}
