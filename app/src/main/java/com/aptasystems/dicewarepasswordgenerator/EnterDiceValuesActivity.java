package com.aptasystems.dicewarepasswordgenerator;

import android.app.Activity;
import android.content.Intent;
import android.content.res.Resources;
import android.os.Bundle;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;

public class EnterDiceValuesActivity extends AppCompatActivity {

    public static final String EXTRA_REQUIRED_ROLL_COUNT = "requiredRollCount";
    public static final String EXTRA_DIE_VALUES = "dieValues";

    private static final String STATE_DIE_VALUES = "dieValues";

    // Widgets.
    private TextView _dieRollStatusTextView;
    private TextView _dieRollInfoTextView;

    // State.
    private int _requiredRollCount;
    private List<Integer> _dieValues;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_enter_dice_values);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);

        _dieRollStatusTextView = (TextView) findViewById(R.id.text_view_die_roll_status);
        _dieRollInfoTextView = (TextView) findViewById(R.id.text_view_die_roll_info);

        // Get the number of required rolls from the intent.
        _requiredRollCount = getIntent().getIntExtra(EXTRA_REQUIRED_ROLL_COUNT, 0);

        Resources res = getResources();
        String text = String.format(res.getString(R.string.die_roll_info), _requiredRollCount);
        _dieRollInfoTextView.setText(text);

        if (savedInstanceState != null) {
            _dieValues = (List<Integer>) savedInstanceState.getSerializable(STATE_DIE_VALUES);
        }
        if (_dieValues == null) {
            _dieValues = new ArrayList<>();
        }

        updateStatusText();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_enter_dice_values, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == android.R.id.home) {
            onBackPressed();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putSerializable(STATE_DIE_VALUES, (Serializable) _dieValues);
    }

    public void onDieTap(View view) {
        switch (view.getId()) {
            case R.id.image_button_die_one:
                _dieValues.add(1);
                break;
            case R.id.image_button_die_two:
                _dieValues.add(2);
                break;
            case R.id.image_button_die_three:
                _dieValues.add(3);
                break;
            case R.id.image_button_die_four:
                _dieValues.add(4);
                break;
            case R.id.image_button_die_five:
                _dieValues.add(5);
                break;
            case R.id.image_button_die_six:
                _dieValues.add(6);
                break;
        }
        updateStatusText();

        // If we've tapped enough entries, finish the activity and set the result back.
        if (_requiredRollCount == _dieValues.size()) {
            Intent resultIntent = new Intent();
            resultIntent.putExtra(EXTRA_DIE_VALUES, (Serializable) _dieValues);
            setResult(Activity.RESULT_OK, resultIntent);
            finish();
        }
    }

    /**
     * Update the status text to show how many more die rolls are required.
     */
    private void updateStatusText() {
        int rollsRequired = _requiredRollCount - _dieValues.size();
        Resources res = getResources();
        String text = String.format(res.getString(R.string.die_roll_status), rollsRequired);
        _dieRollStatusTextView.setText(text);
    }

}
