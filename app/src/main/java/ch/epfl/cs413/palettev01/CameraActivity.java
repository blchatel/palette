package ch.epfl.cs413.palettev01;

import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.provider.DocumentsContract;
import android.provider.MediaStore;
import android.support.v4.graphics.ColorUtils;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ProgressBar;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.epfl.cs413.palettev01.processing.Kmeans;
import ch.epfl.cs413.palettev01.processing.LabColor;
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

        //Pallette
        palette = (Palette) findViewById(R.id.MAIN_paletteGrid);
        PaletteAdapter adapter = new PaletteAdapter(CameraActivity.this, PaletteAdapter.PALETTE_SIZE);
        palette.setAdapter(adapter);
        palette.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {
                final PaletteAdapter pA = (PaletteAdapter) parent.getAdapter();
                pA.setSelectedBox(position);

                // initialColor is the initially-selected color to be shown in the rectangle on the left of the arrow.
                // for example, 0xff000000 is black, 0xff0000ff is blue. Please be aware of the initial 0xff which is the alpha.
                AmbilWarnaDialog dialog = new AmbilWarnaDialog(CameraActivity.this, ((PaletteAdapter)parent.getAdapter()).getColor(position),
                        new AmbilWarnaDialog.OnAmbilWarnaListener(){

                            @Override
                            public void onOk(AmbilWarnaDialog dialog, int color) {
                                // color is the color selected by the user

                                /// TODO : Here we just changed a color in Palette !
                                /// Transform the palette's colors
                                ((PaletteAdapter) parent.getAdapter()).updateAll(position, color);

                                /// TODO : Should apply transform to bitmap
                                // If there is a picture to modify
                                if(!mPicture.isFileNull()) {
                                    // We transform the grid
                                    mPicture.transGrid(palette, position);

                                    // And finally we can also transform the image
                                    mPicture.transImage(mView);
                                }

                                pA.setSelectedBox(-1);
                            }

                            @Override
                            public void onCancel(AmbilWarnaDialog dialog) {
                                // cancel was selected by the user
                                pA.setSelectedBox(-1);
                            }
                        });
                dialog.show();
            }
        });

        palette.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {
                ((PaletteAdapter) parent.getAdapter()).setSelectedBox(position);
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
            Log.e( "EUREKA", "ON RESTORE" );
            mPicture.restoreFile(savedInstanceState.getString("FILE_KEY"));

            launchAsyncPaletteExtract();

            mPicture.setHeight(savedInstanceState.getInt("HEIGHT_KEY"));
            mPicture.setWidth(savedInstanceState.getInt("WIDTH_KEY"));
        }
    }

    @Override
    public void onWindowFocusChanged(boolean hasFocus){
        mPicture.setHeight(mView.getHeight());
        mPicture.setWidth(mView.getWidth());
    }

    /**
     * It will simply launch a palette extraction in background
     */
    private void launchAsyncPaletteExtract() {
        /// TODO : Put a charging indicator
        if (!mPicture.isEmpty()) {
            final ProgressBar paletteProgressBar = (ProgressBar)(findViewById(R.id.palette_progressbar));
            paletteProgressBar.setVisibility(View.VISIBLE);
            palette.setVisibility(View.GONE);
            AsyncTask<Object, Object, List<LabColor>> extractPalette = new AsyncTask<Object, Object, List<LabColor>>(){

                @Override
                protected List<LabColor> doInBackground(Object... params) {
                    int paletteSize = PaletteAdapter.PALETTE_SIZE;
                    double scaleFactor = mPicture.getScaled().getWidth() / 200.0;
                    Bitmap smallImage = Bitmap.createScaledBitmap(mPicture.getScaled(), (int)(mPicture.getScaled().getWidth()/scaleFactor), (int)(mPicture.getScaled().getHeight()/scaleFactor), false);
                    Kmeans kmeans = new Kmeans(paletteSize, smallImage);
                    List<LabColor> paletteColors = kmeans.run();
                    Collections.sort(paletteColors, new Comparator<LabColor>() {
                        @Override
                        public int compare(LabColor o1, LabColor o2) {
                            return (o1.getL() < o2.getL()) ? 1 : (o1.getL() > o2.getL()) ? -1 : 0;
                        }
                    });
                    return paletteColors;
                }

                @Override
                protected void onPostExecute(List<LabColor> labColors) {
                    for (int i = 0; i < PaletteAdapter.PALETTE_SIZE; i++) {
                        LabColor Lab = labColors.get(i);
                        ((PaletteAdapter)palette.getAdapter()).setColor(i, ColorUtils.LABToColor(Lab.getL(), Lab.getA(), Lab.getB()));
                    }

                    // Init the palette
                    mPicture.initTransPalette(palette);
                    palette.setVisibility(View.VISIBLE);
                    paletteProgressBar.setVisibility(View.GONE);
                }
            };

            extractPalette.execute();
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

            case R.id.my_function:
                if(!mPicture.isFileNull()) {
//                    long startTime = System.nanoTime();
//                    long consumingTime;
                    mPicture.rsInit(this);
//                    consumingTime = System.nanoTime() - startTime;
//                    Log.d("time", Long.toString(consumingTime));
                    mPicture.initGrid();
                    mPicture.testInitTransPalette(palette);
                    mPicture.testTransGrid(palette);
//                    consumingTime = System.nanoTime() - startTime;
//                    Log.d("time", Long.toString(consumingTime));
                    mPicture.transImage(mView);
//                    consumingTime = System.nanoTime() - startTime;
//                    Log.d("time", Long.toString(consumingTime));
                    // mPicture.initTransPalette(palette);
//                     mPicture.myFunction(mView);
//                    mPicture.rsClose();
//                    consumingTime = System.nanoTime() - startTime;
//                    Log.d("time", Long.toString(consumingTime));

                    return true;
                }
                return false;

            case R.id.open_camera_item:
                takePicture();
                return true;

            case R.id.open_gallery_item:
                selectPicture();
                return true;

            case R.id.black_and_white:
                if(!mPicture.isFileNull()) {
                    mPicture.transformBlackAndWhite(mView);
                    return true;
                }
                return false;

            case R.id.extract_palette:
                if(!mPicture.isFileNull()) {
                    mPicture.extractPalette(palette);
                    return true;
                }
                return false;
            default:
                return super.onOptionsItemSelected(item);
        }
    }

    public void onDestroy() {
        Log.e( "EUREKA", "ON DESTROY" );
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

            Uri selectedImage = galleryAddPic();

            String selectedImagePath = getPath(this, selectedImage);

            mPicture.restoreFile(selectedImagePath);
            mPicture.setPicture(mView);
            // We launch the extraction of the palette here in async
            launchAsyncPaletteExtract();

        }
        // if result comes from the gallery
        else if (resultCode == RESULT_OK && requestCode == GALLERY_RESULT) {

            Uri selectedImage = data == null ? null : data.getData();
            String selectedImagePath = getPath(this, selectedImage);

            mPicture.restoreFile(selectedImagePath);
            mPicture.setPicture(mView);
            // We launch the extraction of the palette here in async
            launchAsyncPaletteExtract();
        }

        if (mPicture != null) {
            try {
                mPicture.rsClose();
            } catch (Exception e) {
                // To avoid closing a non existing element
            }

            // We initialise the render script
            mPicture.rsInit(getApplicationContext());
            // Init the grid
            mPicture.initGrid();
        }
    }


    /**
     * Prepare and Use the Camera Intent to take a picture
     */
    private void takePicture() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);

        // Ensure that there's a camera activity to handle the intent
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {

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

        try {
            if (Build.VERSION.SDK_INT < 19) {
                Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
                intent.setType("image/*");
                startActivityForResult(intent, GALLERY_RESULT);
            } else {
                Intent intent = new Intent( Intent.ACTION_OPEN_DOCUMENT );
                intent.addCategory(Intent.CATEGORY_OPENABLE);
                intent.setType("image/*");
                startActivityForResult(intent, GALLERY_RESULT);
            }
        } catch (ActivityNotFoundException e) {
            Log.e("OpenImage ERROR", "No gallery: " + e);
        }
    }


    /**
     * Save this.file into the gallery
     */
    private Uri galleryAddPic() {

        Intent mediaScanIntent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);

        Uri contentUri = mPicture.getUri();
        mediaScanIntent.setData(contentUri);

        this.sendBroadcast(mediaScanIntent);
        return mediaScanIntent.getData();

