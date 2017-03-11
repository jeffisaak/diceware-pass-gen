package com.aptasystems.dicewarepasswordgenerator;

import android.content.Context;
import android.os.AsyncTask;
import android.widget.TextView;

import java.util.List;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Task to generate passwords. The generateRandomNumbers() abstract method must be implemented in
 * subclasses (most likely anonymously).  This method must feed random numbers to the task using the
 * number queue using _numberQueue.offer().
 */
public abstract class GeneratePasswordTask extends AsyncTask<Integer, Void, String> {

    private Context _context;
    private Diceware _diceware;
    protected Queue<Integer> _numberQueue;

    public GeneratePasswordTask(Context context, Diceware diceware) {
        _context = context;
        _numberQueue = new ConcurrentLinkedQueue<>();
        _diceware = diceware;
    }

    @Override
    protected String doInBackground(Integer... params) {

        int numberCount = params[0] * Diceware.DICE_PER_WORD;
        generateRandomNumbers(numberCount);

        if (isCancelled()) {
            return null;
        }

        // Wait for the queue to fill.
        while (_numberQueue.size() < numberCount) {
            try {
                Thread.sleep(50);

                if (isCancelled()) {
                    return null;
                }

            } catch (InterruptedException e) {
                // This is probably because we tried to cancel the task.
                return null;
            }
        }

        if (isCancelled()) {
            return null;
        }

        // Generate the password.
        String result = null;
        try {
            result = _diceware.generatePassword(_numberQueue, true);
        } catch (Diceware.PasswordGenerationException e) {
            // Password generation failed.
            return null;
        }

        return result;
    }

    /**
     * Generate random numbers.  This method must offer random numbers to _numberQueue.  Once the
     * queue is full the doInBackground() method will pull them out of the queue and generate the
     * password.  This mechanism allows for asynchronous random number generation (for example,
     * fetching random data from random.org).
     *
     * @param count
     */
    public abstract void generateRandomNumbers(int count);
}
