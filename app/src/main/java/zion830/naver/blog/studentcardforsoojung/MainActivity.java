package zion830.naver.blog.studentcardforsoojung;

import android.Manifest;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Base64;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.Switch;
import android.widget.Toast;

import java.io.ByteArrayOutputStream;
import java.io.File;

public class MainActivity extends AppCompatActivity {
    private static final int REQUEST_EXTERNAL_STORAGE = 0;
    private static final int PICK_IMAGE = 1;
    private static final int CROP_IMAGE = 2;

    private Uri imgUri;
    private Switch onOffSwitch;
    private SharedPreferences pref;
    private ImageView cardImg;
    private WindowManager.LayoutParams params;
    private float brightness; //밝기 값


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        setTitle("수정이의 모바일 학생증 보관함");

        onOffSwitch = findViewById(R.id.on_off_switch);
        cardImg = findViewById(R.id.card_img);
        pref = getSharedPreferences("pref", MODE_PRIVATE);

        //화면 정보 불러오기
        params = getWindow().getAttributes();
        brightness = params.screenBrightness;

        String stateStr = pref.getString("state", "true");
        String bmp = pref.getString("img", "none");

        onOffSwitch.setChecked(Boolean.valueOf(stateStr));
        if (onOffSwitch.isChecked()) {
            //최대 밝기로 변경
            params.screenBrightness = 1f;
            getWindow().setAttributes(params);
        }

        if (bmp.equals("none")) {
            cardImg.setImageResource(R.drawable.info_test);
        } else {
            SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
            String image = pref.getString("img", "none");

            cardImg.setImageBitmap(StringToBitMap(image));
        }

        onOffSwitch.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    //최대 밝기로 변경
                    params.screenBrightness = 1f;
                    getWindow().setAttributes(params);
                } else {
                    params.screenBrightness = brightness;
                    getWindow().setAttributes(params);
                }

                savePreferences();
            }
        });

        //권한 허가 요청
        askForPermission();
    }

    @Override
    protected void onResume() {
        super.onResume();
        String stateStr = pref.getString("state", "true");

        if (Boolean.valueOf(stateStr)) {
            //최대 밝기로 변경
            params.screenBrightness = 1f;
            getWindow().setAttributes(params);
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        //기존 밝기로 변경
        params.screenBrightness = brightness;
        getWindow().setAttributes(params);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (id == R.id.action_settings) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(MainActivity.this);
            builder.setMessage("수정이들의 편리한 학생증 라이프에 보탬이 되기를 바랍니다~\n\n배포일자 : 2018-7-12")
                    .setNegativeButton("확인", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {

                        }
                    }).show();
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    public void changePicBtnOnClicked(View view) {
        Intent intent = new Intent(Intent.ACTION_PICK);
        intent.setType(MediaStore.Images.Media.CONTENT_TYPE);
        startActivityForResult(intent, PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        Log.d("경로", resultCode + " " + requestCode);
        if (resultCode == -1 && requestCode != 2) {
            imgUri = data.getData();

            Intent crop = new Intent("com.android.camera.action.CROP");
            crop.setDataAndType(imgUri, "image/*");

            crop.putExtra("outputX", 690);
            crop.putExtra("outputY", 420);
            crop.putExtra("aspectX", 69);
            crop.putExtra("aspectY", 42);
            crop.putExtra("scale", true);
            crop.putExtra("return-data", true);

            startActivityForResult(crop, CROP_IMAGE);
        } else if (requestCode == CROP_IMAGE && resultCode != 0) { //크롭 된 후
            String filePath = Environment.getExternalStorageDirectory().getAbsolutePath() + System.currentTimeMillis() + ".jpg";

            final Bundle extras = data.getExtras();
            if (extras != null) {
                Bitmap photo = extras.getParcelable("data");
                cardImg.setImageBitmap(photo);

                saveImgPreferences(photo);
            }
        }
    }

    public void getCardBtnOnClicked(View view) {
        Intent link = new Intent(Intent.ACTION_VIEW, Uri.parse("http://smart.sungshin.ac.kr/user/main.jsp"));
        startActivity(link);
    }

    private void savePreferences() {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("state", String.valueOf(onOffSwitch.isChecked()));
        editor.commit();
    }

    private void saveImgPreferences(Bitmap bitmap) {
        SharedPreferences pref = getSharedPreferences("pref", MODE_PRIVATE);
        SharedPreferences.Editor editor = pref.edit();
        editor.putString("img", BitmapToString(bitmap));

        editor.commit();
    }

    public String BitmapToString(Bitmap bitmap) {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        bitmap.compress(Bitmap.CompressFormat.PNG, 100, baos);
        byte[] b = baos.toByteArray();

        String result = Base64.encodeToString(b, Base64.DEFAULT);

        return result;
    }

    public Bitmap StringToBitMap(String encodedString) {
        try {
            byte[] encodeByte = Base64.decode(encodedString, Base64.DEFAULT);
            Bitmap bitmap = BitmapFactory.decodeByteArray(encodeByte, 0, encodeByte.length);
            return bitmap;
        } catch (Exception e) {
            e.getMessage();
            return null;
        }
    }

    //권한 요청
    private void askForPermission() {
        String[] permissions = new String[]{Manifest.permission.READ_EXTERNAL_STORAGE};
        ActivityCompat.requestPermissions(this, permissions, REQUEST_EXTERNAL_STORAGE);
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (requestCode == REQUEST_EXTERNAL_STORAGE) {
            for (int i = 0; i < permissions.length; i++) {
                String permission = permissions[i];
                int grantResult = grantResults[i];

                if (permission.equals(Manifest.permission.READ_EXTERNAL_STORAGE)) {
                    if (grantResult != PackageManager.PERMISSION_GRANTED) {
                        Toast.makeText(this, "앱을 이용하기 위해 접근 권한 허가가 필요합니다.", Toast.LENGTH_LONG).show();
                        this.finish();
                    }
                }
            }
        }
    }
}