//        return contentUri;
    }



    /**
     * Get a file path from a Uri. This will get the the path for Storage Access
     * Framework Documents, as well as the _data field for the MediaStore and
     * other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     */
    public static String getPath(final Context context, final Uri uri) {

        final boolean isKitKat = Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT;

        // DocumentProvider
        if (isKitKat && DocumentsContract.isDocumentUri(context, uri)) {
            // ExternalStorageProvider
            if (isExternalStorageDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                if ("primary".equalsIgnoreCase(type)) {
                    return Environment.getExternalStorageDirectory() + "/" + split[1];
                }

                // TODO handle non-primary volumes
            }
            // DownloadsProvider
            else if (isDownloadsDocument(uri)) {

                final String id = DocumentsContract.getDocumentId(uri);
                final Uri contentUri = ContentUris.withAppendedId(
                        Uri.parse("content://downloads/public_downloads"), Long.valueOf(id));

                return getDataColumn(context, contentUri, null, null);
            }
            // MediaProvider
            else if (isMediaDocument(uri)) {
                final String docId = DocumentsContract.getDocumentId(uri);
                final String[] split = docId.split(":");
                final String type = split[0];

                Uri contentUri = null;
                if ("image".equals(type)) {
                    contentUri = MediaStore.Images.Media.EXTERNAL_CONTENT_URI;
                } else if ("video".equals(type)) {
                    contentUri = MediaStore.Video.Media.EXTERNAL_CONTENT_URI;
                } else if ("audio".equals(type)) {
                    contentUri = MediaStore.Audio.Media.EXTERNAL_CONTENT_URI;
                }

                final String selection = "_id=?";
                final String[] selectionArgs = new String[] {
                        split[1]
                };

                return getDataColumn(context, contentUri, selection, selectionArgs);
            }
        }
        // MediaStore (and general)
        else if ("content".equalsIgnoreCase(uri.getScheme())) {

            // Return the remote address
            if (isGooglePhotosUri(uri))
                return uri.getLastPathSegment();

            return getDataColumn(context, uri, null, null);
        }
        // File
        else if ("file".equalsIgnoreCase(uri.getScheme())) {
            return uri.getPath();
        }

        return null;
    }

    /**
     * Get the value of the data column for this Uri. This is useful for
     * MediaStore Uris, and other file-based ContentProviders.
     *
     * @param context The context.
     * @param uri The Uri to query.
     * @param selection (Optional) Filter used in the query.
     * @param selectionArgs (Optional) Selection arguments used in the query.
     * @return The value of the _data column, which is typically a file path.
     */
    public static String getDataColumn(Context context, Uri uri, String selection,
                                       String[] selectionArgs) {

        Cursor cursor = null;
        final String column = "_data";
        final String[] projection = {
                column
        };

        try {
            cursor = context.getContentResolver().query(uri, projection, selection, selectionArgs,
                    null);
            if (cursor != null && cursor.moveToFirst()) {
                final int index = cursor.getColumnIndexOrThrow(column);
                return cursor.getString(index);
            }
        } finally {
            if (cursor != null)
                cursor.close();
        }
        return null;
    }


    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is ExternalStorageProvider.
     */
    public static boolean isExternalStorageDocument(Uri uri) {
        return "com.android.externalstorage.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is DownloadsProvider.
     */
    public static boolean isDownloadsDocument(Uri uri) {
        return "com.android.providers.downloads.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is MediaProvider.
     */
    public static boolean isMediaDocument(Uri uri) {
        return "com.android.providers.media.documents".equals(uri.getAuthority());
    }

    /**
     * @param uri The Uri to check.
     * @return Whether the Uri authority is Google Photos.
     */
    public static boolean isGooglePhotosUri(Uri uri) {
        return "com.google.android.apps.photos.content".equals(uri.getAuthority());
    }


}