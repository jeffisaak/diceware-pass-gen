package com.aptasystems.dicewarepasswordgenerator;

import android.content.Context;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * This abstract class creates Diceware passwords.  It looks for diceware wordlists in the raw folder.
 */
public abstract class Diceware {

    private Map<Integer, String> _wordMap;

    public Diceware(Context context) {

        InputStream inputStream = context.getResources().openRawResource(getWordlistResource());

        List<String> diceLines = new ArrayList<>();

        try {
            BufferedReader br = new BufferedReader(new InputStreamReader(inputStream));
            String line;
            while ((line = br.readLine()) != null) {
                diceLines.add(line);
            }
            br.close();
        } catch (IOException e) {
            // Problem reading the diceware wordlist.  Return.
            // Users of _wordMap will have to ensure that it is not null.
            return;
        }

        // Some diceware word lists are separated by tabs, some by single spaces.  Try both.
        String[] separators = new String[]{"\t", " "};
        String tokenSeparator = null;
        for (String separator : separators) {
            if (diceLines.get(0).contains(separator)) {
                tokenSeparator = separator;
                break;
            }
        }

        // Parse the file into our word map.
        _wordMap = new HashMap<>();
        for (String diceLine : diceLines) {
            String numberString = diceLine.split(tokenSeparator)[0];
            String word = diceLine.split(tokenSeparator)[1];
            _wordMap.put(Integer.parseInt(numberString), word);
        }

    }

    /**
     * Generate a Diceware password using the collection of integers as random numbers.
     *
     * @param randomNumbers
     * @param withSpaces
     * @return
     * @throws PasswordGenerationException
     */
    public String generatePassword(Collection<Integer> randomNumbers, boolean withSpaces) throws PasswordGenerationException {

        if (_wordMap == null) {
            throw new PasswordGenerationException("Unable to read word list file");
        }

        StringBuilder passwordBuilder = new StringBuilder();
        StringBuilder numberBuilder = new StringBuilder();
        for (Integer randomNumber : randomNumbers) {
            numberBuilder.append(randomNumber);
            if (numberBuilder.length() == getDicePerWord()) {
                if (withSpaces && passwordBuilder.length() > 0) {
                    passwordBuilder.append(" ");
                }
                String word = _wordMap.get(Integer.parseInt(numberBuilder.toString()));
                passwordBuilder.append(word);

                numberBuilder = new StringBuilder();
            }
        }
        return passwordBuilder.toString();
    }

    /**
     * Get the count of words in the word list.  This is typically 7,776.
     *
     * @return
     */
    public int getWordCount() {
        return _wordMap.keySet().size();
    }

    public static class PasswordGenerationException extends Exception {
        public PasswordGenerationException(Throwable cause) {
            super(cause);
        }

        public PasswordGenerationException(String message) {
            super(message);
        }
    }

    /**
     * Get the wordlist raw resource ID.
     *
     * @return
     */
    protected abstract int getWordlistResource();

    /**
     * Get the number of dice throws per word.
     *
     * @return
     */
    protected abstract int getDicePerWord();

}
