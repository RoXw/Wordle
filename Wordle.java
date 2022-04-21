import java.util.List;
import java.util.Map;
import java.util.PriorityQueue;
import java.util.Stack;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.HashMap;
import java.util.LinkedList;

public class Wordle{
    private int sizeOfWords;
    private List<String> vocab;
    private char[] chars = {'a', 'b', 'c', 'd', 'e', 'f', 'g', 'h', 'i', 'j'
                            , 'k', 'l', 'm', 'n', 'o', 'p', 'q', 'r', 's', 't'
                            , 'u', 'v', 'w', 'x', 'y', 'z'};
    private int[][] positionFrequencies;
    private Map<String, Double> likelihoodMap;
    private Map<String, Double> entropyMap;

    public Wordle(int sizeOfWords){
        this.sizeOfWords = sizeOfWords;
        vocab = new LinkedList<>();
        generateVocab(new StringBuilder());
        positionFrequencies = new int[chars.length][this.sizeOfWords];
        computeFrequencies();
    }

    public Wordle(int sizeOfWords, String fileName) throws IOException{
        this.sizeOfWords = sizeOfWords;
        vocab = new LinkedList<>();
        readVocab(fileName);
        positionFrequencies = new int[chars.length][this.sizeOfWords];
        computeFrequencies();
    }

    private void readVocab(String fileName) throws IOException{
        File file = new File(fileName);
        try (BufferedReader bReader = new BufferedReader(new InputStreamReader(new FileInputStream(file)))) {
            String strLine = bReader.readLine();
            while (strLine != null){
                strLine = strLine.replaceAll("[^a-zA-Z]", "");
                if (strLine.length() > 0){
                    if (strLine.length() == this.sizeOfWords){
                        vocab.add(strLine.toLowerCase());
                    }
                }
                strLine = bReader.readLine();
            }
        }
    }

    private void generateVocab(StringBuilder word){
        if (word.length() == this.sizeOfWords){
            vocab.add(word.toString());
            return;
        }

        for (int i = 0; i < 26; i++){
            word.append(chars[i]);
            generateVocab(word);
            word.deleteCharAt(word.length()-1);
        }
    }

    private void computeFrequencies(){
        for (String word: vocab){
            for (int i = 0; i < word.length(); i++){
                positionFrequencies[word.charAt(i) - 'a'][i]++;
            }
        }
    }

    public void removeWord(String word){
        int i = 0;
        while (i < vocab.size()){
            String w = vocab.get(i);
            if (w.equals(word)){
                vocab.remove(i);
                for (int j = 0; j < word.length(); j++){
                    positionFrequencies[word.charAt(j) - 'a'][j]--;
                }
                break;
            }else{
                i++;
            }
        }
    }

    public void updateVocab(char c, boolean flag){
        // delete elements inplace of linkedlist
        // flag = true, 'c' must present in vocab, keep word containing c
        // flag = false, 'c' shouldn't be in vocab, remove word containing c
        int i = 0;
        while (i < vocab.size()){
            String word = vocab.get(i);
            boolean isPresent = false;
            for (char ch: word.toCharArray()){
                if (ch == c) {isPresent = true; break;}
            }
            // isPresent= T and flag = F, remove word 
            // iPresent = F and flag = T, remove word
            if (isPresent ^ flag){
                vocab.remove(i); // removing the word at index i
                for (int j = 0; j < word.length(); j++){
                    positionFrequencies[word.charAt(j) - 'a'][j]--;
                }
            }else {
                i++;
            }
        }
    }

    public void updateVocab(char c, int position, boolean flag){
        // if flag = false, character c should be at position 'poisition'
        // if flag = true, character c shouldn't be at position 'position'
        // modiying in position linkedlist java, removing elements is not good so within for loop 
        int i = 0;
        while (i < vocab.size()){
            String word = vocab.get(i);
            if (flag ^ word.charAt(position) == c){
                i++;
            }else{
                vocab.remove(i);
                for (int j = 0; j < word.length(); j++){
                    positionFrequencies[word.charAt(j) - 'a'][j]--;
                }
            }
        }
    }

    private double computeLikelihoodOfWord(String word){
        double logLikelihood = 0.0;
        for (int i = 0; i < word.length(); i++){
            logLikelihood -= Math.log(positionFrequencies[word.charAt(i) - 'a'][i]*1.0/vocab.size());
        }
        return logLikelihood;
    }

    public void computeLikelihood(){
        likelihoodMap =  new HashMap<>();
        for (String word: vocab){
            double likelihood = computeLikelihoodOfWord(word);
            likelihoodMap.put(word, likelihood);
        }
    }

    public String guessWordLikelihood(){
        Double currLikelihood = -1.0;
        String topGuess = "";
        for (String word: likelihoodMap.keySet()){
            if (currLikelihood.compareTo(likelihoodMap.get(word)) < 0){
                topGuess = word;
                currLikelihood = likelihoodMap.get(topGuess);
            }
        }
        return topGuess;
    }

