package com.aptasystems.dicewarepasswordgenerator;


import android.content.Context;

/**
 * This class uses the EFF "long" word list that matches the original Diceware
 * list (Reinhold's) in size (7,776 words (65)), offering equivalent security
 * for each word you choose. However, they have made some changes, resulting in
 * a list that is hopefully easy to type and remember.
 * More: https://www.eff.org/deeplinks/2016/07/new-wordlists-random-passphrases
 */
public class EffLongDiceware extends Diceware {

    public EffLongDiceware(Context context) {
        super(context);
    }

    @Override
    protected int getWordlistResource() {
        return R.raw.eff_large_wordlist;
    }

    @Override
    protected int getDicePerWord() {
        return 5;
    }
}
