package com.minimalcode.thermalscope;

import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.util.Log;
import android.widget.TextView;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        EdgeToEdge.enable(this);
        setContentView(R.layout.activity_main);
        ViewCompat.setOnApplyWindowInsetsListener(findViewById(R.id.main), (v, insets) -> {
            Insets systemBars = insets.getInsets(WindowInsetsCompat.Type.systemBars());
            v.setPadding(systemBars.left, systemBars.top, systemBars.right, systemBars.bottom);
            return insets;
        });

        Context context = getApplicationContext();
        RoomTemperatureEstimator estimator = new RoomTemperatureEstimator(context);
        double estimatedTemp = estimator.getEstimatedRoomTemp();
        estimator.startAmbientListener();

        TextView roomTempTextView = findViewById(R.id.roomTemp);
        roomTempTextView.setText(String.format("%.2f °C", estimatedTemp));

        Handler handler = new Handler();
        Runnable runnable = new Runnable() {
            @Override
            public void run() {
                float roomTemp = estimator.getEstimatedRoomTemp();
                roomTempTextView.setText(String.format("%.2f °C", roomTemp));
                Log.d("RoomTemp", "Estimated Room Temperature: " + roomTemp + "°C");

                handler.postDelayed(this, 3000); // update every 3 seconds
            }
        };
        handler.post(runnable);

    }
}