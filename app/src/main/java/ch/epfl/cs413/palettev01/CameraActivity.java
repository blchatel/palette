package ch.epfl.cs413.palettev01;

import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ContentUris;
import android.content.Context;
import android.content.DialogInterface;
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
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.widget.AdapterView;
import android.widget.Toast;

import java.io.IOException;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import ch.epfl.cs413.palettev01.processing.Kmeans;
import ch.epfl.cs413.palettev01.processing.LabColor;
import ch.epfl.cs413.palettev01.processing.PaletteBitmap;
import ch.epfl.cs413.palettev01.processing.RSProcessing;
import ch.epfl.cs413.palettev01.views.Miniature;
import ch.epfl.cs413.palettev01.views.OurPalette;
import ch.epfl.cs413.palettev01.views.PaletteAdapter;
import yuku.ambilwarna.AmbilWarnaDialog;

/**
 * The activity is the main and only one of the palette application.
 * It is divided into a menu bar, a minature picture and a palette of color
 *
 * This activity can be into two modes : Main mode and Edit mode
 * This activity support indent of camera and gallery
 */
public class CameraActivity extends AppCompatActivity {

    // Definition of Static constant for tests
    private static final boolean DEBUG_LOG = false;

    private static final int CAMERA_RESULT = 9;
    private static final int GALLERY_RESULT = 8;
    private static final int MAIN_MENU = 0;
    private static final int EDIT_MENU = 1;

    /**
     *  The palette bitmap contains all needed information to work with the selected picture
     *  @see PaletteBitmap
     */
    private PaletteBitmap mPicture;


    /**
     * Miniature is an ImageView containing the display of the picture we working on
     * @see Miniature
     */
    private Miniature mView;


    /**
     * RSProcessing is a wrapper for renderscript parts.
     * @see RSProcessing
     */
    private RSProcessing rsProcessing;


    /**
     * Our palette is a grid view containing the palette elements and in edit mode some tools
     * @see OurPalette
     */
    private OurPalette ourPalette;


    // Used for the touching interaction with the color boxes
    private float historicX = Float.NaN;
    private float historicY = Float.NaN;
    private static final int DELTA = 75;    // Distance needed to remove by sliding

    /**
     * The activity menu which is different for the two mode
     */
    private Menu menu;


    /**
     * Contains either MAIN_MENU or EDIT_MENU value
     */
    private int currentMenuMode;


    /**
     * Dialog containing the Help information
     */
    private AlertDialog helpDialog;


    /**
     * The path of the image to which we want to reset
     */
    private String initialImagePath = "";


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        if(DEBUG_LOG) {
            Log.e("CYCLE", "ON CREATE");
        }
        setContentView(R.layout.activity_camera);

        ////////////////////////////////////////////////////////////////////////////////////////////
        /// INITIALISATION
        ////////////////////////////////////////////////////////////////////////////////////////////
        //Bitmap
        mPicture = new PaletteBitmap();

        //Miniature
        mView = (Miniature) findViewById(R.id.MAIN_image);

        //RSProcessing
        rsProcessing = new RSProcessing();
        // We initialise the render script to make sure it's usable
        rsProcessing.rsInit(getApplicationContext());

        //Palette
        ourPalette = (OurPalette) findViewById(R.id.MAIN_paletteGrid);
        PaletteAdapter adapter = new PaletteAdapter(CameraActivity.this, PaletteAdapter.PALETTE_SIZE);
        ourPalette.setAdapter(adapter);