    private double computeEntropyOfWord(String word){
        double entropy = 0.0;
        for (int i = 0; i < word.length(); i++){
            double prob = positionFrequencies[word.charAt(i) - 'a'][i]*1.0/vocab.size();
            entropy -= prob * Math.log(prob);
        }
        return entropy;
    }

    public void computeEntropy(){
        entropyMap = new HashMap<>();
        for (String word: vocab){
            double ic = computeEntropyOfWord(word);
            entropyMap.put(word, ic);
        }
    }

    public String guessWordEntropy(){
        Double entropy = -1.0;
        String topGuess = "";
        for (String word: entropyMap.keySet()){
            if (entropy.compareTo(entropyMap.get(word)) < 0){
                topGuess = word;
                entropy = entropyMap.get(topGuess);
            }
        }
        return topGuess;
    }

    public String guessWord(String criteria){
        if (criteria.equals("likelihood")){
            return guessWordLikelihood();
        }
        return guessWordEntropy();
    }

    public Stack<String> topKWords(int k){
        PriorityQueue<Node> pq = new PriorityQueue<>();
        for (String word: likelihoodMap.keySet()){
            if (pq.size() > k){
                Double minLikelihood = pq.peek().likelihood;
                if (minLikelihood.compareTo(likelihoodMap.get(word)) < 0){
                    pq.remove();
                    pq.add(new Node(word, likelihoodMap.get(word)));
                }
            }else {
                pq.add(new Node(word, likelihoodMap.get(word)));
            }
        }

        Stack<String> stack = new Stack<>();
        while (!pq.isEmpty()){
            stack.push(pq.remove().word);
        }
        // define your own stack iterator, JAVA stack iterator returns elements in random order from what was present in the stack.
        return stack; // when you pop words appear in highest to lowest order
    }

    public class Node implements Comparable<Node>{
        String word;
        Double likelihood;

        public int compareTo(Node other){
            return (this.likelihood.compareTo(other.likelihood));
        }

        public Node(String word, double likelihood){
            this.word = word;
            this.likelihood = likelihood;
        }
    }

    public void printPositionFrequencies(){
        for (int i = 0; i < 26; i++){
            for (int j = 0; j < sizeOfWords; j++){
                System.out.print(positionFrequencies[i][j] + " , ");
            }
            System.out.println();
        }
    }

    public void printVocab(){
        for (String word: vocab){
            System.out.println(word);
        }
    }

    public void playGame(String criteria) throws IOException{
        BufferedReader bReader = new BufferedReader(new InputStreamReader(System.in));
        boolean flag = true;
        int numberOfGuesses = 0;
        while (flag){
            if (criteria.equals("likelihood")){
                computeLikelihood(); // update likelihood based on current vocab
            }else{
                computeEntropy(); // update entropy based on current vocab
            }
            numberOfGuesses++;
            String suggestedWord = guessWord(criteria); // best guess based on prob
            if (suggestedWord.equals("")){
                flag = false;
                System.out.println("Word is not in the vocabulary");
            }else{
                System.out.println("Best guess: " + suggestedWord);
                System.out.println("Guessed it correct? Enter YES/NO, YES if the guess is correct");
                String line = bReader.readLine();
                if (line.equals("YES")){
                    System.out.println("Number of guesses: " + numberOfGuesses);
                    flag = false;
                }else if (line.equals("NO")) {
                    removeWord(suggestedWord);
                    // next 3 lines
                    // characters present
                    // characters not present
                    // positions if predicted correct
                    System.out.println("Characters Present," +
                                        "(enter each character followed by space)");
                    String charactersPresent = bReader.readLine();
                    if (!charactersPresent.equals("")){
                        for (char c: charactersPresent.toCharArray()){
                            if (c != ' ') updateVocab(c, true);
                        }
                    }
                    System.out.println("Characters Absent," + 
                                        "(enter each character followed by space)");
                    String charactersAbsent = bReader.readLine();
                    if (!charactersAbsent.equals("")){
                        for (char c: charactersAbsent.toCharArray()){
                            if (c != ' ') updateVocab(c, false);
                        }
                    }
                    System.out.println("Correct Positions, (enter the positions at which" +
                                        " characters are guessed correct 0-index");
                    String positions = bReader.readLine();
                    boolean[] correctPositions = new boolean[sizeOfWords]; // correctPositions[i] = false, char shouldnt present
                    if (!positions.equals("")){
                        for (char c: positions.toCharArray()){
                            if (c != ' '){
                                int index = Integer.parseInt(c+"");
                                correctPositions[index] = true;
                                updateVocab(suggestedWord.charAt(index), index, false);
                            }
                        }
                    }
                    // removing words having characters in wrong positions
                    for (int i = 0; i < sizeOfWords; i++){
                        if (!correctPositions[i]){
                            char c = suggestedWord.charAt(i);
                            updateVocab(c, i, true);
                        }
                    }
                }else {
                    throw new RuntimeException("Enter valid input, either YES/NO");
                }
            }
            
        }
    }

    public static void main(String[] args) throws IOException{
        int sizeOfWords = Integer.parseInt(args[0]);
        String criteria = args[1];
        Wordle worlde = new Wordle(sizeOfWords, "words.txt");
        worlde.playGame(criteria);
    }
}