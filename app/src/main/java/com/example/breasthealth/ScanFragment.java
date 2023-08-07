package com.example.breasthealth;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.location.Address;
import android.location.Geocoder;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.RecyclerView;

import android.provider.MediaStore;
import android.provider.Settings;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.Manifest;
import android.content.pm.PackageManager;
import android.os.Build;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.example.breasthealth.ml.Litemodel;

import org.tensorflow.lite.DataType;
import org.tensorflow.lite.support.image.TensorImage;
import org.tensorflow.lite.support.tensorbuffer.TensorBuffer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

/**
 * A simple {@link Fragment} subclass.
 * Use the {@link ScanFragment#newInstance} factory method to
 * create an instance of this fragment.
 */

public class ScanFragment extends Fragment {
    private Button selectBtn, predictBtn, captureBtn, saveDiagnose;
    private TextView result, confidencetxt, notleaftxt, extraconf, tipText;
    private Bitmap bitmap;
    private ImageView imageView;
    private int imageSize = 224;
    private int advice_code;
    private static final int REQUEST_LOCATION_PERMISSION = 1234;
    private LocationManager locationManager;
    private LocationListener locationListener;
    private String locationText;
    private Uri ImageUri;
    private String date;


    // TODO: Rename parameter arguments, choose names that match
    // the fragment initialization parameters, e.g. ARG_ITEM_NUMBER
    private static final String ARG_PARAM1 = "param1";
    private static final String ARG_PARAM2 = "param2";

    // TODO: Rename and change types of parameters
    private String mParam1;
    private String mParam2;

    public ScanFragment() {
        // Required empty public constructor
    }

    /**
     * Use this factory method to create a new instance of
     * this fragment using the provided parameters.
     *
     * @param param1 Parameter 1.
     * @param param2 Parameter 2.
     * @return A new instance of fragment ScanFragment.
     */
    // TODO: Rename and change types and number of parameters
    public static ScanFragment newInstance(String param1, String param2) {
        ScanFragment fragment = new ScanFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if (getArguments() != null) {
            mParam1 = getArguments().getString(ARG_PARAM1);
            mParam2 = getArguments().getString(ARG_PARAM2);
        }
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        locationManager = (LocationManager) context.getSystemService(Context.LOCATION_SERVICE);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        View view =  inflater.inflate(R.layout.fragment_scan, container, false);
        selectBtn = view.findViewById(R.id.idGallerybtn);
        predictBtn = view.findViewById(R.id.idPredictBtn);
        captureBtn = view.findViewById(R.id.idCamerabtn);
        result = view.findViewById(R.id.idresult);
        imageView = view.findViewById(R.id.idImageview);
        confidencetxt = view.findViewById(R.id.idconf);
        notleaftxt = view.findViewById(R.id.notLeaftxt);
        extraconf = view.findViewById(R.id.idextraconf);
        tipText = view.findViewById(R.id.idtips);
        Calendar calendar = Calendar.getInstance();
        int year = calendar.get(Calendar.YEAR);
        int month = calendar.get(Calendar.MONTH);
        int dayOfMonth = calendar.get(Calendar.DAY_OF_MONTH);
        int hour = calendar.get(Calendar.HOUR_OF_DAY);
        int minutes = calendar.get(Calendar.MINUTE);
        int seconds = calendar.get(Calendar.SECOND);

        // Convert the numeric month value to its corresponding textual representation
        String[] months = {"Jan", "Feb", "March", "April", "May", "June", "July", "Aug", "September", "October", "November", "December"};
        String monthTxt = months[month];

        date = "Date: " + dayOfMonth + " " + monthTxt + " " + year + ", Time: " + hour + " : " + minutes;
        selectBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tipText.setVisibility(View.GONE);
                Intent intent = new Intent();
                notleaftxt.setVisibility(View.GONE);
                result.setText("");
                confidencetxt.setText("");
                extraconf.setVisibility(View.GONE);
                getPermission();
                intent.setAction(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, 10);
//                result.setText("");
//                classfyDisease();
            }
        });

        captureBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                tipText.setVisibility(View.GONE);
                Intent intent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(intent, 12);
                notleaftxt.setVisibility(View.GONE);
                extraconf.setVisibility(View.GONE);
                result.setText("");
                confidencetxt.setText("");
