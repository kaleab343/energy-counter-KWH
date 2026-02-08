package com.example.block;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.content.Context;
import android.provider.Settings;
import android.widget.TextView;
import android.widget.Toast;
import android.util.Log;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;
import com.google.android.material.textfield.TextInputLayout;
import android.widget.AutoCompleteTextView;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity extends AppCompatActivity {

    private static final String PREFS_NAME = "app_prefs";
    private static final String KEY_LANGUAGE = "language";

    private static final String KEY_BLOCK = "block";
    private static final String KEY_COUNT = "count";
    private static final String KEY_TARIF = "tarif";
    private static final String KEY_VAT = "vat";
    private static final String KEY_ADDITIONAL = "additional";
    private static final String KEY_PAYMENT_TEXT = "payment_text";

    private DatabaseHelper dbHelper;

    private TextInputEditText blockEditText, countEditText, tarifEditText;
    private TextInputLayout blockLayout, countLayout;
    private AutoCompleteTextView vatSpinner, additionalPaymentsSpinner;
    private MaterialButton btnClear, btnSubmit;
    private TextView paymentText;

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);

        // --- ANDROID_ID DEVICE CHECK START ---
        String androidId = Settings.Secure.getString(
                getContentResolver(),
                Settings.Secure.ANDROID_ID
        );

        Log.d("DEVICE_ID", "ANDROID_ID: " + androidId);

        String allowedId = "44a4d96ea087224c"; // replace with your device's ID

        if (!allowedId.equals(androidId)) {
            Toast.makeText(this, "This app cannot run on this device", Toast.LENGTH_LONG).show();
            finish();
            return;
        }
        // --- ANDROID_ID DEVICE CHECK END ---

        // Load saved language before setContentView
        String lang = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                .getString(KEY_LANGUAGE, "en");
        setAppLocale(lang);

        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);

        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Toolbar toolbar = findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayShowTitleEnabled(false);
        }

        TextView toolbarTitle = toolbar.findViewById(R.id.onlytext);
        toolbarTitle.setText(getString(R.string.block_management));

        dbHelper = new DatabaseHelper(this);

        blockEditText = findViewById(R.id.editText);
        countEditText = findViewById(R.id.count_);
        tarifEditText = findViewById(R.id.tarif_input);
        vatSpinner = findViewById(R.id.vat_spinner);
        additionalPaymentsSpinner = findViewById(R.id.additional_payments_spinner);
        paymentText = findViewById(R.id.payment_text);

        blockLayout = findViewById(R.id.block_input_layout);
        countLayout = findViewById(R.id.count_input_layout);

        btnClear = findViewById(R.id.btn_clear);
        btnSubmit = findViewById(R.id.btn_submit);

        blockLayout.setHint(getString(R.string.block_house_number));
        countLayout.setHint(getString(R.string.count));
        tarifEditText.setHint(getString(R.string.tarif));
        vatSpinner.setHint(getString(R.string.vat_percentage));
        additionalPaymentsSpinner.setHint(getString(R.string.additional_payments_birr));
        btnClear.setText(getString(R.string.clear));
        btnSubmit.setText(getString(R.string.submit));
        paymentText.setText(getString(R.string.payment));

        if (savedInstanceState != null) {
            blockEditText.setText(savedInstanceState.getString(KEY_BLOCK, ""));
            countEditText.setText(savedInstanceState.getString(KEY_COUNT, ""));
            tarifEditText.setText(savedInstanceState.getString(KEY_TARIF, ""));
            vatSpinner.setText(savedInstanceState.getString(KEY_VAT, ""), false);
            additionalPaymentsSpinner.setText(savedInstanceState.getString(KEY_ADDITIONAL, ""), false);
            paymentText.setText(savedInstanceState.getString(KEY_PAYMENT_TEXT, getString(R.string.payment)));
        }

        btnClear.setOnClickListener(v -> {
            blockEditText.setText("");
            countEditText.setText("");
            blockLayout.setError(null);
            countLayout.setError(null);
            paymentText.setText(getString(R.string.payment));
        });

        btnSubmit.setOnClickListener(v -> {
            blockLayout.setError(null);
            countLayout.setError(null);

            String block = blockEditText.getText().toString().trim();
            String countStr = countEditText.getText().toString().trim();
            String tarifStr = tarifEditText.getText().toString().trim();
            String vatStr = vatSpinner.getText().toString().trim();
            String addPaymentStr = additionalPaymentsSpinner.getText().toString().trim();

            boolean valid = true;

            // ✅ Block validation with localized strings
            if (block.isEmpty()) {
                blockLayout.setError(getString(R.string.required));
                valid = false;
            } else if (!block.startsWith("355/")) {
                blockLayout.setError(getString(R.string.block_must_start));
                valid = false;
            } else {
                String afterPrefix = block.substring(4);
                try {
                    int number = Integer.parseInt(afterPrefix);
                    if (number < 1 || number > 66) {
                        blockLayout.setError(getString(R.string.block_range_error));
                        valid = false;
                    }
                } catch (NumberFormatException e) {
                    blockLayout.setError(getString(R.string.invalid_number_format));
                    valid = false;
                }
            }

            if (countStr.isEmpty()) {
                countLayout.setError(getString(R.string.required));
                valid = false;
            }
            if (tarifStr.isEmpty() || vatStr.isEmpty() || addPaymentStr.isEmpty()) {
                valid = false;
            }

            if (!valid) return;

            float count = Float.parseFloat(countStr);
            float tarif = Float.parseFloat(tarifStr);
            float vat = Float.parseFloat(vatStr);
            float addPayment = Float.parseFloat(addPaymentStr);

            float previousCount = dbHelper.getRecentCount(block);
            float usedCount = count - previousCount;
            if (usedCount < 0) usedCount = 0;

            float finalPayment = (usedCount * tarif)
                    + ((usedCount * tarif) * (vat / 100))
                    + addPayment;

            paymentText.setText(String.format(Locale.getDefault(),
                    "%s: %.2f %s", getString(R.string.payment), finalPayment, "Birr"));

            String currentDate = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new Date());
            String currentTime = new SimpleDateFormat("HH:mm:ss", Locale.getDefault()).format(new Date());

            dbHelper.insertData(block, count, vat, addPayment, finalPayment, tarif, currentDate, currentTime);
        });
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_BLOCK, blockEditText.getText().toString());
        outState.putString(KEY_COUNT, countEditText.getText().toString());
        outState.putString(KEY_TARIF, tarifEditText.getText().toString());
        outState.putString(KEY_VAT, vatSpinner.getText().toString());
        outState.putString(KEY_ADDITIONAL, additionalPaymentsSpinner.getText().toString());
        outState.putString(KEY_PAYMENT_TEXT, paymentText.getText().toString());
    }

    @Override
    protected void attachBaseContext(Context newBase) {
        SharedPreferences prefs = newBase.getSharedPreferences(PREFS_NAME, MODE_PRIVATE);
        String lang = prefs.getString(KEY_LANGUAGE, "en");
        super.attachBaseContext(LocaleHelper.setLocale(newBase, lang));
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_switch_language) {
            String currentLang = getSharedPreferences(PREFS_NAME, MODE_PRIVATE)
                    .getString(KEY_LANGUAGE, "en");
            if (currentLang.equals("en")) {
                setLocale("am"); // Amharic
            } else {
                setLocale("en"); // English
            }
            return true;
        } else if (id == R.id.action_history) {
            startActivity(new Intent(this, MainActivity2.class));
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    private void setLocale(String languageCode) {
        getSharedPreferences(PREFS_NAME, MODE_PRIVATE).edit()
                .putString(KEY_LANGUAGE, languageCode)
                .apply();
        recreate();
    }

    private void setAppLocale(String languageCode) {
        Locale locale = new Locale(languageCode);
        Locale.setDefault(locale);
        android.content.res.Configuration config = new android.content.res.Configuration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());
    }

    @Override
    public void onBackPressed() {
        finishAffinity();
    }
}
