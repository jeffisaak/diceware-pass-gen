package com.aptasystems.dicewarepasswordgenerator;


import android.content.Context;

/**
 * This class uses one of the EFF "short" word list with a few additional features
 * making the words easy to type: Each word has a unique three-character prefix.
 * This means that future software could auto-complete words in the passphrase
 * after the user has typed the first three characters. Also, all words are at
 * least an edit distance of 3 apart. This means that future software could
 * correct any single typo in the user's passphrase (and in many cases more than
 * one typo). These features were added in the hope that they might be used by
 * software in the future that was specially designed to take advantage of them,
 * but will not offer a significant benefit today so this list is mostly a
 * proof-of-concept for individual users. Software developers might be able to
 * find interesting uses for this list.
 * More: https://www.eff.org/deeplinks/2016/07/new-wordlists-random-passphrases
 */
public class EffShortEditDistDiceware extends Diceware {

    public EffShortEditDistDiceware(Context context) {
        super(context);
    }

    @Override
    protected int getWordlistResource() {
        return R.raw.eff_short_wordlist_2_0;
    }

    @Override
    protected int getDicePerWord() {
        return 4;
    }
}
