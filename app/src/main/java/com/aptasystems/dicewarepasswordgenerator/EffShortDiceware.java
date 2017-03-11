package com.aptasystems.dicewarepasswordgenerator;


import android.content.Context;

/**
 * This class uses one of the EFF "short" word list which is designed to include
 * the 1,296 most memorable and distinct words in the hope that this approach
 * might offer a usability improvement for longer passphrases. Passphrases
 * generated using the shorter lists will be weaker than the long list on a
 * per-word basis (10.3 bits/word).  Put another way, this means you would
 * need to choose more words from the short list, to get comparable security
 * to the long list—for example, using eight words from the short will provide
 * a strength of about 82 bits, slightly stronger than six words from the long
 * list. Further study is needed to determine conclusively which list will
 * yield passphrases that are easier to remember.
 * More: https://www.eff.org/deeplinks/2016/07/new-wordlists-random-passphrases
 */
public class EffShortDiceware extends Diceware {

    public EffShortDiceware(Context context) {
        super(context);
    }

    @Override
    protected int getWordlistResource() {
        return R.raw.eff_short_wordlist_1;
    }

    @Override
    protected int getDicePerWord() {
        return 4;
    }
}
