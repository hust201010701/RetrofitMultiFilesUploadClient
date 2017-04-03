package com.orzangleli.retrofitmultifilesuploadclient;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AbsListView;
import android.widget.BaseAdapter;
import android.widget.Button;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import com.lzy.imagepicker.ImagePicker;
import com.lzy.imagepicker.bean.ImageItem;
import com.lzy.imagepicker.ui.ImageGridActivity;
import com.lzy.imagepicker.view.CropImageView;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Call;
import retrofit2.Callback;
import retrofit2.Response;

public class MainActivity extends AppCompatActivity {

    TextView images;
    Button upload;
    GridView gridView;
    ImagePicker imagePicker;
    ArrayList<ImageItem> imagesList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        images = bindView(R.id.images);
        upload = bindView(R.id.upload);
        gridView = bindView(R.id.gridView);

        imagesList = new ArrayList<>();

        imagePicker = ImagePicker.getInstance();
        imagePicker.setImageLoader(new GlideImageLoader());   //设置图片加载器
        imagePicker.setShowCamera(true);  //显示拍照按钮
        imagePicker.setCrop(true);        //允许裁剪（单选才有效）
        imagePicker.setSaveRectangle(true); //是否按矩形区域保存
        imagePicker.setSelectLimit(9);    //选中数量限制
        imagePicker.setStyle(CropImageView.Style.RECTANGLE);  //裁剪框的形状
        imagePicker.setFocusWidth(800);   //裁剪框的宽度。单位像素（圆形自动取宽高最小值）
        imagePicker.setFocusHeight(800);  //裁剪框的高度。单位像素（圆形自动取宽高最小值）
        imagePicker.setOutPutX(1000);//保存文件的宽度。单位像素
        imagePicker.setOutPutY(1000);//保存文件的高度。单位像素


        images.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(MainActivity.this, ImageGridActivity.class);
                startActivityForResult(intent, 1);
            }
        });

        upload.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(MainActivity.this, "正在上传", Toast.LENGTH_SHORT).show();
                uploadFiles();
            }
        });

    }


    public <T extends View> T bindView(int id) {
        return (T) this.findViewById(id);
    }

    public void uploadFiles() {
        if(imagesList.size() == 0) {
            Toast.makeText(MainActivity.this, "不能不选择图片", Toast.LENGTH_SHORT).show();
            return;
        }
        Map<String, RequestBody> files = new HashMap<>();
        final FlaskClient service = ServiceGenerator.createService(FlaskClient.class);
        for (int i = 0; i < imagesList.size(); i++) {
            File file = new File(imagesList.get(i).path);
            files.put("file" + i + "\"; filename=\"" + file.getName(), RequestBody.create(MediaType.parse(imagesList.get(i).mimeType), file));
        }
        Call<UploadResult> call = service.uploadMultipleFiles(files);
        call.enqueue(new Callback<UploadResult>() {
            @Override
            public void onResponse(Call<UploadResult> call, Response<UploadResult> response) {
                if (response.isSuccessful() && response.body().code == 1) {
                    Toast.makeText(MainActivity.this, "上传成功", Toast.LENGTH_SHORT).show();
                    Log.i("orzangleli", "---------------------上传成功-----------------------");
                    Log.i("orzangleli", "基础地址为：" + ServiceGenerator.API_BASE_URL);
                    Log.i("orzangleli", "图片相对地址为：" + listToString(response.body().image_urls,','));
                    Log.i("orzangleli", "---------------------END-----------------------");
                }
            }

            @Override
            public void onFailure(Call<UploadResult> call, Throwable t) {
                Toast.makeText(MainActivity.this, "上传失败", Toast.LENGTH_SHORT).show();
            }
        });

    }


    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (resultCode == ImagePicker.RESULT_CODE_ITEMS) {
            if (data != null && requestCode == 1) {
                imagesList = (ArrayList<ImageItem>) data.getSerializableExtra(ImagePicker.EXTRA_RESULT_ITEMS);
                MyAdapter adapter = new MyAdapter(imagesList);
                gridView.setAdapter(adapter);
                images.setText("已选择" + imagesList.size() + "张");
            } else {
                Toast.makeText(this, "没有选择图片", Toast.LENGTH_SHORT).show();
            }
        }
    }


    private class MyAdapter extends BaseAdapter {

        private List<ImageItem> items;

        public MyAdapter(List<ImageItem> items) {
            this.items = items;
        }

        public void setData(List<ImageItem> items) {
            this.items = items;
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return items.size();
        }

        @Override
        public ImageItem getItem(int position) {
            return items.get(position);
        }

        @Override
        public long getItemId(int position) {
            return position;
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            ImageView imageView;
            int size = gridView.getWidth() / 3;
            if (convertView == null) {
                imageView = new ImageView(MainActivity.this);
                AbsListView.LayoutParams params = new AbsListView.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, size);
                imageView.setLayoutParams(params);
                imageView.setBackgroundColor(Color.parseColor("#88888888"));
            } else {
                imageView = (ImageView) convertView;
            }
            imagePicker.getImageLoader().displayImage(MainActivity.this, getItem(position).path, imageView, size, size);
            return imageView;
        }
    }


    public String listToString(List list, char separator) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < list.size(); i++) {
            sb.append(list.get(i)).append(separator);
        }
        return sb.toString().substring(0, sb.toString().length() - 1);
    }

}
