package net.littlejake.qr.ua;

import android.Manifest;
import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Looper;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;

import com.uuzuche.lib_zxing.activity.CaptureActivity;
import com.uuzuche.lib_zxing.activity.CodeUtils;
import com.uuzuche.lib_zxing.activity.ZXingLibrary;

import org.jetbrains.annotations.NotNull;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;

import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

public class MainActivity extends AppCompatActivity {
    //读写权限
    final private static String[] PERMISSIONS = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA,
            Manifest.permission.VIBRATE,
    };
    //请求状态码
    final private static int REQUEST_PERMISSION_CODE = 1;
    final private static int REQUEST_CODE_SCAN = 1;

    //UA
    private static Map<Integer, String> UA = new HashMap<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        //初始化控件
        final Button save_qr = findViewById(R.id.save_qr);
        final Button copy_url = findViewById(R.id.copy_url);
        final Button scan_qr = findViewById(R.id.scan_qr);

        final ImageView image_view = findViewById(R.id.image_view);

        final TextView url = findViewById(R.id.url);

        //初始化UA
        UA.put(R.id.wechat, "Mozilla/5.0 (Linux; Android 10; ELS-NX9 Build/HUAWEIELS-N29; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/78.0.3904.62 XWEB/2759 MMWEBSDK/201201 Mobile Safari/537.36 MMWEBID/1583 MicroMessenger/8.0.1.1840(0x2800013B) Process/tools WeChat/arm64 Weixin NetType/4G Language/zh_CN ABI/arm64");
        UA.put(R.id.unionpay, "Mozilla/5.0 (Linux; Android 10; V1986A Build/QP1A.190711.020; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/78.0.3904.96 Mobile Safari/537.36(com.unionpay.mobilepay) (cordova 7.0.0) (updebug 0) (clientVersion 233) (version 803)(UnionPay/1.0 CloudPay)(language zh_CN)");
        UA.put(R.id.alipay, "Mozilla/5.0 (iPhone; CPU iPhone OS 11_4_1 like Mac OS X) AppleWebKit/605.1.15 (KHTML, like Gecko) Mobile/15G77 NebulaSDK/1.8.100112 Nebula PSDType(1) AlipayDefined(nt:4G,ws:320|504|2.0) AliApp(AP/10.1.32.600) AlipayClient/10.1.32.600 Alipay Language/zh-Hans");
        UA.put(R.id.qq, "Mozilla/5.0 (Linux; Android 10; OPPO R9tm Build/LMY47I; wv) AppleWebKit/537.36 (KHTML, like Gecko) Version/4.0 Chrome/78.0.3904.62 Mobile MQQBrowser/6.2 TBS/043128 Safari/537.36 V1_AND_SQ_7.0.0_676_YYB_D PA QQ/7.0.0.3135 NetType/4G WebP/0.3.0 Pixel/1080");

        //权限初始化
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.LOLLIPOP)
            ActivityCompat.requestPermissions(MainActivity.this
                    , PERMISSIONS, REQUEST_PERMISSION_CODE);

        //zxing init
        ZXingLibrary.initDisplayOpinion(this);

        //监听按钮事件
        save_qr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { saveQR(image_view); }
        });

        scan_qr.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, CaptureActivity.class);
                startActivityForResult(intent, REQUEST_CODE_SCAN);
            }
        });

        copy_url.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) { copyURL(url); }
        });
    }

    /**
     * 处理QR，访问并处理URL
     *
     * @param data 需要处理的URL
     */
    protected void handleQR(String data){
        final TextView url = findViewById(R.id.url);
        final ImageView image_view = findViewById(R.id.image_view);
        final RadioGroup rg = findViewById(R.id.qr_type);

        try {
            new URL(data);
        } catch(MalformedURLException e) {
            Toast.makeText(MainActivity.this, "URL格式错误", Toast.LENGTH_LONG).show();
            return;
        }

        url.setText(data);

        OkHttpClient client = new OkHttpClient();
        Request request = new Request.Builder()
                .header("Accept","*/*")
                .header("Connection","keep-alive")
                .header("Accept-Language","zh-tw")
                .header("Accept-Encoding","")
                .header("User-Agent", Objects.requireNonNull(UA.get(rg.getCheckedRadioButtonId())))
                .url(data)
                .get()
                .build();

        Call call = client.newCall(request);

        call.enqueue(new Callback() {
            @Override
            public void onFailure(@NotNull Call call, @NotNull IOException e) {
                Looper.prepare();
                Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
                Looper.loop();
            }

            @Override
            public void onResponse(@NotNull Call call, @NotNull final Response response) {
                Looper.prepare();
                try {
                    String data = response.request().url().toString();
                    url.setText(data);
                    generateQR(image_view, data);
                    Toast.makeText(MainActivity.this, "生成成功", Toast.LENGTH_LONG).show();
                } catch (Exception e) {
                    Toast.makeText(MainActivity.this,e.getMessage() , Toast.LENGTH_LONG).show();
                } finally {
                    Looper.loop();
                }
            }

        });
    }

    /**
     * 保存QR
     *
     * @param image_view 图像view
     */
    protected void saveQR(ImageView image_view){
        //开启缓存
        image_view.setDrawingCacheEnabled(true);
        //获取bitmap
        Bitmap bm = Bitmap.createBitmap(image_view.getDrawingCache());
        //关闭缓存
        image_view.setDrawingCacheEnabled(false);
        //保存本地

        String path = Environment.getExternalStorageDirectory().getPath() + "/Download/";
        String fileName = "UA_QR.png";

        File file  = new File(path, fileName);
        try {
            //创建文件
            file.createNewFile();
            //Bitmap写入文件
            BufferedOutputStream bos = new BufferedOutputStream(new FileOutputStream(file));
            bm.compress(Bitmap.CompressFormat.PNG, 100, bos);
            bos.flush();
            bos.close();
            //通知媒体文件扫描
            sendBroadcast(new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE, Uri.fromFile(file)));
            Toast.makeText(MainActivity.this, "文件保存至" + file.getAbsolutePath()
                    , Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 根据data判断是否为URL并处理生成QR和最终URL
     *
     * @param image_view 图像view
     * @param data 传入QR解析的data
     */
    protected void generateQR(final ImageView image_view, String data) {
        final Bitmap bm = CodeUtils.createImage(data, 300, 300, null);
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                image_view.setImageBitmap(bm);
            }
        });
    }

    /**
     * 复制URL的TextView内容
     *
     * @param url URL展示view
     */
    protected void copyURL(TextView url){
        try {
            ClipboardManager cm = (ClipboardManager) getSystemService(Context.CLIPBOARD_SERVICE);
            ClipData cd = ClipData.newPlainText("label", url.getText());
            cm.setPrimaryClip(cd);
            Toast.makeText(MainActivity.this, "复制成功", Toast.LENGTH_SHORT).show();
        } catch (Exception e) {
            Toast.makeText(MainActivity.this, e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }


    /**
     * 处理QR扫描intent
     *
     * @param requestCode 请求code
     * @param resultCode 结果code
     * @param data intent传递内容
     */
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_SCAN) {
            if (null != data) {
                Bundle bundle = data.getExtras();

                if (bundle == null) return;

                if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_SUCCESS) {
                    String result = bundle.getString(CodeUtils.RESULT_STRING);
                    handleQR(result);
                    Toast.makeText(MainActivity.this, "解析成功", Toast.LENGTH_SHORT).show();
                } else if (bundle.getInt(CodeUtils.RESULT_TYPE) == CodeUtils.RESULT_FAILED) {
                    Toast.makeText(MainActivity.this, "解析二维码失败", Toast.LENGTH_LONG).show();
                }
            }
        }
    }
}