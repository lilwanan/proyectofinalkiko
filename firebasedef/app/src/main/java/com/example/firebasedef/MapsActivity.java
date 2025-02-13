package com.example.firebasedef;

import androidx.fragment.app.FragmentActivity;

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.os.Bundle;
import android.widget.SearchView;
import com.google.android.gms.maps.CameraUpdateFactory;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.LatLng;
import com.google.android.gms.maps.model.Marker;
import com.google.android.gms.maps.model.MarkerOptions;
import com.example.firebasedef.databinding.ActivityMapsBinding;
import android.location.Address;
import android.location.Geocoder;
import android.Manifest;
import android.content.pm.PackageManager;
import android.widget.Toast;
import androidx.core.app.ActivityCompat;
import com.google.android.gms.maps.model.BitmapDescriptor;
import com.google.android.gms.maps.model.BitmapDescriptorFactory;
import com.google.android.gms.maps.model.PolylineOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.io.IOException;
import java.util.List;

public class MapsActivity extends FragmentActivity implements OnMapReadyCallback {

    private GoogleMap mMap;
    private ActivityMapsBinding binding;
    private SearchView searchView;
    private Marker currentMarker;
    private LatLng originPoint;
    private LatLng destinationPoint;
    private Marker originMarker;
    private Marker destinationMarker;
    FirebaseFirestore db = FirebaseFirestore.getInstance();


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityMapsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        // Instancia de Firestore



        // Obtener el SearchView desde el layout
        searchView = findViewById(R.id.search_view);  // Asegúrate que el ID sea correcto en el XML

        // Configura el Listener para el SearchView
        searchView.setOnQueryTextListener(new SearchView.OnQueryTextListener() {
            @Override
            public boolean onQueryTextSubmit(String query) {
                searchPlace(query);  // Realizar búsqueda al enviar el texto
                return true;
            }

            @Override
            public boolean onQueryTextChange(String newText) {
                // Opcional: Puedes manejar el cambio en el texto
                return false;
            }
        });

