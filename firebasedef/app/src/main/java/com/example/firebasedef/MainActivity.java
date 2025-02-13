package com.example.firebasedef;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.activity.EdgeToEdge;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowInsetsCompat;
import com.google.android.gms.maps.model.LatLng;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.database.DatabaseReference;
import com.google.firebase.database.FirebaseDatabase;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.GeoPoint;
import com.google.firebase.ktx.Firebase;


import java.util.HashMap;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private FirebaseAuth mAuth;
    private FirebaseFirestore db;
    private EditText emailInput,pwdInput;
    private Button registerButton, loginButton, getUserButton;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mAuth = FirebaseAuth.getInstance();
        db = FirebaseFirestore.getInstance();

        emailInput = findViewById(R.id.emailInput);
        pwdInput = findViewById(R.id.pwdInput);
        registerButton = findViewById(R.id.registerButton);
        loginButton = findViewById(R.id.loginButton);
        getUserButton = findViewById(R.id.getUserButton);

        Intent intent = getIntent();
        double latitude = intent.getDoubleExtra("latitude", 0.0);  // Valor por defecto: 0.0
        double longitude = intent.getDoubleExtra("longitude", 0.0);  // Valor por defecto: 0.0
        String email = intent.getStringExtra("email");
        String password = intent.getStringExtra("password");

        LatLng latLng = new LatLng(latitude, longitude);

        registerButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                String pwd = pwdInput.getText().toString().trim();

                if(!email.isEmpty() && !pwd.isEmpty()){
                    registerUser(email,pwd,latLng);
                } else {
                    Toast.makeText(MainActivity.this, "Completa los campos",Toast.LENGTH_SHORT).show();
                }
            }
        });

        loginButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String email = emailInput.getText().toString().trim();
                String pwd = pwdInput.getText().toString().trim();

                if(!email.isEmpty() && !pwd.isEmpty()){
                    loginUser(email,pwd);
                } else {
                    Toast.makeText(MainActivity.this, "Completa los campos",Toast.LENGTH_SHORT).show();
                }
            }
        });

        getUserButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getUserData();
            }
        });

        FirebaseDatabase database = FirebaseDatabase.getInstance();
        DatabaseReference myRef = database.getReference("message");
        myRef.setValue("Conexion correcta");


    }

    private void getUserData() {
        FirebaseUser user = mAuth.getCurrentUser();
        if (user != null) {
            String userId = user.getUid();
            DocumentReference userRef = db.collection("usuarios").document(userId);

            userRef.get().addOnSuccessListener(documentSnapshot -> {
                if (documentSnapshot.exists()) {
                    String email = documentSnapshot.getString("email");
                    Toast.makeText(MainActivity.this, "Email: " + email, Toast.LENGTH_SHORT).show();
                    Log.d("Firestore", "Datos del usuario: " + email);
                } else {
                    Toast.makeText(MainActivity.this, "No se encontraron datos", Toast.LENGTH_SHORT).show();
                }
            }).addOnFailureListener(e -> {
                Log.e("Firestore", "Error al obtener datos: " + e.getMessage());
            });
        } else {
            Toast.makeText(MainActivity.this, "Usuario no autenticado", Toast.LENGTH_SHORT).show();
        }
    }

    public void loginUser(String email, String pwd) {
        mAuth.signInWithEmailAndPassword(email, pwd).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                Toast.makeText(MainActivity.this, "Inicio de sesión exitoso: " + user.getEmail(), Toast.LENGTH_SHORT).show();
                Log.d("FirebaseAuth", "Inicio de sesión: " + user.getEmail());

                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Error al iniciar sesión: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("FirebaseAuth", "Error: " + task.getException().getMessage());
            }
        });
    }

    public void registerUser(String email, String pwd, LatLng latLng) {
        mAuth.createUserWithEmailAndPassword(email, pwd).addOnCompleteListener(task -> {
            if (task.isSuccessful()) {
                FirebaseUser user = mAuth.getCurrentUser();
                if (user != null) {
                    saveUserToFirestore(user.getUid(), email, latLng);
                }
                Toast.makeText(MainActivity.this, "Registro exitoso: " + email, Toast.LENGTH_SHORT).show();
                Log.d("FirebaseAuth", "Usuario registrado: " + email);

                Intent intent = new Intent(MainActivity.this, MapsActivity.class);
                startActivity(intent);
            } else {
                Toast.makeText(MainActivity.this, "Error al registrar: " + task.getException().getMessage(), Toast.LENGTH_SHORT).show();
                Log.e("FirebaseAuth", "Error: " + task.getException().getMessage());
            }
        });
    }

    private void saveUserToFirestore(String userId, String email, LatLng latlng) {
        Map<String, Object> user = new HashMap<>();
        user.put("email", email);
        user.put("ubicacion", new GeoPoint(latlng.latitude,latlng.longitude));

        db.collection("usuarios").document(userId)
                .set(user)
                .addOnSuccessListener(aVoid -> Log.d("Firestore", "Usuario guardado en Firestore"))
                .addOnFailureListener(e -> Log.e("Firestore", "Error al guardar usuario: " + e.getMessage()));
    }
}