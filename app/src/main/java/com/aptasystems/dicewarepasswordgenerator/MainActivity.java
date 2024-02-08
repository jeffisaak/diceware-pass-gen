package com.aptasystems.dicewarepasswordgenerator;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.annotation.NonNull;
import androidx.coordinatorlayout.widget.CoordinatorLayout;
import com.google.android.material.snackbar.Snackbar;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.RadioButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.android.volley.AuthFailureError;
import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;

import java.security.SecureRandom;
import java.text.NumberFormat;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_CODE_ENTER_DICE_VALUES = 1;
    private static final int DEFAULT_PASSWORD_LENGTH = 6;

    // One trillion guesses per second.
    private static final double NSA_GUESSES_PER_SECOND = 1000000000000.0;

    private static final String STATE_RANDOM_MECHANISM = "randomMechanism";
    private static final String STATE_PASSWORD_LENGTH = "passwordLength";
    private static final String STATE_PASSWORD = "password";
    private static final String STATE_WORDLIST_SOURCE = "wordlistSource";

    private static final String INTENT_RESULT_KEY_PASSWORD = "password";

    private static final String PREF_HIDE_CLIPBOARD_WARNING = "hideClipboardWarning";
    private static final String PREF_DEFAULT_SOURCE = "defaultSource";
    private static final String PREF_DEFAULT_PASSWORD_LENGTH = "defaultPasswordLength";
    private static final String PREF_DEFAULT_WORDLIST_SOURCE = "defaultWordlistSource";

    // Widgets.
    private CoordinatorLayout _coordinatorLayout;
    private TextView _passwordLengthInfo;
    private TextView _passwordLength;
    private RadioButton _androidPrngRadioButton;
    private RadioButton _randomOrgRadioButton;
    private RadioButton _diceRadioButton;
    private RadioButton _reinholdRadioButton;
    private RadioButton _effRadioButton;
    private RadioButton _effShortRadioButton;
    private RadioButton _effShortEditDistRadioButton;
    private SeekBar _passwordLengthSeekBar;
    private TextView _passwordTextView;
    private Button _copyToClipboardButton;
    private Button _useThisPasswordButton;
    private Diceware _diceware;

    private GeneratePasswordTask _generatePasswordTask;

    // Tracks whether we've just rotated the screen.  Gets set and unset in the beginning and end of onCreate().
    private boolean _justRotated = false;

    static {
        PRNGFixes.apply();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if (savedInstanceState != null) {
            _justRotated = true;
        }

        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Grab our widgets to use later.
        _coordinatorLayout = (CoordinatorLayout) findViewById(R.id.coordinator_layout);
        _passwordLengthInfo = (TextView) findViewById(R.id.text_view_password_length_info);
        _passwordLength = (TextView) findViewById(R.id.text_view_password_length);
        _androidPrngRadioButton = (RadioButton) findViewById(R.id.radio_android_prng);
        _randomOrgRadioButton = (RadioButton) findViewById(R.id.radio_random_org);
        _diceRadioButton = (RadioButton) findViewById(R.id.radio_dice);
        _reinholdRadioButton = (RadioButton) findViewById(R.id.radio_reinhold);
        _effRadioButton = (RadioButton) findViewById(R.id.radio_eff);
        _effShortRadioButton = (RadioButton) findViewById(R.id.radio_eff_short);
        _effShortEditDistRadioButton = (RadioButton) findViewById(R.id.radio_eff_short_2);
        _passwordLengthSeekBar = (SeekBar) findViewById(R.id.seek_bar_password_length);
        _passwordTextView = (TextView) findViewById(R.id.password_text_view);
        _copyToClipboardButton = (Button) findViewById(R.id.copy_to_clipboard);
        _useThisPasswordButton = (Button) findViewById(R.id.use_this_password);

        // Set visibility of the copy to clipboard button or the use this password button,
        // depending on whether we were called with startActivity or startActivityForResult.
        ComponentName callingActivity = getCallingActivity();
        if (callingActivity == null || callingActivity.getClassName().compareTo(MainActivity.class.getName()) == 0) {
            _useThisPasswordButton.setVisibility(View.GONE);
        } else {
            _copyToClipboardButton.setVisibility(View.GONE);
        }

        // Set up our seekbar listener.
        _passwordLengthSeekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {

                // If this flag is true, we're recreating the activity after a rotation.  In this case, don't do anything.
                if (_justRotated) {
                    return;
                }

                // Update the text that shows the brute-force estimate.
                updatePasswordLengthInfo();

                // Update the password length preference.
                getPreferences(MODE_PRIVATE).edit().putInt(PREF_DEFAULT_PASSWORD_LENGTH, progress + 1).commit();

                // Android PRNG: Generate a new password.
                // Random.org: Do nothing.
                // Dice: Show the 'no password' message and disable the clipboard/use button.
                if (_androidPrngRadioButton.isChecked()) {
                    newAndroidPassword();
                } else if (_diceRadioButton.isChecked()) {
                    _passwordTextView.setText(getResources().getString(R.string.no_password));
                    _copyToClipboardButton.setEnabled(false);
                    _useThisPasswordButton.setEnabled(false);
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                // Noop.
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                // Android PRNG: Do nothing; password is recreated in onProgressChanged().
                // Random.org: Generate a new password.
                // Dice: Do nothing; the "new password" button must be tapped.
                if (_randomOrgRadioButton.isChecked()) {
                    newRandomOrgPassword();
                }
            }
        });

        int radioButtonChecked = R.id.radio_android_prng;
        int passwordLength = DEFAULT_PASSWORD_LENGTH;
        int wordlistRadioButtonChecked = R.id.radio_reinhold;
        if (savedInstanceState != null) {

            // Handle restoration from a saved instance state: which radio button is checked and the selected password length.
            radioButtonChecked = savedInstanceState.getInt(STATE_RANDOM_MECHANISM, radioButtonChecked);
            passwordLength = savedInstanceState.getInt(STATE_PASSWORD_LENGTH, passwordLength);
            wordlistRadioButtonChecked = savedInstanceState.getInt(STATE_WORDLIST_SOURCE, wordlistRadioButtonChecked);

        } else {

            // If there was no saved instance state, attempt to get the default radio button
            // selection and password length from the preferences.
            radioButtonChecked = getPreferences(MODE_PRIVATE).getInt(PREF_DEFAULT_SOURCE, radioButtonChecked);
            passwordLength = getPreferences(MODE_PRIVATE).getInt(PREF_DEFAULT_PASSWORD_LENGTH, passwordLength);
            wordlistRadioButtonChecked = getPreferences(MODE_PRIVATE).getInt(PREF_DEFAULT_WORDLIST_SOURCE, wordlistRadioButtonChecked);
        }

        // It is possible for the saved button IDs to not match the buttons (due to app code changes).
        if (radioButtonChecked != R.id.radio_android_prng && radioButtonChecked != R.id.radio_dice && radioButtonChecked != R.id.radio_random_org) {
            // Default to Android PRNG.
            radioButtonChecked = R.id.radio_android_prng;
        }
        if (wordlistRadioButtonChecked != R.id.radio_reinhold && wordlistRadioButtonChecked != R.id.radio_eff
                && wordlistRadioButtonChecked != R.id.radio_eff_short && wordlistRadioButtonChecked != R.id.radio_eff_short_2) {
            // Default to Android wordlist.
            wordlistRadioButtonChecked = R.id.radio_reinhold;
        }

        // Check the appropriate radio button, set the random mechanism, and set the password length seek bar.
        ((RadioButton) findViewById(radioButtonChecked)).setChecked(true);
        ((RadioButton) findViewById(wordlistRadioButtonChecked)).setChecked(true);
        // set the wordlist source, this has to happen before setRandomMechanism()
        setWordlistSource(wordlistRadioButtonChecked);
        setRandomMechanism(radioButtonChecked);
        // the progress has to be set after the Diceware has been instantiated
        _passwordLengthSeekBar.setProgress(passwordLength - 1);

        // Restore the password if applicable.
        if (savedInstanceState != null) {
            String password = savedInstanceState.getString(STATE_PASSWORD);
            _passwordTextView.setText(password);
            _copyToClipboardButton.setEnabled(true);
            _useThisPasswordButton.setEnabled(true);
        }

        _justRotated = false;
    }

    @Override
    protected void onResume() {

        // Update the text that shows the brute-force estimate.
        updatePasswordLengthInfo();

        super.onResume();
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        int radioButtonChecked = 0;
        if (_androidPrngRadioButton.isChecked()) {
            radioButtonChecked = _androidPrngRadioButton.getId();
        } else if (_randomOrgRadioButton.isChecked()) {
            radioButtonChecked = _randomOrgRadioButton.getId();
        } else if (_diceRadioButton.isChecked()) {
            radioButtonChecked = _diceRadioButton.getId();
        }
        // get radio button id of selected wordlist
        int wordlistRadioButtonChecked = 0;
        if (_reinholdRadioButton.isChecked()) {
            wordlistRadioButtonChecked = _reinholdRadioButton.getId();
        } else if (_effRadioButton.isChecked()) {
            wordlistRadioButtonChecked = _effRadioButton.getId();
        } else if (_effShortRadioButton.isChecked()) {
            wordlistRadioButtonChecked = _effShortRadioButton.getId();
        } else if (_effShortEditDistRadioButton.isChecked()) {
            wordlistRadioButtonChecked = _effShortEditDistRadioButton.getId();
        }
        outState.putInt(STATE_RANDOM_MECHANISM, radioButtonChecked);
        outState.putInt(STATE_PASSWORD_LENGTH, _passwordLengthSeekBar.getProgress());
        outState.putString(STATE_PASSWORD, _passwordTextView.getText().toString());
        outState.putInt(STATE_WORDLIST_SOURCE, wordlistRadioButtonChecked);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_help) {
            Intent intent = new Intent(this, HelpActivity.class);
            startActivity(intent);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        if (requestCode == REQUEST_CODE_ENTER_DICE_VALUES) {

            if (resultCode == RESULT_OK) {

                final List<Integer> dieValues = (List<Integer>) data.getSerializableExtra(EnterDiceValuesActivity.EXTRA_DIE_VALUES);

                // Cancel any existing password generation task.
                if (_generatePasswordTask != null && !_generatePasswordTask.isCancelled()) {
                    _generatePasswordTask.cancel(true);
                }

                // Create a new password generation task to feed numbers from the enter dice values activity.
                _generatePasswordTask = new GeneratePasswordTask(this, _diceware) {
                    @Override
                    public void generateRandomNumbers(int count) {

                        for (Integer value : dieValues) {
                            _numberQueue.offer(value);
                        }
                    }

                    @Override
                    protected void onPostExecute(String s) {
                        if (!isCancelled() && s != null) {
                            _passwordTextView.setText(s);
                            _copyToClipboardButton.setEnabled(true);
                            _useThisPasswordButton.setEnabled(true);
                        } else if (s == null) {
                            _passwordTextView.setText(getResources().getText(R.string.password_gen_failed));
                        }
                        super.onPostExecute(s);
                    }
                };

                int length = _passwordLengthSeekBar.getProgress() + 1;
                _generatePasswordTask.execute(length);

            } else {

                // Noop.

            }
        }
    }

    /**
     * Copy the password to the clipboard.
     *
     * @param view
     */
    public void copyToClipboard(View view) {

        // Show a warning dialog if the user hasn't turned it off yet.
        final SharedPreferences prefs = getPreferences(MODE_PRIVATE);
        boolean hideClipboardWarning = prefs.getBoolean(PREF_HIDE_CLIPBOARD_WARNING, false);
        if (!hideClipboardWarning) {
            MaterialDialog dialog = new MaterialDialog.Builder(this)
                    .title(R.string.clipboard_warning_title)
                    .customView(R.layout.dialog_clipboard_warning, true)
                    .positiveText(R.string.clipboard_dialog_positive)
                    .negativeText(R.string.clipboard_dialog_negative)
                    .onPositive(new MaterialDialog.SingleButtonCallback() {
                        @Override
                        public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                            CheckBox checkBox = (CheckBox) dialog.getCustomView().findViewById(R.id.do_not_show_warning);
                            prefs.edit().putBoolean(PREF_HIDE_CLIPBOARD_WARNING, checkBox.isChecked()).commit();

                            copyToClipboard();
                        }
                    }).build();

            dialog.show();

        } else {
            copyToClipboard();
        }
    }

    private void copyToClipboard() {
        ClipboardManager clipboard = (ClipboardManager) getSystemService(CLIPBOARD_SERVICE);
        ClipData clip = ClipData.newPlainText("label", _passwordTextView.getText().toString());
        clipboard.setPrimaryClip(clip);

        Snackbar
                .make(_coordinatorLayout, R.string.copied_to_clipboard, Snackbar.LENGTH_SHORT).show();
    }

    /**
     * Put the password into the result intent and finish the activity.
     *
     * @param view
     */
    public void useThisPassword(View view) {
        Intent result = new Intent();
        result.putExtra(INTENT_RESULT_KEY_PASSWORD, _passwordTextView.getText().toString());
        setResult(RESULT_OK, result);
        finish();
    }

    /**
     * Update the text that shows the information about how long the password will take to brute
     * force.
     */
    private void updatePasswordLengthInfo() {

        int passwordLength = _passwordLengthSeekBar.getProgress() + 1;

        double wordCount = _diceware.getWordCount();
        double permutations = Math.pow(wordCount, (double) passwordLength);
        double timeForAllPermutations = permutations / NSA_GUESSES_PER_SECOND;

        // First time divisor is two, as we take half of the total time to brute force the
        // entire password space as an estimate.
        double[] timeDivisors = new double[]{2.0, 60.0, 60.0, 24.0, 365.25, 10.0, 10.0, 10.0, 1000.0, 1000.0, 1000.0, 1000.0};
        int[] timeStrings = new int[]{
                R.string.duration_seconds,
                R.string.duration_minutes,
                R.string.duration_hours,
                R.string.duration_days,
                R.string.duration_years,
                R.string.duration_decades,
                R.string.duration_centuries,
                R.string.duration_millennia,
                R.string.duration_thousand_millennia,
                R.string.duration_million_millennia,
                R.string.duration_billion_millennia,
                R.string.duration_trillion_millennia,};
        double unitThreshold = 1.0;
        double largestThreshold = 1000.0;
        double[] timeResults = new double[timeDivisors.length];

        double latestResult = timeForAllPermutations;
        for (int ii = 0; ii < timeDivisors.length; ii++) {
            timeResults[ii] = latestResult / timeDivisors[ii];
            latestResult = timeResults[ii];
        }

        Resources res = getResources();
        String text = null;

        // Case where we have more than the threshold of the largest unit.
        if (timeResults[timeResults.length - 1] >= largestThreshold) {
            text = res.getString(R.string.password_length_forever);
        }

        // Go through the time results and pick one if we didn't already.
        if (text == null) {
            for (int ii = timeDivisors.length - 1; ii >= 0; ii--) {
                if (timeResults[ii] >= unitThreshold) {
                    text = String.format(res.getString(R.string.password_length_segment), NumberFormat.getIntegerInstance().format((int) timeResults[ii]), res.getString(timeStrings[ii]));
                    break;
                }
            }
        }

        // Case where we have less than one of the smallest unit.
        if (timeResults[0] < unitThreshold) {
            text = res.getString(R.string.password_length_zero);
        }

        _passwordLength.setText(String.format(res.getString(R.string.password_length), text));
        _passwordLength.setVisibility(View.VISIBLE);
    }

    /**
     * Set the random mechanism (handles radio button clicks).
     *
     * @param view
     */
    public void setRandomMechanism(View view) {
        setRandomMechanism(view.getId());
    }

    private void setRandomMechanism(int viewId) {

        if (!_justRotated) {
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_DEFAULT_SOURCE, viewId).commit();
        }

        switch (viewId) {
            case R.id.radio_android_prng:
                if (!_justRotated) {
                    newAndroidPassword();
                }
                break;
            case R.id.radio_random_org:
                _copyToClipboardButton.setEnabled(false);
                _useThisPasswordButton.setEnabled(false);
                if (!_justRotated) {
                    newRandomOrgPassword();
                }
                break;
            case R.id.radio_dice:
                if (!_justRotated) {
                    _passwordTextView.setText(getResources().getString(R.string.no_password));
                    _copyToClipboardButton.setEnabled(false);
                    _useThisPasswordButton.setEnabled(false);
                }
                break;
        }
    }

    public void setWordlistSource(View view) {
        setWordlistSource(view.getId());
        // generate new password if radio button was used to change wordlist
        newPassword(null);
    }

    private void setWordlistSource(int viewId) {

        if (!_justRotated) {
            getPreferences(MODE_PRIVATE).edit().putInt(PREF_DEFAULT_WORDLIST_SOURCE, viewId).commit();
        }

        switch (viewId) {
            case R.id.radio_reinhold:
                _diceware = new ReinholdDiceware(this);
                break;
            case R.id.radio_eff:
                _diceware = new EffLongDiceware(this);
                break;
            case R.id.radio_eff_short:
                _diceware = new EffShortDiceware(this);
                break;
            case R.id.radio_eff_short_2:
                _diceware = new EffShortEditDistanceDiceware(this);
                break;
        }
        // Update the text that shows the brute-force estimate.
        updatePasswordLengthInfo();
    }

    /**
     * Handle the "new password" button tap.
     *
     * @param view
     */
    public void newPassword(View view) {
        if (_androidPrngRadioButton.isChecked()) {
            newAndroidPassword();
        } else if (_randomOrgRadioButton.isChecked()) {
            newRandomOrgPassword();
        } else if (_diceRadioButton.isChecked()) {
            newDicePassword();
        }
    }

    /**
     * Use the android pseudorandom number generator to generate a new Diceware password.
     */
    private void newAndroidPassword() {

        if (_generatePasswordTask != null && !_generatePasswordTask.isCancelled()) {
            _generatePasswordTask.cancel(true);
        }

        _generatePasswordTask = new GeneratePasswordTask(this, _diceware) {
            @Override
            public void generateRandomNumbers(int count) {

                SecureRandom secureRandom = new SecureRandom();
                for (int ii = 0; ii < count; ii++) {
                    _numberQueue.offer(secureRandom.nextInt(6) + 1);
                }
            }

            @Override
            protected void onPostExecute(String s) {
                if (!isCancelled() && s != null) {
                    _passwordTextView.setText(s);
                    _copyToClipboardButton.setEnabled(true);
                    _useThisPasswordButton.setEnabled(true);
                } else if (s == null) {
                    _passwordTextView.setText(getResources().getText(R.string.password_gen_failed));
                }
                super.onPostExecute(s);
            }
        };

        int length = _passwordLengthSeekBar.getProgress() + 1;
        _generatePasswordTask.execute(length);
    }

    /**
     * Use data from random.org to generate a new Diceware password.
     */
    private void newRandomOrgPassword() {

        _passwordTextView.setText(getResources().getString(R.string.random_org_fetching));
        _copyToClipboardButton.setEnabled(false);
        _useThisPasswordButton.setEnabled(false);

        if (_generatePasswordTask != null && !_generatePasswordTask.isCancelled()) {
            _generatePasswordTask.cancel(true);
        }

        _generatePasswordTask = new GeneratePasswordTask(this, _diceware) {
            @Override
            public void generateRandomNumbers(int count) {
                Resources res = getResources();
                String url = String.format(res.getString(R.string.random_org_url), count);

                RequestQueue queue = Volley.newRequestQueue(MainActivity.this);
                StringRequest stringRequest = new StringRequest(Request.Method.GET, url,
                        new Response.Listener<String>() {
                            @Override
                            public void onResponse(String response) {
                                String[] tokens = response.split("\n");
                                for (String token : tokens) {
                                    Integer integer = Integer.parseInt(token);
                                    _numberQueue.offer(integer);
                                }
                            }
                        }, new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        _passwordTextView.setText(getResources().getString(R.string.random_org_error));
                        _copyToClipboardButton.setEnabled(false);
                        _useThisPasswordButton.setEnabled(false);
                    }
                }) {
                    @Override
                    public Map<String, String> getHeaders() throws AuthFailureError {
                        // random.org has been responding with 503s if there is no user agent set here.
                        Map<String, String> headers = new HashMap<>();
                        headers.put("User-Agent", "volley/0");
                        return headers;
                    }
                };
                queue.add(stringRequest);
            }

            @Override
            protected void onPostExecute(String s) {
                if (!isCancelled() && s != null) {
                    _passwordTextView.setText(s);
                    _copyToClipboardButton.setEnabled(true);
                    _useThisPasswordButton.setEnabled(true);
                } else if (s == null) {
                    _passwordTextView.setText(getResources().getText(R.string.password_gen_failed));
                }
                super.onPostExecute(s);
            }
        };

        int length = _passwordLengthSeekBar.getProgress() + 1;
        _generatePasswordTask.execute(length);
    }

    /**
     * Start the activity to collect die rolls. When the activity is finished, those rolls will be
     * used to generate a new Dicware password.
     */
    private void newDicePassword() {
        // Jump over to the dice activity to collect the numbers.
        Intent intent = new Intent(this, EnterDiceValuesActivity.class);
        intent.putExtra(EnterDiceValuesActivity.EXTRA_REQUIRED_ROLL_COUNT, (_passwordLengthSeekBar.getProgress() + 1) * _diceware.getDicePerWord());
        startActivityForResult(intent, REQUEST_CODE_ENTER_DICE_VALUES);
    }

}


