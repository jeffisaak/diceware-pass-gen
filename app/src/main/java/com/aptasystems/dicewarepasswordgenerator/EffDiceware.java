package com.aptasystems.dicewarepasswordgenerator;


import android.content.Context;

public class EffDiceware extends Diceware {

    public EffDiceware(Context context) {
        super(context);
    }

    @Override
    protected int getWordlistResource() {
        return R.raw.eff_large_wordlist;
    }
}
