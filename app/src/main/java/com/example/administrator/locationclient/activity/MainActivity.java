package com.example.administrator.locationclient.activity;

import android.content.Intent;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;

import com.example.administrator.locationclient.R;

public class MainActivity extends AppCompatActivity
{
    private Button btn_start_location;
    private Button btn_location_result;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        btn_start_location = (Button) findViewById(R.id.indoor_location);
        btn_start_location.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, CameraActivity.class);
                startActivity(intent);
            }
        });

        btn_location_result = (Button)findViewById(R.id.location_result);
        btn_location_result.setOnClickListener(new View.OnClickListener()
        {
            @Override
            public void onClick(View v)
            {
                Intent intent = new Intent(MainActivity.this, LocalizationActivity.class);
                startActivity(intent);
            }
        });





    }
}
