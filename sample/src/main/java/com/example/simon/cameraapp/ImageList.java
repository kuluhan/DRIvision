package com.example.simon.cameraapp;

/**
 * Created by Simon on 2/11/2017.
 */


import android.app.Activity;
import android.app.Dialog;
import android.content.ContentResolver;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.BitmapFactory.Options;
import android.media.ThumbnailUtils;
import android.net.Uri;
import android.os.Bundle;
import android.os.Environment;
import android.provider.MediaStore.Video.Thumbnails;
import android.util.LruCache;
import android.view.View;
import android.view.View.OnClickListener;
import android.view.ViewGroup;
import android.widget.BaseAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.io.File;
import java.util.ArrayList;

import jp.co.recruit.floatingview.R;

public class ImageList extends Activity {
    private String FOLDER_PATH;
    private MyAdpter adpter;
    ArrayList<File> alListOfImages;
    private int cacheSize;
    OnClickListener delete_image_listner;
    private ListView lvDisplayListOfImages;
    private LruCache<Integer, Bitmap> mMemoryCache;
    OnClickListener play_image_listner;
    OnClickListener share_image_listner;

    /* renamed from: com.aquasoltools.dualhdcamera.ImageList.1 */
    class deleteImg implements OnClickListener {
        deleteImg() {
        }
        public void onClick(View v) {
//            ImageList.this.deletConfirmDialog(Integer.parseInt(v.getTag().toString()));
            Toast.makeText(ImageList.this, "Image will be deleated", Toast.LENGTH_SHORT).show();
        }
    }

    /* renamed from: com.aquasoltools.dualhdcamera.ImageList.2 */
    class PreviewImg implements OnClickListener {
        PreviewImg() {
        }

        public void onClick(View v) {
          /*  Intent intent = new Intent();
            intent.setAction("android.intent.action.VIEW");
            intent.setDataAndType(Uri.parse("file://" + ((File) ImageList.this.alListOfImages.get(Integer.parseInt(v.getTag().toString()))).toString()), "image/jpg");
            ImageList.this.startActivity(intent);*/
            Toast.makeText(ImageList.this, "Previewing image in large size", Toast.LENGTH_LONG).show();
        }
    }

    /* renamed from: com.aquasoltools.dualhdcamera.ImageList.3 */
    class shareImg implements OnClickListener {
        shareImg() {
        }

        public void onClick(View v) {
          /*  Intent intent = new Intent("android.intent.action.SEND");
            File file = new File(((File) ImageList.this.alListOfImages.get(Integer.parseInt(v.getTag().toString()))).toString());
            intent.setType("Image/jpg");
            intent.putExtra("android.intent.extra.SUBJECT", "Image");
            intent.putExtra("android.intent.extra.STREAM", Uri.fromFile(file));
            intent.putExtra("android.intent.extra.TEXT", "Enjoy the Image");
            ImageList.this.startActivity(Intent.createChooser(intent, "Email:"));*/

            Intent share = new Intent(Intent.ACTION_SEND);
            File file = new File(((File) ImageList.this.alListOfImages.get(Integer.parseInt(v.getTag().toString()))).toString());
            String message = "Download the app from playstore";
            share.setType("Image/jpg");
            share.putExtra(android.content.Intent.EXTRA_TEXT, message);
            share.putExtra("android.intent.extra.STREAM", Uri.fromFile(file));
            startActivity(Intent.createChooser(share, "share in social media"));
        }
    }

    /* renamed from: com.aquasoltools.dualhdcamera.ImageList.6 */
    class deleteConform implements OnClickListener {
        private final /* synthetic */ Dialog val$builder;
        private final /* synthetic */ int val$index;

        deleteConform(int i, Dialog dialog) {
            this.val$index = i;
            this.val$builder = dialog;
        }

        public void onClick(View v) {
            if (new File(((File) ImageList.this.alListOfImages.get(this.val$index)).toString()).delete()) {
                Toast.makeText(ImageList.this, "Image is delete.",Toast.LENGTH_LONG ).show();;
            } else {
                Toast.makeText(ImageList.this, "Image is not delete.", Toast.LENGTH_LONG).show();
            }
            ImageList.this.alListOfImages.remove(this.val$index);
            ImageList.this.adpter.notifyDataSetChanged();
            this.val$builder.dismiss();
        }
    }

    /* renamed from: com.aquasoltools.dualhdcamera.ImageList.7 */
    class chooseDialog implements OnClickListener {
        private final /* synthetic */ Dialog val$builder;

        chooseDialog(Dialog dialog) {
            this.val$builder = dialog;
        }

        public void onClick(View v) {
            this.val$builder.dismiss();
        }
    }

    public class MyAdpter extends BaseAdapter {

        class ViewHolder {
            ImageView ibtnDelete;
            ImageView ibtnOpen;
            ImageView ibtnShare;
            ImageView imageThumbnail;
            TextView textfilePath;

            ViewHolder() {
            }
        }

        public int getCount() {
            return ImageList.this.alListOfImages.size();
        }

        public Object getItem(int position) {
            return ImageList.this.alListOfImages.get(position);
        }

        public long getItemId(int position) {
            return (long) position;
        }

