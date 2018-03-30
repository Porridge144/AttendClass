package com.example.gszzz.attendclass.server_interaction;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

import com.example.gszzz.attendclass.LecturerLogIn;
import com.example.gszzz.attendclass.LecturerMenu;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLEncoder;

//DURUKUN test to see if can commit

public class BackgroundTaskRetrieveInfo extends AsyncTask<String, Void, String> {

    private Context ctx;

    public BackgroundTaskRetrieveInfo(Context ctx){
        this.ctx = ctx;
    }

    @Override
    protected void onPreExecute() {
//        super.onPreExecute();
//        alertDialog = new AlertDialog.Builder(ctx).create();
//        alertDialog.setTitle("Login Information");
    }

    @Override
    protected String doInBackground(String... params) {
//------------------------------Change Server IP HERE---------------------------------------
        String classInfoQueryUrl = "http://192.168.0.104:8081/attendance/classinfo_query.php";
//        String classInfoQueryUrl = "http://121.7.122.74:8081/attendance/classinfo_query.php";
//------------------------------------------------------------------------------------------
        String method = params[0];
        if (method.equals("query_class_list")) {
//            String name = params[1];
//            String username = params[2];
//            String password = params[3];
            try {
                URL url = new URL(classInfoQueryUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                OutputStream outputStream = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                String data = URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode("drk123", "UTF-8");
                bufferedWriter.write(data);
                bufferedWriter.flush();
                bufferedWriter.close();
                outputStream.close();
//                InputStream inputStream = httpURLConnection.getInputStream();
//                inputStream.close();
//                httpURLConnection.disconnect();
//                return "Registration Success...";
                InputStream inputStream = httpURLConnection.getInputStream();
                BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(inputStream,"iso-8859-1"));
                String response = "";
                String line = "";
                while ((line = bufferedReader.readLine()) != null) {
                    response += line;
                }
                bufferedReader.close();
                inputStream.close();
                httpURLConnection.disconnect();
                return response;

            } catch (IOException e) {
                e.printStackTrace();
            }

        }


        return null;
    }

    @Override
    protected void onProgressUpdate(Void... values) {
        super.onProgressUpdate(values);
    }

    @Override
    protected void onPostExecute(String result) {
//        super.onPostExecute(result);1111
        Toast.makeText(ctx, result, Toast.LENGTH_LONG).show();
    }
}
