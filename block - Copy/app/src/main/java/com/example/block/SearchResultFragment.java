package com.example.block;

import android.database.Cursor;
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class SearchResultFragment extends Fragment {

    private static final String ARG_BLOCK = "block";
    private String block;
    private DatabaseHelper dbHelper;

    public static SearchResultFragment newInstance(String block) {
        SearchResultFragment fragment = new SearchResultFragment();
        Bundle args = new Bundle();
        args.putString(ARG_BLOCK, block);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            block = getArguments().getString(ARG_BLOCK);
        }
        dbHelper = new DatabaseHelper(requireContext());
    }

    @Nullable
    @Override
    public View onCreateView(
            @NonNull LayoutInflater inflater,
            @Nullable ViewGroup container,
            @Nullable Bundle savedInstanceState
    ) {
        ScrollView scrollView = new ScrollView(requireContext());
        scrollView.setLayoutParams(new ScrollView.LayoutParams(
                ScrollView.LayoutParams.MATCH_PARENT,
                ScrollView.LayoutParams.MATCH_PARENT
        ));

        LinearLayout rootLayout = new LinearLayout(requireContext());
        rootLayout.setOrientation(LinearLayout.VERTICAL);
        rootLayout.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        rootLayout.setPadding(16, 16, 16, 16);

        scrollView.addView(rootLayout);
        loadData(rootLayout);

        return scrollView;
    }

    private void loadData(LinearLayout rootLayout) {
        rootLayout.removeAllViews(); // clear old rows

        Cursor cursor = dbHelper.getRecordsByBlock(block);
        if (cursor != null && cursor.getCount() > 0) {
            while (cursor.moveToNext()) {
                final long id = cursor.getLong(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ID));
                final String houseNumber = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_HOUSE_NUMBER));
                final float energy = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ENERGY_COUNT));
                final float payment = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_FINAL_PAYMENT));
                final float tarif = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TARIF));
                final float vatPerc = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_VAT));
                final float additionalPayment = cursor.getFloat(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_ADDITIONAL_PAYMENT));
                final String date = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_DATE));
                final String time = cursor.getString(cursor.getColumnIndexOrThrow(DatabaseHelper.COLUMN_TIME));

                // --- NEW VAT CALCULATION USING PREVIOUS ENERGY ---
                double previousEnergy = dbHelper.getPreviousEnergy(houseNumber, date, time);
                double energyDiff = energy - previousEnergy;
                if (energyDiff < 0) energyDiff = 0; // avoid negative consumption

                double baseCharge = energyDiff * tarif;
                double vatAmount = (baseCharge * vatPerc) / 100.0;

                String vatDisplay = String.format(Locale.US, "%.1f(%.2f)", vatPerc, vatAmount);

                // Convert time to 12-hour format
                String time12 = formatTo12Hour(time);

                // Row layout
                LinearLayout rowLayout = new LinearLayout(requireContext());
                rowLayout.setOrientation(LinearLayout.HORIZONTAL);
                rowLayout.setGravity(Gravity.CENTER_VERTICAL);
                LinearLayout.LayoutParams rowParams = new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                );
                rowParams.setMargins(0, 8, 0, 8);
                rowLayout.setLayoutParams(rowParams);
                rowLayout.setPadding(16, 16, 16, 16);
                rowLayout.setBackgroundResource(android.R.color.darker_gray);

                // Data text
                TextView textView = new TextView(requireContext());
                textView.setLayoutParams(new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                ));
                textView.setTextSize(16);
                textView.setTextColor(getResources().getColor(android.R.color.black));

                textView.setText(
                        "የቤት ቁጥር: " + houseNumber +
                                "\nየሀይል መጠን: " + energy +
                                "\nታሪፍ: " + tarif +
                                "\nቫት: " + vatDisplay +
                                "\nወርሃዊ መዋጮ: " + additionalPayment +
                                "\nጠቅላላ ክፍያ: " + payment +
                                "\nቀን: " + date +
                                "\nሰአት: " + time12
                );

                // Delete icon
                ImageView deleteIcon = new ImageView(requireContext());
                deleteIcon.setLayoutParams(new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        LinearLayout.LayoutParams.WRAP_CONTENT
                ));
                deleteIcon.setImageResource(android.R.drawable.ic_delete);
                deleteIcon.setPadding(16, 0, 0, 0);

                deleteIcon.setOnClickListener(v -> {
                    boolean deleted = dbHelper.deleteRow(id);
                    if (deleted) {
                        Toast.makeText(requireContext(), "Deleted: " + houseNumber, Toast.LENGTH_SHORT).show();
                        rootLayout.removeView(rowLayout);

                        if (rootLayout.getChildCount() == 0) {
                            showEmptyView(rootLayout);
                        }
                    } else {
                        Toast.makeText(requireContext(), "Failed to delete: " + houseNumber, Toast.LENGTH_SHORT).show();
                    }
                });

                rowLayout.addView(textView);
                rowLayout.addView(deleteIcon);
                rootLayout.addView(rowLayout);
            }
            cursor.close();
        } else {
            showEmptyView(rootLayout);
        }
    }

    private void showEmptyView(LinearLayout rootLayout) {
        TextView emptyView = new TextView(requireContext());
        emptyView.setText("No records found");
        emptyView.setTextSize(16);
        emptyView.setGravity(Gravity.CENTER);
        rootLayout.addView(emptyView);
    }

    // Helper: convert 24-hour time to 12-hour time
    private String formatTo12Hour(String time24) {
        try {
            SimpleDateFormat sdf24 = new SimpleDateFormat("HH:mm", Locale.getDefault());
            SimpleDateFormat sdf12 = new SimpleDateFormat("hh:mm a", Locale.getDefault());
            Date date = sdf24.parse(time24);
            if (date != null) {
                return sdf12.format(date);
            }
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return time24; // fallback
    }
}