        public View getView(int position, View convertView, ViewGroup parent) {
            ViewHolder holder;
            if (convertView == null) {
                convertView = ImageList.this.getLayoutInflater().inflate(R.layout.single_row, parent, false);
                holder = new ViewHolder();
                holder.textfilePath = (TextView) convertView.findViewById(R.id.tvImageName);
                holder.imageThumbnail = (ImageView) convertView.findViewById(R.id.ivTumbnail);
                holder.ibtnDelete = (ImageView) convertView.findViewById(R.id.btnDeleteImage);
                holder.ibtnOpen = (ImageView) convertView.findViewById(R.id.btnOpenImage);
                holder.ibtnShare = (ImageView) convertView.findViewById(R.id.btnShareImage);
                convertView.setTag(holder);
            } else {
                holder = (ViewHolder) convertView.getTag();
            }
            holder.ibtnDelete.setTag(Integer.valueOf(position));
            holder.ibtnDelete.setOnClickListener(ImageList.this.delete_image_listner);
            holder.ibtnOpen.setTag(Integer.valueOf(position));
            holder.ibtnOpen.setOnClickListener(ImageList.this.play_image_listner);
            holder.ibtnShare.setTag(Integer.valueOf(position));
            holder.ibtnShare.setOnClickListener(ImageList.this.share_image_listner);
            holder.textfilePath.setText(((File) ImageList.this.alListOfImages.get(position)).getName().substring(0, ((File) ImageList.this.alListOfImages.get(position)).getName().length() - 4));
            Bitmap bmThumbnail = ImageList.this.getBitmapFromMemCache(((File) ImageList.this.alListOfImages.get(position)).hashCode());
            if (bmThumbnail == null) {
                bmThumbnail = ThumbnailUtils.extractThumbnail(BitmapFactory.decodeFile(((File) ImageList.this.alListOfImages.get(position)).toString()), 64, 64);
                ImageList.this.addBitmapToMemoryCache(((File) ImageList.this.alListOfImages.get(position)).hashCode(), bmThumbnail);
            }
            holder.imageThumbnail.setImageBitmap(bmThumbnail);
            return convertView;
        }
    }

    /* renamed from: com.aquasoltools.dualhdcamera.ImageList.4 */
    class memorycache extends LruCache<Integer, Bitmap> {
        memorycache(int store) {
            super(store);
        }

        protected int sizeOf(Integer key, Bitmap bitmap) {
            return (bitmap.getRowBytes() * bitmap.getHeight());
        }
    }

    public ImageList() {
        this.FOLDER_PATH = Environment.getExternalStorageDirectory() + File.separator + "MyCameraApp";
        this.alListOfImages = new ArrayList();
        this.delete_image_listner = new deleteImg();
        this.play_image_listner = new PreviewImg();
        this.share_image_listner = new shareImg();
    }

    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.image_list);
        init();
        bindView();
        addListner();
        this.mMemoryCache = new memorycache(this.cacheSize);
    }


    private void addBitmapToMemoryCache(int key, Bitmap bitmap) {
        if (getBitmapFromMemCache(key) == null) {
            this.mMemoryCache.put(Integer.valueOf(key), bitmap);
        }
    }

    private Bitmap getBitmapFromMemCache(int key) {
        return (Bitmap) this.mMemoryCache.get(Integer.valueOf(key));
    }

    public void bindView() {
        if (this.alListOfImages.size() > 0) {
            this.lvDisplayListOfImages = (ListView) findViewById(R.id.lvImages);
            this.adpter = new MyAdpter();
            this.lvDisplayListOfImages.setAdapter(this.adpter);
            return;
        }
        Toast.makeText(this, "No recording file foutnd", Toast.LENGTH_SHORT).show();
    }

    public void init() {
        this.cacheSize = (int) (Runtime.getRuntime().maxMemory() / 1024);
        this.alListOfImages = getfile(new File(this.FOLDER_PATH), "");
    }

    public void addListner() {
    }

    public ArrayList<File> getfile(File dir, String fileType) {
        File[] listFile = dir.listFiles();
        if (listFile != null && listFile.length > 0) {
            for (int i = 0; i < listFile.length; i++) {
                if (listFile[i].getName().endsWith(".jpg")) {
                    this.alListOfImages.add(listFile[i]);
                }
            }
        }
        return this.alListOfImages;
    }

    Bitmap getThumbNailofVideo(int id) {
        ContentResolver crThumb = getContentResolver();
        Options options = new Options();
        options.inSampleSize = 1;
        return Thumbnails.getThumbnail(crThumb, (long) id, 3, options);
    }

   /* private void deletConfirmDialog(int index) {
        Dialog builder = new Dialog(this, android.R.style.Theme_Dialog);
        builder.getWindow().requestFeature(1);
        builder.setContentView(R.layout.delete_confirm_dialog);
        builder.show();
        TextView tvHeder = (TextView) builder.findViewById(R.id.tvFileNameForDeteleConfirmDialog);
        TextView tvMessages = (TextView) builder.findViewById(R.id.tvSubTitleForDeleteConfirmDialog);
        ((Button) builder.findViewById(R.id.btnYesForDeleteCofirmDialog)).setOnClickListener(new deleteConform(index, builder));
        ((Button) builder.findViewById(R.id.btnNoForDeleteConfirmDailog)).setOnClickListener(new chooseDialog(builder));
    }*/
}