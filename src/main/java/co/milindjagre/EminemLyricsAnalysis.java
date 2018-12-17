package co.milindjagre;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import opennlp.tools.doccat.DoccatModel;
import opennlp.tools.doccat.DocumentCategorizerME;
import opennlp.tools.doccat.DocumentSample;
import opennlp.tools.doccat.DocumentSampleStream;
import opennlp.tools.namefind.NameFinderME;
import opennlp.tools.namefind.TokenNameFinderModel;
import opennlp.tools.sentdetect.SentenceDetectorME;
import opennlp.tools.sentdetect.SentenceModel;
import opennlp.tools.tokenize.SimpleTokenizer;
import opennlp.tools.tokenize.Tokenizer;
import opennlp.tools.util.InvalidFormatException;
import opennlp.tools.util.ObjectStream;
import opennlp.tools.util.PlainTextByLineStream;
import opennlp.tools.util.Span;

public class EminemLyricsAnalysis {

	/**
	 * @param args
	 */

	public static int getNumberOfSentences(SentenceModel sentenceModel,
			String input) throws IOException {
		SentenceDetectorME detector = new SentenceDetectorME(sentenceModel);
		String sentences[] = detector.sentDetect(input);
		return sentences.length;
	}

	public static int classifyNewText(DoccatModel sentimentModel, String input)
			throws IOException {
		DocumentCategorizerME myCategorizer = new DocumentCategorizerME(
				sentimentModel);
		double[] outcomes = myCategorizer.categorize(input);
		return Integer.parseInt(myCategorizer.getBestCategory(outcomes));
	}

