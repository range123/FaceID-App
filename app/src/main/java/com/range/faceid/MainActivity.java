package com.range.faceid;

import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.annotation.Nullable;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.List;

import clarifai2.api.ClarifaiBuilder;
import clarifai2.api.ClarifaiClient;
import clarifai2.api.ClarifaiResponse;
import clarifai2.dto.input.ClarifaiInput;
import clarifai2.dto.model.Model;
import clarifai2.dto.model.output.ClarifaiOutput;
import clarifai2.dto.prediction.Concept;
import okhttp3.OkHttpClient;

public class MainActivity extends AppCompatActivity
        implements NavigationView.OnNavigationItemSelectedListener {

    static final int REQUEST_IMAGE_CAPTURE = 1,GALLERY_IMAGE = 19;
    int check = 0;
    ClarifaiClient client = new ClarifaiBuilder("3c84e7872d6e4bb98ad74550175d6936").client(new OkHttpClient()).buildSync();

    ImageView imgview;
    Bitmap bitmap;
    TextView tv;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        imgview = findViewById(R.id.imgview);
        tv = findViewById(R.id.tv);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (check == 0)
                    Toast.makeText(MainActivity.this, "Please Add an Image", Toast.LENGTH_SHORT).show();
                else {
                    new AsyncTask<Void, Void, ClarifaiResponse<List<ClarifaiOutput<Concept>>>>() {
                        @Override
                        protected ClarifaiResponse<List<ClarifaiOutput<Concept>>> doInBackground(Void... params) {
                            final Model generalModel = client.getModelByID("FaceID").executeSync().get();
                            ByteArrayOutputStream stream = new ByteArrayOutputStream();
                            bitmap.compress(Bitmap.CompressFormat.PNG, 0, stream);
                            byte[] bitmapData = stream.toByteArray();

                            return generalModel.predict()
                                    //.withInputs(ClarifaiInput.forImage(ClarifaiImage.of(imageBytes)))
                                    .withInputs(ClarifaiInput.forImage(bitmapData))
                                    .executeSync();
                        }

                        @Override
                        protected void onPostExecute(ClarifaiResponse<List<ClarifaiOutput<Concept>>> response) {
                            if (!response.isSuccessful()) {
                                Toast.makeText(MainActivity.this, "Failed", Toast.LENGTH_SHORT).show();
                                ;
                                return;
                            }
                            final List<ClarifaiOutput<Concept>> predictions = response.get();
                            if (predictions.isEmpty()) {
                                Toast.makeText(MainActivity.this, "No predictions", Toast.LENGTH_SHORT).show();
                                return;
                            }

                            List<Concept> concepts = predictions.get(0).data();
                            String maxConcept = null;
                            double maxValue = 0;
                            for (Concept x : concepts) {
                                if (x.value() > maxValue) {
                                    maxValue = x.value();
                                    maxConcept = x.name();
                                }
                            }
                            if (maxValue >= 0.50)
                                tv.setText(maxConcept);
                            else
                                tv.setText("Undefined Face");
                        }


                    }.execute();
                }
            }
        });

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);
        toggle.syncState();

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);

        //CLARIFAI API

        // ConceptModel model = client.getModelByID("FaceID").executeSync()..get().asConceptModel();

    }

    @Override
    public void onBackPressed() {
        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            super.onBackPressed();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data)  {
        if (requestCode == REQUEST_IMAGE_CAPTURE && resultCode == RESULT_OK) {
            Bundle extras = data.getExtras();
            bitmap = (Bitmap) extras.get("data");
            check = 1;
            imgview.setImageBitmap(bitmap);
        }
        else if(requestCode == GALLERY_IMAGE && resultCode == RESULT_OK)
        {
            check = 1;
            Uri imageuri = data.getData();
            try {
                bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(), imageuri);
                imgview.setImageBitmap(bitmap);
            } catch (IOException e) {
                e.printStackTrace();
            }

        }
    }

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.nav_camera) {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
                startActivityForResult(takePictureIntent, REQUEST_IMAGE_CAPTURE);
            }
        } else if (id == R.id.nav_gallery) {
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Select Picture"), GALLERY_IMAGE);

        } else if (id == R.id.nav_slideshow) {

        } else if (id == R.id.nav_manage) {

        }

        DrawerLayout drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }
}
