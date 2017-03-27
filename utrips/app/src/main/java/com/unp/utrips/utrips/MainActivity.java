package com.unp.utrips.utrips;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableLayout.LayoutParams;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener{

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final int REQUEST_CONTACT_ASK_PERMISSIONS = 124;
    Button btnDatePickerFrom,btnDatePickerTo,btnSubmit,btnExport;
    EditText txtDateFrom,txtDateTo;
    private int mYear, mMonth, mDay;
    TableLayout tl;
    ScrollView sv;

    private StringBuilder report = new StringBuilder();
    File tempFile;

    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDatePickerFrom =(Button)findViewById(R.id.btn_date_from);
        btnDatePickerTo =(Button)findViewById(R.id.btn_date_to);
        btnSubmit =(Button)findViewById(R.id.btn_submit);
        btnExport =(Button)findViewById(R.id.btn_export);
        txtDateFrom =(EditText)findViewById(R.id.in_date);
        txtDateTo =(EditText)findViewById(R.id.out_date);
        tl = (TableLayout) findViewById(R.id.main_table);

        listDataChild = new HashMap<String, List<String>>();
        listDataHeader = new ArrayList<String>();

        btnDatePickerFrom.setOnClickListener(this);
        btnDatePickerTo.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);
        btnExport.setOnClickListener(this);
        btnExport.setVisibility(View.GONE);
    }

    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public void onClick(View v) {

        if (v == btnDatePickerFrom) {

            // Get Current Date
            final Calendar c = Calendar.getInstance();
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);


            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            if((month+1)/10>0){
                                txtDateFrom.setText(dayOfMonth + "-" + (month + 1) + "-" + year);
                            }else{
                                txtDateFrom.setText(dayOfMonth + "-0" + (month + 1) + "-" + year);
                            }
                            txtDateFrom.setError(null);
                        }

                    }, mYear, mMonth, mDay);
            datePickerDialog.show();
        }

        if (v == btnDatePickerTo) {

            // Get Current Date
            final Calendar c = Calendar.getInstance();
            mYear = c.get(Calendar.YEAR);
            mMonth = c.get(Calendar.MONTH);
            mDay = c.get(Calendar.DAY_OF_MONTH);


            DatePickerDialog datePickerDialog = new DatePickerDialog(this,
                    new DatePickerDialog.OnDateSetListener() {

                        @Override
                        public void onDateSet(DatePicker view, int year, int month, int dayOfMonth) {
                            if((month+1)/10>0){
                                txtDateTo.setText(dayOfMonth + "-" + (month + 1) + "-" + year);
                            }else{
                                txtDateTo.setText(dayOfMonth + "-0" + (month + 1) + "-" + year);
                            }
                            txtDateTo.setError(null);
                        }


                    }, mYear, mMonth, mDay);
            datePickerDialog.show();
        }

        if (v == btnSubmit) {

            Log.d("Submit","Pressed Submit");
            tl.removeAllViews();
            //int msgCount=0;

            if(txtDateFrom.getText().toString().trim().equals("")){
                Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
                txtDateFrom.startAnimation(shake);
                btnDatePickerFrom.startAnimation(shake);
                Toast.makeText(this, "Enter From", Toast.LENGTH_LONG).show();
                txtDateFrom.setError("Enter From");
                return;
            };

            if(txtDateTo.getText().toString().trim().equals("")){
                Animation shake = AnimationUtils.loadAnimation(this, R.anim.shake);
                txtDateTo.startAnimation(shake);
                btnDatePickerTo.startAnimation(shake);
                Toast.makeText(this, "Enter To", Toast.LENGTH_LONG).show();
                txtDateTo.setError("Enter To");
                return;
            };

            if(checkPermission_SMS()){
                workflow();
            }else{
                requestPermission_SMS();
            }

        }

        if(v==btnExport){
            //report="Vivek,Vivek";
            if(checkPermission()){
                emailEngine();
            }else{
                requestPermission();
            }

        }
    }

    private void workflow(){
        int msgCount=0;
        Uri inboxURI = Uri.parse("content://sms/inbox/");

        report = new StringBuilder();

        // List required columns
        String[] reqCols = new String[]{"_id", "address", "body", "date","person"};
        ContentResolver cr = getContentResolver();

        SimpleDateFormat df = new SimpleDateFormat("dd-MM-yyyy HH:mm");
        Date dateFrom = null;
        Date dateTo = null;
        try{
            dateFrom = df.parse(txtDateFrom.getText().toString()+" 00:00");
            dateTo = df.parse(txtDateTo.getText().toString()+" 23:59");
        }catch(Exception e){
            Toast.makeText(this, "Problem Identifying dates. Exiting...", Toast.LENGTH_LONG).show();
            return;
        }

        Calendar cal = Calendar.getInstance();
        cal.setTime(dateTo);
        Long currentDate = cal.getTimeInMillis();
        //cal.add(Calendar.HOUR, -2);
        cal.setTime(dateFrom);
        Long lastDate = cal.getTimeInMillis();
        String criteria = "date between '" + lastDate + "' and '" + currentDate + "'";

        Cursor c = cr.query(inboxURI, reqCols, criteria, null, null);



        if (c != null) {

            int totalSMS = 0;
            totalSMS = c.getCount();


            listDataChild = new HashMap<String, List<String>>();
            //String initial = "Number"+"\t"+"Name"+"\t"+"Date"+"\t"+"Time"+"\t"+"From"+"\t"+"To"+"\t"+"Train Details"+"\t"+"Receipt No"+"\t"+"Invoice No";
            //fin_res.add(initial);

            String[] colHeaders = {"Invoice #","Date","Time","From/To","Train","Receipt #","Phone"};
            report.append("Invoice #"+","+"Date"+","+"Time"+","+"From/To"+","+"Train"+","+"Receipt #"+","+"Phone"+"\n");
            TableRow tr_headH = new TableRow(this);
            tr_headH.setBackgroundColor(Color.GRAY);
            tr_headH.setLayoutParams(new TableLayout.LayoutParams(
                    TableLayout.LayoutParams.FILL_PARENT,
                    TableLayout.LayoutParams.WRAP_CONTENT));
            for (String header: colHeaders
                    ) {
                TextView lblEachRowH = new TextView(this);
                //label_weight_kg.setId(21);// define id that must be unique
                lblEachRowH.setText(header); // set the text for the header
                lblEachRowH.setTextColor(Color.WHITE); // set the color
                lblEachRowH.setPadding(5, 5, 5, 5); // set the padding (if required)
                tr_headH.addView(lblEachRowH); // add the column to the table row here
                //tr_headH.setGravity(Gravity.CENTER_HORIZONTAL);
            }
            tl.addView(tr_headH, new TableLayout.LayoutParams(
                    LayoutParams.FILL_PARENT,
                    LayoutParams.WRAP_CONTENT));

            if (c.moveToFirst()) {
                for (int j = 0; j < totalSMS; j++) {

                    listDataHeader = new ArrayList<String>();

                    String smsDate = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.DATE));
                    String number = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.ADDRESS));
                    String name = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.PERSON));
                    String body = c.getString(c.getColumnIndexOrThrow(Telephony.Sms.BODY));

                    Date dateFormat = new Date(Long.valueOf(smsDate));
                    String type;
                    String[] elements = body.split(" ");
                    //Log.d("Submit",elements[0]);
                    Pattern p = Pattern.compile("[0-9]{1,2}[A-Z]{3}");

                    Matcher m = p.matcher(elements[0]);
                    if (m.find()){

                        //Log.d("msg", smsDate + "\t" + number + "\t" + body);
                        String date = elements[0];
                        String time = elements[1];
                        String from_to = elements[2];//.split("-");
                        //String from = from_to[0];
                        //String to = from_to[1];

                        String trainDetails = elements[3]+elements[4];
                        //Log.d("Line",elements[7]+" hello");
                        String[] receiptNo_invoice = elements[7].split("\\(");
                        String receiptNo = receiptNo_invoice[0];
                        String invoice = receiptNo_invoice[1].substring(0,receiptNo_invoice[1].length()-2);

                        //Log.d("Table",number+"\t"+name+"\t"+date+"\t"+time+"\t"+from+"\t"+to+"\t"+trainDetails+"\t"+receiptNo+"\t"+invoice);

                        listDataHeader.add(invoice);
                        //listDataHeader.add(name);
                        listDataHeader.add(date);
                        listDataHeader.add(time);
                        listDataHeader.add(from_to);
                        //listDataHeader.add(from);
                        //listDataHeader.add(to);
                        listDataHeader.add(trainDetails);
                        listDataHeader.add(receiptNo);
                        listDataHeader.add(number);

                        report.append(invoice+","+date+","+time+","+from_to+","+trainDetails+","+receiptNo+","+number+"\n");

                        //listDataChild.put(invoice,listDataHeader);


                        TableRow tr_head = new TableRow(this);
                        //tr_head.setId(10);
                        tr_head.setBackgroundColor(Color.GRAY);
                        tr_head.setBackgroundResource(R.drawable.border);
                        tr_head.setLayoutParams(new TableLayout.LayoutParams(
                                TableLayout.LayoutParams.FILL_PARENT,
                                TableLayout.LayoutParams.WRAP_CONTENT));
                        for(String eachCell: listDataHeader){
                            TextView lblEachRow = new TextView(this);
                            //label_weight_kg.setId(21);// define id that must be unique
                            lblEachRow.setText(eachCell); // set the text for the header
                            lblEachRow.setTextColor(Color.WHITE); // set the color
                            lblEachRow.setPadding(20, 20, 20, 20); // set the padding (if required)
                            tr_head.addView(lblEachRow); // add the column to the table row here

                        }
                        tl.addView(tr_head, new TableLayout.LayoutParams(
                                LayoutParams.FILL_PARENT,
                                LayoutParams.WRAP_CONTENT));
                        //((ViewGroup)tl.getParent()).removeView(tl);
                        //ScrollView sv = new ScrollView(this);
                        //sv.addView(tl);
                        //super.setContentView(sv);
                        //main.addView(sv);
                        msgCount+=1;
                    }

                    c.moveToNext();
                }
            }
            if(msgCount <= 0){
                Toast.makeText(this, "No message to show!", Toast.LENGTH_LONG).show();
                Log.d("No MESSAGE","No MESSAGE");
                btnExport.setVisibility(View.GONE);
            }else{
                btnExport.setVisibility(View.VISIBLE);
            }
        } else {
            Toast.makeText(this, "No messages to show!", Toast.LENGTH_LONG).show();

        }
    }

    private void emailEngine(){
        Intent i = new Intent(Intent.ACTION_SEND);
        i.setType("text/csv");
        try {
            Date dateVal = new Date();
            String filename = dateVal.toString();

            tempFile = new File(Environment.getExternalStorageDirectory().getAbsolutePath(), "Flight_Report_"+filename+".csv");//getApplicationContext().getCacheDir();
            //File tempFile = File.createTempFile("values", ".csv");

            String[] permissions = new String[]{
                    //Manifest.permission.INTERNET,
                    //Manifest.permission.READ_PHONE_STATE,
                    //Manifest.permission.READ_EXTERNAL_STORAGE,
                    Manifest.permission.WRITE_EXTERNAL_STORAGE,
                    //Manifest.permission.VIBRATE,
                    //Manifest.permission.RECORD_AUDIO,
            };

            try {
                FileOutputStream out    =   new FileOutputStream(tempFile);
                out.write(report.toString().getBytes());
                out.close();
            } catch (IOException e) {
                Log.e("BROKEN", "Could not write file " + e.getMessage());
            }
            i.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            i.putExtra(Intent.EXTRA_STREAM, Uri.fromFile(tempFile));
            //i.putExtra(Intent.EXTRA_EMAIL, new String[] { to });
            i.putExtra(Intent.EXTRA_SUBJECT, "Report for trips between "+txtDateFrom.getText().toString()+" and "+txtDateTo.getText().toString());
            i.putExtra(Intent.EXTRA_TEXT, "Please find attached the spreadsheet report.");

            startActivityForResult(Intent.createChooser(i, "E-mail"),12);



            Log.d("Delete FILE",Environment.getExternalStorageDirectory().getAbsolutePath());

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 12) {
            if (resultCode == RESULT_OK) {
                Log.d("DELETE FILE",String.valueOf(tempFile.delete()));
                Toast.makeText(this, "Sent", Toast.LENGTH_LONG).show();
            } else if (resultCode == RESULT_CANCELED) {

                Toast.makeText(this, "Done", Toast.LENGTH_LONG).show();
                Log.d("DELETE FILE",String.valueOf(tempFile.delete()));
            }
        }
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        //super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == 100) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                emailEngine();
            }
            return;
        }

        if (requestCode == 101) {
            if (grantResults.length > 0
                    && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                workflow();
            }
            return;
        }
    }

    protected boolean checkPermission() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.WRITE_EXTERNAL_STORAGE);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    protected void requestPermission() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.WRITE_EXTERNAL_STORAGE)) {
            Toast.makeText(this, "Write External Storage permission allows us to do store images. Please allow this permission in App Settings.", Toast.LENGTH_LONG).show();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 100);
            }
        }
    }

    protected boolean checkPermission_SMS() {
        int result = ContextCompat.checkSelfPermission(this, android.Manifest.permission.READ_SMS);
        if (result == PackageManager.PERMISSION_GRANTED) {
            return true;
        } else {
            return false;
        }
    }

    protected void requestPermission_SMS() {

        if (ActivityCompat.shouldShowRequestPermissionRationale(this, Manifest.permission.READ_SMS)) {
            Toast.makeText(this, "Lets us read your SMS to report them", Toast.LENGTH_LONG).show();
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                requestPermissions(new String[]{Manifest.permission.READ_SMS}, 101);
            }
        }
    }
}
