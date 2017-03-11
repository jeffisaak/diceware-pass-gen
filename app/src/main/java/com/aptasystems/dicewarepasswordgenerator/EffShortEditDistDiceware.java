package com.aptasystems.dicewarepasswordgenerator;


import android.content.Context;

public class EffShortEditDistDiceware extends Diceware {

    public EffShortEditDistDiceware(Context context) {
        super(context);
    }

    @Override
    protected int getWordlistResource() {
        return R.raw.eff_short_wordlist_2_0;
    }
}
