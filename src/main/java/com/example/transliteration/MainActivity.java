package com.example.transliteration;


import android.Manifest;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.util.Log;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;

import com.google.android.material.textfield.TextInputEditText;
import com.googlecode.tesseract.android.TessBaseAPI;
import com.yalantis.ucrop.UCrop;
import org.pytorch.IValue;
import org.pytorch.Module;
import org.pytorch.Tensor;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;


public class MainActivity extends AppCompatActivity {
    private Module mModule;
    String k ;
    public static final String TESS_DATA = "/tessdata";
    private static final String TAG = MainActivity.class.getSimpleName();
    private static final int REQUEST_CAMERA_CODE = 100;
    private static final int SELECT_IMAGE_CODE = 200;
    private Uri imageuri;
    private Uri cropimg;
    EditText t1;
    EditText t2;
    Button capture,gallery,detect,translit;
    String currentPhotoPath;

    int flag = 0;
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mModule = Module.load(assetFilePath(this, "Androidmodel1.pt"));
        main();
    }
    private void main(){
        flag = 0;
        capture = findViewById(R.id.button);
        gallery = findViewById(R.id.gallery);
        capture.setOnClickListener(view -> {
            flag = 1;
            askCameraPermissions();
        });
        gallery.setOnClickListener(view -> {
            flag = 1;
            Intent intent = new Intent();
            intent.setType("image/*");
            intent.setAction(Intent.ACTION_GET_CONTENT);
            startActivityForResult(Intent.createChooser(intent, "Title"), SELECT_IMAGE_CODE);
        });

    }

    private void detect(){
        flag = 1;
        setContentView(R.layout.activity_main3);
        ImageView image = findViewById(R.id.image);
        image.setImageURI(cropimg);
        detect = findViewById(R.id.detect);
        detect.setOnClickListener(view -> {
            detect.setOnClickListener(null);
            setContentView(R.layout.activity_main2);
            prepareTessData();
            k = startOCR(cropimg);
            k = k.replaceAll("[\\\n]"," ");
            k = k.replace("౦", "ం");
            k = k.replace("రి", "ం");
            k = k.replace("  ", " ");
            t1 = findViewById(R.id.textView);
            t1.setText(k);
            t1.setTextSize(18);
            translit = findViewById(R.id.button1);
            translit.setOnClickListener(view1 -> {
                flag =1;
                StringBuilder predicted_words = new StringBuilder();
                predict p = new predict();
                k = t1.getText().toString();
                String s = "\"@#$_&-+()/*':;!?{}.‘’,[]%^<\">|=\\\\0123456789\"";
                String l = "";
                    for (int i = 0; i < k.length(); i++) {
                        char c = k.charAt(i);
                        if (c == ' ') {
                            if (!l.isEmpty()) {
                                predicted_words.append(p.prediction(l));
                            }
                            predicted_words.append("  ");
                            l = "";
                        } else if (s.indexOf(c) == -1) {
                            l += c;
                        } else {
                            if (!l.isEmpty()) {
                                predicted_words.append(p.prediction(l));
                            }
                            predicted_words.append(c);
                            l = "";
                        }
                    }

                t2 = findViewById(R.id.textView2);
                t2.setText(predicted_words.toString());
                t2.setTextSize(18);
            });

        });
    }
    @Override
    public void onBackPressed() {
        if(flag == 1){
            setContentView(R.layout.activity_main);
            main();
        }
        else {
            super.onBackPressed();
        }
    }
    public static String assetFilePath(Context context, String assetName) {
        File file = new File(context.getFilesDir(), assetName);
        try (InputStream is = context.getAssets().open(assetName)) {
            try (OutputStream os = new FileOutputStream(file)) {
                byte[] buffer = new byte[4 * 1024];
                int read;
                while ((read = is.read(buffer)) != -1) {
                    os.write(buffer, 0, read);
                }
                os.flush();
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Error initializing PyTorch", e);
        }
    }
    public static class OneHotEncoder {
        private final char pad = 'P';
        private final char[] telAlphabets = new char[129]; // Maximum possible Telugu letters in Unicode range
        private final int telSize;
        private final HashMap<Character, Integer> telDic = new HashMap<Character, Integer>();

        public OneHotEncoder() {
            int i = 0;
            for (char alpha = '\u0C00'; alpha < '\u0C7F'; alpha++) {
                telAlphabets[i] = alpha;
                i++;
            }
            telAlphabets[i] = '\u200C';
            i++;
            telAlphabets[i] = '\u200D';
            telDic.put(pad, 0);
            for (int index = 0; index < telAlphabets.length; index++) {
                telDic.put(telAlphabets[index], index + 1);
            }
            telSize = telDic.size();
        }

        public Tensor wordRep(String word) {
            float[][][] wordRep = new float[word.length() + 1][1][telSize];
            for (int letterIndex = 0; letterIndex < word.length(); letterIndex++) {
                char letter = word.charAt(letterIndex);
                int pos = telDic.get(letter);
                for (int i = 0; i < telSize; i++) {
                    if (i == pos) {
                        wordRep[letterIndex][0][i] = 1.0f;
                    } else {
                        wordRep[letterIndex][0][i] = 0.0f;
                    }
                }
            }
            int padPos = telDic.get(pad);
            for (int i = 0; i < telSize; i++) {
                if (i == padPos) {
                    wordRep[word.length()][0][i] = 1.0f;
                } else {
                    wordRep[word.length()][0][i] = 0.0f;
                }
            }
            float[] arr1d = new float[wordRep.length * wordRep[0].length * wordRep[0][0].length];
            int k = 0;
            for (float[][] floats : wordRep) {
                for (int j = 0; j < wordRep[0].length; j++) {
                    for (int l = 0; l < wordRep[0][0].length; l++) {
                        arr1d[k++] = floats[j][l];
                    }
                }
            }
            return Tensor.fromBlob(arr1d, new long[] {word.length() + 1, 1, telDic.size()});
        }

    }
    public static class decoder{
            private final String eng_alphabets = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
            private final String pad = "PAD";
            private String s = new String("");
            private final HashMap<Character, Integer> engdic = new HashMap<Character, Integer>();
            public decoder(Tensor[] out){
                engdic.put(pad.charAt(0), 0);
                for (int i = 0; i < eng_alphabets.length(); i++) {
                    char alpha = eng_alphabets.charAt(i);
                    engdic.put(alpha, i + 1);
                }
                // convert the element to a tensor
                for (Tensor tensor : out) {
                    float[] tensorValues = tensor.getDataAsFloatArray();
                    int argmax = 0;
                    float maxValue = tensorValues[0];
                    for (int j = 1; j < tensorValues.length; j++) {
                        if (tensorValues[j] > maxValue) {
                            argmax = j;
                            maxValue = tensorValues[j];
                        }
                    }
                    Character maxChar = null;
                    for (Map.Entry<Character, Integer> entry : engdic.entrySet()) {
                        if (entry.getValue().equals(argmax)) {
                            maxChar = entry.getKey();
                            break;
                        }
                    }
                    if (maxChar != null) {
                        s = s + maxChar;
                    }
                }
            }

            public Character getKeyFromValue(HashMap<Character, Integer> map, Long value) {
                for (HashMap.Entry<Character, Integer> entry : map.entrySet()) {
                    if (Objects.equals(value, entry.getValue())) {
                        return entry.getKey();
                    }
                }
                return null;
            }


    }
    public class predict {
        public String prediction(String s) {
            OneHotEncoder o = new OneHotEncoder();
            Tensor wordRep1 = o.wordRep(s);
            IValue outputTensors = mModule.forward(IValue.from(wordRep1));
            Tensor[] a = outputTensors.toTensorList();
            decoder decode = new decoder(a);
            return decode.s;
        }
    }
    private void askCameraPermissions() {
        if (ContextCompat.checkSelfPermission(this, android.Manifest.permission.CAMERA) != PackageManager.PERMISSION_GRANTED){
            ActivityCompat.requestPermissions(this,new String[] {Manifest.permission.CAMERA},REQUEST_CAMERA_CODE);
        }
        else{
            dispatchTakePictureIntent();
        }
    }
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if(requestCode == UCrop.REQUEST_CROP){
            if(resultCode == RESULT_OK) {
                cropimg = UCrop.getOutput(data);
                detect();
            }
        }
        else if(resultCode == RESULT_OK) {
            if (requestCode == SELECT_IMAGE_CODE) {
                assert data != null;
                Uri uri = data.getData();
                imageuri = uri;

            } else if (requestCode == REQUEST_CAMERA_CODE) {
                Uri uri = Uri.fromFile(new File(currentPhotoPath));
                imageuri = uri;
            }
            String dest = "sample.jpg";
            UCrop.of(imageuri, Uri.fromFile(new File(getCacheDir(), dest)))
                        .withMaxResultSize(2000, 2000)
                        .start(MainActivity.this);
        }

    }
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "JPEG_" + timeStamp + "_";
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File image = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",         /* suffix */
                storageDir      /* directory */
        );
        // Save a file: path for use with ACTION_VIEW intents
        currentPhotoPath = image.getAbsolutePath();
        return image;
    }
    private void dispatchTakePictureIntent() {
        Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
        if (takePictureIntent.resolveActivity(getPackageManager()) != null) {
            File photoFile = null;
            try {
                photoFile = createImageFile();
            } catch (IOException ex) {
            }
            if (photoFile != null) {
                Uri photoURI = FileProvider.getUriForFile(this,
                        "net.example.android.fileprovider",
                        photoFile);
                takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, photoURI);
                startActivityForResult(takePictureIntent, REQUEST_CAMERA_CODE);
            }
        }
    }
    private void prepareTessData(){
        try{
            File dir = getExternalFilesDir(TESS_DATA);
            if(!dir.exists()){
                if (!dir.mkdir()) {
                    Toast.makeText(getApplicationContext(), "The folder " + dir.getPath() + "was not created", Toast.LENGTH_SHORT).show();
                }
            }
            String[] fileList = getAssets().list("");
            for(String fileName : fileList){
                String pathToDataFile = dir + "/" + fileName;
                if(!(new File(pathToDataFile)).exists()){
                    InputStream in = getAssets().open(fileName);
                    OutputStream out = new FileOutputStream(pathToDataFile);
                    byte [] buff = new byte[1024];
                    int len ;
                    while(( len = in.read(buff)) > 0){
                        out.write(buff,0,len);
                    }
                    in.close();
                    out.close();
                }
            }
        } catch (Exception e) {
            Log.e(TAG, e.getMessage());
        }
    }
    private String startOCR(Uri uri){
        String result = new String("not working");
        try{
            Bitmap bitmap = MediaStore.Images.Media.getBitmap(this.getContentResolver(),uri);
            //bitmap = preprocessImage(bitmap);
            result = this.getText(bitmap);

        }catch (Exception e){
            Log.e(TAG, e.getMessage());
        }
        return result;
    }
    private String getText(Bitmap bitmap){
        TessBaseAPI tessBaseAPI;
        tessBaseAPI = new TessBaseAPI();
        String dataPath = getExternalFilesDir("/").getPath() + "/";
        tessBaseAPI.init(dataPath, "tel");
        tessBaseAPI.setVariable(TessBaseAPI.VAR_CHAR_BLACKLIST, "!@#$%^&*()_+=-`~[]{}|;:\"',.<>/?");
        tessBaseAPI.setImage(bitmap);
        String retStr = tessBaseAPI.getUTF8Text();
        tessBaseAPI.end();
        return retStr;
    }
}
