package ch.epfl.cs413.palettev01;

import android.app.Activity;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.AdapterView;

import ch.epfl.cs413.palettev01.processing.PaletteBitmap;
import ch.epfl.cs413.palettev01.views.Miniature;
import ch.epfl.cs413.palettev01.views.Palette;
import ch.epfl.cs413.palettev01.views.PaletteAdapter;
import yuku.ambilwarna.AmbilWarnaDialog;


public class CameraActivity extends AppCompatActivity {

    private static final int CAMERA_RESULT = 9;
    private static final int GALLERY_RESULT = 8;

    private PaletteBitmap mPicture;
    private Miniature mView;
    private Palette palette;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_camera);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        /////////////////////////////////////////////////////

        //Bitmap
        mPicture = new PaletteBitmap();


        //Miniature
        mView = (Miniature) findViewById(R.id.MAIN_image);


        //Pallette
        palette = (Palette) findViewById(R.id.MAIN_paletteGrid);
        PaletteAdapter adapter = new PaletteAdapter(CameraActivity.this, 6);
        palette.setAdapter(adapter);
        palette.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {


                PaletteAdapter pA = (PaletteAdapter) parent.getAdapter();

                // initialColor is the initially-selected color to be shown in the rectangle on the left of the arrow.
                // for example, 0xff000000 is black, 0xff0000ff is blue. Please be aware of the initial 0xff which is the alpha.
                AmbilWarnaDialog dialog = new AmbilWarnaDialog(CameraActivity.this, ((PaletteAdapter)parent.getAdapter()).getColor(position),
                        new AmbilWarnaDialog.OnAmbilWarnaListener(){

                    @Override
                    public void onOk(AmbilWarnaDialog dialog, int color) {
                        // color is the color selected by the user.
                        ((PaletteAdapter) parent.getAdapter()).setColor(position, color);
                    }

                    @Override
                    public void onCancel(AmbilWarnaDialog dialog) {
                        // cancel was selected by the user
                    }
                });
                dialog.show();

            }
        });
        /////////////////////////////////////////////////////

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }


    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.open_camera_item:
                Intent camera = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
                CameraActivity.this.startActivityForResult(camera, CAMERA_RESULT);
                return true;
            case R.id.open_gallery_item:
                Intent pickPhoto = new Intent(Intent.ACTION_PICK, android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI);
                CameraActivity.this.startActivityForResult(pickPhoto , GALLERY_RESULT);//one can be replaced with any action code

                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mPicture.recycle();
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        // if results comes from the camera activity
        if (resultCode == Activity.RESULT_OK && requestCode == CAMERA_RESULT) {

            // if a picture was taken
            // Free the data of the last picture
            mPicture.recycle();

            // Get the picture taken by the user
            mPicture.setBitmap((Bitmap) data.getExtras().get("data"));

            // Avoid IllegalStateException with Immutable bitmap
            mPicture.makeMutable();

            // Show the picture
            mPicture.showBitmap(mView);

        }
        else if (resultCode == RESULT_OK && requestCode == GALLERY_RESULT) {

            mPicture.recycle();

            Uri selectedImage = data.getData();
            String[] filePath = { MediaStore.Images.Media.DATA };
            Cursor c = getContentResolver().query(selectedImage, filePath,
                    null, null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePath[0]);
            String selectedImagePath = c.getString(columnIndex);
            c.close();

            mPicture.setBitmap(BitmapFactory.decodeFile(selectedImagePath)); // load
            mPicture.makeMutable();

            mPicture.showBitmap(mView);
        }
    }
}