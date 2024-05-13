package com.example.trail6;

import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.location.Location;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.exifinterface.media.ExifInterface;

import org.opencv.android.OpenCVLoader;
import org.opencv.android.Utils;
import org.opencv.core.Mat;
import org.opencv.core.MatOfRect;
import org.opencv.core.Rect;
import org.opencv.core.Scalar;
import org.opencv.core.Size;
import org.opencv.imgproc.Imgproc;
import org.opencv.objdetect.CascadeClassifier;

import com.google.android.gms.location.FusedLocationProviderClient;
import com.google.android.gms.location.LocationCallback;
import com.google.android.gms.location.LocationRequest;
import com.google.android.gms.location.LocationResult;
import com.google.android.gms.location.LocationServices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.List;

import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;

public class MainActivity extends AppCompatActivity {

    private static final int REQUEST_GALLERY = 1;
    private static final int PERMISSIONS_REQUEST_READ_MEDIA_IMAGES = 101;
    private static final int LOCATION_PERMISSION_REQUEST_CODE = 100;
    private TextView metadataTextView;
    private WebView mapView;
    private Button openMapButton;
    private String currentLatitude = "0.0";
    private String currentLongitude = "0.0";
    private FusedLocationProviderClient fusedLocationClient;
    private LocationRequest locationRequest;
    private LocationCallback locationCallback;
    private CascadeClassifier cascadeClassifier;
    private Mat grayscaleImage;
    private boolean isEyeDetected = false;
    // Declare ImageView
    private ImageView imageView;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        // Initialize ImageView
        imageView = findViewById(R.id.selected_image_view);

        // Initialize OpenCV
        if (!OpenCVLoader.initDebug()) {
            Log.e("OpenCV", "OpenCV initialization failed.");
        } else {
            Log.d("OpenCV", "OpenCV initialized successfully.");
        }

        Button selectImageButton = findViewById(R.id.select_image_button);
        metadataTextView = findViewById(R.id.metadata_text_view);
        mapView = findViewById(R.id.map_view);
        openMapButton = findViewById(R.id.open_map_button);
        Button hospitalsButton = findViewById(R.id.hospitals_button);

        selectImageButton.setOnClickListener(new View.OnClickListener() {
            public void onClick(View view) {
                if (ContextCompat.checkSelfPermission(MainActivity.this,
                        Manifest.permission.READ_MEDIA_IMAGES)
                        != PackageManager.PERMISSION_GRANTED) {
                    ActivityCompat.requestPermissions(MainActivity.this,
                            new String[]{Manifest.permission.READ_MEDIA_IMAGES},
                            PERMISSIONS_REQUEST_READ_MEDIA_IMAGES);
                } else {
                    openGallery();
                }
            }
        });

        openMapButton.setOnClickListener(view -> {
            if (currentLatitude != null && currentLongitude != null) {
                mapView.setVisibility(View.VISIBLE);
                WebSettings webSettings = mapView.getSettings();
                webSettings.setJavaScriptEnabled(true);
                mapView.setWebChromeClient(new WebChromeClient());
                mapView.setWebViewClient(new WebViewClient() {
                    @Override
                    public void onPageFinished(WebView view, String url) {
                        mapView.loadUrl("javascript:initMap(" + currentLatitude + ", " + currentLongitude + ")");
                    }
                });
                mapView.addJavascriptInterface(new WebAppInterface(this), "Android"); // Add this line
                mapView.loadUrl("file:///android_asset/map.html");
            } else {
                Toast.makeText(MainActivity.this, "No GPS coordinates found to show on map.", Toast.LENGTH_SHORT).show();
            }
        });

        hospitalsButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (isEyeDetected) {
                    fetchNearbyEyeHospitals(currentLatitude, currentLongitude);
                } else {
                    fetchNearbyHospitals(currentLatitude, currentLongitude);
                }
            }
        });

        // Initialize fusedLocationClient
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this);

        // Create location request
        locationRequest = LocationRequest.create();
        locationRequest.setPriority(LocationRequest.PRIORITY_HIGH_ACCURACY);
        locationRequest.setInterval(10000); // 10 seconds in milliseconds

        // Create location callback
        locationCallback = new LocationCallback() {
            @Override
            public void onLocationResult(LocationResult locationResult) {
                if (locationResult == null) {
                    return;
                }
                for (Location location : locationResult.getLocations()) {
                    // Update currentLatitude and currentLongitude with the new location
                    currentLatitude = String.valueOf(location.getLatitude());
                    currentLongitude = String.valueOf(location.getLongitude());
                    metadataTextView.setText("Latitude: " + currentLatitude + "\nLongitude: " + currentLongitude);

                    // Update the source marker position using Nominatim
                    updateSourceMarkerWithNominatim(currentLatitude, currentLongitude);
                    break; // Only update once for the first location received
                }
            }
        };

        // Check if the app has permission to access location
        if (checkLocationPermission()) {
            startGettingLocationUpdates();
        } else {
            requestLocationPermission();
        }

        // Load Haarcascade XML for eye detection
        cascadeClassifier = new CascadeClassifier();
        InputStream is = getResources().openRawResource(R.raw.haarcascade_eye);
        File cascadeDir = getDir("cascade", Context.MODE_PRIVATE);
        File cascadeFile = new File(cascadeDir, "haarcascade_eye.xml");

        try {
            FileOutputStream os = new FileOutputStream(cascadeFile);

            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = is.read(buffer)) != -1) {
                os.write(buffer, 0, bytesRead);
            }

            is.close();
            os.close();

            if (cascadeClassifier.load(cascadeFile.getAbsolutePath())) {
                Log.d("CascadeClassifier", "Haar cascade loaded successfully.");
            } else {
                Log.e("CascadeClassifier", "Failed to load Haar cascade.");
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        // Initialize grayscale image Mat
        grayscaleImage = new Mat();

    }

    private boolean checkLocationPermission() {
        return ContextCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED;
    }

    private void requestLocationPermission() {
        ActivityCompat.requestPermissions(this,
                new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                LOCATION_PERMISSION_REQUEST_CODE);
    }

    private void startGettingLocationUpdates() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED) {
            // Permission is granted, start getting location updates
            fusedLocationClient.requestLocationUpdates(locationRequest, locationCallback, null /* Looper */);
        } else {
            // Permission is denied, show a message or handle this case as appropriate for your app
            Toast.makeText(this, "Location permission is required to get location updates.", Toast.LENGTH_SHORT).show();
        }
    }

    private void updateSourceMarkerWithNominatim(String latitude, String longitude) {
        // Update the source marker position on the map directly using JavaScript
        mapView.evaluateJavascript("addMarker(" + latitude + ", " + longitude + ", 'Source', 'yellow')", null);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == PERMISSIONS_REQUEST_READ_MEDIA_IMAGES) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                openGallery();
            } else {
                metadataTextView.setText("Permission denied to access media storage");
            }
        } else if (requestCode == LOCATION_PERMISSION_REQUEST_CODE) {
            if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                startGettingLocationUpdates();
            } else {
                Toast.makeText(this, "Location permission denied", Toast.LENGTH_SHORT).show();
            }
        }
    }

    private void openGallery() {
        Intent galleryIntent = new Intent(Intent.ACTION_PICK, MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
        startActivityForResult(galleryIntent, REQUEST_GALLERY);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == REQUEST_GALLERY && resultCode == RESULT_OK && data != null) {
            Uri selectedImage = data.getData();
            processImage(selectedImage);
            displayMetadata(selectedImage);
            // Set the selected image to the ImageView
            try {
                InputStream inputStream = getContentResolver().openInputStream(selectedImage);
                Bitmap bitmap = BitmapFactory.decodeStream(inputStream);
                imageView.setImageBitmap(bitmap);
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
    }
    // Method to process selected image for eye detection
    private void processImage(Uri selectedImageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(selectedImageUri);
            Bitmap bitmap = BitmapFactory.decodeStream(inputStream);

            if (bitmap != null) {
                Log.d("ImageProcessing", "Image successfully loaded and decoded.");
            } else {
                Log.e("ImageProcessing", "Failed to load or decode the image.");
            }

            // Convert Bitmap to Mat for OpenCV processing
            Mat imageMat = new Mat();
            Utils.bitmapToMat(bitmap, imageMat);

            if (!imageMat.empty()) {
                Log.d("ImageProcessing", "Image successfully converted to Mat.");
            } else {
                Log.e("ImageProcessing", "Failed to convert the image to Mat.");
            }

            // Convert the image to grayscale for better eye detection
            Imgproc.cvtColor(imageMat, grayscaleImage, Imgproc.COLOR_BGR2GRAY);

            if (!grayscaleImage.empty()) {
                Log.d("ImageProcessing", "Image successfully converted to grayscale.");
            } else {
                Log.e("ImageProcessing", "Failed to convert the image to grayscale.");
            }

            // Perform eye detection
            detectEyes(grayscaleImage);

            // Release resources
            imageMat.release();
            grayscaleImage.release();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void displayMetadata(Uri imageUri) {
        try {
            InputStream inputStream = getContentResolver().openInputStream(imageUri);
            ExifInterface exifInterface = new ExifInterface(inputStream);
            String latitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE);
            String latitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LATITUDE_REF);
            String longitude = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE);
            String longitudeRef = exifInterface.getAttribute(ExifInterface.TAG_GPS_LONGITUDE_REF);

            if (latitude != null && longitude != null && latitudeRef != null && longitudeRef != null) {
                currentLatitude = convertToDegree(latitude, latitudeRef);
                currentLongitude = convertToDegree(longitude, longitudeRef);
                metadataTextView.setText("Latitude: " + currentLatitude + "\nLongitude: " + currentLongitude);
            } else {
                metadataTextView.setText("GPS coordinates not found");
            }
        } catch (Exception e) {
            metadataTextView.setText("Failed to load metadata");
            e.printStackTrace();
        }
    }
    // Method to detect eyes in an image using OpenCV
    // Method to detect eyes in an image using OpenCV and add markers at eye locations
    private void detectEyes(Mat image) {
        MatOfRect eyes = new MatOfRect();
        cascadeClassifier.detectMultiScale(image, eyes, 1.05, 5, 2, new Size(20, 20), new Size());

        if (!eyes.empty()) {
            Toast.makeText(MainActivity.this, "Eye detected", Toast.LENGTH_SHORT).show();
            isEyeDetected = true;
            // Proceed with other actions based on eye detection
        } else {
            Toast.makeText(MainActivity.this, "No eye detected", Toast.LENGTH_SHORT).show();
            isEyeDetected = false;
            // Proceed with other actions if no eye is detected
        }
    }
    private void drawMarkerOnImageView(int x, int y) {
        // Create a new ImageView to hold the marker image
        ImageView markerImageView = new ImageView(MainActivity.this);
        markerImageView.setImageResource(R.drawable.circle_marker); // Use the correct resource ID

        // Define layout parameters for the marker ImageView
        FrameLayout.LayoutParams layoutParams = new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.WRAP_CONTENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
        );
        layoutParams.gravity = Gravity.START | Gravity.TOP; // Position the marker at (x, y) coordinates
        layoutParams.leftMargin = x; // Set left margin to x coordinate
        layoutParams.topMargin = y; // Set top margin to y coordinate

        // Add the marker ImageView to the marker container (FrameLayout)
        FrameLayout markerContainer = findViewById(R.id.marker_container);
        markerContainer.addView(markerImageView, layoutParams);
    }

    private String convertToDegree(String stringDMS, String ref) {
        String[] DMS = stringDMS.split(",", 3);

        String[] stringD = DMS[0].split("/", 2);
        double degrees = Double.parseDouble(stringD[0]) / Double.parseDouble(stringD[1]);

        String[] stringM = DMS[1].split("/", 2);
        double minutes = Double.parseDouble(stringM[0]) / Double.parseDouble(stringM[1]);

        String[] stringS = DMS[2].split("/", 2);
        double seconds = Double.parseDouble(stringS[0]) / Double.parseDouble(stringS[1]);

        double result = degrees + (minutes / 60) + (seconds / 3600);
        if (ref.equals("S") || ref.equals("W")) {
            result = -result;
        }

        return String.valueOf(result);
    }

    public void fetchNearbyHospitals(String latitude, String longitude) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NominatimService service = retrofit.create(NominatimService.class);

        Call<List<NominatimResponse>> call = service.searchNearbyHospitals(
                latitude + "," + longitude, // String containing latitude and longitude
                "json", // String for the format
                1, // int for the limit
                5, // int for the maximum number of results
                0, // int for polygonGeojson (assuming 0 as default value)
                "hospital" // String for the category
        );

        call.enqueue(new Callback<List<NominatimResponse>>() {
            @Override
            public void onResponse(Call<List<NominatimResponse>> call, Response<List<NominatimResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<NominatimResponse> hospitals = response.body();
                    for (NominatimResponse hospital : hospitals) {
                        addMarkerAndDisplayNavigationPath(hospital);
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to fetch hospitals", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<NominatimResponse>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failed to fetch hospitals: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }
    public void fetchNearbyEyeHospitals(String latitude, String longitude) {
        Retrofit retrofit = new Retrofit.Builder()
                .baseUrl("https://nominatim.openstreetmap.org/")
                .addConverterFactory(GsonConverterFactory.create())
                .build();

        NominatimService service = retrofit.create(NominatimService.class);

        Call<List<NominatimResponse>> call = service.searchNearbyHospitals(
                latitude + "," + longitude, // String containing latitude and longitude
                "json", // String for the format
                1, // int for the limit
                5, // int for the maximum number of results
                0, // int for polygonGeojson (assuming 0 as default value)
                "hospital" // String for the category
        );

        call.enqueue(new Callback<List<NominatimResponse>>() {
            @Override
            public void onResponse(Call<List<NominatimResponse>> call, Response<List<NominatimResponse>> response) {
                if (response.isSuccessful() && response.body() != null) {
                    List<NominatimResponse> hospitals = response.body();
                    for (NominatimResponse hospital : hospitals) {
                        // Check if the hospital name or description contains "Eye"
                        if (hospital.getName().toLowerCase().contains("eye") || hospital.getDescription().toLowerCase().contains("eye")) {
                            // This is an eye hospital
                            addMarkerAndDisplayNavigationPath(hospital);
                        }
                    }
                } else {
                    Toast.makeText(MainActivity.this, "Failed to fetch hospitals", Toast.LENGTH_SHORT).show();
                }
            }

            @Override
            public void onFailure(Call<List<NominatimResponse>> call, Throwable t) {
                Toast.makeText(MainActivity.this, "Failed to fetch eye hospitals: " + t.getMessage(), Toast.LENGTH_SHORT).show();
            }
        });
    }

    private void addMarkerAndDisplayNavigationPath(NominatimResponse hospital) {
        // Parse the latitude and longitude from the NominatimResponse object
        double lat = Double.parseDouble(hospital.getLatitude());
        double lon = Double.parseDouble(hospital.getLongitude());

        // Assuming mapView is your WebView object
        mapView.evaluateJavascript("addMarker(" + lat + ", " + lon + ", '" + hospital.getName() + "')", null);
    }
    public WebView getMapView() {
        return mapView;
    }

    public class WebAppInterface {
        private final MainActivity mainActivity;

        public WebAppInterface(MainActivity mainActivity) {
            this.mainActivity = mainActivity;
        }

        @android.webkit.JavascriptInterface
        public void fetchHospitalsFromLocation(double latitude, double longitude) {
            fetchNearbyHospitals(String.valueOf(latitude), String.valueOf(longitude));
        }

        @android.webkit.JavascriptInterface
        public void handleMarkerClick(String markerName) {
            // Handle marker click here, you can start navigation or perform any other action
            Toast.makeText(mainActivity, "Clicked on marker: " + markerName, Toast.LENGTH_SHORT).show();
        }
    }
}
