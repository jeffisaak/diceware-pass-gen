package com.aptasystems.dicewarepasswordgenerator;


import android.content.Context;

public class EffLongDiceware extends Diceware {

    public EffLongDiceware(Context context) {
        super(context);
    }

    @Override
    protected int getWordlistResource() {
        return R.raw.eff_large_wordlist;
    }
}