//                result.setText("");
//                classfyDisease();
            }
        });

        predictBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                result.setText("");
                classfyDisease();
            }
        });
        checkLocationEnabled();
        // Initialize LocationListener
        locationListener = new LocationListener() {
            @Override
            public void onLocationChanged(@NonNull Location location) {
                double latitude = location.getLatitude();
                double longitude = location.getLongitude();

                Geocoder geocoder = new Geocoder(getActivity(), Locale.getDefault());
                try {
                    List<Address> addresses = geocoder.getFromLocation(latitude, longitude, 1);
                    if (addresses.size() > 0) {
                        String country = addresses.get(0).getCountryName();
                        String city = addresses.get(0).getLocality();
                        String village = addresses.get(0).getSubLocality();
                        // Use the country and city information as needed
                        Log.d("Location", "Country: " + country);
                        Log.d("Location", "City: " + city);
                        locationText = village + " " + city + " " + country;
//                        countryinfo = country;
//                        cityinfo = city;
                        System.out.println(locationText);
                    }
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }

            @Override
            public void onStatusChanged(String provider, int status, Bundle extras) {
            }

            @Override
            public void onProviderEnabled(String provider) {
            }

            @Override
            public void onProviderDisabled(String provider) {
            }
        };

        return view;
    }


    private int getMax(float[] arr) {
        int max = 0;
        for (int i = 0; i < arr.length; i++){
            if(arr[i] > arr[max]) max = i;
        }
        return max;
    }

    private String getClassNameForIndex(int index) {
        // Implement your logic to map index to class name
        // Return the class name corresponding to the index

        // Example:
        switch (index) {
            case 0:
                return "Benign";
            case 1:
                return "Malignant";
            default:
                return "Unknown";
        }
    }

    void getPermission() {
        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.M){
            // Check if the camera permission is granted
            if (ContextCompat.checkSelfPermission(requireContext(), Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED) {
                // Request the camera permission
                ActivityCompat.requestPermissions(requireActivity(), new String[]{Manifest.permission.CAMERA}, 11);
            }
        }
    }

    private void checkLocationEnabled() {
        LocationManager locationManager = (LocationManager) requireContext().getSystemService(Context.LOCATION_SERVICE);
        boolean gpsEnabled = locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER);
        boolean networkEnabled = locationManager.isProviderEnabled(LocationManager.NETWORK_PROVIDER);

        if (!gpsEnabled && !networkEnabled) {
            // Location services are disabled, show a dialog to the user
            AlertDialog.Builder dialogBuilder = new AlertDialog.Builder(requireContext());
            dialogBuilder.setTitle("Enable Location")
                    .setMessage("Please enable location services to use this feature.")
                    .setPositiveButton("Settings", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            // Open settings to enable location services
                            Intent intent = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                            startActivity(intent);
                        }
                    }).setNegativeButton("Cancel", null)
                    .show();
        }
    }

    @Override
    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        if (requestCode == 10) {
            if (data != null) {
                Uri uri = data.getData();
                try {
                    bitmap = MediaStore.Images.Media.getBitmap(requireContext().getContentResolver(), uri);
                    imageView.setImageBitmap(bitmap);
                    ImageUri = uri;
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        } else if (requestCode == 12) {
            if (resultCode == Activity.RESULT_OK) {
                // The image was captured successfully
                bitmap = (Bitmap) data.getExtras().get("data");
                imageView.setImageBitmap(bitmap);
//                Uri imageUri = getImageUriFromBitmap(bitmap);
                // Continue with further processing of the captured image
            } else {
                // Image capture was canceled or failed
                // Handle the error or show a message to the user
            }
        }
        super.onActivityResult(requestCode, resultCode, data);
    }


    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        if (requestCode == 11){
            if(grantResults.length>0){
                if(grantResults[0] == PackageManager.PERMISSION_GRANTED){
                    this.getPermission();
                }
            }
        }
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
    }

    @Override
    public void onResume() {
        super.onResume();

//        if (ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED
//                && ActivityCompat.checkSelfPermission(getActivity(), Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
//            // Request location permissions if not granted
//            ActivityCompat.requestPermissions(getActivity(), new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, REQUEST_LOCATION_PERMISSION);
//            return;
//        }

//        locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 10000, 0, locationListener);
    }

    @Override
    public void onPause() {
        super.onPause();
        locationManager.removeUpdates(locationListener);
    }

