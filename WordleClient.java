import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStreamWriter;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import edu.princeton.cs.algs4.StdDraw;

public class WordleClient {
    private List<String> targetWords;
    private String fileName;
    private int wordSize;

    public WordleClient(String fileName, int wordSize) throws IOException{
        this.wordSize = wordSize;
        this.fileName = fileName;
        targetWords = readFile();
    }

    public List<String> readFile() throws IOException{
        List<String> targetWords = new ArrayList<>();
        File file = new File(fileName);
        try (BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String strLine = bReader.readLine();
            while (strLine != null){
                strLine = strLine.replaceAll("[^a-zA-Z]", "");
                if (strLine.length() > 0){
                    if (strLine.length() == wordSize){
                        targetWords.add(strLine.toLowerCase());
                    }
                }
                strLine = bReader.readLine();
            }
        }
        return targetWords;
    }

    public String[] getCharactersDecision(String search, String target){
        boolean[] isPresent = new boolean[26];
        for (int i = 0; i < search.length(); i++){
            for (int j = 0; j < target.length(); j++){
                if (search.charAt(i) == target.charAt(j)){
                    isPresent[search.charAt(i) - 'a'] = true;
                }
            }
        }

        StringBuilder stringBuilderPresent = new StringBuilder();
        for (int i = 0; i < 26; i++){
            if (isPresent[i]){
                if (stringBuilderPresent.length() > 0) stringBuilderPresent.append(" ");
                stringBuilderPresent.append((char)(i + 'a')+"");
            }
        }

        StringBuilder stringBuilderAbsent = new StringBuilder();
        for (char c: search.toCharArray()){
            if (!isPresent[c - 'a']){
                if (stringBuilderAbsent.length() > 0) stringBuilderAbsent.append(" ");
                stringBuilderAbsent.append(c+"");
                isPresent[c - 'a'] = true;
            }
        }
        return new String[]{stringBuilderPresent.toString(), stringBuilderAbsent.toString()};
    }

    public String getCorrectPositions(String search, String target){
        boolean[] position = new boolean[wordSize];
        for (int i = 0; i < wordSize; i++){
            if (search.charAt(i) == target.charAt(i)){
                position[i] = true;
            }
        }

        StringBuilder corPosition = new StringBuilder();
        for (int i = 0; i < wordSize; i++){
            if (position[i]){
                if (corPosition.length() > 0) corPosition.append(" ");
                corPosition.append(i);
            }
        }
        return corPosition.toString();
    }

    public int playGame(String word, String criteria) throws IOException{
        Wordle wordle = new Wordle(wordSize, fileName);
        int numberOfGuesses = 0;
        boolean flag = true;
        while (flag){
            if (criteria.equals("likelihood")){
                wordle.computeLikelihood();
            }else wordle.computeEntropy();
            numberOfGuesses++;
            String guessWord = wordle.guessWord(criteria);
            if (guessWord.equals(word)){
                flag = false;
                return numberOfGuesses;
            }
            wordle.removeWord(guessWord);
            String[] charDecision = getCharactersDecision(guessWord, word);
            String charPresent = charDecision[0];
            if (!charPresent.equals("")){
                for (char c: charPresent.toCharArray()){
                    if (c != ' ') wordle.updateVocab(c, true);
                }
            }

            String charAbsent = charDecision[1];
            if (!charAbsent.equals("")){
                for (char c: charAbsent.toCharArray()){
                    if (c != ' ') wordle.updateVocab(c, false);
                }
            }
            String positions = getCorrectPositions(guessWord, word);
            boolean[] corrPositions = new boolean[wordSize]; // correctPositions[i] = false, char shouldnt present
            if (!positions.equals("")){
                for (char c: positions.toCharArray()){
                    if (c != ' '){
                        int index = Integer.parseInt(c+"");
                        corrPositions[index] = true;
                        wordle.updateVocab(guessWord.charAt(index), index, false);
                    }
                }
            }
            // removing words having characters in wrong positions
            for (int i = 0; i < wordSize; i++){
                if (!corrPositions[i]){
                    char c = guessWord.charAt(i);
                    wordle.updateVocab(c, i, true);
                }
            }
        }
        return numberOfGuesses;
    }

    public Map<Integer, Integer> getFrequencyMap(String criteria) throws IOException{
        Map<Integer, Integer> freqMap = new HashMap<>();
        int count = 0;
        for (String word: targetWords){
            count++;
            int numberOfTrails = playGame(word, criteria);
            freqMap.put(numberOfTrails, freqMap.getOrDefault(numberOfTrails, 0) + 1);
            if (count % 100 == 0) System.out.println(count);
        }
        return freqMap;
    }

    public void plot_frequencies(Map<Integer, Integer> fMap){
        StdDraw.setPenColor(StdDraw.RED);
		StdDraw.setPenRadius(0.01);
        StdDraw.setXscale(0, fMap.size() + 1);
        StdDraw.setYscale(0, fMap.values().stream().mapToInt(Integer::intValue).max().getAsInt() + 1);
        for (int k: fMap.keySet()){
            StdDraw.point(k, fMap.get(k));
        }
    }

    public void writeFile(String outputFileName, Map<Integer, Integer> fMap) throws IOException{
        File file = new File(outputFileName);
        try (BufferedWriter bWriter = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(file)))) {
            bWriter.write("NumberOfGuesses" + " , " + "Frequency\n");
            for (int k: fMap.keySet()){
                bWriter.write(k + " , " + fMap.get(k));
                bWriter.newLine();
            }
            bWriter.flush();
        }
    }

    public static void main(String[] args) throws IOException{
        int wordSize = Integer.parseInt(args[0]);
        String criteria = args[1];
        WordleClient wordleClient = new WordleClient("words.txt", wordSize);
        Map<Integer, Integer> freqMap = wordleClient.getFrequencyMap(criteria);
        wordleClient.plot_frequencies(freqMap);
        wordleClient.writeFile("frequencyData_wordsize_" + wordSize + "_criteria_"+  criteria + ".csv", freqMap);
    }
}
