package com.attendme.offline;

import android.app.Activity;
import android.app.AlertDialog;
import android.os.Bundle;
import android.graphics.Color;
import android.graphics.Typeface;
import android.content.SharedPreferences;
import android.view.Gravity;
import android.view.View;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import java.time.LocalDate;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class MainActivity extends Activity {
    private SharedPreferences prefs;
    private LinearLayout root;
    private final List<String> subjects = new ArrayList<>();
    private final List<String> entries = new ArrayList<>();
    private final DateTimeFormatter dateFmt = DateTimeFormatter.ofPattern("dd MMM yyyy");
    private final DateTimeFormatter timeFmt = DateTimeFormatter.ofPattern("HH:mm");

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        prefs = getSharedPreferences("attendme", MODE_PRIVATE);
        loadData();
        if (subjects.isEmpty()) {
            subjects.addAll(Arrays.asList("Mathematics", "Physics", "Chemistry"));
            saveSubjects();
        }
        render();
        if (prefs.getString("name", "").trim().isEmpty()) {
            showNameDialog();
        }
    }

    private void loadData() {
        subjects.clear();
        entries.clear();
        String s = prefs.getString("subjects", "");
        if (!s.isEmpty()) subjects.addAll(Arrays.asList(s.split("\\n")));
        String e = prefs.getString("entries", "");
        if (!e.isEmpty()) entries.addAll(Arrays.asList(e.split("\\n")));
    }

    private void saveSubjects() {
        prefs.edit().putString("subjects", String.join("\n", subjects)).apply();
    }

    private void saveEntries() {
        prefs.edit().putString("entries", String.join("\n", entries)).apply();
    }

    private int dp(int n) {
        return Math.round(n * getResources().getDisplayMetrics().density);
    }

    private TextView text(String value, int size, boolean bold) {
        TextView t = new TextView(this);
        t.setText(value);
        t.setTextSize(size);
        t.setTextColor(Color.rgb(30, 30, 35));
        if (bold) t.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        return t;
    }

    private void render() {
        ScrollView scroll = new ScrollView(this);
        root = new LinearLayout(this);
        root.setOrientation(LinearLayout.VERTICAL);
        root.setPadding(dp(20), dp(24), dp(20), dp(30));
        root.setBackgroundColor(Color.rgb(247, 248, 252));
        scroll.addView(root);

        String name = prefs.getString("name", "User");
        TextView title = text("Hey, " + (name.isEmpty() ? "User" : name) + " 👋", 28, true);
        root.addView(title);

        TextView subtitle = text("AttendMe Offline • Your attendance stays on this device", 14, false);
        subtitle.setTextColor(Color.DKGRAY);
        subtitle.setPadding(0, dp(4), 0, dp(18));
        root.addView(subtitle);

        int present = 0, absent = 0, leave = 0;
        for (String row : entries) {
            String[] p = row.split("\\|", -1);
            if (p.length >= 4) {
                if (p[3].equals("Present")) present++;
                else if (p[3].equals("Absent")) absent++;
                else if (p[3].equals("Leave")) leave++;
            }
        }
        int counted = present + absent;
        int pct = counted == 0 ? 0 : Math.round(present * 100f / counted);

        LinearLayout card = new LinearLayout(this);
        card.setOrientation(LinearLayout.VERTICAL);
        card.setPadding(dp(20), dp(18), dp(20), dp(18));
        card.setBackgroundColor(Color.WHITE);
        TextView overall = text("Overall Attendance", 15, false);
        overall.setTextColor(Color.DKGRAY);
        card.addView(overall);
        TextView big = text(pct + "%", 44, true);
        big.setPadding(0, dp(6), 0, dp(4));
        card.addView(big);
        card.addView(text(present + " Present   •   " + absent + " Absent   •   " + leave + " Leave", 15, true));
        LinearLayout.LayoutParams cardLp = new LinearLayout.LayoutParams(-1, -2);
        cardLp.setMargins(0, 0, 0, dp(14));
        root.addView(card, cardLp);

        Button mark = new Button(this);
        mark.setText("✓  MARK ATTENDANCE");
        mark.setTextSize(16);
        mark.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        mark.setAllCaps(false);
        mark.setOnClickListener(v -> showMarkDialog());
        LinearLayout.LayoutParams btnLp = new LinearLayout.LayoutParams(-1, dp(58));
        btnLp.setMargins(0, 0, 0, dp(10));
        root.addView(mark, btnLp);

        Button manage = new Button(this);
        manage.setText("+  Add Subject / Category");
        manage.setAllCaps(false);
        manage.setOnClickListener(v -> showAddSubjectDialog());
        LinearLayout.LayoutParams smallBtnLp = new LinearLayout.LayoutParams(-1, dp(50));
        smallBtnLp.setMargins(0, 0, 0, dp(22));
        root.addView(manage, smallBtnLp);

        root.addView(text("Recent History", 22, true));
        if (entries.isEmpty()) {
            TextView empty = text("No attendance yet. Pehli entry maar bhai 😭", 15, false);
            empty.setPadding(0, dp(14), 0, 0);
            root.addView(empty);
        } else {
            int shown = 0;
            for (int i = entries.size() - 1; i >= 0 && shown < 30; i--, shown++) {
                String[] p = entries.get(i).split("\\|", -1);
                if (p.length < 5) continue;
                LinearLayout row = new LinearLayout(this);
                row.setOrientation(LinearLayout.VERTICAL);
                row.setPadding(dp(16), dp(14), dp(16), dp(14));
                row.setBackgroundColor(Color.WHITE);
                row.addView(text(p[2] + "  •  " + p[3], 17, true));
                TextView when = text(p[0] + " at " + p[1], 13, false);
                when.setTextColor(Color.DKGRAY);
                row.addView(when);
                if (!p[4].isEmpty()) row.addView(text(p[4], 14, false));
                LinearLayout.LayoutParams rowLp = new LinearLayout.LayoutParams(-1, -2);
                rowLp.setMargins(0, dp(10), 0, 0);
                root.addView(row, rowLp);
            }
        }

        TextView footer = text("\n100% offline • No account • No ads • No INTERNET permission", 12, false);
        footer.setTextColor(Color.GRAY);
        footer.setGravity(Gravity.CENTER);
        root.addView(footer);

        setContentView(scroll);
    }

    private void showNameDialog() {
        EditText input = new EditText(this);
        input.setHint("Your name");
        input.setSingleLine(true);
        input.setPadding(dp(16), dp(8), dp(16), dp(8));
        new AlertDialog.Builder(this)
                .setTitle("Welcome to AttendMe Offline")
                .setMessage("Naam daal, account-shaccount kuch nahi banega 😎")
                .setView(input)
                .setCancelable(false)
                .setPositiveButton("Start", (d, w) -> {
                    String n = input.getText().toString().trim();
                    prefs.edit().putString("name", n.isEmpty() ? "User" : n).apply();
                    render();
                })
                .show();
    }

    private void showAddSubjectDialog() {
        EditText input = new EditText(this);
        input.setHint("e.g. English / Office / Project Alpha");
        new AlertDialog.Builder(this)
                .setTitle("Add subject/category")
                .setView(input)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Add", (d, w) -> {
                    String name = input.getText().toString().trim().replace("\n", " ");
                    if (!name.isEmpty() && !subjects.contains(name)) {
                        subjects.add(name);
                        saveSubjects();
                        Toast.makeText(this, name + " added", Toast.LENGTH_SHORT).show();
                        render();
                    }
                })
                .show();
    }

    private void showMarkDialog() {
        LinearLayout box = new LinearLayout(this);
        box.setOrientation(LinearLayout.VERTICAL);
        box.setPadding(dp(20), dp(6), dp(20), 0);

        Spinner subjectSpinner = new Spinner(this);
        ArrayAdapter<String> subjectAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, subjects);
        subjectSpinner.setAdapter(subjectAdapter);
        box.addView(subjectSpinner);

        Spinner statusSpinner = new Spinner(this);
        List<String> statuses = Arrays.asList("Present", "Absent", "Leave");
        ArrayAdapter<String> statusAdapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_dropdown_item, statuses);
        statusSpinner.setAdapter(statusAdapter);
        LinearLayout.LayoutParams statusLp = new LinearLayout.LayoutParams(-1, -2);
        statusLp.setMargins(0, dp(10), 0, 0);
        box.addView(statusSpinner, statusLp);

        EditText note = new EditText(this);
        note.setHint("Optional note");
        note.setSingleLine(false);
        LinearLayout.LayoutParams noteLp = new LinearLayout.LayoutParams(-1, -2);
        noteLp.setMargins(0, dp(10), 0, 0);
        box.addView(note, noteLp);

        new AlertDialog.Builder(this)
                .setTitle("Mark attendance")
                .setView(box)
                .setNegativeButton("Cancel", null)
                .setPositiveButton("Save", (d, w) -> {
                    String date = LocalDate.now().format(dateFmt);
                    String time = LocalTime.now().format(timeFmt);
                    String subject = String.valueOf(subjectSpinner.getSelectedItem()).replace("|", "/");
                    String status = String.valueOf(statusSpinner.getSelectedItem());
                    String n = note.getText().toString().trim().replace("|", "/").replace("\n", " ");
                    entries.add(date + "|" + time + "|" + subject + "|" + status + "|" + n);
                    saveEntries();
                    Toast.makeText(this, "Attendance marked ✓", Toast.LENGTH_SHORT).show();
                    render();
                })
                .show();
    }
}
