package com.example.administrator.locationclient.activity;

import android.content.Intent;
import android.graphics.BitmapRegionDecoder;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.widget.TextView;

import com.example.administrator.locationclient.R;

import java.io.IOException;

import sysu.mobile.limk.library.MapView;
import sysu.mobile.limk.library.OnRealLocationMoveListener;
import sysu.mobile.limk.library.Position;

public class LocalizationActivity extends AppCompatActivity
{
    private TextView infoTxt;
    private MapView mapView;

    @Override
    protected void onCreate(Bundle savedInstanceState)
    {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_localization);
        infoTxt = (TextView) findViewById(R.id.indoorMap_infoTxt);
        mapView = (MapView) findViewById(R.id.indoorMap_mapView);

        Intent intent = getIntent();
        String result_string = intent.getStringExtra("data");
        String[] result_x_y = result_string.split("#");
        double x = Double.parseDouble(result_x_y[0]);
        double y = Double.parseDouble(result_x_y[1]);
        showLocation(x, y);
    }


    private void showLocation(double x, double y)
    {
        try
        {
            mapView.initNewMap(getAssets().open("map.png"), 1, 0, new Position(x, y));
            BitmapRegionDecoder mMapDecoder = BitmapRegionDecoder.newInstance(
                    getAssets().open("map.png"), false);
        } catch (IOException e)
        {
            e.printStackTrace();
        }
        mapView.updateMyLocation(new Position(x, y));
        mapView.setOnRealLocationMoveListener(new OnRealLocationMoveListener() {
            @Override
            public void onMove(Position position) {
                infoTxt.setText(position.toString());
            }
        });
    }
}
