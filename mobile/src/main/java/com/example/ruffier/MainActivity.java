package com.example.ruffier;

import android.content.Intent;
import android.os.Bundle;
import android.view.KeyEvent;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;

import com.example.common.Patient;
import com.example.common.SQLiteDBHandler;
import com.example.testruffier.R;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

public class MainActivity extends AppCompatActivity implements View.OnClickListener, AdapterView.OnItemClickListener {

    private final String TAG = "MainActivity";

    Button searchB;
    EditText fname;
    EditText lname;

    // results list
    List<Patient> array = new ArrayList<>();
    ArrayAdapter<Patient> adapter;

    // used to get patients
    SQLiteDBHandler dbHandler;
    Patient p;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Toolbar tb = findViewById(R.id.main_toolbar);
        setSupportActionBar(tb);

        searchB = findViewById(R.id.searchB);
        fname = findViewById(R.id.fname);
        lname = findViewById(R.id.lname);

        lname.setOnEditorActionListener(new TextView.OnEditorActionListener() {
            @Override
            public boolean onEditorAction(TextView textView, int i, KeyEvent keyEvent) {
                if (i == EditorInfo.IME_ACTION_DONE) {
                    performAction();
                }
                return false;
            }
        });

        searchB.setOnClickListener(this);

        //Database test (sucess)
        dbHandler = new SQLiteDBHandler(this);

        adapter = new ArrayAdapter<>(this, R.layout.list_cell, array);

        ListView listView = (ListView) findViewById(R.id.list);
        listView.setAdapter(adapter);
        listView.setOnItemClickListener(this);
    }

    @Override
    protected void onResume() {
        showAll();
        super.onResume();
    }

    private void showAll() {
        array.clear();
        List<Patient> listPatients = dbHandler.getAllPatients();
        if (listPatients.size() != 0) {
            array.addAll(listPatients);
            adapter.notifyDataSetChanged();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();
    }

    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == R.id.add) {
            Intent intent = new Intent(this, AddPatientActivity.class);
            startActivity(intent);
            return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onClick(View view) {
        performAction();
    }

    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int i, long l) {
        Intent intent = new Intent(this, ViewPatientActivity.class);
        p = (Patient) adapterView.getItemAtPosition(i);
        intent.putExtra("PATIENT_ID", p.getId());

        startActivity(intent);
        System.out.println(p.getFirstname());
    }

    // hide the keyboard
    private void hideSoftKeyBoard() {
        InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);

        assert imm != null;
        if (imm.isAcceptingText()) { // verify if the soft keyboard is open
            imm.hideSoftInputFromWindow(Objects.requireNonNull(getCurrentFocus()).getWindowToken(), 0);
        }
    }

    public void performAction() {
        array.clear();
        boolean resultFound = false;

        // if both fields are filled
        if (!fname.getText().toString().equals("") && !lname.getText().toString().equals("")) {
            Patient p = dbHandler.getPatient(fname.getText().toString(), lname.getText().toString());
            if (p.getId() != 0) {
                resultFound = true;
                array.add(p);
                adapter.notifyDataSetChanged();
            }
        } else if (lname.getText().toString().equals("") && !fname.getText().toString().equals("")) {

            // if firstname only is filled
            List<Patient> listPatients = dbHandler.getPatientsByFirstname(fname.getText().toString());
            if (listPatients.size() != 0) {
                resultFound = true;
                array.addAll(listPatients);
                adapter.notifyDataSetChanged();
            }
        } else if (fname.getText().toString().equals("") && !lname.getText().toString().equals("")) {

            // if lastname only is filled
            List<Patient> listPatients = dbHandler.getPatientsByLastname(lname.getText().toString());
            if (listPatients.size() != 0) {
                resultFound = true;
                array.addAll(listPatients);
                adapter.notifyDataSetChanged();
            }
        } else {
            resultFound = true;
            showAll();
            //Toast.makeText(this, "Veuillez remplir les champs", Toast.LENGTH_SHORT).show();
        }
        if (!resultFound) {
            Toast.makeText(this, "Aucun résultat trouvé", Toast.LENGTH_SHORT).show();
        }
        hideSoftKeyBoard();
    }
}
