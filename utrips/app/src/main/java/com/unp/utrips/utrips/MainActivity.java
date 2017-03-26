package com.unp.utrips.utrips;

import android.Manifest;
import android.app.DatePickerDialog;
import android.content.ContentResolver;
import android.content.DialogInterface;
import android.content.pm.PackageManager;
import android.database.Cursor;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.Telephony;
import android.support.annotation.NonNull;
import android.support.annotation.RequiresApi;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.widget.SimpleCursorAdapter;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.DatePicker;
import android.widget.EditText;
import android.widget.ExpandableListAdapter;
import android.widget.ExpandableListView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TableLayout;
import android.widget.TableRow;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import android.widget.TableLayout.LayoutParams;

public class MainActivity extends AppCompatActivity implements
        View.OnClickListener{

    private static final int REQUEST_CODE_ASK_PERMISSIONS = 123;
    private static final int REQUEST_CONTACT_ASK_PERMISSIONS = 124;
    Button btnDatePickerFrom,btnDatePickerTo,btnSubmit;
    EditText txtDateFrom,txtDateTo;
    private int mYear, mMonth, mDay;
    TableLayout tl;
    ScrollView sv;

    List<String> listDataHeader;
    HashMap<String, List<String>> listDataChild;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        btnDatePickerFrom =(Button)findViewById(R.id.btn_date_from);
        btnDatePickerTo =(Button)findViewById(R.id.btn_date_to);
        btnSubmit =(Button)findViewById(R.id.btn_submit);
        txtDateFrom =(EditText)findViewById(R.id.in_date);
        txtDateTo =(EditText)findViewById(R.id.out_date);
        //lv = (ExpandableListView) findViewById(R.id.lv_results);
        tl = (TableLayout) findViewById(R.id.main_table);
        //tl.removeAllViews();

        //TableRow tr_head = new TableRow(this);
        //tr_head.setId(10);
        //tr_head.setBackgroundColor(Color.GRAY);
        //tr_head.setLayoutParams(new TableLayout.LayoutParams(
          //      LayoutParams.FILL_PARENT,
           //     LayoutParams.WRAP_CONTENT));
        //TextView label_date = new TextView(this);
        //label_date.setId(20);
        //label_date.setText("DATE");
        //label_date.setTextColor(Color.WHITE);
        //label_date.setPadding(5, 5, 5, 5);
        //tr_head.addView(label_date);// add the column to the table row here
        //sv = (ScrollView)findViewById(R.id.scroll_table);
        //sv.addView(tl);//setContentView(sv);


        listDataChild = new HashMap<String, List<String>>();
        listDataHeader = new ArrayList<String>();

        btnDatePickerFrom.setOnClickListener(this);
        btnDatePickerTo.setOnClickListener(this);
        btnSubmit.setOnClickListener(this);
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
                            txtDateFrom.setText(dayOfMonth + "-" + (month + 1) + "-" + year);
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
                            txtDateTo.setText(dayOfMonth + "-" + (month + 1) + "-" + year);
                        }


                    }, mYear, mMonth, mDay);
            datePickerDialog.show();
        }

        if (v == btnSubmit) {

            Log.d("Submit","Pressed Submit");
            tl.removeAllViews();

            if(ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_SMS") != PackageManager.PERMISSION_GRANTED) {
                ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_SMS"}, REQUEST_CODE_ASK_PERMISSIONS);
                //if(ContextCompat.checkSelfPermission(getBaseContext(), "android.permission.READ_CONTACTS") != PackageManager.PERMISSION_GRANTED) {
                //    ActivityCompat.requestPermissions(this, new String[]{"android.permission.READ_CONTACTS"}, REQUEST_CODE_ASK_PERMISSIONS);
                //}
            }else{
                smsEngine();
                Uri inboxURI = Uri.parse("content://sms/inbox/");

                // List required columns
                String[] reqCols = new String[]{"_id", "address", "body", "date","person"};
                ContentResolver cr = getContentResolver();

                Calendar cal = Calendar.getInstance();
                Long currentDate = cal.getTimeInMillis();
                cal.add(Calendar.HOUR, -24);
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
                            }

                            c.moveToNext();
                        }
                    }
                } else {
                    Toast.makeText(this, "No message to show!", Toast.LENGTH_SHORT).show();
                }
            }

        }
    }

    private void smsEngine(){

    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        smsEngine();
    }
}
