/*
 * Copyright (c) 2010-2011, The MiCode Open Source Community (www.micode.net)
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *        http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package net.micode.compass;

import android.app.Activity;
import android.content.Context;
import android.hardware.Sensor;
import android.hardware.SensorEvent;
import android.hardware.SensorEventListener;
import android.hardware.SensorManager;
import android.location.Criteria;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.location.LocationProvider;
import android.os.Bundle;
import android.os.Handler;
import android.text.TextUtils;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup.LayoutParams;
import android.view.animation.AccelerateInterpolator;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.util.Locale;

public class CompassActivity extends Activity {

    private final float MAX_ROATE_DEGREE = 2.0f;
    private SensorManager mSensorManager;
    //private Sensor mOrientationSensor;
    private Sensor sensorAccelerometer;
    private Sensor sensorMagneticField;
    //private LocationManager mLocationManager;
    //private String mLocationProvider;
    private float mDirection;
    private float mTargetDirection;
    private AccelerateInterpolator mInterpolator;
    protected final Handler mHandler = new Handler();
    private boolean mStopDrawing;
    private boolean mChinease;
    
    private float[] valuesAccelerometer = new float[3];
    private float[] valuesMagneticField = new float[3];
      
    private float[] matrixR = new float[9];
    private float[] matrixI = new float[9];
    private float[] matrixValues = new float[3];

    View mCompassView;
    CompassView mPointer;
    TextView mLocationTextView;
    //LinearLayout mDirectionLayout;
    //LinearLayout mAngleLayout;

    protected Runnable mCompassViewUpdater = new Runnable() {
        @Override
        public void run() {
            if (mPointer != null && !mStopDrawing) {
                //if (mDirection != mTargetDirection) {
            	if (Math.abs(mDirection - mTargetDirection) > 10.0f) {	//rotate >10f then refresh display

                    // calculate the short routine
                    float to = mTargetDirection;
                    if (to - mDirection > 180) {
                        to -= 360;
                    } else if (to - mDirection < -180) {
                        to += 360;
                    }

                    // limit the max speed to MAX_ROTATE_DEGREE
                    float distance = to - mDirection;
                    if (Math.abs(distance) > MAX_ROATE_DEGREE) {
                        distance = distance > 0 ? MAX_ROATE_DEGREE : (-1.0f * MAX_ROATE_DEGREE);
                    }

                    // need to slow down if the distance is short
                    mDirection = normalizeDegree(mDirection
                            + ((to - mDirection) * mInterpolator.getInterpolation(Math
                                    .abs(distance) > MAX_ROATE_DEGREE ? 0.8f : 0.4f)));
                    mPointer.updateDirection(mDirection);
                    
                    updateDirection();
                }

                //updateDirection();

                //mHandler.postDelayed(mCompassViewUpdater, 20);
                mHandler.postDelayed(mCompassViewUpdater, 20);//to fit eink display 5fps
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.main);
        com.tomoon.sdk.Emulator.configure(getWindow());
        initResources();
        initServices();
    }

    @Override
    protected void onResume() {
        super.onResume();
//        if (mLocationProvider != null) {
//            updateLocation(mLocationManager.getLastKnownLocation(mLocationProvider));
//            mLocationManager.requestLocationUpdates(mLocationProvider, 2000, 10, mLocationListener);
//        } else {
//            mLocationTextView.setText(R.string.cannot_get_location);
//        }
//        if (mOrientationSensor != null) {
//            mSensorManager.registerListener(mOrientationSensorEventListener, mOrientationSensor,
//                    SensorManager.SENSOR_DELAY_GAME);
//            		//SensorManager.SENSOR_DELAY_UI);
//        }
        if (sensorAccelerometer != null) {
            mSensorManager.registerListener(mOrientationSensorEventListener, sensorAccelerometer,
                    SensorManager.SENSOR_DELAY_GAME);
            		//SensorManager.SENSOR_DELAY_UI);
        }
        if (sensorMagneticField != null) {
            mSensorManager.registerListener(mOrientationSensorEventListener, sensorMagneticField,
                    SensorManager.SENSOR_DELAY_GAME);
            		//SensorManager.SENSOR_DELAY_UI);
        }
        mStopDrawing = false;
        //mHandler.postDelayed(mCompassViewUpdater, 20);
        mHandler.postDelayed(mCompassViewUpdater, 20);//to fit eink display 5fps
    }

    @Override
    protected void onPause() {
        super.onPause();
        mStopDrawing = true;
//        if (mOrientationSensor != null) {
//            mSensorManager.unregisterListener(mOrientationSensorEventListener);
//        }
        if (sensorAccelerometer != null) {
            mSensorManager.unregisterListener(mOrientationSensorEventListener);
        }
        if (sensorMagneticField != null) {
            mSensorManager.unregisterListener(mOrientationSensorEventListener);
        }
//        if (mLocationProvider != null) {
//            mLocationManager.removeUpdates(mLocationListener);
//        }
    }

    private void initResources() {
        mDirection = 0.0f;
        mTargetDirection = 0.0f;
        mInterpolator = new AccelerateInterpolator();
        mStopDrawing = true;
        mChinease = TextUtils.equals(Locale.getDefault().getLanguage(), "zh");

        mCompassView = findViewById(R.id.view_compass);
        mPointer = (CompassView) findViewById(R.id.compass_pointer);
        mLocationTextView = (TextView) findViewById(R.id.textview_location);
        //mDirectionLayout = (LinearLayout) findViewById(R.id.layout_direction);
        //mAngleLayout = (LinearLayout) findViewById(R.id.layout_angle);

        mPointer.setImageResource(mChinease ? R.drawable.compass_cn : R.drawable.compass);
    }

    private void initServices() {
        // sensor manager
        mSensorManager = (SensorManager) getSystemService(Context.SENSOR_SERVICE);
//        mOrientationSensor = mSensorManager.getDefaultSensor(Sensor.TYPE_ORIENTATION);
        sensorAccelerometer = mSensorManager.getDefaultSensor(Sensor.TYPE_ACCELEROMETER);
        sensorMagneticField = mSensorManager.getDefaultSensor(Sensor.TYPE_MAGNETIC_FIELD);


    }

	  private void updateDirection() {
		StringBuilder sb = new StringBuilder();
		boolean east = false;
		boolean west = false;
		boolean north = false;
		boolean south = false;
	    float direction = normalizeDegree(mTargetDirection * -1.0f);
	    if (direction > 22.5f && direction < 157.5f) {
	        // east
	    	east = true;
	    } else if (direction > 202.5f && direction < 337.5f) {
	        // west
	    	west = true;
	    }
	
	    if (direction > 112.5f && direction < 247.5f) {
	        // south
	    	south = true;
	    } else if (direction < 67.5 || direction > 292.5f) {
	        // north
	    	north = true;
	    }
	
	    if (mChinease) {
	        // east/west should be before north/south
	    	if(east)
	    		sb.append("东");
	    	if(west)
	    		sb.append("西");
	    	if(north)
	    		sb.append("北");
	    	if(south)
	    		sb.append("南");
	    } else {
	        // north/south should be before east/west
	    	if(east)
	    		sb.append("east");
	    	if(west)
	    		sb.append("west");
	    	if(north)
	    		sb.append("north");
	    	if(south)
	    		sb.append("south");
	    }
	    int directionInteger = (int) direction;
	    sb.append(directionInteger + "°");
	    mLocationTextView.setText(sb.toString());
	
	}
    
 /*   private void updateDirection() {
        LayoutParams lp = new LayoutParams(LayoutParams.WRAP_CONTENT, LayoutParams.WRAP_CONTENT);

        mDirectionLayout.removeAllViews();
        mAngleLayout.removeAllViews();

        ImageView east = null;
        ImageView west = null;
        ImageView south = null;
        ImageView north = null;
        float direction = normalizeDegree(mTargetDirection * -1.0f);
        if (direction > 22.5f && direction < 157.5f) {
            // east
            east = new ImageView(this);
            east.setImageResource(mChinease ? R.drawable.e_cn : R.drawable.e);
            east.setLayoutParams(lp);
        } else if (direction > 202.5f && direction < 337.5f) {
            // west
            west = new ImageView(this);
            west.setImageResource(mChinease ? R.drawable.w_cn : R.drawable.w);
            west.setLayoutParams(lp);
        }

        if (direction > 112.5f && direction < 247.5f) {
            // south
            south = new ImageView(this);
            south.setImageResource(mChinease ? R.drawable.s_cn : R.drawable.s);
            south.setLayoutParams(lp);
        } else if (direction < 67.5 || direction > 292.5f) {
            // north
            north = new ImageView(this);
            north.setImageResource(mChinease ? R.drawable.n_cn : R.drawable.n);
            north.setLayoutParams(lp);
        }

        if (mChinease) {
            // east/west should be before north/south
            if (east != null) {
                mDirectionLayout.addView(east);
            }
            if (west != null) {
                mDirectionLayout.addView(west);
            }
            if (south != null) {
                mDirectionLayout.addView(south);
            }
            if (north != null) {
                mDirectionLayout.addView(north);
            }
        } else {
            // north/south should be before east/west
            if (south != null) {
                mDirectionLayout.addView(south);
            }
            if (north != null) {
                mDirectionLayout.addView(north);
            }
            if (east != null) {
                mDirectionLayout.addView(east);
            }
            if (west != null) {
                mDirectionLayout.addView(west);
            }
        }

        int direction2 = (int) direction;
        boolean show = false;
        if (direction2 >= 100) {
            mAngleLayout.addView(getNumberImage(direction2 / 100));
            direction2 %= 100;
            show = true;
        }
        if (direction2 >= 10 || show) {
            mAngleLayout.addView(getNumberImage(direction2 / 10));
            direction2 %= 10;
        }
        mAngleLayout.addView(getNumberImage(direction2));

        ImageView degreeImageView = new ImageView(this);
        degreeImageView.setImageResource(R.drawable.degree);
        degreeImageView.setLayoutParams(lp);
        mAngleLayout.addView(degreeImageView);
    }*/


    private SensorEventListener mOrientationSensorEventListener = new SensorEventListener() {

        @Override
        public void onSensorChanged(SensorEvent event) {
            //float direction = event.values[0] * -1.0f;
            //mTargetDirection = normalizeDegree(direction);
            
            switch(event.sensor.getType()){
            case Sensor.TYPE_ACCELEROMETER:
             for(int i =0; i < 3; i++){
              valuesAccelerometer[i] = event.values[i];//Log.e("valuesAccelerometer", "valuesAccelerometer " + i + " : " + valuesAccelerometer[i]);
             }
             break;
            case Sensor.TYPE_MAGNETIC_FIELD:
             for(int i =0; i < 3; i++){
              valuesMagneticField[i] = event.values[i];Log.e("valuesMagneticField", "valuesMagneticField " + i + " : " + valuesMagneticField[i]);
             }
             break;
            }
            
            boolean success = SensorManager.getRotationMatrix(
            	       matrixR,
            	       matrixI,
            	       valuesAccelerometer,
            	       valuesMagneticField);
            
            if(success){
            	SensorManager.getOrientation(matrixR, matrixValues);
            	double azimuth = Math.toDegrees(matrixValues[0]);
            	//double pitch = Math.toDegrees(matrixValues[1]);
            	//double roll = Math.toDegrees(matrixValues[2]);
            	
            	//if(Math.random() < 0.1f) //slow down frequency
            		mTargetDirection = normalizeDegree((float) azimuth* -1.0f);
            }
        }

        @Override
        public void onAccuracyChanged(Sensor sensor, int accuracy) {
        }
    };

    private float normalizeDegree(float degree) {
        return (degree + 720) % 360;
    	//return (360 - degree) % 360; 
    }

}
