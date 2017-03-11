package com.aptasystems.dicewarepasswordgenerator;


import android.content.Context;

public class EffShortDiceware extends Diceware {

    public EffShortDiceware(Context context) {
        super(context);
    }

    @Override
    protected int getWordlistResource() {
        return R.raw.eff_short_wordlist_1;
    }
}
