package com.example.block;

import android.content.SharedPreferences;
import android.database.Cursor;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.pdf.PdfDocument;
import android.os.Bundle;
import android.os.Environment;
import android.text.TextUtils;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

import com.google.android.material.button.MaterialButton;
import com.google.android.material.textfield.TextInputEditText;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MainActivity2 extends AppCompatActivity {

    private TextInputEditText blockEditText;
    private MaterialButton searchButton;
    private ImageView downloadIcon;
    private SharedPreferences preferences;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // Load saved language
        preferences = getSharedPreferences("app_prefs", MODE_PRIVATE);
        String lang = preferences.getString("language", "en"); // default English
        Locale locale = new Locale(lang);
        Locale.setDefault(locale);
        android.content.res.Configuration config = getResources().getConfiguration();
        config.setLocale(locale);
        getResources().updateConfiguration(config, getResources().getDisplayMetrics());

        setContentView(R.layout.activity_main2);

        // Edge-to-edge padding
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            WindowInsetsCompat systemInsets = insets;
            v.setPadding(
                    systemInsets.getInsets(WindowInsetsCompat.Type.systemBars()).left,
                    systemInsets.getInsets(WindowInsetsCompat.Type.systemBars()).top,
                    systemInsets.getInsets(WindowInsetsCompat.Type.systemBars()).right,
                    systemInsets.getInsets(WindowInsetsCompat.Type.systemBars()).bottom
            );
            return insets;
        });

        // Toolbar & download icon
        downloadIcon = findViewById(R.id.downloadIcon);

        // Search UI
        blockEditText = findViewById(R.id.editText);
        searchButton = findViewById(R.id.searchButton);

        searchButton.setOnClickListener(v -> {
            String block = blockEditText.getText().toString().trim();
            if (TextUtils.isEmpty(block)) {
                blockEditText.setError(getString(R.string.required));
                return;
            }
            displayData(block);
        });

        // Download icon click
        downloadIcon.setOnClickListener(v -> {
            String block = blockEditText.getText().toString().trim();

            if (TextUtils.isEmpty(block)) {
                // Show date filter popup
                PopupMenu popupMenu = new PopupMenu(this, downloadIcon);
                popupMenu.getMenuInflater().inflate(R.menu.date, popupMenu.getMenu());
                popupMenu.setOnMenuItemClickListener(item -> {
                    int id = item.getItemId();
                    if (id == R.id.menu_day) {
                        exportDatabaseToPDF("day");
                    } else if (id == R.id.menu_week) {
                        exportDatabaseToPDF("week");
                    } else if (id == R.id.menu_month) {
                        exportDatabaseToPDF("month");
                    } else if (id == R.id.menu_year) {
                        exportDatabaseToPDF("year");
                    } else if (id == R.id.menu_all) {
                        exportDatabaseToPDF(null); // all data
                    }
                    return true;
                });
                popupMenu.show();
            } else {
                exportDatabaseToPDF(block); // export filtered by block
            }
        });
    }

    private void displayData(String block) {
        SearchResultFragment fragment = SearchResultFragment.newInstance(block);
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.resultFragmentContainer, fragment)
                .commit();
    }

    private void exportDatabaseToPDF(String filter) {
        DatabaseHelper dbHelper = new DatabaseHelper(this);
        Cursor cursor;

        if (filter == null) {
            cursor = dbHelper.getAllData();
        } else if (filter.equals("day") || filter.equals("week") || filter.equals("month") || filter.equals("year")) {
            cursor = dbHelper.getRecordsByDateFilter(filter);
        } else {
            cursor = dbHelper.getRecordsByBlock(filter);
        }

        if (cursor == null || cursor.getCount() == 0) {
            Toast.makeText(this, "No data found", Toast.LENGTH_LONG).show();
            return;
        }

        File exportDir = Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS);
        if (!exportDir.exists()) exportDir.mkdirs();

        String safeFilter = (filter == null) ? "all" : filter.replaceAll("[\\\\/:*?\"<>|]", "_");
        String timestamp = new SimpleDateFormat("yyyy-MM-dd_HH-mm-ss", Locale.getDefault()).format(new Date());
        String fileName = "property_data_" + safeFilter + "_" + timestamp + ".pdf";
        File file = new File(exportDir, fileName);

        PdfDocument pdfDocument = new PdfDocument();
        PdfDocument.PageInfo pageInfo = new PdfDocument.PageInfo.Builder(595, 842, 1).create();
        PdfDocument.Page page = pdfDocument.startPage(pageInfo);
        Canvas canvas = page.getCanvas();
        Paint paint = new Paint();
        Paint linePaint = new Paint();
        linePaint.setStrokeWidth(1f);

        int x = 10;
        int y = 40;
        int rowHeight = 40;

        String[] headers = {
                " የቤት ቁጥር ", " የሀይል መጠን ", " ታርፍ ", " ቫት ",
                " ወርሃዊ መዋጮ ", " ጠቅላላ ክፍያ ", " ቀን ", " ሰአት "
        };
        int[] colWidths = {70, 70, 60, 80, 90, 90, 70, 80};

        int xPos = x;
        paint.setFakeBoldText(true);
        for (int i = 0; i < headers.length; i++) {
            canvas.drawText(headers[i], xPos + 5, y, paint);
            xPos += colWidths[i];
        }
        canvas.drawLine(x, y + 10, x + getTotalWidth(colWidths), y + 10, linePaint);
        y += rowHeight;

        while (cursor.moveToNext()) {
            xPos = x;
            paint.setFakeBoldText(false);

            String houseNumber = cursor.getString(1);

            // --- parse numbers safely ---
            double currentEnergy = safeParse(cursor.getString(2));
            double tarifVal = safeParse(cursor.getString(6));
            double vatPercVal = safeParse(cursor.getString(3));

            // --- NEW VAT CALCULATION BASED ON ENERGY DIFFERENCE ---
            double previousEnergy = dbHelper.getPreviousEnergy(houseNumber, cursor.getString(7), cursor.getString(8));
            double energyDiff = currentEnergy - previousEnergy;
            if (energyDiff < 0) energyDiff = currentEnergy; // first record

            double baseCharge = energyDiff * tarifVal;
            double vatAmount = (baseCharge * vatPercVal) / 100.0;

            String vat = formatNumber(String.valueOf(vatPercVal)) + "(" + formatNumber(String.valueOf(vatAmount)) + ")";
            String power = formatNumber(String.valueOf(currentEnergy));
            String tarif = formatNumber(String.valueOf(tarifVal));
            String additional = formatNumber(cursor.getString(4));
            String total = formatNumber(cursor.getString(5));
            String date = cursor.getString(7);
            String time = cursor.getString(8);

            String[] rowData = {houseNumber, power, tarif, vat, additional, total, date, time};

            for (int i = 0; i < rowData.length; i++) {
                String value = rowData[i] != null ? rowData[i] : "";
                canvas.drawText(value, xPos + 5, y, paint);
                canvas.drawLine(xPos, y - rowHeight + 10, xPos, y + 10, linePaint);
                xPos += colWidths[i];
            }

            canvas.drawLine(xPos, y - rowHeight + 10, xPos, y + 10, linePaint);
            canvas.drawLine(x, y + 10, x + getTotalWidth(colWidths), y + 10, linePaint);
            y += rowHeight;

            if (y > pageInfo.getPageHeight() - 50) {
                pdfDocument.finishPage(page);
                page = pdfDocument.startPage(pageInfo);
                canvas = page.getCanvas();
                y = 40;
            }
        }

        pdfDocument.finishPage(page);

        try {
            pdfDocument.writeTo(new FileOutputStream(file));
            Toast.makeText(this, "PDF saved in Downloads:\n" + file.getAbsolutePath(), Toast.LENGTH_LONG).show();
        } catch (Exception e) {
            Toast.makeText(this, "Error exporting PDF: " + e.getMessage(), Toast.LENGTH_LONG).show();
        } finally {
            pdfDocument.close();
            cursor.close();
        }
    }

    private double safeParse(String value) {
        if (value == null || value.trim().isEmpty()) return 0.0;
        try {
            return Double.parseDouble(value.replaceAll(",", "").trim());
        } catch (Exception e) {
            return 0.0;
        }
    }

    private String formatNumber(String value) {
        if (value == null || value.trim().isEmpty()) return "0.0";
        try {
            double number = Double.parseDouble(value.replaceAll(",", "").trim());
            if (number == 0) {
                return "0.0";
            } else {
                String formatted = String.format(Locale.US, "%.2f", number);
                if (formatted.indexOf('.') > 0) {
                    formatted = formatted.replaceAll("0*$", "");
                    if (formatted.endsWith(".")) formatted += "0";
                }
                return formatted;
            }
        } catch (NumberFormatException e) {
            return value;
        }
    }

    private int getTotalWidth(int[] colWidths) {
        int total = 0;
        for (int w : colWidths) total += w;
        return total;
    }
}