	public static List<String> getPositiveWords() throws IOException {
		List<String> outputList = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(
				"C:\\positive-words.txt"));
		String line = null;
		while ((line = br.readLine()) != null) {
			outputList.add(line);
		}
		br.close();
		return outputList;
	}

	public static List<String> getNegativeWords() throws IOException {
		List<String> outputList = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(
				"C:\\negative-words.txt"));
		String line = null;
		while ((line = br.readLine()) != null) {
			outputList.add(line);
		}
		br.close();
		return outputList;
	}

	public static List<String> getPeople(String sentence) {
		TokenNameFinderModel model = null;
		try {
			model = new TokenNameFinderModel(new File("C:\\en-ner-person.bin"));
		} catch (InvalidFormatException e) {
			e.printStackTrace();
		} catch (IOException e) {
			e.printStackTrace();
		}
		NameFinderME finder = new NameFinderME(model);
		Tokenizer tokenizer = SimpleTokenizer.INSTANCE;
		String[] tokens = tokenizer.tokenize(sentence);
		Span[] nameSpans = finder.find(tokens);
		List<String> peopleNames = new ArrayList<String>();
		String[] spanns = Span.spansToStrings(nameSpans, tokens);
		for (int i = 0; i < spanns.length; i++) {
			peopleNames.add(spanns[i]);
		}
		finder.clearAdaptiveData();
		return peopleNames;
	}

	public static List<String> getStopWords() throws IOException {
		List<String> outputList = new ArrayList<String>();
		BufferedReader br = new BufferedReader(new FileReader(
				"C:\\nlp_en_stop_words.txt"));
		String line = null;
		while ((line = br.readLine()) != null) {
			outputList.add(line);
		}
		br.close();
		return outputList;
	}

	public static LinkedHashMap<String, Integer> sortHashMapByValues(
			Map<String, Integer> wordCountMap) {
		List<String> mapKeys = new ArrayList<String>(wordCountMap.keySet());
		List<Integer> mapValues = new ArrayList<Integer>(wordCountMap.values());
		Collections.sort(mapValues, Collections.reverseOrder());
		Collections.sort(mapKeys);
		LinkedHashMap<String, Integer> sortedMap = new LinkedHashMap<String, Integer>();
		Iterator<Integer> valueIt = mapValues.iterator();
		while (valueIt.hasNext()) {
			Integer val = valueIt.next();
			Iterator<String> keyIt = mapKeys.iterator();

			while (keyIt.hasNext()) {
				String key = keyIt.next();
				Integer comp1 = wordCountMap.get(key);
				Integer comp2 = val;

				if (comp1.equals(comp2)) {
					keyIt.remove();
					sortedMap.put(key, val);
					break;
				}
			}
		}
		return sortedMap;
	}

	@SuppressWarnings("deprecation")
	public static void main(String[] args) throws IOException {
		// TODO Auto-generated method stub
		String[] inputFilePathArray = new String[4];
		inputFilePathArray[0] = "C:\\input1.txt";
		inputFilePathArray[1] = "C:\\input2.txt";
		inputFilePathArray[2] = "C:\\input3.txt";
		inputFilePathArray[3] = "C:\\input4.txt";
		Map<String, Integer> wordCountMap = new HashMap<String, Integer>();
		Map<String, Integer> personNamesMap = new HashMap<String, Integer>();
		List<String> stopWordsList = getStopWords();
		List<String> globalPositiveWordsList = getPositiveWords();
		Map<String, Integer> positiveWordsMap = new HashMap<String, Integer>();
		List<String> globalNegativeWordsList = getNegativeWords();
		Map<String, Integer> negativeWordsMap = new HashMap<String, Integer>();
		List<String> peopleNamesList = new ArrayList<String>();
		InputStream dataIn = new FileInputStream("C:\\tweets.txt");
		ObjectStream<String> lineStream = new PlainTextByLineStream(dataIn,
				"UTF-8");
		ObjectStream<DocumentSample> sampleStream = new DocumentSampleStream(
				lineStream);
		DoccatModel sentimentModel = DocumentCategorizerME.train("en",
				sampleStream);

		InputStream inputStream = new FileInputStream("C:\\en-sent.bin");
		SentenceModel sentenceModel = new SentenceModel(inputStream);
		int positiveCounts = 0, negativeCounts = 0, sentenceCounts = 0;
		for (String inputFilePath : inputFilePathArray) {
			BufferedReader br = new BufferedReader(
					new FileReader(inputFilePath));
			String line = null, mapKey = null;
			String[] lineSplitter = null;
			int i = 0;
			while ((line = br.readLine()) != null) {
				peopleNamesList.addAll(getPeople(line));
				if (classifyNewText(sentimentModel, line) == 1)
					positiveCounts++;
				if (classifyNewText(sentimentModel, line) == 0)
					negativeCounts++;
				lineSplitter = line.split(" ");
				for (i = 0; i < lineSplitter.length; i++) {
					mapKey = lineSplitter[i].replaceAll("[\\.\\',\\?]", "");
					if (!(stopWordsList.contains(mapKey))
							&& mapKey.length() > 3) {
						if (wordCountMap.containsKey(mapKey))
							wordCountMap.put(mapKey,
									wordCountMap.get(mapKey) + 1);
						else
							wordCountMap.put(mapKey, 1);
					}

					if (globalPositiveWordsList.contains(mapKey)) {
						if (positiveWordsMap.containsKey(mapKey))
							positiveWordsMap.put(mapKey,
									positiveWordsMap.get(mapKey) + 1);
						else
							positiveWordsMap.put(mapKey, 1);
					}
					if (globalNegativeWordsList.contains(mapKey)) {
						if (negativeWordsMap.containsKey(mapKey))
							negativeWordsMap.put(mapKey,
									negativeWordsMap.get(mapKey) + 1);
						else
							negativeWordsMap.put(mapKey, 1);
					}
				}
				sentenceCounts += getNumberOfSentences(sentenceModel, line);
			}
			br.close();
		}
		LinkedHashMap<String, Integer> sortedWordCountMap = sortHashMapByValues(wordCountMap);
		System.out.println("***TOP 10 MOST USED WORDS***");
		int count = 0;
		for (Entry<String, Integer> entry : sortedWordCountMap.entrySet()) {
			if (count < 10)
				System.out.println(entry.getKey() + " - " + entry.getValue());
			count++;
		}
		System.out
				.println("\n----------------------------------------------------------------");
		System.out.println("***TOP 10 MOST USED PERSON NAMES***");
		count = 0;
		for (String person : peopleNamesList) {
			if (personNamesMap.containsKey(person))
				personNamesMap.put(person, personNamesMap.get(person) + 1);
			else
				personNamesMap.put(person, 1);
		}
		LinkedHashMap<String, Integer> sortedPersonNamesMap = sortHashMapByValues(personNamesMap);
		for (Entry<String, Integer> entry : sortedPersonNamesMap.entrySet()) {
			if (count < 10)
				System.out.println(entry.getKey() + " - " + entry.getValue());
			count++;
		}
		System.out
				.println("\n----------------------------------------------------------------");
		System.out.println("***POSITIVE WORDS***");
		count = 0;
		LinkedHashMap<String, Integer> sortedPositiveWordsMap = sortHashMapByValues(positiveWordsMap);
		for (Entry<String, Integer> entry : sortedPositiveWordsMap.entrySet()) {
			if (count < 10)
				System.out.println(entry.getKey() + " - " + entry.getValue());
			count++;
		}
		System.out
				.println("\n----------------------------------------------------------------");
		System.out.println("***NEGATIVE WORDS***");
		count = 0;
		LinkedHashMap<String, Integer> sortedNegativeWordsMap = sortHashMapByValues(negativeWordsMap);
		for (Entry<String, Integer> entry : sortedNegativeWordsMap.entrySet()) {
			if (count < 10)
				System.out.println(entry.getKey() + " - " + entry.getValue());
			count++;
		}
		System.out
				.println("\n----------------------------------------------------------------");
		System.out.println("***POSITIVE SENTIMENT COUNT***");
		System.out.print(positiveCounts);
		System.out
				.println("\n----------------------------------------------------------------");
		System.out.println("***NEGATIVE SENTIMENT COUNT***");
		System.out.print(negativeCounts);
		System.out
				.println("\n----------------------------------------------------------------");
		System.out.println("***NUMBER OF SENTENCES USED***");
		System.out.print(sentenceCounts);
	}
}
