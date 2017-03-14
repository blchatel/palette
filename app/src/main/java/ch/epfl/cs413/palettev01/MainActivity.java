package ch.epfl.cs413.palettev01;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.AdapterView;
import android.widget.GridView;
import android.widget.Toast;

import ch.epfl.cs413.palettev01.views.Miniature;
import ch.epfl.cs413.palettev01.views.Palette;
import ch.epfl.cs413.palettev01.views.PaletteAdapter;

public class MainActivity extends AppCompatActivity {

    private Miniature miniature;
    private Palette palette;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);


        /////////////////////////////////////////////////////


        //Miniature
        miniature = (Miniature) findViewById(R.id.MAIN_image);

        //Pallette
        palette = (Palette) findViewById(R.id.MAIN_paletteGrid);
        PaletteAdapter adapter = new PaletteAdapter(MainActivity.this, 6);
        palette.setAdapter(adapter);
        palette.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                Snackbar.make(view, "Replace with color picker", Snackbar.LENGTH_LONG).setAction("Action", null).show();
            }
        });

        // Floating button for image selection
        FloatingActionButton cameraBtn = (FloatingActionButton) findViewById(R.id.open_camera_btn);
        cameraBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // To take picture from camera:
                Intent takePicture = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                startActivityForResult(takePicture, 0);//zero can be replaced with any action code
            }
        });

        FloatingActionButton galleryBtn = (FloatingActionButton) findViewById(R.id.open_gallery_btn);
        galleryBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                // To pick photo from gallery:
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                startActivityForResult(pickPhoto , 1);//one can be replaced with any action code
            }
        });

        /////////////////////////////////////////////////////

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    protected void onActivityResult(int requestCode, int resultCode, Intent imageReturnedIntent) {
        super.onActivityResult(requestCode, resultCode, imageReturnedIntent);


        Log.d("TEST", "test of pass here");
        Log.d("Code", requestCode +", "+resultCode);

        switch(requestCode) {
            case 0:
                if(resultCode == RESULT_OK){

                //  TODO undersand why this doesnt work yet : supposed to update de miniature by the camera taken pic
                    Uri selectedImage = imageReturnedIntent.getData();
                    miniature.setImageURI(selectedImage);

                }

                break;
            case 1:
                if(resultCode == RESULT_OK){
                    //  TODO undersand why this doesnt work yet : supposed to update de miniature by the gallery taken pic
                    Uri selectedImage = imageReturnedIntent.getData();
                    miniature.setImageURI(selectedImage);
                }
                break;
        }
    }

}
