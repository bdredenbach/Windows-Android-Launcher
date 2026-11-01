package com.nthexperience.desktop;

import android.content.Context;
import android.graphics.*;
import android.net.Uri;
import android.os.Build;
import android.util.Base64;
import android.view.View;
import android.widget.*;
import java.io.*;

/** A small private copy survives provider changes and travels with desktop backups. */
final class ProfilePortrait {
    private static final int SIZE=256,MAX_TEXT=180000;
    static String importPicture(Context context,Uri uri)throws IOException {
        if(!"content".equals(uri.getScheme()))throw new IOException("Choose a picture from the file picker");
        Bitmap input;
        if(Build.VERSION.SDK_INT>=28){
            input=ImageDecoder.decodeBitmap(ImageDecoder.createSource(context.getContentResolver(),uri),(decoder,info,source)->{
                int w=info.getSize().getWidth(),h=info.getSize().getHeight();float ratio=Math.min(1,512f/Math.max(w,h));decoder.setTargetSize(Math.max(1,Math.round(w*ratio)),Math.max(1,Math.round(h*ratio)));decoder.setAllocator(ImageDecoder.ALLOCATOR_SOFTWARE);
            });
        }else{
            BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;try(InputStream in=context.getContentResolver().openInputStream(uri)){BitmapFactory.decodeStream(in,null,bounds);}
            if(bounds.outWidth<1||bounds.outHeight<1)throw new IOException("Choose a readable picture");
            BitmapFactory.Options options=new BitmapFactory.Options();options.inSampleSize=1;while(Math.max(bounds.outWidth,bounds.outHeight)/options.inSampleSize>512)options.inSampleSize*=2;
            try(InputStream in=context.getContentResolver().openInputStream(uri)){input=BitmapFactory.decodeStream(in,null,options);}
        }
        if(input==null)throw new IOException("Choose a readable picture");
        Bitmap square=Bitmap.createBitmap(SIZE,SIZE,Bitmap.Config.ARGB_8888);
        try{
            int edge=Math.min(input.getWidth(),input.getHeight()),x=(input.getWidth()-edge)/2,y=(input.getHeight()-edge)/2;
            new Canvas(square).drawBitmap(input,new Rect(x,y,x+edge,y+edge),new Rect(0,0,SIZE,SIZE),new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG));
            ByteArrayOutputStream bytes=new ByteArrayOutputStream();square.compress(Build.VERSION.SDK_INT>=30?Bitmap.CompressFormat.WEBP_LOSSLESS:Bitmap.CompressFormat.WEBP,96,bytes);
            if(bytes.size()>MAX_TEXT*3/4){bytes.reset();square.compress(Bitmap.CompressFormat.WEBP,96,bytes);}
            String encoded=Base64.encodeToString(bytes.toByteArray(),Base64.NO_WRAP);if(!valid(encoded))throw new IOException("This picture could not be prepared");return encoded;
        }finally{input.recycle();square.recycle();}
    }
    static boolean valid(String text){
        if(text.isEmpty())return true;if(text.length()>MAX_TEXT)return false;
        try{byte[] bytes=Base64.decode(text,Base64.NO_WRAP);BitmapFactory.Options bounds=new BitmapFactory.Options();bounds.inJustDecodeBounds=true;BitmapFactory.decodeByteArray(bytes,0,bytes.length,bounds);return bounds.outWidth==SIZE&&bounds.outHeight==SIZE;}catch(IllegalArgumentException ex){return false;}
    }
    static View view(Context c,DesktopStore store){
        FrameLayout frame=new FrameLayout(c);frame.setBackground(Aero.panel(c,0xffecf8ff,0xff6c8eab,5,0xffb6d5e8));int pad=Aero.dp(c,3);frame.setPadding(pad,pad,pad,pad);
        String value=store.prefs.getString("profile_portrait","");Bitmap bitmap=null;
        if(!value.isEmpty()&&valid(value))try{byte[] bytes=Base64.decode(value,Base64.NO_WRAP);bitmap=BitmapFactory.decodeByteArray(bytes,0,bytes.length);}catch(IllegalArgumentException ignored){}
        View image;if(bitmap==null)image=new AeroIcon(c,"orb");else{ImageView photo=new ImageView(c);photo.setImageBitmap(bitmap);photo.setScaleType(ImageView.ScaleType.CENTER_CROP);image=photo;}
        frame.addView(image,new FrameLayout.LayoutParams(-1,-1));frame.setContentDescription("Profile picture");return frame;
    }
}
