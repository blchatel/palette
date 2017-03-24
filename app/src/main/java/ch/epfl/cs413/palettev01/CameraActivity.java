package ch.epfl.cs413.palettev01;

import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewTreeObserver;
import android.widget.AdapterView;

import java.io.IOException;

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

        Log.e( "EUREKA", "ON CREATE" );

        setContentView(R.layout.activity_camera);

        ////////////////////////////////////////////////////////////////////////////////////////////
        /// INITIALISATION
        ////////////////////////////////////////////////////////////////////////////////////////////

        //Bitmap
        mPicture = new PaletteBitmap();

        //Miniature
        mView = (Miniature) findViewById(R.id.MAIN_image);
        mView.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                mPicture.setHeight(mView.getHeight());
                mPicture.setWidth(mView.getWidth());
            }
        });

        //Pallette
        palette = (Palette) findViewById(R.id.MAIN_paletteGrid);
        PaletteAdapter adapter = new PaletteAdapter(CameraActivity.this, 6);
        palette.setAdapter(adapter);
        palette.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {

                ((PaletteAdapter) parent.getAdapter()).setSelectedBox(position);
            }
        });

        palette.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {

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

                return true;
            }
        });

        mView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                PaletteAdapter a = ((PaletteAdapter) palette.getAdapter());

                if(!mPicture.isFileNull() && a.isBoxSelected()) {
                    int[] viewCorrs = new int[2];
                    //x = 0; y = 213
                    mView.getLocationOnScreen(viewCorrs);

                    int touchX = (int) event.getX();
                    int touchY = (int) event.getY();

                    int color = mPicture.getColor(touchX, touchY);

                    if (color != Color.TRANSPARENT)
                        a.setColor(color);

                    return true;
                }
                return false;
            }
        });




        ////////////////////////////////////////////////////////////////////////////////////////////
        // RECOVERING THE INSTANCE STATE
        ////////////////////////////////////////////////////////////////////////////////////////////

        if (savedInstanceState != null) {
            mPicture.restaureFile(savedInstanceState.getString("FILE_KEY"));

            mPicture.setHeight(savedInstanceState.getInt("HEIGHT_KEY"));
            mPicture.setWidth(savedInstanceState.getInt("WIDTH_KEY"));
        }


    }


    /**
     * invoked when the activity may be temporarily destroyed, save the instance state here
     * @param outState
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {

        Log.e( "EUREKA", "ON SAVE" );

        if(! mPicture.isFileNull())
            outState.putString("FILE_KEY", mPicture.fileAbsolutePath());

        outState.putInt("HEIGHT_KEY", mPicture.getHeight());
        outState.putInt("WIDTH_KEY", mPicture.getWidth());

        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }


    /**
     *
     * @param menu
     * @return
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        Log.e( "EUREKA", "ON CREATE OPTION MENU" );

        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_camera, menu);
        return true;
    }

    /**
     *
     * @param item
     * @return
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        Log.e( "EUREKA", "ON OPTION ITEM SELECTED" );

        switch (item.getItemId()) {

            case R.id.open_camera_item:

                takePicture();
                return true;

            case R.id.open_gallery_item:

                if(!mPicture.isFileNull()) {
                    selectPicture();
                    return true;
                }
                return false;

            case R.id.black_and_white:
                mPicture.transformBlackAndWhite(mView);
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onDestroy() {
        super.onDestroy();
        mPicture.recycle();
    }

    /**
     *
     * @param requestCode
     * @param resultCode
     * @param data
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        Log.e( "EUREKA", "ON RESULT" );

        // if results comes from the camera activity
        if (resultCode == RESULT_OK && requestCode == CAMERA_RESULT) {

            //TODO not working as expected. GALLERY RESULT WORKS
            galleryAddPic();
            mPicture.setPicture(mView);

        }
        else if (resultCode == RESULT_OK && requestCode == GALLERY_RESULT) {

            Uri selectedImage = data.getData();
            String[] filePath = { MediaStore.Images.Media.DATA };
            Cursor c = getContentResolver().query(selectedImage, filePath,
                    null, null, null);
            c.moveToFirst();
            int columnIndex = c.getColumnIndex(filePath[0]);
            String selectedImagePath = c.getString(columnIndex);
            c.close();

            mPicture.restaureFile(selectedImagePath);

            mPicture.setPicture(mView);
        }
    }


    /**
     * Prepare and Use the Camera Intent to take a picture
     */
    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

            // Create the File where the photo should go
            mPicture.setFileToNull();

            try {
                mPicture.prepareImageFile();
            } catch (IOException ex) {
                // Error occurred while creating the File
                Log.d("ERROR prepareImageFile", "ERROR - Prepare Image File");
            }

            // Continue only if the File was successfully created
            if (!mPicture.isFileNull()) {
                Uri photoUri = mPicture.getUri();
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoUri);
                startActivityForResult(takePictureIntent, CAMERA_RESULT);
            }else{
                Log.d("ERROR file null", "ERROR - File file is null");
            }
        }
    }




    /**
     * Prepare and Use the Gallery Intent to select an existing picture
     */
    private void selectPicture(){

        // Create the File where the photo should go
        mPicture.setFileToNull();

        try {
            Intent intent = new Intent( Intent.ACTION_GET_CONTENT );
            intent.setType("image/*");
            startActivityForResult(intent, GALLERY_RESULT);
        } catch (ActivityNotFoundException e) {
            Log.e("OpenImage ERROR", "No gallery: " + e);
        }
    }


    /**
     * Save this.file into the gallery
     */
    private void galleryAddPic() {

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

        Uri contentUri = mPicture.getUri();

        mediaScanIntent.setData(contentUri);

        this.sendBroadcast(mediaScanIntent);
    }


}