//    private Uri getImageUriFromBitmap(Bitmap bitmap) {
//        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
//        bitmap.compress(Bitmap.CompressFormat.JPEG, 100, bytes);
//        String path = MediaStore.Images.Media.insertImage(requireActivity().getContentResolver(), bitmap, "Title", null);
//        return Uri.parse(path);
//    }


    void classfyDisease(){
        try {
            Litemodel model = Litemodel.newInstance(getContext().getApplicationContext());

            // Creates inputs for reference.
            TensorBuffer inputFeature0 = TensorBuffer.createFixedSize(new int[]{1, 224, 224, 3}, DataType.UINT8);

            if (bitmap != null){
                bitmap = Bitmap.createScaledBitmap(bitmap, 224, 224, true);
                inputFeature0.loadBuffer(TensorImage.fromBitmap(bitmap).getBuffer());
                // Runs model inference and gets result.
                Litemodel.Outputs outputs = model.process(inputFeature0);
                TensorBuffer outputFeature0 = outputs.getOutputFeature0AsTensorBuffer();

                // Normalize the probabilities
                float[] probabilities = outputFeature0.getFloatArray();
                float sum = 0.0f;
                for (float probability : probabilities) {
                    sum += probability;
                }
                for (int i = 0; i < probabilities.length; i++) {
                    probabilities[i] = probabilities[i] / sum;
                }

                // Output the confidence values for each class or value
                StringBuilder confidenceBuilder = new StringBuilder();
                for (int i = 0; i < probabilities.length; i++) {
                    String className = getClassNameForIndex(i);
                    float confidence = probabilities[i] * 100;
                    String confidenceString = String.format("%.2f", confidence);
                    confidenceBuilder.append(className).append(": ").append(confidenceString).append("%\n");
                    System.out.println(className + ": " + confidenceString + "%");
                }
                extraconf.setVisibility(View.VISIBLE);
                extraconf.setText(confidenceBuilder.toString());
                tipText.setVisibility(View.VISIBLE);

                // Releases model resources if no longer used.
                int results = getMax(outputFeature0.getFloatArray());
                if (results == 0){
                    result.setText("Benign");
                    confidencetxt.setText(String.format("%.2f", probabilities[results] * 100));
                    tipText.setText("Please visit a nearby hosiptal to learn how to prevent cancer .");

                } else if(results == 1) {
                    result.setText("Malignant");
                    confidencetxt.setText(String.format("%.2f", probabilities[results] * 100));
                    tipText.setText("Please visit a nearby hosiptal for Treatment .");

                }else if(results == 2){
                    result.setText("Unknown");
                    confidencetxt.setText(String.format("%.2f", probabilities[results] * 100));
                }else{
                    Toast.makeText(getContext(), "Error !", Toast.LENGTH_SHORT).show();
                }
                model.close();

            }else{
                Toast.makeText(getContext(), "No Image selected", Toast.LENGTH_SHORT).show();
            }

        } catch (IOException e) {
            // TODO Handle the exception
        }

    }

}