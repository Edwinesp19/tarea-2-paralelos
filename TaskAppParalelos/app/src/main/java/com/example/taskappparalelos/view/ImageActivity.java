package com.example.taskappparalelos.view;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.lifecycle.Observer;

import com.example.taskappparalelos.R;
import com.example.taskappparalelos.utils.FileUtils;
import com.example.taskappparalelos.viewmodel.ImageViewModel;
import com.example.taskappparalelos.adapters.ImagePreviewAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class ImageActivity extends AppCompatActivity {
    private ImageViewModel imageViewModel;
    private GridView gridView,gridViewUnsplash;
    private ProgressBar progressBar;
    private Button btnUpload;
    ImageView btnBack;

    private EditText etSearch;
    private final List<String> selectedImagePaths = new ArrayList<>();
    private final List<Uri> selectedImageUris = new ArrayList<>();
    private final List<Uri> unsplashImageUris = new ArrayList<>();

    private final ActivityResultLauncher<String> imagePickerLauncher = registerForActivityResult(
            new ActivityResultContracts.GetMultipleContents(),
            uris -> {
                selectedImageUris.clear();
                selectedImagePaths.clear();

                for (Uri uri : uris) {
                    selectedImageUris.add(uri);
                    selectedImagePaths.add(FileUtils.getPathFromUri(this, uri));
                }

                gridView.setAdapter(new ImagePreviewAdapter(this, selectedImageUris));
            }
    );

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_image);

        imageViewModel = new ViewModelProvider(this).get(ImageViewModel.class);
        btnBack = findViewById(R.id.btnBack);

        gridView = findViewById(R.id.gridView);
        gridViewUnsplash = findViewById(R.id.gridViewUnsplash);
        progressBar = findViewById(R.id.progressBar);
        btnUpload = findViewById(R.id.btnUpload);

        etSearch = findViewById(R.id.etSearch);

        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                navBack();
            }
        });

        // Escuchar búsquedas
        etSearch.setOnEditorActionListener((v, actionId, event) -> {
            String query = etSearch.getText().toString();
            if (!query.isEmpty()) {
                progressBar.setVisibility(View.VISIBLE);
                imageViewModel.searchUnsplashImages(query);
            }
            return true;
        });

        // Observa las imágenes obtenidas de Unsplash
        imageViewModel.getUnsplashImages().observe(this, images -> {
            progressBar.setVisibility(View.GONE);
            if (images != null) {
                unsplashImageUris.clear();
                for (int i = 0; i < images.size(); i++) {
                    String imageUrl = images.get(i).getAsJsonObject().get("urls").getAsJsonObject().get("small").getAsString();
                    unsplashImageUris.add(Uri.parse(imageUrl));
                }
                gridViewUnsplash.setAdapter(new ImagePreviewAdapter(this, unsplashImageUris));
            } else {
                Toast.makeText(this, "Error al buscar imágenes", Toast.LENGTH_SHORT).show();
            }
        });


        findViewById(R.id.btnSelectImages).setOnClickListener(v -> imagePickerLauncher.launch("image/*"));

        btnUpload.setOnClickListener(v -> {
            if (selectedImagePaths.isEmpty()) {
                Toast.makeText(this, "Selecciona al menos una imagen", Toast.LENGTH_SHORT).show();
            } else {
                progressBar.setVisibility(View.VISIBLE);
                imageViewModel.uploadImages(selectedImagePaths);

                // Realizar búsqueda en Unsplash al mismo tiempo
                String query = etSearch.getText().toString();
                if (!query.isEmpty()) {
                    imageViewModel.searchUnsplashImages(query);
                } else {
                    Toast.makeText(this, "Por favor, ingresa un término de búsqueda", Toast.LENGTH_SHORT).show();
                }
            }
        });

        imageViewModel.getUploadStatus().observe(this, new Observer<Map<String, String>>() {
            @Override
            public void onChanged(Map<String, String> statusMap) {
                progressBar.setVisibility(View.GONE);
                StringBuilder statusMessage = new StringBuilder();
                for (Map.Entry<String, String> entry : statusMap.entrySet()) {
                    statusMessage.append(entry.getKey()).append(": ").append(entry.getValue()).append("\n");
                }
                Toast.makeText(ImageActivity.this, statusMessage.toString(), Toast.LENGTH_SHORT).show();
            }
        });


    }

    private void navBack() {
        setResult(RESULT_OK); // Indica que hubo un cambio en los datos
        finish(); // Cierra la actividad actual
    }
}
