package com.aptasystems.dicewarepasswordgenerator;


import android.content.Context;

public class ReinholdDiceware extends Diceware {

    public ReinholdDiceware(Context context) {
        super(context);
    }

    @Override
    protected int getWordlistResource() {
        return R.raw.diceware_wordlist;
    }
}
