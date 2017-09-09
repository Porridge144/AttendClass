package com.example.gszzz.attendclass;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.widget.Toast;

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

public class BackgroundTask1 extends AsyncTask<String, Void, String> {

    private Context ctx;

    BackgroundTask1(Context ctx){
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
//        String regUrl = "http://121.7.122.74:8081/attendance/register.php";
//        String loginUrl = "http://121.7.122.74:8081/attendance/login.php";
        String regUrl = "http://172.17.0.145:8081/attendance/register.php";
        String loginUrl = "http://172.17.0.145:8081/attendance/login.php";
//------------------------------------------------------------------------------------------
        String method = params[0];
        if (method.equals("register")) {
            String name = params[1];
            String username = params[2];
            String password = params[3];
            try {
                URL url = new URL(regUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                OutputStream outputStream = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                String data = URLEncoder.encode("name", "UTF-8") + "=" + URLEncoder.encode(name, "UTF-8") + "&" +
                        URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(username, "UTF-8") + "&" +
                        URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8");
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

        } else if (method.equals("login")) {
            String username = params[1];
            String password = params[2];
            try {
                URL url = new URL(loginUrl);
                HttpURLConnection httpURLConnection = (HttpURLConnection) url.openConnection();
                httpURLConnection.setRequestMethod("POST");
                httpURLConnection.setDoOutput(true);
                httpURLConnection.setDoInput(true);
                OutputStream outputStream = httpURLConnection.getOutputStream();
                BufferedWriter bufferedWriter = new BufferedWriter(new OutputStreamWriter(outputStream, "UTF-8"));
                String data = URLEncoder.encode("username", "UTF-8") + "=" + URLEncoder.encode(username, "UTF-8") + "&" +
                        URLEncoder.encode("password", "UTF-8") + "=" + URLEncoder.encode(password, "UTF-8");
                bufferedWriter.write(data);
                bufferedWriter.flush();
                bufferedWriter.close();
                outputStream.close();

                //Get response from server!!
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
        if (result.equals("Registration Succeeded...")) {
            Toast.makeText(ctx, result, Toast.LENGTH_LONG).show();
            //close activity
            Intent i = new Intent(ctx, LecturerLogIn.class);
            i.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK | Intent.FLAG_ACTIVITY_NEW_TASK);
            ctx.startActivity(i);
        }else {
            if (result.equals("Username already existed. Registration failed...")){

            } else if (!result.equals("Login Failed...Try Again")){
                Intent i = new Intent(ctx, LecturerMenu.class);
                ctx.startActivity(i);
            }
            Toast.makeText(ctx, result, Toast.LENGTH_LONG).show();
        }
    }
}