        // Obtener el fragmento del mapa
        SupportMapFragment mapFragment = (SupportMapFragment) getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
    }

    // Método para realizar la búsqueda de lugar
    private void searchPlace(String query) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocationName(query, 1);
            if (addresses != null && !addresses.isEmpty()) {
                Address address = addresses.get(0);
                LatLng latLng = new LatLng(address.getLatitude(), address.getLongitude());

                // Mueve la cámara y coloca el marcador
                mMap.moveCamera(CameraUpdateFactory.newLatLngZoom(latLng, 15));
                mMap.addMarker(new MarkerOptions().position(latLng).title(query));
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Método para habilitar la ubicación del usuario en el mapa
    private void enableUserLocation() {
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            mMap.setMyLocationEnabled(true);  // Habilitar ubicación
            mMap.getUiSettings().setMyLocationButtonEnabled(true);  // Mostrar botón de ubicación
        } else {
            // Solicitar permisos si no están concedidos
            ActivityCompat.requestPermissions(this,
                    new String[]{Manifest.permission.ACCESS_FINE_LOCATION},
                    1);
        }
    }

    // Método para agregar un marcador con ícono personalizado
    private void addCustomMarker() {
        // Coordenada donde quieres colocar el marcador
        LatLng customLocation = new LatLng(36.69192096610655, -4.460193933912516);  // Ubicación del marcador

        // Obtener el Bitmap original de la imagen
        Bitmap originalBitmap = BitmapFactory.decodeResource(getResources(), R.drawable.mapsandlocation);

        // Redimensionar el bitmap (por ejemplo, 100x100 píxeles)
        int width = 100;  // Ancho del marcador en píxeles
        int height = 100; // Alto del marcador en píxeles
        Bitmap resizedBitmap = Bitmap.createScaledBitmap(originalBitmap, width, height, false);

        // Crear un BitmapDescriptor a partir del Bitmap redimensionado
        BitmapDescriptor icon = BitmapDescriptorFactory.fromBitmap(resizedBitmap);

        // Agregar el marcador con el ícono redimensionado
        mMap.addMarker(new MarkerOptions().position(customLocation).title("Custom Marker").icon(icon));
    }

    // Método para trazar una ruta entre dos puntos
    private void drawRoute(LatLng origin, LatLng destination) {
        PolylineOptions polylineOptions = new PolylineOptions().add(origin).add(destination).color(0xFFFF0000).width(5);
        mMap.addPolyline(polylineOptions);
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mMap = googleMap;

        mMap.getUiSettings().setZoomControlsEnabled(true);
        mMap.getUiSettings().setScrollGesturesEnabled(true);
        mMap.getUiSettings().setZoomGesturesEnabled(true);

        // Establecer el tipo de mapa y agregar un marcador en una ubicación predeterminada
        mMap.setMapType(GoogleMap.MAP_TYPE_NORMAL);

        // Agregar un marcador en Sydney
        LatLng sydney = new LatLng(-34, 151);
        mMap.addMarker(new MarkerOptions().position(sydney).title("Marker in Sydney"));
        mMap.moveCamera(CameraUpdateFactory.newLatLng(sydney));

        // Habilitar la ubicación del usuario
        enableUserLocation();

        // Agregar un marcador con ícono personalizado
        addCustomMarker();

        // Trazar una ruta entre dos ubicaciones
        LatLng origin = new LatLng(36.69192096610655, -4.460193933912516);  // Medac
        LatLng destination = new LatLng(36.71094694138073, -4.453370080044371);  // mi casa
        drawRoute(origin, destination);

        mMap.setOnMapClickListener(new GoogleMap.OnMapClickListener() {
            @Override
            public void onMapClick(LatLng latLng) {
                // Si no hay un marcador de origen, establece el marcador de origen
                if (originPoint == null) {
                    originPoint = latLng;

                    // Eliminar el marcador de origen anterior si existe
                    if (originMarker != null) {
                        originMarker.remove();
                    }

                    // Crear el nuevo marcador de origen
                    originMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Punto de inicio"));
                } else {
                    // Si ya existe un origen, establece el marcador de destino
                    destinationPoint = latLng;

                    // Eliminar el marcador de destino anterior si existe
                    if (destinationMarker != null) {
                        destinationMarker.remove();
                    }

                    // Crear el nuevo marcador de destino
                    destinationMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Punto de destino"));

                    // Traza la ruta entre los dos puntos seleccionados
                    drawRoute(originPoint, destinationPoint);
                }

                // Si hay un marcador anterior de la localización seleccionada, eliminarlo
                if (currentMarker != null) {
                    currentMarker.remove();
                }

                // Coloca un marcador donde el usuario haga clic
                currentMarker = mMap.addMarker(new MarkerOptions().position(latLng).title("Localización seleccionada"));

                // Llama a la geocodificación inversa
                reverseGeocode(latLng);
                sendLocationToFirebase(latLng);


            }





        });


    }




    // Geocodificación inversa: obtener la dirección a partir de las coordenadas
    private void reverseGeocode(LatLng latLng) {
        Geocoder geocoder = new Geocoder(this);
        try {
            List<Address> addresses = geocoder.getFromLocation(latLng.latitude, latLng.longitude, 1);
            if (addresses != null && !addresses.isEmpty()) {
                String address = addresses.get(0).getAddressLine(0);
                Toast.makeText(this, "Address: " + address, Toast.LENGTH_LONG).show();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    private void sendLocationToFirebase(LatLng latLng) {
        // Obtener el usuario autenticado
        FirebaseUser user = FirebaseAuth.getInstance().getCurrentUser();
        if (user != null) {
            // Crea un objeto para almacenar la ubicación
            LocationData locationData = new LocationData(latLng.latitude, latLng.longitude);

            // Obtén la colección de usuarios en Firestore (puedes ajustar la colección según tu modelo de datos)
            CollectionReference locationsRef = db.collection("locations");

            // Guarda los datos en la colección
            locationsRef.add(locationData)
                    .addOnSuccessListener(documentReference -> {
                        // Se ha guardado correctamente
                        Toast.makeText(MapsActivity.this, "Ubicación guardada exitosamente", Toast.LENGTH_SHORT).show();
                    })
                    .addOnFailureListener(e -> {
                        // Ocurrió un error al guardar
                        Toast.makeText(MapsActivity.this, "Error al guardar la ubicación", Toast.LENGTH_SHORT).show();
                    });
        } else {
            Toast.makeText(MapsActivity.this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }



}