        // Add a listener on the item simple click.
        // Warning : we must deal with both edit and main mode
        ourPalette.setOnItemClickListener(new AdapterView.OnItemClickListener() {

            @Override
            public void onItemClick(final AdapterView<?> parent, View view, final int position, long id) {

                // Get the adapter
                final PaletteAdapter pA = (PaletteAdapter) parent.getAdapter();

                // If the item is a palette color
                if(position < pA.getSize()) {

                    // change the select status of the current box
                    pA.setSelectedBox(position);

                    // if there is one box selected (previous step select and not deselect)
                    if (pA.isBoxSelected()) {
                        // Create a Dialog using the AmbilWarnaDialog library
                        // https://github.com/yukuku/ambilwarna
                        // initialColor is the initially-selected color to be shown in the rectangle on the left of the arrow.
                        // for example, 0xff000000 is black, 0xff0000ff is blue. Please be aware of the initial 0xff which is the alpha.
                        AmbilWarnaDialog dialog = new AmbilWarnaDialog(CameraActivity.this, ((PaletteAdapter) parent.getAdapter()).getColor(position),
                                new AmbilWarnaDialog.OnAmbilWarnaListener() {

                                    @Override
                                    public void onOk(AmbilWarnaDialog dialog, int color) {
                                        // color is the color selected by the user

                                        /// Transform the palette's colors
                                        if (currentMenuMode != EDIT_MENU) {
                                            // In transformation mode we want to adjust all color - If there is a problem we just relaunch kmeans
                                            ((PaletteAdapter) parent.getAdapter()).updateAll(position, color);
                                        } else {
                                            // In edit mode we want to change only the selected color
                                            ((PaletteAdapter) parent.getAdapter()).setColor(color);
                                        }

                                        // If there is a picture to modify and the palette is not in edit mode
                                        if (!mPicture.isFileNull() && currentMenuMode != EDIT_MENU) {
                                            // We transform the grid
                                            rsProcessing.transGrid(ourPalette);

                                            // And finally we can also transform the image
                                            rsProcessing.transImage(mPicture.getScaled());
                                            mPicture.displayScaledImage(mView);
                                        }
                                        // Deselect the modified color box
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
                }
                // If the selection is the ADD color button
                // must be in edit mode to perform this action
                else if (position == pA.getSize() && pA.isEditing()){

                    // first deselect any selected color box
                    pA.setSelectedBox(-1);

                    // Display an alert window to assert the adding of a color
                    if (pA.isColorManuallyChanged()) {
                        new AlertDialog.Builder(CameraActivity.this)
                                .setCancelable(true)
                                .setTitle(R.string.addAColor)
                                .setMessage(R.string.resetWarning)
                                .setIcon(android.R.drawable.ic_dialog_alert)
                                .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int whichButton) {
                                        // We had a color in palette
                                        pA.addColorContainer();
                                        // And we extract the palette with kmeans
                                        launchAsyncPaletteExtract();
                                    }})
                                .setNegativeButton("Cancel", null).show();
                    } else {
                        // We had a color in palette
                        pA.addColorContainer();
                        // And we extract the palette with kmeans
                        launchAsyncPaletteExtract();
                    }

                }
                // if the selected item is the magic extraction palette button -> extract the palette
                // Must be in edit mode to perform this action
                else if(position == pA.getSize()+1 && pA.isEditing()){
                    // Extract palette if image exists
                    if(!mPicture.isFileNull()) {
                        pA.setColorManuallyChanged(false);
                        launchAsyncPaletteExtract();
                    }
                }
                // otherwise do nothing because we are not on a clickable element
            }
        });

        // Add a listener on long click on item.
        // This action is performed only in EDIT mode because it does not make really sense in main mode
        ourPalette.setOnItemLongClickListener(new AdapterView.OnItemLongClickListener() {

            @Override
            public boolean onItemLongClick(final AdapterView<?> parent, View view, final int position, long id) {

                // Allow to select pixel from the picture only if the user is in edit palette mode
                if(currentMenuMode == EDIT_MENU) {
                    ((PaletteAdapter) parent.getAdapter()).setSelectedBox(position);
                }
                return true;
            }
        });

        // Add a listener on touch item
        // This is used to remove items in edit mode.
        ourPalette.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                final PaletteAdapter pA = (PaletteAdapter) ourPalette.getAdapter();

                // If the palette is in edit mode and if it is allowed to remove an item
                if(pA.isEditing() && pA.getSize() > PaletteAdapter.PALETTE_MIN_SIZE) {

                    switch (event.getAction()) {
                        // On down touch set the position of the touch (use to find the corresponding color)
                        case MotionEvent.ACTION_DOWN:
                            historicX = event.getX();
                            historicY = event.getY();
                            break;

                        // when release (up action) check if the swipe right on the item is
                        // large enough to remove the item
                        case MotionEvent.ACTION_UP:
                            if (event.getX() - historicX > DELTA) {
                                final int position = ourPalette.pointToPosition((int) historicX, (int) historicY);

                                // Display an alert window to assert the delete of a color
                                if (pA.isColorManuallyChanged()) {
                                    new AlertDialog.Builder(CameraActivity.this)
                                            .setTitle(R.string.removeAColor)
                                            .setMessage(R.string.resetWarning2)
                                            .setIcon(android.R.drawable.ic_dialog_alert)
                                            .setPositiveButton(android.R.string.yes, new DialogInterface.OnClickListener() {

                                                public void onClick(DialogInterface dialog, int whichButton) {
                                                    pA.removeColorContainer(position);
                                                    // We recompute the palette with the new palette size
                                                    launchAsyncPaletteExtract();
                                                }})
                                            .setNegativeButton(android.R.string.no, null).show();
                                } else {
                                    pA.removeColorContainer(position);
                                    // We recompute the palette with the new palette size
                                    launchAsyncPaletteExtract();
                                }

                                return true;
                            }
                            break;
                        default:
                            return false;
                    }
                }
                return false;
            }
        });

        // set a on touch listener on the Miniature image. This is use to get a miniature
        // pixel color in edit mode when a box has been long click selected
        // We must be in edit mode to perform this action, and the picture must be non null
        mView.setOnTouchListener(new View.OnTouchListener() {

            @Override
            public boolean onTouch(View v, MotionEvent event) {

                PaletteAdapter pA = ((PaletteAdapter) ourPalette.getAdapter());

                if(!mPicture.isFileNull() && pA.isEditing() && pA.isBoxSelected()) {
                    int[] viewCorrs = new int[2];
                    mView.getLocationOnScreen(viewCorrs);

                    int touchX = (int) event.getX();
                    int touchY = (int) event.getY();
                    int color = mPicture.getColor(touchX, touchY);

                    if (color != Color.TRANSPARENT)
                        pA.setColor(color);

                    return true;
                }
                return false;
            }
        });


        // help dialog window
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.dialog_help_message)
               .setTitle(R.string.dialog_help_title);

        // Add the buttons
        builder.setPositiveButton(R.string.dialog_help_close, new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                // User clicked OK button
            }
        });


        // Get the AlertDialog from create()
        helpDialog = builder.create();




        ////////////////////////////////////////////////////////////////////////////////////////////
        // RECOVERING THE INSTANCE STATE
        ////////////////////////////////////////////////////////////////////////////////////////////

        if (savedInstanceState != null) {
            if(DEBUG_LOG) {
                Log.e("CYCLE", "ON RESTORE");
            }
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

        if (!mPicture.isEmpty()) {
            // Uncomment the commented section of this function to have a progress bar on palette extraction
            /*
            final ProgressBar paletteProgressBar = (ProgressBar)(findViewById(R.id.palette_progressbar));
            paletteProgressBar.setVisibility(View.VISIBLE);
            */
            ourPalette.setVisibility(View.GONE);
            AsyncTask<Integer, Object, List<LabColor>> extractPalette = new AsyncTask<Integer, Object, List<LabColor>>(){

                @Override
                protected List<LabColor> doInBackground(Integer... size) {
                    int paletteSize = size[0];
                    Bitmap smallImage = mPicture.getKmean();

                    if(DEBUG_LOG) {
                        Log.d("resolution", Integer.toString(smallImage.getWidth()) + " " + Integer.toString(smallImage.getHeight()));
                    }

                    Kmeans kmeans = new Kmeans(paletteSize, smallImage, rsProcessing);
                    List<LabColor> paletteColors = kmeans.run(rsProcessing);
                    Collections.sort(paletteColors, new Comparator<LabColor>() {
                        @Override
                        public int compare(LabColor o1, LabColor o2) {
                            return (o1.getL() > o2.getL()) ? 1 : (o1.getL() < o2.getL()) ? -1 : 0;
                        }
                    });

                    return paletteColors;
                }

                @Override
                protected void onPostExecute(List<LabColor> labColors) {
                    for (int i = 1; i < labColors.size(); i++) {
                        LabColor Lab = labColors.get(i);
                        ((PaletteAdapter) ourPalette.getAdapter()).setColor(i-1, ColorUtils.LABToColor(Lab.getL(), Lab.getA(), Lab.getB()));
                    }

                    // Init the palette
                    rsProcessing.initTransPalette(ourPalette);
                    ourPalette.setVisibility(View.VISIBLE);

                    // Uncomment the commented section of this function to have a progress bar on palette extraction
                    /*
                    paletteProgressBar.setVisibility(View.GONE);
                    */
                }
            };

            int paletteSize = ((PaletteAdapter) ourPalette.getAdapter()).getSize();
            extractPalette.execute(paletteSize);
        }
    }


    /**
     * invoked when the activity may be temporarily destroyed, save the instance state here
     * @param outState a Bundle
     */
    @Override
    public void onSaveInstanceState(Bundle outState) {

        if(DEBUG_LOG) {
            Log.e("CYCLE", "ON SAVE");
        }

        if(! mPicture.isFileNull())
            outState.putString("FILE_KEY", mPicture.fileAbsolutePath());

        outState.putInt("HEIGHT_KEY", mPicture.getHeight());
        outState.putInt("WIDTH_KEY", mPicture.getWidth());

        // call superclass to save any view hierarchy
        super.onSaveInstanceState(outState);
    }


    /**
     *  Create the menu of the activity
     * @param menu the menu to display
     * @return if the menu has been created with success
     */
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {

        if(DEBUG_LOG) {
            Log.e("CYCLE", "ON CREATE OPTION MENU");
        }

        this.menu = menu;
        // Inflate the menu; this adds items to the action bar if it is present.
        return setMenuMode(MAIN_MENU);
    }


    /**
     * Set the menu given a mode parameter
     * Allow to switch between edit menu mode and main menu mode
     * @param mode the menu mode as an integer
     * @return if the menu has been set or not
     */
    private boolean setMenuMode(int mode){
        if(DEBUG_LOG) {
            Log.e( "CYCLE", "SET MENU" );
        }

        // Clear view of previous menu
        menu.clear();
        MenuInflater inflater = getMenuInflater();
        if(mode == EDIT_MENU) {
            inflater.inflate(R.menu.menu_edit, menu);
            setTitle(R.string.edit_title);
        }
        else if (mode == MAIN_MENU) {
            inflater.inflate(R.menu.menu_camera, menu);
            setTitle(R.string.main_title);
        }
        else {
            return false;
        }
        currentMenuMode = mode;
        return true;
    }


    /**
     * Perform action when an menu item is selected
     * @param item the selected menu item
     * @return if selection operation is a success
     */
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {

        if(DEBUG_LOG) {
            Log.e("CYCLE", "ON OPTION ITEM SELECTED");
        }

        PaletteAdapter a = ((PaletteAdapter) ourPalette.getAdapter());

        switch (item.getItemId()) {
            // Enter in the edit palette mode
            case R.id.edit_palette_item:
                setMenuMode(EDIT_MENU);
                a.enableEditing();
                return true;

            // Launch the camera to take a new picture
            case R.id.open_camera_item:
                takePicture();
                return true;

            // Open the gallery to select a new picture
            case R.id.open_gallery_item:
                selectPicture();
                return true;

            // Reset the previous imported picture with the first extracted palette before any change
            case R.id.reset_image_item:
                if(!mPicture.isFileNull()) {
                    mPicture.restoreFile(initialImagePath);
                    mPicture.setPicture(mView);
                    // We launch the extraction of the palette here in async
                    launchAsyncPaletteExtract();
                    return true;
                }
                return false;

            // Validate the edited palette and return to main transformation mode
            case R.id.validate_item:
                setMenuMode(MAIN_MENU);
                a.disableEditing(true);

                // The initial palette will be updated
                if (!mPicture.isFileNull()) {
                    // We create the new palette
                    rsProcessing.initTransPalette(ourPalette);
                }
                return true;

            // Cancel the edited palette and return to main transformation mode
            case R.id.cancel_item:
                setMenuMode(MAIN_MENU);
                a.disableEditing(false);

                return true;

            case R.id.help_item:
                helpDialog.show();
                return true;

            case R.id.export_image_item:
                if(mPicture != null && !mPicture.isFileNull()) {
                    mPicture.exportImage();
                    Toast.makeText(this, R.string.confirmExport, Toast.LENGTH_SHORT).show();
                }
                return true;

            default:
                return super.onOptionsItemSelected(item);
        }
    }


    /**
     * Recycle element on activity destroy
     */
    public void onDestroy() {
        if(DEBUG_LOG) {
            Log.e("CYCLE", "ON DESTROY");
        }
        super.onDestroy();
        mPicture.recycle();
    }


    /**
     * Perform action when returning from another activity (i.e camera gallery)
     * @param requestCode the code of the indent call to another activity
     * @param resultCode the code returned by the other activity
     * @param data if any, the data returned by the other activity
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if(DEBUG_LOG) {
            Log.e( "CYCLE", "ON RESULT" );
        }

        // if results comes from the camera activity and is valid
        if (resultCode == RESULT_OK && requestCode == CAMERA_RESULT) {

            Uri selectedImage = galleryAddPic();
            initialImagePath = getPath(this, selectedImage);

            mPicture.restoreFile(initialImagePath);
            mPicture.setPicture(mView);
            // We launch the extraction of the palette here in async
            launchAsyncPaletteExtract();
        }
        // if result comes from the gallery and is valid
        else if (resultCode == RESULT_OK && requestCode == GALLERY_RESULT) {
            Uri selectedImage = data == null ? null : data.getData();
            initialImagePath = getPath(this, selectedImage);

            mPicture.restoreFile(initialImagePath);
            mPicture.setPicture(mView);
            // We launch the extraction of the palette here in async
            launchAsyncPaletteExtract();
        }

        if (resultCode == RESULT_OK && !mPicture.isFileNull()) {
            try {
                rsProcessing.rsClose();
            } catch (Exception e) {
                // To avoid closing a non existing element
            }

            // We initialise the render script
            rsProcessing.rsInit(getApplicationContext());
            // Init the grid
            rsProcessing.initGrid(mPicture.getScaled());
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

            // Should be unnecessary because sdk should never be under 21
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

        // Should be unnecessary because sdk should never be under 21
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