package com.aptasystems.dicewarepasswordgenerator;


import android.content.Context;

/**
 * This class uses the most popular Diceware list: Arnold Reinhold's, first
 * published in 1995. This list contains 7,776 words, equal to the number of
 * possible ordered rolls of five six-sided dice (7776=65), making it suitable
 * for using standard dice as a source of randomness.
 * More: http://world.std.com/~reinhold/diceware.html
 */
public class ReinholdDiceware extends Diceware {

    public ReinholdDiceware(Context context) {
        super(context);
    }

    @Override
    protected int getWordlistResource() {
        return R.raw.diceware_wordlist;
    }

    @Override
    protected int getDicePerWord() {
        return 5;
    }
}
