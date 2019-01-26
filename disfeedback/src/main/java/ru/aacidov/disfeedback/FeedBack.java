package ru.aacidov.disfeedback;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.Dialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.res.Resources;
import android.os.Build;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.view.inputmethod.EditorInfo;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.android.volley.toolbox.Volley;


import org.json.JSONException;
import org.json.JSONObject;

import java.net.HttpURLConnection;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;

/**
 * Created by aacidov on 30.11.16.
 */
public class FeedBack {
    Context cxt;
    final RequestQueue queue;
    private Activity activity;

    public FeedBack( Context context){
        cxt=context;
        activity = (Activity) context;

        queue = Volley.newRequestQueue(cxt);
    }
    public void openFeedbackForm() {
        final AlertDialog.Builder builder = new AlertDialog.Builder(cxt);
        builder.setTitle(R.string.action_feedback);

        LinearLayout v = new LinearLayout( cxt);
        v.setOrientation(LinearLayout.VERTICAL);
        EditText emailEditView = new EditText(cxt);
        EditText feedbackEditView = new EditText(cxt);
        emailEditView.setHint(R.string.input_email);
        feedbackEditView.setHint(R.string.input_feedback);
        emailEditView.setInputType(InputType.TYPE_CLASS_TEXT|InputType.TYPE_TEXT_VARIATION_EMAIL_ADDRESS);
        feedbackEditView.setImeOptions(EditorInfo.IME_FLAG_NO_ENTER_ACTION);
        feedbackEditView.setRawInputType(InputType.TYPE_TEXT_FLAG_MULTI_LINE);
        v.addView(emailEditView);
        v.addView(feedbackEditView);
        builder.setView(v);
        MyOnClickListener mocl = new MyOnClickListener();
        builder.setPositiveButton(R.string.ok, mocl);
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                dialogInterface.cancel();
            }
        });
        final Dialog d = builder.show();
        mocl.d = d;
        mocl.emailEditView = emailEditView;
        mocl.feedbackEditView =feedbackEditView;
    }

    private class MyOnClickListener  implements DialogInterface.OnClickListener{
        private Dialog d;
        private EditText emailEditView ;
        private EditText feedbackEditView;

        @Override
        public void onClick(DialogInterface dialogInterface, int i) {
            String email = emailEditView.getText().toString();
            String text = feedbackEditView.getText().toString();

            if (email.length()==0||text.length()==0){
                Toast.makeText(cxt, R.string.dont_input, Toast.LENGTH_LONG).show();

                return;
            }
            send(email, text);
        }
    }

    public void send (final String email, final String text){
        Toast.makeText(cxt, R.string.sending, Toast.LENGTH_LONG).show();

        String url = "http://feedback.aacidov.ru";

        StringRequest postRequest = new StringRequest(Request.Method.POST, url,
                new Response.Listener<String>()
                {
                    @Override
                    public void onResponse(String response) {
                        // response
                        try {
                            JSONObject jo = new JSONObject(response);
                            if(jo.getInt("status")==1){
                                Toast.makeText(cxt, R.string.success, Toast.LENGTH_LONG).show();
                                return;
                            }
                            Toast.makeText(cxt, R.string.fail, Toast.LENGTH_LONG).show();

                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                    }
                },
                new Response.ErrorListener()
                {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        // errorLog.d("Error.Response", error.getMessage());
                        Toast.makeText(cxt, R.string.fail, Toast.LENGTH_LONG).show();

                    }
                }
        ) {
            @Override
            protected Map<String, String> getParams()
            {
                Map<String, String> params = new HashMap<String, String>();
                params.put("email", email);
                params.put("text", text);
                params.put("app", getSoftwareVersion());

                return params;
            }
        };

        queue.add(postRequest);
    }
    private String getSoftwareVersion() {
        PackageInfo pi;
        try {
            pi = cxt.getPackageManager().getPackageInfo(cxt.getPackageName(), 0);
            return pi.packageName;
        } catch (final PackageManager.NameNotFoundException e) {
            return "na";
        }
    }
}
